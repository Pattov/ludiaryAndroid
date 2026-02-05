package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.auth.AuthRepositoryProvider
import com.ludiary.android.util.ResourceProvider

/**
 * Fábrica responsable de crear instancias de [RegisterViewModel].
 *
 * Implementa [ViewModelProvider.Factory] para inyectar las dependencias.
 */
class RegisterViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    /**
     * Crea y devuelve una instancia de [RegisterViewModel].
     *
     * @param modelClass La clase del modelo que se está creando.
     * @return Una instancia de [RegisterViewModel].
     * @throws IllegalArgumentException Si [modelClass] no es de tipo [RegisterViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val appContext = context.applicationContext
        val dbLocal = LudiaryDatabase.getInstance(appContext)
        val localUserDataSource = LocalUserDataSource(dbLocal)

        val repo = AuthRepositoryProvider.provide(
            context = appContext,
            localUserDataSource = localUserDataSource,
            resourceProvider = ResourceProvider(context)
        )

        return RegisterViewModel(repo) as T
    }
}