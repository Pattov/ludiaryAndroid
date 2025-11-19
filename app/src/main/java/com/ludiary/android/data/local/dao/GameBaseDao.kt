package com.ludiary.android.data.local.dao

import androidx.room.*
import com.ludiary.android.data.local.entity.GameBaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameBaseDao {

    @Query("SELECT * FROM game_base ORDER BY title")
    fun getAll(): Flow<List<GameBaseEntity>>

    @Query("SELECT * FROM game_base WHERE id = :id")
    suspend fun getById(id: String): GameBaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameBaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: GameBaseEntity)

    @Query("DELETE FROM game_base")
    suspend fun clear()

}