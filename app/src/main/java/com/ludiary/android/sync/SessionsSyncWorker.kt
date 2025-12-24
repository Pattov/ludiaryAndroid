package com.ludiary.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalSessionsDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.FirestoreSessionsRepository
import com.ludiary.android.data.repository.GroupIdProvider
import com.ludiary.android.data.repository.SessionsRepository
import com.ludiary.android.data.repository.SessionsRepositoryImpl

class SessionsSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success() // sin login → no sync remoto

        return try {
            val db = LudiaryDatabase.getInstance(applicationContext)
            val local = LocalSessionsDataSource(db.sessionDao())
            val remote = FirestoreSessionsRepository(FirebaseFirestore.getInstance())
            val prefs = SyncPrefs(applicationContext)

            // 2025 De momento, sin grupos reales:
            val groupProvider = object : GroupIdProvider {
                override suspend fun getGroupIdsForUser(uid: String): List<String> = emptyList()
            }

            val repo: SessionsRepository = SessionsRepositoryImpl(
                local = local,
                remote = remote,
                syncPrefs = prefs,
                groupIdProvider = groupProvider
            )

            repo.sync(uid)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
