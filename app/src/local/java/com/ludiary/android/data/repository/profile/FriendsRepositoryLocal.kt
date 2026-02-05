package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.local.LocalFriendsDataSource

class FriendsRepositoryLocal(
    private val local: LocalFriendsDataSource
) : FriendsRepository {
    override fun observeFriends(query: String) = local.observeFriends(query)
    override fun observeGroups(query: String) = local.observeGroups(query)
    override fun observeIncomingRequests(query: String) = local.observeIncomingRequests(query)
    override fun observeOutgoingRequests(query: String) = local.observeOutgoingRequests(query)

    override fun startRemoteSync() { /* no-op */ }
    override fun stopRemoteSync() { /* no-op */ }

    override suspend fun sendInviteByCode(code: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no se pueden enviar invitaciones."))

    override suspend fun flushOfflineInvites(): Result<Unit> = Result.success(Unit)
    override suspend fun acceptRequest(friendId: Long): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible."))

    override suspend fun rejectRequest(friendId: Long): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible."))

    override suspend fun removeFriend(friendId: Long): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible."))

    override suspend fun updateNickname(friendId: Long, nickname: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible."))

    override suspend fun getMyFriendCode(): Result<String> =
        Result.failure(IllegalStateException("Modo local: no disponible."))
}