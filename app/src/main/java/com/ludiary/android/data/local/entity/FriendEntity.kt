package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.FriendStatus

/**
 * Representa a un amigo almacenado en local de la app.
 *
 * @property id Clave primaria autogenerada
 * @property email Email del amigo
 * @property displayName Nombre del amigo
 * @property nickname Mote del amigo
 * @property status Estado del amigo
 * @property createdAt Fecha de creación del amigo
 * @property updatedAt Fecha de actualización del amigo
 */
@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["status"])
    ]
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val email: String,

    val displayName: String? = null,
    val nickname: String? = null,

    val status: FriendStatus = FriendStatus.ACCEPTED,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun primaryLabel(): String = nickname?.takeIf { it.isNotBlank() }
        ?: displayName?.takeIf { it.isNotBlank() }
        ?: email
}