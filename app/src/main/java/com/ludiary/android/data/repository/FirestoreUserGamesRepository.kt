package com.ludiary.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.PurchasePrice
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.tasks.await

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
     * Devuelve un flujo que emite un juego del usuario.
     * @param uid Identificador único del usuario.
     * @param userGame Identificador único del juego.
     * @return Juego del usuario.
     */
    suspend fun addUserGame(uid: String, userGame: UserGame) {
        upsertUserGame(uid, userGame)
    }

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
    suspend fun updateUserGame(uid: String, userGame: UserGame) {
        upsertUserGame(uid, userGame)
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

        return snapshot.documents.map { doc ->
            val data = doc.data.orEmpty()

            val amount = (data["purchasePrice"] as? Number)?.toDouble()
            val currency = data["purchaseCurrency"] as? String

            val purchasePrice =
                if (amount != null && currency != null) PurchasePrice(
                    amount = amount,
                    currency = currency
                )
                else null

            UserGame(
                id = doc.id,
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

                // Todo lo que baja entra CLEAN
                syncStatus = SyncStatus.CLEAN
            )
        }
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
        "titleSnapshot" to titleSnapshot,
        "personalRating" to personalRating,
        "language" to language,
        "edition" to edition,
        "notes" to notes,
        "location" to location,
        "condition" to condition,
        "purchaseDate" to purchaseDate,
        "purchasePrice" to purchasePrice?.amount,
        "purchaseCurrency" to purchasePrice?.currency,
        "baseGameVersionAtLastSync" to baseGameVersionAtLastSync,
        "hasBaseUpdate" to hasBaseUpdate
    )