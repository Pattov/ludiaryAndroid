package com.ludiary.android.data.repository.notification

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class FunctionsNotificationsRepository(
    private val functions: FirebaseFunctions
) {
    private companion object {
        const val FN_MARK_AS_READ = "notificationsMarkAsRead"
    }

    suspend fun markAsRead(notifId: String) {
        functions.getHttpsCallable(FN_MARK_AS_READ)
            .call(hashMapOf("notifId" to notifId))
            .await()
    }
}