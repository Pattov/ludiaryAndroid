package com.ludiary.android.sync

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepositoryImpl
import com.ludiary.android.ui.profile.SyncFragment

/**
 * Worker para sincronizar juegos del usuario con Firestore.
 * @param appContext Contexto de la aplicación.
 * @param params Parámetros del worker.
 * @constructor Crea una nueva instancia de [UserGamesSyncWorker].
 * @property appContext Contexto de la aplicación.
 * @property params Parámetros del worker.
 */
class UserGamesSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /**
     * Punto de entrada del Worker
     * @return [Result] que indica el estado del trabajo.
     * @throws Exception si ocurre un error durante la ejecución del trabajo.
     */
    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val db = LudiaryDatabase.getInstance(applicationContext)
        val localDS = LocalUserGamesDataSource(db.userGameDao())
        val remote = FirestoreUserGamesRepository(FirebaseFirestore.getInstance())
        val repo = UserGamesRepositoryImpl(localDS, remote)

        val syncPrefs = SyncPrefs(applicationContext)

        return try {
            repo.syncPending(uid)

            val lastPull = syncPrefs.getLastUserGamesPull(uid)
            repo.syncDownIncremental(uid, lastPull)

            val now = System.currentTimeMillis()
            syncPrefs.setLastUserGamesPull(uid, now)

            val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            prefs.edit { putLong(SyncFragment.KEY_LAST_LIBRARY_SYNC, now) }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}