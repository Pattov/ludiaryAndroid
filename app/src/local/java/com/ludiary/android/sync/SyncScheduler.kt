package com.ludiary.android.sync

import android.content.Context
import androidx.work.WorkManager

object SyncScheduler {

    fun enableAutoSyncUserGames(context: Context) {
        // no-op en modo local
        disableAutoSyncUserGames(context)
    }

    fun enableAutoSyncSessions(context: Context) {
        // no-op en modo local
        disableAutoSyncSessions(context)
    }

    fun enableAutoSyncFriendsGroups(context: Context) {
        // no-op en modo local
        disableAutoSyncFriendsGroups(context)
    }

    fun disableAutoSyncUserGames(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("ludiary_usergames_auto_sync")
        WorkManager.getInstance(context).cancelUniqueWork("ludiary_usergames_one_time_sync")
    }

    fun disableAutoSyncSessions(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("ludiary_sessions_auto_sync")
        WorkManager.getInstance(context).cancelUniqueWork("ludiary_sessions_one_time_sync")
    }

    fun disableAutoSyncFriendsGroups(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("ludiary_friends_groups_auto_sync")
        WorkManager.getInstance(context).cancelUniqueWork("ludiary_friends_groups_one_time_sync")
    }

    fun enqueueOneTimeUserGamesSync(context: Context) { /* no-op */ }
    fun enqueueOneTimeSessionsSync(context: Context) { /* no-op */ }
    fun enqueueOneTimeFriendsGroupsSync(context: Context) { /* no-op */ }
}