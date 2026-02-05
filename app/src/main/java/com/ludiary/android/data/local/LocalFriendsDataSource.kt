package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.FriendDao
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fuente de datos local para amigos (Room).
 */
class LocalFriendsDataSource(
    private val friendDao: FriendDao
) {

    /**
     * Observa amigos aceptados, filtrando por texto.
     * @param query Texto de búsqueda. Puede ser vacío.
     */
    fun observeFriends(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatus(FriendStatus.ACCEPTED, query)

    /**
     * Observa solicitudes entrantes pendientes, filtrando por texto.
     * @param query Texto de búsqueda. Puede ser vacío.
     */
    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatus(FriendStatus.PENDING_INCOMING, query)

    /**
     * Observa solicitudes salientes:
     * - [FriendStatus.PENDING_OUTGOING] (ya sincronizada)
     * - [FriendStatus.PENDING_OUTGOING_LOCAL] (creada offline / pendiente de flush)
     *
     * @param query Texto de búsqueda. Puede ser vacío.
     */
    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        friendDao.observeByStatuses(
            listOf(FriendStatus.PENDING_OUTGOING, FriendStatus.PENDING_OUTGOING_LOCAL),
            query
        )

    /**
     * Observa grupos.
     */
    fun observeGroups(query: String): Flow<List<FriendEntity>> = flowOf(emptyList())

    /**
     * Obtiene un registro por ID local.
     */
    suspend fun getById(id: Long): FriendEntity? = friendDao.getById(id)

    /**
     * Obtiene un registro por UID remoto del amigo.
     */
    suspend fun getByFriendUid(uid: String): FriendEntity? = friendDao.getByFriendUid(uid)

    /**
     * Actualiza el nickname (alias) de un amigo en local.
     * @param id ID local.
     * @param nickname Nuevo alias (puede ser null).
     * @param updatedAt Timestamp (epoch millis) de actualización.
     */
    suspend fun updateNickname(id: Long, nickname: String?, updatedAt: Long) {
        friendDao.updateNickname(id, nickname, updatedAt)
    }

    /**
     * Devuelve invitaciones pendientes creadas en local (offline-first) que aún no se han subido a Firestore.
     */
    suspend fun getPendingInvites(): List<FriendEntity> =
        friendDao.getPendingInvites(
            status = FriendStatus.PENDING_OUTGOING_LOCAL,
            syncStatus = SyncStatus.PENDING
        )

    /**
     * Inserta un registro en local.
     * @return ID local generado.
     */
    suspend fun insert(entity: FriendEntity): Long = friendDao.insert(entity)

    /**
     * MVP: Upsert simple.
     * Inserta (ABORT si choca con UNIQUE(friendUid)).
     *
     * Para actualizaciones parciales, usa [updateStatusAndUid] / [updateSyncStatus] / [updateNickname].
     */
    suspend fun upsert(entity: FriendEntity) {
        friendDao.insert(entity)
    }

    /**
     * Actualiza estado, UID remoto y syncStatus por ID local.
     */
    suspend fun updateStatusAndUid(
        id: Long,
        status: FriendStatus,
        friendUid: String?,
        syncStatus: SyncStatus
    ): Int = friendDao.updateStatusAndUid(
        id = id,
        status = status,
        friendUid = friendUid,
        syncStatus = syncStatus,
        updatedAt = System.currentTimeMillis()
    )

    /**
     * Actualiza el syncStatus por ID local.
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus): Int =
        friendDao.updateSyncStatus(id, syncStatus, System.currentTimeMillis())

    /**
     * Elimina un registro por ID local.
     */
    suspend fun deleteById(id: Long): Int = friendDao.deleteById(id)

    /**
     * Borra toda la tabla `friends`.
     */
    suspend fun clearAll() = friendDao.clearAll()

    /**
     * Aplica un amigo remoto ya mapeado a entidad Room (pull).
     */
    suspend fun upsertRemoteEntity(entity: FriendEntity) {
        friendDao.upsertByFriendUid(entity)
    }
}