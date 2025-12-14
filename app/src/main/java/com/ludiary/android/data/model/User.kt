package com.ludiary.android.data.model

/**
 * Modelo de datos que representa las preferencias de usuario de Ludiary.
 *
 * @property language Idioma del usuario.
 * @property theme Tema del usuario (claro, oscuro o sistema).
 */
data class UserPreferences(
    val language: String?,
    val theme: String?
)

/**
 * Modelo de datos que representa a un usuario de Ludiary.
 *
 * @property uid Identificador único del usuario.
 * @property email Dirección de correo electrónico del usuario.
 * @property displayName Nombre de usuario o apodo.
 * @property isAnonymous Indica si el usuario es anónimo.
 * @property createdAt Fecha y hora en la que se registró el usuario.
 * @property updatedAt Fecha y hora en la que se actualizó el usuario.
 * @property preferences Preferencias del usuario.
 * @property isAdmin Indica si el usuario es administrador.
 */
data class User(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val preferences: UserPreferences? = null,
    val isAdmin: Boolean = false
)
