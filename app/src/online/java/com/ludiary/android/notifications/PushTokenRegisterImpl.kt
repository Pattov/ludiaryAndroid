package com.ludiary.android.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class PushTokenRegisterImpl : PushTokenRegistrar {
    override fun registerTokenForCurrentUser() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("fcmTokens").document(token)
                .set(mapOf("createdAt" to FieldValue.serverTimestamp()))
        }
    }
}