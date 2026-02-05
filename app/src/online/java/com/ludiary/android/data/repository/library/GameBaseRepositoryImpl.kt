package com.ludiary.android.data.repository.library

import com.ludiary.android.data.local.LocalGameBaseDataSource
import java.time.Instant

/**
 * Implementación del repositorio de juegos base.
 * @param local Fuente de datos local.
 * @param remote Fuente de datos remota.
 * @return [GameBaseRepository]
 */
class GameBaseRepositoryImpl(
    private val local: LocalGameBaseDataSource,
    private val remote: FirestoreGameBaseRepository
) : GameBaseRepository {

    override fun getGamesBase() = local.getGamesBase()

    /**
     * Sincroniza el catálogo de juegos.
     * @param forceGamesBase Indica si se sincronizará el catálogo completo.
     * @return Número de juegos sincronizados.
     * @throws Exception Si ocurre un error durante la sincronización.
     */
    override suspend fun syncGamesBase(forceGamesBase: Boolean): Int {
        val lastUpdatedMillis: Long? =
            if (forceGamesBase) null else local.getLastUpdatedAtMillis()

        val lastUpdatedInstant = lastUpdatedMillis?.let(Instant::ofEpochMilli)

        val remoteGames =
            if (forceGamesBase || lastUpdatedInstant == null) {
                remote.getAllGamesBase()
            } else {
                remote.getGamesBaseUpdatedSince(lastUpdatedInstant)
            }

        if (remoteGames.isEmpty()) return 0

        if (forceGamesBase) {
            local.clear()
        }

        local.upsertAll(remoteGames)

        return remoteGames.size
    }
}