package com.ludiary.android.data.repository.library

import android.content.Context
import com.ludiary.android.data.local.LocalUserGamesDataSource

object UserGamesRepositoryProvider {
    fun provide(
        context: Context,
        local: LocalUserGamesDataSource
    ): UserGamesRepository = UserGamesRepositoryLocal(local)
}