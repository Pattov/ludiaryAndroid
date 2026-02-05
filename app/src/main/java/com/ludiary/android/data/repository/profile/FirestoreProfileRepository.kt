package com.ludiary.android.data.repository.profile

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Locale
import kotlin.collections.get

/**
 * Implementación de [ProfileRepository] que utiliza Firestore para obtener y actualizar el perfil del usuario.
 * @property auth Instancia de [FirebaseAuth] para autenticación de Firebase.
 * @property db Instancia de [FirebaseFirestore] para acceso a la base de datos de Firestore.
 * @property localUser Fuente de datos local
 */
class FirestoreProfileRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val localUser: LocalUserDataSource
) : ProfileRepository {

    /**
     * Referencia a la colección de usuarios en Firestore.
     */
    private fun users() = db.collection("users")

    /**
     * Obtiene el perfil del usuario.
     *
     * - Si no hay sesión Firebase → devuelve usuario local invitado en Room.
     * - Si hay sesión Firebase → intenta obtener/crear el documento en Firestore.
     * - Si falla por falta de conexión u otro error → fallback a Room, combinando con claims si es posible.
     */
    override suspend fun getOrCreate(): User {
        val firebaseUser = auth.currentUser ?: return localUser.getLocalUser()

        return try {
            val userRef = users().document(firebaseUser.uid)
            val doc = userRef.get().await()

            val userFromDb: User = if (!doc.exists()) {
                //  crear perfil SIN friendCode
                val profile = createProfileFromLocalOrAuth(firebaseUser)
                userRef.set(profile.toMap(), SetOptions.merge()).await()

                // asignar friendCode único
                val code = allocateUniqueFriendCode(firebaseUser.uid, length = 12)

                // perfil ya con friendCode
                profile.copy(friendCode = code)
            } else {
                // existe: asegurar friendCode si falta
                val code = ensureFriendCode(firebaseUser.uid)
                val fromDb = doc.toUserProfile()
                fromDb.copy(friendCode = code)
            }

            val tokenResult = firebaseUser.getIdToken(true).await()
            val isAdminClaim = (tokenResult.claims["admin"] as? Boolean) == true

            val finalUser = userFromDb.copy(isAdmin = isAdminClaim)

            localUser.saveLocalUser(finalUser)

            finalUser
        } catch (_: Exception) {
            // fallback offline
            val local = localUser.getLocalUser()

            val currentFirebaseUser = auth.currentUser
            val tokenResult = currentFirebaseUser?.getIdToken(false)?.await()
            val isAdminClaim = (tokenResult?.claims?.get("admin") as? Boolean) == true

            val offlineUser = User(
                uid = currentFirebaseUser?.uid ?: local.uid,
                email = currentFirebaseUser?.email ?: local.email,
                friendCode = local.friendCode,
                displayName = local.displayName ?: currentFirebaseUser?.displayName.orEmpty(),
                isAnonymous = false,
                createdAt = local.createdAt,
                updatedAt = local.updatedAt,
                preferences = local.preferences ?: UserPreferences(Locale.getDefault().language, "system"),
                isAdmin = isAdminClaim
            )

            localUser.saveLocalUser(offlineUser)
            offlineUser
        }
    }

    /**
     * Genera un código de amistad único para el usuario.
     * @param uid Identificador único del usuario.
     * @return Código de amistad generado
     */
    private suspend fun allocateUniqueFriendCode(uid: String, length: Int = 12): String {
        val maxAttempts = 10

        repeat(maxAttempts) {
            val code = generateFriendCode(length)
            val userRef = users().document(uid)
            val idxRef = db.collection("friend_code_index").document(code)

            try {
                val result = db.runTransaction { tx ->
                    val idxSnap = tx.get(idxRef)
                    if (idxSnap.exists()) {
                        throw IllegalStateException("CODE_TAKEN")
                    }

                    tx.set(idxRef, mapOf("uid" to uid))
                    tx.set(
                        userRef,
                        mapOf(
                            "friendCode" to code,
                            "updatedAt" to Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                    code
                }.await()

                return result
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (!msg.contains("CODE_TAKEN")) throw e
            }
        }

        throw IllegalStateException("No se pudo generar un friendCode único tras varios intentos")
    }

    /**
     * Crea un código de amistad si no existe.
     * @param uid Identificador único del usuario.
     * @return Código de amistad generado.
     */
    private suspend fun ensureFriendCode(uid: String): String {
        val userRef = users().document(uid)
        val snap = userRef.get().await()

        val existing = snap.getString("friendCode")
        if (!existing.isNullOrBlank()) return existing

        // No existe -> crear uno único con transacción (para evitar duplicados)
        return allocateUniqueFriendCode(uid, length = 12)
    }

    /**
     * Genera un código de amistad aleatorio.
     * @param length Longitud del código.
     * @return Código de amistad generado.
     */
    private fun generateFriendCode(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val rnd = SecureRandom()
        return buildString(length) {
            repeat(length) { append(chars[rnd.nextInt(chars.length)]) }
        }
    }

    /**
     * Actualiza el perfil del usuario en Firestore con los datos proporcionados.
     */
    override suspend fun update(displayName: String?, language: String?, theme: String?): User {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            val current = localUser.getLocalUser()
            val updated = current.copy(
                displayName = displayName ?: current.displayName,
                preferences = UserPreferences(
                    language = language ?: current.preferences?.language ?: Locale.getDefault().language,
                    theme = theme ?: current.preferences?.theme ?: "system"
                )
            )
            localUser.saveLocalUser(updated)
            return updated
        }

        val now = Timestamp.now()
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to now
        )

        displayName?.let { updates["displayName"] = it }

        val prefUpdates = mutableMapOf<String, Any>()
        language?.let { prefUpdates["language"] = it }
        theme?.let { prefUpdates["theme"] = it }
        if (prefUpdates.isNotEmpty()) {
            updates["preferences"] = prefUpdates
        }

        users().document(firebaseUser.uid).set(updates, SetOptions.merge()).await()

        val remoteSnap = users().document(firebaseUser.uid).get().await()
        val remote = remoteSnap.toUserProfile()

        val code = ensureFriendCode(firebaseUser.uid)

        // claim admin desde token
        val tokenResult = firebaseUser.getIdToken(false).await()
        val isAdminClaim = (tokenResult.claims["admin"] as? Boolean) == true

        val finalUser = remote.copy(isAdmin = isAdminClaim, friendCode = code)

        localUser.saveLocalUser(finalUser)
        return finalUser
    }

    /**
     * Cierra la sesión del usuario en Firebase.
     */
    override suspend fun signOut() {
        auth.signOut()
        localUser.clear()
    }

    /**
     * Convierte un User a un Map para poder persistirlo en Firestore.
     */
    private fun User.toMap(): Map<String, Any?> {
        val createdTs = createdAt?.let { Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt()) }
            ?: Timestamp.now()
        val updatedTs = Timestamp.now()

        return mapOf(
            "email" to email,
            "displayName" to displayName,
            "createdAt" to createdTs,
            "updatedAt" to updatedTs,
            "preferences" to mapOf(
                "language" to (preferences?.language ?: Locale.getDefault().language),
                "theme" to (preferences?.theme ?: "system")
            ),
            "isAdmin" to isAdmin
        )
    }

    /**
     * Convierte un DocumentSnapshot de Firestore a un User.
     */
    private fun DocumentSnapshot.toUserProfile(): User {
        val pref = (this.get("preferences") as? Map<*, *>) ?: emptyMap<String, Any>()

        val legacyLang = getString("preferences.language")
        val legacyTheme = getString("preferences.theme")

        val lang = (pref["language"] as? String)
            ?: legacyLang
            ?: Locale.getDefault().language

        val theme = (pref["theme"] as? String)
            ?: legacyTheme
            ?: "system"

        val createdTs = getTimestamp("createdAt")
        val updatedTs = getTimestamp("updatedAt")

        return User(
            uid = id, // docId
            email = getString("email"),
            displayName = getString("displayName") ?: "",
            isAnonymous = getBoolean("isAnonymous") ?: false,
            createdAt = createdTs?.toDate()?.time,
            updatedAt = updatedTs?.toDate()?.time,
            preferences = UserPreferences(language = lang, theme = theme),
            isAdmin = getBoolean("isAdmin") ?: false,
            friendCode = getString("friendCode")
        )
    }

    /**
     * Crea un perfil de usuario en Firestore si no existe, y lo devuelve.
     * @param firebaseUser Usuario de Firebase.
     * @return Perfil de usuario creado o existente.
     */
    private suspend fun createProfileFromLocalOrAuth(firebaseUser: FirebaseUser): User {
        val local = localUser.getLocalUser()
        val now = Timestamp.now()

        val tokenResult = firebaseUser.getIdToken(true).await()
        val isAdminClaim = (tokenResult.claims["admin"] as? Boolean) == true

        val defaultLang = Locale.getDefault().language

        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = local.displayName ?: firebaseUser.displayName.orEmpty(),
            isAnonymous = false,
            createdAt = now.toDate().time,
            updatedAt = now.toDate().time,
            preferences = local.preferences ?: UserPreferences(defaultLang, "system"),
            isAdmin = isAdminClaim,
            friendCode = null
        )
    }
}