package com.ludiary.data.model

data class Session(
    val id: String = "",
    val gameId: String = "",
    val date: Long = System.currentTimeMillis(),
    val players: List<String> = emptyList(),
    val winner: String? = null,
    val scores: Map<String, Int>? = null,
    val liked: Boolean? = null,
    val notes: String? = null,
    val durationMinutes: Int? = null
)
