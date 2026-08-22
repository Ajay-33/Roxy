package com.roxy.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(event: LocalEventEntity)

    @Query("SELECT * FROM local_events WHERE syncState = :state ORDER BY recordedAtEpochMillis LIMIT :limit")
    fun eventsWithState(state: SyncState, limit: Int): List<LocalEventEntity>

    @Query("SELECT COUNT(*) FROM local_events WHERE syncState = :state")
    fun countWithState(state: SyncState): Int

    @Query("SELECT COUNT(*) FROM local_events WHERE eventType LIKE 'notification.%' AND syncState = :state")
    fun notificationCountWithState(state: SyncState): Int

    @Query("SELECT MIN(recordedAtEpochMillis) FROM local_events WHERE syncState = :state")
    fun oldestRecordedAt(state: SyncState): Long?

    @Query("UPDATE local_events SET syncState = :state, rejectionCode = :rejectionCode WHERE id = :id")
    fun updateSyncState(id: String, state: SyncState, rejectionCode: String? = null)
}
