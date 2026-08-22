package com.roxy.app.notifications

import com.roxy.app.sync.Pairing
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

data class NotificationTimelineItem(
    val id: String,
    val type: String,
    val occurredAt: String,
    val packageName: String,
    val redactionCount: Int,
)

sealed interface NotificationSummaryResult {
    data class Success(val count: Int, val items: List<NotificationTimelineItem>) : NotificationSummaryResult
    data class Error(val code: String) : NotificationSummaryResult
}

object NotificationSummaryReader {
    fun read(pairing: Pairing, date: String): NotificationSummaryResult = runCatching {
        val query = "deviceId=${encode(pairing.deviceId)}&date=${encode(date)}&limit=10"
        val connection = (URL("${pairing.endpoint}/v1/notifications/summary?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer ${pairing.credential}")
        }
        if (connection.responseCode !in 200..299) return NotificationSummaryResult.Error("notification_summary_http_${connection.responseCode}")
        parse(connection.inputStream.bufferedReader().use { it.readText() })
    }.getOrElse { NotificationSummaryResult.Error("notification_summary_unavailable") }

    internal fun parse(body: String): NotificationSummaryResult {
        val count = Regex("\\\"count\\\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: return NotificationSummaryResult.Error("notification_summary_invalid_response")
        val itemsBody = Regex("\\\"items\\\"\\s*:\\s*\\[(.*?)]", setOf(RegexOption.DOT_MATCHES_ALL)).find(body)?.groupValues?.get(1)
            ?: return NotificationSummaryResult.Error("notification_summary_invalid_response")
        if (itemsBody.isBlank()) return NotificationSummaryResult.Success(count, emptyList())
        val itemPattern = Regex(
            "\\{\\s*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"type\\\"\\s*:\\s*\\\"(notification\\.(?:posted|removed))\\\"\\s*,\\s*\\\"occurredAt\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"packageName\\\"\\s*:\\s*\\\"([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+)\\\"\\s*,\\s*\\\"redactionCount\\\"\\s*:\\s*(\\d+)\\s*\\}",
        )
        val items = mutableListOf<NotificationTimelineItem>()
        for (match in itemPattern.findAll(itemsBody)) {
            val occurredAt = match.groupValues[3]
            if (runCatching { Instant.parse(occurredAt) }.isFailure) return NotificationSummaryResult.Error("notification_summary_invalid_response")
            val redactionCount = match.groupValues[5].toIntOrNull()
                ?: return NotificationSummaryResult.Error("notification_summary_invalid_response")
            items += NotificationTimelineItem(
                id = match.groupValues[1],
                type = match.groupValues[2],
                occurredAt = occurredAt,
                packageName = match.groupValues[4],
                redactionCount = redactionCount,
            )
        }
        if (items.size != Regex("\\{").findAll(itemsBody).count() || items.size > count) {
            return NotificationSummaryResult.Error("notification_summary_invalid_response")
        }
        return NotificationSummaryResult.Success(count, items)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
