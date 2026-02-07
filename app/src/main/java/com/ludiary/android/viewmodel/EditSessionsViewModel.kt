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
        durationMinutes: Int?,
        rating: Int?,
        notes: String?,
        players: List<PlayerDraft>
    ) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        val finalSessionId = sessionId ?: UUID.randomUUID().toString()

        val sessionEntity = buildSessionEntity(
            sessionId = finalSessionId,
            uid = uid,
            gameTitle = gameTitle,
            playedAtMillis = playedAtMillis,
            durationMinutes = durationMinutes,
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
            db.sessionDao().applyRemoteSessionReplacePlayers(sessionEntity, playerEntities)
        }
    }

    fun loadSession(sessionId: String, onLoaded: (SessionWithPlayers) -> Unit) {
        viewModelScope.launch {
            val data = db.sessionDao().getSessionWithPlayers(sessionId)
            if (data != null) onLoaded(data)
        }
    }

    suspend fun getCurrentUserDisplay(): String? {
        val local = db.userDao().getLocalUser()
        if (!local?.displayName.isNullOrBlank()) {
            return local.displayName
        }
        return auth.currentUser?.displayName
    }

    fun getCurrentUid(): String {
        return auth.currentUser?.uid.orEmpty()
    }


    private fun buildSessionEntity(
        sessionId: String,
        uid: String,
        gameTitle: String,
        playedAtMillis: Long,
        durationMinutes: Int?,
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

            gameTitle = gameTitle,
            gameRefType = GameRefType.SUGGESTION,
            gameRefId = "",

            playedAt = playedAtMillis,
            durationMinutes = durationMinutes,
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

    private fun buildPlayerEntities(
        sessionId: String,
        players: List<PlayerDraft>
    ): List<SessionPlayerEntity> {
        return players.mapIndexed { index, player ->
            SessionPlayerEntity(
                sessionId = sessionId,
                playerId = UUID.randomUUID().toString(),

                displayName = player.name,

                refType = player.refType,
                refId = player.refId,

                score = player.score,
                isWinner = player.isWinner,
                sortOrder = index
            )
        }
    }
}

data class PlayerDraft(
    val name: String,
    val score: Int?,
    val isWinner: Boolean,
    val refType: PlayerRefType = PlayerRefType.NAME,
    val refId: String? = null
)