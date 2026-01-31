package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de grupos e invitaciones.
 */
interface GroupsRepository {

    /**
     * Observa la lista de grupos del usuario, con filtrado por texto.
     * @param query Texto de búsqueda. Puede ser vacío para devolver todos los grupos.
     * @return Flow reactivo con la lista de [GroupEntity].
     */
    fun observeGroups(query: String): Flow<List<GroupEntity>>

    /**
     * Observa la lista de miembros de un grupo.
     * @param groupId Identificador del grupo.
     * @return Flow reactivo con la lista de [GroupMemberEntity].
     */
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    /**
     * Observa invitaciones entrantes pendientes (pendientes de aceptar).
     * @return Flow reactivo con la lista de [GroupInviteEntity] entrantes.
     */
    fun observeIncomingPendingInvites(): Flow<List<GroupInviteEntity>>

    /**
     * Observa invitaciones salientes pendientes creadas por el usuario.
     * @return Flow reactivo con la lista de [GroupInviteEntity] salientes.
     */
    fun observeOutgoingPendingInvites(): Flow<List<GroupInviteEntity>>

    /**
     * Inicia la sincronización remota (realtime) con Firestore.
     */
    fun startRemoteSync()

    /**
     * Detiene la sincronización remota (realtime) con Firestore.
     */
    fun stopRemoteSync()

    /**
     * Obtiene las invitaciones salientes pendientes asociadas a un grupo concreto.
     * @param groupId Identificador del grupo.
     * @return Lista de [GroupInviteEntity] salientes pendientes para ese grupo.
     */
    suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity>

    /**
     * Crea un grupo nuevo y añade al usuario como primer miembro.
     * @param name Nombre del grupo.
     * @return Result<Unit>
     *     Éxito si el grupo se creó correctamente (local y/o remoto según implementación).
     *     Failure si no hay sesión o la operación falla.
     */
    suspend fun createGroup(name: String): Result<Unit>

    /**
     * Invita a un usuario a un grupo.
     * @param groupId Identificador del grupo.
     * @param groupNameSnapshot Nombre del grupo (snapshot) para mostrar sin lecturas extra.
     * @param toUid UID del usuario destinatario.
     * @return Result<Unit>
     *     Éxito si se registra la invitación.
     */
    suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String
    ): Result<Unit>

    /**
     * Acepta una invitación entrante.
     * @param inviteId ID remoto/local de la invitación (según implementación).
     * @return Result<Unit>
     *     Éxito si se acepta y se actualiza el estado en local/remoto.
     */
    suspend fun acceptInvite(inviteId: String): Result<Unit>

    /**
     * Cancela una invitación (normalmente saliente).
     * @param inviteId ID de la invitación.
     * @return Result<Unit>
     *     Éxito si se cancela y se actualiza en local/remoto.
     */
    suspend fun cancelInvite(inviteId: String): Result<Unit>

    /**
     * Rechaza una invitación entrante.
     * @param inviteId ID de la invitación.
     * @return Result<Unit>
     *     Éxito si se rechaza y se refleja el cambio.
     */
    suspend fun rejectInvite(inviteId: String): Result<Unit>

    /**
     * Abandona un grupo.
     * @param groupId Identificador del grupo.
     * @return Result<Unit>
     *     Éxito si se abandona el grupo y se actualiza local/remoto.
     */
    suspend fun leaveGroup(groupId: String): Result<Unit>

    /**
     * Sincroniza invitaciones pendientes creadas en local (offline-first) con Firestore.
     * @return Result<Unit>
     *     Éxito si se procesa la cola sin error fatal.
     */
    suspend fun flushPendingInvites(): Result<Unit>
}