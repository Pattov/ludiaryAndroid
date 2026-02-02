package com.ludiary.android.data.repository.profile

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.RemoteInvite
import com.ludiary.android.data.model.RemoteUserGroupIndex
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repositorio remoto para la gestión de grupos y sus invitaciones en Firestore.
 * @param db Instancia de [FirebaseFirestore].
 */
class FirestoreGroupsRepository(
    private val db: FirebaseFirestore
) {

    /**
     * Observa en tiempo real los metadatos principales de un grupo.
     * @param groupId Identificador del grupo.
     * @return Flow que emite un par
     *      first: número de miembros del grupo
     *      second: timestamp (epoch millis) de la última actualización
     */
    fun observeGroupDoc(groupId: String): Flow<Pair<Int, Long>> = callbackFlow {
        val reg = db.collection("groups")
            .document(groupId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                if (snap == null || !snap.exists()) return@addSnapshotListener

                val count = snap.getLong("membersCount")?.toInt() ?: 1
                val updatedAt = snap.getLong("updatedAt") ?: System.currentTimeMillis()
                trySend(count to updatedAt)
            }

        awaitClose { reg.remove() }
    }

    /**
     * Observa invitaciones entrantes en estado PENDING para un usuario.
     * @param uid UID del usuario.
     * @return Flow con la lista de invitaciones entrantes pendientes.
     */
    fun observeIncomingPendingInvites(uid: String): Flow<List<RemoteInvite>> = callbackFlow {
        val reg = db.collection("group_invites")
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

    /**
     * Observa invitaciones salientes en estado PENDING para un usuario.
     * @param uid UID del usuario.
     * @return Flow con la lista de invitaciones salientes pendientes.
     */
    fun observeOutgoingPendingInvites(uid: String): Flow<List<RemoteInvite>> = callbackFlow {
        val reg = db.collection("group_invites")
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

    /**
     * Observa en tiempo real el índice de grupos del usuario.
     * @param uid UID del usuario.
     * @return Flow con la lista de [RemoteUserGroupIndex].
     */
    fun observeUserGroupsIndex(uid: String): Flow<List<RemoteUserGroupIndex>> = callbackFlow {
        val reg = db.collection("users")
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

    /**
     * Convierte un [DocumentSnapshot] a [RemoteInvite] si contiene los campos mínimos.
     * @param nowFallback Timestamp usado como fallback cuando faltan campos.
     * @return RemoteInvite o null si falta `groupId`.
     */
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