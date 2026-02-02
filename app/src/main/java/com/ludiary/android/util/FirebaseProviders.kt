package com.ludiary.android.util

import com.google.firebase.functions.FirebaseFunctions

/**
 * Proveedor centralizado de instancias Firebase utilizadas en la aplicación.
 */
object FirebaseProviders {

    private const val FUNCTIONS_REGION = "europe-west1"

    /**
    * Instancia singleton de [FirebaseFunctions] configurada con la región correcta.
    */
    val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
    }
}