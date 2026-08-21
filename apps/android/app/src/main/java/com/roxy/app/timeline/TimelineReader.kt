package com.roxy.app.timeline

import com.roxy.app.sync.Pairing
import java.net.HttpURLConnection
import java.net.URL

sealed interface TimelineReadResult {
    data class Success(val itemCount: Int, val incompleteReason: String) : TimelineReadResult
    data class Error(val code: String) : TimelineReadResult
}

object TimelineReader {
    fun read(pairing: Pairing, date: String): TimelineReadResult = runCatching {
        val url = URL("${pairing.endpoint}/v1/timeline?deviceId=${pairing.deviceId}&date=$date")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer ${pairing.credential}")
        }
        if (connection.responseCode !in 200..299) return TimelineReadResult.Error("timeline_http_${connection.responseCode}")
        parse(connection.inputStream.bufferedReader().use { it.readText() })
    }.getOrElse { TimelineReadResult.Error("timeline_unavailable") }

    internal fun parse(body: String): TimelineReadResult {
        val items = Regex("\\\"items\\\"\\s*:\\s*\\[(.*?)]", setOf(RegexOption.DOT_MATCHES_ALL)).find(body)?.groupValues?.get(1) ?: return TimelineReadResult.Error("timeline_invalid_response")
        val status = Regex("\\\"status\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(body)?.groupValues?.get(1)
        if (status != "incomplete") return TimelineReadResult.Error("timeline_invalid_response")
        val reason = Regex("\\\"reason\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(body)?.groupValues?.get(1) ?: return TimelineReadResult.Error("timeline_invalid_response")
        if (reason !in setOf("no_aggregate_data", "coverage_not_proven")) return TimelineReadResult.Error("timeline_invalid_response")
        return TimelineReadResult.Success(Regex("\\{").findAll(items).count(), reason)
    }
}
