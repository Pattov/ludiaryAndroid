package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.UserGamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de biblioteca.
 */
class LibraryViewModel(
    private val uid: String,
    private val userGamesRepository: UserGamesRepository
) : ViewModel(){

    /**
     * Estado de la pantalla.
     */
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        loadUserGames()
    }

    /**
     * Carga la lista de juegos del usuario.
     */
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

    /**
     * Borra un juego del usuario.
     * @param gameId Identificador único del juego.
     */
    fun onDeleteGameClicked(gameId: String){
        viewModelScope.launch {
            userGamesRepository.deleteUserGame(uid, gameId)
        }
    }
}