package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO de sesiones (partidas).
 */
@Dao
interface SessionDao {

    /**
     * Obtiene las partidas personales activas (no borradas) de un usuario.
     * @param uid Identificador único del usuario.
     * @param scope Alcance de la partida.
     */
    @Query("SELECT * FROM sessions WHERE scope = :scope AND ownerUserId = :uid AND isDeleted = 0 ORDER BY playedAt DESC")
    fun observePersonalSessions(
        uid: String,
        scope: SessionScope = SessionScope.PERSONAL
    ): Flow<List<SessionEntity>>

    /**
     * Obtiene las sesiones activas (no borradas) de un grupo.
     * @param groupId Identificador único del grupo.
     * @param scope Alcance de la sesión.
     */
    @Query("SELECT * FROM sessions WHERE scope = :scope AND groupId = :groupId AND isDeleted = 0 ORDER BY playedAt DESC")
    fun observeGroupSessions(
        groupId: String,
        scope: SessionScope = SessionScope.GROUP
    ): Flow<List<SessionEntity>>

    /**
     * Obtiene una partida concreta junto con sus jugadores (relación 1-N).
     * @param sessionId Identificador único de la partida.
     */
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionWithPlayers(sessionId: String): Flow<SessionWithPlayers?>

    /**
     *
     * Obtiene una sesión junto con sus jugadores (relación 1-N).
     * @param uid Identificador único del usuario.
     * @param scope Alcance de la sesión.
     */
    @Transaction
    @Query("SELECT * FROM sessions WHERE ownerUserId = :uid AND scope = :scope AND isDeleted = 0 ORDER BY playedAt DESC")
    fun observePersonalSessionsWithPlayers(
        uid: String,
        scope: SessionScope
    ): Flow<List<SessionWithPlayers>>

    /**
     * Obtiene partidas de grupos con jugadores, para pantallas que necesitan
     */
    @Query("SELECT * FROM sessions WHERE isDeleted = 0 ORDER BY playedAt DESC")
    fun observeAllActiveSessions(): Flow<List<SessionEntity>>

    /**
     * Devuelve una partida con jugadores de forma puntual.
     * @param sessionId Identificador único de la partida.
     */
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithPlayers(sessionId: String): SessionWithPlayers?

    // Query CRUD

    /**
     * Inserta o actualiza una partida.
     * @param session La partida.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    /**
     * Inserta o actualiza jugadores de partidas.
     * @param players Lista de jugadores.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayers(players: List<SessionPlayerEntity>)

    /**
     * Elimina todos los jugadores asociados a una partida.
     * @param sessionId Identificador único de la partida.
     */
    @Query("DELETE FROM session_players WHERE sessionId = :sessionId")
    suspend fun deletePlayersBySession(sessionId: String)

    /**
     * Borra físicamente una partida.
     * @param sessionId Identificador único de la partida.
     */
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun hardDeleteSession(sessionId: String)

    /**
     * Borra físicamente una partida y sus jugadores.
     * @param sessionId Identificador único de la partida.
     */
    @Transaction
    suspend fun hardDeleteSessionCascade(sessionId: String) {
        deletePlayersBySession(sessionId)
        hardDeleteSession(sessionId)
    }

    /**
     * Actualiza el estado de sincronización de una partida.
     * @param sessionId Identificador único de la partida.
     * @param status Estado de sincronización.
     * @param updatedAtMillis Fecha y hora actual en millis.
     */
    @Query("UPDATE sessions SET syncStatus = :status, updatedAt = :updatedAtMillis WHERE id = :sessionId")
    suspend fun updateSyncStatus(
        sessionId: String,
        status: SyncStatus,
        updatedAtMillis: Long?
    )

    /**
     * Marca una partida como borrada (soft delete).
     * @param sessionId Identificador único de la partida.
     * @param now Fecha y hora actual en millis.
     * @param deletedStatus Estado de borrado.
     */
    @Query("UPDATE sessions SET isDeleted = 1, syncStatus = :deletedStatus, deletedAt = :now, updatedAt = :now WHERE id = :sessionId")
    suspend fun markSessionDeleted(
        sessionId: String,
        now: Long,
        deletedStatus: SyncStatus = SyncStatus.DELETED
    )

    // Query Sincronización

    /**
     * Devuelve partidas que tienen cambios locales pendientes de sincronizar.
     * @param pending Estado pendiente.
     * @param deleted Estado borrado.
     */
    @Query("SELECT * FROM sessions WHERE syncStatus IN (:pending, :deleted)")
    suspend fun getSessionsPendingPush(
        pending: SyncStatus = SyncStatus.PENDING,
        deleted: SyncStatus = SyncStatus.DELETED
    ): List<SessionEntity>

    /**
     * Devuelve partidas que tienen cambios remotos pendientes de sincronizar.
     * @param uid Identificador único del usuario.
     * @param personal Scope personal.
     * @param pending Estado pendiente.
     */
    @Query("UPDATE sessions SET ownerUserId = :uid, syncStatus = :pending WHERE ownerUserId IS NULL AND scope = :personal AND isDeleted = 0")
    suspend fun adoptOfflinePersonalSessions(
        uid: String,
        pending: SyncStatus = SyncStatus.PENDING,
        personal: SessionScope = SessionScope.PERSONAL
    ): Int

    /**
     * Aplica una partida remota (pull desde Firestore) en local,reemplazando completamente sus jugadores.
     * @param session La partida remota.
     * @param players Los jugadores de la partida remota.
     * @return El número de filas actualizadas.
     */
    @Transaction
    suspend fun applyRemoteSessionReplacePlayers(
        session: SessionEntity,
        players: List<SessionPlayerEntity>
    ) {
        upsertSession(session)
        deletePlayersBySession(session.id)

        if (players.isNotEmpty()) {
            upsertPlayers(players)
        }
    }

    // Query Dashboard

    /**
     * Observa el total de partidas activas.
     * @return El número de partidas.
     */
    @Query("SELECT COUNT(*) FROM sessions WHERE isDeleted = 0")
    fun observeSessionsCount(): Flow<Int>

    /**
     * Observa la media de las partidas jugadas (ignorando nulls).
     * @return Flow con la media de rating.
     */
    @Query("SELECT AVG(overallRating) FROM sessions WHERE isDeleted = 0 AND overallRating IS NOT NULL")
    fun observeAvgRating(): Flow<Double?>

    /**
     * Observa la suma total de minutos jugados (ignorando nulls).
     * @return Flow con la suma de minutos.
     */
    @Query("SELECT SUM(durationMinutes) FROM sessions WHERE isDeleted = 0 AND durationMinutes IS NOT NULL")
    fun observeTotalMinutes(): Flow<Int?>

    /**
     * Observa las partidas más recientes (activas).
     * @param limit Número máximo de partidas a devolver.
     * @return Flow con la lista de partidas más recientes.
     */
    @Query("SELECT * FROM sessions WHERE isDeleted = 0 ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 3): Flow<List<SessionEntity>>

    /**
     * Devuelve el total de minutos jugados.
     * @param fromMillis Fecha de inicio en millis.
     * @param toMillis Fecha de fin en millis.
     * @return Suma de minutos jugados.
     */
    @Query("SELECT playedAt FROM sessions WHERE isDeleted = 0 AND playedAt BETWEEN :fromMillis AND :toMillis")
    suspend fun getPlayedAtBetween(fromMillis: Long, toMillis: Long): List<Long>

}