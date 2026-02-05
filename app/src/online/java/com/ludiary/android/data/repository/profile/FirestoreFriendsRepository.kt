package com.ludiary.android.data.repository.profile

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio remoto para gestión de amigos en Firestore
 * @param db Instancia de Firebase.
 */
class FirestoreFriendsRepository(
    private val db: FirebaseFirestore
) {

    /**
     * Modelo remoto de una relación de amistad en Firestore
     * @property friendUid UID del amigo
     * @property friendCode Código de amigo (si está disponible)
     * @property displayName Nombre visible del amigo (si está disponible)
     * @property nickname Alias asignado por el usuario (opcional)
     * @property status Estado de la relación (normalmente el name() de FriendStatus)
     * @property createdAt Marca de tiempo (epoch millis) de creación
     * @property updatedAt Marca de tiempo (epoch millis) de última actualización
     */
    data class RemoteFriend(
        val friendUid: String = "",
        val friendCode: String? = null,
        val displayName: String? = null,
        val nickname: String? = null,
        val status: String = "",
        val createdAt: Long? = null,
        val updatedAt: Long? = null
    )

    /**
     * Referencia a la colección `users/{uid}/friends`
     * @param uid UID del usuario dueño de la subcolección
     */
    private fun col(uid: String) =
        db.collection("users").document(uid).collection("friends")

    /**
     * Observa en tiempo real todos los documentos de amigos para un usuario
     * @param uid UID del usuario
     * @return Flow con la lista remota de amigos
     * @throws Throwable si Firestore devuelve un error en el listener
     */
    fun observeAll(uid: String): Flow<List<RemoteFriend>> = callbackFlow {
        val reg = col(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err); return@addSnapshotListener
            }
            val items = snap?.documents?.mapNotNull { it.toRemoteFriendOrNull() }.orEmpty()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    /**
     * Crea o actualiza un documento de amistad en `users/{uid}/friends/{friendUid}`
     * @param uid UID del usuario dueño del documento.
     * @param friendUid UID del amigo (id del documento).
     * @param data Datos remotos a persistir.
     */
    suspend fun upsert(uid: String, friendUid: String, data: RemoteFriend) {
        col(uid).document(friendUid)
            .set(data.toMap(), SetOptions.merge())
            .await()
    }

    /**
     * Elimina un documento de amistad de `users/{uid}/friends/{friendUid}`
     * @param uid UID del usuario dueño del documento
     * @param friendUid UID del amigo (id del documento)
     */
    suspend fun delete(uid: String, friendUid: String) {
        col(uid).document(friendUid).delete().await()
    }

    /**
     * Obtiene el `friendCode` del usuario desde `users/{uid}`
     * @param uid UID del usuario
     * @return El friendCode o null si no existe
     */
    suspend fun getMyFriendCode(uid: String): String? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("friendCode")
    }

    /**
     * Convierte un [DocumentSnapshot] a [RemoteFriend] si contiene los campos mínimos
     * @return RemoteFriend o null si faltan campos requeridos.
     */
    private fun DocumentSnapshot.toRemoteFriendOrNull(): RemoteFriend? {
        val status = getString("status") ?: return null
        val friendUid = getString("friendUid") ?: id

        return RemoteFriend(
            friendUid = friendUid,
            friendCode = getString("friendCode"),
            displayName = getString("displayName"),
            nickname = getString("nickname"),
            status = status,
            createdAt = getLong("createdAt"),
            updatedAt = getLong("updatedAt")
        )
    }

    /**
     * Convierte un [RemoteFriend] a un Map para persistirlo en Firestore
     */
    private fun RemoteFriend.toMap(): Map<String, Any?> = mapOf(
        "friendUid" to friendUid,
        "friendCode" to friendCode,
        "displayName" to displayName,
        "nickname" to nickname,
        "status" to status,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}