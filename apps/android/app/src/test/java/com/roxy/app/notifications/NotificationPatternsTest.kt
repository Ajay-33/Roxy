package com.roxy.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPatternsTest {
    @Test fun `uses a deterministic median baseline with supporting counts`() {
        val observation = NotificationPatterns.compare(9, listOf(2, 5, 7, 5, 11))!!
        assertEquals("higher", observation.direction)
        assertEquals(5, observation.baselineCount)
        assertEquals(5, observation.comparedPeriods)
        assertEquals("Notification activity was higher: 9 activity events compared with a baseline of 5 across 5 prior periods.", observation.explanation)
    }

    @Test fun `does not make an observation without enough valid history`() {
        assertNull(NotificationPatterns.compare(3, listOf(1, 2)))
        assertNull(NotificationPatterns.compare(3, listOf(1, -1, 2)))
    }
}
