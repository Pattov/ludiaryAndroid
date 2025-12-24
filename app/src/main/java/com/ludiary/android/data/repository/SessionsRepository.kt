package com.ludiary.android.data.repository

data class SessionsSyncResult(
    val adopted: Int,
    val pushed: Int,
    val pulledPersonal: Int,
    val pulledGroups: Int
)

interface SessionsRepository {
    suspend fun sync(uid: String): SessionsSyncResult
}