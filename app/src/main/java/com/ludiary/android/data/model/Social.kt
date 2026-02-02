package com.ludiary.android.data.model

/**
 * Resultado de creación de un grupo.
 * @property groupId ID del grupo creado.
 * @property name Nombre del grupo.
 * @property now Timestamp (epoch millis) usado durante la operación.
 */
data class CreatedGroup(
    val groupId: String,
    val name: String,
    val now: Long
)

/**
 * Snapshot de una invitación a grupo tras ejecutar una acción (aceptar/cancelar) en Firestore.
 * @property inviteId Identificador único de la invitación. Coincide con el ID del documento en Firestore.
 * @property groupId Identificador del grupo al que se refiere la invitación.
 * @property groupNameSnapshot Nombre del grupo en el momento de crear la invitación.
 * @property fromUid UID del usuario que envía la invitación.
 * @property toUid UID del usuario destinatario de la invitación.
 * @property status Estado de la invitación tras la operación.
 * @property createdAt Marca de tiempo (epoch millis) en la que se creó la invitación.
 * @property respondedAt Marca de tiempo (epoch millis) en la que se respondió a la invitación.
 */
data class InviteSnapshot(
    val inviteId: String,
    val groupId: String,
    val groupNameSnapshot: String,
    val fromUid: String,
    val toUid: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long?
)

/**
 * Representa una invitación a un grupo almacenada en Firestore.
 * @property inviteId Identificador único de la invitación.
 * @property groupId Identificador del grupo al que se refiere la invitación.
 * @property groupNameSnapshot Nombre del grupo en el momento de crear la invitación.
 * @property fromUid UID del usuario que envía la invitación.
 * @property toUid UID del usuario destinatario de la invitación.
 * @property status Estado actual de la invitación.
 * @property createdAt Fecha de creación de la amistad.
 * @property respondedAt Marca de tiempo (epoch millis) en la que se respondió la invitación (aceptada o cancelada).
 */
data class RemoteInvite(
    val inviteId: String,
    val groupId: String,
    val groupNameSnapshot: String,
    val fromUid: String,
    val toUid: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long?
)

/**
 * Representa la relación de un usuario con un grupo en Firestore.
 * @property groupId Identificador único del grupo.
 * @property nameSnapshot Nombre del grupo en el momento en que el usuario se unió.
 * @property joinedAt Marca de tiempo (epoch millis) en la que el usuario se unió al grupo.
 * @property updatedAt Marca de tiempo (epoch millis) de la última actualización relevante del grupo para el usuario
 */
data class RemoteUserGroupIndex(
    val groupId: String,
    val nameSnapshot: String,
    val joinedAt: Long,
    val updatedAt: Long
)

/**
 * Resultado devuelto al invitar a un usuario a un grupo.
 *
 * @property inviteId ID de la invitación (documento/registro).
 * @property groupId ID del grupo.
 * @property groupNameSnapshot Nombre del grupo en el momento de la invitación (snapshot).
 * @property fromUid UID del usuario que envía la invitación.
 * @property toUid UID del usuario invitado.
 * @property status Estado de la invitación (ej: "pending", "accepted", "rejected", etc.).
 * @property createdAt Timestamp (ms) de creación.
 * @property respondedAt Timestamp (ms) de respuesta si ya fue aceptada/rechazada.
 */
data class GroupInviteResult(
    val inviteId: String,
    val groupId: String,
    val groupNameSnapshot: String,
    val fromUid: String,
    val toUid: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long?
)

/**
 * Resultado devuelto al crear un grupo.
 * @property groupId ID del grupo creado.
 * @property name Nombre final.
 * @property now Timestamp de servidor (ms) para coherencia temporal.
 * @property membersCount Miembros iniciales del grupo.
 */
data class CreateGroupResult(
    val groupId: String,
    val name: String,
    val now: Long,
    val membersCount: Int
)