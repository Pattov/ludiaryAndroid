package com.ludiary.android.data.model

import java.time.Instant

/**
 * Representa a un juego almacenado en local de la app.
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
 * @property approved Indica si el juego ha sido aprobado
 * @property version Versión del juego
 * @property createdAt Fecha de creación del juego
 * @property updatedAt Fecha de actualización del juego
 */
data class GameBase(
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
    val approved: Boolean = true,
    val version: Int = 1,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)