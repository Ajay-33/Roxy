package com.roxy.app.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCollectionSchedulerTest {
    @Test fun `automatic collection requires both owner consent and pairing`() {
        assertTrue(UsageCollectionScheduler.shouldSchedule(hasUsageAccess = true, isPaired = true))
        assertFalse(UsageCollectionScheduler.shouldSchedule(hasUsageAccess = false, isPaired = true))
        assertFalse(UsageCollectionScheduler.shouldSchedule(hasUsageAccess = true, isPaired = false))
    }

    @Test fun `collection target stays within the balanced battery cadence`() {
        assertTrue(UsageCollectionScheduler.REPEAT_MINUTES in 30L..60L)
        assertEquals("roxy-usage-collection", UsageCollectionScheduler.WORK_NAME)
    }
}
