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

    private const val WORK_NAME_USER_GAMES_PERIODIC = "ludiary_usergames_auto_sync"
    private const val WORK_NAME_SESSIONS_PERIODIC = "ludiary_sessions_auto_sync"
    private const val WORK_NAME_SESSIONS_ONE_TIME = "ludiary_sessions_one_time_sync"

    /**
     * Programa una sync automática de juegos del usuario.
     * @param context Contexto de la aplicación.
     */
    fun enableAutoSyncUserGames(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<UserGamesSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_USER_GAMES_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        enqueueOneTimeUserGamesSync(context)
    }

    /**
     * Programa una sync automática de partidas.
     * @param context Contexto de la aplicación.
     */
    fun enableAutoSyncSessions(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SessionsSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_SESSIONS_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        enqueueOneTimeSessionsSync(context)
    }

    /**
     * Desprograma una sync automática de juegos del usuario.
     * @param context Contexto de la aplicación.
     */
    fun disableAutoSyncUserGames(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_USER_GAMES_PERIODIC)
    }

    /**
     * Desprograma una sync automática de partidas.
     * @param context Contexto de la aplicación.
     */
    fun disableAutoSyncSessions(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(WORK_NAME_SESSIONS_PERIODIC)
    }

    /**
     * Lanza una sync puntual de juegos del usuario (one-shot).
     * @param context Contexto de la aplicación.
     */
    fun enqueueOneTimeUserGamesSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<UserGamesSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Lanza una sync puntual de partidas (one-shot).
     * @param context Contexto de la aplicación.
     */
    fun enqueueOneTimeSessionsSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SessionsSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME_SESSIONS_ONE_TIME, ExistingWorkPolicy.KEEP, request)
    }
}