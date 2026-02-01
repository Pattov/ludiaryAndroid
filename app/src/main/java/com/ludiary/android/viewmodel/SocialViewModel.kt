package com.ludiary.android.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.profile.FriendsRepository
import com.ludiary.android.data.repository.profile.GroupsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado UI de la pantalla de Amigos/Grupos/Solicitudes.
 * @property tab Pestaña seleccionada actualmente.
 * @property query Texto de búsqueda actual.
 * @property myFriendCode Código de amigo del usuario (si está disponible).
 * @property hasFriends Indica si el usuario tiene al menos 1 amigo aceptado.
 */
data class FriendsUiState(
    val tab: FriendsTab = FriendsTab.FRIENDS,
    val query: String = "",
    val myFriendCode: String? = null,
    val hasFriends: Boolean = false
)

/**
 * Modelo UI para filas de la lista de grupos.
 * @property group Entidad del grupo.
 * @property membersCount Conteo de miembros mostrado en UI.
 */
data class GroupRowUi(
    val group: GroupEntity,
    val membersCount: Int
)

/**
 * Eventos de una sola vez (one-shot) para la UI: navegación, diálogos, snackbars, etc.
 */
sealed class FriendsUiEvent {

    /**
     * Solicita abrir el diálogo de edición de nickname.
     * @property friendId ID local (Room) del amigo.
     * @property currentNickname Nick actual si existe.
     */
    data class OpenEditNickname(val friendId: Long, val currentNickname: String?) : FriendsUiEvent()

    /**
     * Muestra un snackbar con un mensaje.
     */
    data class ShowSnack(@param:StringRes val messageRes: Int) : FriendsUiEvent()

    /** Abre el diálogo para añadir amigo por código. */
    object OpenAddFriend : FriendsUiEvent()

    /** Abre el diálogo para crear grupo. */
    object OpenAddGroup : FriendsUiEvent()
}

/**
 * Filas UI para la lista de “Solicitudes” (amigos + invitaciones de grupo), con headers.
 */
sealed class FriendRowUi {

    /**
     * Header separador dentro de la lista.
     * @property titleRes Texto del header.
     */
    data class Header(@param:StringRes val titleRes: Int) : FriendRowUi()

    /**
     * Fila de solicitud de amigo.
     * @property friend Entidad local del amigo.
     */
    data class FriendItem(val friend: FriendEntity) : FriendRowUi()

    /**
     * Fila de invitación de grupo.
     * @property invite Entidad local de la invitación.
     * @property isOutgoing True si la invitación la envió el usuario actual.
     */
    data class GroupItem(val invite: GroupInviteEntity, val isOutgoing: Boolean) : FriendRowUi()
}

/**
 * ViewModel de la pantalla de Amigos.
 * @param friendsRepo Repositorio de amigos.
 * @param groupsRepo Repositorio de grupos.
 */
class FriendsViewModel(
    private val friendsRepo: FriendsRepository,
    private val groupsRepo: GroupsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    /** Estado observable por la UI. */
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private var started = false

    private val _events = MutableSharedFlow<FriendsUiEvent>()
    val events: SharedFlow<FriendsUiEvent> = _events.asSharedFlow()

    /**
     * Arranca sincronización y cargas iniciales.
     */
    fun start() {
        if (started) return
        started = true

        friendsRepo.startRemoteSync()
        groupsRepo.startRemoteSync()

        viewModelScope.launch {
            // flush de pendientes (por ejemplo, si hubo invitaciones creadas offline)
            groupsRepo.flushPendingInvites()

            // Cargar mi friend code
            friendsRepo.getMyFriendCode()
                .onSuccess { code ->
                    _uiState.update { it.copy(myFriendCode = code) }
                }

            // Saber si tengo amigos aceptados -> habilitar creación de grupos
            friendsRepo.observeFriends("")
                .map { friends -> friends.isNotEmpty() }
                .distinctUntilChanged()
                .collect { has ->
                    _uiState.update { it.copy(hasFriends = has) }
                }
        }
    }

    /**
     * Cambia pestaña seleccionada.
     * @param tab Nueva pestaña.
     */
    fun onTabChanged(tab: FriendsTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    /**
     * Cambia el texto de búsqueda actual.
     * @param q Nuevo texto.
     */
    fun onQueryChanged(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    /**
     * Acción principal (botón) dependiendo de la pestaña actual.
     */
    fun onPrimaryActionClicked() {
        when (uiState.value.tab) {
            FriendsTab.FRIENDS -> viewModelScope.launch { _events.emit(FriendsUiEvent.OpenAddFriend) }
            FriendsTab.GROUPS -> viewModelScope.launch { _events.emit(FriendsUiEvent.OpenAddGroup) }
            FriendsTab.REQUESTS -> Unit
        }
    }

    /**
     * Click en un amigo: abre editar con el nickname actual.
     * @param item Entidad de amigo clicada.
     */
    fun onFriendClicked(item: FriendEntity) {
        viewModelScope.launch {
            _events.emit(FriendsUiEvent.OpenEditNickname(item.id, item.nickname))
        }
    }

    /**
     * Fuerza abrir el diálogo de edición de nickname.
     * @param friendId ID local del amigo.
     * @param currentNickname Nick actual si existe.
     */
    fun editNickname(friendId: Long, currentNickname: String?) {
        viewModelScope.launch {
            Log.d("LUDIARY_EDIT_DEBUG", "VM emit OpenEditNickname($friendId, $currentNickname)")
            _events.emit(FriendsUiEvent.OpenEditNickname(friendId, currentNickname))
        }
    }

    /**
     * Acepta una invitación de grupo entrante.
     * @param inviteId ID de invitación.
     */
    fun acceptGroupInvite(inviteId: String) {
        viewModelScope.launch {
            val r = groupsRepo.acceptInvite(inviteId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.groups_snack_invite_accepted
                    else friendlyErrorResId(r.exceptionOrNull())
                )
            )
        }
    }

    /**
     * Rechaza una invitación de grupo entrante.
     * @param inviteId ID de invitación.
     */
    fun rejectGroupInvite(inviteId: String) {
        viewModelScope.launch {
            val r = groupsRepo.rejectInvite(inviteId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.groups_snack_invite_rejected
                    else friendlyErrorResId(r.exceptionOrNull())
                )
            )
        }
    }

    /**
     * Guarda el nickname de un amigo.
     * @param friendId ID local del amigo.
     * @param nickname Nuevo apodo.
     */
    fun saveNickname(friendId: Long, nickname: String) {
        viewModelScope.launch {
            friendsRepo.updateNickname(friendId, nickname)
                .onSuccess { _events.emit(FriendsUiEvent.ShowSnack(R.string.friends_snack_nickname_updated)) }
                .onFailure { _events.emit(FriendsUiEvent.ShowSnack(R.string.friends_snack_nickname_update_failed)) }
        }
    }

    /**
     * Crea una solicitud saliente por friend code (offline-first).
     * @param code Código introducido por el usuario.
     */
    fun sendInviteByCode(code: String) {
        viewModelScope.launch {
            val r = friendsRepo.sendInviteByCode(code)
            if (r.isSuccess) {
                _events.emit(FriendsUiEvent.ShowSnack(R.string.profile_friend_message))
                friendsRepo.flushOfflineInvites()
            } else {
                _events.emit(FriendsUiEvent.ShowSnack(friendlyErrorResId(r.exceptionOrNull())))
            }
        }
    }

    /**
     * Acepta una solicitud entrante de amistad.
     * @param friendId ID local (Room) del registro.
     */
    fun acceptRequest(friendId: Long) {
        viewModelScope.launch {
            val r = friendsRepo.acceptRequest(friendId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.friends_snack_request_accepted else friendlyErrorResId(r.exceptionOrNull())
                )
            )
        }
    }

    /**
     * Rechaza una solicitud.
     * - Si es entrante: rechaza.
     * - Si es saliente: se usa como “cancelar”.
     *
     * @param friendId ID local (Room) del registro.
     */
    fun rejectRequest(friendId: Long) {
        viewModelScope.launch {
            val r = friendsRepo.rejectRequest(friendId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.friends_snack_request_rejected else friendlyErrorResId(r.exceptionOrNull())
                )
            )
        }
    }

    /**
     * Detiene listeners realtime.
     */
    fun stop() {
        started = false
        friendsRepo.stopRemoteSync()
        groupsRepo.stopRemoteSync()
    }

    /**
     * Elimina un amigo confirmado.
     * @param friendId ID local (Room).
     */
    fun removeFriend(friendId: Long) = viewModelScope.launch {
        friendsRepo.removeFriend(friendId)
            .onSuccess { _events.emit(FriendsUiEvent.ShowSnack(R.string.friends_snack_removed)) }
            .onFailure { e ->
                e.printStackTrace()
                _events.emit(FriendsUiEvent.ShowSnack(R.string.friends_snack_remove_failed))
            }
    }

    /**
     * Crea un grupo con el nombre indicado.
     * @param name Nombre del grupo.
     */
    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _events.emit(FriendsUiEvent.ShowSnack(R.string.groups_snack_name_required)) }
            return
        }

        viewModelScope.launch {
            val r = groupsRepo.createGroup(trimmed)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.groups_snack_created
                    else R.string.groups_snack_create_failed
                )
            )
        }
    }

    /**
     * Observa miembros de un grupo
     * @param groupId ID del grupo.
     * @return Flow con los miembros del grupo.
     */
    fun groupMembers(groupId: String) = groupsRepo.observeMembers(groupId)

    /**
     * Envía invitación a un grupo (offline-first).
     * @param groupId ID del grupo.
     * @param groupNameSnapshot Nombre snapshot del grupo.
     * @param toUid UID del destinatario.
     */
    fun inviteToGroup(groupId: String, groupNameSnapshot: String, toUid: String) {
        viewModelScope.launch {
            val r = groupsRepo.inviteToGroup(groupId, groupNameSnapshot, toUid)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.groups_snack_invite_sent
                    else R.string.groups_snack_invite_pending_offline
                )
            )
        }
    }

    /**
     * Cancela una invitación de grupo (saliente).
     * @param inviteId ID de invitación.
     */
    fun cancelGroupInvite(inviteId: String) {
        viewModelScope.launch {
            val r = groupsRepo.cancelInvite(inviteId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.groups_snack_invite_cancelled
                    else friendlyErrorResId(r.exceptionOrNull())
                )
            )
        }
    }

    /**
     * Devuelve filas para mostrar grupos en UI (con membersCount).
     */
    fun groupRows(): Flow<List<GroupRowUi>> =
        groupItems().map { groups ->
            groups.map { g -> GroupRowUi(g, g.membersCount) }
        }

    /**
     * Abandona un grupo.
     * @param groupId ID del grupo.
     */
    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            val r = groupsRepo.leaveGroup(groupId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) R.string.groups_snack_left
                    else friendlyErrorResId(r.exceptionOrNull())
                )
            )
        }
    }

    /**
     * Lista de invitaciones salientes pendientes asociadas a un grupo.
     * @param groupId ID del grupo.
     */
    suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity> =
        groupsRepo.pendingOutgoingInvitesForGroup(groupId)

    /**
     * Snapshot puntual de miembros de un grupo.
     * @param groupId ID del grupo.
     */
    suspend fun groupMembersOnce(groupId: String) =
        groupsRepo.observeMembers(groupId).first()

    /**
     * Snapshot puntual de amigos aceptados (para picker de invitación).
     */
    suspend fun friendsSnapshotForInvite(): List<FriendEntity> =
        friendsRepo.observeFriends("").first()

    /**
     * Lista principal según pestaña:
     * - FRIENDS: amigos aceptados
     * - GROUPS: grupos del usuario
     * - REQUESTS: aquí no se usa (Requests tiene requestRows())
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun items(tab: FriendsTab): Flow<List<FriendEntity>> {
        return uiState
            .map { it.query }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                when (tab) {
                    FriendsTab.FRIENDS -> friendsRepo.observeFriends(q)
                    FriendsTab.GROUPS -> flowOf(emptyList()) // (esta pantalla usa GroupsAdapter + groupRows())
                    FriendsTab.REQUESTS -> combine(
                        friendsRepo.observeIncomingRequests(q),
                        friendsRepo.observeOutgoingRequests(q)
                    ) { incoming, outgoing ->
                        incoming + outgoing
                    }
                }
            }
    }

    /**
     * Construye filas para la pestaña REQUESTS:
     * - Amigos entrantes/salientes
     * - Invitaciones de grupo entrantes/salientes
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun requestRows(): Flow<List<FriendRowUi>> {
        return uiState
            .map { it.query }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                combine(
                    friendsRepo.observeIncomingRequests(q),
                    friendsRepo.observeOutgoingRequests(q),
                    groupsRepo.observeIncomingPendingInvites(),
                    groupsRepo.observeOutgoingPendingInvites()
                ) { fin, fout, ginAll, goutAll ->

                    val rows = mutableListOf<FriendRowUi>()

                    if (fin.isNotEmpty()) {
                        rows += FriendRowUi.Header(R.string.requests_header_friends_incoming)
                        rows += fin.map { FriendRowUi.FriendItem(it) }
                    }

                    if (fout.isNotEmpty()) {
                        rows += FriendRowUi.Header(R.string.requests_header_friends_outgoing)
                        rows += fout.map { FriendRowUi.FriendItem(it) }
                    }

                    val ql = q.trim().lowercase()

                    val gin = if (ql.isBlank()) ginAll else ginAll.filter {
                        it.groupNameSnapshot.lowercase().contains(ql)
                    }

                    val gout = if (ql.isBlank()) goutAll else goutAll.filter {
                        it.groupNameSnapshot.lowercase().contains(ql)
                    }

                    if (gin.isNotEmpty()) {
                        rows += FriendRowUi.Header(R.string.requests_header_groups_incoming)
                        rows += gin.map { FriendRowUi.GroupItem(it, isOutgoing = false) }
                    }

                    if (gout.isNotEmpty()) {
                        rows += FriendRowUi.Header(R.string.requests_header_groups_outgoing)
                        rows += gout.map { FriendRowUi.GroupItem(it, isOutgoing = true) }
                    }

                    rows
                }
            }
    }

    /**
     * Observa grupos del usuario aplicando filtro por query.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun groupItems(): Flow<List<GroupEntity>> =
        uiState
            .map { it.query }
            .distinctUntilChanged()
            .flatMapLatest { q -> groupsRepo.observeGroups(q) }
}

/**
 * Convierte excepciones típicas en mensajes más entendibles para UI.
 */
@StringRes
private fun friendlyErrorResId(e: Throwable?): Int {
    val msg = e?.message?.lowercase().orEmpty()
    return when {
        msg.contains("permission") -> R.string.error_permission
        msg.contains("unavailable") || msg.contains("network") -> R.string.error_offline_retry
        msg.contains("not found") -> R.string.error_not_found_or_handled
        else -> R.string.error_generic
    }
}