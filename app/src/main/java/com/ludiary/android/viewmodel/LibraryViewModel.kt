package com.ludiary.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.data.repository.GameBaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de biblioteca.
 *
 * @param uid Identificador único del usuario.
 * @param userGamesRepository Repositorio de juegos del usuario.
 * @param gameBaseRepository Repositorio de catálogo de juegos.
 * @param syncCatalogAutomatically Indica si el catálogo se sincronizará automáticamente.
 */
class LibraryViewModel(
    private val uid: String,
    private val userGamesRepository: UserGamesRepository,
    private val gameBaseRepository: GameBaseRepository,
    private val syncCatalogAutomatically: Boolean = true
) : ViewModel(){

    /**
     * Estado de la pantalla.
     */
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        loadUserGames()

        if (syncCatalogAutomatically) {
            syncCatalog()
        }
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

    /**
     * Sincroniza el catálogo de juegos.
     * @param forceFullSync Indica si se sincronizará el catálogo completo.
     */
    fun syncCatalog(forceFullSync: Boolean = false) {
        viewModelScope.launch {
            try {
                gameBaseRepository.syncGamesBase(forceFullSync)
            } catch (e: Exception) {
                Log.e("LUDIARY", "Error syncing catalog", e)
            }
        }
    }
}