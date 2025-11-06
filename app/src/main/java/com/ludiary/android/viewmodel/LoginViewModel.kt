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
 * Estado de la interfaz de usuario del proceso de login
 *
 * Contiene los valores de correo, contraseña y el estado de carga o errores que deben reflejarse en la vista
 * @property email Correo electrónico del usuario
 * @property password Contraseña del usuario
 * @property loading Indica si se está cargando la información
 * @property error Mensaje de error
 * @property success Indica si el inicio de sesión fue exitoso
 *
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

/**
 * ViewModel responsable de gestionar la lógica del login
 *
 * Interactua con AuthRepository para autenticar al usuario y actualiza el estado de la interfaz de usuario correspondiente.
 * Expone el estado para que Fragment y activity pueda observarlo
 */
class LoginViewModel(
    /** Repositorio de autenticación usado para acceder a Firebase */
    private val repo: AuthRepository
):ViewModel() {
    /** Estado interno mutable del login */
    private val _ui = MutableStateFlow(LoginUiState())
    /** Estado observable de la interfaz de usuario */
    val ui: StateFlow<LoginUiState> = _ui

    /**
     * Intenta iniciar sesión con las credenciales actuales
     *
     * Valida los datos y si son correctos, llama al respositorio.
     * Actualiza el estado de la interfaz de usuario con el resultado.
     */
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

    /**
     * Actualiza el campo con el valor introducido por el usuario
     * Reinicia el posible error previo.
     *
     * @param value Valor introducido por el usuario del campo de correo
     */
    fun onEmailChanged(value: String) {
        _ui.update { it.copy(email = value, error = null) }
    }

    /**
     * Actualiza el campo con el valor introducido por el usuario.
     * Reinicia el posible error previo.
     *
     * @param value Valor introducido por el usuario del campo de contraseña
     */
    fun onPasswordChanged(value: String) {
        _ui.update { it.copy(password = value, error = null) }
    }

    /**
     * Intenta iniciar sesión con un usuario anónimo
     *
     * Llama al repositorio para iniciar sesión con un usuario anónimo.
     */
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

    /**
     * Envía una solicitud de restablecimiento de contraseña a un usuario
     *
     * Valida que el correo no esté vacío.
     */
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

    /**
     * Valida el formato del correo y la contraseña
     *
     * @param email Correo electrónico del usuario
     * @param pass Contraseña del usuario
     * @return Mensaje de error si los datos no son válidos, null en caso contrario
     */
    private fun validate(email: String, pass: String): String? {
        if (email.isEmpty()) return "Introduce el correo."
        val ok = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!ok) return "Correo no válido."
        if (pass.isEmpty()) return "Introduce la contraseña."
        return null
    }
}