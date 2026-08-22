package com.roxy.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notification_metadata_events", indices = [Index(value = ["identityDigest", "eventKind", "occurredAtEpochMillis"], unique = true)])
data class NotificationMetadataEntity(
    @PrimaryKey val id: String,
    val eventKind: String,
    val occurredAtEpochMillis: Long,
    val recordedAtEpochMillis: Long,
    val observedTimezone: String,
    val packageName: String,
    val identityDigest: String?,
    val source: String = "android.notification_listener",
    val createdAtEpochMillis: Long,
)
