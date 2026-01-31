package com.ludiary.android.data.repository.library

import com.ludiary.android.data.model.GameBase
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de juegos base.
 * @return [GameBaseRepository]
 */
interface GameBaseRepository {

    /**
     * Obtiene un flujo con la lista de juegos base.
     * @return [Flow] con la lista de juegos base.
     */
    fun getGamesBase(): Flow<List<GameBase>>

    /**
     * Sincroniza el catálogo de juegos base.
     * @param forceGamesBase Indica si se sincronizará el catálogo completo.
     * @return Número de juegos sincronizados.
     * @throws Exception Si ocurre un error durante la sincronización.
     */
    suspend fun syncGamesBase(forceGamesBase: Boolean = false): Int
}