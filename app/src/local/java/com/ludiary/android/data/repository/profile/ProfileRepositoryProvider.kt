package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences
import java.util.Locale

class ProfileRepositoryLocal(
    private val localUser: LocalUserDataSource
) : ProfileRepository {

    override suspend fun getOrCreate(): User = localUser.getLocalUser()

    override suspend fun update(displayName: String?, language: String?, theme: String?): User {
        val current = localUser.getLocalUser()
        val updated = current.copy(
            displayName = displayName ?: current.displayName,
            preferences = UserPreferences(
                language = language ?: current.preferences?.language ?: Locale.getDefault().language,
                theme = theme ?: current.preferences?.theme ?: "system"
            )
        )
        localUser.saveLocalUser(updated)
        return updated
    }

    override suspend fun signOut() {
        localUser.clear()
    }
}