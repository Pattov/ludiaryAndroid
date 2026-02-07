package com.ludiary.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.profile.FirestoreProfileRepository
import com.ludiary.android.data.repository.library.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.profile.ProfileRepository
import com.ludiary.android.data.repository.library.UserGamesRepository
import com.ludiary.android.data.repository.library.UserGamesRepositoryImpl

class ProfileViewModelFactory (
    context: Context
) : ViewModelProvider.Factory {

    private val db = LudiaryDatabase.getInstance(context.applicationContext)

    private val localDS = LocalUserDataSource(db)
    private val profileRepo: ProfileRepository = FirestoreProfileRepository(
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance(),
        localDS
    )

    private val localUserGames = LocalUserGamesDataSource(db.userGameDao())
    private val remoteUserGames = FirestoreUserGamesRepository(FirebaseFirestore.getInstance())
    private val userGamesRepo: UserGamesRepository = UserGamesRepositoryImpl(
        local = localUserGames,
        remote = remoteUserGames
    )
    private val localFriends = LocalFriendsDataSource(db.friendDao())

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(
            repo = profileRepo,
            userGamesRepo = userGamesRepo,
            localUserGames = localUserGames,
            localFriends = localFriends,
            db = db
        ) as T
    }
}