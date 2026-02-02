package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus

/**
 * Representa una relación de amistad almacenada en local.
 * @property id Identificador local autogenerado (Room).
 * @property friendCode Código de amistad del usuario remoto (puede ser null si aún no se ha resuelto).
 * @property friendUid UID del usuario amigo en Firebase (null mientras no se sincroniza).
 * @property displayName Nombre visible del amigo (snapshot remoto).
 * @property nickname Alias personalizado asignado por el usuario.
 * @property status Estado actual de la relación de amistad.
 * @property createdAt Timestamp (epoch millis) de creación del registro.
 * @property updatedAt Timestamp (epoch millis) de última actualización.
 * @property syncStatus Estado de sincronización con Firestore.
 */
@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["friendUid"], unique = true),
        Index(value = ["friendCode"]),
        Index(value = ["status"])
    ]
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val friendCode: String? = null,
    val friendUid: String? = null,

    val displayName: String? = null,
    val nickname: String? = null,

    val status: FriendStatus = FriendStatus.PENDING_INCOMING,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val syncStatus: SyncStatus = SyncStatus.CLEAN
)