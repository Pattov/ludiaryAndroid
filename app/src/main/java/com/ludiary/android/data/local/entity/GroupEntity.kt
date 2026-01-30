package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa un grupo al que pertenece el usuario.
 * Corresponde a una entrada del índice local de grupos (`users/{uid}/groups`)
 *
 * @property groupId Identificador único del grupo.
 * @property nameSnapshot Nombre del grupo en el momento de la sincronización.
 * @property createdAt Timestamp (epoch millis) de unión del usuario al grupo.
 * @property updatedAt Timestamp (epoch millis) de la última actualización del grupo.
 * @property membersCount Número total de miembros del grupo.
 */

@Entity(tableName = "user_groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val nameSnapshot: String,
    val createdAt: Long,
    val updatedAt: Long,
    val membersCount: Int = 1
)

/**
 * Representa a un miembro de un grupo.
 * @property groupId Identificador del grupo.
 * @property uid UID del usuario miembro.
 * @property joinedAt Timestamp (epoch millis) de incorporación al grupo.
 */
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "uid"]
)
data class GroupMemberEntity(
    val groupId: String,
    val uid: String,
    val joinedAt: Long
)

/**
 * Representa una invitación a un grupo almacenada en local.
 * @property inviteId Identificador único de la invitación.
 * @property groupId Identificador del grupo al que se invita.
 * @property groupNameSnapshot Nombre del grupo en el momento de la invitación.
 * @property fromUid UID del usuario que envía la invitación.
 * @property toUid UID del usuario receptor.
 * @property status Estado de la invitación (PENDING, ACCEPTED, CANCELLED).
 * @property createdAt Timestamp (epoch millis) de creación de la invitación.
 * @property respondedAt Timestamp (epoch millis) de respuesta, si existe.
 */
@Entity(tableName = "group_invites")
data class GroupInviteEntity(
    @PrimaryKey val inviteId: String,
    val groupId: String,
    val groupNameSnapshot: String,
    val fromUid: String,
    val toUid: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long? = null
)