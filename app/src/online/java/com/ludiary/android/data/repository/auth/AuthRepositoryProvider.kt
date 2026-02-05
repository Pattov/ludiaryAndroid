package com.ludiary.android.data.repository.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.util.ResourceProvider

object AuthRepositoryProvider {
    fun provide(
        context: Context,
        localUserDataSource: LocalUserDataSource,
        resourceProvider: ResourceProvider
    ): AuthRepository = FirestoreAuthRepository(
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance(),
        localUserDataSource,
        resourceProvider
    )
}