package com.ludiary.android.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreFriendsRepository(
    private val db: FirebaseFirestore
) {
    /**
     * Modelo remoto para /users/{uid}/friends/{friendUid}
     */
    data class RemoteFriend(
        val friendUid: String = "",
        val email: String? = null,
        val displayName: String? = null,
        val nickname: String? = null,
        val status: String = "",
        val createdAt: Long? = null,
        val updatedAt: Long? = null
    )

    data class RemoteUser(
        val uid: String,
        val email: String?,
        val displayName: String?
    )

    private fun col(uid: String) =
        db.collection("users").document(uid).collection("friends")

    fun observeAll(uid: String): Flow<List<RemoteFriend>> = callbackFlow {
        val reg = col(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val items = snap?.documents?.mapNotNull { it.toRemoteFriendOrNull() }.orEmpty()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    suspend fun upsert(uid: String, friendUid: String, data: RemoteFriend) {
        col(uid).document(friendUid)
            .set(data.toMap(), SetOptions.merge())
            .awaitResult()
    }

    suspend fun delete(uid: String, friendUid: String) {
        col(uid).document(friendUid)
            .delete()
            .awaitResult()
    }

    /**
     * Resuelve friend code via /friend_code_index/{code} -> { uid: ... }
     * Solo se usa en sync (no en UI).
     */
    suspend fun findUserByFriendCode(code: String): RemoteUser? {
        val idx = db.collection("friend_code_index")
            .document(code)
            .get()
            .awaitResult()

        val uid = idx.getString("uid") ?: return null

        val user = db.collection("users")
            .document(uid)
            .get()
            .awaitResult()

        return RemoteUser(
            uid = user.id,
            email = user.getString("email"),
            displayName = user.getString("displayName")
        )
    }

    // -------- mapping sin toObject() --------

    private fun DocumentSnapshot.toRemoteFriendOrNull(): RemoteFriend? {
        val status = getString("status") ?: return null
        val friendUid = getString("friendUid") ?: id

        return RemoteFriend(
            friendUid = friendUid,
            email = getString("email"),
            displayName = getString("displayName"),
            nickname = getString("nickname"),
            status = status,
            createdAt = getLong("createdAt"),
            updatedAt = getLong("updatedAt")
        )
    }

    private fun RemoteFriend.toMap(): Map<String, Any?> = mapOf(
        "friendUid" to friendUid,
        "email" to email,
        "displayName" to displayName,
        "nickname" to nickname,
        "status" to status,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    // -------- Task.await sin coroutines-play-services --------

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
            addOnCanceledListener { cont.cancel() }
        }
}