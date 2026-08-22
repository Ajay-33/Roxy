package com.roxy.app.notifications

import com.roxy.app.data.LocalEventEntity
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.SyncState
import com.roxy.app.sync.Pairing

object NotificationMetadataExporter {
    fun queue(database: RoxyDatabase, pairing: Pairing, event: NotificationMetadataEvent, redactionCount: Int = 0) {
        database.localEventDao().insert(outboxEvent(pairing, event, redactionCount, System.currentTimeMillis()))
    }

    internal fun outboxEvent(
        pairing: Pairing,
        event: NotificationMetadataEvent,
        redactionCount: Int,
        createdAtEpochMillis: Long,
    ) = LocalEventEntity(
            id = event.id, schemaVersion = 1, deviceId = pairing.deviceId, eventType = event.eventKind,
            occurredAtEpochMillis = event.occurredAtEpochMillis, recordedAtEpochMillis = event.recordedAtEpochMillis,
            observedTimezone = event.observedTimezone, source = "android.notification_listener", sensitivity = "private",
            payloadJson = payloadJson(event, redactionCount), confidence = 1.0, isDerived = false,
            syncState = SyncState.PENDING, rejectionCode = null, createdAtEpochMillis = createdAtEpochMillis,
        )

    internal fun payloadJson(event: NotificationMetadataEvent, redactionCount: Int): String =
        "{\"packageName\":\"${escapeJson(event.packageName)}\",\"identityDigest\":\"${escapeJson(event.identityDigest)}\",\"redactionCount\":$redactionCount}"

    private fun escapeJson(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }
}
