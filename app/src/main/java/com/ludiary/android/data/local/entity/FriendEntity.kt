package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus

@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["friendUid"], unique = true),
        Index(value = ["friendCode"]),
        Index(value = ["status"])
    ]
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val friendCode: String? = null,
    val friendUid: String? = null,

    val displayName: String? = null,
    val nickname: String? = null,

    val status: FriendStatus = FriendStatus.ACCEPTED,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val syncStatus: SyncStatus = SyncStatus.CLEAN
)