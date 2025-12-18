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


class UserGamesSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val db = LudiaryDatabase.getInstance(applicationContext)
        val localDS = LocalUserGamesDataSource(db.userGameDao())
        val remote = FirestoreUserGamesRepository(FirebaseFirestore.getInstance())
        val repo = UserGamesRepositoryImpl(localDS, remote)

        return try {
            val synced = repo.syncPending(uid)

            if (synced > 0) {
                val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                prefs.edit { putLong(SyncFragment.KEY_LAST_LIBRARY_SYNC, now) }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}