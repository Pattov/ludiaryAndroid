package com.ludiary.android.data.model

/**
 * Modelo de notificación interna de la aplicación.
 * @property id Identificador único de la notificación.
 * @property type Tipo lógico de notificación.
 * @property title Título breve.
 * @property body Texto principal.
 * @property createdAt Fecha de creación.
 * @property read Indica si el usuario la ha marcado como leída.
 * @property data Datos extra asociados (metadata para deep links/acciones).
 */
data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val read: Boolean,
    val data: Map<String, Any?> = emptyMap()
)

/**
 * Estadísticas agregadas del centro de notificaciones del usuario.
 * @property unreadCount Número total de notificaciones no leídas.
 * @property updatedAt Marca de tiempo del último update (epoch millis).
 */
data class NotificationStats(
    val unreadCount: Int = 0,
    val updatedAt: Long = 0L
)