package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.sync.SyncPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de sincronización.
 * @param pendingCount Número de juegos pendientes de sincronización.
 * @param syncing Indica si se está sincronizando.
 * @param lastError Último error al sincronizar.
 * @return Instancia de [SyncUiState].
 * @throws Exception si ocurre un error al cargar las preferencias.
 */
data class SyncUiState(
    val pendingCount: Int = 0,
    val syncing: Boolean = false,
    val lastError: String? = null
)

/**
 * ViewModel para la pantalla de sincronización.
 * @param userGamesRepo Repositorio de juegos del usuario.
 * @return Instancia de [SyncViewModel].
 * @throws Exception si ocurre un error al cargar las preferencias.
 */
class SyncViewModel(
    private val appContext: Context,
    private val userGamesRepo: UserGamesRepository
): ViewModel() {

    private val _ui = MutableStateFlow(SyncUiState())
    val ui: StateFlow<SyncUiState> = _ui

    /**
     * Carga el número de juegos pendientes de sincronización.
     * @param uid Identificador único del usuario.
     * @return Instancia de [SyncViewModel].
     * @throws Exception si ocurre un error al cargar las preferencias.
     */
    fun loadPending(uid: String) {
        viewModelScope.launch {
            val count = userGamesRepo.countPending(uid)
            _ui.value = _ui.value.copy(pendingCount = count, lastError = null)
        }
    }

    /**
     * Sincroniza juegos pendientes.
     * @param uid Identificador único del usuario.
     * @param onSuccess Callback de éxito.
     * @return Instancia de [SyncViewModel].
     * @throws Exception si ocurre un error al cargar las preferencias.
     * @throws Exception si ocurre un error al sincronizar
     */
    fun syncNow(uid: String, onSuccess: (syncedUp: Int, appliedDown: Int) -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(syncing = true, lastError = null)

            try {
                val syncPrefs = SyncPrefs(appContext.applicationContext)

                val syncedUp = userGamesRepo.syncPending(uid)

                val lastPull = syncPrefs.getLastUserGamesPull(uid)
                val appliedDown = userGamesRepo.syncDownIncremental(uid, lastPull)

                syncPrefs.setLastUserGamesPull(uid, System.currentTimeMillis())

                val countAfter = userGamesRepo.countPending(uid)

                _ui.value = _ui.value.copy(
                    syncing = false,
                    pendingCount = countAfter,
                    lastError = null
                )

                onSuccess(syncedUp, appliedDown)
            }catch (e: Exception) {
                _ui.value = _ui.value.copy(syncing = false, lastError = e.message?: "Error desconocido")
            }
        }
    }
}