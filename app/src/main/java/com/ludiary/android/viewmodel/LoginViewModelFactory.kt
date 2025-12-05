package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.AuthRepository
import com.ludiary.android.data.repository.FirestoreAuthRepository
import com.ludiary.android.util.ResourceProvider

/**
 * Fábrica responsable de crear instancias de [LoginViewModel].
 *
 * Implementa [ViewModelProvider.Factory] para inyectar las dependencias necesarias,
 * en este caso un [AuthRepository] basado en Firebase.
 *
 * Esta clase permite inicializar el ViewModel sin utilizar librerías de inyección
 * de dependencias (como Hilt o Koin), creando manualmente las instancias de
 * [FirestoreAuthRepository], [FirebaseAuth] y [FirebaseFirestore].
 */
class LoginViewModelFactory(private val context: Context)  : ViewModelProvider.Factory {
    /**
     * Crea una instancia de [LoginViewModel] cuando es solicitada por el fragmento o actividad.
     *
     * @param modelClass clase del ViewModel solicitado.
     * @return una instancia de [LoginViewModel] con su repositorio inyectado.
     * @throws IllegalArgumentException si el tipo solicitado no coincide con [LoginViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Room local DB
        val appContext = context.applicationContext
        val dblocal = LudiaryDatabase.getInstance(appContext)
        val localUserDataSource = LocalUserDataSource(dblocal)

        // ResourceProvider para strings.xml
        val resourceProvider = ResourceProvider(context)

        // Auth repo con Firebase + Room + Strings
        val repo: AuthRepository = FirestoreAuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            localUserDataSource,
            resourceProvider
        )
        return LoginViewModel(repo) as T
    }
}