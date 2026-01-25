package com.ludiary.android.data.local

import com.ludiary.android.data.local.dao.GroupDao
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class LocalGroupsDataSource(
    private val groupDao: GroupDao
) {
    fun observeGroups(query: String): Flow<List<GroupEntity>> =
        groupDao.observeGroups(query)

    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>> =
        groupDao.observeMembers(groupId)

    fun observePendingInvites(myUid: String?): Flow<List<GroupInviteEntity>> =
        if (myUid.isNullOrBlank()) flowOf(emptyList()) else groupDao.observePendingInvites(myUid)

    fun observeOutgoingPendingInvites(myUid: String?): Flow<List<GroupInviteEntity>> =
        if (myUid.isNullOrBlank()) flowOf(emptyList()) else groupDao.observeOutgoingPendingInvites(myUid)

    suspend fun pendingOutgoingInvitesForGroup(groupId: String, myUid: String?): List<GroupInviteEntity> =
        if (myUid.isNullOrBlank()) emptyList() else groupDao.pendingOutgoingInvitesForGroup(groupId, myUid)

    suspend fun pendingOutgoingInvitesAll(myUid: String?): List<GroupInviteEntity> =
        if (myUid.isNullOrBlank()) emptyList() else groupDao.pendingOutgoingInvitesAll(myUid)

    suspend fun upsertGroups(items: List<GroupEntity>) = groupDao.upsertGroups(items)
    suspend fun upsertGroup(item: GroupEntity) = groupDao.upsertGroup(item)

    suspend fun upsertMember(item: GroupMemberEntity) = groupDao.upsertMember(item)

    suspend fun upsertInvite(item: GroupInviteEntity) = groupDao.upsertInvite(item)
    suspend fun upsertInvites(items: List<GroupInviteEntity>) = groupDao.upsertInvites(items)

    suspend fun deleteInvite(inviteId: String) = groupDao.deleteInvite(inviteId)

    suspend fun deleteAllIncomingPendingInvites(myUid: String) =
        groupDao.deleteAllIncomingPendingInvites(myUid)

    suspend fun deleteMissingIncomingInvites(myUid: String, remoteIds: List<String>) =
        groupDao.deleteMissingIncomingInvites(myUid, remoteIds)

    suspend fun deleteAllOutgoingPendingInvites(myUid: String) =
        groupDao.deleteAllOutgoingPendingInvites(myUid)

    suspend fun deleteMissingOutgoingInvites(myUid: String, remoteIds: List<String>) =
        groupDao.deleteMissingOutgoingInvites(myUid, remoteIds)

    suspend fun leaveGroupLocal(groupId: String, myUid: String) =
        groupDao.leaveGroupLocal(groupId, myUid)
}