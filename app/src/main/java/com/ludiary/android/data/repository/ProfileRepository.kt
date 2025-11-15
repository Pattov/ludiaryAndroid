package com.ludiary.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences
import kotlinx.coroutines.tasks.await

interface ProfileRepository {
    suspend fun getOrCreate(): User
    suspend fun update(displayName: String?, language: String?, theme: String?): User
    suspend fun signOut()
}

class FirestoreProfileRepository (
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val localUser: LocalUserDataSource
) : ProfileRepository {

    private fun users() = db.collection("users")

    override suspend fun getOrCreate(): User {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            return localUser.getLocalUser()
        }

        val doc = users().document(firebaseUser.uid).get().await()
            return if (!doc.exists()) {
                val now = com.google.firebase.Timestamp.now()
                val profile = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName ?: "",
                    isAnonymous = false,
                    createdAt = now.toDate().time,
                    updatedAt = now.toDate().time,
                    preferences = UserPreferences(language = "es", theme = "system"),
                    isAdmin = false
                )
                users().document(firebaseUser.uid).set(profile.toMap()).await()
                profile
            }else{
                doc.toUserProfile()
            }
    }

    override suspend fun update(displayName: String?, language: String?, theme: String?): User{
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            val current = localUser.getLocalUser()
            val updated = current.copy(
                displayName = displayName ?: current.displayName,
                preferences = UserPreferences(
                    language = language ?: current.preferences?.language?: "es",
                    theme = theme ?: current.preferences?.theme ?: "system"
                )
            )
            localUser.saveLocalUser(updated)
            return updated
        }

        val now = com.google.firebase.Timestamp.now()
        val updates = mutableMapOf<String, Any?>(
            "updatedAt" to now
        )

        displayName?.let { updates["displayName"] = it }
        language?.let { updates["preferences.language"] = it }
        theme?.let { updates["preferences.theme"] = it }

        users().document(firebaseUser.uid).set(updates, SetOptions.merge()).await()

        val remote = users().document(firebaseUser.uid).get().await().toUserProfile()
        return remote
    }

    override suspend fun signOut() {
        auth.signOut()
    }


    private fun User.toMap(): Map<String, Any?> {
        val createdTs = createdAt?.let {com.google.firebase.Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt()) }
            ?: com.google.firebase.Timestamp.now()
        val updatedTs = com.google.firebase.Timestamp.now()

        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "createdAt" to createdTs,
            "updatedAt" to updatedTs,
            "preferences" to mapOf(
                "language" to (preferences?.language ?: "es"),
                "theme" to (preferences?.theme ?: "system")
            ),
            "isAdmin" to isAdmin
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(): User {
        val pref = (this.get("preferences") as? Map<*, *>) ?: emptyMap<String, Any>()

        val lang = pref["language"] as? String ?: "es"
        val theme = pref["theme"] as? String ?: "system"

        val createdTs = getTimestamp("createdAt")
        val updatedTs = getTimestamp("updatedAt")

        return User(
            uid = getString("uid") ?: id,
            email = getString("email"),
            displayName = getString("displayName") ?: "",
            isAnonymous = getBoolean("isAnonymous") ?: false,
            createdAt = createdTs?.toDate()?.time,
            updatedAt = updatedTs?.toDate()?.time,
            preferences = UserPreferences( language = lang, theme = theme),
            isAdmin = getBoolean("isAdmin") ?: false
        )
    }
}