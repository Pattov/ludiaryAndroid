package com.ludiary.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.UserGame

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
    fun deleteUserGame(uid: String) {
        userGamesCollection(uid)
            .document()
            .delete()
    }

    /**
     * Actualiza un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param userGame Juego del usuario.
     */
    fun updateUserGame(uid: String, userGame: UserGame) {
        if (userGame.id.isBlank()) return

        val data = userGame.toFirestoreMapWithoutId()
        userGamesCollection(uid)
            .document(userGame.id)
            .set(data)
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