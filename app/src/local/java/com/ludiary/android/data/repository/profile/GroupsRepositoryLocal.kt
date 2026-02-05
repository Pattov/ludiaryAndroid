package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class GroupsRepositoryLocal(
    private val local: LocalGroupsDataSource
) : GroupsRepository {

    override fun observeGroups(query: String): Flow<List<GroupEntity>> =
        local.observeGroups(query)

    override fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>> =
        local.observeMembers(groupId)

    override fun observeIncomingPendingInvites(): Flow<List<GroupInviteEntity>> =
        emptyFlow()

    override fun observeOutgoingPendingInvites(): Flow<List<GroupInviteEntity>> =
        emptyFlow()

    override fun startRemoteSync() { /* no-op */ }
    override fun stopRemoteSync() { /* no-op */ }

    override suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity> =
        emptyList()

    override suspend fun createGroup(name: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible"))

    override suspend fun inviteToGroup(groupId: String, groupNameSnapshot: String, toUid: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible"))

    override suspend fun acceptInvite(inviteId: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible"))

    override suspend fun cancelInvite(inviteId: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible"))

    override suspend fun rejectInvite(inviteId: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible"))

    override suspend fun leaveGroup(groupId: String): Result<Unit> =
        Result.failure(IllegalStateException("Modo local: no disponible"))

    override suspend fun flushPendingInvites(): Result<Unit> =
        Result.success(Unit)
}