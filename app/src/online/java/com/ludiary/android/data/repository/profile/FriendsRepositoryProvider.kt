package com.ludiary.android.data.repository.profile

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.ludiary.android.data.local.LocalFriendsDataSource

object FriendsRepositoryProvider {
    fun provide(
        context: Context,
        local: LocalFriendsDataSource
    ): FriendsRepository {

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val functions = FirebaseFunctions.getInstance()

        val remote = FirestoreFriendsRepository(firestore)
        val socialFunctions = FunctionsSocialRepository(functions)

        return FriendsRepositoryImpl(
            local = local,
            remote = remote,
            function = socialFunctions,
            auth = auth
        )
    }
}