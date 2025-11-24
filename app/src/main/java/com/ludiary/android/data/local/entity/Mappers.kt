package com.ludiary.android.data.local.entity

import com.ludiary.android.data.model.*
import java.time.Instant

// User
/**
 * Convierte una entidad Room [UserEntity] en el modelo del dominio [User].
 */
fun UserEntity.toModel(): User =
    User(
        uid = uid,
        email = null,
        displayName = displayName,
        isAnonymous = isAnonymous,
        createdAt = createdAt,
        updatedAt = updatedAt,
        preferences = UserPreferences(
            language = language,
            theme = theme
        ),
        isAdmin = isAdmin
    )

fun User.toEntity(): UserEntity =
    UserEntity(
        id = 0,
        uid = uid,
        displayName = displayName ?: "Invitado",
        language = preferences?.language ?: "es",
        theme = preferences?.theme ?: "system",
        isAnonymous = isAnonymous,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isAdmin = isAdmin
    )


// GameBase
/**
 * Convierte una entidad Room [GameBaseEntity] en el modelo del dominio [GameBase].
 */
fun GameBaseEntity.toModel(): GameBase =
    GameBase(
        id = id,
        title = title,
        year = year,
        designers = designers,
        publishers = publishers,
        bggId = bggId,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        durationMinutes = durationMinutes,
        recommendedAge = recommendedAge,
        weightBgg = weightBgg,
        defaultLanguage = defaultLanguage,
        type = type,
        baseGameId = baseGameId,
        imageUrl = imageUrl,
        approved = approved,
        version = version,
        createdAt = createdAtMillis?.let(Instant::ofEpochMilli),
        updatedAt = updatedAtMillis?.let(Instant::ofEpochMilli)
    )

/**
 * Convierte un modelo del dominio [GameBase] en una entidad Room [GameBaseEntity].
 */
fun GameBase.toEntity(): GameBaseEntity =
    GameBaseEntity(
        id = id,
        title = title,
        year = year,
        designers = designers,
        publishers = publishers,
        bggId = bggId,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        durationMinutes = durationMinutes,
        recommendedAge = recommendedAge,
        weightBgg = weightBgg,
        defaultLanguage = defaultLanguage,
        type = type,
        baseGameId = baseGameId,
        imageUrl = imageUrl,
        approved = approved,
        version = version,
        createdAtMillis = createdAt?.toEpochMilli(),
        updatedAtMillis = updatedAt?.toEpochMilli()
    )

// UserGame
/**
 * Convierte una entidad Room [UserGameEntity] en el modelo del dominio [UserGame].
 */
fun UserGameEntity.toModel(): UserGame =
    UserGame(
        id = id,
        userId = userId,
        gameId = gameId ?: "",
        isCustom = isCustom,
        titleSnapshot = titleSnapshot,
        personalRating = personalRating,
        language = language,
        edition = edition,
        condition = condition,
        location = location,
        purchaseDate = purchaseDate,
        purchasePrice =
            if (purchasePriceAmount != null && purchasePriceCurrency != null) {
                PurchasePrice(
                    amount = purchasePriceAmount,
                    currency = purchasePriceCurrency
                )
            } else {
                null
            },
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        baseGameVersionAtLastSync = baseGameVersionAtLastSync,
        hasBaseUpdate = hasBaseUpdate,
        syncStatus = syncStatus
    )

/**
 * Convierte un modelo del dominio [UserGame] en una entidad Room [UserGameEntity].
 */
fun UserGame.toEntity(): UserGameEntity =
    UserGameEntity(
        id = id,
        userId = userId,
        gameId = gameId.ifEmpty { null },
        isCustom = isCustom,
        titleSnapshot = titleSnapshot,
        personalRating = personalRating,
        language = language,
        edition = edition,
        condition = condition,
        location = location,

        purchaseDate = purchaseDate,
        purchasePriceAmount = purchasePrice?.amount,
        purchasePriceCurrency = purchasePrice?.currency,

        notes = notes,

        createdAt = createdAt,
        updatedAt = updatedAt,

        baseGameVersionAtLastSync = baseGameVersionAtLastSync,
        hasBaseUpdate = hasBaseUpdate,
        syncStatus = syncStatus
    )

// GameSuggestion
/**
 * Convierte una entidad Room [GameSuggestionEntity] en el modelo del dominio [GameSuggestion].
 */
fun GameSuggestionEntity.toModel(): GameSuggestion =
    GameSuggestion(
        id = id,
        title = title,
        year = year,
        designers = designers,
        publishers = publishers,
        bggId = bggId,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        durationMinutes = durationMinutes,
        recommendedAge = recommendedAge,
        weightBgg = weightBgg,
        defaultLanguage = defaultLanguage,
        type = type,
        baseGameId = baseGameId,
        imageUrl = imageUrl,
        reason = reason,
        status = status,
        userId = userId,
        userEmail = userEmail,
        createdAt = createdAtMillis?.let(Instant::ofEpochMilli),
        reviewedAt = reviewedAtMillis?.let(Instant::ofEpochMilli),
        reviewedBy = reviewedBy,
        createdFromUserGameId = createdFromUserGameId
    )

/**
 * Convierte un modelo del dominio [GameSuggestion] en una entidad Room [GameSuggestionEntity].
 */
fun GameSuggestion.toEntity(): GameSuggestionEntity =
    GameSuggestionEntity(
        id = id,
        title = title,
        year = year,
        designers = designers,
        publishers = publishers,
        bggId = bggId,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        durationMinutes = durationMinutes,
        recommendedAge = recommendedAge,
        weightBgg = weightBgg,
        defaultLanguage = defaultLanguage,
        type = type,
        baseGameId = baseGameId,
        imageUrl = imageUrl,
        reason = reason,
        status = status,
        userId = userId,
        userEmail = userEmail,
        createdAtMillis = createdAt?.toEpochMilli(),
        reviewedAtMillis = reviewedAt?.toEpochMilli(),
        reviewedBy = reviewedBy,
        createdFromUserGameId = createdFromUserGameId
    )