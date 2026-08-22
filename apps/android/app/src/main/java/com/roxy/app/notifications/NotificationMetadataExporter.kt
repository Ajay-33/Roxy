package com.roxy.app.notifications

import com.roxy.app.data.LocalEventEntity
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.SyncState
import com.roxy.app.sync.Pairing
import org.json.JSONObject

object NotificationMetadataExporter {
    fun queue(database: RoxyDatabase, pairing: Pairing, event: NotificationMetadataEvent, redactionCount: Int = 0) {
        val now = System.currentTimeMillis()
        database.localEventDao().insert(LocalEventEntity(
            id = event.id, schemaVersion = 1, deviceId = pairing.deviceId, eventType = event.eventKind,
            occurredAtEpochMillis = event.occurredAtEpochMillis, recordedAtEpochMillis = event.recordedAtEpochMillis,
            observedTimezone = event.observedTimezone, source = "android.notification_listener", sensitivity = "private",
            payloadJson = payloadJson(event, redactionCount), confidence = 1.0, isDerived = false,
            syncState = SyncState.PENDING, rejectionCode = null, createdAtEpochMillis = now,
        ))
    }

    internal fun payloadJson(event: NotificationMetadataEvent, redactionCount: Int): String = JSONObject().apply {
        put("packageName", event.packageName); put("identityDigest", event.identityDigest); put("redactionCount", redactionCount)
    }.toString()
}
