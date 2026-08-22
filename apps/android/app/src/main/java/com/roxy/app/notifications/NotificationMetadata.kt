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

data class NotificationLifecycleState(val eventKind: String, val occurredAtEpochMillis: Long)

/**
 * Gives each accepted Android callback a truthful lifecycle meaning without ever inspecting
 * notification content. A repeated or out-of-order active callback is ignored; a later active
 * callback becomes an update; an active callback after a removal begins a new object lifecycle.
 */
object NotificationLifecycle {
    data class Transition(val eventKind: String, val occurredAtEpochMillis: Long)

    fun transition(proposedKind: String, postedAtEpochMillis: Long, recordedAtEpochMillis: Long, previous: NotificationLifecycleState?): Transition? = when (proposedKind) {
        "notification.posted" -> when {
            previous == null || previous.eventKind == "notification.removed" -> Transition("notification.posted", postedAtEpochMillis)
            recordedAtEpochMillis <= previous.occurredAtEpochMillis -> null
            else -> Transition("notification.updated", recordedAtEpochMillis)
        }
        "notification.removed" -> when {
            previous?.eventKind == "notification.removed" && recordedAtEpochMillis <= previous.occurredAtEpochMillis -> null
            else -> Transition("notification.removed", recordedAtEpochMillis)
        }
        else -> null
    }
}

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
