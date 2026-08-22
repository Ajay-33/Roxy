package com.roxy.app.usage

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.UsageBucketEntity
import com.roxy.app.sync.PairingStore
import com.roxy.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object UsageCollectionScheduler {
    const val WORK_NAME = "roxy-usage-collection"
    const val REPEAT_MINUTES = 30L

    internal fun shouldSchedule(hasUsageAccess: Boolean, isPaired: Boolean): Boolean = hasUsageAccess && isPaired

    fun reconcile(context: Context): Boolean {
        val manager = WorkManager.getInstance(context)
        if (!shouldSchedule(UsageAccess.isAllowed(context), PairingStore(context).read() != null)) {
            manager.cancelUniqueWork(WORK_NAME)
            return false
        }
        val request = PeriodicWorkRequestBuilder<UsageCollectionWorker>(REPEAT_MINUTES, TimeUnit.MINUTES).addTag(WORK_NAME).build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        return true
    }
}

class UsageCollectionWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pairing = PairingStore(applicationContext).read()
        if (!UsageAccess.isAllowed(applicationContext) || pairing == null) return@withContext Result.success()
        runCatching {
            val database = RoxyDatabase.create(applicationContext)
            UsageCollector.collect(applicationContext, database) ?: return@runCatching 0
            val dao = database.usageCollectionDao()
            database.runInTransaction {
                dao.clearBuckets()
                dao.saveBuckets(UsageAggregation.aggregate(dao.observations().map { UsageObservation(it.packageName, it.eventType, it.occurredAtEpochMillis) })
                    .map { UsageBucketEntity(it.packageName, it.bucketStart, it.durationMillis) })
            }
            UsageBucketExporter.queue(database, pairing.deviceId)
        }.fold(onSuccess = { queued -> if (queued > 0) SyncScheduler.enqueue(applicationContext); Result.success() }, onFailure = { Result.retry() })
    }
}
