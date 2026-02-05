package com.ludiary.android.data.repository.profile

import android.content.Context
import com.ludiary.android.data.local.LocalFriendsDataSource

object FriendsRepositoryProvider {
    fun provide(context: Context, local: LocalFriendsDataSource): FriendsRepository =
        FriendsRepositoryLocal(local)
}