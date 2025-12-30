package com.ludiary.android.sync

import android.content.Context
import androidx.core.content.edit

/**
 * Gestión centralizada de preferencias relacionadas con la sincronización.
 * @param context Contexto de la aplicación. Se usa siempre el applicationContext.
 */
class SyncStatusPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Devuelve si el usuario tiene activada la sincronización automática.
     * @return Si la sincronización automática está habilitada.
     */
    fun isAutoSyncEnabled(): Boolean =
        prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)

    /**
     * Guarda si el usuario tiene activada la sincronización automática.
     * @param enabled Si la sincronización automática está habilitada.
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SYNC_ENABLED, enabled) }
    }

    /**
     * Devuelve el timestamp del último intento de sincronización exitosa.
     * @return Timestamp en milisegundos o 0L si nunca se ha sincronizado.
     */
    fun getLastSyncMillis(): Long =
        prefs.getLong(KEY_LAST_SYNC_MILLIS, 0L)

    /**
     * Guarda el timestamp del último intento de sincronización exitosa.
     * @param value Timestamp en milisegundos.
     */
    fun setLastSyncMillis(value: Long) {
        prefs.edit { putLong(KEY_LAST_SYNC_MILLIS, value) }
    }

    companion object {
        private const val PREFS_NAME = "sync_status_prefs"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_LAST_SYNC_MILLIS = "last_sync_millis"
    }
}