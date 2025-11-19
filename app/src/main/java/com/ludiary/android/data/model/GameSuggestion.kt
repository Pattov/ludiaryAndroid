package com.ludiary.android.data.model

import java.time.Instant

data class GameSuggestion(
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

    val reason: String? = null,
    val status: SuggestionStatus = SuggestionStatus.PENDING,

    val userId: String,
    val userEmail: String? = null,

    val createdAt: Instant? = null,
    val reviewedAt: Instant? = null,
    val reviewedBy: String? = null,

    val createdFromUserGameId: String? = null
)
