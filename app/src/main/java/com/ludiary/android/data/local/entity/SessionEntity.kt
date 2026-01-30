package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.GameRefType
import com.ludiary.android.data.model.SessionScope
import com.ludiary.android.data.model.SyncStatus

/**
 * Representa una partida registrada por el usuario en Ludiary.
 * Cada sesión se asocia a un juego concreto de la ludoteca.
 *
 * @property id Identificador único de la sesión.
 * @property scope Alcance de la sesión.
 * @property ownerUserId Identificador único del usuario propietario de la sesión.
 * @property groupId Identificador único del grupo al que pertenece la sesión.
 * @property gameRefType Tipo de referencia al juego de la sesión.
 * @property gameRefId Identificador único del juego de la sesión.
 * @property gameTitle Título del juego de la sesión.
 * @property playedAt Fecha y hora en la que se jugó la sesión.
 * @property location Ubicación en la que se jugó la sesión.
 * @property durationMinutes Duración de la sesión en minutos.
 * @property overallRating Calificación general de la sesión.
 * @property notes Notas adicionales de la sesión.
 * @property syncStatus Estado de sincronización entre copia y Firestore.
 * @property isDeleted Indica si la sesión ha sido eliminada.
 * @property createdAt Fecha de creación de la sesión.
 * @property updatedAt Fecha de actualización de la sesión.
 * @property deletedAt Fecha de eliminación de la sesión.
 */
@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["ownerUserId", "playedAt"]),
        Index(value = ["groupId", "playedAt"]),
        Index(value = ["syncStatus"])
    ]
)
data class SessionEntity(
    @PrimaryKey val id: String,

    val scope: SessionScope,
    val ownerUserId: String?,
    val groupId: String?,

    val gameRefType: GameRefType,
    val gameRefId: String,
    val gameTitle: String,

    val playedAt: Long,
    val location: String?,
    val durationMinutes: Int?,

    val overallRating: Int?,
    val notes: String?,

    val syncStatus: SyncStatus,
    val isDeleted: Boolean,

    val createdAt: Long?,
    val updatedAt: Long?,
    val deletedAt: Long?
)