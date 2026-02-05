package com.ludiary.android.data.repository.profile

import android.content.Context
import com.ludiary.android.data.local.LocalUserDataSource

object ProfileRepositoryProvider {
    fun provide(
        context: Context,
        localUser: LocalUserDataSource
    ): ProfileRepository = throw IllegalStateException("Missing flavor implementation")
}