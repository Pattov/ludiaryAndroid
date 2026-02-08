package com.ludiary.android.viewmodel

import com.ludiary.android.data.model.UserGame

/**
 * Filtro aplicado a la ludoteca.
 */
sealed class LibraryFilter {
    object All : LibraryFilter()
    object Mine : LibraryFilter()
    data class Group(val groupId: String, val groupName: String) : LibraryFilter()
}

/**
 * Elemento de visualización en la ludoteca (puede ser mío o de un compañero).
 */
data class LibraryItem(
    val game: UserGame,
    val ownerName: String? = null,
    val groupName: String? = null
)

/**
 * Estado inmutable que representa el contenido de la pantalla de Ludoteca.
 * @property isLoading Indica si se está cargando el contenido.
 * @property items Lista de juegos a mostrar (wrappers).
 * @property filter Filtro actual aplicado.
 * @property availableGroups Lista de grupos disponibles para filtrar (ID -> Nombre).
 * @property errorMessage Mensaje de error en caso de fallo.
 * @property isEmpty Indica si la lista de juegos está vacía.
 */
data class LibraryUiState(
    val isLoading: Boolean = true,
    val items: List<LibraryItem> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.All,
    val availableGroups: List<Pair<String, String>> = emptyList(),
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)