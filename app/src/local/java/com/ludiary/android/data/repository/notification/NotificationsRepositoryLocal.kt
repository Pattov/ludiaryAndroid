package com.ludiary.android.data.repository.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationsRepositoryLocal : NotificationsRepository {
    private val unreadCountFlow = MutableStateFlow(0)

    override fun observeUnreadCount(): Flow<Int> = unreadCountFlow.asStateFlow()

    override fun stopUnreadCountListener() {
        // No hay listener en local
    }
}