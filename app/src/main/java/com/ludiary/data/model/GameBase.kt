package com.ludiary.data.model

data class GameBase(
    val id: String = "",
    val title: String = "",
    val year: Int? = null,
    val designers: List<String>? = null,
    val publisher: List<String>? = null,
    val bggId: String? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val duration: Int? = null,
    val recommendedAge: Int? = null,
    val weightBGG: Double? = null,
    val defaultLanguage: String? = null,
    val type: String = "fisico",
    val baseGameId: String? = null,
    val imageUrl: String? = null,
    val approved: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
