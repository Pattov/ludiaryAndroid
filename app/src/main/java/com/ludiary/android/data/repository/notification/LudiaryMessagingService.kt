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

/**
 * Servicio Firebase Cloud Messaging (FCM) de Ludiary.
 */
class LudiaryMessagingService : FirebaseMessagingService() {

    private val tokensRepo: FcmTokensRepository by lazy {
        FirestoreFcmTokensRepository(FirebaseFirestore.getInstance())
    }

    /**
     * Callback llamado por Firebase cuando el token FCM del dispositivo se crea o rota.
     * @param token Nuevo token FCM del dispositivo.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { tokensRepo.upsertToken(uid, token) }
                .onFailure { Log.w("LUDIARY_FCM", "upsertToken failed: ${it.message}") }
        }
    }

    /**
     * Callback llamado cuando se recibe un mensaje push.
     * @param message Mensaje remoto recibido desde FCM.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: return

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notif = NotificationCompat.Builder(this, CHANNEL_SOCIAL)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        // Se usa un ID basado en tiempo para evitar colisiones entre notificaciones.
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}