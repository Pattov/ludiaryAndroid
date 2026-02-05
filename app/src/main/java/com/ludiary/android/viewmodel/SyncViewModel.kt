package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.auth.AuthRepository
import com.ludiary.android.data.repository.library.UserGamesRepository
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.sync.SyncStatusPrefs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SyncUiState(
    val autoSyncEnabled: Boolean = true,
    val lastSyncMillis: Long = 0L,
    val pendingCount: Int = 0,
    val syncNowEnabled: Boolean = false,
    val errorMessage: String? = null
)

class SyncViewModel(
    private val appContext: Context,
    private val authRepo: AuthRepository,
    private val userGamesRepo: UserGamesRepository,
    private val statusPrefs: SyncStatusPrefs
) : ViewModel() {

    private val _ui = MutableStateFlow(SyncUiState())
    val ui: StateFlow<SyncUiState> = _ui

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events

    sealed class UiEvent {
        data class Toast(val message: String) : UiEvent()
    }

    fun start() {
        val autoEnabled = statusPrefs.isAutoSyncEnabled()
        val lastSync = statusPrefs.getLastSyncMillis()

        _ui.value = _ui.value.copy(
            autoSyncEnabled = autoEnabled,
            lastSyncMillis = lastSync,
            errorMessage = null
        )

        refreshPending()

        val ctx = appContext.applicationContext
        if (autoEnabled) {
            SyncScheduler.enableAutoSyncUserGames(ctx)
            SyncScheduler.enableAutoSyncSessions(ctx)
            SyncScheduler.enableAutoSyncFriendsGroups(ctx)
        } else {
            SyncScheduler.disableAutoSyncUserGames(ctx)
            SyncScheduler.disableAutoSyncSessions(ctx)
            SyncScheduler.disableAutoSyncFriendsGroups(ctx)
        }
    }

    fun onToggleAutoSync(enabled: Boolean) {
        val ctx = appContext.applicationContext

        statusPrefs.setAutoSyncEnabled(enabled)

        if (enabled) {
            SyncScheduler.enableAutoSyncUserGames(ctx)
            SyncScheduler.enableAutoSyncSessions(ctx)
            SyncScheduler.enableAutoSyncFriendsGroups(ctx)
        } else {
            SyncScheduler.disableAutoSyncUserGames(ctx)
            SyncScheduler.disableAutoSyncSessions(ctx)
            SyncScheduler.disableAutoSyncFriendsGroups(ctx)
        }

        _ui.value = _ui.value.copy(autoSyncEnabled = enabled, errorMessage = null)

        refreshPending()
    }

    fun onSyncNowClicked() {
        val uid = authRepo.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _events.tryEmit(UiEvent.Toast("Necesitas iniciar sesión para sincronizar."))
            return
        }

        val ctx = appContext.applicationContext

        SyncScheduler.enqueueOneTimeUserGamesSync(ctx)
        SyncScheduler.enqueueOneTimeSessionsSync(ctx)

        _events.tryEmit(UiEvent.Toast("Sincronización iniciada."))

        refreshPending()
    }

    private fun refreshPending() {
        val uid = authRepo.currentUser?.uid
        val autoEnabled = statusPrefs.isAutoSyncEnabled()

        if (uid.isNullOrBlank()) {
            _ui.value = _ui.value.copy(
                pendingCount = 0,
                syncNowEnabled = false,
                errorMessage = null
            )
            return
        }

        viewModelScope.launch {
            val pendingCount = runCatching { userGamesRepo.countPending(uid) }
                .getOrElse { 0 }

            val hasPending = pendingCount > 0

            _ui.value = _ui.value.copy(
                pendingCount = pendingCount,
                syncNowEnabled = !autoEnabled && hasPending,
                errorMessage = null
            )
        }
    }
}