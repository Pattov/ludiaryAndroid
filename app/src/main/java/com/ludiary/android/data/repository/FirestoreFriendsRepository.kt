package com.ludiary.android.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreFriendsRepository(
    private val firestore: FirebaseFirestore
) {
    data class RemoteFriend(
        val friendUid: String = "",
        val friendCode: String? = null,
        val displayName: String? = null,
        val nickname: String? = null,
        val status: String = "",
        val createdAt: Long? = null,
        val updatedAt: Long? = null
    )

    data class RemoteUser(
        val uid: String,
        val displayName: String?
    )

    private fun col(uid: String) =
        firestore.collection("users").document(uid).collection("friends")

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

    suspend fun upsert(uid: String, friendUid: String, data: RemoteFriend) {
        col(uid).document(friendUid)
            .set(data.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun delete(uid: String, friendUid: String) {
        col(uid).document(friendUid).delete().await()
    }

    suspend fun getMyFriendCode(uid: String): String? {
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.getString("friendCode")
    }

    suspend fun getUserDisplayName(uid: String): String? {
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.getString("displayName")
    }

    suspend fun findFriendCodeByUid(uid: String): String? {
        val snap = firestore.collection("friend_code_index")
            .whereEqualTo("uid", uid)
            .limit(1)
            .get()
            .await()

        return snap.documents.firstOrNull()?.id
    }

    suspend fun findUserByFriendCode(codeRaw: String): RemoteUser? {
        val code = codeRaw.trim().uppercase()
        Log.d("LUDIARY_FRIENDS_DEBUG", "findUserByFriendCode() code=$code")

        val idx = firestore.collection("friend_code_index")
            .document(code)
            .get()
            .await()

        if (!idx.exists()) {
            Log.d("LUDIARY_FRIENDS_DEBUG", "friend_code_index MISS code=$code")
            return null
        }

        val uid = idx.getString("uid") ?: return null

        val userDoc = firestore.collection("users")
            .document(uid)
            .get()
            .await()

        if (!userDoc.exists()) return null

        Log.d("LUDIARY_FRIENDS_DEBUG", "friend_code_index HIT code=$code uid=$uid")

        return RemoteUser(
            uid = uid,
            displayName = userDoc.getString("displayName")
        )
    }

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