package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.repository.AuthRepository
import com.ludiary.android.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la interfaz de usuario de registro.
 *
 * @property email El correo electrónico del usuario.
 * @property password La contraseña del usuario.
 * @property confirm La confirmación de la contraseña del usuario.
 * @property loading Indica si la operación de registro está en proceso.
 * @property error El mensaje de error en caso de que ocurra uno.
 * @property success Indica si el registro fue exitoso.
 */
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

/**
 * ViewModel responsable de la lógica de registro de nuevos usuarios.
 *
 * Coordina las validaciones locales y las llamadas al [AuthRepository], exponiendo el estado [ui] para que la vista reaccione a los cambios.
 */
class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    /** Estado interno mutable del registro */
    private val _ui = MutableStateFlow(RegisterUiState())

    /** Estado observable que la vista debe observar */
    val ui: StateFlow<RegisterUiState> = _ui

    /** Actualiza el campo de correo y limpia un posible error previo. */
    fun onEmailChange(v: String) = _ui.update { it.copy(email = v, error = null) }

    /** Actualiza el campo de contraseña y limpia un posible error previo. */
    fun onPasswordChange(v: String) = _ui.update { it.copy(password = v, error = null) }

    /** Actualiza el campo de confirmación de contraseña y limpia un posible error previo. */
    fun onConfirmChange(v: String) = _ui.update { it.copy(confirm = v, error = null) }

    /**
     * Ejecuta el flujo de registro:
     * - Valida los campos de entrada.
     * - Llama al repositorio para realizar el registro.
     * - Actualiza el estado de la interfaz de usuario según el resultado.
     */
    fun register() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password
        val confirm = _ui.value.confirm

        val err = validate(email, pass, confirm)
        if (err != null) {
            _ui.update { it.copy(error = err) }
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

    /**
     * Valida los campos de entrada del registro.
     *
     * @param email El correo electrónico del usuario.
     * @param pass La contraseña del usuario.
     * @param confirm La confirmación de la contraseña del usuario.
     * @return Un mensaje de error si los campos son inválidos, `null` en caso contrario.
     */
    private fun validate(email: String, pass: String, confirm: String): String? {
        if (email.isEmpty()) return "Introduce el correo."
        val ok = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!ok) return "Correo inválido."
        if (pass.length < 6) return "La contraseña debe tener al menos 6 caracteres."
        if (pass != confirm) return "Las contraseñas no coinciden."
        return null
    }
}