package com.ludiary.android.data.repository.notification

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Implementación Firestore de [FcmTokensRepository].
 * @param fs Instancia de [FirebaseFirestore].
 */
class FirestoreFcmTokensRepository(
    private val fs: FirebaseFirestore
) : FcmTokensRepository {

    /**
     * Referencia al documento del token dentro del usuario.
     *
     * @param uid UID del usuario propietario del token.
     * @param token Token FCM (usado como id del documento).
     */
    private fun tokenDoc(uid: String, token: String) =
        fs.collection("users")
            .document(uid)
            .collection("fcmTokens")
            .document(token)

    /**
     * Inserta o actualiza un token FCM del usuario.
     * @param uid UID del usuario autenticado.
     * @param token Token FCM del dispositivo.
     */
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

    /**
     * Elimina un token FCM del usuario.
     * @param uid UID del usuario propietario.
     * @param token Token FCM a eliminar.
     */
    override suspend fun deleteToken(uid: String, token: String) {
        val t = token.trim()
        if (t.isBlank()) return
        tokenDoc(uid, t).delete().await()
    }
}
