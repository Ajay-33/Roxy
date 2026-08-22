package com.roxy.app.notifications

import com.roxy.app.sync.Pairing
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class NotificationAnalytics(val count: Int, val postedCount: Int, val removedCount: Int, val busiestHour: Int?, val topSourcePackage: String?, val topSourceCount: Int)
sealed interface NotificationAnalyticsResult { data class Success(val value: NotificationAnalytics) : NotificationAnalyticsResult; data class Error(val code: String) : NotificationAnalyticsResult }

object NotificationAnalyticsReader {
    fun read(pairing: Pairing, date: String = LocalDate.now().toString()): NotificationAnalyticsResult = runCatching {
        val connection = (URL("${pairing.endpoint}/v1/notifications/analytics?deviceId=${pairing.deviceId}&date=$date").openConnection() as HttpURLConnection).apply { requestMethod="GET"; connectTimeout=10_000; readTimeout=10_000; setRequestProperty("Authorization", "Bearer ${pairing.credential}") }
        if (connection.responseCode !in 200..299) return NotificationAnalyticsResult.Error("notification_analytics_http_${connection.responseCode}")
        parse(connection.inputStream.bufferedReader().use { it.readText() })
    }.getOrElse { NotificationAnalyticsResult.Error("notification_analytics_unavailable") }
    internal fun parse(body: String): NotificationAnalyticsResult {
        fun n(name:String)=Regex("\\\"$name\\\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()
        val count=n("count") ?: return NotificationAnalyticsResult.Error("notification_analytics_invalid_response")
        val posted=n("postedCount") ?: return NotificationAnalyticsResult.Error("notification_analytics_invalid_response")
        val removed=n("removedCount") ?: return NotificationAnalyticsResult.Error("notification_analytics_invalid_response")
        val hours=Regex("\\\"hour\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"count\\\"\\s*:\\s*(\\d+)").findAll(body).map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }.toList()
        val top=Regex("\\\"topApps\\\"\\s*:\\s*\\[.*?\\\"packageName\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"count\\\"\\s*:\\s*(\\d+)", RegexOption.DOT_MATCHES_ALL).find(body)
        return NotificationAnalyticsResult.Success(NotificationAnalytics(count,posted,removed,hours.maxByOrNull { it.second }?.first,top?.groupValues?.get(1),top?.groupValues?.get(2)?.toIntOrNull() ?: 0))
    }
}
