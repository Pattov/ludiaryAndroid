package com.ludiary.android.data.local

import androidx.room.TypeConverter
import com.ludiary.android.data.model.*

/**
 * Conversores personalizados para tipos de datos no soportados por Room.
 */
class LudiaryConverters {

    //List<String> <-> String

    /** Convierte una lista de String en una cadena separada por "|". */
    @TypeConverter fun fromStringList(list: List<String>?): String? = list?.joinToString(separator = "|")

    /** Convierte una cadena separada por "|" en lista de String. */
    @TypeConverter fun toStringList(value: String?): List<String> = value?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
    // Enums
    @TypeConverter fun fromGameType(type: GameType?): String? = type?.name
    @TypeConverter fun toGameType(value: String?): GameType? = value?.let(GameType::valueOf)

    /** Convierte un objeto [SuggestionStatus] en una cadena para su almacenamiento en Room. */
    @TypeConverter fun fromSuggestionStatus(status: SuggestionStatus?): String? = status?.name

    /** Convierte una cadena almacenada en Room en un objeto [SuggestionStatus]. */
    @TypeConverter fun toSuggestionStatus(value: String?): SuggestionStatus? = value?.let(SuggestionStatus::valueOf)

    @TypeConverter fun fromSyncStatus(status: SyncStatus?): String? = status?.name
    @TypeConverter fun toSyncStatus(value: String?): SyncStatus? = value?.let(SyncStatus::valueOf)

    // SessionScope <-> String
    /** Convierte un [SessionScope] en String para guardarlo en Room. */
    @TypeConverter fun fromSessionScope(scope: SessionScope?): String? = scope?.name

    /** Convierte un String en [SessionScope]. */
    @TypeConverter fun toSessionScope(value: String?): SessionScope? = value?.let(SessionScope::valueOf)

    // GameRefType <-> String
    /** Convierte un [GameRefType] en String para guardarlo en Room. */
    @TypeConverter fun fromGameRefType(type: GameRefType?): String? = type?.name

    /** Convierte un String en [GameRefType]. */
    @TypeConverter fun toGameRefType(value: String?): GameRefType? = value?.let(GameRefType::valueOf)


    // PlayerRefType <-> String
    /** Convierte un [PlayerRefType] en String para guardarlo en Room.*/
    @TypeConverter fun fromPlayerRefType(type: PlayerRefType?): String? = type?.name

    /** Convierte un String en [PlayerRefType].*/
    @TypeConverter fun toPlayerRefType(value: String?): PlayerRefType? = value?.let(PlayerRefType::valueOf)

    //Friend
    /** Convierte un [FriendStatus] en una cadena para su almacenamiento en Room. */
    @TypeConverter fun fromStatus(value: FriendStatus): String = value.name

    /** Convierte un String en [FriendStatus] */
    @TypeConverter fun toStatus(value: String): FriendStatus = FriendStatus.valueOf(value)
}