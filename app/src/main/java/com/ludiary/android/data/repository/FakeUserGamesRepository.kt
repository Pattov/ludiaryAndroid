package com.ludiary.android.data.repository

import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeUserGamesRepository: UserGamesRepository {

    override fun getUserGames(uid: String): Flow<List<UserGame>> = flow {
        emit(
            listOf(
                UserGame(
                    id = "1",
                    titleSnapshot = "Catan",
                    personalRating = 4.5f
                ),
                UserGame(
                    id = "2",
                    titleSnapshot = "Carcassonne",
                    personalRating = 4.0f
                )
            )
        )
    }

    override suspend fun addUserGame(uid: String, userGame: UserGame) {}
    override suspend fun deleteUserGame(uid: String, gameId: String) {}
    override suspend fun updateUserGame(uid: String, userGame: UserGame) {}
}