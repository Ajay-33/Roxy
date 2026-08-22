package com.roxy.app.notifications

import java.security.MessageDigest
import java.time.ZoneId

data class NotificationMetadataEvent(
    val id: String,
    val eventKind: String,
    val occurredAtEpochMillis: Long,
    val recordedAtEpochMillis: Long,
    val observedTimezone: String,
    val packageName: String,
    val identityDigest: String,
)

object NotificationEventGate {
    fun create(
        enabled: Boolean,
        rules: List<NotificationPackageRule>,
        packageName: String,
        eventKind: String,
        occurredAtEpochMillis: Long,
        recordedAtEpochMillis: Long,
        observedTimezone: ZoneId,
        eventId: String,
        notificationKey: String,
        isGroupSummary: Boolean,
    ): NotificationMetadataEvent? {
        if (!enabled || isGroupSummary || notificationKey.isBlank() || NotificationPolicy.policyFor(rules, packageName) == NotificationPackagePolicy.BLOCKED) return null
        return NotificationMetadataEvent(eventId, eventKind, occurredAtEpochMillis, recordedAtEpochMillis, observedTimezone.id, packageName, identityDigest(packageName, notificationKey))
    }

    fun identityDigest(packageName: String, notificationKey: String): String = MessageDigest.getInstance("SHA-256")
        .digest("$packageName|$notificationKey".toByteArray())
        .joinToString("") { "%02x".format(it) }
}
