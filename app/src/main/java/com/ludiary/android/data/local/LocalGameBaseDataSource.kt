package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.GameBaseDao
import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.GameBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalGameBaseDataSource (
    private val gameBaseDao: GameBaseDao
){

    fun getGamesBase(): Flow<List<GameBase>> = gameBaseDao.getAll().map { entities -> entities.map{ it.toModel() } }

    suspend fun upsertAll(games: List<GameBase>) = gameBaseDao.upsertAll(games.map { it.toEntity() })

    suspend fun clear() = gameBaseDao.clear()

    suspend fun getLastUpdatedAtMillis(): Long? = gameBaseDao.getLastUpdatedAtMillis()

}