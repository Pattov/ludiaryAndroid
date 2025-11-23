package com.ludiary.android.data.local

import androidx.room.TypeConverter
import com.ludiary.android.data.model.*
import java.time.Instant
import java.time.LocalDate

/**
 * Conversores personalizados para tipos de datos no soportados por Room.
 */
class LudiaryConverters {

    /**
     * Convierte una lista de [String] en una [String] separada por "|" y viceversa.
     *
     * @return [String] o null si la lista es null
     */
    // List<String> <-> String
    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.joinToString(separator = "|")

    /**
     * Convierte una [String] separada por "|" en una lista de [String].
     *
     * @return [List<String>] o una lista vacía si la cadena es null o vacía
     */
    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    // Instant <-> Long
    /**
     * Convierte un objeto [Instant] en milisegundos desde época Unix.
     *
     * @return Milisegundos desde 1970 o null si Instant es null
     */
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? =
        instant?.toEpochMilli()

    /**
     *  Convierte milisegundos desde época Unix en un objeto [Instant].
     *
     *  @return Instant o null si milisegundos es null
     */
    @TypeConverter
    fun toInstant(millis: Long?): Instant? =
        millis?.let(Instant::ofEpochMilli)

    // LocalDate <-> String
    /**
     * Convierte un objeto [LocalDate] en una cadena ISO(yyyy-MM-dd).
     *
     * @return Cadena formateada o null si la fecha es null
     */
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? =
        date?.toString()

    /**
     * Convierte una cadena en formato ISO (yyyy-MM-dd) en un objeto [LocalDate].
     *
     * @return LocalDate o null si la cadena es null
     */
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let(LocalDate::parse)

    // Enums
    /**
     * Convierte un objeto [GameType] en una cadena.
     */
    @TypeConverter
    fun fromGameType(type: GameType?): String? =
        type?.name

    /**
     * Convierte una cadena en un objeto [GameType].
     */
    @TypeConverter
    fun toGameType(value: String?): GameType? =
        value?.let(GameType::valueOf)

    /**
     * Convierte un objeto [GameCondition] en una cadena.
     */
    @TypeConverter
    fun fromGameCondition(condition: GameCondition?): String? =
        condition?.name

    /**
     * Convierte una cadena en un objeto [GameCondition].
     */
    @TypeConverter
    fun toGameCondition(value: String?): GameCondition? =
        value?.let(GameCondition::valueOf)

    /**
     * Convierte un objeto [SuggestionStatus] en una cadena.
     */
    @TypeConverter
    fun toSuggestionStatus(value: String?): SuggestionStatus? =
        value?.let(SuggestionStatus::valueOf)

    /**
     * Convierte una cadena en un objeto [SuggestionStatus].
     */
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? =
        status?.name

    /**
     * Convierte una cadena en un objeto [SyncStatus].
     */
    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? =
        value?.let(SyncStatus::valueOf)

}