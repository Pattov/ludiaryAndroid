package com.ludiary.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.GameBase
import kotlinx.coroutines.tasks.await
import java.time.Instant


interface FirestoreGameBaseRepository {

    suspend fun getAllGamesBase(): List<GameBase>

    suspend fun getGamesBaseUpdatedSince(lastUpdatedAt: Instant?): List<GameBase>
}

class FirestoreGameBaseRepositoryImpl(
    private val firestore: FirebaseFirestore
) : FirestoreGameBaseRepository {

    private val collection = firestore.collection("games_base")

    override suspend fun getAllGamesBase(): List<GameBase> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toGameBaseOrNull()
        }
    }

    override suspend fun getGamesBaseUpdatedSince(lastUpdatedAt: Instant?): List<GameBase> {
        // Si no hay fecha de última actualización, hacemos sync completa
        if (lastUpdatedAt == null) return getAllGamesBase()

        val lastTimestamp = com.google.firebase.Timestamp(lastUpdatedAt.epochSecond, lastUpdatedAt.nano)

        val snapshot = collection
            .whereGreaterThan("updatedAt", lastTimestamp)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toGameBaseOrNull()
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toGameBaseOrNull(): GameBase? {
        val id = id // doc.id
        val title = getString("title") ?: return null

        return GameBase(
            id = id,
            title = title,
            year = getLong("year")?.toInt(),
            designers = (get("designers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            publishers = (get("publishers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            bggId = getString("bggId"),
            minPlayers = getLong("minPlayers")?.toInt(),
            maxPlayers = getLong("maxPlayers")?.toInt(),
            durationMinutes = getLong("durationMinutes")?.toInt()
                ?: getLong("duration")?.toInt(), // por si en la web lo llamas duration
            recommendedAge = getLong("recommendedAge")?.toInt(),
            weightBgg = getDouble("weightBGG") ?: getDouble("weightBgg"),
            defaultLanguage = getString("defaultLanguage"),
            type = runCatching {
                val typeStr = getString("type") ?: "FISICO"
                com.ludiary.android.data.model.GameType.valueOf(typeStr.uppercase())
            }.getOrDefault(com.ludiary.android.data.model.GameType.FISICO),
            baseGameId = getString("baseGameId"),
            imageUrl = getString("imageUrl"),
            approved = getBoolean("approved") ?: true,
            version = getLong("version")?.toInt() ?: 1,
            createdAt = (getTimestamp("createdAt") ?: getTimestamp("created_at"))?.toDate()
                ?.toInstant(),
            updatedAt = (getTimestamp("updatedAt") ?: getTimestamp("updated_at"))?.toDate()
                ?.toInstant()
        )
    }
}