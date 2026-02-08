package com.ludiary.android.data.repository.library

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

    override fun getUserGames(uid: String): Flow<List<UserGame>> = local.getUserGames(uid)

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
            updatedAt = System.currentTimeMillis(),
            createdAt = userGame.createdAt ?: System.currentTimeMillis()
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
            updatedAt = System.currentTimeMillis(),
            createdAt = userGame.createdAt ?: System.currentTimeMillis()
        )

        local.upsert(pending)
    }

    /**
     * Borra un juego del usuario de forma diferida.
     * @param uid Identificador único del usuario.
     * @param gameId Identificador único del juego.
     * @return Instancia de [UserGamesRepositoryImpl].
     */
    override suspend fun deleteUserGame(uid: String, gameId: String) {
        local.markAsDeleted(gameId)
    }

    override suspend fun getRemoteUserGames(uid: String): List<UserGame> {
        return remote.fetchAll(uid)
    }

    /**
     * Sincroniza en remoto los cambios pendientes (PENDING/DELETED) y actualiza Room.
     * @param uid Identificador único del usuario.
     * @return Instancia de [UserGamesRepositoryImpl].
     * @throws Exception si ocurre un error durante la sincronización.
     */
    override suspend fun syncDown(uid: String) {
        val remoteList = remote.fetchAll(uid)
        val cleaned = remoteList.map { it.copy(userId = uid, syncStatus = SyncStatus.CLEAN) }
        local.replaceAllByUser(uid, cleaned)
    }

    /**
     * Sincroniza en remoto los cambios pendientes (PENDING/DELETED) y actualiza Room.
     * @param uid Identificador único del usuario.
     * @return Instancia de [UserGamesRepositoryImpl].
     * @throws Exception si ocurre un error durante la sincronización.
     */
    override suspend fun syncDownIncremental( uid: String, since: Long ): Pair<Int, Long?> {

        val remoteChanged = remote.fetchChangedSince(uid, since)

        var applied = 0
        var maxRemoteUpdatedAt: Long? = null

        for (remoteGame in remoteChanged) {

            remoteGame.updatedAt?.let { rTs ->
                maxRemoteUpdatedAt = maxOf(maxRemoteUpdatedAt ?: 0L, rTs)
            }

            val localGame = local.getById(remoteGame.id)

            if (remoteGame.isDeleted) {

                // Si hay cambios locales pendientes, no tocamos
                if (
                    localGame?.syncStatus == SyncStatus.PENDING ||
                    localGame?.syncStatus == SyncStatus.DELETED ||
                    localGame?.syncStatus == SyncStatus.CONFLICT
                ) {
                    continue
                }

                local.hardDeleteById(remoteGame.id)
                applied++
                continue
            }

            // no existe en local → Insert
            if (localGame == null) {
                local.upsert(
                    remoteGame.copy(
                        userId = uid,
                        syncStatus = SyncStatus.CLEAN
                    )
                )
                applied++
                continue
            }

            // Local pending → Posible conflicto
            if (localGame.syncStatus == SyncStatus.PENDING) {

                val localUpdatedAt = localGame.updatedAt ?: 0L
                val remoteUpdatedAt = remoteGame.updatedAt ?: 0L

                if (remoteUpdatedAt > localUpdatedAt) {
                    local.upsert(
                        localGame.copy(
                            syncStatus = SyncStatus.CONFLICT
                        )
                    )
                }

                continue
            }

            if (localGame.syncStatus == SyncStatus.CONFLICT) {
                continue
            }

            if (localGame.syncStatus == SyncStatus.DELETED) {
                continue
            }

            val r = remoteGame.updatedAt ?: 0L
            val l = localGame.updatedAt ?: 0L

            if (r > l) {
                local.upsert(
                    remoteGame.copy( userId = uid, syncStatus = SyncStatus.CLEAN)
                )
                applied++
            }
        }

        return applied to maxRemoteUpdatedAt
    }


    /**
     * Sincroniza los cambios pendientes (PENDING/DELETED) y actualiza Room.
     * @param uid Identificador único del usuario.
     * @return número de elementos sincronizados correctamente
     * @throws Exception si ocurre un error durante la sincronización.
     */
    override suspend fun syncPending(uid: String): Int {
        val pendingList = local.getPending(uid)
        var syncedCount = 0

        for (game in pendingList) {
            try {
                when (game.syncStatus) {
                    SyncStatus.PENDING -> {
                        remote.upsertUserGame(uid, game.copy(userId = uid))
                        local.upsert(
                            game.copy(
                                syncStatus = SyncStatus.CLEAN,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        syncedCount++
                    }

                    SyncStatus.DELETED -> {
                        remote.softDeleteUserGame(uid, game.id)
                        local.hardDeleteById(game.id)
                        syncedCount++
                    }

                    else -> {
                        Log.w("Ludiary_UserGamesSync", "UserGame id=${game.id} con estado ${game.syncStatus} ignorado en sync")
                    }
                }
            } catch (e: Exception) {
                Log.e("Ludiary_UserGamesSync", "Error sincronizando gameId=${game.id} status=${game.syncStatus}", e)
            }
        }

        return syncedCount
    }

    /**
     * Cuenta los registros pendientes de sincronización en local para un usuario específico.
     * @param uid Identificador único del usuario.
     * @return número total de registros pendientes.
     * @throws Exception si ocurre un error al contar los registros.
     */
    override suspend fun countPending(uid: String): Int = local.countPending(uid)

    /**
     * Realiza la sincronización inicial si la lista local está vacía.
     * @param uid Identificador único del usuario.
     * @throws Exception si ocurre un error durante la sincronización inicial.
     */
    override suspend fun initialSyncIfNeeded(uid: String) {
        if (local.isEmpty(uid)) {
            Log.d("LUDIARY_SYNC_INIT", "local empty -> syncDown")
            syncDown(uid)
        } else {
            Log.d("LUDIARY_SYNC_INIT", "local not empty -> Manejamos conflicto")
        }
    }
}