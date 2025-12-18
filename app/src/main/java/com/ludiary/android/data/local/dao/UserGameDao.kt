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
     * Obtiene todos los juegos del catálogo ordenado por título, excluyendo los marcados como borrados.
     * @return [Flow] que emite una lista de juegos.
     */
    @Query("SELECT * FROM user_games WHERE userId = :userId AND syncStatus != 'DELETED' ORDER BY titleSnapshot")
    fun getUserGames(userId: String): Flow<List<UserGameEntity>>

    /**
     * Obtiene un juego por su ID.
     * @param id Identificador único del juego.
     * @return [UserGameEntity] que representa el juego o nulo si no se encuentra.
     */
    @Query("SELECT * FROM user_games WHERE id = :id")
    suspend fun getById(id: String): UserGameEntity?

    /**
     * Inserta o actualiza una lista de juegos en la base de datos.
     * @param game Lista de juegos a insertar o actualizar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: UserGameEntity)

    /**
     * Elimina todos los juegos de la base de datos.
     * @param game Juego a eliminar.
     */
    @Delete
    suspend fun delete(game: UserGameEntity)

    /**
     * Devuelve los juegos pendientes de sincronizar.
     * @param uid Identificador único del usuario.
     * @return Cantidad de juegos pendientes.
     */
    @Query("SELECT * FROM user_games WHERE userId = :uid AND (syncStatus = 'PENDING' OR syncStatus = 'DELETED')")
    suspend fun getPending(uid: String): List<UserGameEntity>

    /**
     * Cuenta la cantidad de juegos pendientes del usuario.
     * @param uid Identificador único del usuario.
     * @return Cantidad de juegos pendientes.
     */
    @Query("SELECT COUNT(*) FROM user_games WHERE userId = :uid AND (syncStatus = 'PENDING' OR syncStatus = 'DELETED')")
    suspend fun countPending(uid: String): Int

    /**
     * Elimina de forma definitiva un juego por su ID almacenado en local pero ya sincronizados.
     * @param id Identificador único del juego.
     * @return Cantidad de juegos eliminados.
     */
    @Query("DELETE FROM user_games WHERE id = :id")
    suspend fun deleteById(id: String)
}