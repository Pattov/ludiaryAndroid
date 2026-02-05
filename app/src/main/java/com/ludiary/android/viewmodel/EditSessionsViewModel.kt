package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.GameRefType
import com.ludiary.android.data.model.PlayerRefType
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.data.repository.auth.AuthRepository
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel para editar partidas.
 * @param db Instancia de Room.
 * @param authRepo Instancia de Firebase Auth.
 */
class EditSessionsViewModel(
    private val db: LudiaryDatabase,
    private val authRepo: AuthRepository
) : ViewModel() {

    /**
     * Crea o actualiza una partida.
     * @param sessionId Si es null, crea una nueva partida.
     * @param gameTitle Título del juego.
     * @param playedAtMillis Fecha y hora en milisegundos.
     * @param rating Valoración general.
     * @param notes Notas.
     * @param players Lista de jugadores.
     */
    fun saveSession(
        sessionId: String?,
        gameTitle: String,
        playedAtMillis: Long,
        rating: Int?,
        notes: String?,
        players: List<PlayerDraft>
    ) {
        val uid = authRepo.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        val finalSessionId = sessionId ?: UUID.randomUUID().toString()

        val sessionEntity = buildSessionEntity(
            sessionId = finalSessionId,
            uid = uid,
            gameTitle = gameTitle,
            playedAtMillis = playedAtMillis,
            rating = rating,
            notes = notes,
            now = now,
            isNew = sessionId == null
        )

        val playerEntities = buildPlayerEntities(
            sessionId = finalSessionId,
            players = players
        )

        viewModelScope.launch {
            // Guardado sesión + jugadores
            db.sessionDao().applyRemoteSessionReplacePlayers(sessionEntity, playerEntities)
        }
    }

    /**
     * Carga una partida.
     * @param sessionId ID de la partida.
     * @param onLoaded Callback que se llama cuando se carga la partida.
     */
    fun loadSession(sessionId: String, onLoaded: (SessionWithPlayers) -> Unit) {
        viewModelScope.launch {
            val data = db.sessionDao().getSessionWithPlayers(sessionId)
            if (data != null) {
                onLoaded(data)
            }
        }
    }

    /**
     * Construye la entidad de partida.
     * @param sessionId ID de la partida.
     * @param uid ID del usuario.
     * @param gameTitle Título del juego.
     * @param playedAtMillis Fecha y hora en milisegundos.
     * @param rating Valoración general.
     * @param notes Notas.
     * @param now Fecha y hora actual en milisegundos.
     * @param isNew Si es true, crea una nueva partida.
     */
    private fun buildSessionEntity(
        sessionId: String,
        uid: String,
        gameTitle: String,
        playedAtMillis: Long,
        rating: Int?,
        notes: String?,
        now: Long,
        isNew: Boolean
    ): SessionEntity {
        return SessionEntity(
            id = sessionId,
            ownerUserId = uid,
            scope = SessionScope.PERSONAL,
            groupId = null,

            // 2026 guardamos por título y marcamos como sugerencia.
            gameTitle = gameTitle,
            gameRefType = GameRefType.SUGGESTION,
            gameRefId = "",

            playedAt = playedAtMillis,
            durationMinutes = null,
            location = null,

            overallRating = rating,
            notes = notes,

            createdAt = if (isNew) now else now,
            updatedAt = now,

            isDeleted = false,
            deletedAt = null,

            syncStatus = SyncStatus.PENDING
        )
    }

    /**
     * Construye las entidades de jugadores.
     * @param sessionId ID de la partida.
     * @param players Lista de jugadores.
     */
    private fun buildPlayerEntities(
        sessionId: String,
        players: List<PlayerDraft>
    ): List<SessionPlayerEntity> {
        return players.mapIndexed { index, player ->
            SessionPlayerEntity(
                sessionId = sessionId,
                playerId = UUID.randomUUID().toString(),

                // 2026 aún no vinculamos a usuarios reales / contactos.
                refId = null,
                refType = PlayerRefType.NAME,

                displayName = player.name,
                score = player.score,
                isWinner = player.isWinner,
                sortOrder = index
            )
        }
    }
}

/**
 * Modelo simple del form (no entidad).
 * Se usa para transportar los datos del UI al ViewModel.
 * @param name Nombre del jugador.
 * @param score Puntuación del jugador.
 * @param isWinner Si el jugador ha ganado.
 */
data class PlayerDraft(
    val name: String,
    val score: Int?,
    val isWinner: Boolean
)