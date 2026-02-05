package com.ludiary.android.data.repository.library

import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow

class UserGamesRepositoryLocal(
    private val local: LocalUserGamesDataSource
) : UserGamesRepository {

    override fun getUserGames(uid: String): Flow<List<UserGame>> =
        local.getUserGames(uid)

    override suspend fun addUserGame(uid: String, userGame: UserGame) {
        local.upsert(userGame.copy(userId = uid))
    }

    override suspend fun deleteUserGame(uid: String, gameId: String) {
        val all = local.getUserGames(uid)
        throw IllegalStateException("En modo local falta soporte delete por gameId. Ver nota debajo.")
    }

    override suspend fun updateUserGame(uid: String, userGame: UserGame) {
        local.upsert(userGame.copy(userId = uid))
    }

    override suspend fun countPending(uid: String): Int =
        local.countPending(uid)

    // ---- Sync remoto: no aplica en modo local ----

    override suspend fun initialSyncIfNeeded(uid: String) {
    }

    override suspend fun syncDown(uid: String) {}

    override suspend fun syncDownIncremental(uid: String, since: Long): Pair<Int, Long?> =
        0 to null

    override suspend fun syncPending(uid: String): Int = 0
}