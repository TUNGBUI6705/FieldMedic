package com.example.fieldmedic.data.remote.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.fieldmedic.data.local.db.DatabaseHolder
import com.example.fieldmedic.data.remote.engine.PullSyncEngine
import com.example.fieldmedic.data.remote.transport.FirestoreSyncTransport
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val databaseHolder = DatabaseHolder(applicationContext)

        val db = try {
            databaseHolder.requireDb()
        } catch (_: Exception) {
            return Result.success()
        }

        return try {
            val syncTransport = FirestoreSyncTransport(db.syncDao())
            val pullEngine = PullSyncEngine({ db }, db.syncCursorDao())

            syncTransport.pushBatch(limit = 20)
            pullEngine.pullAll()

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "fieldmedic_periodic_sync"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
