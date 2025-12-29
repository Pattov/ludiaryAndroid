package com.ludiary.android.sync

import android.content.Context
import androidx.core.content.edit

/**
 * Gestión centralizada de preferencias relacionadas con la sincronización.
 * @param context Contexto de la aplicación. Se usa siempre el applicationContext.
 */
class SyncPrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Genera la clave para almacenar el último pull de sesiones personales de un usuario concreto.
     * @param uid Identificador único del usuario.
     */
    private fun keyLastPullPersonalSessions(uid: String): String =
        "lastPull_personal_sessions_$uid"

    /**
     * Genera la clave para almacenar el último pull de sesiones de grupo para un usuario y grupo concretos.
     * @param uid Identificador único del usuario.
     * @param groupId Identificador único del grupo.
     */
    private fun keyLastPullGroupSessions(uid: String, groupId: String): String =
        "lastPull_group_sessions_${uid}_$groupId"

    /**
     * Devuelve el timestamp del último pull de juegos del usuario.
     * @param uid Identificador único del usuario.
     * @return Timestamp en milisegundos o 0L si nunca se ha sincronizado.
     */
    fun getLastUserGamesPull(uid: String): Long =
        prefs.getLong("last_pull_usergames_$uid", 0L)

    /**
     * Guarda el timestamp del último pull de juegos del usuario.
     * @param uid Identificador único del usuario.
     * @param value Timestamp en milisegundos.
     */
    fun setLastUserGamesPull(uid: String, value: Long) {
        prefs.edit { putLong("last_pull_usergames_$uid", value) }
    }


    /**
     * Devuelve el timestamp del último pull de sesiones personales del usuario.
     * @param uid Identificador único del usuario.
     * @return Timestamp en milisegundos o 0L si nunca se ha sincronizado.
     */
    fun getLastPullPersonalSessions(uid: String): Long =
        prefs.getLong(keyLastPullPersonalSessions(uid), 0L)

    /**
     * Guarda el timestamp del último pull de sesiones personales del usuario.
     * @param uid Identificador único del usuario.
     * @param value Timestamp en milisegundos.
     */
    fun setLastPullPersonalSessions(uid: String, value: Long) {
        prefs.edit { putLong(keyLastPullPersonalSessions(uid), value) }
    }

    /**
     * Devuelve el timestamp del último pull de sesiones de un grupo concreto.
     * @param uid Identificador único del usuario.
     * @param groupId Identificador único del grupo.
     * @return Timestamp en milisegundos o 0L si nunca se ha sincronizado.
     */
    fun getLastPullGroupSessions(uid: String, groupId: String): Long =
        prefs.getLong(keyLastPullGroupSessions(uid, groupId), 0L)

    /**
     * Guarda el timestamp del último pull de sesiones de un grupo concreto.
     * @param uid Identificador único del usuario.
     * @param groupId Identificador único del grupo.
     * @param value Timestamp en milisegundos.
     */
    fun setLastPullGroupSessions(uid: String, groupId: String, value: Long) {
        prefs.edit { putLong(keyLastPullGroupSessions(uid, groupId), value) }
    }

    /**
     * Indica si el usuario tiene activada la sincronización automática.
     * @return true si está activada, false si no.
     */
    fun isAutoSyncEnabled(): Boolean =
        prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)

    /**
     * Guarda si el usuario quiere la sincronización automática activada.
     * @param enabled true para activar auto-sync, false para desactivar.
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SYNC_ENABLED, enabled) }
    }

    /**
     * Última sincronización del catálogo (valor informativo para UI).
     * @return Timestamp en milisegundos o 0L si nunca.
     */
    fun getLastCatalogSyncMillis(): Long =
        prefs.getLong(KEY_LAST_CATALOG_SYNC_MILLIS, 0L)

    /**
     * Guarda la última sincronización del catálogo (valor informativo para UI).
     * @param value Timestamp en milisegundos.
     */
    fun setLastCatalogSyncMillis(value: Long) {
        prefs.edit { putLong(KEY_LAST_CATALOG_SYNC_MILLIS, value) }
    }

    /**
     * Última sincronización de la ludoteca del usuario (valor informativo para UI).
     * @return Timestamp en milisegundos o 0L si nunca.
     */
    fun getLastLibrarySyncMillis(): Long =
        prefs.getLong(KEY_LAST_LIBRARY_SYNC_MILLIS, 0L)

    /**
     * Guarda la última sincronización de la ludoteca del usuario (valor informativo para UI).
     * @param value Timestamp en milisegundos.
     */
    fun setLastLibrarySyncMillis(value: Long) {
        prefs.edit { putLong(KEY_LAST_LIBRARY_SYNC_MILLIS, value) }
    }

    private companion object {
        private const val PREFS_NAME = "sync_prefs"

        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_LAST_CATALOG_SYNC_MILLIS = "last_catalog_sync"
        private const val KEY_LAST_LIBRARY_SYNC_MILLIS = "last_library_sync"
    }
}