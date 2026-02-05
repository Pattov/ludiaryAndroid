package com.ludiary.android.data.repository.sessions

import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.model.Session

interface RemoteSessionsRepository {
    suspend fun upsertSession(session: Session)
    suspend fun upsertSession(sw: SessionWithPlayers)
    suspend fun softDeleteSession(sessionId: String)

    suspend fun fetchPersonalChangedSince(uid: String, sinceMillis: Long): List<RemoteAppliedSession>
    suspend fun fetchGroupChangedSince(groupId: String, sinceMillis: Long): List<RemoteAppliedSession>
}