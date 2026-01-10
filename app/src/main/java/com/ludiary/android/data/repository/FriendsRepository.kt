package com.ludiary.android.data.repository

import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    fun observeFriends(query: String): Flow<List<FriendEntity>>
    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>>
    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>>
    fun observeGroups(query: String): Flow<List<FriendEntity>>

    suspend fun sendInviteByCode(codeRaw: String): Result<Unit>

    suspend fun upsert(friend: FriendEntity)
    suspend fun setSyncStatus(id: Long, status: SyncStatus)

    suspend fun acceptRequest(friendId: Long): Result<Unit>
    suspend fun rejectRequest(friendId: Long): Result<Unit>

    suspend fun flushOfflineInvites(): Result<Unit>
}
