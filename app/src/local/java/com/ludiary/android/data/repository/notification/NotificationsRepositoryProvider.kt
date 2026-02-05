package com.ludiary.android.data.repository.notification

import android.content.Context

object NotificationsRepositoryProvider {
    fun provide(context: Context): NotificationsRepository =
        NotificationsRepositoryLocal()
}
