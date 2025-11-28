package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.repository.UserGamesRepository

class EditUserGameViewModelFactory (
    private val uid: String,
    private val repository: UserGamesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditUserGameViewModel::class.java)) {
            return EditUserGameViewModel(uid, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}