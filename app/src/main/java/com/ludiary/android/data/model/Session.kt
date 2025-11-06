package com.ludiary.android.data.model

/**
 * Representa una partida registrada por el usuario en Ludiary.
 *
 * Cada sesión se asocia a un juego concreto de la ludoteca.
 *
 * Se almacena en la subcolección `/users/{uid}/sessions`.
 *
 * @property id Identificador único de la sesión.
 * @property gameId Identificador único del juego.
 * @property date Fecha de la sesión.
 * @property players Lista de jugadores de la sesión.
 * @property winner Jugador ganador de la sesión.
 * @property scores Puntuaciones de los jugadores.
 * @property liked Indica si la sesión ha sido metida en favoritos.
 * @property notes Notas de la sesión.
 * @property durationMinutes Duración de la sesión en minutos.
 */
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
