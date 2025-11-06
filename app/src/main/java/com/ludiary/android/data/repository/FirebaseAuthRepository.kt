package com.ludiary.android.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {
    override val currentUser: User?
        get() = auth.currentUser?.let{
            User(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName,
                isAnonymous = it.isAnonymous
            )
        }

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

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            AuthResult.Success(User())
        }catch (e: Exception){
            AuthResult.Error(e.message ?: "No se pudo enviar el email")
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

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
                "preferences" to mapOf(
                    "language" to "es",
                    "theme" to "system"
                )
            )
            ref.set(payload).await()
        }
    }
}