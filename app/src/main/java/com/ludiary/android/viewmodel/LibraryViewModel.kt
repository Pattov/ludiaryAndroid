package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.UserGamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val uid: String,
    private val userGamesRepository: UserGamesRepository
) : ViewModel(){

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        loadUserGames()
    }

    private fun loadUserGames() {
        viewModelScope.launch {
            userGamesRepository.getUserGames(uid).collect { games ->
                _uiState.value = LibraryUiState(
                    games = games,
                    isLoading = false,
                    isEmpty = games.isEmpty(),
                    errorMessage = null
                )
            }
        }
    }

    fun onAddGameClicked(){ }
    fun onEditGameClicked(gameId: String){ }
    fun onDeleteGameClicked(gameId: String){
        viewModelScope.launch {
            userGamesRepository.deleteUserGame(uid, gameId)
        }
    }
}