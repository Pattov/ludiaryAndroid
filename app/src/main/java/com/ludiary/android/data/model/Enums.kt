package com.ludiary.android.data.model

/**
 * Tipo principal de juego.
 * @property FISICO Juego físico.
 * @property DIGITAL Juego digital.
 * @property EXPANSION Juego de expansión.
 */
enum class GameType {
    FISICO,
    DIGITAL,
    EXPANSION
}

/**
 * Estado fisico de la copia del juego que posee el usuario.
 * @property MINT Juego en perfecto estado.
 * @property EXCELENTE Juego en excelente estado.
 * @property BUENO Juego en buen estado.
 * @property ACEPTABLE Juego en un buen estado.
 * @property POBRE Juego en un estado pobre.
 */
enum class GameCondition {
    MINT,
    EXCELENTE,
    BUENO,
    ACEPTABLE,
    POBRE
}

/**
 * Estado de la sugerencia enviada por un usuario.
 * @property PENDING Sugerencia pendiente de aprobación.
 * @property APPROVED Sugerencia aprobada.
 * @property REJECTED Sugerencia rechazada.
 */
enum class SuggestionStatus {
    PENDING,
    APPROVED,
    REJECTED
}

/**
 * Estado de sincronización de un juego entre la copia local y Firestore.
 * @property CLEAN Juego sincronizado correctamente.
 * @property PENDING Juego pendiente de sincronización.
 * @property CONFLICT Juego con conflicto de sincronización.
 * @property DELETED Juego eliminado.
 */
enum class SyncStatus {
    CLEAN,
    PENDING,
    CONFLICT,
    DELETED
}

/**
 * Alcance de una partida.
 * @property PERSONAL Partida personal.
 * @property GROUP Partida grupal.
 */
enum class SessionScope { PERSONAL, GROUP }

/**
 * Tipo de referencia a un juego.
 * @property BASE Juego base.
 * @property USER Juego del usuario.
 * @property SUGGESTION Juego sugerido.
 */
enum class GameRefType { BASE, USER, SUGGESTION }

/**
 * Tipo de referencia a un jugador.
 * @property LUDIARY_USER Jugador de Ludiary.
 * @property GROUP_MEMBER Jugador miembro de un grupo.
 */
enum class PlayerRefType { NAME, LUDIARY_USER, GROUP_MEMBER }