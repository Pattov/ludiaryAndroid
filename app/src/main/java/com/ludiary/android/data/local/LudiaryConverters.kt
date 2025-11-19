package com.ludiary.android.data.local

import androidx.room.TypeConverter
import com.ludiary.android.data.model.*
import java.time.Instant
import java.time.LocalDate

class LudiaryConverters {

    // List<String> <-> String
    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.joinToString(separator = "|")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    // Instant <-> Long
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? =
        instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(millis: Long?): Instant? =
        millis?.let(Instant::ofEpochMilli)

    // LocalDate <-> String
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? =
        date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let(LocalDate::parse)

    // Enums
    @TypeConverter
    fun fromGameType(type: GameType?): String? =
        type?.name

    @TypeConverter
    fun toGameType(value: String?): GameType? =
        value?.let(GameType::valueOf)

    @TypeConverter
    fun fromGameCondition(condition: GameCondition?): String? =
        condition?.name

    @TypeConverter
    fun toGameCondition(value: String?): GameCondition? =
        value?.let(GameCondition::valueOf)

    @TypeConverter
    fun toSuggestionStatus(value: String?): SuggestionStatus? =
        value?.let(SuggestionStatus::valueOf)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? =
        status?.name

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? =
        value?.let(SyncStatus::valueOf)

}