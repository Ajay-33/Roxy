package com.roxy.app.notifications

data class FutureNotificationContent(
    val title: String?,
    val body: String?,
    val hasActions: Boolean = false,
    val hasIntent: Boolean = false,
    val hasImage: Boolean = false,
    val hasRemoteView: Boolean = false,
    val hasToken: Boolean = false,
)

data class SanitizedNotificationContent(
    val title: String?,
    val body: String?,
    val redactionCount: Int,
)

object NotificationSanitizer {
    private val sensitivePatterns = listOf(
        Regex("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", RegexOption.IGNORE_CASE),
        Regex("(?<!\\d)(?:\\+?\\d[ -]?){7,15}(?!\\d)"),
        Regex("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)"),
        Regex("\\b\\d{4,8}\\b"),
    )

    fun sanitize(input: FutureNotificationContent): SanitizedNotificationContent? {
        if (input.hasActions || input.hasIntent || input.hasImage || input.hasRemoteView || input.hasToken) return null
        val title = redact(input.title)
        val body = redact(input.body)
        return SanitizedNotificationContent(title.value, body.value, title.count + body.count)
    }

    private fun redact(value: String?): Redaction = value?.let { original ->
        var result = original
        var count = 0
        sensitivePatterns.forEach { pattern ->
            result = pattern.replace(result) { count += 1; "[redacted]" }
        }
        Redaction(result.takeIf { it.isNotBlank() }, count)
    } ?: Redaction(null, 0)

    private data class Redaction(val value: String?, val count: Int)
}
