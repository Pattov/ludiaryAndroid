package com.ludiary.android.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.ludiary.android.data.local.entity.GameBaseEntity
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.local.entity.UserGameEntity

/**
 * Representa una partida junto con sus jugadores.
 * @property session Partida.
 * @property players Lista de jugadores.
 */
data class SessionWithPlayers(
    @Embedded val session: SessionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
        entity = SessionPlayerEntity::class
    )
    val players: List<SessionPlayerEntity>
)

// ---------- Ludoteca ----------

/**
 * Representa un juego junto con su base de datos.
 * @property userGame Juego del usuario
 * @property baseGame Base de datos del juego
 */
data class UserGameWithBaseGame(
    @Embedded val userGame: UserGameEntity,
    @Relation(
        parentColumn = "gameId",
        entityColumn = "id"
    )
    val baseGame: GameBaseEntity?
)

