package com.ludiary.android.data.repository.profile

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalUserDataSource

object ProfileRepositoryProvider {
    fun provide(
        context: Context,
        localUser: LocalUserDataSource
    ): ProfileRepository =
        FirestoreProfileRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            localUser
        )
}