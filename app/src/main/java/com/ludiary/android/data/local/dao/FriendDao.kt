package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends WHERE status = :status ORDER BY CASE WHEN nickname IS NOT NULL AND TRIM(nickname) != '' THEN 0 ELSE 1 END, COALESCE(NULLIF(TRIM(nickname), ''), NULLIF(TRIM(displayName), ''), email) COLLATE NOCASE")
    fun observeByStatus(status: FriendStatus): Flow<List<FriendEntity>>

    fun observeFriends(): Flow<List<FriendEntity>> =
        observeByStatus(FriendStatus.ACCEPTED)

    fun observeIncomingRequests(): Flow<List<FriendEntity>> =
        observeByStatus(FriendStatus.PENDING_INCOMING)

    fun observeOutgoingRequests(): Flow<List<FriendEntity>> =
        observeByStatus(FriendStatus.PENDING_OUTGOING)

    @Query("SELECT * FROM friends WHERE status = :status AND (email LIKE '%' || :q || '%' OR displayName LIKE '%' || :q || '%' OR nickname LIKE '%' || :q || '%' ) ORDER BY CASE WHEN nickname IS NOT NULL AND TRIM(nickname) != '' THEN 0 ELSE 1 END, COALESCE(NULLIF(TRIM(nickname), ''), NULLIF(TRIM(displayName), ''), email) COLLATE NOCASE")
    fun observeSearch(status: FriendStatus, q: String): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FriendEntity>)

    @Update
    suspend fun update(friend: FriendEntity)

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FriendEntity?

    @Query("SELECT * FROM friends WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): FriendEntity?

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE friends SET nickname = :nickname, updatedAt = :now WHERE id = :id")
    suspend fun updateNickname(id: Long, nickname: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE friends SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: FriendStatus, now: Long = System.currentTimeMillis())
}