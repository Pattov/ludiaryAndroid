package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.AuthRepository
import com.ludiary.android.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class LoginViewModel(private val repo: AuthRepository):ViewModel() {
    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui

    fun login() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password
        val err = validate(email, pass)
        if (err != null) { _ui.update { it.copy(error = err) }; return }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val res = repo.login(email, pass)) {
                is AuthResult.Success -> _ui.update { it.copy(loading = false, success = true) }
                is AuthResult.Error -> _ui.update { it.copy(loading = false, error = res.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun onEmailChanged(value: String) {
        _ui.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChanged(value: String) {
        _ui.update { it.copy(password = value, error = null) }
    }

    fun loginAnonymous(){
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when(val res = repo.loginAnonymously()) {
                is AuthResult.Success -> _ui.update { it.copy(loading = false, success = true) }
                is AuthResult.Error -> _ui.update { it.copy(loading = false, error = res.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun resetPassword(){
        val email = _ui.value.email.trim()
        if (email.isEmpty()) { _ui.update { it.copy(error = "Introduce el correo.") }; return }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val res = repo.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> _ui.update { it.copy(loading = false, success = true) }
                is AuthResult.Error -> _ui.update { it.copy(loading = false, error = res.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    private fun validate(email: String, pass: String): String? {
        if (email.isEmpty()) return "Introduce el correo."
        val ok = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!ok) return "Correo no válido."
        if (pass.isEmpty()) return "Introduce la contraseña."
        return null
    }
}