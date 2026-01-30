package com.ludiary.android.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Programador de tareas de sincronización automática de juegos del usuario.
 * @param context Contexto de la aplicación.
 * @constructor Crea una nueva instancia de [SyncScheduler].
 */
object SyncScheduler {

    private const val WORK_USER_GAMES_PERIODIC = "ludiary_usergames_auto_sync"
    private const val WORK_SESSIONS_PERIODIC = "ludiary_sessions_auto_sync"
    private const val WORK_USER_GAMES_ONE_TIME = "ludiary_usergames_one_time_sync"
    private const val WORK_SESSIONS_ONE_TIME = "ludiary_sessions_one_time_sync"
    private const val WORK_FRIENDS_GROUPS_PERIODIC = "ludiary_friends_groups_auto_sync"
    private const val WORK_FRIENDS_GROUPS_ONE_TIME = "ludiary_friends_groups_one_time_sync"


    /**
     * Programa una sync automática de juegos del usuario.
     * @param context Contexto de la aplicación.
     */
    fun enableAutoSyncUserGames(context: Context) {
        val constraints = connectedConstraints()

        val request = PeriodicWorkRequestBuilder<UserGamesSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_USER_GAMES_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // Sincronización inicial sin esperar 6h
        enqueueOneTimeUserGamesSync(context)
    }

    /**
     * Programa una sync automática de partidas.
     * @param context Contexto de la aplicación.
     */
    fun enableAutoSyncSessions(context: Context) {
        val constraints = connectedConstraints()

        val request = PeriodicWorkRequestBuilder<SessionsSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_SESSIONS_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // Sincronización inicial sin esperar 6h
        enqueueOneTimeSessionsSync(context)
    }

    fun enableAutoSyncFriendsGroups(context: Context) {
        val constraints = connectedConstraints()

        val request = PeriodicWorkRequestBuilder<SocialSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_FRIENDS_GROUPS_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        enqueueOneTimeFriendsGroupsSync(context)
    }

    /**
     * Desprograma una sync automática de juegos del usuario.
     * @param context Contexto de la aplicación.
     */
    fun disableAutoSyncUserGames(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_USER_GAMES_PERIODIC)
    }

    /**
     * Desprograma una sync automática de partidas.
     * @param context Contexto de la aplicación.
     */
    fun disableAutoSyncSessions(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_SESSIONS_PERIODIC)
    }

    fun disableAutoSyncFriendsGroups(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_FRIENDS_GROUPS_PERIODIC)
    }

    /**
     * Lanza una sync puntual de juegos del usuario (one-shot).
     * @param context Contexto de la aplicación.
     */
    fun enqueueOneTimeUserGamesSync(context: Context) {
        val constraints = connectedConstraints()

        val request = OneTimeWorkRequestBuilder<UserGamesSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_USER_GAMES_ONE_TIME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Lanza una sync puntual de partidas (one-shot).
     * @param context Contexto de la aplicación.
     */
    fun enqueueOneTimeSessionsSync(context: Context) {
        val constraints = connectedConstraints()

        val request = OneTimeWorkRequestBuilder<SessionsSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_SESSIONS_ONE_TIME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueOneTimeFriendsGroupsSync(context: Context) {
        val constraints = connectedConstraints()

        val request = OneTimeWorkRequestBuilder<SocialSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_FRIENDS_GROUPS_ONE_TIME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun connectedConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}