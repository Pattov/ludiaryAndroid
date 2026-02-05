package com.ludiary.android.data.repository.notification

class FcmTokensRepositoryLocal : FcmTokensRepository {

    override suspend fun upsertToken(uid: String, token: String) {
        // no-op en local
    }

    override suspend fun deleteToken(uid: String, token: String) {
        // no-op en local
    }
}
