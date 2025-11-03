package com.ludiary.android.data.model

data class User(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val isAnonymous: Boolean = false,
    val createdAt: Long? = null
)
