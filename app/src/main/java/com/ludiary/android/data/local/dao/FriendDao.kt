package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends WHERE status = :status AND ( IFNULL(friendCode,'') LIKE :query OR IFNULL(displayName,'') LIKE :query OR IFNULL(nickname,'') LIKE :query) ORDER BY updatedAt DESC")
    fun observeByStatus(status: FriendStatus, query: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE status IN (:statuses) AND ( IFNULL(friendCode,'') LIKE :query OR IFNULL(displayName,'') LIKE :query OR IFNULL(nickname,'') LIKE :query) ORDER BY updatedAt DESC")
    fun observeByStatuses(statuses: List<FriendStatus>, query: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE syncStatus = :syncStatus ORDER BY updatedAt DESC")
    fun observeBySyncStatus(syncStatus: SyncStatus): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE syncStatus = :syncStatus ORDER BY updatedAt DESC")
    suspend fun getBySyncStatus(syncStatus: SyncStatus): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FriendEntity?

    @Query("SELECT * FROM friends WHERE friendCode = :code LIMIT 1")
    suspend fun getByFriendCode(code: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE friendUid = :uid LIMIT 1")
    suspend fun getByFriendUid(uid: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity): Long

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE friends SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus, updatedAt: Long)

    @Query("UPDATE friends SET status = :status, friendUid = :friendUid, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatusAndUid(
        id: Long,
        status: FriendStatus,
        friendUid: String?,
        syncStatus: SyncStatus,
        updatedAt: Long
    )
}