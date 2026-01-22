package com.ludiary.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GroupsRepositoryImpl(
    private val db: LudiaryDatabase,
    private val fs: FirebaseFirestore,
    private val auth: FirebaseAuth
) : GroupsRepository {

    private val groupDao = db.groupDao()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    override fun observeGroups(query: String) = groupDao.observeGroups(query)
    override fun observeMembers(groupId: String) = groupDao.observeMembers(groupId)
    override fun observePendingInvites() = groupDao.observePendingInvites()

    override fun startRemoteSync() {
        val me = auth.currentUser ?: return
        syncJob?.cancel()

        // Nota: listeners reales devuelven ListenerRegistration; aquí seguimos tu estilo simple.
        syncJob = scope.launch {
            Log.d("LUDIARY_GROUPS_DEBUG", "startRemoteSync uid=${me.uid}")

            // 1) Índice: users/{uid}/groups
            fs.collection("users").document(me.uid).collection("groups")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val groups = snap.documents.mapNotNull { d ->
                            val gid = d.id
                            val name = d.getString("nameSnapshot") ?: return@mapNotNull null
                            val createdAt = d.getLong("joinedAt") ?: now
                            val updatedAt = d.getLong("updatedAt") ?: now
                            GroupEntity(gid, name, createdAt, updatedAt)
                        }
                        groupDao.upsertGroups(groups)
                    }
                }

            // 2) Invitaciones a mí (pending)
            fs.collection("group_invites")
                .whereEqualTo("toUid", me.uid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val invites = snap.documents.mapNotNull { d ->
                            GroupInviteEntity(
                                inviteId = d.id,
                                groupId = d.getString("groupId") ?: return@mapNotNull null,
                                groupNameSnapshot = d.getString("groupNameSnapshot") ?: "Grupo",
                                fromUid = d.getString("fromUid") ?: "",
                                toUid = d.getString("toUid") ?: "",
                                status = d.getString("status") ?: "PENDING",
                                createdAt = d.getLong("createdAt") ?: now,
                                respondedAt = d.getLong("respondedAt")
                            )
                        }
                        groupDao.upsertInvites(invites)
                    }
                }
        }
    }

    override fun stopRemoteSync() {
        syncJob?.cancel()
        syncJob = null
    }

    override suspend fun createGroup(name: String): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("No hay sesión")
        val now = System.currentTimeMillis()
        val groupId = UUID.randomUUID().toString()

        val groupRef = fs.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(me.uid)
        val idxRef = fs.collection("users").document(me.uid).collection("groups").document(groupId)

        fs.runBatch { b ->
            b.set(groupRef, mapOf("name" to name, "createdAt" to now, "updatedAt" to now))
            b.set(memberRef, mapOf("uid" to me.uid, "joinedAt" to now))
            b.set(idxRef, mapOf("groupId" to groupId, "nameSnapshot" to name, "joinedAt" to now, "updatedAt" to now))
        }.await()

        // Cache local inmediata
        groupDao.upsertGroup(GroupEntity(groupId, name, now, now))
        groupDao.upsertMember(GroupMemberEntity(groupId, me.uid, now))
    }

    override suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String
    ): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("No hay sesión")
        val now = System.currentTimeMillis()
        val inviteId = UUID.randomUUID().toString()

        fs.collection("group_invites").document(inviteId)
            .set(
                mapOf(
                    "groupId" to groupId,
                    "groupNameSnapshot" to groupNameSnapshot,
                    "fromUid" to me.uid,
                    "toUid" to toUid,
                    "status" to "PENDING",
                    "createdAt" to now
                )
            ).await()

        // Local
        groupDao.upsertInvite(
            GroupInviteEntity(
                inviteId = inviteId,
                groupId = groupId,
                groupNameSnapshot = groupNameSnapshot,
                fromUid = me.uid,
                toUid = toUid,
                status = "PENDING",
                createdAt = now,
                respondedAt = null
            )
        )
    }

    override suspend fun acceptInvite(inviteId: String): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("No hay sesión")
        val now = System.currentTimeMillis()

        val inviteRef = fs.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) error("Invitación no existe")

        val toUid = snap.getString("toUid") ?: error("toUid missing")
        if (toUid != me.uid) error("No autorizado")

        val groupId = snap.getString("groupId") ?: error("groupId missing")
        val groupName = snap.getString("groupNameSnapshot") ?: "Grupo"

        val groupRef = fs.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(me.uid)
        val idxRef = fs.collection("users").document(me.uid).collection("groups").document(groupId)

        fs.runBatch { b ->
            b.update(inviteRef, mapOf("status" to "ACCEPTED", "respondedAt" to now))
            b.set(memberRef, mapOf("uid" to me.uid, "joinedAt" to now))
            b.set(idxRef, mapOf("groupId" to groupId, "nameSnapshot" to groupName, "joinedAt" to now, "updatedAt" to now))
            b.update(groupRef, mapOf("updatedAt" to now))
        }.await()

        // Local
        groupDao.upsertGroup(GroupEntity(groupId, groupName, now, now))
        groupDao.upsertMember(GroupMemberEntity(groupId, me.uid, now))
        groupDao.upsertInvite(
            GroupInviteEntity(
                inviteId = inviteId,
                groupId = groupId,
                groupNameSnapshot = groupName,
                fromUid = snap.getString("fromUid") ?: "",
                toUid = me.uid,
                status = "ACCEPTED",
                createdAt = snap.getLong("createdAt") ?: now,
                respondedAt = now
            )
        )
    }

    override suspend fun cancelInvite(inviteId: String): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("No hay sesión")
        val now = System.currentTimeMillis()

        val inviteRef = fs.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) return@runCatching

        val fromUid = snap.getString("fromUid") ?: ""
        val toUid = snap.getString("toUid") ?: ""
        if (me.uid != fromUid && me.uid != toUid) error("No autorizado")

        inviteRef.update(mapOf("status" to "CANCELLED", "respondedAt" to now)).await()

        groupDao.upsertInvite(
            GroupInviteEntity(
                inviteId = inviteId,
                groupId = snap.getString("groupId") ?: "",
                groupNameSnapshot = snap.getString("groupNameSnapshot") ?: "Grupo",
                fromUid = fromUid,
                toUid = toUid,
                status = "CANCELLED",
                createdAt = snap.getLong("createdAt") ?: now,
                respondedAt = now
            )
        )
    }

    override suspend fun rejectInvite(inviteId: String): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("No hay sesión")
        val inviteRef = fs.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) return@runCatching

        val toUid = snap.getString("toUid") ?: ""
        if (toUid != me.uid) error("No autorizado")

        // Rechazar = borrar (tu decisión)
        inviteRef.delete().await()
        groupDao.deleteInvite(inviteId)
    }

    override suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity> {
        return db.groupDao().pendingOutgoingInvitesForGroup(groupId)
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> = runCatching {
        val me = auth.currentUser ?: error("No hay sesión")
        val now = System.currentTimeMillis()

        val groupRef = fs.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(me.uid)
        val idxRef = fs.collection("users").document(me.uid).collection("groups").document(groupId)

        fs.runBatch { b ->
            b.delete(memberRef)
            b.delete(idxRef)
            b.update(groupRef, mapOf("updatedAt" to now))
        }.await()

        // Local
        groupDao.leaveGroupLocal(groupId, me.uid)

        // NOTA MVP:
        // El borrado del grupo cuando queda vacío lo remataremos luego
        // (memberCount o limpieza por backend).
    }
}