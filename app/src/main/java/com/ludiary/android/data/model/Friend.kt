package com.ludiary.android.data.model

/**
 * Representa la información de una amistad entre dos usuarios.
 * @property friendUid Identificador del amigo.
 * @property status Estado de la amistad.
 * @property nickname Nickname del amigo.
 * @property isFavorite Indica si el amigo es favorito.
 * @property friendEmail Email del amigo.
 * @property syncStatus Estado de sincronización entre copia y Firestore.
 * @property isDeleted Indica si la amistad ha sido eliminada.
 * @property createdAt Fecha de creación de la amistad.
 * @property updatedAt Fecha de actualización de la amistad.
 * @property deletedAt Fecha de eliminación de la amistad.
 */
data class Friend(
    val friendUid: String,
    val status: FriendStatus,

    val nickname: String?,
    val isFavorite: Boolean,

    val friendEmail: String,

    val syncStatus: SyncStatus = SyncStatus.CLEAN,
    val isDeleted: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)