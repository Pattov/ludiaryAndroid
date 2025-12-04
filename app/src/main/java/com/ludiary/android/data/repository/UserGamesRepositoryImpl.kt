package com.ludiary.android.data.repository

import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow

/**
 * Implementación de [UserGamesRepository] que opera tanto en Room como en Firestore.
 * @param local Fuente de datos local (Room).
 * @param remote Fuente de datos remoto (Firestore).
 * @return Instancia de [UserGamesRepositoryImpl].
 */
class UserGamesRepositoryImpl(
    private val local: LocalUserGamesDataSource,
    private val remote: FirestoreUserGamesRepository
) : UserGamesRepository {

    /**
     * Devuelve un flujo que emite una lista de juegos del usuario.
     * @param uid Identificador único del usuario.
     * @return Lista de juegos del usuario.
     */
    override fun getUserGames(uid: String): Flow<List<UserGame>> {
        // La UI siempre escucha a Room
        return local.getUserGames(uid)
    }

    /**
     * Actualiza la lista de juegos del usuario en Room y Firestore.
     * @param uid Identificador único del usuario.
     * @param userGame Lista de juegos del usuario.
     * @return Instancia de [UserGamesRepositoryImpl].
     */
    override suspend fun addUserGame(uid: String, userGame: UserGame) {
        remote.addUserGame(uid, userGame)
        local.upsert(userGame)
    }

    /**
     * Actualiza la lista de juegos del usuario en Room y Firestore.
     * @param uid Identificador único del usuario.
     * @param userGame Lista de juegos del usuario.
     * @return Instancia de [UserGamesRepositoryImpl].
     */
    override suspend fun updateUserGame(uid: String, userGame: UserGame) {
        remote.updateUserGame(uid, userGame)
        local.upsert(userGame)
    }

    /**
     * Elimina un juego del usuario en Room y Firestore.
     * @param uid Identificador único del usuario.
     * @param gameId Identificador único del juego.
     * @return Instancia de [UserGamesRepositoryImpl].
     */
    override suspend fun deleteUserGame(uid: String, gameId: String) {
        val game = local.getById(gameId)
        if (game != null) {
            remote.deleteUserGame(uid)
            local.delete(game)
        }
    }
}