package com.ludiary.android.data.repository

import com.ludiary.android.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Representa el resultado de una operación de autenticación
 *
 * Puede ser uno de los siguientes estados:
 * - Loading: La operación de autenticación está en proceso
 * - Success: La autenticación se realizó con éxito
 * - Error: Ocurrió un error durante la autenticación
 */
sealed class AuthResult{
    /** Estado de carga mientras se ejecuta la operación. */
    data object Loading : AuthResult()
    /** Estado de éxito. */
    data class Success(val user: User) : AuthResult()
    /** Estado de error. */
    data class Error(val message: String) : AuthResult()
}

/**
 * Interfaz que define las operaciones de autenticación.
 *
 * Este repositorio gestiona la sesión del usuario y permite reaccionar a los cambios.
 */
interface AuthRepository {
    /**
     * Usuario autenticado actualmente, o null si no hay sesión activa.
     */
    val currentUser: User?

    /**
     * Devuelve un flujo que emite el estado actual del usuario.
     *
     * @return flujo de [User] o null.
     */
    fun authState(): Flow<User?>

    /**
     * Inicia sesión con las credenciales proporcionadas.
     *
     * @param email correo electrónico del usuario.
     * @param password contraseña del usuario.
     * @return [AuthResult] con el resultado de la autenticación.
     */
    suspend fun login(email: String, password: String): AuthResult

    /**
     * Registra un nuevo usuario con las credenciales proporcionadas.
     *
     * @param email correo electrónico del usuario.
     * @param password contraseña del usuario.
     * @return [AuthResult] con el resultado del registro.
     */
    suspend fun register(email: String, password: String): AuthResult

    /**
     * Inicia sesión de manera anónima.
     *
     * @return [AuthResult] con el resultado de la operación.
     */
    suspend fun loginAnonymously(): AuthResult

    /**
     * Envia un correo electrónico para restablecer la contraseña.
     *
     * @param email correo electrónico del usuario.
     * @return [AuthResult] con el resultado de la operación.
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult

    /**
     * Cierra la sesión actual.
     */
    suspend fun signOut()
}