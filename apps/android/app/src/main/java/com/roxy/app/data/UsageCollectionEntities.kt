package com.roxy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_collection_cursor")
data class UsageCollectionCursor(@PrimaryKey val id: Int = 1, val collectedUntilEpochMillis: Long)

@Entity(tableName = "usage_observations")
data class UsageObservationEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val eventType: String,
    val occurredAtEpochMillis: Long,
    val recordedAtEpochMillis: Long,
)

@Entity(tableName = "usage_15m_buckets", primaryKeys = ["packageName", "bucketStartEpochMillis"])
data class UsageBucketEntity(val packageName: String, val bucketStartEpochMillis: Long, val durationMillis: Long)

@Entity(tableName = "usage_bucket_exports", primaryKeys = ["packageName", "bucketStartEpochMillis"])
data class UsageBucketExportEntity(val packageName: String, val bucketStartEpochMillis: Long, val eventId: String)

data class UsageBucketExportKey(val packageName: String, val bucketStartEpochMillis: Long)
