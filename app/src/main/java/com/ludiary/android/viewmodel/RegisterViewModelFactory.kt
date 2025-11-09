package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.repository.AuthRepository
import com.ludiary.android.data.repository.FirebaseAuthRepository

/**
 * Fábrica responsable de crear instancias de [RegisterViewModel].
 *
 * Implementa [ViewModelProvider.Factory] para inyectar las dependencias.
 */
class RegisterViewModelFactory : ViewModelProvider.Factory {
    /**
     * Crea y devuelve una instancia de [RegisterViewModel].
     *
     * @param modelClass La clase del modelo que se está creando.
     * @return Una instancia de [RegisterViewModel].
     * @throws IllegalArgumentException Si [modelClass] no es de tipo [RegisterViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo: AuthRepository = FirebaseAuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
        return RegisterViewModel(repo) as T
    }
}