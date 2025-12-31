package com.ludiary.android.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * Metodo para obtener cadenas de texto almacenadas en 'strings.xml'
 * @property context contexto necesario para acceder a los recursos del sistema.
 */
class ResourceProvider ( private val context: Context ) {
    /**
     * Devuelve el texto asociado al recurso indicado.
     *
     * @param resId identificador del recurso `string`.
     * @return cadena obtenida desde `strings.xml`.
     */
    fun getString(@StringRes resId: Int): String = context.getString(resId)
}