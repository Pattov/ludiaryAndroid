package com.ludiary.android.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.R
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
    val errorResId: Int? = null,
    val errorArgs: List<Any> = emptyList(),
    val success: Boolean = false
)

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow(RegisterUiState())
    val ui: StateFlow<RegisterUiState> = _ui

    fun onEmailChange(v: String) = _ui.update {
        it.copy(email = v, errorResId = null, errorArgs = emptyList())
    }

    fun onPasswordChange(v: String) = _ui.update {
        it.copy(password = v, errorResId = null, errorArgs = emptyList())
    }

    fun onConfirmChange(v: String) = _ui.update {
        it.copy(confirm = v, errorResId = null, errorArgs = emptyList())
    }

    fun register() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password
        val confirm = _ui.value.confirm

        val err = validate(email, pass, confirm)
        if (err != null) {
            _ui.update { it.copy(errorResId = err.first, errorArgs = err.second) }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, errorResId = null, errorArgs = emptyList()) }
            when (repo.register(email, pass)) {
                is AuthResult.Success -> _ui.update { it.copy(loading = false, success = true) }
                is AuthResult.Error -> _ui.update {
                    it.copy(
                        loading = false,
                        errorResId = R.string.auth_error_generic_register,
                        errorArgs = emptyList()
                    )
                }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    private fun validate(email: String, pass: String, confirm: String): Pair<Int, List<Any>>? {
        if (email.isEmpty()) return R.string.register_error_email_required to emptyList()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return R.string.register_error_email_invalid to emptyList()
        }
        if (pass.length < 6) return R.string.register_error_password_min to listOf(6)
        if (pass != confirm) return R.string.register_error_password_mismatch to emptyList()
        return null
    }
}