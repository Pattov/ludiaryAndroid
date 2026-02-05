package com.ludiary.android.data.repository.auth

import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences
import com.ludiary.android.util.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class AuthRepositoryLocal(
    private val localUserDataSource: LocalUserDataSource,
    private val resourceProvider: ResourceProvider
) : AuthRepository {

    private val authStateFlow = MutableStateFlow<User?>(null)

    override val currentUser: User?
        get() = authStateFlow.value

    override fun authState(): Flow<User?> = authStateFlow.asStateFlow()

    override suspend fun login(email: String, password: String): AuthResult =
        AuthResult.Error("Login no disponible en modo local")

    override suspend fun register(email: String, password: String): AuthResult =
        AuthResult.Error("Registro no disponible en modo local")

    override suspend fun sendPasswordResetEmail(email: String): AuthResult =
        AuthResult.Error("Restablecer contraseña no disponible en modo local")

    override suspend fun loginAnonymously(): AuthResult {
        val now = System.currentTimeMillis()

        val user = User(
            uid = "local_${UUID.randomUUID()}",
            email = null,
            friendCode = null,
            displayName = "Invitado",
            isAnonymous = true,
            createdAt = now,
            updatedAt = now,
            preferences = UserPreferences(
                language = Locale.getDefault().language,
                theme = "system"
            ),
            isAdmin = false
        )

        // Guardar en Room (si tu LocalUserDataSource lo soporta)
        runCatching { localUserDataSource.saveLocalUser(user) }

        authStateFlow.value = user
        return AuthResult.Success(user)
    }

    override suspend fun signOut() {
        authStateFlow.value = null
        runCatching { localUserDataSource.clear() }
    }
}