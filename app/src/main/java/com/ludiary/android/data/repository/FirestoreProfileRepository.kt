package com.ludiary.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [ProfileRepository] que utiliza Firestore para obtener y actualizar el perfil del usuario.
 *
 * @property auth Instancia de [FirebaseAuth] para autenticación de Firebase.
 * @property db Instancia de [FirebaseFirestore] para acceso a la base de datos de Firestore.
 * @property localUser Fuente de datos local
 */
class FirestoreProfileRepository (
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val localUser: LocalUserDataSource
) : ProfileRepository {

    /**
     * Devuelve la referencia a la colección de usuarios en Firestore.
     */
    private fun users() = db.collection("users")

    /**
     * Obtiene el usuario actual del usuario autenticado en Firebase.
     */
    override suspend fun getOrCreate(): User {
        // Si el usuario no tiene Sesión Firebase, devuelve el usuario local
        val firebaseUser = auth.currentUser ?: return localUser.getLocalUser()

        // Si el usuario tiene Sesión Firebase, intenta obtener su perfil de Firestore
        return try {
            // 1) Obtener documento Firestore
            val doc = users().document(firebaseUser.uid).get().await()
            val userFromDb = if (!doc.exists()) {
                createProfileFromLocalOrAuth(firebaseUser)
            } else {
                doc.toUserProfile()
            }

            // 2) Obtener token y claims
            val tokenResult = firebaseUser.getIdToken(true).await()
            val isAdminClaim = (tokenResult.claims["admin"] as? Boolean) == true

            // 3) Devolver usuario final combinando Firestore + token.claims
            userFromDb.copy(isAdmin = isAdminClaim)

        } catch (_: Exception) {

            // Fallback offline
            val local = localUser.getLocalUser()

            val firebaseUser = auth.currentUser
            val tokenResult = firebaseUser?.getIdToken(false)?.await()
            val isAdminClaim = tokenResult?.claims?.get("admin") == true

            User(
                uid = firebaseUser?.uid ?: local.uid,
                email = firebaseUser?.email ?: local.email,
                displayName = local.displayName ?: firebaseUser?.displayName ?: "",
                isAnonymous = false,
                createdAt = local.createdAt,
                updatedAt = local.updatedAt,
                preferences = local.preferences ?: UserPreferences("es", "system"),
                isAdmin = isAdminClaim
            )
        }
    }

    /**
     * Actualiza el perfil del usuario en Firestore con los datos proporcionados.
     */
    override suspend fun update(displayName: String?, language: String?, theme: String?): User {
        val firebaseUser = auth.currentUser

        //Modo local: actualiza solo la base de datos interna
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

        // modo online autenticado: actualiza el perfil en Firestore
        val now = com.google.firebase.Timestamp.now()
        val updates = mutableMapOf<String, Any?>(
            "updatedAt" to now
        )

        displayName?.let { updates["displayName"] = it }
        language?.let { updates["preferences.language"] = it }
        theme?.let { updates["preferences.theme"] = it }

        users().document(firebaseUser.uid).set(updates, SetOptions.merge()).await()

        // Perfil tal y como está en Firestore
        val remote = users().document(firebaseUser.uid).get().await().toUserProfile()

        // Obtener claim "admin" desde el token de Firebase
        val tokenResult = firebaseUser.getIdToken(false).await()
        val isAdminClaim = (tokenResult.claims["admin"] as? Boolean) == true

        // Devolver perfil remoto pero con isAdmin actualizado desde el token
        return remote.copy(isAdmin = isAdminClaim)
    }

    /**
     * Cierra la sesión del usuario en Firebase.
     */
    override suspend fun signOut() {
        auth.signOut()
    }

    /**
     * Convierte un User a un Map para poder persistirlo en Firestore.
     */
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

    /**
     * Convierte un DocumentSnapshot de Firestore a un User.
     */
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

    /**
     * Crea un perfil de usuario en Firestore si no existe, y lo devuelve.
     */
    private suspend fun createProfileFromLocalOrAuth(firebaseUser: FirebaseUser): User {
        val local = localUser.getLocalUser()
        val now = com.google.firebase.Timestamp.now()

        // Obtener el claim admin del token
        val tokenResult = firebaseUser.getIdToken(true).await()
        val isAdminClaim = (tokenResult.claims["admin"] as? Boolean) == true

        val profile = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = local.displayName ?: firebaseUser.displayName ?: "",
            isAnonymous = false,
            createdAt = now.toDate().time,
            updatedAt = now.toDate().time,
            preferences = local.preferences ?: UserPreferences("es", "system"),
            isAdmin = isAdminClaim
        )
        users().document(firebaseUser.uid).set(profile.toMap()).await()
        return profile
    }
}