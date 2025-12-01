package com.ludiary.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.dao.UserGameDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.UserGame

/**
 * Implementación de [UserGamesRepository] que opera directamente sobre Firebase
 *
 * @property userGameDao DAO para operaciones de base de datos local.
 * @property firestore Instancia de FirebaseFirestore para acceso a la base de datos de Firestore.
 */
class UserGamesRepositoryImpl(
    private val userGameDao: UserGameDao,
    private val firestore: FirebaseFirestore
) : UserGamesRepository {

    /**
     * Devuelve un flujo que emite una lista de juegos del usuario.
     *
     * @param uid Identificador único del usuario.
     * @return Lista de juegos del usuario.
     */
    override fun getUserGames(uid: String): Flow<List<UserGame>> {
        return userGameDao.getUserGames(uid)
            .map { entities -> entities.map { it.toModel() } }
    }

    /**
     * Devuelve un flujo que emite un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param gameId Identificador único del juego.
     */
    override suspend fun addUserGame(uid: String, userGame: UserGame) {
        val docRef = firestore.collection("users")
            .document(uid)
            .collection("userGames")
            .document(userGame.id)

        docRef.set(userGame)
        userGameDao.upsert(userGame.toEntity())
    }

    /**
     * Actualiza un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param userGame Juego del usuario.
     */
    override suspend fun updateUserGame(uid: String, userGame: UserGame) {
        val docRef = firestore.collection("users")
            .document(uid)
            .collection("userGames")
            .document(userGame.id)

        docRef.set(userGame)
        userGameDao.upsert(userGame.toEntity())
    }

    /**
     * Elimina un juego del usuario.
     *
     * @param uid Identificador único del usuario.
     * @param gameId Identificador único del juego.
     */
    override suspend fun deleteUserGame(uid: String, gameId: String) {
        val docRef = firestore.collection("users")
            .document(uid)
            .collection("userGames")
            .document(gameId)

        docRef.delete()

        val entity = userGameDao.getById(gameId)
        if (entity != null) {
            userGameDao.delete(entity)
        }
    }
}