package com.ludiary.android.data.repository

import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {

    /** Amigos aceptados */
    fun observeFriends(query: String = ""): Flow<List<FriendEntity>>

    /** Solicitudes recibidas */
    fun observeIncomingRequests(query: String = ""): Flow<List<FriendEntity>>

    fun observeGroups(query: String = ""): Flow<List<FriendEntity>>

    /** Solicitudes enviadas */
    fun observeOutgoingRequests(query: String = ""): Flow<List<FriendEntity>>

    suspend fun getById(id: Long): FriendEntity?

    suspend fun updateNickname(id: Long, nickname: String?)

    suspend fun updateStatus(id: Long, status: FriendStatus)

    suspend fun upsertAll(items: List<FriendEntity>)
}
