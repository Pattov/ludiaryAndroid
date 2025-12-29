package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SyncUiState(
    val pendingCount: Int = 0,
    val lastError: String? = null
)

class SyncViewModel(
    private val appContext: Context,
    private val userGamesRepo: UserGamesRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SyncUiState())
    val ui: StateFlow<SyncUiState> = _ui

    fun loadPending(uid: String) {
        viewModelScope.launch {
            runCatching { userGamesRepo.countPending(uid) }
                .onSuccess { count ->
                    _ui.value = _ui.value.copy(pendingCount = count, lastError = null)
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(lastError = e.message ?: "Error desconocido")
                }
        }
    }

    /**
     * Sync manual: delega en WorkManager.
     * (No hace red desde el ViewModel.)
     */
    fun enqueueManualSync() {
        val ctx = appContext.applicationContext
        SyncScheduler.enqueueOneTimeUserGamesSync(ctx)
        SyncScheduler.enqueueOneTimeSessionsSync(ctx)
    }
}