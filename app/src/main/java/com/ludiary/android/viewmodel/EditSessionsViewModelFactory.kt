package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.data.local.LudiaryDatabase

/**
 * Factory para crear una instancia de [EditSessionsViewModel].
 * @property db Instancia de [LudiaryDatabase].
 * @property auth Instancia de [FirebaseAuth].
 */
class EditSessionsViewModelFactory(
    private val db: LudiaryDatabase,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditSessionsViewModel(db, auth) as T
    }
}
