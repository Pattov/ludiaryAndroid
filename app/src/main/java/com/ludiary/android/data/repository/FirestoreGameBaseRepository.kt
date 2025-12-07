package com.ludiary.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.model.GameBase
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Repositorio de juegos base.
 * @return [GameBaseRepository]
 */
interface FirestoreGameBaseRepository {

    /**
     * Obtiene la lista completa de juegos base.
     * @return Lista de juegos base.
     */
    suspend fun getAllGamesBase(): List<GameBase>

    /**
     * Obtiene la lista de juegos base actualizados desde una fecha.
     * @param lastUpdatedAt Fecha desde la que se considera actualizada.
     */
    suspend fun getGamesBaseUpdatedSince(lastUpdatedAt: Instant?): List<GameBase>
}

/**
 * Implementación del repositorio de juegos base.
 * @param firestore Instancia de Firebase Firestore.
 * @return [FirestoreGameBaseRepository]
 */
class FirestoreGameBaseRepositoryImpl(
    private val firestore: FirebaseFirestore
) : FirestoreGameBaseRepository {

    /**
     * Referencia a la colección de juegos base.
     * @return Referencia a la colección de juegos base.
     */
    private val collection = firestore.collection("games_base")

    /**
     * Obtiene la lista completa de juegos base.
     * @return Lista de juegos base.
     * @throws Exception Si ocurre un error al obtener la lista de juegos base.
     */
    override suspend fun getAllGamesBase(): List<GameBase> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toGameBaseOrNull()
        }
    }

    /**
     * Obtiene la lista de juegos base actualizados desde una fecha.
     * @param lastUpdatedAt Fecha desde la que se considera actualizada.
     * @return Lista de juegos base actualizados.
     * @throws Exception Si ocurre un error al obtener la lista de juegos base actualizados.
     */
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

    /**
     * Convierte un documento de Firestore en un objeto [GameBase].
     * @return Objeto [GameBase] o null si la conversión falla.
     * @throws Exception Si ocurre un error al convertir el documento.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toGameBaseOrNull(): GameBase? {
        val id = id // doc.id
        val title = getString("title") ?: return null

        val bggId: Long? = when (val value = get("bggId")) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }

        return GameBase(
            id = id,
            title = title,
            year = getLong("year")?.toInt(),
            designers = (get("designers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            publishers = (get("publishers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            bggId = bggId,
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
            createdAt = getInstant("createdAt") ?: getInstant("created_at"),
            updatedAt = getInstant("updatedAt") ?: getInstant("updated_at")
        )
    }

    /**
     * Convierte un valor de Firestore en una instancia de [Instant].
     * @param field Nombre del campo en Firestore.
     * @return Instancia de [Instant] o null si el valor es nulo.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.getInstant(field: String): Instant? {
        val value = get(field) ?: return null

        return when (value) {
            is com.google.firebase.Timestamp -> value.toDate().toInstant()
            is java.util.Date -> value.toInstant()
            is Number -> Instant.ofEpochMilli(value.toLong())
            is String -> {
                runCatching { Instant.parse(value) }.getOrNull()
            }
            else -> null
        }
    }

}