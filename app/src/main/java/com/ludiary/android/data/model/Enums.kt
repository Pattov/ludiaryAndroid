package com.ludiary.android.data.model

enum class GameType {
    FISICO,
    DIGITAL,
    EXPANSION
}

enum class GameCondition {
    MINT,
    EXCELENTE,
    BUENO,
    ACEPTABLE,
    POBRE
}

enum class SuggestionStatus {
    PENDING,
    APROVED,
    REJECTED
}

enum class SyncStatus {
    CLEAN,
    PENDING,
    CONFLICT
}
