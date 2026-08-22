package com.roxy.app.notifications

enum class NotificationPackagePolicy {
    BLOCKED,
    METADATA_ONLY,
    TEXT_REDACTED,
}

data class NotificationPackageRule(
    val packageName: String,
    val policy: NotificationPackagePolicy,
)

enum class NotificationCollectorStatus {
    DISABLED,
    LISTENER_NOT_INSTALLED,
    ACCESS_NEEDED,
    READY,
    REVOKED,
}

object NotificationPolicy {
    private val packageNamePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun normalizePackageName(value: String): String? = value.trim().takeIf { packageNamePattern.matches(it) }

    fun defaultRule(packageName: String): NotificationPackageRule? =
        normalizePackageName(packageName)?.let { NotificationPackageRule(it, NotificationPackagePolicy.BLOCKED) }

    fun update(rules: List<NotificationPackageRule>, packageName: String, policy: NotificationPackagePolicy): List<NotificationPackageRule> {
        val normalized = normalizePackageName(packageName) ?: return rules
        return (rules.filterNot { it.packageName == normalized } + NotificationPackageRule(normalized, policy))
            .sortedBy { it.packageName }
    }

    fun policyFor(rules: List<NotificationPackageRule>, packageName: String): NotificationPackagePolicy =
        rules.firstOrNull { it.packageName == packageName }?.policy ?: NotificationPackagePolicy.BLOCKED

    fun collectorStatus(
        enabled: Boolean,
        listenerInstalled: Boolean,
        accessGranted: Boolean,
        wasAccessGranted: Boolean,
    ): NotificationCollectorStatus = when {
        !enabled -> NotificationCollectorStatus.DISABLED
        !listenerInstalled -> NotificationCollectorStatus.LISTENER_NOT_INSTALLED
        accessGranted -> NotificationCollectorStatus.READY
        wasAccessGranted -> NotificationCollectorStatus.REVOKED
        else -> NotificationCollectorStatus.ACCESS_NEEDED
    }

    fun statusDetail(status: NotificationCollectorStatus): String = when (status) {
        NotificationCollectorStatus.DISABLED -> "Notifications are off. Roxy does not read notification metadata or text."
        NotificationCollectorStatus.LISTENER_NOT_INSTALLED -> "Notifications are prepared locally, but Roxy cannot collect them until its listener is added in the next task."
        NotificationCollectorStatus.ACCESS_NEEDED -> "Notification Access is needed before collection can begin. You can keep notifications off at any time."
        NotificationCollectorStatus.READY -> "Notification Access is granted. Collection is still separately controlled by the owner."
        NotificationCollectorStatus.REVOKED -> "Notification Access was removed. Roxy is not collecting notifications; re-enable it only in Android Settings when you choose."
    }
}
