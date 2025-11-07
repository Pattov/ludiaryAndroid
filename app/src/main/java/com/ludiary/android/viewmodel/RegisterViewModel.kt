package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.AuthRepository
import com.ludiary.android.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(RegisterUiState())
    val ui: StateFlow<RegisterUiState> = _ui

    fun onEmailChange(v: String) = _ui.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _ui.update { it.copy(password = v, error = null) }
    fun onConfirmChange(v: String) = _ui.update { it.copy(confirm = v, error = null) }

    fun register() {
        val (email, pass, confirm) = _ui.value.let { it.email.trim() to it.password to it.confirm }
        val err = validate(email, pass, confirm)
        if (err != null) {
            _ui.update { it.copy(error = err) };
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val res = repo.register(email, pass)) {
                is AuthResult.Success -> _ui.update { it.copy(loading = false, success = true) }
                is AuthResult.Error -> _ui.update { it.copy(loading = false, error = res.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    private fun validate(email: String, pass: String, confirm: String): String? {
        if (email.isEmpty()) return "Introduce el correo."
        val ok = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!ok) return "Correo inválido."
        if (pass.length < 6) return "La contraseña debe tener al menos 6 caracteres."
        if (pass != confirm) return "Las contraseñas no coinciden."
        return null
    }
}