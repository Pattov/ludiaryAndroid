package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.ludiary.android.data.model.PlayerRefType

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
