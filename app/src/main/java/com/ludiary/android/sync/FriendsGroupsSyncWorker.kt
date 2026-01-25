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

class FriendsGroupsSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val me = auth.currentUser ?: return Result.success()

        return try {
            val db = LudiaryDatabase.getInstance(applicationContext)
            val fs = FirebaseFirestore.getInstance()

            // Friends repo (igual que en UI)
            val friendsLocal = LocalFriendsDataSource(db.friendDao())
            val friendsRemote = FirestoreFriendsRepository(fs)
            val friendsRepo: FriendsRepository = FriendsRepositoryImpl(
                local = friendsLocal,
                remote = friendsRemote,
                auth = auth
            )

            // Groups repo (refactor: local + remote)
            val groupsLocal = LocalGroupsDataSource(db.groupDao())
            val groupsRemote = FirestoreGroupsRepository(fs)
            val groupsRepo: GroupsRepository = GroupsRepositoryImpl(
                local = groupsLocal,
                remote = groupsRemote,
                auth = auth
            )

            // 1) flush pendientes
            friendsRepo.flushOfflineInvites()
            groupsRepo.flushPendingInvites()

            // 2) marcar “última sync ok”
            SyncStatusPrefs(applicationContext).setLastSyncMillis(System.currentTimeMillis())

            Result.success()
        } catch (e: Exception) {
            Log.w("LUDIARY_SYNC_FG", "Retry", e)
            Result.retry()
        }
    }
}