package com.ludiary.android.data.repository

import com.ludiary.android.data.model.GameBase
import kotlinx.coroutines.flow.Flow

interface GameBaseRepository {

    fun getGamesBase(): Flow<List<GameBase>>

    suspend fun syncGamesBase(forceGamesBase: Boolean = false): Int
}