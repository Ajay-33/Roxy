package com.roxy.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsageCollectionDao {
    @Query("SELECT * FROM usage_collection_cursor WHERE id = 1") fun cursor(): UsageCollectionCursor?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun saveCursor(cursor: UsageCollectionCursor)
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insertObservations(rows: List<UsageObservationEntity>): List<Long>
    @Query("SELECT COUNT(*) FROM usage_observations") fun observationCount(): Int
    @Query("SELECT MAX(occurredAtEpochMillis) FROM usage_observations") fun latestObservationEpochMillis(): Long?
    @Query("SELECT * FROM usage_observations ORDER BY occurredAtEpochMillis") fun observations(): List<UsageObservationEntity>
    @Query("DELETE FROM usage_15m_buckets") fun clearBuckets()
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun saveBuckets(rows: List<UsageBucketEntity>)
    @Query("SELECT COUNT(*) FROM usage_15m_buckets") fun bucketCount(): Int
    @Query("SELECT * FROM usage_15m_buckets") fun buckets(): List<UsageBucketEntity>
    @Query("SELECT e.packageName, e.bucketStartEpochMillis FROM usage_bucket_exports e JOIN local_events l ON l.id = e.eventId WHERE l.syncState IN ('PENDING', 'IN_FLIGHT', 'ACKNOWLEDGED')") fun activeExportedBucketKeys(): List<UsageBucketExportKey>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun markExported(rows: List<UsageBucketExportEntity>)
}
