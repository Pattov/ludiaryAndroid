package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.data.local.LudiaryDatabase

/**
 * Factory para crear una instancia de [EditSessionsViewModel].
 * @property context Contexto de la aplicación.
 * @property db Instancia de [LudiaryDatabase].
 * @property auth Instancia de [FirebaseAuth].
 */
class EditSessionsViewModelFactory(
    private val context: Context,
    private val db: LudiaryDatabase,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditSessionsViewModel(context, db, auth) as T
    }
}