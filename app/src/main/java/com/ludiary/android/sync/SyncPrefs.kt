package com.ludiary.android.sync

import android.content.Context
import androidx.core.content.edit

/**
 * Clase de preferencias de sincronización.
 * @param context Contexto de la aplicación.
 * @return Instancia de [SyncPrefs].
 * @throws Exception si ocurre un error al cargar las preferencias.
 */
class SyncPrefs (context: Context) {
    /**
     * Obtiene las preferencias de sincronización.
     * @param context Contexto de la aplicación.
     */
    private val prefs =
        context.applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    /**
     * Obtiene el último pull de juegos del usuario.
     * @param uid Identificador único del usuario.
     */
    fun getLastUserGamesPull(uid: String): Long =
        prefs.getLong("last_pull_usergames_$uid", 0L)

    /**
     * Guarda el último pull de juegos del usuario.
     * @param uid Identificador único del usuario.
     * @param value Valor a guardar.
     */
    fun setLastUserGamesPull(uid: String, value: Long) {
        prefs.edit {
            putLong("last_pull_usergames_$uid", value)
        }
    }
}