package com.ludiary.android.util

import com.google.firebase.functions.FirebaseFunctions

object FirebaseProviders {

    private const val FUNCTIONS_REGION = "europe-west1"

    val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
    }
}