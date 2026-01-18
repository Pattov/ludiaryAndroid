package com.ludiary.android.data.repository

import com.ludiary.android.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface FriendsRepository {
    fun observeFriends(query: String): Flow<List<FriendEntity>>
    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>>
    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>>
    fun observeGroups(query: String) = flowOf(emptyList<FriendEntity>())

    fun startRemoteSync()
    fun stopRemoteSync()

    suspend fun getMyFriendCode(): Result<String>
    suspend fun sendInviteByCode(code: String): Result<Unit>
    suspend fun acceptRequest(friendId: Long): Result<Unit>
    suspend fun rejectRequest(friendId: Long): Result<Unit>
    suspend fun flushOfflineInvites(): Result<Unit>
    suspend fun removeFriend(friendId: Long): Result<Unit>
}