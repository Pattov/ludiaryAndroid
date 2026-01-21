package com.ludiary.android.data.repository

import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

interface GroupsRepository {
    fun observeGroups(query: String): Flow<List<GroupEntity>>
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>
    fun observePendingInvites(): Flow<List<GroupInviteEntity>>

    fun startRemoteSync()
    fun stopRemoteSync()

    suspend fun createGroup(name: String): Result<Unit>
    suspend fun inviteToGroup(groupId: String, groupNameSnapshot: String, toUid: String): Result<Unit>
    suspend fun acceptInvite(inviteId: String): Result<Unit>
    suspend fun cancelInvite(inviteId: String): Result<Unit>
    suspend fun rejectInvite(inviteId: String): Result<Unit>
    suspend fun leaveGroup(groupId: String): Result<Unit>
}