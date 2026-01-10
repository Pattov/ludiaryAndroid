package com.ludiary.android.data.local

import FriendDao
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

class LocalFriendsDataSource(
    private val friendDao: FriendDao
) {
    fun observeFriends(query: String): Flow<List<FriendEntity>> =
        friendDao.observeSearchByStatuses(
            statuses = listOf(
                FriendStatus.ACCEPTED,
                FriendStatus.PENDING_OUTGOING,
                FriendStatus.PENDING_OUTGOING_LOCAL
            ),
            query = "%${query.trim()}%"
        )

    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeSearch(
            status = FriendStatus.PENDING_INCOMING,
            query = "%${query.trim()}%"
        )

    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeSearch(
            status = FriendStatus.PENDING_OUTGOING,
            query = "%${query.trim()}%"
        )

    fun observePendingToSync(): Flow<List<FriendEntity>> =
        friendDao.observeBySyncStatus(SyncStatus.PENDING)

    fun observeFriendsAndOutgoing(query: String): Flow<List<FriendEntity>> =
        friendDao.observeSearchByStatuses(
            statuses = listOf(
                FriendStatus.ACCEPTED,
                FriendStatus.PENDING_OUTGOING,
                FriendStatus.PENDING_OUTGOING_LOCAL
            ),
            query = "%${query.trim()}%"
        )

    suspend fun getPendingToSync(): List<FriendEntity> =
        friendDao.getBySyncStatus(SyncStatus.PENDING)

    suspend fun getById(id: Long): FriendEntity? =
        friendDao.getById(id)

    suspend fun getByFriendCode(code: String): FriendEntity? =
        friendDao.getByFriendCode(code)

    suspend fun upsert(entity: FriendEntity): Long =
        friendDao.upsert(entity)

    suspend fun updateStatusAndUid(id: Long, status: FriendStatus, friendUid: String?, syncStatus: SyncStatus) {
        friendDao.updateStatusAndUid(
            id = id,
            status = status,
            friendUid = friendUid,
            updatedAt = System.currentTimeMillis(),
            syncStatus = syncStatus
        )
    }

    suspend fun updateStatus(id: Long, status: FriendStatus, syncStatus: SyncStatus) {
        friendDao.updateStatus(
            id = id,
            status = status,
            updatedAt = System.currentTimeMillis(),
            syncStatus = syncStatus
        )
    }

    suspend fun markClean(id: Long) {
        friendDao.updateSyncStatus(
            id = id,
            status = SyncStatus.CLEAN,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteById(id: Long) {
        friendDao.deleteById(id)
    }
}