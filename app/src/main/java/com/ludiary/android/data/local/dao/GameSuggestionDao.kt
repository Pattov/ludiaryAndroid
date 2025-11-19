package com.ludiary.android.data.local.dao

import androidx.room.*
import com.ludiary.android.data.local.entity.GameSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSuggestionDao {

    @Query("SELECT * FROM game_suggestions WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun getUserSuggestions(userId: String): Flow<List<GameSuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(suggestion: GameSuggestionEntity)

    @Delete
    suspend fun delete(suggestion: GameSuggestionEntity)

}