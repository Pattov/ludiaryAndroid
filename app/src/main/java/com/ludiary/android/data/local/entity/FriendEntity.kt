package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus

@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["friendUid"], unique = true)
    ]
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val friendUid: String? = null,

    // Para mostrar y para reintentos offline antes de resolver uid
    val friendCode: String? = null,

    val displayName: String? = null,
    val nickname: String? = null,

    val status: FriendStatus,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val syncStatus: SyncStatus = SyncStatus.PENDING
)