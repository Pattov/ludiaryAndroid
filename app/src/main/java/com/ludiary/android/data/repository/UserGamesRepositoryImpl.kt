package com.ludiary.android.data.repository

import android.util.Log
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
        val id = userGame.id.ifBlank { UUID.randomUUID().toString() }

        val pending = userGame.copy(
            id = id,
            userId = uid,
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        local.upsert(pending)
    }

    /**
     * Actualiza la lista de juegos del usuario en Room y Firestore.
     * @param uid Identificador único del usuario.
     * @param userGame Lista de juegos del usuario.
     * @return Instancia de [UserGamesRepositoryImpl].
     */
    override suspend fun updateUserGame(uid: String, userGame: UserGame) {
        val id = userGame.id.ifBlank { UUID.randomUUID().toString() }

        val pending = userGame.copy(
            id = id,
            userId = uid,
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        local.upsert(pending)
    }

    override suspend fun deleteUserGame(uid: String, gameId: String) {
        val current = local.getById(gameId) ?: return
        // El registro se marca como delete
        val deleted = current.copy(
            userId = uid,
            syncStatus = SyncStatus.DELETED,
            updatedAt = System.currentTimeMillis()
        )
        local.upsert(deleted)
    }

    override suspend fun syncPending(uid: String): Int {
        val pendingList = local.getPending(uid)
        var syncedCount = 0

        for (game in pendingList){
            try {
                when(game.syncStatus) {
                    SyncStatus.PENDING -> {
                        remote.addUserGame(uid, game)

                        // Marcar como Clean en local
                        local.upsert(game.copy(syncStatus = SyncStatus.CLEAN, updatedAt = System.currentTimeMillis()))
                        syncedCount++
                    }

                    SyncStatus.DELETED -> {
                        remote.deleteUserGame(uid, game.id)
                        local.hardDeleteById(game.id)
                        syncedCount++
                    }
                    else -> {
                        Log.w(
                            "UserGamesSync",
                            "UserGame id=${game.id} con estado ${game.syncStatus} ignorado en sync"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "Ludiary_UserGamesSync",
                    "Error sincronizando gameId=${game.id} status=${game.syncStatus}",
                    e
                )
            }
        }
        return syncedCount
    }

    override suspend fun countPending(uid: String): Int = local.countPending(uid)
}