package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.model.CreateGroupResult
import com.ludiary.android.data.model.GroupInviteResult

class FunctionsSocialRepositoryLocal : FunctionsSocialRepository {

    override suspend fun sendFriendInviteByCode(code: String, clientCreatedAt: Long) =
        FunctionsSocialRepository.FriendInviteResult(null, null, null)

    override suspend fun acceptFriend(friendUid: String) {}
    override suspend fun rejectFriend(friendUid: String) {}
    override suspend fun removeFriend(friendUid: String) {}
    override suspend fun updateFriendNickname(friendUid: String, nickname: String?) {}

    override suspend fun acceptGroupInvite(inviteId: String) {}
    override suspend fun cancelGroupInvite(inviteId: String) {}

    override suspend fun createGroup(name: String): CreateGroupResult {
        throw IllegalStateException("Función no disponible en modo local")
    }

    override suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String,
        clientCreatedAt: Long
    ): GroupInviteResult {
        throw IllegalStateException("Función no disponible en modo local")
    }

    override suspend fun rejectGroupInvite(inviteId: String) {}
    override suspend fun leaveGroup(groupId: String) {}
}