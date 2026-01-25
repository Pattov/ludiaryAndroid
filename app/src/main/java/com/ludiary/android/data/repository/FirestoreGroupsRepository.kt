package com.ludiary.android.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreGroupsRepository(
    private val firestore: FirebaseFirestore
) {
    data class RemoteUserGroupIndex(
        val groupId: String,
        val nameSnapshot: String,
        val joinedAt: Long,
        val updatedAt: Long
    )

    data class RemoteInvite(
        val inviteId: String,
        val groupId: String,
        val groupNameSnapshot: String,
        val fromUid: String,
        val toUid: String,
        val status: String,
        val createdAt: Long,
        val respondedAt: Long?
    )

    // ---------------------------
    // Observers (realtime)
    // ---------------------------

    fun observeUserGroupsIndex(uid: String): Flow<List<RemoteUserGroupIndex>> = callbackFlow {
        val reg = firestore.collection("users")
            .document(uid)
            .collection("groups")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                val items = snap?.documents?.mapNotNull { d ->
                    val groupId = d.id
                    val name = d.getString("nameSnapshot") ?: return@mapNotNull null
                    RemoteUserGroupIndex(
                        groupId = groupId,
                        nameSnapshot = name,
                        joinedAt = d.getLong("joinedAt") ?: now,
                        updatedAt = d.getLong("updatedAt") ?: now
                    )
                }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun observeIncomingPendingInvites(uid: String): Flow<List<RemoteInvite>> = callbackFlow {
        val reg = firestore.collection("group_invites")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                val items = snap?.documents?.mapNotNull { it.toRemoteInviteOrNull(now) }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    fun observeOutgoingPendingInvites(uid: String): Flow<List<RemoteInvite>> = callbackFlow {
        val reg = firestore.collection("group_invites")
            .whereEqualTo("fromUid", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                val items = snap?.documents?.mapNotNull { it.toRemoteInviteOrNull(now) }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    // ---------------------------
    // Commands
    // ---------------------------

    data class CreatedGroup(
        val groupId: String,
        val name: String,
        val now: Long
    )

    suspend fun createGroup(myUid: String, name: String): CreatedGroup {
        val now = System.currentTimeMillis()
        val groupId = UUID.randomUUID().toString()

        val groupRef = firestore.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(myUid)
        val idxRef = firestore.collection("users").document(myUid).collection("groups").document(groupId)

        firestore.runBatch { b ->
            b.set(groupRef, mapOf("name" to name, "createdAt" to now, "updatedAt" to now))
            b.set(memberRef, mapOf("uid" to myUid, "joinedAt" to now))
            b.set(
                idxRef,
                mapOf(
                    "groupId" to groupId,
                    "nameSnapshot" to name,
                    "joinedAt" to now,
                    "updatedAt" to now
                )
            )
        }.await()

        return CreatedGroup(groupId = groupId, name = name, now = now)
    }

    suspend fun createInvite(
        inviteId: String,
        groupId: String,
        groupNameSnapshot: String,
        fromUid: String,
        toUid: String,
        createdAt: Long
    ) {
        firestore.collection("group_invites").document(inviteId)
            .set(
                mapOf(
                    "groupId" to groupId,
                    "groupNameSnapshot" to groupNameSnapshot,
                    "fromUid" to fromUid,
                    "toUid" to toUid,
                    "status" to "PENDING",
                    "createdAt" to createdAt
                )
            )
            .await()
    }

    suspend fun inviteExists(inviteId: String): Boolean =
        firestore.collection("group_invites").document(inviteId).get().await().exists()

    data class InviteSnapshot(
        val inviteId: String,
        val groupId: String,
        val groupNameSnapshot: String,
        val fromUid: String,
        val toUid: String,
        val status: String,
        val createdAt: Long,
        val respondedAt: Long?
    )

    suspend fun getInvite(inviteId: String): InviteSnapshot? {
        val snap = firestore.collection("group_invites").document(inviteId).get().await()
        if (!snap.exists()) return null

        val now = System.currentTimeMillis()

        val groupId = snap.getString("groupId") ?: return null
        val groupName = snap.getString("groupNameSnapshot") ?: "Grupo"
        val fromUid = snap.getString("fromUid") ?: ""
        val toUid = snap.getString("toUid") ?: ""
        val status = snap.getString("status") ?: "PENDING"
        val createdAt = snap.getLong("createdAt") ?: now
        val respondedAt = snap.getLong("respondedAt")

        return InviteSnapshot(
            inviteId = inviteId,
            groupId = groupId,
            groupNameSnapshot = groupName,
            fromUid = fromUid,
            toUid = toUid,
            status = status,
            createdAt = createdAt,
            respondedAt = respondedAt
        )
    }

    suspend fun acceptInvite(myUid: String, inviteId: String): InviteSnapshot {
        val now = System.currentTimeMillis()

        val inviteRef = firestore.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) error("Invitación no existe")

        val toUid = snap.getString("toUid") ?: error("toUid missing")
        if (toUid != myUid) error("No autorizado")

        val groupId = snap.getString("groupId") ?: error("groupId missing")
        val groupName = snap.getString("groupNameSnapshot") ?: "Grupo"
        val fromUid = snap.getString("fromUid") ?: ""

        val groupRef = firestore.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(myUid)
        val idxRef = firestore.collection("users").document(myUid).collection("groups").document(groupId)

        firestore.runBatch { b ->
            b.update(inviteRef, mapOf("status" to "ACCEPTED", "respondedAt" to now))
            b.set(memberRef, mapOf("uid" to myUid, "joinedAt" to now))
            b.set(
                idxRef,
                mapOf(
                    "groupId" to groupId,
                    "nameSnapshot" to groupName,
                    "joinedAt" to now,
                    "updatedAt" to now
                )
            )
            b.update(groupRef, mapOf("updatedAt" to now))
        }.await()

        return InviteSnapshot(
            inviteId = inviteId,
            groupId = groupId,
            groupNameSnapshot = groupName,
            fromUid = fromUid,
            toUid = myUid,
            status = "ACCEPTED",
            createdAt = snap.getLong("createdAt") ?: now,
            respondedAt = now
        )
    }

    suspend fun cancelInvite(myUid: String, inviteId: String): InviteSnapshot? {
        val now = System.currentTimeMillis()

        val inviteRef = firestore.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) return null

        val fromUid = snap.getString("fromUid") ?: ""
        val toUid = snap.getString("toUid") ?: ""
        if (myUid != fromUid && myUid != toUid) error("No autorizado")

        inviteRef.update(mapOf("status" to "CANCELLED", "respondedAt" to now)).await()

        return InviteSnapshot(
            inviteId = inviteId,
            groupId = snap.getString("groupId") ?: "",
            groupNameSnapshot = snap.getString("groupNameSnapshot") ?: "Grupo",
            fromUid = fromUid,
            toUid = toUid,
            status = "CANCELLED",
            createdAt = snap.getLong("createdAt") ?: now,
            respondedAt = now
        )
    }

    suspend fun rejectInvite(myUid: String, inviteId: String) {
        val inviteRef = firestore.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) return

        val toUid = snap.getString("toUid") ?: ""
        if (toUid != myUid) error("No autorizado")

        // Rechazar = borrar (mantengo tu decisión actual)
        inviteRef.delete().await()
    }

    suspend fun leaveGroup(myUid: String, groupId: String) {
        val now = System.currentTimeMillis()

        val groupRef = firestore.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(myUid)
        val idxRef = firestore.collection("users").document(myUid).collection("groups").document(groupId)

        firestore.runBatch { b ->
            b.delete(memberRef)
            b.delete(idxRef)
            b.update(groupRef, mapOf("updatedAt" to now))
        }.await()

        // si se queda vacío, borramos
        val membersSnap = groupRef.collection("members").limit(1).get().await()
        if (membersSnap.isEmpty) {
            groupRef.delete().await()
        }
    }

    private fun DocumentSnapshot.toRemoteInviteOrNull(nowFallback: Long): RemoteInvite? {
        val groupId = getString("groupId") ?: return null
        return RemoteInvite(
            inviteId = id,
            groupId = groupId,
            groupNameSnapshot = getString("groupNameSnapshot") ?: "Grupo",
            fromUid = getString("fromUid") ?: "",
            toUid = getString("toUid") ?: "",
            status = getString("status") ?: "PENDING",
            createdAt = getLong("createdAt") ?: nowFallback,
            respondedAt = getLong("respondedAt")
        )
    }
}