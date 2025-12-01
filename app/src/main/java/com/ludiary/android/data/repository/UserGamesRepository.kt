package com.ludiary.android.data.repository

import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz que define las operaciones de gestión de juegos del usuario.
 */
interface UserGamesRepository {

    /**
     * Devuelve un flujo que emite una lista de juegos del usuario.
     *
     * @param uid Identificador único del usuario.
     * @return Lista de juegos del usuario.
     */
    fun getUserGames(uid: String): Flow<List<UserGame>>

    /**
     * Devuelve un flujo que emite un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param gameId Identificador único del juego.
     */
    suspend fun addUserGame(uid: String, userGame: UserGame)

    /**
     * Elimina un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param gameId Identificador único del juego.
     */
    suspend fun deleteUserGame(uid: String, gameId: String)

    /**
     * Actualiza un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param userGame Juego del usuario.
     */
    suspend fun updateUserGame(uid: String, userGame: UserGame)
}