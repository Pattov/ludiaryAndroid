package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.model.CreateGroupResult
import com.ludiary.android.data.model.GroupInviteResult

interface FunctionsSocialRepository {

    data class FriendInviteResult(
        val friendUid: String?,
        val friendCode: String?,
        val displayName: String?
    )

    suspend fun sendFriendInviteByCode(code: String, clientCreatedAt: Long): FriendInviteResult
    suspend fun acceptFriend(friendUid: String)
    suspend fun rejectFriend(friendUid: String)
    suspend fun removeFriend(friendUid: String)
    suspend fun updateFriendNickname(friendUid: String, nickname: String?)

    suspend fun acceptGroupInvite(inviteId: String)
    suspend fun cancelGroupInvite(inviteId: String)
    suspend fun createGroup(name: String): CreateGroupResult
    suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String,
        clientCreatedAt: Long
    ): GroupInviteResult
    suspend fun rejectGroupInvite(inviteId: String)
    suspend fun leaveGroup(groupId: String)
}