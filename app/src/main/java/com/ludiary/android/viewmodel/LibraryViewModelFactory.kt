package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.data.repository.GameBaseRepository

/**
 * Factory para crear LibraryViewModel con sus dependencias reales
 * @param uid Identificador único del usuario.
 * @param userGamesRepository Repositorio de juegos del usuario.
 * @param gameBaseRepository Repositorio de catálogo de juegos.
 * @param syncCatalogAutomatically Indica si el catálogo se sincronizará automáticamente.
 * @return [LibraryViewModel]
 */
class LibraryViewModelFactory(
    private val uid: String,
    private val userGamesRepository: UserGamesRepository,
    private val gameBaseRepository: GameBaseRepository,
    private val syncCatalogAutomatically: Boolean = true
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(
                uid = uid,
                userGamesRepository = userGamesRepository,
                gameBaseRepository = gameBaseRepository,
                syncCatalogAutomatically = syncCatalogAutomatically
            ) as T
        }
        throw IllegalArgumentException("Clase de ViewModel desconocida: ${modelClass.name}")
    }
}
