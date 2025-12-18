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

    /**
     * Nombre del trabajo de sincronización automática.
     */
    private const val WORK_NAME = "ludiary_usergames_auto_sync"

    /**
     * Programa una tarea de sincronización automática de juegos del usuario.
     * @param context Contexto de la aplicación.
     * @param constraints Restricciones para la ejecución del trabajo.
     * @return [WorkRequest] para la ejecución programada.
     */
    fun enableAutoSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<UserGamesSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Desprograma una tarea de sincronización automática de juegos del usuario.
     * @param context Contexto de la aplicación.
     * @return [WorkRequest] para la ejecución programada.
     */
    fun disableAutoSync(context: Context){
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Verifica si una tarea de sincronización automática de juegos del usuario está programada.
     * @param context Contexto de la aplicación.
     * @return [Boolean] indicando si la tarea está programada.
     */
    fun runOneTimeSyncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<UserGamesSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}