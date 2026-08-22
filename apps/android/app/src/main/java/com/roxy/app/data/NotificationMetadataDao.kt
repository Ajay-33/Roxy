package com.roxy.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.roxy.app.notifications.NotificationLifecycle
import com.roxy.app.notifications.NotificationLifecycleState

@Dao
interface NotificationMetadataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: NotificationMetadataEntity): Long

    @Query("SELECT eventKind, occurredAtEpochMillis FROM notification_metadata_events WHERE identityDigest = :identityDigest ORDER BY occurredAtEpochMillis DESC, createdAtEpochMillis DESC LIMIT 1")
    fun latestLifecycle(identityDigest: String): NotificationLifecycleState?

    @Query("SELECT COUNT(*) FROM notification_metadata_events WHERE identityDigest = :identityDigest AND eventKind = :eventKind AND occurredAtEpochMillis = :occurredAtEpochMillis")
    fun countMatchingCallback(identityDigest: String, eventKind: String, occurredAtEpochMillis: Long): Int

    @Transaction
    fun insertLifecycle(candidate: NotificationMetadataEntity): NotificationMetadataEntity? {
        val identityDigest = candidate.identityDigest ?: return null
        val transition = NotificationLifecycle.transition(candidate.eventKind, candidate.occurredAtEpochMillis, candidate.recordedAtEpochMillis, latestLifecycle(identityDigest)) ?: return null
        if (countMatchingCallback(identityDigest, transition.eventKind, transition.occurredAtEpochMillis) != 0) return null
        val event = candidate.copy(eventKind = transition.eventKind, occurredAtEpochMillis = transition.occurredAtEpochMillis)
        return event.takeIf { insert(it) != -1L }
    }

    @Query("SELECT COUNT(*) FROM notification_metadata_events")
    fun count(): Int

    @Query("SELECT COUNT(*) FROM notification_metadata_events WHERE eventKind = :eventKind")
    fun countWithKind(eventKind: String): Int

    @Query("DELETE FROM notification_metadata_events WHERE packageName = :packageName")
    fun deleteForPackage(packageName: String): Int
}
