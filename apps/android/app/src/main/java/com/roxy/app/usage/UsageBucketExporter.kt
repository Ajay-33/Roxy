package com.roxy.app.usage

import com.roxy.app.data.LocalEventEntity
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.SyncState
import com.roxy.app.data.UsageBucketExportEntity
import com.roxy.app.data.UsageBucketExportKey
import com.roxy.app.data.UsageBucketEntity
import org.json.JSONObject
import java.security.SecureRandom
import java.util.TimeZone

object UsageBucketExporter {
    fun queue(database: RoxyDatabase, deviceId: String, observedTimezone: String = TimeZone.getDefault().id, now: Long = System.currentTimeMillis()): Int = database.runInTransaction<Int> {
        val usageDao = database.usageCollectionDao()
        val buckets = unqueuedBuckets(usageDao.buckets(), usageDao.activeExportedBucketKeys().toSet())
        val exports = buckets.map { bucket ->
            val id = uuidV7()
            database.localEventDao().insert(LocalEventEntity(id, 1, deviceId, "usage.bucket", bucket.bucketStartEpochMillis, now, observedTimezone, "android.usage_stats", "private", payloadJson(bucket.packageName, bucket.durationMillis), 1.0, true, SyncState.PENDING, null, now))
            UsageBucketExportEntity(bucket.packageName, bucket.bucketStartEpochMillis, id)
        }
        usageDao.markExported(exports); exports.size
    }

    internal fun unqueuedBuckets(buckets: List<UsageBucketEntity>, activeExports: Set<UsageBucketExportKey>): List<UsageBucketEntity> =
        buckets.filter { UsageBucketExportKey(it.packageName, it.bucketStartEpochMillis) !in activeExports }

    internal fun payloadFields(packageName: String, durationMillis: Long): Map<String, Any> = mapOf(
        "packageName" to packageName,
        "durationMillis" to durationMillis,
    )

    internal fun payloadJson(packageName: String, durationMillis: Long): String = JSONObject().apply {
        payloadFields(packageName, durationMillis).forEach { (key, value) -> put(key, value) }
    }.toString()

    private fun uuidV7(): String { val random = ByteArray(10).also(SecureRandom()::nextBytes); val timestamp = System.currentTimeMillis().toString(16).padStart(12, '0'); val hex = random.joinToString("") { "%02x".format(it.toInt() and 0xff) }; val variant = ((random[0].toInt() and 3) or 8).toString(16); return "${timestamp.substring(0,8)}-${timestamp.substring(8,12)}-7${hex.substring(0,3)}-$variant${hex.substring(3,6)}-${hex.substring(6,18)}" }
}
