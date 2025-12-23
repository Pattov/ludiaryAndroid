package com.ludiary.android.data.repository

import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz que define las operaciones de gestión de juegos del usuario.
 */
interface UserGamesRepository {

    /**
     * Devuelve un flujo que emite una lista de juegos del usuario.
     * @param uid Identificador único del usuario.
     * @return Lista de juegos del usuario.
     */
    fun getUserGames(uid: String): Flow<List<UserGame>>

    /**
     * Devuelve un flujo que emite un juego del usuario.
     * @param uid Identificador único del usuario.
     * @param userGame Identificador único del juego.
     */
    suspend fun addUserGame(uid: String, userGame: UserGame)

    /**
     * Elimina un juego del usuario.
     * @param uid Identificador único del usuario.
     * @param gameId Identificador único del juego.
     */
    suspend fun deleteUserGame(uid: String, gameId: String)

    /**
     * Actualiza un juego del usuario.
     * @param uid Identificador único del usuario.
     * @param userGame Juego del usuario.
     */
    suspend fun updateUserGame(uid: String, userGame: UserGame)

    /**
     * Cuenta los registros pendientes de sincronización en local para un usuario específico.
     * @param uid Identificador único del usuario.
     * @return número total de registros pendientes.
     */
    suspend fun countPending(uid: String): Int

    /**
     * Realiza la sincronización inicial si la lista local está vacía.
     * @param uid Identificador único del usuario.
     */
    suspend fun initialSyncIfNeeded(uid: String)

    /**
     * Sincronización descendente completa (Firestore → Room).
     * @param uid Identificador único del usuario.
     */
    suspend fun syncDown(uid: String)

    /**
     * Sincronización descendente incremental (Firestore → Room).
     * @return número de cambios aplicados en local
     */
    suspend fun syncDownIncremental(uid: String, since: Long): Pair<Int, Long?>

    /**
     * Sincroniza en remoto los cambios pendientes (PENDING/DELETED) y actualiza Room.
     * @return número de elementos sincronizados correctamente
     */
    suspend fun syncPending(uid: String): Int
}