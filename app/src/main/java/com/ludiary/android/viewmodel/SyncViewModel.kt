package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.UserGamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SyncUiState(
    val pendingCount: Int = 0,
    val syncing: Boolean = false,
    val lastError: String? = null
)

class SyncViewModel(
    private val userGamesRepo: UserGamesRepository
): ViewModel() {

    private val _ui = MutableStateFlow(SyncUiState())
    val ui: StateFlow<SyncUiState> = _ui

    fun loadPending(uid: String) {
        viewModelScope.launch {
            val count = userGamesRepo.countPending(uid)
            _ui.value = _ui.value.copy(pendingCount = count, lastError = null)
        }
    }

    fun syncNow(uid: String, onSuccess: (syncedCount: Int) -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(syncing = true, lastError = null)

            try {
                val synced = userGamesRepo.syncPending(uid)
                val countAfter = userGamesRepo.countPending(uid)

                _ui.value = _ui.value.copy(
                    syncing = false,
                    pendingCount = countAfter,
                    lastError = null
                )

                onSuccess(synced)
            }catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    syncing = false, lastError = e.message?: "Error desconocido"
                )
            }
        }
    }
}