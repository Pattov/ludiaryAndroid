package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// ------------------- GROUP -------------------
@Entity(tableName = "user_groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val nameSnapshot: String,
    val createdAt: Long,
    val updatedAt: Long
)

// ------------------- MEMBERS -------------------
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "uid"]
)
data class GroupMemberEntity(
    val groupId: String,
    val uid: String,
    val joinedAt: Long
)

// ------------------- INVITES -------------------
@Entity(tableName = "group_invites")
data class GroupInviteEntity(
    @PrimaryKey val inviteId: String,
    val groupId: String,
    val groupNameSnapshot: String,
    val fromUid: String,
    val toUid: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long? = null
)