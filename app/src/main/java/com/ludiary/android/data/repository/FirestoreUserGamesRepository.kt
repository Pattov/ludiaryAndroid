package com.ludiary.android.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.PurchasePrice
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Implementación de [UserGamesRepository] que opera directamente sobre Firebase
 */
class FirestoreUserGamesRepository (
    private val firestore: FirebaseFirestore
) {

    /**
     * Devuelve una referencia a la colección de juegos del usuario en Firestore.
     * @param uid Identificador único del usuario.
     */
    private fun userGamesCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("userGames")

    /**
     * Elimina un juego del usuario.
     * @param uid Identificador único del usuario.
     */
    suspend fun deleteUserGame(uid: String, gameId: String) {
        if (gameId.isBlank()) return
        userGamesCollection(uid)
            .document(gameId)
            .delete()
            .await()
    }

    /**
     * Crea o actualiza un userGame
     * @param uid Identificador único del usuario.
     * @param userGame Juego del usuario.
     */
    suspend fun upsertUserGame(uid: String, userGame: UserGame) {
        require(userGame.id.isNotBlank()) { "UserGame.id no puede estar vacío para upsert en Firestore" }

        val data = userGame.toFirestoreMapWithoutId()
        userGamesCollection(uid)
            .document(userGame.id)
            .set(data)
            .await()
    }

    /**
     * Se devuelve una lista de juegos del usuario.
     * @param uid Identificador único del usuario.
     * @return Lista de juegos del usuario en Firebase.
     */
    suspend fun fetchAll(uid: String): List<UserGame> {
        val snapshot = userGamesCollection(uid).get().await()
        return snapshot.documents.map { doc -> doc.toUserGame(uid) }
    }

    suspend fun fetchChangedSince(uid: String, since: Long): List<UserGame> {
        val snapshot = userGamesCollection(uid)
            .whereGreaterThan("updatedAt", since)
            .get()
            .await()

        return snapshot.documents.map { doc -> doc.toUserGame(uid) }
    }
}

/**
 * Convierte un [UserGame] a un [Map] de Firestore.
 */
private fun UserGame.toFirestoreMapWithoutId(): Map<String, Any?> =
    mapOf(
        "userId" to userId,
        "gameId" to gameId,
        "isCustom" to isCustom,
        "title" to titleSnapshot.ifBlank { null },
        "titleSnapshot" to titleSnapshot.ifBlank { null },
        "personalRating" to personalRating,
        "language" to language,
        "edition" to edition,
        "condition" to condition,
        "location" to location,
        "notes" to notes,
        "purchaseDate" to purchaseDate?.let {
            Timestamp(Date(it))
        },
        "purchasePrice" to purchasePrice?.let {
            mapOf(
                "amount" to it.amount,
                "currency" to it.currency
            )
        },
        "baseGameVersionAtLastSync" to baseGameVersionAtLastSync,
        "hasBaseUpdate" to hasBaseUpdate,
        "createdAt" to createdAt?.let { Timestamp(Date(it)) },
        "updatedAt" to Timestamp(Date(updatedAt ?: System.currentTimeMillis()))
    )

private fun com.google.firebase.firestore.DocumentSnapshot.toUserGame(uid: String): UserGame {
    val data = this.data.orEmpty()

    val amount = (data["purchasePrice"] as? Number)?.toDouble()
    val currency = data["purchaseCurrency"] as? String
    val purchasePrice =
        if (amount != null && currency != null) PurchasePrice(amount = amount, currency = currency) else null

    return UserGame(
        id = this.id,
        userId = (data["userId"] as? String) ?: uid,
        gameId = (data["gameId"] as? String) ?: "",
        isCustom = (data["isCustom"] as? Boolean) ?: false,
        titleSnapshot = (data["titleSnapshot"] as? String) ?: "",
        personalRating = (data["personalRating"] as? Number)?.toFloat(),
        language = data["language"] as? String,
        edition = data["edition"] as? String,
        condition = data["condition"] as? String,
        location = data["location"] as? String,
        purchaseDate = (data["purchaseDate"] as? Number)?.toLong(),
        purchasePrice = purchasePrice,
        notes = data["notes"] as? String,
        createdAt = (data["createdAt"] as? Number)?.toLong(),
        updatedAt = (data["updatedAt"] as? Number)?.toLong(),
        baseGameVersionAtLastSync = (data["baseGameVersionAtLastSync"] as? Number)?.toInt(),
        hasBaseUpdate = (data["hasBaseUpdate"] as? Boolean) ?: false,
        syncStatus = SyncStatus.CLEAN
    )
}