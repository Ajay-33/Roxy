package com.roxy.app.notifications

import com.roxy.app.sync.Pairing
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class NotificationHour(val hour: Int, val count: Int)
data class NotificationSource(val packageName: String, val count: Int)
data class NotificationBurst(val startHour: Int, val count: Int)
data class NotificationCompleteness(val status: String, val reason: String)
data class NotificationAnalytics(val count: Int, val postedCount: Int, val updatedCount: Int, val removedCount: Int, val period: String, val hourly: List<NotificationHour>, val sources: List<NotificationSource>, val bursts: List<NotificationBurst>, val completeness: NotificationCompleteness) {
    val busiestHour: Int? get() = hourly.maxByOrNull { it.count }?.hour
    val topSourcePackage: String? get() = sources.firstOrNull()?.packageName
    val topSourceCount: Int get() = sources.firstOrNull()?.count ?: 0
}
sealed interface NotificationAnalyticsResult { data class Success(val value: NotificationAnalytics) : NotificationAnalyticsResult; data class Error(val code: String) : NotificationAnalyticsResult }

object NotificationAnalyticsReader {
    fun read(pairing: Pairing, date: String = LocalDate.now().toString(), period: String = "day"): NotificationAnalyticsResult = runCatching {
        val connection = (URL("${pairing.endpoint}/v1/notifications/analytics?deviceId=${pairing.deviceId}&date=$date&period=$period").openConnection() as HttpURLConnection).apply { requestMethod="GET"; connectTimeout=10_000; readTimeout=10_000; setRequestProperty("Authorization", "Bearer ${pairing.credential}") }
        if (connection.responseCode !in 200..299) return NotificationAnalyticsResult.Error("notification_analytics_http_${connection.responseCode}")
        parse(connection.inputStream.bufferedReader().use { it.readText() })
    }.getOrElse { NotificationAnalyticsResult.Error("notification_analytics_unavailable") }
    internal fun parse(body: String): NotificationAnalyticsResult {
        fun n(name:String)=Regex("\\\"$name\\\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()
        fun q(name:String)=Regex("\\\"$name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(body)?.groupValues?.get(1)
        val count=n("count") ?: return NotificationAnalyticsResult.Error("notification_analytics_invalid_response")
        val posted=n("postedCount") ?: return NotificationAnalyticsResult.Error("notification_analytics_invalid_response")
        val updated=n("updatedCount") ?: 0
        val removed=n("removedCount") ?: return NotificationAnalyticsResult.Error("notification_analytics_invalid_response")
        val hours=Regex("\\\"hour\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"count\\\"\\s*:\\s*(\\d+)").findAll(body).map { NotificationHour(it.groupValues[1].toInt(),it.groupValues[2].toInt()) }.toList()
        val sources=Regex("\\\"packageName\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"count\\\"\\s*:\\s*(\\d+)").findAll(body).map { NotificationSource(it.groupValues[1],it.groupValues[2].toInt()) }.toList()
        val bursts=Regex("\\\"startHour\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"count\\\"\\s*:\\s*(\\d+)").findAll(body).map { NotificationBurst(it.groupValues[1].toInt(),it.groupValues[2].toInt()) }.toList()
        return NotificationAnalyticsResult.Success(NotificationAnalytics(count,posted,updated,removed,q("period") ?: "day",hours,sources,bursts,NotificationCompleteness(q("status") ?: "incomplete",q("reason") ?: if(count==0)"no_notification_activity" else "coverage_not_proven")))
    }
}
