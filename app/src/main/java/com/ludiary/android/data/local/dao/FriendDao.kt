package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends WHERE status = :status AND (displayName LIKE '%' || :q || '%' OR nickname LIKE '%' || :q || '%' OR friendCode LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun observeByStatus(status: String, q: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE status IN (:statuses) AND (displayName LIKE '%' || :q || '%' OR nickname LIKE '%' || :q || '%' OR friendCode LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun observeByStatuses(statuses: List<String>, q: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FriendEntity?

    @Query("SELECT * FROM friends WHERE friendUid = :uid LIMIT 1")
    suspend fun getByFriendUid(uid: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE status = :status AND syncStatus = :syncStatus ORDER BY updatedAt ASC")
    suspend fun getPendingInvites(status: String, syncStatus: String): List<FriendEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FriendEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: FriendEntity): Long

    @Query("UPDATE friends SET friendCode = :friendCode, displayName = :displayName, nickname = :nickname, status = :status, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE friendUid = :friendUid")
    suspend fun updateByFriendUid(
        friendUid: String,
        friendCode: String?,
        displayName: String?,
        nickname: String?,
        status: FriendStatus,
        syncStatus: SyncStatus,
        updatedAt: Long
    ): Int

    // --- tu update actual por id, etc. ---
    @Query("UPDATE friends SET status = :status, friendUid = :friendUid, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatusAndUid(
        id: Long,
        status: String,
        friendUid: String?,
        syncStatus: String,
        updatedAt: Long
    ): Int

    @Query("UPDATE friends SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String, updatedAt: Long): Int

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Transaction
    suspend fun upsertByFriendUid(entity: FriendEntity): Long {
        val uid = entity.friendUid

        // Si aún no hay uid, no puede haber conflicto por friendUid: inserta normal
        if (uid.isNullOrBlank()) {
            return insertIgnore(entity)
        }

        val rowId = insertIgnore(entity)
        if (rowId != -1L) return rowId

        // Conflicto por UNIQUE(friendUid): actualiza el existente
        updateByFriendUid(
            friendUid = uid,
            friendCode = entity.friendCode,
            displayName = entity.displayName,
            nickname = entity.nickname,
            status = entity.status,
            syncStatus = entity.syncStatus,
            updatedAt = System.currentTimeMillis()
        )

        return getByFriendUid(uid)?.id ?: -1L
    }
}