package com.roxy.app.notifications

import com.roxy.app.data.SyncState
import com.roxy.app.sync.Pairing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMetadataExporterTest {
    private val event = NotificationMetadataEvent(
        id = "01900000-0000-7000-8000-000000000000",
        eventKind = "notification.posted",
        occurredAtEpochMillis = 100,
        recordedAtEpochMillis = 110,
        observedTimezone = "UTC",
        packageName = "example.safe",
        identityDigest = "a".repeat(64),
    )

    @Test
    fun `approved metadata becomes one pending private outbox event`() {
        val outbox = NotificationMetadataExporter.outboxEvent(
            pairing = Pairing("https://example.test", "credential", "device-id"),
            event = event,
            redactionCount = 0,
            createdAtEpochMillis = 120,
        )

        assertEquals(event.id, outbox.id)
        assertEquals(SyncState.PENDING, outbox.syncState)
        assertEquals("android.notification_listener", outbox.source)
        assertEquals("private", outbox.sensitivity)
        assertEquals(120, outbox.createdAtEpochMillis)
    }

    @Test
    fun `payload contains only the allowlisted notification metadata fields`() {
        val payload = NotificationMetadataExporter.payloadJson(event, redactionCount = 2)

        assertEquals(
            "{\"packageName\":\"example.safe\",\"identityDigest\":\"${"a".repeat(64)}\",\"redactionCount\":2}",
            payload,
        )
        assertFalse(payload.contains("\"title\""))
        assertFalse(payload.contains("\"body\""))
        assertFalse(payload.contains("\"text\""))
        assertFalse(payload.contains("\"action\""))
        assertTrue(payload.contains("redactionCount"))
    }
}
