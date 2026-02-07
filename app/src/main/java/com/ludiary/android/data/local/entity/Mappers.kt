package com.ludiary.android.data.local.entity

import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.model.*
import com.ludiary.android.data.repository.profile.FirestoreFriendsRepository
import java.time.Instant
import java.util.Locale

// User
/**
 * Convierte una entidad Room [UserEntity] en el modelo del dominio [User].
 */
fun UserEntity.toModel(): User =
    User(
        uid = uid,
        email = email,
        friendCode = friendCode,
        displayName = displayName,
        isAnonymous = isAnonymous,
        createdAt = createdAt,
        updatedAt = updatedAt,
        preferences = UserPreferences(
            language = language,
            theme = theme,
            mentionUserPrefix = mentionUserPrefix.ifBlank { "@" },
            mentionGroupPrefix = mentionGroupPrefix.ifBlank { "#" }
        ),
        isAdmin = isAdmin
    )

fun User.toEntity(): UserEntity =
    UserEntity(
        id = 0,
        uid = uid,
        email = email,
        friendCode = friendCode,
        displayName = displayName ?: "Invitado",
        language = preferences?.language ?: Locale.getDefault().language,
        theme = preferences?.theme ?: "system",
        mentionUserPrefix = preferences?.mentionUserPrefix ?: "@",
        mentionGroupPrefix = preferences?.mentionGroupPrefix ?: "#",
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
        isDeleted = isDeleted,
        deletedAt = deletedAt,
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
        isDeleted = isDeleted,
        deletedAt = deletedAt,

        baseGameVersionAtLastSync = baseGameVersionAtLastSync,
        hasBaseUpdate = hasBaseUpdate,
        syncStatus = syncStatus
    )

/**
 * Convierte un modelo del dominio [UserGame] en una entidad Room [UserGameEntity].
 * @param uid Identificador único del usuario.
 */
fun UserGame.toEntityFromRemote(uid: String): UserGameEntity =
    this.copy(
        userId = uid,
        syncStatus = SyncStatus.CLEAN
    ).toEntity()

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

// Partidas (Session)
/**
 * Convierte una relación Room [SessionWithPlayers] en el modelo de dominio [Session].
 */
fun SessionWithPlayers.toModel(): Session =
    Session(
        id = session.id,
        scope = session.scope,
        ownerUserId = session.ownerUserId,
        groupId = session.groupId,
        gameRef = GameRef(
            type = session.gameRefType,
            id = session.gameRefId
        ),
        gameTitle = session.gameTitle,
        playedAt = session.playedAt,
        location = session.location,
        durationMinutes = session.durationMinutes,
        players = players
            .sortedBy { it.sortOrder }
            .map { it.toModel() },
        overallRating = session.overallRating,
        notes = session.notes,
        syncStatus = session.syncStatus,
        isDeleted = session.isDeleted,
        createdAt = session.createdAt,
        updatedAt = session.updatedAt,
        deletedAt = session.deletedAt
    )

/**
 * Convierte una entidad Room [SessionPlayerEntity] en el modelo de dominio [SessionPlayer].
 */
fun SessionPlayerEntity.toModel(): SessionPlayer =
    SessionPlayer(
        id = playerId,
        displayName = displayName,
        ref = refId
            ?.takeIf { it.isNotBlank() }
            ?.let { PlayerRef(type = refType, id = it) },
        score = score,
        isWinner = isWinner
    )

/**
 * Convierte un modelo del dominio [Session] en una entidad Room [SessionEntity].
 */
fun Session.toEntity(): SessionEntity =
    SessionEntity(
        id = id,
        scope = scope,
        ownerUserId = ownerUserId,
        groupId = groupId,
        gameRefType = gameRef.type,
        gameRefId = gameRef.id,
        gameTitle = gameTitle,
        playedAt = playedAt,
        location = location,
        durationMinutes = durationMinutes,
        overallRating = overallRating,
        notes = notes,
        syncStatus = syncStatus,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

/**
 * Convierte un modelo del dominio [Session] en entidades Room [SessionPlayerEntity].
 */
fun Session.toPlayerEntities(): List<SessionPlayerEntity> =
    players.mapIndexed { idx, p ->
        SessionPlayerEntity(
            sessionId = id,
            playerId = p.id.ifBlank { java.util.UUID.randomUUID().toString() },
            displayName = p.displayName,
            refType = p.ref?.type ?: PlayerRefType.NAME,
            refId = p.ref?.id,
            score = p.score,
            sortOrder = idx,
            isWinner = p.isWinner
        )
    }

// Groups

/**
 * Convierte un índice remoto `users/{uid}/groups/{groupId}` a entidad Room [GroupEntity].
 */
fun RemoteUserGroupIndex.toEntity(): GroupEntity =
    GroupEntity(
        groupId = groupId,
        nameSnapshot = nameSnapshot,
        createdAt = joinedAt,
        updatedAt = updatedAt
    )

/**
 * Convierte una invitación remota `group_invites/{inviteId}` a entidad Room [GroupInviteEntity].
 */
fun RemoteInvite.toEntity(): GroupInviteEntity =
    GroupInviteEntity(
        inviteId = inviteId,
        groupId = groupId,
        groupNameSnapshot = groupNameSnapshot,
        fromUid = fromUid,
        toUid = toUid,
        status = status,
        createdAt = createdAt,
        respondedAt = respondedAt
    )

/**
 * Convierte el resultado de aceptar/cancelar invitación a entidad Room [GroupInviteEntity].
 */
fun InviteSnapshot.toEntity(): GroupInviteEntity =
    GroupInviteEntity(
        inviteId = inviteId,
        groupId = groupId,
        groupNameSnapshot = groupNameSnapshot,
        fromUid = fromUid,
        toUid = toUid,
        status = status,
        createdAt = createdAt,
        respondedAt = respondedAt
    )

/**
 * Convierte el resultado de crear grupo a entidad Room [GroupEntity].
 * Nota: en este proyecto `createdAt` se usa como "joinedAt" (momento en que el usuario entra al grupo).
 */
fun CreatedGroup.toEntity(): GroupEntity =
    GroupEntity(
        groupId = groupId,
        nameSnapshot = name,
        createdAt = now,
        updatedAt = now
    )

/**
 * Helper para construir un miembro de grupo en Room.
 * @param groupId ID del grupo.
 * @param uid UID del miembro.
 * @param joinedAt Momento (epoch millis) en el que se unió.
 */
fun groupMemberEntity(
    groupId: String,
    uid: String,
    joinedAt: Long
): GroupMemberEntity =
    GroupMemberEntity(
        groupId = groupId,
        uid = uid,
        joinedAt = joinedAt
    )

// Friends (Firestore <-> Room)

/**
 * Convierte un modelo remoto [FirestoreFriendsRepository.RemoteFriend] a entidad Room [FriendEntity].
 * @param fallbackNow Timestamp (epoch millis) usado si falta createdAt/updatedAt.
 */
fun FirestoreFriendsRepository.RemoteFriend.toEntity(fallbackNow: Long = System.currentTimeMillis()): FriendEntity {
    val parsedStatus = runCatching { FriendStatus.valueOf(status) }
        .getOrElse { FriendStatus.ACCEPTED } // fallback seguro

    return FriendEntity(
        id = 0L,
        friendUid = friendUid,
        friendCode = friendCode,
        displayName = displayName,
        nickname = nickname,
        status = parsedStatus,
        createdAt = createdAt ?: fallbackNow,
        updatedAt = updatedAt ?: fallbackNow,
        syncStatus = SyncStatus.CLEAN
    )
}