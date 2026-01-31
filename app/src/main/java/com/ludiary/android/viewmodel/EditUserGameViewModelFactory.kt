package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.repository.library.UserGamesRepository

/**
 * Factory para crear [EditUserGameViewModel].
 * @param uid Identificador único del usuario.
 * @param repository Repositorio de juegos del usuario.
 * @return Instancia de [EditUserGameViewModel].
 */
class EditUserGameViewModelFactory (
    private val uid: String,
    private val repository: UserGamesRepository
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [EditUserGameViewModel].
     * @param modelClass Clase del ViewModel.
     * @return Instancia de [EditUserGameViewModel].
     *
     * @throws IllegalArgumentException Si la clase del ViewModel no es compatible.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditUserGameViewModel::class.java)) {
            return EditUserGameViewModel(uid, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}