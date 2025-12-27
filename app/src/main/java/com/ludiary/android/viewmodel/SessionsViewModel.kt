package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.model.SessionScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de sesiones.
 * @property loading Indica si se está cargando.
 * @property sessions Lista de sesiones.
 * @property error Mensaje de error.
 */
data class SessionsUiState(
    val loading: Boolean = true,
    val sessions: List<SessionEntity> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel para la pantalla de sesiones.
 * @property db Instancia de [LudiaryDatabase].
 * @property auth Instancia de [FirebaseAuth].
 * @return Instancia de [SessionsViewModel].
 */
class SessionsViewModel(
    private val db: LudiaryDatabase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState

    fun start() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.value = SessionsUiState(
                loading = false,
                sessions = emptyList(),
                error = "Necesitas iniciar sesión para ver tus partidas."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            db.sessionDao()
                .observePersonalSessions(uid, SessionScope.PERSONAL)
                .collectLatest { list: List<SessionEntity> ->
                    _uiState.value = SessionsUiState(
                        loading = false,
                        sessions = list,
                        error = null
                    )
                }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            db.sessionDao().markSessionDeleted(
                sessionId = sessionId,
                now = now
            )
        }
    }

}

/**
 * Factory para crear una instancia de [SessionsViewModel].
 * @property db Instancia de [LudiaryDatabase].
 * @property auth Instancia de [FirebaseAuth].
 * @return Instancia de [SessionsViewModelFactory].
 */
class SessionsViewModelFactory(
    private val db: LudiaryDatabase,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SessionsViewModel(db, auth) as T
    }
}
