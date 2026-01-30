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

/**
 * DAO para la gestión local de grupos, miembros e invitaciones.
 */
@Dao
interface GroupDao {

    // ------------------- GROUPS -------------------

    /**
     * Observa la lista de grupos del usuario, aplicando filtro por nombre.
     * @param q Texto de búsqueda. Puede ser vacío.
     * @return Flow con la lista de [GroupEntity] ordenada por updatedAt DESC.
     */
    @Query("SELECT * FROM user_groups WHERE (nameSnapshot LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun observeGroups(q: String): Flow<List<GroupEntity>>

    /**
     * Inserta o actualiza un grupo.
     * @param item Grupo a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(item: GroupEntity)

    /**
     * Inserta o actualiza múltiples grupos.
     * @param items Lista de grupos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(items: List<GroupEntity>)

    /**
     * Elimina un grupo por ID.
     * @param groupId Identificador del grupo.
     */
    @Query("DELETE FROM user_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    // ------------------- MEMBERS -------------------
    /**
     * Observa los miembros de un grupo.
     * @param groupId Identificador del grupo.
     * @return Flow con la lista de [GroupMemberEntity] ordenada por joinedAt ASC.
     */
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    /**
     * Inserta o actualiza un miembro de grupo.
     * @param item Miembro del grupo.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(item: GroupMemberEntity)

    /**
     * Inserta o actualiza múltiples miembros de grupo.
     * @param items Lista de miembros.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(items: List<GroupMemberEntity>)

    /**
     * Elimina un miembro concreto de un grupo.
     * @param groupId Identificador del grupo.
     * @param uid UID del miembro.
     */
    @Query("DELETE FROM group_members WHERE groupId = :groupId AND uid = :uid")
    suspend fun deleteMember(groupId: String, uid: String)

    /**
     * Elimina todos los miembros de un grupo.
     * @param groupId Identificador del grupo.
     */
    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersByGroup(groupId: String)

    // ------------------- INVITES -------------------
    /**
     * Observa invitaciones entrantes pendientes para el usuario.
     * @param myUid UID del usuario actual.
     * @return Flow con la lista de [GroupInviteEntity] entrantes.
     */
    @Query("SELECT * FROM group_invites WHERE toUid = :myUid AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observePendingInvites(myUid: String): Flow<List<GroupInviteEntity>>

    /**
     * Observa invitaciones salientes pendientes del usuario.
     * @param myUid UID del usuario actual.
     * @return Flow con la lista de [GroupInviteEntity] salientes.
     */
    @Query("SELECT * FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observeOutgoingPendingInvites(myUid: String): Flow<List<GroupInviteEntity>>

    /**
     * Devuelve invitaciones salientes pendientes para un grupo concreto.
     * @param groupId Identificador del grupo.
     * @param myUid UID del usuario actual.
     */
    @Query("SELECT * FROM group_invites WHERE groupId = :groupId AND fromUid = :myUid AND status = 'PENDING'")
    suspend fun pendingOutgoingInvitesForGroup(groupId: String, myUid: String): List<GroupInviteEntity>

    /**
     * Devuelve todas las invitaciones salientes pendientes del usuario.
     * @param myUid UID del usuario actual.
     */
    @Query("SELECT * FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING'")
    suspend fun pendingOutgoingInvitesAll(myUid: String): List<GroupInviteEntity>

    /**
     * Inserta o actualiza una invitación.
     * @param item Invitación.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvite(item: GroupInviteEntity)

    /**
     * Inserta o actualiza múltiples invitaciones.
     * @param items Lista de invitaciones.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvites(items: List<GroupInviteEntity>)

    /**
     * Elimina una invitación por ID.
     * @param inviteId Identificador de la invitación.
     */
    @Query("DELETE FROM group_invites WHERE inviteId = :inviteId")
    suspend fun deleteInvite(inviteId: String)

    /**
     * Elimina invitaciones salientes pendientes que ya no existen en remoto.
     *
     * @param myUid UID del usuario actual.
     * @param remoteIds IDs que siguen existiendo en Firestore.
     */
    @Query("DELETE FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING' AND inviteId NOT IN (:remoteIds)")
    suspend fun deleteMissingOutgoingInvites(myUid: String, remoteIds: List<String>)

    /**
     * Elimina todas las invitaciones salientes pendientes.
     * @param myUid UID del usuario actual.
     */
    @Query("DELETE FROM group_invites WHERE fromUid = :myUid AND status = 'PENDING'")
    suspend fun deleteAllOutgoingPendingInvites(myUid: String)

    /**
     * Elimina invitaciones entrantes pendientes que ya no existen en remoto.
     * @param myUid UID del usuario actual.
     * @param remoteIds IDs que siguen existiendo en Firestore.
     */
    @Query("DELETE FROM group_invites WHERE toUid = :myUid AND status = 'PENDING' AND inviteId NOT IN (:remoteIds)")
    suspend fun deleteMissingIncomingInvites(myUid: String, remoteIds: List<String>)

    /**
     * Elimina todas las invitaciones entrantes pendientes.
     * @param myUid UID del usuario actual.
     */
    @Query("DELETE FROM group_invites WHERE toUid = :myUid AND status = 'PENDING'")
    suspend fun deleteAllIncomingPendingInvites(myUid: String)

    // ------------------- HELPERS -------------------
    /**
     * Abandona un grupo en local:
     * - Elimina miembros
     * - Elimina el grupo
     *
     * Se ejecuta como transacción atómica.
     *
     * @param groupId Identificador del grupo.
     * @param myUid UID del usuario actual.
     */
    @Transaction
    suspend fun leaveGroupLocal(groupId: String, myUid: String) {
        deleteMember(groupId, myUid)
        deleteMembersByGroup(groupId)
        deleteGroup(groupId)
    }
}