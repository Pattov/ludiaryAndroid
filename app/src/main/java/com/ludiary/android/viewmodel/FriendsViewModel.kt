package com.ludiary.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.FriendsRepository
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FriendsUiState(
    val tab: FriendsTab = FriendsTab.FRIENDS,
    val query: String = ""
)

sealed class FriendsUiEvent {
    data class ShowSnack(val message: String) : FriendsUiEvent()
    object OpenAddFriend : FriendsUiEvent()
    object OpenAddGroup : FriendsUiEvent()
    data class OpenEditNickname(val friendId: Long) : FriendsUiEvent()
}

class FriendsViewModel(
    private val repo: FriendsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FriendsUiEvent>()
    val events: SharedFlow<FriendsUiEvent> = _events.asSharedFlow()

    fun start() {
        (repo as? FriendsRepositoryImpl)?.startRemoteSync()

        viewModelScope.launch {
            repo.flushOfflineInvites()
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

    fun onFriendClicked(item: FriendEntity) {
        viewModelScope.launch { _events.emit(FriendsUiEvent.OpenEditNickname(item.id)) }
    }

    fun sendInviteByCode(code: String) {
        viewModelScope.launch {
            val r = repo.sendInviteByCode(code)
            if (r.isSuccess) {
                _events.emit(FriendsUiEvent.ShowSnack("Si el usuario existe, recibirá tu solicitud."))
                repo.flushOfflineInvites()
            } else {
                _events.emit(FriendsUiEvent.ShowSnack(r.exceptionOrNull()?.message ?: "Error"))
            }
        }
    }

    fun acceptRequest(friendId: Long) {
        viewModelScope.launch {
            val r = repo.acceptRequest(friendId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Solicitud aceptada" else (r.exceptionOrNull()?.message ?: "Error")
                )
            )
        }
    }

    fun rejectRequest(friendId: Long) {
        viewModelScope.launch {
            val r = repo.rejectRequest(friendId)
            _events.emit(
                FriendsUiEvent.ShowSnack(
                    if (r.isSuccess) "Solicitud rechazada" else (r.exceptionOrNull()?.message ?: "Error")
                )
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun items(tab: FriendsTab): Flow<List<FriendEntity>> {
        return uiState
            .map { it.query }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                Log.d("LUDIARY_FRIENDS_DEBUG", "VM.items() tab=$tab query='$q'")

                when (tab) {
                    FriendsTab.FRIENDS -> repo.observeFriends(q)
                    FriendsTab.GROUPS -> repo.observeGroups(q)
                    FriendsTab.REQUESTS -> combine(
                        repo.observeIncomingRequests(q),
                        repo.observeOutgoingRequests(q)
                    ) { incoming, outgoing ->
                        Log.d(
                            "LUDIARY_FRIENDS_DEBUG",
                            "REQUESTS incoming=${incoming.size} outgoing=${outgoing.size}"
                        )
                        incoming + outgoing
                    }
                }
            }
    }

}