package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Int = 0,
    val uid: String = "local-guest",
    val displayName: String = "Invitado",
    val language: String = "es",
    val theme: String = "system",
    val isAnonymous: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val isAdmin: Boolean = false
)