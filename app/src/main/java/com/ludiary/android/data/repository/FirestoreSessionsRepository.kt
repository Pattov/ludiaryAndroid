package com.ludiary.android.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.GameRefType
import com.ludiary.android.data.model.PlayerRefType
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Repositorio de sesiones de Firebase.
 * @property db Instancia de Firestore.
 */
class FirestoreSessionsRepository(
    private val db: FirebaseFirestore
) {

    /**
     * Colección de sesiones en Firestore.
     * @property sessionsCol Referencia a la colección de sesiones.
     */
    private val sessionsCol = db.collection("sessions")

    /**
     * Actualiza una sesión en Firestore.
     * @param sw Sesión a actualizar.
     */
    suspend fun upsertSession(sw: SessionWithPlayers) {
        val s = sw.session

        val payload = FirestoreMapper.sessionWithPlayersToFirestore(sw).toMutableMap()
        payload["updatedAt"] = FieldValue.serverTimestamp()
        if (s.createdAt == null) payload["createdAt"] = FieldValue.serverTimestamp()

        sessionsCol.document(s.id).set(payload).await()
    }

    /**
     * Elimina una sesión en Firestore.
     * @param sessionId Identificador único de la sesión.
     */
    suspend fun softDeleteSession(sessionId: String) {
        val updates = mapOf(
            "isDeleted" to true,
            "deletedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        sessionsCol.document(sessionId)
            .set(updates, SetOptions.merge())
            .await()
    }

    /**
     * Obtiene las sesiones modificadas desde una fecha específica.
     * @param uid Identificador único del usuario.
     * @param sinceMillis Fecha desde la cual se obtuvieron las sesiones.
     * @return Lista de sesiones modificadas.
     */
    suspend fun fetchPersonalChangedSince(uid: String, sinceMillis: Long): List<RemoteAppliedSession> {
        val sinceTs = Timestamp(Date(sinceMillis))
        val snap = sessionsCol
            .whereEqualTo("scope", "personal")
            .whereEqualTo("ownerUserId", uid)
            .whereGreaterThan("updatedAt", sinceTs)
            .orderBy("updatedAt")
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            FirestoreMapper.docToRemoteApplied(doc.id, doc.data ?: return@mapNotNull null)
        }
    }

    /**
     * Obtiene las sesiones modificadas desde una fecha específica.
     * @param groupId Identificador único del grupo.
     * @param sinceMillis Fecha desde la cual se obtuvieron las sesiones.
     * @return Lista de sesiones modificadas.
     */
    suspend fun fetchGroupChangedSince(groupId: String, sinceMillis: Long): List<RemoteAppliedSession> {
        val sinceTs = Timestamp(Date(sinceMillis))
        val snap = sessionsCol
            .whereEqualTo("scope", "group")
            .whereEqualTo("groupId", groupId)
            .whereGreaterThan("updatedAt", sinceTs)
            .orderBy("updatedAt")
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            FirestoreMapper.docToRemoteApplied(doc.id, doc.data ?: return@mapNotNull null)
        }
    }

    /**
     * Clase que representa una sesión remota aplicada.
     * @property id Identificador único de la sesión.
     * @property isDeleted Indica si la sesión ha sido eliminada.
     * @property updatedAtMillis Fecha de actualización en milisegundos.
     * @property sessionEntity Entidad de la sesión.
     * @property playerEntities Lista de jugadores asociados a la sesión.
     */
    data class RemoteAppliedSession(
        val id: String,
        val isDeleted: Boolean,
        val updatedAtMillis: Long?,
        val sessionEntity: SessionEntity,
        val playerEntities: List<SessionPlayerEntity>
    )

    /**
     * Mapeo entre objetos de dominio y objetos de Firestore.
     */
    private object FirestoreMapper {

        /**
         * Convierte una sesión con jugadores a un mapa de datos de Firestore.
         * @param sw Sesión con jugadores.
         * @return Mapa de datos de Firestore.
         */
        fun sessionWithPlayersToFirestore(sw: SessionWithPlayers): Map<String, Any> {
            val s = sw.session

            val players = sw.players
                .sortedBy { it.sortOrder }
                .map { p ->
                    val m = mutableMapOf<String, Any>(
                        "id" to p.playerId,
                        "displayName" to p.displayName,
                        "sortOrder" to p.sortOrder,
                        "isWinner" to p.isWinner
                    )

                    p.score?.let { m["score"] = it }

                    if (p.refId != null) {
                        m["ref"] = mapOf(
                            "type" to when (p.refType) {
                                PlayerRefType.LUDIARY_USER -> "ludiaryUser"
                                PlayerRefType.GROUP_MEMBER -> "groupMember"
                                PlayerRefType.NAME -> "name"
                            },
                            "id" to p.refId
                        )
                    } else {
                        m["ref"] = mapOf(
                            "type" to "name"
                        )
                    }
                }

            val gameRef = mapOf(
                "type" to when (s.gameRefType) {
                    GameRefType.BASE -> "base"
                    GameRefType.USER -> "user"
                    GameRefType.SUGGESTION -> "suggestion"
                },
                "id" to s.gameRefId
            )

            val payload = mutableMapOf(
                "scope" to when (s.scope) {
                    SessionScope.PERSONAL -> "personal"
                    SessionScope.GROUP -> "group"
                },
                "gameRef" to gameRef,
                "gameTitle" to s.gameTitle,
                "playedAt" to Timestamp(Date(s.playedAt)),
                "players" to players,
                "isDeleted" to s.isDeleted
            )

            //solo se guardan si tienen valor real
            s.ownerUserId?.takeIf { it.isNotBlank() }?.let { payload["ownerUserId"] = it }
            s.groupId?.takeIf { it.isNotBlank() }?.let { payload["groupId"] = it }
            s.location?.takeIf { it.isNotBlank() }?.let { payload["location"] = it }
            s.durationMinutes?.let { payload["durationMinutes"] = it }
            s.overallRating?.let { payload["overallRating"] = it }
            s.notes?.takeIf { it.isNotBlank() }?.let { payload["notes"] = it }

            return payload
        }

        /**
         * Convierte un documento de Firestore a una sesión remota aplicada.
         * @param docId Identificador único del documento.
         * @param data Datos del documento.
         * @return Sesión remota aplicada
         */
        @Suppress("UNCHECKED_CAST")
        fun docToRemoteApplied(docId: String, data: Map<String, Any>): RemoteAppliedSession? {
            val isDeleted = data["isDeleted"] as? Boolean ?: false

            val updatedAtMillis = (data["updatedAt"] as? Timestamp)?.toDate()?.time

            val scopeStr = data["scope"] as? String ?: "personal"
            val scope = if (scopeStr == "group") SessionScope.GROUP else SessionScope.PERSONAL

            val ownerUserId = (data["ownerUserId"] as? String)?.takeIf { it.isNotBlank() }
            val groupId = (data["groupId"] as? String)?.takeIf { it.isNotBlank() }

            val gameRef = data["gameRef"] as? Map<String, Any> ?: return null
            val gameRefTypeStr = gameRef["type"] as? String ?: "base"
            val gameRefType = when (gameRefTypeStr) {
                "user" -> GameRefType.USER
                "suggestion" -> GameRefType.SUGGESTION
                else -> GameRefType.BASE
            }
            val gameRefId = gameRef["id"] as? String ?: return null

            val gameTitle = data["gameTitle"] as? String ?: ""
            val playedAtMillis = (data["playedAt"] as? Timestamp)?.toDate()?.time ?: return null

            val location = (data["location"] as? String)?.takeIf { it.isNotBlank() }
            val durationMinutes = (data["durationMinutes"] as? Number)?.toInt()
            val overallRating = (data["overallRating"] as? Number)?.toInt()?.takeIf { it in 1..10 }
            val notes = (data["notes"] as? String)?.takeIf { it.isNotBlank() }

            val createdAtMillis = (data["createdAt"] as? Timestamp)?.toDate()?.time
            val deletedAtMillis = (data["deletedAt"] as? Timestamp)?.toDate()?.time

            val sessionEntity = SessionEntity(
                id = docId,
                scope = scope,
                ownerUserId = ownerUserId,
                groupId = groupId,
                gameRefType = gameRefType,
                gameRefId = gameRefId,
                gameTitle = gameTitle,
                playedAt = playedAtMillis,
                location = location,
                durationMinutes = durationMinutes,
                overallRating = overallRating,
                notes = notes,
                syncStatus = SyncStatus.CLEAN,
                isDeleted = isDeleted,
                createdAt = createdAtMillis,
                updatedAt = updatedAtMillis,
                deletedAt = deletedAtMillis
            )

            val playersRaw = data["players"] as? List<Map<String, Any>> ?: emptyList()

            val playerEntities = playersRaw
                .sortedBy { (it["sortOrder"] as? Number)?.toInt() ?: 0 }
                .mapIndexed { idx, p ->
                    val pid = p["id"] as? String ?: return@mapIndexed null
                    val dn = p["displayName"] as? String ?: ""
                    val sortOrder = (p["sortOrder"] as? Number)?.toInt() ?: idx
                    val score = (p["score"] as? Number)?.toInt()

                    val ref = p["ref"] as? Map<String, Any>
                    val refType: PlayerRefType = when (ref?.get("type") as? String) {
                        "ludiaryUser" -> PlayerRefType.LUDIARY_USER
                        "groupMember" -> PlayerRefType.GROUP_MEMBER
                        "name" -> PlayerRefType.NAME
                        else -> PlayerRefType.NAME
                    }
                    val refId = ref?.get("id") as? String

                    val winnersLegacy = (data["winners"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                    val isWinnerFromPlayer = p["isWinner"] as? Boolean
                    val isWinner = isWinnerFromPlayer ?: ((pid in winnersLegacy) || (dn in winnersLegacy))

                    SessionPlayerEntity(
                        sessionId = docId,
                        playerId = pid,
                        displayName = dn,
                        refType = refType,
                        refId = refId,
                        score = score,
                        sortOrder = sortOrder,
                        isWinner = isWinner
                    )
                }
                .filterNotNull()

            return RemoteAppliedSession(
                id = docId,
                isDeleted = isDeleted,
                updatedAtMillis = updatedAtMillis,
                sessionEntity = sessionEntity,
                playerEntities = playerEntities
            )
        }
    }
}

// -------- await() helper --------
/**
 * Espera a que se complete una tarea de Firebase y devuelve el resultado.
 * @return El resultado de la tarea.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
