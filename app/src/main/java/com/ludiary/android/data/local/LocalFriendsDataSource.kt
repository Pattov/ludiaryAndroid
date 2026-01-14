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
    fun observeFriends(query: String): Flow<List<FriendEntity>> =
        dao.observeByStatus(FriendStatus.ACCEPTED.name, query)

    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        dao.observeByStatus(FriendStatus.PENDING_INCOMING.name, query)

    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        dao.observeByStatuses(
            listOf(FriendStatus.PENDING_OUTGOING.name, FriendStatus.PENDING_OUTGOING_LOCAL.name),
            query
        )

    // MVP: grupos aún no implementados
    fun observeGroups(query: String): Flow<List<FriendEntity>> = flowOf(emptyList())

    suspend fun getById(id: Long): FriendEntity? = dao.getById(id)

    suspend fun getByFriendUid(uid: String): FriendEntity? = dao.getByFriendUid(uid)

    suspend fun getPendingInvites(): List<FriendEntity> =
        dao.getPendingInvites(
            status = FriendStatus.PENDING_OUTGOING_LOCAL.name,
            syncStatus = SyncStatus.PENDING.name
        )

    suspend fun insert(entity: FriendEntity): Long = dao.insert(entity)

    suspend fun upsert(entity: FriendEntity) {
        // MVP simple: insert (ABORT si choca unique). Para actualizar, usa updateStatusAndUid por id.
        dao.insert(entity)
    }

    suspend fun updateStatusAndUid(
        id: Long,
        status: FriendStatus,
        friendUid: String?,
        syncStatus: SyncStatus
    ): Int = dao.updateStatusAndUid(
        id = id,
        status = status.name,
        friendUid = friendUid,
        syncStatus = syncStatus.name,
        updatedAt = System.currentTimeMillis()
    )

    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus): Int =
        dao.updateSyncStatus(id, syncStatus.name, System.currentTimeMillis())

    suspend fun deleteById(id: Long): Int = dao.deleteById(id)

    suspend fun upsertRemote(
        friendUid: String,
        displayName: String?,
        nickname: String?,
        status: FriendStatus,
        createdAt: Long?,
        updatedAt: Long?
    ) {
        dao.upsertByFriendUid(
            FriendEntity(
                id = 0L,
                friendUid = friendUid,
                friendCode = null,
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