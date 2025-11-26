package com.ludiary.android.viewmodel

import com.ludiary.android.data.model.UserGame

data class LibraryUiState(
    val isLoading: Boolean = true,
    val games: List<UserGame> = emptyList(),
    val errorMessage: String ?= null,
    val isEmpty: Boolean = false
)
