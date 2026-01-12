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

    @Query("""
        SELECT * FROM friends
        WHERE status = :status
          AND (
            IFNULL(friendCode, '') LIKE :query OR
            IFNULL(displayName, '') LIKE :query OR
            IFNULL(nickname, '') LIKE :query
          )
        ORDER BY
          CASE WHEN nickname IS NOT NULL AND nickname != '' THEN 0 ELSE 1 END,
          nickname COLLATE NOCASE,
          displayName COLLATE NOCASE,
          friendCode COLLATE NOCASE
    """)
    fun observeByStatus(status: FriendStatus, query: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FriendEntity?

    @Query("SELECT * FROM friends WHERE friendUid = :friendUid LIMIT 1")
    suspend fun getByFriendUid(friendUid: String): FriendEntity?

    @Query("""
        SELECT * FROM friends
        WHERE status = :status AND syncStatus = :syncStatus
        ORDER BY updatedAt DESC
    """)
    suspend fun getByStatusAndSync(status: FriendStatus, syncStatus: SyncStatus): List<FriendEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        UPDATE friends
        SET syncStatus = :syncStatus, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus, updatedAt: Long)

    @Query("""
        UPDATE friends
        SET status = :status, friendUid = :friendUid, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun updateStatusAndUid(
        id: Long,
        status: FriendStatus,
        friendUid: String?,
        updatedAt: Long,
        syncStatus: SyncStatus
    )

    @Query("""
        UPDATE friends
        SET nickname = :nickname, updatedAt = :updatedAt, syncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun updateNickname(id: Long, nickname: String?, updatedAt: Long, syncStatus: SyncStatus)
}