package com.roxy.app.usage

import android.app.usage.UsageStatsManager
import android.content.Context

data class UsageSample(val packageName: String, val foregroundDurationMillis: Long)

sealed interface UsageQueryResult {
    data object AccessDenied : UsageQueryResult
    data class Success(val samples: List<UsageSample>) : UsageQueryResult
}

object UsageStatsReader {
    const val MAX_WINDOW_MILLIS: Long = 24 * 60 * 60 * 1000L

    fun queryPrevious24Hours(context: Context, nowEpochMillis: Long = System.currentTimeMillis()): UsageQueryResult {
        if (!UsageAccess.isAllowed(context)) return UsageQueryResult.AccessDenied
        val usageStats = context.getSystemService(UsageStatsManager::class.java)
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, nowEpochMillis - MAX_WINDOW_MILLIS, nowEpochMillis)
        return UsageQueryResult.Success(
            UsageStatsNormalizer.normalize(usageStats.orEmpty().map {
                UsageSample(it.packageName, it.totalTimeInForeground)
            }),
        )
    }
}

object UsageStatsNormalizer {
    fun normalize(samples: List<UsageSample>): List<UsageSample> = samples
        .asSequence()
        .filter { it.packageName.isNotBlank() && it.foregroundDurationMillis > 0 }
        .groupingBy { it.packageName }
        .fold(0L) { total, sample -> saturatingAdd(total, sample.foregroundDurationMillis) }
        .map { UsageSample(it.key, it.value) }
        .sortedBy { it.packageName }
        .toList()

    private fun saturatingAdd(first: Long, second: Long): Long =
        if (Long.MAX_VALUE - first < second) Long.MAX_VALUE else first + second
}
