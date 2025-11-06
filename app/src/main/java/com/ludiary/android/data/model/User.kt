package com.ludiary.android.data.model

/**
 * Modelo de datos que representa a un usuario de Ludiary.
 *
 * @property uid Identificador único del usuario.
 * @property email Dirección de correo electrónico del usuario.
 * @property displayName Nombre de usuario o apodo.
 * @property isAnonymous Indica si el usuario es anónimo.
 * @property createdAt Fecha y hora en la que se registró el usuario.
 */
data class User(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = false,
    val createdAt: Long? = null
)
