package com.ludiary.android.data.model

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

data class PurchasePrice(
    val amount: Double = 0.0,
    val currency: String = "EUR"
)
