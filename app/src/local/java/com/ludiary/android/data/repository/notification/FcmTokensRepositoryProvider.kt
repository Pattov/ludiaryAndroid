package com.ludiary.android.data.repository.notification

import android.content.Context

object FcmTokensRepositoryProvider {
    fun provide(context: Context): FcmTokensRepository = FcmTokensRepositoryLocal()
}