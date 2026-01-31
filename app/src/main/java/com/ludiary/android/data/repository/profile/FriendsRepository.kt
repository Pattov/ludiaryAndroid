package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorio de gestión de amigos y grupos
 */
interface FriendsRepository {

    /**
     * Observa la lista de amigos aceptados
     * @param query Texto de búsqueda. Puede ser vacío
     */
    fun observeFriends(query: String): Flow<List<FriendEntity>>

    /**
     * Observa las solicitudes de amistad entrantes
     * @param query Texto de búsqueda. Puede ser vacío
     */
    fun observeIncomingRequests(query: String): Flow<List<FriendEntity>>

    /**
     * Observa las solicitudes de amistad salientes enviadas por el usuario
     * @param query Texto de búsqueda. Puede ser vacío
     */
    fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>>

    /**
     * Observa la lista de grupos del usuario
     * @param query Texto de búsqueda. Puede ser vacío
     */
    fun observeGroups(query: String) = flowOf(emptyList<FriendEntity>())

    /**
     * Inicia la sincronización remota en tiempo real con Firestore
     */
    fun startRemoteSync()

    /**
     * Detiene la sincronización remota en tiempo real
     */
    fun stopRemoteSync()

    /**
     * Obtiene el código de amigo del usuario actual
     * @return Result<String>
     *     Éxito: código de amigo asociado al usuario
     *     Failure: si no hay sesión activa o el código no existe
     */
    suspend fun getMyFriendCode(): Result<String>

    /**
     * Envía una invitación de amistad usando un código de amigo
     * @param code Código de amigo introducido por el usuario
     * @return Result<Unit>
     *     Éxito: invitación registrada en local
     *     Failure: si no hay sesión o ocurre un error al guardar
     */
    suspend fun sendInviteByCode(code: String): Result<Unit>

    /**
     * Acepta una solicitud de amistad entrante
     * @param friendId Identificador local (Room) de la solicitud
     * @return Result<Unit>
     *     Éxito: solicitud aceptada correctamente
     *     Failure: si no hay sesión o la solicitud no es válida
     */
    suspend fun acceptRequest(friendId: Long): Result<Unit>

    /**
     * Rechaza una solicitud de amistad
     * @param friendId Identificador local (Room) de la solicitud
     * @return Result<Unit>
     *     Éxito: solicitud eliminada
     *     Failure: si no hay sesión o ocurre un error
     */
    suspend fun rejectRequest(friendId: Long): Result<Unit>

    /**
     * Sincroniza con Firestore las invitaciones de amistad pendientes guardadas en local
     * @return Result<Unit>
     *     Éxito: cola procesada
     *     Failure: si ocurre un error durante la sincronización
     */
    suspend fun flushOfflineInvites(): Result<Unit>

    /**
     * Elimina un amigo confirmado
     * @param friendId Identificador local (Room) del amigo
     * @return Result<Unit>
     *     Éxito: amigo eliminado
     *     Failure: si no hay sesión o faltan datos
     */
    suspend fun removeFriend(friendId: Long): Result<Unit>

    /**
     * Actualiza el alias (nickname) de un amigo
     * @param friendId Identificador local (Room) del amigo.
     * @param nickname Nuevo alias asignado
     * @return Result<Unit>
     *     Éxito: alias actualizado
     *     Failure: si no hay sesión o ocurre un error
     */
    suspend fun updateNickname(friendId: Long, nickname: String): Result<Unit>
}