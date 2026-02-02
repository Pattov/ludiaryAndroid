package com.ludiary.android.data.repository.notification

interface FcmTokensRepository {
    suspend fun upsertToken(uid: String, token: String)
    suspend fun deleteToken(uid: String, token: String)
}