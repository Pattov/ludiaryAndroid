package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.UserGameDao
import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Fuente de datos local para la ludoteca del usuario.
 * @param userGameDao DAO para operaciones de base de datos relacionadas con juegos.
 * @constructor Crea una nueva instancia de [LocalUserGamesDataSource].
 * @property userGameDao DAO para operaciones de base de datos relacionadas con juegos.
 */
class LocalUserGamesDataSource (
    private val userGameDao: UserGameDao
) {

    /**
     * Elimina todos los juegos del usuario de la base de datos local.
     * @param uid Identificador único del usuario.
     */
    suspend fun clearUser(uid: String) {
        userGameDao.deleteAllByUser(uid)
    }

    /**
     * Cuenta los registros pendientes de sincronización en local para un usuario específico.
     * @param uid Identificador único del usuario.
     * @return número total de registros pendientes.
     */
    suspend fun countPending(uid: String): Int = userGameDao.countPending(uid)

    /**
     * Elimina un juego de la base de datos local.
     * @param game Juego a eliminar.
     */
    suspend fun delete(game: UserGame) = userGameDao.deleteById(game.id)

    /**
     * Recupera un juego del usuario por su ID.
     * @param id Identificador único del juego.
     * @return Juego recuperado o nulo si no se encuentra.
     */
    suspend fun getById(id: String): UserGame? = userGameDao.getById(id)?.toModel()

    /**
     * Recupera los juegos pendientes de sincronización del usuario.
     * @param uid Identificador único del usuario.
     * @return Lista de juegos pendientes.
     */
    suspend fun getPending(uid: String): List<UserGame> =
        userGameDao.getPending(uid).map { it.toModel() }

    /**
     * Recupera todos los juegos del usuario.
     * @param uid Identificador único del usuario.
     * @return flujo de lista de [UserGame] para la UI.
     */
    fun getUserGames(uid: String): Flow<List<UserGame>> =
        userGameDao.getUserGames(uid).map { entities ->
            entities.map { it.toModel() }
        }

    /**
     * Elimina un juego de la base de datos local por su ID.
     * @param id Identificador único del juego.
     * @return Cantidad de juegos eliminados.
     */
    suspend fun hardDeleteById(id: String) = userGameDao.deleteById(id)

    /**
     * Comprueba si la base de datos local está vacía para un usuario concreto.
     * @param uid Identificador único del usuario.
     * @return true si la base de datos está vacía, false en caso contrario.
     */
    suspend fun isEmpty(uid: String): Boolean = userGameDao.countByUser(uid) == 0

    /**
     * Reemplaza completamente todos los juegos del usuario en la base de datos local.
     * @param uid Identificador único del usuario.
     * @param games Lista de juegos a reemplazar.
     * @return Cantidad de juegos eliminados.
     */
    suspend fun replaceAllByUser(uid: String, games: List<UserGame>) {
        userGameDao.deleteAllByUser(uid)
        userGameDao.upsertAll(games.map { it.toEntity() })
    }

    /**
     * Inserta o actualiza un juego en la base de datos local.
     * @param game Juego a insertar o actualizar.
     */
    suspend fun upsert(game: UserGame) = userGameDao.upsert(game.toEntity())

}