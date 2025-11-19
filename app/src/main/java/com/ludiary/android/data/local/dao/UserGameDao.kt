package com.ludiary.android.data.local.dao

import androidx.room.*
import com.ludiary.android.data.local.entity.UserGameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGameDao {

    @Query("SELECT * FROM user_games WHERE userId = :userId ORDER BY titleSnapshot")
    fun getUserGames(userId: String): Flow<List<UserGameEntity>>

    @Query("SELECT * FROM user_games WHERE id = :id")
    suspend fun getById(id: String): UserGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: UserGameEntity)

    @Delete
    suspend fun delete(game: UserGameEntity)

}