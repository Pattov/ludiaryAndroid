package com.ludiary.android.data.model

/**
 * Tipo principal de juego.
 */
enum class GameType {
    FISICO,
    DIGITAL,
    EXPANSION
}

/**
 * Estado fisico de la copia del juego que posee el usuario.
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
 */
enum class SuggestionStatus {
    PENDING,
    APROVED,
    REJECTED
}

/**
 * Estado de sincronización de un juego entre la copia local y Firestore.
 */
enum class SyncStatus {
    CLEAN,
    PENDING,
    CONFLICT
}
