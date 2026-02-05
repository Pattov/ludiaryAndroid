package com.ludiary.android.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.repository.library.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.library.UserGamesRepositoryImpl

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
        val local = LocalUserGamesDataSource(db.userGameDao())
        val remote = FirestoreUserGamesRepository(FirebaseFirestore.getInstance())
        val repo = UserGamesRepositoryImpl(local, remote)

        val syncPrefs = SyncPrefs(applicationContext)
        val statusPrefs = SyncStatusPrefs(applicationContext)

        return try {
            repo.syncPending(uid)

            val lastPull = syncPrefs.getLastUserGamesPull(uid)
            val (_, maxTs) = repo.syncDownIncremental(uid, lastPull)
            if (maxTs != null) syncPrefs.setLastUserGamesPull(uid, maxTs)

            statusPrefs.setLastSyncMillis(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            Log.e("LUDIARY_SYNC_USER_GAMES", "Error syncing user games", e)
            Result.retry()
        }
    }
}