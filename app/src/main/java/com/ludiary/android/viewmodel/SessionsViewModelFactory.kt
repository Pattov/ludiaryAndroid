package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.auth.AuthRepositoryProvider
import com.ludiary.android.util.ResourceProvider

class SessionsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        val db = LudiaryDatabase.getInstance(appContext)

        val localUserDataSource = LocalUserDataSource(db)
        val resourceProvider = ResourceProvider(context)

        val authRepo = AuthRepositoryProvider.provide(
            context = appContext,
            localUserDataSource = localUserDataSource,
            resourceProvider = resourceProvider
        )

        return SessionsViewModel(db, authRepo) as T
    }
}