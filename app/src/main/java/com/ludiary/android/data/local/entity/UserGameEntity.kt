package com.ludiary.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ludiary.android.data.model.SyncStatus

/**
 * Representa a un usuario almacenado en local de la app.
 *
 * @property id Clave primaria fija
 * @property userId Identificador único del usuario
 * @property gameId Identificador único del juego
 * @property isCustom Indica si el juego es personalizado
 * @property titleSnapshot Título del juego en la copia local
 * @property personalRating Rating del usuario
 * @property language Idioma del usuario
 * @property edition Edición del juego
 * @property condition Estado físico del juego
 * @property location Ubicación del juego
 * @property purchaseDate Fecha de compra del juego
 * @property purchasePriceAmount Precio de compra del juego
 * @property purchasePriceCurrency Moneda de compra del juego
 * @property notes Notas del usuario
 * @property createdAt Fecha de creación del usuario
 * @property updatedAt Fecha de actualización del usuario
 * @property baseGameVersionAtLastSync Versión de la base de datos de la copia local del juego
 * @property hasBaseUpdate Indica si la copia local del juego tiene un juego base actualizado
 * @property syncStatus Estado de sincronización del juego con la copia local
 */
@Entity(tableName = "user_games")
data class UserGameEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val gameId: String?, // Puede ser nulo si es un juego personalizado
    val isCustom: Boolean = false,

    val titleSnapshot: String,
    val personalRating: Float? = null,
    val language: String? = null,
    val edition: String? = null,
    val condition: String? = null,
    val location: String? = null,

    val purchaseDate: Long? = null,
    val purchasePriceAmount: Double? = null,
    val purchasePriceCurrency: String? = null,

    val notes: String? = null,

    val createdAt: Long? = null,
    val updatedAt: Long? = null,

    val baseGameVersionAtLastSync: Int? = null,
    val hasBaseUpdate: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.CLEAN

)
