package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.GameBaseDao
import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.GameBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Fuente de datos local para juegos base.
 * @param gameBaseDao DAO para operaciones de base de datos relacionadas con juegos base.
 */
class LocalGameBaseDataSource (
    private val gameBaseDao: GameBaseDao
){

    /**
     * Obtiene un flujo con la lista de juegos base.
     * @return [Flow] con la lista de juegos base.
     */
    fun getGamesBase(): Flow<List<GameBase>> = gameBaseDao.getAll().map { entities -> entities.map{ it.toModel() } }

    /**
     * Inserta o actualiza una lista de juegos base en la base de datos.
     * @param games Lista de juegos base a insertar o actualizar.
     */
    suspend fun upsertAll(games: List<GameBase>) = gameBaseDao.upsertAll(games.map { it.toEntity() })

    /**
     * Elimina todos los juegos base de la base de datos.
     */
    suspend fun clear() = gameBaseDao.clear()

    /**
     * Obtiene la fecha de actualización más reciente de los juegos base.
     * @return Fecha de actualización más reciente
     */
    suspend fun getLastUpdatedAtMillis(): Long? = gameBaseDao.getLastUpdatedAtMillis()

}