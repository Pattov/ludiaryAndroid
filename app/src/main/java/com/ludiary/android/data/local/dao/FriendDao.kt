package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO de amigos
 */
@Dao
interface FriendDao {

    /**
     * Observa amigos por un único estado, aplicando filtro por texto
     * @param status Estado a filtrar.
     * @param q Texto de búsqueda. Puede ser vacío.
     * @return Flow con la lista de [FriendEntity] ordenada por updatedAt DESC.
     */
    @Query("SELECT * FROM friends WHERE status = :status AND (displayName LIKE '%' || :q || '%' OR nickname LIKE '%' || :q || '%' OR friendCode LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun observeByStatus(status: FriendStatus, q: String): Flow<List<FriendEntity>>

    /**
     * Observa amigos por varios estados, aplicando filtro por texto.
     * @param statuses Lista de estados a incluir.
     * @param q Texto de búsqueda. Puede ser vacío.
     * @return Flow con la lista de [FriendEntity] ordenada por updatedAt DESC.
     */
    @Query("SELECT * FROM friends WHERE status IN (:statuses) AND (displayName LIKE '%' || :q || '%' OR nickname LIKE '%' || :q || '%' OR friendCode LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun observeByStatuses(statuses: List<FriendStatus>, q: String): Flow<List<FriendEntity>>

    /**
     * Devuelve un amigo por ID local.
     * @param id ID local (Room).
     */
    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FriendEntity?

    /**
     * Devuelve un amigo por UID remoto.
     * @param uid UID del usuario amigo.
     */
    @Query("SELECT * FROM friends WHERE friendUid = :uid LIMIT 1")
    suspend fun getByFriendUid(uid: String): FriendEntity?

    /**
     * Devuelve invitaciones pendientes según estado y estado de sincronización.
     * @param status Estado de amistad.
     * @param syncStatus Estado de sincronización.
     * @return Lista ordenada por updatedAt ASC.
     */
    @Query("SELECT * FROM friends WHERE status = :status AND syncStatus = :syncStatus ORDER BY updatedAt ASC")
    suspend fun getPendingInvites(status: FriendStatus, syncStatus: SyncStatus): List<FriendEntity>

    /**
     * Inserta un registro en `friends`.
     * @param entity Entidad a insertar.
     * @return ID local generado.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FriendEntity): Long

    /**
     * Inserta ignorando conflicto.
     * @param entity Entidad a insertar.
     * @return
     *      Devuelve rowId si inserta
     *      Devuelve -1 si hubo conflicto (ya existía).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: FriendEntity): Long

    /**
     * Actualiza un registro existente por `friendUid`.
     *
     * @param friendUid UID del amigo.
     * @param friendCode Código de amistad.
     * @param displayName Nombre a mostrar.
     * @param nickname Alias del usuario.
     * @param status Estado de amistad.
     * @param syncStatus Estado de sincronización.
     * @param updatedAt Timestamp epoch millis.
     * @return Número de filas actualizadas.
     */
    @Query("UPDATE friends SET friendCode = :friendCode, displayName = :displayName, nickname = :nickname, status = :status, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE friendUid = :friendUid")
    suspend fun updateByFriendUid(
        friendUid: String,
        friendCode: String?,
        displayName: String?,
        nickname: String?,
        status: FriendStatus,
        syncStatus: SyncStatus,
        updatedAt: Long
    ): Int

    /**
     * Actualiza estado + friendUid de un registro por ID local.
     * @param id ID local (Room).
     * @param status Nuevo estado.
     * @param friendUid UID remoto resuelto.
     * @param syncStatus Estado de sincronización.
     * @param updatedAt Timestamp epoch millis.
     * @return Número de filas actualizadas.
     */
    @Query("UPDATE friends SET status = :status, friendUid = :friendUid, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatusAndUid(
        id: Long,
        status: FriendStatus,
        friendUid: String?,
        syncStatus: SyncStatus,
        updatedAt: Long
    ): Int

    /**
     * Actualiza solo el estado de sincronización por ID local.
     * @param id ID local (Room).
     * @param syncStatus Nuevo estado de sincronización.
     * @param updatedAt Timestamp epoch millis.
     * @return Número de filas actualizadas.
     */
    @Query("UPDATE friends SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus, updatedAt: Long): Int

    /**
     * Actualiza el nickname (alias) por ID local.
     * @param id ID local (Room).
     * @param nickname Alias (puede ser null).
     * @param updatedAt Timestamp epoch millis.
     * @return Número de filas actualizadas.
     */
    @Query("UPDATE friends SET nickname = :nickname, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNickname(id: Long, nickname: String?, updatedAt: Long)

    /**
     * Elimina un registro por ID local.
     * @param id ID local (Room).
     * @return Número de filas eliminadas.
     */
    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Borra completamente la tabla `friends`.
     */
    @Query("DELETE FROM friends")
    suspend fun clearAll()

    /**
     * Upsert por `friendUid`.
     * @param entity Entidad que se quiere aplicar en local.
     * @return
     *      rowId insertado o ID existente si se actualiza;
     *      -1 si no se encuentra tras conflicto (no debería).
     */
    @Transaction
    suspend fun upsertByFriendUid(entity: FriendEntity): Long {
        val uid = entity.friendUid

        if (uid.isNullOrBlank()) {
            return insertIgnore(entity)
        }

        val rowId = insertIgnore(entity)
        if (rowId != -1L) return rowId

        updateByFriendUid(
            friendUid = uid,
            friendCode = entity.friendCode,
            displayName = entity.displayName,
            nickname = entity.nickname,
            status = entity.status,
            syncStatus = entity.syncStatus,
            updatedAt = System.currentTimeMillis()
        )

        return getByFriendUid(uid)?.id ?: -1L
    }
}