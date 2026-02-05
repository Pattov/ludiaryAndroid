package com.ludiary.android.data.repository.sessions

import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.model.Session

class LocalRemoteSessionsRepository : RemoteSessionsRepository {
    override suspend fun upsertSession(session: Session) { /* no-op */ }
    override suspend fun upsertSession(sw: SessionWithPlayers) { /* no-op */ }
    override suspend fun softDeleteSession(sessionId: String) { /* no-op */ }

    override suspend fun fetchPersonalChangedSince(uid: String, sinceMillis: Long): List<RemoteAppliedSession> = emptyList()
    override suspend fun fetchGroupChangedSince(groupId: String, sinceMillis: Long): List<RemoteAppliedSession> = emptyList()
}
