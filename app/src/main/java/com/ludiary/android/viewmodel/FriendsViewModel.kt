package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.FriendsRepository
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

    fun onMenuAddFriendClicked() {
        viewModelScope.launch { _events.emit(FriendsUiEvent.OpenAddFriend) }
    }

    fun onFriendClicked(item: FriendEntity) {
        viewModelScope.launch { _events.emit(FriendsUiEvent.OpenEditNickname(item.id)) }
    }

    fun sendInviteByCode(code: String) {
        viewModelScope.launch {
            val r = repo.sendInviteByCode(code)
            if (r.isSuccess) {
                _events.emit(
                    FriendsUiEvent.ShowSnack("Si el usuario existe, recibirá tu solicitud.")
                )
            } else {
                _events.emit(FriendsUiEvent.ShowSnack(r.exceptionOrNull()?.message ?: "Error"))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun items(tab: FriendsTab): Flow<List<FriendEntity>> {
        return uiState
            .map { it.query }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                when (tab) {
                    FriendsTab.FRIENDS -> repo.observeFriends(q)
                    FriendsTab.GROUPS -> repo.observeGroups(q)
                    FriendsTab.REQUESTS -> repo.observeIncomingRequests(q)
                }
            }
    }
}