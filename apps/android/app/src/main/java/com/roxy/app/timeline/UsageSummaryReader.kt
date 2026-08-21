package com.roxy.app.timeline

import android.content.Context
import com.roxy.app.sync.Pairing
import java.net.HttpURLConnection
import java.net.URL

data class TodayApp(val id: String, val durationMillis: Long)
data class TodayAppLabel(val text: String, val resolvedLocally: Boolean)

sealed interface UsageSummaryResult {
    data class Success(
        val totalMillis: Long,
        val apps: List<TodayApp>,
        val reason: String,
    ) : UsageSummaryResult

    data class Error(val code: String) : UsageSummaryResult
}

object UsageSummaryReader {
    fun read(pairing: Pairing, date: String): UsageSummaryResult = runCatching {
        val connection = (URL("${pairing.endpoint}/v1/usage/summary?deviceId=${pairing.deviceId}&date=$date&limit=5").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer ${pairing.credential}")
        }
        if (connection.responseCode !in 200..299) return UsageSummaryResult.Error("summary_http_${connection.responseCode}")
        parse(connection.inputStream.bufferedReader().use { it.readText() })
    }.getOrElse { UsageSummaryResult.Error("summary_unavailable") }
    internal fun parse(body: String): UsageSummaryResult {
        val total = Regex("\\\"totalDurationMillis\\\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toLongOrNull() ?: return UsageSummaryResult.Error("summary_invalid_response")
        val reason = Regex("\\\"reason\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(body)?.groupValues?.get(1)
            ?.takeIf { it == "no_aggregate_data" || it == "coverage_not_proven" }
            ?: return UsageSummaryResult.Error("summary_invalid_response")
        val apps = Regex("\\\"appId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"durationMillis\\\"\\s*:\\s*(\\d+)").findAll(body).map { TodayApp(it.groupValues[1], it.groupValues[2].toLong()) }.toList()
        return UsageSummaryResult.Success(total, apps, reason)
    }
    fun resolveLabel(context: Context, id: String): TodayAppLabel = displayLabel(
        runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(id, 0)).toString()
        }.getOrNull(),
        id,
    )

    internal fun displayLabel(resolvedLabel: String?, id: String): TodayAppLabel =
        resolvedLabel?.takeIf { it.isNotBlank() }
            ?.let { TodayAppLabel(it, resolvedLocally = true) }
            ?: TodayAppLabel(id, resolvedLocally = false)
}
