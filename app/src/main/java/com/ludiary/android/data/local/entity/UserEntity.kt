package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa al usuario almacenado en local de la app.
 *
 * @property id Clave primaria fija
 * @property uid Identificador único del usuario
 * @property displayName Nombre del usuario
 * @property language Idioma del usuario
 * @property theme Tema del usuario
 * @property isAnonymous Indica si el usuario es anónimo
 * @property createdAt Fecha de creación del usuario
 * @property updatedAt Fecha de actualización del usuario
 * @property isAdmin Indica si el usuario es administrador
 */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Int = 0,
    val uid: String = "local-guest",
    val email: String?,
    val displayName: String = "Invitado",
    val language: String = "es",
    val theme: String = "system",
    val isAnonymous: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val isAdmin: Boolean = false
)