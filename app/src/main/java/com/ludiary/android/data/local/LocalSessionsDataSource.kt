package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.SessionDao
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.SyncStatus

/**
 * Fuente de datos local para las sesiones.
 * @property sessionDao Instancia del DAO de sesiones.
 */
class LocalSessionsDataSource(
    private val sessionDao: SessionDao
) {

    /**
     * Obtiene todas las partidas de un usuario local.
     * @param uid Identificador único del usuario.
     */
    suspend fun getPendingForPush(): List<SessionEntity> = sessionDao.getSessionsPendingPush()

    /**
     * Obtiene una partida por su identificador.
     * @param sessionId Identificador único de la partida.
     */
    suspend fun getWithPlayers (sessionId: String): SessionWithPlayers? = sessionDao.getSessionWithPlayers(sessionId)

    suspend fun softDelete(sessionId: String, now: Long = System.currentTimeMillis()) {
        sessionDao.markSessionDeleted(sessionId, now)
    }

    /**
     * Actualiza el estado de sincronización de una partida.
     * @param sessionId Identificador único de la partida.
     * @param updatedAtMillis Fecha de actualización.
     */
    suspend fun markClean(sessionId: String, updatedAtMillis: Long?) {
        sessionDao.updateSyncStatus(sessionId, SyncStatus.CLEAN, updatedAtMillis)
    }

    /**
     * Marca una partida como eliminada.
     * @param sessionId Identificador único de la partida.
     */
    suspend fun hardDeleteCascade(sessionId: String) {
        sessionDao.hardDeleteSessionCascade(sessionId)
    }

    /**
     * Aplica una sesión remota (recibida desde Firestore) en la base de datos local reemplazando completamente sus jugadores.
     * @param session Partida remota a aplicar.
     * @param players Lista de jugadores a aplicar.
     */
    suspend fun applyRemoteReplacePlayers(
        session: SessionEntity,
        players: List<SessionPlayerEntity>
    ) {
        sessionDao.applyRemoteSessionReplacePlayers(session, players)
    }

    /**
     * Actualiza el estado de sincronización de una partida.
     * @param uid Identificador único de la partida.
     */
    suspend fun adoptOfflinePersonalSessionsForUser(uid: String): Int {
        return sessionDao.adoptOfflinePersonalSessions(uid)
    }
}