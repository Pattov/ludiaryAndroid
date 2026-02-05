package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.library.UserGamesRepositoryProvider
import com.ludiary.android.data.repository.profile.ProfileRepositoryProvider

class ProfileViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val db = LudiaryDatabase.getInstance(appContext)

    private val localUser = LocalUserDataSource(db)
    private val localUserGames = LocalUserGamesDataSource(db.userGameDao())
    private val localFriends = LocalFriendsDataSource(db.friendDao())

    private val profileRepo = ProfileRepositoryProvider.provide(
        context = appContext,
        localUser = localUser
    )

    private val userGamesRepo = UserGamesRepositoryProvider.provide(
        context = appContext,
        local = localUserGames
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(
            repo = profileRepo,
            userGamesRepo = userGamesRepo,
            localUserGames = localUserGames,
            localFriends = localFriends
        ) as T
    }
}
