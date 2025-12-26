package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.data.local.SessionWithPlayers
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a datos de la tabla 'sessions'
 */
@Dao
interface SessionDao {

    /**
     * Obtiene todas las partidas de un usuario personal.
     * @param uid Identificador único del usuario.
     * @param scope Alcance de la sesión.
     */
    @Query("SELECT * FROM sessions WHERE scope = :scope AND ownerUserId = :uid AND isDeleted = 0 ORDER BY playedAt DESC")
    fun observePersonalSessions( uid: String, scope: SessionScope = SessionScope.PERSONAL): Flow<List<SessionEntity>>

    /**
     * Obtiene todas las partidas de un grupo.
     * @param groupId Identificador único del grupo.
     * @param scope Alcance de la partida.
     */
    @Query("SELECT * FROM sessions WHERE scope = :scope AND groupId = :groupId AND isDeleted = 0 ORDER BY playedAt DESC")
    fun observeGroupSessions( groupId: String, scope: SessionScope = SessionScope.GROUP ): Flow<List<SessionEntity>>

    /**
     * Devuelve una partida concreta junto con los jugadores.
     * @param sessionId Identificador único de la partida.
     */
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionWithPlayers(sessionId: String): Flow<SessionWithPlayers?>

    /**
     * Obtiene una partida por su identificador.
     * @param sessionId Identificador único de la partida.
     */
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithPlayers(sessionId: String): SessionWithPlayers?

    /**
     * Inserta o actualiza una partida en la base de datos.
     * @param session Partida a insertar o actualizar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    /**
     * Inserta o actualiza una lista de jugadores en la base de datos.
     * @param players Lista de jugadores a insertar o actualizar.
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
     * Elimina una partida por su identificador.
     * @param sessionId Identificador único de la partida.
     */
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun hardDeleteSession(sessionId: String)

    /**
     * Actualiza el estado de sincronización de una partida.
     * @param sessionId Identificador único de la partida.
     * @param status Estado de sincronización.
     * @param updateAtMillis Fecha de actualización.
     */
    @Query("UPDATE sessions SET syncStatus = :status, updatedAt = :updateAtMillis WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: String, status: SyncStatus, updateAtMillis: Long?)

    /**
     * Marca una partida como eliminada.
     * @param sessionId Identificador único de la partida.
     * @param now Fecha de eliminación.
     * @param deletedStatus Estado de eliminación.
     */
    @Query("UPDATE sessions SET isDeleted = 1, syncStatus = :deletedStatus, deletedAt = :now, updatedAt = :now WHERE id = :sessionId")
    suspend fun markSessionDeleted(sessionId: String, now: Long, deletedStatus: SyncStatus = SyncStatus.DELETED)

    // ---------- Sync ----------
    /**
     * Devuelve todas las sesiones con cambios pendientes de sincronizar.
     * @param pending Estado de sincronización pendiente.
     * @param deleted Estado de eliminación.
     */
    @Query(" SELECT * FROM sessions WHERE syncStatus IN (:pending, :deleted) ")
    suspend fun getSessionsPendingPush(
        pending: SyncStatus = SyncStatus.PENDING,
        deleted: SyncStatus = SyncStatus.DELETED
    ): List<SessionEntity>

    /**
     * Devuelve todas las sesiones con cambios pendientes de sincronizar.
     * @param uid Identificador único del usuario.
     * @param pending Estado de sincronización pendiente.
     * @param personal Alcance de la sesión.
     */
    @Query("UPDATE sessions SET ownerUserId = :uid, syncStatus = :pending WHERE ownerUserId IS NULL AND scope = :personal AND isDeleted = 0")
    suspend fun adoptOfflinePersonalSessions(
        uid: String,
        pending: SyncStatus = SyncStatus.PENDING,
        personal: SessionScope = SessionScope.PERSONAL
    ): Int


    /**
     * Aplica una sesión remota (recibida desde Firestore) en la base de datos local reemplazando completamente sus jugadores.
     * @param session Partida remota a aplicar.
     * @param players Lista de jugadores a aplicar.
     */
    @Transaction
    suspend fun applyRemoteSessionReplacePlayers( session: SessionEntity, players: List<SessionPlayerEntity> )
    {
        upsertSession(session)
        deletePlayersBySession(session.id)
        if (players.isNotEmpty()) { upsertPlayers(players) }
    }

    /**
     * Elimina una partida por su identificador.
     * @param sessionId Identificador único de la partida.
     */
    @Transaction
    suspend fun hardDeleteSessionCascade(sessionId: String) {
        hardDeleteSession(sessionId)
    }
}