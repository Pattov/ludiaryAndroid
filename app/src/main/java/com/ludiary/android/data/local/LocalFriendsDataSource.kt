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
        friendDao.observeSearch(FriendStatus.ACCEPTED, "%${query.trim()}%")

    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeSearch(FriendStatus.PENDING_INCOMING, "%${query.trim()}%")

    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeSearch(FriendStatus.PENDING_OUTGOING, "%${query.trim()}%")

    fun observePendingToSync(): Flow<List<FriendEntity>> =
        friendDao.observeBySyncStatus(SyncStatus.PENDING)

    suspend fun getPendingToSync(): List<FriendEntity> =
        friendDao.getBySyncStatus(SyncStatus.PENDING)

    suspend fun upsert(friend: FriendEntity) {
        friendDao.upsert(friend)
    }

    suspend fun setSyncStatus(id: Long, status: SyncStatus) {
        friendDao.updateSyncStatus(id, status, System.currentTimeMillis())
    }

    suspend fun updateStatus(id: Long, status: FriendStatus, syncStatus: SyncStatus) {
        friendDao.updateStatus(id, status, System.currentTimeMillis(), syncStatus)
    }

    suspend fun updateStatusAndUid(id: Long, status: FriendStatus, friendUid: String?) {
        friendDao.updateStatusAndUid(id, status, friendUid, System.currentTimeMillis(), SyncStatus.CLEAN)
    }

    suspend fun getByFriendCode(code: String): FriendEntity? =
        friendDao.getByFriendCode(code)

    suspend fun deleteById(id: Long) {
        friendDao.deleteById(id)
    }
}
