package com.ludiary.android.data.model

/**
 * Representa una partida registrada por el usuario en Ludiary.
 *
 * Cada sesión se asocia a un juego concreto de la ludoteca.
 *
 * Se almacena en la subcolección `/users/{uid}/sessions`.
 *
 * @property id Identificador único de la sesión.
 * @property scope Alcance de la sesión.
 * @property ownerUserId Identificador único del usuario propietario de la sesión.
 * @property groupId Identificador único del grupo al que pertenece la sesión.
 * @property gameRef Referencia al juego de la sesión.
 * @property gameTitle Título del juego de la sesión.
 * @property playedAt Fecha y hora en la que se jugó la sesión.
 * @property location Ubicación en la que se jugó la sesión.
 * @property overallRating Calificación general de la sesión.
 * @property players Lista de jugadores de la sesión.
 * @property notes Notas adicionales de la sesión.
 * @property syncStatus Estado de sincronización entre copia y Firestore.
 * @property isDeleted Indica si la sesión ha sido eliminada.
 * @property createdAt Fecha de creación de la sesión.
 * @property updatedAt Fecha de actualización de la sesión.
 * @property deletedAt Fecha de eliminación de la sesión.
 * @property durationMinutes Duración de la sesión en minutos.
 */
data class Session(
    val id: String = "",

    // Scope / propiedad
    val scope: SessionScope = SessionScope.PERSONAL,
    val ownerUserId: String? = null,
    val groupId: String? = null,

    // Juego
    val gameRef: GameRef,
    val gameTitle: String,

    // Partida
    val playedAt: Long = System.currentTimeMillis(),
    val location: String? = null,
    val durationMinutes: Int? = null,

    // Jugadores
    val players: List<SessionPlayer> = emptyList(),

    // Valoración
    val overallRating: Int? = null,
    val notes: String? = null,

    // Sync
    val syncStatus: SyncStatus = SyncStatus.CLEAN,
    val isDeleted: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)

/**
 * Representa una referencia a un jugador.
 * @property type Tipo de referencia.
 * @property id Identificador único del jugador.
 */
data class PlayerRef(
    val type: PlayerRefType,
    val id: String
)

/**
 * Representa un jugador de una partida.
 * @property id Identificador único del jugador.
 * @property displayName Nombre del jugador.
 * @property ref Referencia al jugador.
 * @property score Puntuación del jugador.
 * @property isWinner Indica si el jugador ha ganado la partida.
 */
data class SessionPlayer(
    val id: String,
    val displayName: String,
    val ref: PlayerRef? = null,
    val score: Int? = null,
    val isWinner: Boolean = false
)

/**
 * Representa una referencia a un juego.
 * @property type Tipo de referencia.
 * @property id Identificador único del juego.
 */
data class GameRef(
    val type: GameRefType,
    val id: String
)