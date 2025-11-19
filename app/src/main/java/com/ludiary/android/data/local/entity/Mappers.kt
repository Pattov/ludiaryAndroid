package com.ludiary.android.data.local.entity

import com.ludiary.android.data.model.*
import java.time.Instant

// GameBase

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