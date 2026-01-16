package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.FriendDao
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class LocalFriendsDataSource(
    private val friendDao: FriendDao
) {
    fun observeFriends(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatus(FriendStatus.ACCEPTED.name, query)

    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatus(FriendStatus.PENDING_INCOMING.name, query)

    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatuses(
            listOf(FriendStatus.PENDING_OUTGOING.name, FriendStatus.PENDING_OUTGOING_LOCAL.name),
            query
        )

    // MVP: grupos aún no implementados
    fun observeGroups(query: String): Flow<List<FriendEntity>> = flowOf(emptyList())

    suspend fun getById(id: Long): FriendEntity? = friendDao.getById(id)

    suspend fun getByFriendUid(uid: String): FriendEntity? = friendDao.getByFriendUid(uid)

    suspend fun getPendingInvites(): List<FriendEntity> =
        friendDao.getPendingInvites(
            status = FriendStatus.PENDING_OUTGOING_LOCAL.name,
            syncStatus = SyncStatus.PENDING.name
        )

    suspend fun insert(entity: FriendEntity): Long = friendDao.insert(entity)

    suspend fun upsert(entity: FriendEntity) {
        // MVP simple: insert (ABORT si choca unique). Para actualizar, usa updateStatusAndUid por id.
        friendDao.insert(entity)
    }

    suspend fun updateStatusAndUid(
        id: Long,
        status: FriendStatus,
        friendUid: String?,
        syncStatus: SyncStatus
    ): Int = friendDao.updateStatusAndUid(
        id = id,
        status = status.name,
        friendUid = friendUid,
        syncStatus = syncStatus.name,
        updatedAt = System.currentTimeMillis()
    )

    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus): Int =
        friendDao.updateSyncStatus(id, syncStatus.name, System.currentTimeMillis())

    suspend fun deleteById(id: Long): Int = friendDao.deleteById(id)

    suspend fun clearAll() = friendDao.clearAll()

    suspend fun upsertRemote(
        friendUid: String,
        friendCode: String?,
        displayName: String?,
        nickname: String?,
        status: FriendStatus,
        createdAt: Long?,
        updatedAt: Long?
    ) {
        friendDao.upsertByFriendUid(
            FriendEntity(
                id = 0L,
                friendUid = friendUid,
                friendCode = friendCode,
                displayName = displayName,
                nickname = nickname,
                status = status,
                createdAt = createdAt ?: System.currentTimeMillis(),
                updatedAt = updatedAt ?: System.currentTimeMillis(),
                syncStatus = SyncStatus.CLEAN
            )
        )
    }
}