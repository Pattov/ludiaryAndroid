package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.GameType

@Entity(tableName = "game_base")
data class GameBaseEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val year: Int? = null,
    val designers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val bggId: String? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val durationMinutes: Int? = null,
    val recommendedAge: Int? = null,
    val weightBgg: Double? = null,
    val defaultLanguage: String? = null,
    val type: GameType = GameType.FISICO,
    val baseGameId: String? = null,
    val imageUrl: String? = null,
    val approved: Boolean = true,
    val version: Int = 1,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
)
