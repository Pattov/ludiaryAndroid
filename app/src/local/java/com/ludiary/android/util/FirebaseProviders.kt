package com.ludiary.android.util

object FirebaseProviders {
    val functions: Any
        get() = throw IllegalStateException("FirebaseFunctions no disponible en modo local")
}
