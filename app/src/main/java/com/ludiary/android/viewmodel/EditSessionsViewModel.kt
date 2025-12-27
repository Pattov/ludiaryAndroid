package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.GameRefType
import com.ludiary.android.data.model.PlayerRefType
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.launch
import java.util.UUID

class EditSessionsViewModel(
    private val db: LudiaryDatabase,
    private val auth: FirebaseAuth
) : ViewModel() {

    fun saveSession(
        sessionId: String?,
        gameTitle: String,
        playedAtMillis: Long,
        rating: Int?,
        notes: String?,
        players: List<PlayerDraft>
    ) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        val finalSessionId = sessionId ?: UUID.randomUUID().toString()

        val playersWithIds = players.mapIndexed { index, p ->
            val playerId = UUID.randomUUID().toString()
            Triple(playerId, index, p)
        }

        val session = SessionEntity(
            id = finalSessionId,
            ownerUserId = uid,
            scope = SessionScope.PERSONAL,
            groupId = null,

            gameTitle = gameTitle,
            gameRefType = GameRefType.SUGGESTION,
            gameRefId = "",

            playedAt = playedAtMillis,
            durationMinutes = null,
            location = null,

            overallRating = rating,
            notes = notes,

            createdAt = if (sessionId == null) now else now,
            updatedAt = now,

            isDeleted = false,
            deletedAt = null,

            syncStatus = SyncStatus.PENDING
        )

        val playerEntities = playersWithIds.map { (playerId, index, p) ->
            SessionPlayerEntity(
                sessionId = finalSessionId,
                playerId = playerId,

                refId = null,
                refType = PlayerRefType.LUDIARY_USER,

                displayName = p.name,
                score = p.score,
                isWinner = p.isWinner,
                sortOrder = index
            )
        }

        viewModelScope.launch {
            db.sessionDao().applyRemoteSessionReplacePlayers(session, playerEntities)
        }
    }

    fun loadSession(sessionId: String, onLoaded: (SessionWithPlayers) -> Unit) {
        viewModelScope.launch {
            val data = db.sessionDao().getSessionWithPlayers(sessionId)
            if (data != null) {
                onLoaded(data)
            }
        }
    }
}

/** Modelo simple del form (no entidad) */
data class PlayerDraft(
    val name: String,
    val score: Int?,
    val isWinner: Boolean
)