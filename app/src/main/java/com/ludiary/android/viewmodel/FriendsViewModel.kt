package com.ludiary.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.FriendsRepository
import com.ludiary.android.data.repository.GroupsRepository
import com.ludiary.android.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FriendsUiState(
    val tab: FriendsTab = FriendsTab.FRIENDS,
    val query: String = "",
    val myFriendCode: String? = null,
    val hasFriends: Boolean = false
)

data class GroupRowUi(
    val group: GroupEntity,
    val membersCount: Int
)

sealed class FriendsUiEvent {
    data class OpenEditNickname(val friendId: Long, val currentNickname: String?) : FriendsUiEvent()
    data class ShowSnack(val message: String) : FriendsUiEvent()
    object OpenAddFriend : FriendsUiEvent()
    object OpenAddGroup : FriendsUiEvent()
}

sealed class FriendRowUi {
    data class Header(val title: String) : FriendRowUi()
    data class FriendItem(val friend: FriendEntity) : FriendRowUi()
    data class GroupItem(val invite: GroupInviteEntity, val isOutgoing: Boolean) : FriendRowUi()
}

class FriendsViewModel(
    private val friendsRepo: FriendsRepository,
    private val groupsRepo: GroupsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()
    private var started = false

    private val _events = MutableSharedFlow<FriendsUiEvent>()
    val events: SharedFlow<FriendsUiEvent> = _events.asSharedFlow()

    fun start() {
        if (started) return
        started = true

        friendsRepo.startRemoteSync()
        groupsRepo.startRemoteSync()

        viewModelScope.launch {
            groupsRepo.flushPendingInvites()

            friendsRepo.getMyFriendCode()
                .onSuccess { code -> _uiState.update { it.copy(myFriendCode = code) } }

            friendsRepo.observeFriends("")
                .map { friends -> friends.size >= 1 }
                .distinctUntilChanged()
                .collect { has ->
                    _uiState.update { it.copy(hasFriends = has) }
                }
        }
    }

    fun onTabChanged(tab: FriendsTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun onQueryChanged(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun onPrimaryActionClicked() {
        when (uiState.value.tab) {
            FriendsTab.FRIENDS -> viewModelScope.launch { _events.emit(FriendsUiEvent.OpenAddFriend) }
            FriendsTab.GROUPS -> viewModelScope.launch { _events.emit(FriendsUiEvent.OpenAddGroup) }
            FriendsTab.REQUESTS -> Unit
        }
    }

    // ✅ Click en fila (si lo usas): abre editar con el nickname actual si existe
    fun onFriendClicked(item: FriendEntity) {
        viewModelScope.launch {
            _events.emit(FriendsUiEvent.OpenEditNickname(item.id, item.nickname))
        }
    }

    // ✅ ÚNICA función de editar: siempre con (id, nickname?)
    fun editNickname(friendId: Long, currentNickname: String?) {
        viewModelScope.launch {
            Log.d("LUDIARY_EDIT_DEBUG", "VM emit OpenEditNickname($friendId, $currentNickname)")
            _events.emit(FriendsUiEvent.OpenEditNickname(friendId, currentNickname))
        }
    }

    fun acceptGroupInvite(inviteId: String) {
        viewModelScope.launch {
            val r = groupsRepo.acceptInvite(inviteId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Invitación de grupo aceptada"
                    else friendlyError(r.exceptionOrNull())
                )
            )
        }
    }

    fun rejectGroupInvite(inviteId: String) {
        viewModelScope.launch {
            val r = groupsRepo.rejectInvite(inviteId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Invitación de grupo rechazada"
                    else friendlyError(r.exceptionOrNull())
                )
            )
        }
    }

    fun saveNickname(friendId: Long, nickname: String) {
        viewModelScope.launch {
            friendsRepo.updateNickname(friendId, nickname)
                .onSuccess { _events.emit(FriendsUiEvent.ShowSnack("Apodo actualizado")) }
                .onFailure { _events.emit(FriendsUiEvent.ShowSnack("No se pudo actualizar")) }
        }
    }

    fun sendInviteByCode(code: String) {
        viewModelScope.launch {
            val r = friendsRepo.sendInviteByCode(code)
            if (r.isSuccess) {
                _events.emit(FriendsUiEvent.ShowSnack("Si el usuario existe, recibirá tu solicitud."))
                friendsRepo.flushOfflineInvites()
            } else {
                _events.emit(FriendsUiEvent.ShowSnack(r.exceptionOrNull()?.message ?: "Error"))
            }
        }
    }

    fun acceptRequest(friendId: Long) {
        viewModelScope.launch {
            val r = friendsRepo.acceptRequest(friendId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Solicitud aceptada" else friendlyError(r.exceptionOrNull())
                )
            )
        }
    }

    fun rejectRequest(friendId: Long) {
        viewModelScope.launch {
            val r = friendsRepo.rejectRequest(friendId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Solicitud rechazada" else friendlyError(r.exceptionOrNull())
                )
            )
        }
    }

    fun stop() {
        started = false
        friendsRepo.stopRemoteSync()
        groupsRepo.stopRemoteSync()
    }

    fun removeFriend(friendId: Long) = viewModelScope.launch {
        friendsRepo.removeFriend(friendId)
            .onSuccess { _events.emit(FriendsUiEvent.ShowSnack("Amigo eliminado")) }
            .onFailure { _events.emit(FriendsUiEvent.ShowSnack("No se pudo eliminar")) }
    }

    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _events.emit(FriendsUiEvent.ShowSnack("Pon un nombre de grupo")) }
            return
        }

        viewModelScope.launch {
            val r = groupsRepo.createGroup(trimmed)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Grupo creado" else (r.exceptionOrNull()?.message ?: "Error al crear grupo")
                )
            )
        }
    }

    fun groupMembers(groupId: String) = groupsRepo.observeMembers(groupId)

    fun inviteToGroup(groupId: String, groupNameSnapshot: String, toUid: String) {
        viewModelScope.launch {
            val r = groupsRepo.inviteToGroup(groupId, groupNameSnapshot, toUid)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Invitación enviada"
                    else "Invitación pendiente (sin conexión)"
                )
            )
        }
    }

    fun cancelGroupInvite(inviteId: String) {
        viewModelScope.launch {
            val r = groupsRepo.cancelInvite(inviteId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Invitación cancelada"
                    else friendlyError(r.exceptionOrNull())
                )
            )
        }
    }

    fun groupRows(): Flow<List<GroupRowUi>> {
        return groupItems().flatMapLatest { groups ->
            if (groups.isEmpty()) return@flatMapLatest flowOf(emptyList())

            // combinamos counts de miembros por grupo
            val flows = groups.map { g ->
                groupsRepo.observeMembers(g.groupId)
                    .map { members -> GroupRowUi(g, members.size) }
            }

            combine(flows) { it.toList() }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            val r = groupsRepo.leaveGroup(groupId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Has salido del grupo"
                    else friendlyError(r.exceptionOrNull())
                )
            )
        }
    }

    suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity> {
        return groupsRepo.pendingOutgoingInvitesForGroup(groupId)
    }

    suspend fun groupMembersOnce(groupId: String) =
        groupsRepo.observeMembers(groupId).first()


    // Para el picker: lista de amigos aceptados sin filtrar por búsqueda (query vacía)
    suspend fun friendsSnapshotForInvite(): List<FriendEntity> {
        // Si tu repo.observeFriends("") es Flow, necesitamos el first()
        return friendsRepo.observeFriends("").first()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun items(tab: FriendsTab): Flow<List<FriendEntity>> {
        return uiState
            .map { it.query }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                when (tab) {
                    FriendsTab.FRIENDS -> friendsRepo.observeFriends(q)
                    FriendsTab.GROUPS -> flowOf(emptyList())
                    FriendsTab.REQUESTS -> combine(
                        friendsRepo.observeIncomingRequests(q),
                        friendsRepo.observeOutgoingRequests(q)
                    ) { incoming, outgoing ->
                        incoming + outgoing
                    }
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun requestRows(): Flow<List<FriendRowUi>> {
        return uiState
            .map { it.query }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                combine(
                    friendsRepo.observeIncomingRequests(q),
                    friendsRepo.observeOutgoingRequests(q),
                    groupsRepo.observePendingInvites(),
                    groupsRepo.observeOutgoingPendingInvites()
                ) { fin, fout, ginAll, goutAll ->

                    val rows = mutableListOf<FriendRowUi>()

                    if (fin.isNotEmpty()) {
                        rows += FriendRowUi.Header("Amigos · Recibidas")
                        rows += fin.map { FriendRowUi.FriendItem(it) }
                    }

                    if (fout.isNotEmpty()) {
                        rows += FriendRowUi.Header("Amigos · Enviadas")
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
                        rows += FriendRowUi.Header("Grupos · Recibidas")
                        rows += gin.map { FriendRowUi.GroupItem(it, isOutgoing = false) }
                    }

                    if (gout.isNotEmpty()) {
                        rows += FriendRowUi.Header("Grupos · Enviadas")
                        rows += gout.map { FriendRowUi.GroupItem(it, isOutgoing = true) }
                    }

                    rows
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun groupItems(): Flow<List<GroupEntity>> =
        uiState.map { it.query }.distinctUntilChanged().flatMapLatest { q ->
            groupsRepo.observeGroups(q)
        }
}

private fun friendlyError(e: Throwable?): String {
    val msg = e?.message?.lowercase().orEmpty()
    return when {
        msg.contains("permission") -> "No tienes permisos para esta acción"
        msg.contains("unavailable") || msg.contains("network") -> "Sin conexión. Se reintentará automáticamente"
        msg.contains("not found") -> "Ya no existe o ya se gestionó"
        else -> e?.message ?: "Error"
    }
}