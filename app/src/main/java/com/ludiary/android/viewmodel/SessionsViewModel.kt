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

data class SessionsUiState(
    val loading: Boolean = true,
    val sessions: List<SessionEntity> = emptyList(),
    val error: String? = null
)

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
                .observePersonalSessions(uid = uid, scope = SessionScope.PERSONAL)
                .collectLatest { list ->
                    _uiState.value = SessionsUiState(
                        loading = false,
                        sessions = list,
                        error = null
                    )
                }
        }
    }
}

class SessionsViewModelFactory(
    private val db: LudiaryDatabase,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SessionsViewModel(db, auth) as T
    }
}
