package com.roxy.app.timeline

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

enum class TimelineAvailability {
    UNAVAILABLE,
    INCOMPLETE,
}

data class TimelineShellState(
    val selectedDate: LocalDate,
    val availability: TimelineAvailability = TimelineAvailability.UNAVAILABLE,
)

object TimelineShell {
    fun initial(clock: Clock = Clock.systemDefaultZone(), zone: ZoneId = ZoneId.systemDefault()): TimelineShellState =
        TimelineShellState(LocalDate.now(clock.withZone(zone)))

    fun previousDay(state: TimelineShellState): TimelineShellState = state.copy(selectedDate = state.selectedDate.minusDays(1))

    fun nextDay(state: TimelineShellState, clock: Clock = Clock.systemDefaultZone(), zone: ZoneId = ZoneId.systemDefault()): TimelineShellState {
        return if (canMoveToNextDay(state, clock, zone)) state.copy(selectedDate = state.selectedDate.plusDays(1)) else state
    }

    fun canMoveToNextDay(state: TimelineShellState, clock: Clock = Clock.systemDefaultZone(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
        state.selectedDate < LocalDate.now(clock.withZone(zone))

    fun detail(availability: TimelineAvailability): String = when (availability) {
        TimelineAvailability.UNAVAILABLE -> "Timeline data has not been read from Roxy yet. Prepare local totals, queue aggregate totals, and sync them before a future read can show verified data."
        TimelineAvailability.INCOMPLETE -> "Aggregate timeline coverage is incomplete. Missing entries do not prove there was no phone activity and are never treated as confirmed sleep."
    }
}
