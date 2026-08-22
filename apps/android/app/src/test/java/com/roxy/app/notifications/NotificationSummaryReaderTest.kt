package com.roxy.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSummaryReaderTest {
    @Test
    fun `parses a metadata-only notification summary`() {
        val result = NotificationSummaryReader.parse(
            """{"count":2,"items":[{"id":"synthetic-1","type":"notification.posted","occurredAt":"2026-08-22T10:00:00.000Z","packageName":"example.safe","redactionCount":0}]}""",
        )

        assertEquals(
            NotificationSummaryResult.Success(
                count = 2,
                items = listOf(NotificationTimelineItem("synthetic-1", "notification.posted", "2026-08-22T10:00:00.000Z", "example.safe", 0)),
            ),
            result,
        )
    }

    @Test
    fun `parses an empty summary`() {
        assertEquals(NotificationSummaryResult.Success(0, emptyList()), NotificationSummaryReader.parse("""{"count":0,"items":[]}"""))
    }

    @Test
    fun `rejects malformed and non-metadata responses`() {
        assertEquals(NotificationSummaryResult.Error("notification_summary_invalid_response"), NotificationSummaryReader.parse("""{"count":1,"items":[{"id":"synthetic-1","type":"notification.posted","occurredAt":"not-a-date","packageName":"example.safe","redactionCount":0}]}"""))
        assertEquals(NotificationSummaryResult.Error("notification_summary_invalid_response"), NotificationSummaryReader.parse("""{"count":1,"items":[{"id":"synthetic-1","type":"notification.posted","occurredAt":"2026-08-22T10:00:00.000Z","packageName":"example.safe","redactionCount":0,"body":"must-not-render"}]}"""))
    }
}
