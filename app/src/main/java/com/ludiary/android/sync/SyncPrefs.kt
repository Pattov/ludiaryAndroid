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
    private fun keyLastPullPersonalSessions(uid: String) = "lastPull_personal_sessions_$uid"
    private fun keyLastPullGroupSessions(uid: String, groupId: String) = "lastPull_group_sessions_${uid}_$groupId"

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
        prefs.edit { putLong("last_pull_usergames_$uid", value) }
    }

    fun getLastPullPersonalSessions(uid: String): Long =
        prefs.getLong(keyLastPullPersonalSessions(uid), 0L)

    fun setLastPullPersonalSessions(uid: String, value: Long) {
        prefs.edit { putLong(keyLastPullPersonalSessions(uid), value) }
    }

    fun getLastPullGroupSessions(uid: String, groupId: String): Long =
        prefs.getLong(keyLastPullGroupSessions(uid, groupId), 0L)

    fun setLastPullGroupSessions(uid: String, groupId: String, value: Long) {
        prefs.edit { putLong(keyLastPullGroupSessions(uid, groupId), value) }
    }
}