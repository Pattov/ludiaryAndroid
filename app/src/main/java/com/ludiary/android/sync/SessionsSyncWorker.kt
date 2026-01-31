package com.ludiary.android.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalSessionsDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.sessions.FirestoreSessionsRepository
import com.ludiary.android.data.repository.sessions.GroupIdProvider
import com.ludiary.android.data.repository.sessions.SessionsRepository
import com.ludiary.android.data.repository.sessions.SessionsRepositoryImpl

/**
 * Worker para sincronizar sesiones.
 * @property appContext Contexto de la aplicación.
 * @property params Parámetros del worker.
 */
class SessionsSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /**
     * Realiza la sincronización de sesiones.
     * @return Resultado de la operación.
     */
    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val db = LudiaryDatabase.getInstance(applicationContext)
        val local = LocalSessionsDataSource(db.sessionDao())
        val remote = FirestoreSessionsRepository(FirebaseFirestore.getInstance())
        val syncPrefs = SyncPrefs(applicationContext)
        val statusPrefs = SyncStatusPrefs(applicationContext)

        // 2025 De momento, sin grupos reales:
        val groupProvider = object : GroupIdProvider {
            override suspend fun getGroupIdsForUser(uid: String): List<String> = emptyList()
        }

        val repo: SessionsRepository = SessionsRepositoryImpl(
            local = local,
            remote = remote,
            syncPrefs = syncPrefs,
            groupIdProvider = groupProvider
        )

        return try {
            repo.sync(uid)
            statusPrefs.setLastSyncMillis(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            Log.e("LUDIARY_SYNC_SESSIONS", "Error syncing sessions", e)
            Result.retry()
        }
    }
}
