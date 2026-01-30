package com.ludiary.android.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.repository.FirestoreFriendsRepository
import com.ludiary.android.data.repository.FirestoreGroupsRepository
import com.ludiary.android.data.repository.FriendsRepository
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import com.ludiary.android.data.repository.GroupsRepository
import com.ludiary.android.data.repository.GroupsRepositoryImpl

/**
 * Worker de WorkManager para sincronizar operaciones pendientes (offline-first) relacionadas con Amigos y Grupos.
 * @property appContext Contexto de la aplicación.
 * @property params Parámetros del worker.
 */
class SocialSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()

        // Sin sesión: no hay nada que sincronizar.
        auth.currentUser ?: return Result.success()

        return try {
            val db = LudiaryDatabase.getInstance(applicationContext)
            val fs = FirebaseFirestore.getInstance()

            // Friends: Room + Firestore
            val friendsRepo: FriendsRepository = FriendsRepositoryImpl(
                local = LocalFriendsDataSource(db.friendDao()),
                remote = FirestoreFriendsRepository(fs),
                auth = auth
            )

            // Groups: Room + Firestore
            val groupsRepo: GroupsRepository = GroupsRepositoryImpl(
                local = LocalGroupsDataSource(db.groupDao()),
                remote = FirestoreGroupsRepository(fs),
                auth = auth
            )

            // 1) Flush de operaciones pendientes (offline-first)
            friendsRepo.flushOfflineInvites()
            groupsRepo.flushPendingInvites()

            // 2) Marcar "última sync OK"
            SyncStatusPrefs(applicationContext).setLastSyncMillis(System.currentTimeMillis())

            Result.success()
        } catch (e: Exception) {
            Log.w("LUDIARY_SYNC_FG", "SocialSyncWorker failed -> retry", e)
            Result.retry()
        }
    }
}