package com.ludiary.android.data.repository.auth

import android.content.Context
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.util.ResourceProvider

object AuthRepositoryProvider {
    fun provide(
        context: Context,
        localUserDataSource: LocalUserDataSource,
        resourceProvider: ResourceProvider
    ): AuthRepository = AuthRepositoryLocal(localUserDataSource, resourceProvider)
}