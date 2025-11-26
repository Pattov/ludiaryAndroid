package com.ludiary.android.data.repository

import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow

interface UserGamesRepository {
    fun getUserGames(uid: String): Flow<List<UserGame>>

    suspend fun addUserGame(uid: String, userGame: UserGame)

    suspend fun deleteUserGame(uid: String, gameId: String)

    suspend fun updateUserGame(uid: String, userGame: UserGame)
}