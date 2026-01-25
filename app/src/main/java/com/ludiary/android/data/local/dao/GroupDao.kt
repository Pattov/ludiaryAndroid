package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    // ------------------- GROUPS -------------------

    @Query("SELECT * FROM user_groups WHERE (nameSnapshot LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun observeGroups(q: String): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(item: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(items: List<GroupEntity>)

    @Query("DELETE FROM user_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    // ------------------- MEMBERS -------------------

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(item: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(items: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND uid = :uid")
    suspend fun deleteMember(groupId: String, uid: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersByGroup(groupId: String)

    // ------------------- INVITES -------------------

    @Query("SELECT * FROM group_invites WHERE toUid = :myUid AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observePendingInvites(myUid: String): Flow<List<GroupInviteEntity>>

    @Query("SELECT * FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observeOutgoingPendingInvites(myUid: String): Flow<List<GroupInviteEntity>>

    @Query("SELECT * FROM group_invites WHERE groupId = :groupId AND fromUid = :myUid AND status = 'PENDING'")
    suspend fun pendingOutgoingInvitesForGroup(groupId: String, myUid: String): List<GroupInviteEntity>

    @Query("SELECT * FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING'")
    suspend fun pendingOutgoingInvitesAll(myUid: String): List<GroupInviteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvite(item: GroupInviteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvites(items: List<GroupInviteEntity>)

    @Query("DELETE FROM group_invites WHERE inviteId = :inviteId")
    suspend fun deleteInvite(inviteId: String)

    @Query("DELETE FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING' AND inviteId NOT IN (:remoteIds)")
    suspend fun deleteMissingOutgoingInvites(myUid: String, remoteIds: List<String>)

    @Query("DELETE FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING'")
    suspend fun deleteAllOutgoingPendingInvites(myUid: String)

    @Query("DELETE FROM group_invites WHERE toUid = :myUid AND status = 'PENDING' AND inviteId NOT IN (:remoteIds)")
    suspend fun deleteMissingIncomingInvites(myUid: String, remoteIds: List<String>)

    @Query("DELETE FROM group_invites WHERE toUid = :myUid AND status = 'PENDING'")
    suspend fun deleteAllIncomingPendingInvites(myUid: String)

    // -------------------------
    // Helper (opcional) para “salir” local
    // -------------------------
    @Transaction
    suspend fun leaveGroupLocal(groupId: String, myUid: String) {
        deleteMember(groupId, myUid)
        deleteGroup(groupId)
    }
}