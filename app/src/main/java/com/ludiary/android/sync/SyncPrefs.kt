package com.ludiary.android.sync

import android.content.Context
import androidx.core.content.edit


class SyncPrefs (context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    fun getLastUserGamesPull(uid: String): Long =
        prefs.getLong("last_pull_usergames_$uid", 0L)

    fun setLastUserGamesPull(uid: String, value: Long) {
        prefs.edit {
            putLong("last_pull_usergames_$uid", value)
        }
    }
}