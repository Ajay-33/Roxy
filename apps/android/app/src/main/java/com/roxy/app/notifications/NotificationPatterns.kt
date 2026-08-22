package com.roxy.app.notifications

/** A transparent comparison of notification activity counts; it never infers message meaning. */
data class NotificationPatternObservation(
    val direction: String,
    val currentCount: Int,
    val baselineCount: Int,
    val comparedPeriods: Int,
    val explanation: String,
)

object NotificationPatterns {
    fun compare(currentCount: Int, priorCounts: List<Int>): NotificationPatternObservation? {
        if (currentCount < 0 || priorCounts.size < 3 || priorCounts.any { it < 0 }) return null
        val baseline = priorCounts.sorted()[priorCounts.size / 2]
        val direction = when {
            currentCount > baseline -> "higher"
            currentCount < baseline -> "lower"
            else -> "similar"
        }
        return NotificationPatternObservation(
            direction = direction,
            currentCount = currentCount,
            baselineCount = baseline,
            comparedPeriods = priorCounts.size,
            explanation = "Notification activity was $direction: $currentCount activity events compared with a baseline of $baseline across ${priorCounts.size} prior periods.",
        )
    }
}
