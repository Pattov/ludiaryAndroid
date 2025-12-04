package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.GameType

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
 * @property createdAtMillis Fecha de creación del juego
 * @property updatedAtMillis Fecha de actualización del juego
 */
@Entity(tableName = "games_base")
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
