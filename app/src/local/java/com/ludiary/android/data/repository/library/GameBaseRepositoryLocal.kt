package com.ludiary.android.data.repository.library

import com.ludiary.android.data.model.GameBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GameBaseRepositoryLocal : GameBaseRepository {

    override fun getGamesBase(): Flow<List<GameBase>> =
        flowOf(emptyList())

    override suspend fun syncGamesBase(forceGamesBase: Boolean): Int =
        0
}