package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.model.User
import com.ludiary.android.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val user: User? = null
)

class ProfileViewModel(
    private val repo: ProfileRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    init { loadProfile() }

    fun loadProfile() = viewModelScope.launch {
        _ui.value = ProfileUiState(loading = true)
        runCatching { repo.getOrCreate() }
            .onSuccess { user -> _ui.value = ProfileUiState(loading = false, user = user )}
            .onFailure { e -> _ui.value = ProfileUiState(loading = false,error = e.message) }
    }

    fun save(displayName: String?) = viewModelScope.launch {
        val current = _ui.value.user
        _ui.value = _ui.value.copy(loading = true)

        runCatching{ repo.update(displayName, language = null, theme = null) }
            .onSuccess{ updated -> _ui.value = ProfileUiState(loading = false, user = updated) }
            .onFailure { e -> _ui.value = _ui.value.copy(loading = false, error = e.message, user = current) }
    }

    fun logout() = viewModelScope.launch { repo.signOut() }
}