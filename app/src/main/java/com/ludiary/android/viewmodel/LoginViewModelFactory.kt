package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.repository.AuthRepository
import com.ludiary.android.data.repository.FirebaseAuthRepository

/**
 * Fábrica responsable de crear instancias de [LoginViewModel].
 *
 * Implementa [ViewModelProvider.Factory] para inyectar las dependencias necesarias,
 * en este caso un [AuthRepository] basado en Firebase.
 *
 * Esta clase permite inicializar el ViewModel sin utilizar librerías de inyección
 * de dependencias (como Hilt o Koin), creando manualmente las instancias de
 * [FirebaseAuthRepository], [FirebaseAuth] y [FirebaseFirestore].
 */
class LoginViewModelFactory : ViewModelProvider.Factory {
    /**
     * Crea una instancia de [LoginViewModel] cuando es solicitada por el fragmento o actividad.
     *
     * @param modelClass clase del ViewModel solicitado.
     * @return una instancia de [LoginViewModel] con su repositorio inyectado.
     * @throws IllegalArgumentException si el tipo solicitado no coincide con [LoginViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo: AuthRepository = FirebaseAuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
        return LoginViewModel(repo) as T
    }
}