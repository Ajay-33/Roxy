package com.roxy.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("roxy-sync")
            .build()
        // A pairing change must schedule a fresh attempt. A completed one-time request with the
        // same name would otherwise leave newly queued local events waiting indefinitely.
        WorkManager.getInstance(context).enqueueUniqueWork("roxy-sync", ExistingWorkPolicy.REPLACE, request)
    }
}
