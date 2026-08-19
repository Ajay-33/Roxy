package com.roxy.app.usage

data class UsageObservation(val packageName: String, val eventType: String, val at: Long)
data class UsageBucket(val packageName: String, val bucketStart: Long, val durationMillis: Long)

object UsageAggregation {
    const val BUCKET_MILLIS = 15 * 60 * 1000L
    fun aggregate(rows: List<UsageObservation>): List<UsageBucket> {
        val totals = mutableMapOf<Pair<String, Long>, Long>()
        rows.groupBy { it.packageName }.forEach { (pkg, events) ->
            var started: Long? = null
            events.sortedBy { it.at }.forEach { event ->
                if (event.eventType == "foreground") started = event.at
                if (event.eventType == "background") started?.let { split(pkg, it, event.at, totals); started = null }
            }
        }
        return totals.map { UsageBucket(it.key.first, it.key.second, it.value) }.sortedWith(compareBy({ it.bucketStart }, { it.packageName }))
    }
    private fun split(pkg: String, start: Long, end: Long, totals: MutableMap<Pair<String, Long>, Long>) {
        var cursor = start
        while (cursor < end) {
            val bucket = cursor / BUCKET_MILLIS * BUCKET_MILLIS
            val until = minOf(end, bucket + BUCKET_MILLIS)
            totals[pkg to bucket] = (totals[pkg to bucket] ?: 0) + until - cursor
            cursor = until
        }
    }
}
