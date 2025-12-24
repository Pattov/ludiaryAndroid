package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.GameRefType
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["ownerUserId", "playedAt"]),
        Index(value = ["groupId", "playedAt"]),
        Index(value = ["syncStatus"])
    ]
)
data class SessionEntity(
    @PrimaryKey val id: String,

    val scope: SessionScope,
    val ownerUserId: String?,
    val groupId: String?,

    val gameRefType: GameRefType,
    val gameRefId: String,
    val gameTitle: String,

    val playedAt: Long,
    val location: String?,
    val durationMinutes: Int?,

    val overallRating: Int?,
    val notes: String?,

    val winners: List<String>,

    val syncStatus: SyncStatus,
    val isDeleted: Boolean,

    val createdAt: Long?,
    val updatedAt: Long?,
    val deletedAt: Long?
)
