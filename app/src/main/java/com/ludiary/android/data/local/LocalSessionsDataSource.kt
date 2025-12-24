package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.SessionDao
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.SyncStatus

class LocalSessionsDataSource(
    private val sessionDao: SessionDao
) {
    suspend fun getPendingForPush(): List<SessionEntity> = sessionDao.getSessionsPendingPush()

    suspend fun getWithPlayers (sessionId: String): SessionWithPlayers? = sessionDao.getSessionWithPlayers(sessionId)

    suspend fun markClean(sessionId: String, updatedAtMillis: Long?) {
        sessionDao.updateSyncStatus(sessionId, SyncStatus.CLEAN, updatedAtMillis)
    }

    suspend fun hardDeleteCascade(sessionId: String) {
        sessionDao.hardDeleteSessionCascade(sessionId)
    }

    suspend fun applyRemoteReplacePlayers(
        session: SessionEntity,
        players: List<SessionPlayerEntity>
    ) {
        sessionDao.applyRemoteSessionReplacePlayers(session, players)
    }

    suspend fun adoptOfflinePersonalSessionsForUser(uid: String): Int {
        return sessionDao.adoptOfflinePersonalSessions(uid)
    }
}