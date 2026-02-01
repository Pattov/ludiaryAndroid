package com.ludiary.android.data.repository.notification

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ludiary.android.data.model.AppNotification
import com.ludiary.android.data.model.NotificationStats
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreNotificationsRepository(
    private val fs: FirebaseFirestore
) {
    fun observeStats(uid: String): Flow<NotificationStats> = callbackFlow {
        val ref = fs.collection("users").document(uid).collection("meta")
            .document("notificationStats")

        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val unread = (snap?.getLong("unreadCount") ?: 0L).toInt()
            val updatedAt = snap?.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            trySend(NotificationStats(unreadCount = unread, updatedAt = updatedAt))
        }
        awaitClose { reg.remove() }
    }

    fun observeNotifications(uid: String, limit: Long = 50): Flow<List<AppNotification>> =
        callbackFlow {
            val ref = fs.collection("users").document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

            val reg = ref.addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents.orEmpty().map { d ->
                    AppNotification(
                        id = d.id,
                        type = d.getString("type") ?: "unknown",
                        title = d.getString("title") ?: "",
                        body = d.getString("body") ?: "",
                        createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        read = d.getBoolean("read") ?: false,
                        data = d.get("data") as? Map<String, Any?> ?: emptyMap()
                    )
                }

                trySend(list)
            }
            awaitClose { reg.remove() }
        }
}