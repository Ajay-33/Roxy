package com.roxy.app.health

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectorHealthReaderTest {
    @Test fun `usage health fails safely before consent or pairing`() {
        assertEquals("needs_usage_access", CollectorHealthReader.usageState(false, true, 1))
        assertEquals("needs_pairing", CollectorHealthReader.usageState(true, false, 1))
        assertEquals("waiting_for_observation", CollectorHealthReader.usageState(true, true, null))
        assertEquals("collecting_automatically", CollectorHealthReader.usageState(true, true, 1))
    }

    @Test fun `notification health distinguishes pause permission and listener failure`() {
        assertEquals("paused_by_owner", CollectorHealthReader.notificationState(false, true, "connected"))
        assertEquals("needs_notification_access", CollectorHealthReader.notificationState(true, false, "connected"))
        assertEquals("listener_unavailable", CollectorHealthReader.notificationState(true, true, "disconnected"))
        assertEquals("collecting_metadata", CollectorHealthReader.notificationState(true, true, "connected"))
    }
}
