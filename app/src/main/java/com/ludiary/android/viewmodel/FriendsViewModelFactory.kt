package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.repository.FriendsRepository
import com.ludiary.android.data.repository.GroupsRepository

class FriendsViewModelFactory(
    private val friendsRepo: FriendsRepository,
    private val groupsRepo: GroupsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FriendsViewModel(friendsRepo, groupsRepo) as T
    }
}