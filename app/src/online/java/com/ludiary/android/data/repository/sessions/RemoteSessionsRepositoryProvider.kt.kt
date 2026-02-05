package com.ludiary.android.data.repository.sessions

import com.google.firebase.firestore.FirebaseFirestore

object RemoteSessionsRepositoryProvider {
    fun provide(): RemoteSessionsRepository =
        FirestoreSessionsRepository(FirebaseFirestore.getInstance())
}