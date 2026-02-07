package com.ludiary.android.data.local.dao

import androidx.room.*
import com.ludiary.android.data.local.entity.GameSuggestionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos relacionadas con juegos sugeridos por el usuario.
 */
@Dao
interface GameSuggestionDao {

    /**
     * Obtiene todas las sugerencias de juegos del usuario.
     *
     * @return [Flow] que emite una lista de sugerencias.
     */
    @Query("SELECT * FROM game_suggestions WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun getUserSuggestions(userId: String): Flow<List<GameSuggestionEntity>>

    /**
     * Obtiene una sugerencia de juego por su ID.
     *
     * @param suggestion Identificador único de la sugerencia.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(suggestion: GameSuggestionEntity)

    /**
     * Elimina una sugerencia de juego de la base de datos.
     *
     * @param suggestion Sugerencia de juego a eliminar.
     */
    @Delete
    suspend fun delete(suggestion: GameSuggestionEntity)

    /**
     * Elimina todas las sugerencias.
     */
    @Query("DELETE FROM game_suggestions")
    suspend fun clearAll()
}