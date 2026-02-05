package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.repository.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de partidas.
 * @property loading Indica si se están cargando los datos.
 * @property sessions Lista de partidas del usuario.
 * @property errorRes Mensaje de error.
 */
data class SessionsUiState(
    val loading: Boolean = true,
    val sessions: List<SessionEntity> = emptyList(),
    val errorRes: Int? = null
)

class SessionsViewModel(
    private val db: LudiaryDatabase,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState

    fun start() {
        val uid = authRepo.currentUser?.uid

        if (uid.isNullOrBlank()) {
            _uiState.value = SessionsUiState(
                loading = false,
                sessions = emptyList(),
                errorRes = R.string.sessions_error_login_required
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                errorRes = null
            )

            db.sessionDao()
                .observePersonalSessions(uid, SessionScope.PERSONAL)
                .collectLatest { sessions ->
                    _uiState.value = SessionsUiState(
                        loading = false,
                        sessions = sessions,
                        errorRes = null
                    )
                }
        }
    }

    /**
     * Elimina una partida.
     * @param sessionId Identificador único de la sesión.
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            db.sessionDao().markSessionDeleted(
                sessionId = sessionId,
                now = System.currentTimeMillis()
            )
        }
    }
}
