package com.ludiary.android.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.ludiary.android.data.local.entity.GameBaseEntity
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity
import com.ludiary.android.data.local.entity.UserGameEntity


data class SessionWithPlayers(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val players: List<SessionPlayerEntity>
)

// ---------- Ludoteca ----------

data class UserGameWithBaseGame(
    @Embedded val userGame: UserGameEntity,
    @Relation(
        parentColumn = "gameId",
        entityColumn = "id"
    )
    val baseGame: GameBaseEntity?
)

