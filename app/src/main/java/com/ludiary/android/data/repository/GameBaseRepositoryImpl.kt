package com.ludiary.android.data.repository

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
     * @param forceFullSync Indica si se sincronizará el catálogo completo.
     * @return Número de juegos sincronizados.
     * @throws Exception Si ocurre un error durante la sincronización.
     */
    override suspend fun syncGamesBase(forceFullSync: Boolean): Int {
        val lastUpdatedMillis: Long? =
            if (forceFullSync) null else local.getLastUpdatedAtMillis()

        val lastUpdatedInstant = lastUpdatedMillis?.let(Instant::ofEpochMilli)

        val remoteGames =
            if (forceFullSync || lastUpdatedInstant == null) {
                remote.getAllGamesBase()
            } else {
                remote.getGamesBaseUpdatedSince(lastUpdatedInstant)
            }

        if (remoteGames.isEmpty()) return 0

        if (forceFullSync) {
            local.clear()
        }

        local.upsertAll(remoteGames)

        return remoteGames.size
    }
}