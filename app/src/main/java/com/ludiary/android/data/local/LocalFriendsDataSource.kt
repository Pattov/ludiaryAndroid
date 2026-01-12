package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.FriendDao
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class LocalFriendsDataSource(
    private val dao: FriendDao
) {
    fun observeByStatus(status: FriendStatus, query: String): Flow<List<FriendEntity>> {
        val q = "%${query.trim()}%"
        return dao.observeByStatus(status, q)
    }

    // 👇 MVP: no hay “grupos” en esta tabla todavía
    fun observeGroups(query: String): Flow<List<FriendEntity>> = flowOf(emptyList())

    suspend fun upsert(friend: FriendEntity) = dao.upsert(friend)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long): FriendEntity? = dao.getById(id)

    suspend fun getByFriendUid(friendUid: String): FriendEntity? = dao.getByFriendUid(friendUid)

    suspend fun updateStatusAndUid(
        id: Long,
        status: FriendStatus,
        friendUid: String?,
        updatedAt: Long,
        syncStatus: SyncStatus
    ) = dao.updateStatusAndUid(id, status, friendUid, updatedAt, syncStatus)

    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus, updatedAt: Long) =
        dao.updateSyncStatus(id, syncStatus, updatedAt)

    suspend fun getPendingInvites(): List<FriendEntity> =
        dao.getByStatusAndSync(FriendStatus.PENDING_OUTGOING_LOCAL, SyncStatus.PENDING)
}