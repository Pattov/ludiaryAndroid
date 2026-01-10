package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.FriendDao
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

class LocalFriendsDataSource(
    private val friendDao: FriendDao
) {
    fun observeFriends(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatus(
            status = FriendStatus.ACCEPTED,
            query = "%${query.trim()}%"
        )

    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatus(
            status = FriendStatus.PENDING_INCOMING,
            query = "%${query.trim()}%"
        )

    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatuses(
            statuses = listOf(
                FriendStatus.PENDING_OUTGOING,
                FriendStatus.PENDING_OUTGOING_LOCAL
            ),
            query = "%${query.trim()}%"
        )

    fun observePendingToSync(): Flow<List<FriendEntity>> =
        friendDao.observeBySyncStatus(SyncStatus.PENDING)

    suspend fun getPendingToSync(): List<FriendEntity> =
        friendDao.getBySyncStatus(SyncStatus.PENDING)

    suspend fun getById(id: Long): FriendEntity? =
        friendDao.getById(id)

    suspend fun getByFriendCode(code: String): FriendEntity? =
        friendDao.getByFriendCode(code)

    suspend fun getByFriendUid(uid: String): FriendEntity? =
        friendDao.getByFriendUid(uid)

    suspend fun upsert(entity: FriendEntity): Long =
        friendDao.upsert(entity)

    suspend fun updateStatusAndUid(id: Long, status: FriendStatus, friendUid: String?, syncStatus: SyncStatus) {
        friendDao.updateStatusAndUid(
            id = id,
            status = status,
            friendUid = friendUid,
            syncStatus = syncStatus,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun markClean(id: Long) {
        friendDao.updateSyncStatus(
            id = id,
            syncStatus = SyncStatus.CLEAN,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteById(id: Long) {
        friendDao.deleteById(id)
    }
}