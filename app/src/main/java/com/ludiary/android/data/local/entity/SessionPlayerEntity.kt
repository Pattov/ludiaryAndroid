package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.ludiary.android.data.model.PlayerRefType

/**
 * Jugador asociado a una partida.
 *
 * @property sessionId Id de la partida a la que pertenece.
 * @property playerId Id del jugador dentro de esa partida.
 * @property displayName Nombre visible del jugador.
 * @property refType Tipo de referencia.
 * @property refId Id de referencia si aplica.
 * @property score Puntuación del jugador.
 * @property sortOrder Orden del jugador en el formulario/lista.
 * @property isWinner Indica si el jugador marcó “ganador”.
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

    val refType: PlayerRefType,
    val refId: String?,

    val score: Int?,
    val sortOrder: Int,
    val isWinner: Boolean
)