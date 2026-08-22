package com.roxy.app.notifications

import android.content.Context

class NotificationPolicyStore(context: Context) {
    private val preferences = context.getSharedPreferences("notification_policy", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = preferences.getBoolean(ENABLED_KEY, true)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(ENABLED_KEY, enabled).apply()
    }

    fun rules(): List<NotificationPackageRule> =
        preferences.getStringSet(RULES_KEY, emptySet())
            .orEmpty()
            .mapNotNull(::decode)
            .sortedBy { it.packageName }

    fun saveRules(rules: List<NotificationPackageRule>) {
        preferences.edit().putStringSet(RULES_KEY, rules.map(::encode).toSet()).apply()
    }

    private fun encode(rule: NotificationPackageRule): String = "${rule.policy.name}|${rule.packageName}"

    private fun decode(value: String): NotificationPackageRule? {
        val separator = value.indexOf('|')
        if (separator <= 0) return null
        val policy = runCatching { NotificationPackagePolicy.valueOf(value.substring(0, separator)) }.getOrNull() ?: return null
        return NotificationPolicy.defaultRule(value.substring(separator + 1))?.copy(policy = policy)
    }

    private companion object {
        const val ENABLED_KEY = "enabled"
        const val RULES_KEY = "rules"
    }
}
