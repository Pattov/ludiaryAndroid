package com.ludiary.android.util


import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Muestra un mensaje de error sencillo.
 */
fun Context.showError(@StringRes resId: Int) {
    Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show()
}