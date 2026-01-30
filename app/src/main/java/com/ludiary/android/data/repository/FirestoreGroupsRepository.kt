package com.ludiary.android.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.model.CreatedGroup
import com.ludiary.android.data.model.InviteSnapshot
import com.ludiary.android.data.model.RemoteInvite
import com.ludiary.android.data.model.RemoteUserGroupIndex
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

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
     * Acepta una invitación a un grupo.
     * @param myUid UID del usuario que acepta.
     * @param inviteId ID de la invitación.
     * @return [InviteSnapshot] con el estado resultante.
     */
    suspend fun acceptInvite(myUid: String, inviteId: String): InviteSnapshot {
        val now = System.currentTimeMillis()

        val inviteRef = db.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) error(R.string.groups_error_invite_not_found)

        val toUid = snap.getString("toUid") ?: error("toUid missing")
        if (toUid != myUid) error(R.string.groups_error_not_authorized)

        val groupId = snap.getString("groupId") ?: error("groupId missing")
        val groupName = snap.getString("groupNameSnapshot") ?: "Grupo"
        val fromUid = snap.getString("fromUid") ?: ""

        val groupRef = db.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(myUid)
        val idxRef = db.collection("users").document(myUid).collection("groups").document(groupId)

        db.runBatch { b ->
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
            b.update(groupRef, mapOf("updatedAt" to now, "membersCount" to FieldValue.increment(1)))
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

    /**
     * Cancela una invitación.
     * @param myUid UID del usuario que cancela.
     * @param inviteId ID de la invitación.
     * @return [InviteSnapshot] si existía, o null si no existe.
     */
    suspend fun cancelInvite(myUid: String, inviteId: String): InviteSnapshot? {
        val now = System.currentTimeMillis()

        val inviteRef = db.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) return null

        val fromUid = snap.getString("fromUid") ?: ""
        val toUid = snap.getString("toUid") ?: ""
        if (myUid != fromUid && myUid != toUid) error(R.string.groups_error_not_authorized)

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

    /**
     * Crea un grupo y añade al usuario como primer miembro.
     * @param myUid UID del creador del grupo.
     * @param name Nombre del grupo.
     * @return [CreatedGroup] con el ID creado y el timestamp usado.
     */
    suspend fun createGroup(myUid: String, name: String): CreatedGroup {
        val now = System.currentTimeMillis()
        val groupId = UUID.randomUUID().toString()

        val groupRef = db.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(myUid)
        val idxRef = db.collection("users").document(myUid).collection("groups").document(groupId)

        db.runBatch { b ->
            b.set(groupRef, mapOf("name" to name, "createdAt" to now, "updatedAt" to now, "membersCount" to 1L))
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

    /**
     * Crea una invitación a un grupo en la colección `group_invites`.
     * @param inviteId ID del documento de invitación.
     * @param groupId ID del grupo.
     * @param groupNameSnapshot Nombre del grupo en el momento de invitar.
     * @param fromUid UID del emisor.
     * @param toUid UID del receptor.
     * @param createdAt Timestamp (epoch millis) de creación.
     */
    suspend fun createInvite(
        inviteId: String,
        groupId: String,
        groupNameSnapshot: String,
        fromUid: String,
        toUid: String,
        createdAt: Long
    ) {
        db.collection("group_invites").document(inviteId)
            .set(
                mapOf(
                    "groupId" to groupId,
                    "groupNameSnapshot" to groupNameSnapshot,
                    "fromUid" to fromUid,
                    "toUid" to toUid,
                    "status" to "PENDING",
                    "createdAt" to createdAt
                    // respondedAt se añade al aceptar/cancelar
                )
            )
            .await()
    }

    /**
     * Comprueba si existe una invitación por ID.
     * @param inviteId ID de la invitación.
     * @return true si existe el documento `group_invites/{inviteId}`.
     */
    suspend fun inviteExists(inviteId: String): Boolean =
        db.collection("group_invites").document(inviteId).get().await().exists()

    /**
     * Abandona un grupo.
     * @param myUid UID del usuario que abandona.
     * @param groupId ID del grupo.
     */
    suspend fun leaveGroup(myUid: String, groupId: String) {
        val now = System.currentTimeMillis()

        val groupRef = db.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(myUid)
        val idxRef = db.collection("users").document(myUid).collection("groups").document(groupId)

        db.runBatch { b ->
            b.delete(memberRef)
            b.delete(idxRef)
            b.update(groupRef, mapOf("updatedAt" to now, "membersCount" to FieldValue.increment(-1)))
        }.await()

        // si se queda vacío, borramos
        val membersSnap = groupRef.collection("members").limit(1).get().await()
        if (membersSnap.isEmpty) {
            groupRef.delete().await()
        }
    }

    /**
     * Rechaza una invitación entrante.
     * @param myUid UID del usuario receptor.
     * @param inviteId ID de la invitación.
     */
    suspend fun rejectInvite(myUid: String, inviteId: String) {
        val inviteRef = db.collection("group_invites").document(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) return

        val toUid = snap.getString("toUid") ?: ""
        if (toUid != myUid) error(R.string.groups_error_not_authorized)

        inviteRef.delete().await()
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