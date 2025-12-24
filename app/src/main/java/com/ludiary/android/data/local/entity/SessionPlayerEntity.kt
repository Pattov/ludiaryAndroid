package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.ludiary.android.data.model.PlayerRefType

/**
 * Representa una partida registrada por el usuario en Ludiary.
 * Cada sesión se asocia a un juego concreto de la ludoteca.
 *
 * @property sessionId Identificador único de la sesión.
 * @property playerId Identificador único del jugador.
 * @property displayName Nombre del jugador.
 * @property refType Tipo de referencia del jugador.
 * @property refId Identificador de referencia del jugador.
 * @property score Puntuación del jugador.
 * @property sortOrder Orden de aparición del jugador en la lista.
 */
@Entity(
    tableName = "session_players",
    primaryKeys = ["sessionId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId", "sortOrder"])
    ]
)

data class SessionPlayerEntity(
    val sessionId: String,
    val playerId: String,

    val displayName: String,

    val refType: PlayerRefType?,
    val refId: String?,

    val score: Int?,
    val sortOrder: Int
)
