package com.ludiary.android.data.repository.notification

import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_SOCIAL = "social"

class LudiaryMessagingService : FirebaseMessagingService() {

    private val tokensRepo: FcmTokensRepository by lazy {
        FirestoreFcmTokensRepository(FirebaseFirestore.getInstance())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { tokensRepo.upsertToken(uid, token) }
                .onFailure { Log.w("FCM", "upsertToken failed: ${it.message}") }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: return

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notif = NotificationCompat.Builder(this, CHANNEL_SOCIAL)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun showLocalNotification(title: String, body: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notif = NotificationCompat.Builder(this, CHANNEL_SOCIAL)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}