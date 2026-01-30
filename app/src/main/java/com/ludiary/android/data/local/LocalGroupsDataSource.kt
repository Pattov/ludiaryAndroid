package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.GroupDao
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Fuente de datos local para grupos, basada en Room ([GroupDao]).
 */
class LocalGroupsDataSource(
    private val groupDao: GroupDao
) {

    /**
     * Observa los grupos del usuario aplicando filtro por nombre.
     * @param query Texto de búsqueda. Puede ser vacío.
     */
    fun observeGroups(query: String): Flow<List<GroupEntity>> =
        groupDao.observeGroups(query)

    /**
     * Observa los miembros de un grupo.
     * @param groupId ID del grupo.
     */
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>> =
        groupDao.observeMembers(groupId)

    /**
     * Observa invitaciones entrantes pendientes para el usuario actual.
     * Si no hay sesión, devuelve un Flow vacío.
     * @param myUid UID del usuario actual (puede ser null).
     */
    fun observePendingInvites(myUid: String?): Flow<List<GroupInviteEntity>> =
        if (myUid.isNullOrBlank()) emptyFlow() else groupDao.observePendingInvites(myUid)

    /**
     * Observa invitaciones salientes pendientes del usuario actual.
     * Si no hay sesión, devuelve un Flow vacío.
     * @param myUid UID del usuario actual (puede ser null).
     */
    fun observeOutgoingPendingInvites(myUid: String?): Flow<List<GroupInviteEntity>> =
        if (myUid.isNullOrBlank()) emptyFlow() else groupDao.observeOutgoingPendingInvites(myUid)

    /**
     * Devuelve invitaciones salientes pendientes asociadas a un grupo.
     * Si no hay sesión, devuelve lista vacía.
     * @param groupId ID del grupo.
     * @param myUid UID del usuario actual (puede ser null).
     */
    suspend fun pendingOutgoingInvitesForGroup(
        groupId: String,
        myUid: String?
    ): List<GroupInviteEntity> =
        if (myUid.isNullOrBlank()) emptyList() else groupDao.pendingOutgoingInvitesForGroup(groupId, myUid)

    /**
     * Devuelve todas las invitaciones salientes pendientes del usuario.
     * Si no hay sesión, devuelve lista vacía.
     * @param myUid UID del usuario actual (puede ser null).
     */
    suspend fun pendingOutgoingInvitesAll(myUid: String?): List<GroupInviteEntity> =
        if (myUid.isNullOrBlank()) emptyList() else groupDao.pendingOutgoingInvitesAll(myUid)

    /**
     * Inserta o actualiza múltiples grupos.
     */
    suspend fun upsertGroups(items: List<GroupEntity>) = groupDao.upsertGroups(items)

    /**
     * Inserta o actualiza un grupo.
     */
    suspend fun upsertGroup(item: GroupEntity) = groupDao.upsertGroup(item)

    /**
     * Inserta o actualiza un miembro.
     */
    suspend fun upsertMember(item: GroupMemberEntity) = groupDao.upsertMember(item)

    /**
     * Inserta o actualiza una invitación.
     */
    suspend fun upsertInvite(item: GroupInviteEntity) = groupDao.upsertInvite(item)

    /**
     * Inserta o actualiza múltiples invitaciones.
     */
    suspend fun upsertInvites(items: List<GroupInviteEntity>) = groupDao.upsertInvites(items)

    /**
     * Elimina una invitación por ID.
     */
    suspend fun deleteInvite(inviteId: String) = groupDao.deleteInvite(inviteId)

    /**
     * Elimina todas las invitaciones entrantes pendientes del usuario.
     */
    suspend fun deleteAllIncomingPendingInvites(myUid: String) =
        groupDao.deleteAllIncomingPendingInvites(myUid)

    /**
     * Elimina invitaciones entrantes pendientes que ya no existen en remoto.
     */
    suspend fun deleteMissingIncomingInvites(myUid: String, remoteIds: List<String>) =
        groupDao.deleteMissingIncomingInvites(myUid, remoteIds)

    /**
     * Elimina todas las invitaciones salientes pendientes del usuario.
     */
    suspend fun deleteAllOutgoingPendingInvites(myUid: String) =
        groupDao.deleteAllOutgoingPendingInvites(myUid)

    /**
     * Elimina invitaciones salientes pendientes que ya no existen en remoto.
     */
    suspend fun deleteMissingOutgoingInvites(myUid: String, remoteIds: List<String>) =
        groupDao.deleteMissingOutgoingInvites(myUid, remoteIds)

    /**
     * Aplica el abandono del grupo en local.
     */
    suspend fun leaveGroupLocal(groupId: String, myUid: String) =
        groupDao.leaveGroupLocal(groupId, myUid)
}