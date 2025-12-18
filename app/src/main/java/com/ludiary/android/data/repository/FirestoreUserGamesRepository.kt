package com.ludiary.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
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
     *
     * @param uid Identificador único del usuario.
     */
    private fun userGamesCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("userGames")

    /**
     * Devuelve un flujo que emite un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param userGame Identificador único del juego.
     * @return Juego del usuario.
     */
    fun addUserGame(uid: String, userGame: UserGame) {
        val data = userGame.toFirestoreMapWithoutId()
        userGamesCollection(uid).add(data)
    }

    /**
     * Elimina un juego del usuario.
     *
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
     *
     * @param uid Identificador único del usuario.
     * @param userGame Juego del usuario.
     */
    suspend fun updateUserGame(uid: String, userGame: UserGame) {
        require(userGame.id.isNotBlank()) { "UserGame.id no puede estar vacío para upsert en Firestore" }
        val data = userGame.toFirestoreMapWithoutId()
        userGamesCollection(uid)
            .document(userGame.id)
            .set(data)
            .await()
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