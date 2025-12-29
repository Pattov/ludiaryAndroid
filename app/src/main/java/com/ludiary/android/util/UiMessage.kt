package com.ludiary.android.util


import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Muestra un mensaje de error sencillo.
 * @param resId Recurso de string que se mostrará como mensaje de error.
 */
fun Context.showError(@StringRes resId: Int) {
    Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show()
}