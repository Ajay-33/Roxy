package com.roxy.app.timeline

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineShellTest {
    private val kolkata = ZoneId.of("Asia/Calcutta")
    private val clock = Clock.fixed(Instant.parse("2026-08-21T20:00:00Z"), ZoneId.of("UTC"))

    @Test fun `initial state uses the observed local date`() {
        assertEquals(LocalDate.of(2026, 8, 22), TimelineShell.initial(clock, kolkata).selectedDate)
    }

    @Test fun `date navigation cannot move beyond today`() {
        val today = TimelineShell.initial(clock, kolkata)
        val previous = TimelineShell.previousDay(today)
        assertEquals(LocalDate.of(2026, 8, 21), previous.selectedDate)
        assertTrue(TimelineShell.canMoveToNextDay(previous, clock, kolkata))
        assertFalse(TimelineShell.canMoveToNextDay(today, clock, kolkata))
        assertEquals(today, TimelineShell.nextDay(previous, clock, kolkata))
        assertEquals(today, TimelineShell.nextDay(today, clock, kolkata))
    }

    @Test fun `unavailable and incomplete text never claims inactivity or sleep`() {
        val unavailable = TimelineShell.detail(TimelineAvailability.UNAVAILABLE)
        val incomplete = TimelineShell.detail(TimelineAvailability.INCOMPLETE)
        assertFalse(unavailable.contains("sleep", ignoreCase = true))
        assertTrue(incomplete.contains("never treated as confirmed sleep", ignoreCase = true))
        assertTrue(incomplete.contains("do not prove there was no phone activity", ignoreCase = true))
    }
}
