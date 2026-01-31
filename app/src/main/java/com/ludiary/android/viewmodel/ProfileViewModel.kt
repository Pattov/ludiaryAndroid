package com.ludiary.android.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.model.User
import com.ludiary.android.data.repository.profile.ProfileRepository
import com.ludiary.android.data.repository.library.UserGamesRepository
import com.ludiary.android.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsable de gestionar la lógica del perfil del usuario.
 * Actúa como intermediario entre [com.ludiary.android.ui.profile.ProfileFragment] y [ProfileRepository]
 */

/**
 * Modelo inmutable expuesto para la UI.
 *
 * @property loading Indica si se está cargando el perfil.
 * @property error Mensaje de error, si ocurre uno.
 * @property user Información del perfil del usuario.
 */
data class ProfileUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: User? = null
)

/**
 * ViewModel asociado al perfil del usuario.
 *
 * @param repo Implementación de [ProfileRepository] que se utiliza para interactuar con los dos repositorios.
 */
class ProfileViewModel(
    private val repo: ProfileRepository,
    private val userGamesRepo: UserGamesRepository,
    private val localUserGames: LocalUserGamesDataSource,
    private val localFriends: LocalFriendsDataSource
) : ViewModel() {

    /**
     * Estado interno mutable.
     * Nunca se expone a la vista directamente.
     */
    private val _ui = MutableStateFlow(ProfileUiState(loading = true))

    /**
     * Estado observable por la UI.
     * La vista observa este StateFlow y se actualiza automáticamente.
     */
    val ui: StateFlow<ProfileUiState> = _ui

    /**
     * Carga inicial del perfil al crear el ViewModel.
     */
    init { loadProfile() }

    /**
     * Obtiene el perfil del usuario
     *
     * - Si no hay sesión Firebase -> devuelve usuario local invitado en Room.
     * - Si hay sesión Firebase -> obtiene el documento en Firestore.
     * - Si falla por falta de conexión -> Funciona con el almacenamiento de Room.
     */
    fun loadProfile() = viewModelScope.launch {
        _ui.value = ProfileUiState(loading = true)
        runCatching { repo.getOrCreate() }
            .onSuccess { user -> _ui.value = ProfileUiState(loading = false, user = user, error = null )}
            .onFailure { e -> _ui.value = ProfileUiState(loading = false, user = null, error = e.message) }
    }

    /**
     * Cierra la sesión del usuario Firebase.
     * @param onDone Callback a ejecutar al terminar.
     */
    fun logout(context: Context, onDone: () -> Unit = {}) = viewModelScope.launch {
        val user = ui.value.user
        if (user == null) {
            onDone()
            return@launch
        }

        SyncScheduler.disableAutoSyncUserGames(context.applicationContext)
        SyncScheduler.disableAutoSyncSessions(context.applicationContext)
        // 2026 Añadir aquí las nuevas sincro

        if (!user.isAnonymous) {
            runCatching {
                val pending = userGamesRepo.countPending(user.uid)
                if (pending > 0) userGamesRepo.syncPending(user.uid)
            }.onFailure {
                Log.w("LUDIARY_LOGOUT", "No se pudo sincronizar antes de salir: ${it.message}")
            }
        }

        runCatching { repo.signOut() }
            .onFailure { Log.e("LUDIARY_LOGOUT", "Error en signOut: ${it.message}", it) }

        runCatching { localUserGames.clearUser(user.uid) }
            .onFailure { Log.w("LUDIARY_LOGOUT", "No se pudo limpiar Room: ${it.message}") }

        runCatching { localFriends.clearAll()}
            .onFailure { Log.w("LUDIARY_LOGOUT", "No se pudo limpiar friends en Room: ${it.message}")
        }

        onDone()
    }


    /**
     * Actualiza únicamente las preferencias (idioma/tema) del usuario.
     * @param language Idioma del usuario.
     * @param theme Tema del usuario.
     */
    fun updatePreferences(language: String?, theme: String?) {
        val current = _ui.value.user

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)

            runCatching { repo.update(displayName = null, language = language, theme = theme) }
                .onSuccess { updated -> _ui.value = ProfileUiState(loading = false, user = updated, error = null) }
                .onFailure { e -> _ui.value = _ui.value.copy(loading = false, error = e.message, user = current) }
        }
    }

    /**
     * Actualiza el alias del usuario.
     */
    fun save(displayName: String?) = viewModelScope.launch {
        val current = _ui.value.user
        _ui.value = _ui.value.copy(loading = true)

        runCatching { repo.update(displayName = displayName, language = null, theme = null) }
            .onSuccess { updated -> _ui.value = ProfileUiState(loading = false, user = updated, error = null) }
            .onFailure { e -> _ui.value = _ui.value.copy(loading = false, error = e.message, user = current ) }
    }
}