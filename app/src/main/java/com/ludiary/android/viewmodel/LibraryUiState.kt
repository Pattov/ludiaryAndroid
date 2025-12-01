package com.ludiary.android.viewmodel

import com.ludiary.android.data.model.UserGame

/**
 * Estado inmutable que representa el contenido de la pantalla de Ludoteca.
 * @property isLoading Indica si se está cargando el contenido.
 * @property games Lista de juegos del usuario.
 * @property errorMessage Mensaje de error en caso de fallo.
 * @property isEmpty Indica si la lista de juegos está vacía.
 */
data class LibraryUiState(
    val isLoading: Boolean = true,
    val games: List<UserGame> = emptyList(),
    val errorMessage: String ?= null,
    val isEmpty: Boolean = false
)
