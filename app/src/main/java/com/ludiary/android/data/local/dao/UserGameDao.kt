package com.ludiary.android.data.local.dao

import androidx.room.*
import com.ludiary.android.data.local.entity.UserGameEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos relacionadas con juegos.
 */
@Dao
interface UserGameDao {

    /**
     * Obtiene todos los juegos del catálogo ordenado por título.
     *
     * @return [Flow] que emite una lista de juegos.
     */
    @Query("SELECT * FROM user_games WHERE userId = :userId ORDER BY titleSnapshot")
    fun getUserGames(userId: String): Flow<List<UserGameEntity>>

    /**
     * Obtiene un juego por su ID.
     *
     * @param id Identificador único del juego.
     * @return [GameBaseEntity] o null si no se encuentra el juego.
     */
    @Query("SELECT * FROM user_games WHERE id = :id")
    suspend fun getById(id: String): UserGameEntity?

    /**
     * Inserta o actualiza una lista de juegos en la base de datos.
     *
     * @param game Lista de juegos a insertar o actualizar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: UserGameEntity)

    /**
     * Elimina todos los juegos de la base de datos.
     */
    @Delete
    suspend fun delete(game: UserGameEntity)
}