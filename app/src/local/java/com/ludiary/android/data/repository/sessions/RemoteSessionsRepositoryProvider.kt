package com.ludiary.android.data.repository.sessions

object RemoteSessionsRepositoryProvider {
    fun provide(): RemoteSessionsRepository = LocalRemoteSessionsRepository()
}