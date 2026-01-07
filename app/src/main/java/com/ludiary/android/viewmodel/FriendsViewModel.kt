package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class FriendsUiState(
    val query: String = ""
)

sealed class FriendsUiEvent {
    data class OpenEditNickname(val friendId: Long) : FriendsUiEvent()
    data class ShowSnack(val message: String) : FriendsUiEvent()
}

class FriendsViewModel(
    private val repo: FriendsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FriendsUiEvent>()

    fun onQueryChanged(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    /** Lista reactiva para una pestaña concreta
     * @param tab Tab a mostrar
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun items(tab: FriendsTab): kotlinx.coroutines.flow.Flow<List<com.ludiary.android.data.local.entity.FriendEntity>> {
        return uiState
            .map { it.query }
            .flatMapLatest { q ->
                when (tab) {
                    FriendsTab.FRIENDS -> repo.observeFriends(q)
                    FriendsTab.GROUPS -> repo.observeGroups(q)
                    FriendsTab.REQUESTS -> repo.observeIncomingRequests(q)
                }
            }
    }

    fun onFriendClicked(friend: FriendEntity) {
        viewModelScope.launch {
            _events.emit(FriendsUiEvent.OpenEditNickname(friend.id))
        }
    }

    fun onAddFriendClicked() {
        viewModelScope.launch {
            _events.emit(FriendsUiEvent.ShowSnack("TODO: Abrir 'Añadir amigo'"))
        }
    }

    fun onAddGroupClicked() {
        // TODO: abrir modal/diálogo "Añadir grupo"
    }

}