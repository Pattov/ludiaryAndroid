package com.ludiary.android.data.repository.notification

import kotlinx.coroutines.flow.Flow

interface NotificationsRepository {
    fun observeUnreadCount(): Flow<Int>
    fun stopUnreadCountListener()
}
