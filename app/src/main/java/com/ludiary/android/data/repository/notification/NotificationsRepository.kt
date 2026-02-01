package com.ludiary.android.data.repository.notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationsRepository(
    private val auth: FirebaseAuth,
    private val remote: FirestoreNotificationsRepository,
    private val functions: FunctionsNotificationsRepository,
    private val firestore: FirebaseFirestore
) {
    private val unreadCountFlow = MutableStateFlow(0)
    private var unreadListener: ListenerRegistration? = null

    fun observeUnreadCount(): Flow<Int> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            unreadCountFlow.value = 0
            return unreadCountFlow.asStateFlow()
        }

        // Evita duplicar listeners si se llama varias veces
        if (unreadListener == null) {
            val ref = firestore
                .collection("users")
                .document(uid)
                .collection("notificationStats")
                .document("stats")

            unreadListener = ref.addSnapshotListener { snap, _ ->
                val count = snap?.getLong("unreadCount")?.toInt() ?: 0
                unreadCountFlow.value = count
            }
        }

        return unreadCountFlow.asStateFlow()
    }

    fun stopUnreadCountListener() {
        unreadListener?.remove()
        unreadListener = null
    }
}