package com.ludiary.android.data.local.dao

import androidx.room.*
import com.ludiary.android.data.local.entity.GameBaseEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos relacionadas con el catálogo oficial de juegos.
 */
@Dao
interface GameBaseDao {

    /**
     * Obtiene todos los juegos del catálogo ordenados por título.
     *
     * @return [Flow] que emite una lista de juegos.
     */
    @Query("SELECT * FROM games_base ORDER BY title")
    fun getAll(): Flow<List<GameBaseEntity>>

    /**
     * Obtiene un juego por su ID.
     *
     * @param id Identificador único del juego.
     * @return [GameBaseEntity] o null si no se encuentra el juego.
     */
    @Query("SELECT * FROM games_base WHERE id = :id")
    suspend fun getById(id: String): GameBaseEntity?

    /**
     * Inserta o actualiza una lista de juegos en la base de datos.
     *
     * @param games Lista de juegos a insertar o actualizar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameBaseEntity>)

    /**
     * Inserta o actualiza un juego en la base de datos.
     *
     * @param game Juego a insertar o actualizar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: GameBaseEntity)

    /**
     * Elimina todos los juegos de la base de datos.
     */
    @Query("DELETE FROM games_base")
    suspend fun clear()

    /**
     * Obtiene la última marca de tiempo de actualización entre todos los juegos.
     *
     * @return Momento de última actualización en milisegundos desde epoch, o null si no hay datos.
     */
    @Query("SELECT MAX(updatedAtMillis) FROM games_base")
    suspend fun getLastUpdatedAtMillis(): Long?
}