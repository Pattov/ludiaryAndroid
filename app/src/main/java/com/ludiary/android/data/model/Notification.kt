package com.ludiary.android.data.model

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val read: Boolean,
    val data: Map<String, Any?> = emptyMap()
)

data class NotificationStats(
    val unreadCount: Int = 0,
    val updatedAt: Long = 0L
)