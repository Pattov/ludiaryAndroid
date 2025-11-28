package com.ludiary.android.data.repository

import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeUserGamesRepository: UserGamesRepository {

    private val gamesState = MutableStateFlow(
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

    override fun getUserGames(uid: String): Flow<List<UserGame>> = gamesState

    override suspend fun addUserGame(uid: String, userGame: UserGame) {
        val current = gamesState.value
        val newId = (current.size + 1).toString()

        val newGame = userGame.copy(id = newId)
        gamesState.value = current + newGame
    }
    override suspend fun deleteUserGame(uid: String, gameId: String) {
        val current = gamesState.value
        gamesState.value = current.filterNot { it.id == gameId }
    }
    override suspend fun updateUserGame(uid: String, userGame: UserGame) {
        val current = gamesState.value
        gamesState.value = current.map { if (it.id == userGame.id) userGame else it }
    }
}