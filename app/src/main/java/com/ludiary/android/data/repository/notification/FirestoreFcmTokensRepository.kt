package com.ludiary.android.data.repository.notification

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreFcmTokensRepository(
    private val fs: FirebaseFirestore
) : FcmTokensRepository {

    private fun tokenDoc(uid: String, token: String) =
        fs.collection("users")
            .document(uid)
            .collection("fcmTokens")
            .document(token)

    override suspend fun upsertToken(uid: String, token: String) {
        val t = token.trim()
        if (t.isBlank()) return

        tokenDoc(uid, t).set(
            mapOf(
                "token" to t,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun deleteToken(uid: String, token: String) {
        val t = token.trim()
        if (t.isBlank()) return
        tokenDoc(uid, t).delete().await()
    }
}