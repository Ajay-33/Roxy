package com.roxy.app.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UsageCollectorTest {
    @Test fun `initial window is bounded to 24 hours`() {
        assertEquals(UsageCollectionWindow(0, UsageCollector.MAX_WINDOW_MILLIS), UsageCollector.window(null, UsageCollector.MAX_WINDOW_MILLIS))
    }
    @Test fun `cursor window overlaps by fifteen minutes`() {
        assertEquals(UsageCollectionWindow(100, 1_000), UsageCollector.window(100 + UsageCollector.OVERLAP_MILLIS, 1_000))
    }
    @Test fun `identity is deterministic and distinguishes observations`() {
        assertEquals(UsageCollector.observationId("example.alpha", "foreground", 1), UsageCollector.observationId("example.alpha", "foreground", 1))
        assertNotEquals(UsageCollector.observationId("example.alpha", "foreground", 1), UsageCollector.observationId("example.alpha", "background", 1))
    }
}
