package com.roxy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_events")
data class LocalEventEntity(
    @PrimaryKey val id: String,
    val schemaVersion: Int,
    val deviceId: String,
    val eventType: String,
    val occurredAtEpochMillis: Long,
    val recordedAtEpochMillis: Long,
    val observedTimezone: String,
    val source: String,
    val sensitivity: String,
    val payloadJson: String,
    val confidence: Double,
    val isDerived: Boolean,
    val syncState: SyncState,
    val rejectionCode: String?,
    val createdAtEpochMillis: Long,
)

enum class SyncState { PENDING, IN_FLIGHT, ACKNOWLEDGED, REJECTED }
