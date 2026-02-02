package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import com.ludiary.android.data.repository.notification.NotificationsRepository
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel encargado de exponer el estado de notificaciones a la UI.
 * @param repo Repositorio de notificaciones.
 */
class NotificationsViewModel(
    private val repo: NotificationsRepository
) : ViewModel() {

    val unreadCount: Flow<Int> = repo.observeUnreadCount()

    /**
     * Callback del ciclo de vida del ViewModel.
     *
     * Se utiliza para detener el listener de notificaciones cuando la UI asociada deja de existir, liberando recursos.
     */
    override fun onCleared() {
        repo.stopUnreadCountListener()
        super.onCleared()
    }
}
