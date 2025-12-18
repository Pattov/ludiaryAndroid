package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.UserGameDao
import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalUserGamesDataSource (
    private val userGameDao: UserGameDao
) {

    suspend fun countPending(uid: String): Int = userGameDao.countPending(uid)

    suspend fun delete(game: UserGame) = userGameDao.delete(game.toEntity())

    suspend fun getById(id: String) = userGameDao.getById(id)?.toModel()

    suspend fun getPending(uid: String): List<UserGame> =
        userGameDao.getPending(uid).map { it.toModel() }

    fun getUserGames(uid: String): Flow<List<UserGame>> =
        userGameDao.getUserGames(uid).map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun hardDeleteById(id: String) = userGameDao.deleteById(id)

    suspend fun upsert(game: UserGame) = userGameDao.upsert(game.toEntity())

    suspend fun upsertAll(games: List<UserGame>) = games.forEach { userGameDao.upsert(it.toEntity()) }
}