package com.ludiary.android.data.repository

import com.ludiary.android.data.local.LocalSessionsDataSource
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.sync.SyncPrefs

/**
 * Repositorio de partidas.
 * @property local Fuente de datos local.
 * @property remote Fuente de datos remota.
 * @property syncPrefs Preferencias de sincronización.
 * @property groupIdProvider Proveedor de identificadores de grupos.
 */
class SessionsRepositoryImpl(
    private val local: LocalSessionsDataSource,
    private val remote: FirestoreSessionsRepository,
    private val syncPrefs: SyncPrefs,
    private val groupIdProvider: GroupIdProvider
) : SessionsRepository {

    /**
     * Sincroniza las partidas.
     * @param uid Identificador único del usuario.
     * @return Resultado de la sincronización.
     */
    override suspend fun sync(uid: String): SessionsSyncResult {

        val adopted = local.adoptOfflinePersonalSessionsForUser(uid)

        val pushed = pushPending()

        val (pulledPersonal, pulledGroups) = pullIncremental(uid)

        return SessionsSyncResult(
            adopted = adopted,
            pushed = pushed,
            pulledPersonal = pulledPersonal,
            pulledGroups = pulledGroups
        )
    }

    /**
     * Elimina una partida.
     * @param sessionId Identificador único de la partida.
     * @return Resultado de la operación.
     */
    override suspend fun deleteSession(sessionId: String) {
        local.softDelete(sessionId)
    }

    /**
     * Envía las partidas pendientes a Firestore.
     * @return Número de partidas enviadas.
     */
    private suspend fun pushPending(): Int {
        val pending = local.getPendingForPush()
        var pushed = 0

        for (s in pending) {
            if (s.ownerUserId.isNullOrBlank()) continue

            if (s.syncStatus == SyncStatus.DELETED || s.isDeleted) {
                remote.softDeleteSession(s.id)
                local.hardDeleteCascade(s.id)
                pushed++
            } else {
                val withPlayers = local.getWithPlayers(s.id) ?: continue
                remote.upsertSession(withPlayers)
                local.markClean(s.id, System.currentTimeMillis())
                pushed++
            }
        }
        return pushed
    }

    /**
     * Obtiene las partidas modificadas desde una fecha específica.
     * @param uid Identificador único del usuario.
     * @return Número de partidas modificadas.
     */
    private suspend fun pullIncremental(uid: String): Pair<Int, Int> {
        var pulledPersonal = 0
        var pulledGroups = 0

        // PERSONAL
        val lastPersonal = syncPrefs.getLastPullPersonalSessions(uid)
        val personalChanged = remote.fetchPersonalChangedSince(uid, lastPersonal)

        var maxPersonal: Long? = null
        for (r in personalChanged) {
            maxPersonal = maxOf(maxPersonal ?: 0L, r.updatedAtMillis ?: 0L)

            if (r.isDeleted) {
                local.hardDeleteCascade(r.id)
            } else {
                local.applyRemoteReplacePlayers(r.sessionEntity, r.playerEntities)
            }
            pulledPersonal++
        }
        if (maxPersonal != null && maxPersonal > 0L) {
            syncPrefs.setLastPullPersonalSessions(uid, maxPersonal)
        }

        // GROUPS
        val groupIds = groupIdProvider.getGroupIdsForUser(uid)
        for (gid in groupIds) {
            val lastGroup = syncPrefs.getLastPullGroupSessions(uid, gid)
            val groupChanged = remote.fetchGroupChangedSince(gid, lastGroup)

            var maxGroup: Long? = null
            for (r in groupChanged) {
                maxGroup = maxOf(maxGroup ?: 0L, r.updatedAtMillis ?: 0L)

                if (r.isDeleted) {
                    local.hardDeleteCascade(r.id)
                } else {
                    local.applyRemoteReplacePlayers(r.sessionEntity, r.playerEntities)
                }
                pulledGroups++
            }
            if (maxGroup != null && maxGroup > 0L) {
                syncPrefs.setLastPullGroupSessions(uid, gid, maxGroup)
            }
        }

        return pulledPersonal to pulledGroups
    }
}

/**
 * Interfaz que define el repositorio de sesiones.
 */
interface GroupIdProvider {

    /**
     * Obtiene los identificadores de grupos para un usuario.
     * @param uid Identificador único del usuario.
     * @return Lista de identificadores de grupos.
     */
    suspend fun getGroupIdsForUser(uid: String): List<String>
}