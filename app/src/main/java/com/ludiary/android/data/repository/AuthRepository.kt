package com.ludiary.android.data.repository

import com.ludiary.android.data.model.User
import kotlinx.coroutines.flow.Flow


sealed class AuthResult{
    data object Loading : AuthResult()
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
interface AuthRepository {
    val currentUser: User?
    fun authState(): Flow<User?>
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String): AuthResult
    suspend fun loginAnonymously(): AuthResult
    suspend fun sendPasswordReset(email: String): AuthResult
    suspend fun signOut()
}