package com.ludiary.android.data.repository

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
     * Modelo remoto mínimo de usuario para resolver invitaciones
     * @property uid UID del usuario
     * @property displayName Nombre visible (si existe)
     */
    data class RemoteUser(
        val uid: String,
        val displayName: String?
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
     * Obtiene el `displayName` del usuario desde `users/{uid}`
     * @param uid UID del usuario
     * @return El displayName o null si no existe
     */
    suspend fun getUserDisplayName(uid: String): String? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("displayName")
    }

    /**
     * Resuelve el `friendCode` de un usuario a partir de su UID usando el índice `friend_code_index`.
     * @param uid UID del usuario.
     * @return friendCode (ID del doc) o null si no se encuentra.
     */
    suspend fun findFriendCodeByUid(uid: String): String? {
        val snap = db.collection("friend_code_index")
            .whereEqualTo("uid", uid)
            .limit(1)
            .get()
            .await()

        return snap.documents.firstOrNull()?.id
    }

    /**
     * Resuelve un usuario a partir de un código de amigo usando `friend_code_index/{CODE}`
     * @param codeRaw Código introducido por el usuario
     * @return RemoteUser si existe, o null si el código no existe o faltan datos
     */
    suspend fun findUserByFriendCode(codeRaw: String): RemoteUser? {
        val code = codeRaw.trim().uppercase()

        val idx = db.collection("friend_code_index")
            .document(code)
            .get()
            .await()

        if (!idx.exists()) {
            return null
        }

        val uid = idx.getString("uid") ?: return null

        val userDoc = db.collection("users")
            .document(uid)
            .get()
            .await()

        if (!userDoc.exists()) return null

        return RemoteUser(
            uid = uid,
            displayName = userDoc.getString("displayName")
        )
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