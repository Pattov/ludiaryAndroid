package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.repository.AuthRepository
import com.ludiary.android.data.repository.FirebaseAuthRepository

class LoginViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo: AuthRepository = FirebaseAuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
        return LoginViewModel(repo) as T
    }
}