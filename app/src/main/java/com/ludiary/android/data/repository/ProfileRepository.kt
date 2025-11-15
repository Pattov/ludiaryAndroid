package com.ludiary.android.data.repository

import com.ludiary.android.data.model.User

interface ProfileRepository {

    suspend fun getOrCreate(): User

    suspend fun update(
        displayName: String?,
        language: String?,
        theme: String?
    ): User

    suspend fun signOut()
}