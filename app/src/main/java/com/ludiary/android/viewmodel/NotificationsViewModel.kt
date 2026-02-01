package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import com.ludiary.android.data.repository.notification.NotificationsRepository
import kotlinx.coroutines.flow.Flow

class NotificationsViewModel(
    private val repo: NotificationsRepository
) : ViewModel() {

    val unreadCount: Flow<Int> = repo.observeUnreadCount()

    override fun onCleared() {
        repo.stopUnreadCountListener()
        super.onCleared()
    }
}
