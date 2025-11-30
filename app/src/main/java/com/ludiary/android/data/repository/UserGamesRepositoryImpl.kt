package com.ludiary.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.dao.UserGameDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.UserGame



class UserGamesRepositoryImpl(
    private val userGameDao: UserGameDao,
    private val firestore: FirebaseFirestore
) : UserGamesRepository {

    override fun getUserGames(uid: String): Flow<List<UserGame>> {
        return userGameDao.getUserGames(uid)
            .map { entities -> entities.map { it.toModel() } }
    }

    override suspend fun addUserGame(uid: String, userGame: UserGame) {
        val docRef = firestore.collection("users")
            .document(uid)
            .collection("userGames")
            .document(userGame.id)

        docRef.set(userGame)
        userGameDao.upsert(userGame.toEntity())
    }

    override suspend fun updateUserGame(uid: String, userGame: UserGame) {
        val docRef = firestore.collection("users")
            .document(uid)
            .collection("userGames")
            .document(userGame.id)

        docRef.set(userGame)
        userGameDao.upsert(userGame.toEntity())
    }

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