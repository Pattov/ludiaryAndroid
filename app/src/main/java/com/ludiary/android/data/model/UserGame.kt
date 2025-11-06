package com.ludiary.android.data.model

/**
 * Representa una copia personal de un juego de la ludoteca del usuario.
 *
 * Este modelo se almacena en la subcolección `/users/{uid}/userGames`.
 *
 * @property id Identificador único del juego en la base de datos.
 * @property gameId Identificador único del juego en la ludoteca.
 * @property titleSnapshot Título del juego en su versión actual.
 * @property personalRating Calificación personal del juego.
 * @property language Idioma del juego.
 * @property edition Edición del juego.
 * @property condition Condición del juego.
 * @property purchaseDate Fecha de compra del juego.
 * @property purchasePrice Precio de compra del juego.
 * @property notes Notas del juego.
 * @property createdAt Fecha de creación del juego.
 * @property updatedAt Fecha de actualización del juego.
 */
data class UserGame(
    val id: String = "",
    val gameId: String = "",
    val titleSnapshot: String = "",
    val personalRating: Int? = null,
    val language: String? = null,
    val edition: String? = null,
    val condition: String? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: PurchasePrice? = null,
    val notes: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

/**
 * Representa el precio de compra de un juego.
 *
 * se incluye dentro de [UserGame] para registrar el importe y la moneda.
 *
 * @property amount Importe númerico del juego.
 * @property currency Código ISO de la moneda (Por defecto EUR).
 */
data class PurchasePrice(
    val amount: Double = 0.0,
    val currency: String = "EUR"
)
