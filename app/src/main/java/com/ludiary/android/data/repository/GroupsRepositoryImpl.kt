package com.ludiary.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupsRepositoryImpl(
    private val local: LocalGroupsDataSource,
    private val remote: FirestoreGroupsRepository,
    private val auth: FirebaseAuth
) : GroupsRepository {

    private var remoteSyncJob: Job? = null
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val groupDocJobs = mutableMapOf<String, Job>()

    override fun observeGroups(query: String) = local.observeGroups(query)
    override fun observeMembers(groupId: String) = local.observeMembers(groupId)

    override fun observePendingInvites() =
        local.observePendingInvites(auth.currentUser?.uid)

    override fun observeOutgoingPendingInvites() =
        local.observeOutgoingPendingInvites(auth.currentUser?.uid)

    /**
     * Realtime Firestore -> Room
     * Firebase manda: lo remoto pisa lo local (como en Friends).
     */
    override fun startRemoteSync() {
        val me = auth.currentUser ?: return

        Log.d("LUDIARY_GROUPS_DEBUG", "startRemoteSync uid=${me.uid}")

        // evitar duplicados
        remoteSyncJob?.cancel()

        remoteSyncJob = repoScope.launch {
            // 1) Índice users/{uid}/groups
            launch {
                remote.observeUserGroupsIndex(me.uid).collect { remoteGroups ->

                    // 1) upsert básico (nombre, fechas)
                    val items = remoteGroups.map {
                        GroupEntity(
                            groupId = it.groupId,
                            nameSnapshot = it.nameSnapshot,
                            createdAt = it.joinedAt,
                            updatedAt = it.updatedAt
                        )
                    }
                    local.upsertGroups(items)

                    // 2) 👇 AQUÍ VA EL CÓDIGO QUE PREGUNTABAS
                    val groupIds = remoteGroups.map { it.groupId }.toSet()

                    // parar jobs de grupos que ya no están
                    (groupDocJobs.keys - groupIds).forEach { gid ->
                        groupDocJobs.remove(gid)?.cancel()
                    }

                    // arrancar jobs nuevos
                    (groupIds - groupDocJobs.keys).forEach { gid ->
                        groupDocJobs[gid] = repoScope.launch {
                            remote.observeGroupDoc(gid).collect { (count, updatedAt) ->

                                val meta = remoteGroups.firstOrNull { it.groupId == gid }

                                local.upsertGroup(
                                    GroupEntity(
                                        groupId = gid,
                                        nameSnapshot = meta?.nameSnapshot ?: "Grupo",
                                        createdAt = meta?.joinedAt ?: System.currentTimeMillis(),
                                        updatedAt = updatedAt,
                                        membersCount = count
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2) Invitaciones RECIBIDAS (pending)
            launch {
                remote.observeIncomingPendingInvites(me.uid).collect { remoteInvites ->
                    val items = remoteInvites.map {
                        GroupInviteEntity(
                            inviteId = it.inviteId,
                            groupId = it.groupId,
                            groupNameSnapshot = it.groupNameSnapshot,
                            fromUid = it.fromUid,
                            toUid = it.toUid,
                            status = it.status,
                            createdAt = it.createdAt,
                            respondedAt = it.respondedAt
                        )
                    }

                    local.upsertInvites(items)

                    // limpieza incoming
                    val remoteIds = items.map { it.inviteId }
                    if (remoteIds.isEmpty()) {
                        local.deleteAllIncomingPendingInvites(me.uid)
                    } else {
                        local.deleteMissingIncomingInvites(me.uid, remoteIds)
                    }

                    Log.d("LUDIARY_GROUPS_DEBUG", "incoming pending=${items.size}")
                }
            }

            // 3) Invitaciones ENVIADAS por mí (pending)
            launch {
                remote.observeOutgoingPendingInvites(me.uid).collect { remoteInvites ->
                    val items = remoteInvites.map {
                        GroupInviteEntity(
                            inviteId = it.inviteId,
                            groupId = it.groupId,
                            groupNameSnapshot = it.groupNameSnapshot,
                            fromUid = it.fromUid,
                            toUid = it.toUid,
                            status = it.status,
                            createdAt = it.createdAt,
                            respondedAt = it.respondedAt
                        )
                    }

                    local.upsertInvites(items)

                    // limpieza outgoing
                    val remoteIds = items.map { it.inviteId }
                    if (remoteIds.isEmpty()) {
                        local.deleteAllOutgoingPendingInvites(me.uid)
                    } else {
                        local.deleteMissingOutgoingInvites(me.uid, remoteIds)
                    }

                    Log.d("LUDIARY_GROUPS_DEBUG", "outgoing pending=${items.size}")
                }
            }
        }
    }

    override fun stopRemoteSync() {
        remoteSyncJob?.cancel()
        remoteSyncJob = null
        groupDocJobs.values.forEach { it.cancel() }
        groupDocJobs.clear()
    }

    override suspend fun createGroup(name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error("No hay sesión")

                val created = remote.createGroup(me.uid, name)

                // cache local inmediata
                local.upsertGroup(
                    GroupEntity(
                        groupId = created.groupId,
                        nameSnapshot = created.name,
                        createdAt = created.now,
                        updatedAt = created.now
                    )
                )
                local.upsertMember(GroupMemberEntity(created.groupId, me.uid, created.now))
            }
        }

    override suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            val now = System.currentTimeMillis()
            val inviteId = "${groupId}_${toUid}"

            // 1) Local first
            local.upsertInvite(
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

            // 2) Remoto
            try {
                remote.createInvite(
                    inviteId = inviteId,
                    groupId = groupId,
                    groupNameSnapshot = groupNameSnapshot,
                    fromUid = me.uid,
                    toUid = toUid,
                    createdAt = now
                )
            } catch (e: Exception) {
                Log.w("LUDIARY_GROUPS_DEBUG", "inviteToGroup pending/offline inviteId=$inviteId", e)
                throw e
            }
        }
    }

    override suspend fun flushPendingInvites(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: return@runCatching
                val pending = local.pendingOutgoingInvitesAll(me.uid)

                pending.forEach { inv ->
                    val exists = try {
                        remote.inviteExists(inv.inviteId)
                    } catch (_: Exception) {
                        false
                    }

                    if (!exists) {
                        remote.createInvite(
                            inviteId = inv.inviteId,
                            groupId = inv.groupId,
                            groupNameSnapshot = inv.groupNameSnapshot,
                            fromUid = inv.fromUid,
                            toUid = inv.toUid,
                            createdAt = inv.createdAt
                        )
                    }
                }
            }
        }

    override suspend fun acceptInvite(inviteId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error("No hay sesión")

                val accepted = remote.acceptInvite(me.uid, inviteId)

                // local
                val now = accepted.respondedAt ?: System.currentTimeMillis()
                local.upsertGroup(GroupEntity(accepted.groupId, accepted.groupNameSnapshot, now, now))
                local.upsertMember(GroupMemberEntity(accepted.groupId, me.uid, now))
                local.upsertInvite(
                    GroupInviteEntity(
                        inviteId = accepted.inviteId,
                        groupId = accepted.groupId,
                        groupNameSnapshot = accepted.groupNameSnapshot,
                        fromUid = accepted.fromUid,
                        toUid = accepted.toUid,
                        status = accepted.status,
                        createdAt = accepted.createdAt,
                        respondedAt = accepted.respondedAt
                    )
                )
            }
        }

    override suspend fun cancelInvite(inviteId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error("No hay sesión")

                val cancelled = remote.cancelInvite(me.uid, inviteId) ?: return@runCatching

                local.upsertInvite(
                    GroupInviteEntity(
                        inviteId = cancelled.inviteId,
                        groupId = cancelled.groupId,
                        groupNameSnapshot = cancelled.groupNameSnapshot,
                        fromUid = cancelled.fromUid,
                        toUid = cancelled.toUid,
                        status = cancelled.status,
                        createdAt = cancelled.createdAt,
                        respondedAt = cancelled.respondedAt
                    )
                )
            }
        }

    override suspend fun rejectInvite(inviteId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error("No hay sesión")
                remote.rejectInvite(me.uid, inviteId)
                local.deleteInvite(inviteId)
            }
        }

    override suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity> =
        withContext(Dispatchers.IO) {
            val me = auth.currentUser ?: return@withContext emptyList()
            local.pendingOutgoingInvitesForGroup(groupId, me.uid)
        }

    override suspend fun leaveGroup(groupId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error("No hay sesión")

                // remoto (incluye borrado si queda vacío)
                try {
                    remote.leaveGroup(me.uid, groupId)
                } catch (e: Exception) {
                    Log.w("LUDIARY_GROUPS_DEBUG", "leaveGroup remote failed groupId=$groupId", e)
                    throw e
                }

                // local
                local.leaveGroupLocal(groupId, me.uid)
            }
        }
}