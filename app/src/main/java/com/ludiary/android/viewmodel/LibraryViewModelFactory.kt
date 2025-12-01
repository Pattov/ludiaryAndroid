package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.ludiary.android.data.repository.UserGamesRepository

/**
 * Factory para crear [LibraryViewModel].
 * @param uid Identificador único del usuario.
 * @param repository Repositorio de juegos del usuario.
 */
class LibraryViewModelFactory(
    private val uid: String,
    private val repository: UserGamesRepository
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [LibraryViewModel].
     * @param modelClass Clase del ViewModel.
     * @return Instancia de [LibraryViewModel].
     * @throws IllegalArgumentException Si la clase del ViewModel no es compatible.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(uid, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}