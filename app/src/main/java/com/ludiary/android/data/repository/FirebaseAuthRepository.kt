package com.ludiary.android.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación de AuthRepository que utiliza Firebase Authentication para autenticación.
 * Gestiona el registro, inicio de sesión y persistencia del usuario
 *
 * @property auth Firebase Authentication para autenticación
 * @property db Firebase Firestore para el almacenamiento de datos
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
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
    override fun authState(): Flow<User?> = callbackFlow{
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
            ensureUserDoc(u.uid, u.email, u.displayName, u.isAnonymous)
            AuthResult.Success(User(u.uid, u.email, u.displayName, u.isAnonymous))
        }catch (e: Exception){
            AuthResult.Error(e.message ?: "Error al iniciar sesión")
        }
    }

    /**
     * Registra un nuevo usuario con correo y contraseña en Firebase.
     *
     * Crea también su documento en la colección Users de Firestore si no existía.
     *
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * [AuthResult.Success] con los datos del usuario o [AuthResult.Error] con el mensaje de error
     */
    override suspend fun register(email: String, password: String): AuthResult {
        return try {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val u = auth.currentUser ?: return AuthResult.Error("Usuario no encontrado tras registro")
            ensureUserDoc(u.uid, u.email, u.displayName, u.isAnonymous)
            AuthResult.Success(User(u.uid, u.email, u.displayName, u.isAnonymous))
        }catch (e: Exception){
            AuthResult.Error(e.message ?: "Error al registrar")
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
            val u = auth.currentUser ?: return AuthResult.Error("Usuario anónimo no encontrado")
            ensureUserDoc(u.uid, u.email, u.displayName, u.isAnonymous)
            AuthResult.Success(User(u.uid, u.email, u.displayName, u.isAnonymous))
        }catch (e: Exception){
            AuthResult.Error(e.message ?: "Error al iniciar sesión anónimo")
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
            auth.sendPasswordResetEmail(email.trim()).await()
            AuthResult.Success(User())
        }catch (e: Exception){
            AuthResult.Error(e.message ?: "No se pudo enviar el email")
        }
    }

    /**
     * Cierra la sesión actual del usuario autenticado.
     */
    override suspend fun signOut() {
        auth.signOut()
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
            val now = Timestamp.now()
            val payload = mapOf(
                "uid" to uid,
                "email" to email,
                "displayName" to displayName,
                "isAnonymous" to isAnonymous,
                "createdAt" to now,
                "updatedAt" to now,
                "preferences" to mapOf(
                    "language" to "es",
                    "theme" to "system"
                ),
                "isAdmin" to false
            )
            ref.set(payload).await()
        }
    }
}