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

    @Query(" SELECT * FROM friends WHERE status = :status AND ( IFNULL(displayName, '') LIKE :query OR IFNULL(nickname, '') LIKE :query OR IFNULL(friendCode, '') LIKE :query ) ORDER BY CASE WHEN nickname IS NOT NULL AND nickname != '' THEN 0 ELSE 1 END, nickname COLLATE NOCASE, displayName COLLATE NOCASE")
    fun observeSearch(status: FriendStatus, query: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE status IN (:statuses) AND (nickname LIKE :query OR displayName LIKE :query OR friendCode LIKE :query) ORDER BY updatedAt DESC")
    fun observeSearchByStatuses(statuses: List<FriendStatus>, query: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE syncStatus = :syncStatus ORDER BY updatedAt DESC")
    fun observeBySyncStatus(syncStatus: SyncStatus): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE syncStatus = :syncStatus ORDER BY updatedAt DESC")
    suspend fun getBySyncStatus(syncStatus: SyncStatus): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE friendCode = :code LIMIT 1")
    suspend fun getByFriendCode(code: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity): Long

    @Query("UPDATE friends SET syncStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, updatedAt: Long)

    @Query("UPDATE friends SET status = :status, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateStatus(id: Long, status: FriendStatus, updatedAt: Long, syncStatus: SyncStatus)

    @Query("UPDATE friends SET status = :status, friendUid = :friendUid, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateStatusAndUid(id: Long, status: FriendStatus, friendUid: String?, updatedAt: Long, syncStatus: SyncStatus)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long)
}