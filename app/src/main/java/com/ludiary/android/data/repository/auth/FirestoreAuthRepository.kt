package com.ludiary.android.data.repository.auth

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ludiary.android.R
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences
import com.ludiary.android.util.ResourceProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Locale
import kotlin.collections.get

/**
 * Implementación de AuthRepository que utiliza Firebase Authentication para autenticación.
 * Gestiona el registro, inicio de sesión y persistencia del usuario
 *
 * @property auth Firebase Authentication para autenticación
 * @property db Firebase Firestore para el almacenamiento de datos
 * @property localUser Fuente de datos local para el almacenamiento del usuario
 * @property resources Proveedor de recursos para mensajes de error
 */
class FirestoreAuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val localUser: LocalUserDataSource,
    private val resources: ResourceProvider
) : AuthRepository {
    /**
     * Devuelve el usuario autenticado actualmente, o null si no hay sesión activa.
     */
    override val currentUser: User?
        get() = auth.currentUser?.let{
            User(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName,
                isAnonymous = it.isAnonymous
            )
        }

    /**
     * Flujo reactivo que emite el estado de autenticación del usuario.
     *
     * Cada vez que cambia la sesión, se emite el nuevo usuario autenticado o null si no hay ninguno
     *
     * @return Flujo reactivo de User
     */
    override fun authState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val u = fa.currentUser?.let {
                User(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    isAnonymous = it.isAnonymous
                )
            }
            trySend(u)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Inicia sesión con el correo electrónico y la contraseña proporcionados.
     *
     * Si el usuario es correcto, asegura que el documento del usuario exista en Firestore.
     *
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @return [AuthResult.Success] con los datos del usuario o [AuthResult.Error] con el mensaje de error.
     */
    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            val u = auth.currentUser ?: return AuthResult.Error("Usuario no encontrado")

            // Nos aseguramos de que exista el documento en Firestore
            ensureUserDoc(u.uid, u.email, u.displayName, u.isAnonymous)
            ensureFriendCode(u.uid)

            // Descargamos el perfil desde Firestore (si es posible) y lo guardamos en local
            val profile = fetchUserFromFirestoreOrFallback(u)
            localUser.saveLocalUser(profile)

            AuthResult.Success(profile)
        }catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> {
                    // Credenciales incorrectas (correo o contraseña)
                    resources.getString(R.string.auth_error_invalid_credentials)
                }
                is FirebaseNetworkException -> {
                    // Problema de conexión
                    resources.getString(R.string.auth_error_network)
                }
                else -> {
                    // Cualquier otro error genérico
                    resources.getString(R.string.auth_error_generic)                }
            }

            Log.e("LUDIARY_AUTH_REPO", "Error al iniciar sesión", e)
            AuthResult.Error(msg)
        }
    }

    /**
     * Registra un nuevo usuario con correo y contraseña en Firebase Auth.
     * @param email Correo electrónico del usuario.
     * @param password Contraseña del usuario.
     * @return [AuthResult.Success] con el perfil del usuario, o [AuthResult.Error] con un mensaje localizado.
     */
    override suspend fun register(email: String, password: String): AuthResult {
        val emailTrimmed = email.trim()

        return try {
            val result = auth.createUserWithEmailAndPassword(emailTrimmed, password).await()
            val firebaseUser = result.user
                ?: return AuthResult.Error(
                    resources.getString(R.string.auth_error_user_not_found_after_register)
                )

            // Firestore: asegurar doc de usuario + friendCode
            ensureUserDoc(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                isAnonymous = firebaseUser.isAnonymous
            )
            ensureFriendCode(firebaseUser.uid)

            // Perfil final + persistencia local
            val profile = fetchUserFromFirestoreOrFallback(firebaseUser)
            localUser.saveLocalUser(profile)

            AuthResult.Success(profile)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthUserCollisionException ->
                    resources.getString(R.string.auth_error_collision)

                is FirebaseNetworkException ->
                    resources.getString(R.string.auth_error_network)

                else ->
                    resources.getString(R.string.auth_error_generic_register)
            }

            Log.e("LUDIARY_AUTH_REPO", "Error al registrar usuario", e)
            AuthResult.Error(msg)
        }
    }

    /**
     * Inicia sesión de manera anónima en Firebase.
     *
     * Crea también su documento en la colección Users de Firestore si no existía.
     *
     * @return [AuthResult.Success] con los datos del usuario o [AuthResult.Error] con el mensaje de error
     */
    override suspend fun loginAnonymously(): AuthResult {
        return try {
            auth.signInAnonymously().await()
            val u = auth.currentUser ?: return AuthResult.Error(resources.getString(R.string.auth_error_user_not_found))

            // Usuario invitado solo en local
            val now = System.currentTimeMillis()
            val guest = User(
                uid = u.uid,
                email = null,
                displayName = resources.getString(R.string.auth_guest_name),
                isAnonymous = true,
                createdAt = now,
                updatedAt = now,
                preferences = UserPreferences(
                    language = Locale.getDefault().language,
                    theme = "system"
                ),
                isAdmin = false,
                friendCode = null
            )

            localUser.saveLocalUser(guest)

            AuthResult.Success(guest)
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseNetworkException ->
                    resources.getString(R.string.auth_error_network)
                else ->
                    resources.getString(R.string.auth_error_guest_failed)
            }

            Log.e("LUDIARY_AUTH_REPO", "Error al iniciar sesión como invitado", e)
            AuthResult.Error(msg)
        }
    }

    /**
     * Envia un correo electrónico de restablecimiento de contraseña.
     *
     * @param email Correo electrónico del usuario que solicita el restablecimiento.
     * @return [AuthResult.Success] si el correo fue enviado o [AuthResult.Error] con el motivo de fallo.
     */
    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            val cleanEmail = email.trim()

            // Comprobar manualmente en Firestore si existe un usuario con ese correo
            val snap = db.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()

            if (snap.isEmpty) {
                // No existe en nuestra base → mensaje de error
                return AuthResult.Error(resources.getString(R.string.auth_error_reset_no_user))
            }

            // Si existe, ahora sí pedimos a Firebase que envíe el correo
            auth.sendPasswordResetEmail(cleanEmail).await()
            AuthResult.Success(User())
        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseNetworkException -> {
                    resources.getString(R.string.auth_error_network)
                }
                else -> {
                    resources.getString(R.string.auth_error_reset_generic)
                }
            }
            AuthResult.Error(msg)
        }
    }

    /**
     * Cierra la sesión actual del usuario autenticado.
     */
    override suspend fun signOut() {
        auth.signOut()
        localUser.clear()
    }

    /**
     * Asegura que existe un documento para el usuario en la colección "users" de Firestore.
     *
     * Si no existe, crea uno con los datos proporcionados.
     *
     * @param uid Identificador único del usuario
     * @param email Correo electrónico del usuario
     * @param displayName Nombre de usuario del usuario
     * @param isAnonymous Indica si el usuario es anónimo
     */
    private suspend fun ensureUserDoc(
        uid: String,
        email: String?,
        displayName: String?,
        isAnonymous: Boolean
    ){
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()
        if(!snap.exists()){
            val now: Timestamp = Timestamp.now()
            val payload = mapOf(
                "email" to email,
                "displayName" to displayName,
                "isAnonymous" to isAnonymous,
                "createdAt" to now,
                "updatedAt" to now,
                "preferences" to mapOf(
                    "language" to Locale.getDefault().language,
                    "theme" to "system"
                ),
                "isAdmin" to false
            )
            ref.set(payload).await()
        }
    }

    /**
     * Genera un código de amistad aleatorio.
     * @param length Longitud del código.
     */
    private fun generateFriendCode(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val rnd = SecureRandom()
        return buildString(length) {
            repeat(length) { append(chars[rnd.nextInt(chars.length)]) }
        }
    }

    /**
     * Garantiza que el usuario tiene friendCode y que es único.
     * Si falta, lo asigna usando transacción (evita duplicados).
     */
    private suspend fun ensureFriendCode(uid: String): String {
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        val existing = snap.getString("friendCode")
        if (!existing.isNullOrBlank()) return existing

        return allocateUniqueFriendCode(uid, length = 12)
    }

    /**
     * Asigna un código de amistad único a un usuario.
     * @param uid Identificador único del usuario.
     * @param length Longitud del código.
     */
    private suspend fun allocateUniqueFriendCode(uid: String, length: Int = 12): String {
        val maxAttempts = 10

        repeat(maxAttempts) {
            val code = generateFriendCode(length)
            val userRef = db.collection("users").document(uid)
            val idxRef = db.collection("friend_code_index").document(code)

            try {
                val result = db.runTransaction { tx ->
                    val idxSnap = tx.get(idxRef)
                    if (idxSnap.exists()) throw IllegalStateException("CODE_TAKEN")

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
     * Intenta descargar el perfil del usuario desde Firestore.
     *
     * Si falla, devuelve un usuario con los datos básicos.
     *
     * @param firebaseUser Usuario de Firebase
     * @return Perfil del usuario descargado o un usuario con los datos básicos
     */
    private suspend fun fetchUserFromFirestoreOrFallback(firebaseUser: FirebaseUser): User {
        return try {
            val doc = db.collection("users").document(firebaseUser.uid).get().await()
            if (!doc.exists()) {
                basicUserFromFirebase(firebaseUser)
            } else {
                doc.toUserModel(firebaseUser.uid, firebaseUser)
            }
        } catch (_: Exception) {
            // Si algo falla con Firestore (sin conexión, etc.), devolvemos un usuario básico
            basicUserFromFirebase(firebaseUser)
        }
    }

    /**
     * Crea un usuario básico a partir de los datos de Firebase.
     *
     * @param firebaseUser Usuario de Firebase
     * @return Usuario básico con los datos básicos
     */
    private fun basicUserFromFirebase(firebaseUser: FirebaseUser): User {
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName,
            isAnonymous = firebaseUser.isAnonymous
        )
    }

    /**
     * Convierte un documento de Firestore en un modelo de usuario.
     *
     * @param defaultUid UID por defecto si no se encuentra en el documento
     * @param firebaseUser Usuario de Firebase
     * @return Modelo de usuario con los datos del documento
     */
    private fun DocumentSnapshot.toUserModel(defaultUid: String, firebaseUser: FirebaseUser): User {
        val pref = (this.get("preferences") as? Map<*, *>) ?: emptyMap<String, Any>()

        val lang = pref["language"] as? String ?: Locale.getDefault().language
        val theme = pref["theme"] as? String ?: "system"

        val createdTs = getTimestamp("createdAt")
        val updatedTs = getTimestamp("updatedAt")

        return User(
            uid = getString("uid") ?: defaultUid,
            email = getString("email") ?: firebaseUser.email,
            friendCode = getString("friendCode"),
            displayName = getString("displayName") ?: firebaseUser.displayName ?: "",
            isAnonymous = getBoolean("isAnonymous") ?: firebaseUser.isAnonymous,
            createdAt = createdTs?.toDate()?.time,
            updatedAt = updatedTs?.toDate()?.time,
            preferences = UserPreferences(language = lang, theme = theme),
            isAdmin = getBoolean("isAdmin") ?: false
        )
    }
}