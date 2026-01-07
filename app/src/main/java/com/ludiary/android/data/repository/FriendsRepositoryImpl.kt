package com.ludiary.android.data.repository

import com.ludiary.android.data.local.dao.FriendDao
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import kotlinx.coroutines.flow.Flow

class FriendsRepositoryImpl(
    private val local: FriendDao
) : FriendsRepository {

    override fun observeFriends(query: String): Flow<List<FriendEntity>> {
        return if (query.isBlank()) local.observeFriends()
        else local.observeSearch(FriendStatus.ACCEPTED, query.trim())
    }

    override fun observeGroups(query: String): Flow<List<FriendEntity>> {
        return if (query.isBlank()) local.observeFriends()
        else local.observeSearch(FriendStatus.ACCEPTED, query.trim())
    }


    override fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> {
        return if (query.isBlank()) local.observeIncomingRequests()
        else local.observeSearch(FriendStatus.PENDING_INCOMING, query.trim())
    }

    override fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> {
        return if (query.isBlank()) local.observeOutgoingRequests()
        else local.observeSearch(FriendStatus.PENDING_OUTGOING, query.trim())
    }

    override suspend fun getById(id: Long): FriendEntity? = local.getById(id)

    override suspend fun updateNickname(id: Long, nickname: String?) {
        local.updateNickname(id, nickname?.trim()?.takeIf { it.isNotBlank() })
    }

    override suspend fun updateStatus(id: Long, status: FriendStatus) {
        local.updateStatus(id, status)
    }

    override suspend fun upsertAll(items: List<FriendEntity>) {
        local.upsertAll(items)
    }
}