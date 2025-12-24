package com.ludiary.android.data.local.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.data.local.SessionWithPlayers
import kotlinx.coroutines.flow.Flow

interface SessionDao {

    @Query("SELECT * FROM sessions WHERE scope = :scope AND ownerUserId = :uid AND isDeleted = 0 ORDER BY playedAt DESC")
    fun observePersonalSessions( uid: String, scope: SessionScope = SessionScope.PERSONAL): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE scope = :scope AND groupId = :groupId AND isDeleted = 0 ORDER BY playedAt DESC")
    fun observeGroupSessions( groupId: String, scope: SessionScope = SessionScope.GROUP ): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionWithPlayers(sessionId: String): Flow<SessionWithPlayers?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithPlayers(sessionId: String): SessionWithPlayers?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayers(players: List<SessionPlayerEntity>)

    @Query("DELETE FROM session_players WHERE sessionId = :sessionId")
    suspend fun deletePlayersBySession(sessionId: String)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun hardDeleteSession(sessionId: String)

    @Query("UPDATE sessions SET syncStatus = :status, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: String, status: SyncStatus, updatedAt: Long?)

    @Query("UPDATE sessions SET isDeleted = 1, syncStatus = :deletedStatus WHERE id = :sessionId")
    suspend fun markDeleted(sessionId: String, deletedStatus: SyncStatus = SyncStatus.DELETED)

    // ---------- Sync ----------
    @Query(" SELECT * FROM sessions WHERE syncStatus IN (:pending, :deleted) ")
    suspend fun getSessionsPendingPush(
        pending: SyncStatus = SyncStatus.PENDING,
        deleted: SyncStatus = SyncStatus.DELETED
    ): List<SessionEntity>

    @Transaction
    suspend fun applyRemoteSessionReplacePlayers( session: SessionEntity, players: List<SessionPlayerEntity> )
    {
        upsertSession(session)
        deletePlayersBySession(session.id)
        if (players.isNotEmpty()) { upsertPlayers(players) }
    }

    @Transaction
    suspend fun hardDeleteSessionCascade(sessionId: String) {
        hardDeleteSession(sessionId)
    }
}