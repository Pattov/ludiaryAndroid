package com.ludiary.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UserGamesSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // En modo local NO se sincroniza contra Firebase.
        return Result.success()
    }
}
