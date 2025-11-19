package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.SyncStatus

@Entity(tableName = "user_games")
data class UserGameEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val gameId: String?, // Puede ser nulo si es un juego personalizado
    val isCustom: Boolean = false,

    val titleSnapshot: String,
    val personalRating: Float? = null,
    val language: String? = null,
    val edition: String? = null,
    val condition: String? = null,
    val location: String? = null,

    val purchaseDate: Long? = null,
    val purchasePriceAmount: Double? = null,
    val purchasePriceCurrency: String? = null,

    val notes: String? = null,

    val createdAt: Long? = null,
    val updatedAt: Long? = null,

    val baseGameVersionAtLastSync: Int? = null,
    val hasBaseUpdate: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.CLEAN

)
