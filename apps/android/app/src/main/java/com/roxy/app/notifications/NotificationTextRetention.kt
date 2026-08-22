package com.roxy.app.notifications

object NotificationTextRetention {
    const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1_000

    fun expiresAt(recordedAtEpochMillis: Long): Long = recordedAtEpochMillis + SEVEN_DAYS_MILLIS

    fun expiredIds(rows: List<Pair<String, Long>>, nowEpochMillis: Long): Set<String> =
        rows.filter { (_, expiresAt) -> expiresAt <= nowEpochMillis }.mapTo(mutableSetOf()) { it.first }
}
