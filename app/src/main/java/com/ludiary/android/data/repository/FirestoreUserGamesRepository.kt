package com.ludiary.android.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.UserGame
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreUserGamesRepository (
    private val firestore: FirebaseFirestore
) : UserGamesRepository {

    private fun userGamesCollection(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("userGames")

    override fun getUserGames(uid: String): Flow<List<UserGame>> = callbackFlow {

        val registration = userGamesCollection(uid)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val games: List<UserGame> = querySnapshot
                    ?.documents
                    ?.mapNotNull { doc: DocumentSnapshot ->
                        doc.toUserGame()
                    }
                    ?: emptyList()

                trySend(games)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun addUserGame(uid: String, userGame: UserGame) {
        val data = userGame.toFirestoreMapWithoutId()
        userGamesCollection(uid).add(data)
    }

    override suspend fun deleteUserGame(uid: String, gameId: String) {
        userGamesCollection(uid)
            .document()
            .delete()
    }

    override suspend fun updateUserGame(uid: String, userGame: UserGame) {
        if (userGame.id.isBlank()) return

        val data = userGame.toFirestoreMapWithoutId()
        userGamesCollection(uid)
            .document(userGame.id)
            .set(data)
    }
}

private fun DocumentSnapshot.toUserGame() : UserGame? {
    val titleSnapshot = getString("title") ?: return null

    return UserGame(
        id = id,
        userId = getString("userId") ?: "",
        gameId = getString("gameId") ?: "",
        isCustom = getBoolean("isCustom") ?: false,
        titleSnapshot = titleSnapshot,
        personalRating = getDouble("personalRating")?.toFloat(),
        language = getString("language"),
        edition = getString("edition"),
        notes = getString("notes"),
        location = getString("location"),
        condition = getString("condition"),
        purchaseDate = getLong("purchaseDate"),
        baseGameVersionAtLastSync = (getLong("baseGameVersionAtLastSync")?.toInt()),
        hasBaseUpdate = getBoolean("hasBaseUpdate") ?: false
    )
}

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