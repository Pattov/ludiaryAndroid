package com.ludiary.android.data.model

import java.time.Instant

/**
 * Representa a un juego sugerido almacenado en local de la app.
 *
 * @property id Clave primaria fija
 * @property title Título del juego
 * @property year Año de lanzamiento del juego
 * @property designers Desarrolladores del juego
 * @property publishers Editoriales del juego
 * @property bggId Identificador del juego en BGG
 * @property minPlayers Número mínimo de jugadores
 * @property maxPlayers Número máximo de jugadores
 * @property durationMinutes Duración del juego en minutos
 * @property recommendedAge Edad recomendada
 * @property weightBgg Peso del juego en BGG
 * @property defaultLanguage Idioma por defecto del juego
 * @property type Tipo de juego
 * @property baseGameId Identificador del juego base
 * @property imageUrl URL de la imagen del juego
 * @property reason Motivo de la sugerencia
 * @property status Estado de la sugerencia
 * @property userId Identificador del usuario que envió la sugerencia
 * @property userEmail Email del usuario que envió la sugerencia
 * @property createdAt Fecha de creación de la sugerencia
 * @property reviewedAt Fecha de revisión de la sugerencia
 * @property reviewedBy Usuario que revisó la sugerencia
 * @property createdFromUserGameId Identificador del juego que creó la sugerencia
 */
data class GameSuggestion(
    val id: String,
    val title: String,
    val year: Int? = null,
    val designers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val bggId: Long? = null,
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