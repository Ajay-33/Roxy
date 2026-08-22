package com.roxy.app.notifications

import android.content.Context

class NotificationListenerHealthStore(context: Context) {
    private val preferences = context.getSharedPreferences("notification_listener_health", Context.MODE_PRIVATE)

    fun wasAccessGranted(): Boolean = preferences.getBoolean(ACCESS_GRANTED_KEY, false)
    fun listenerState(): String = preferences.getString(LISTENER_STATE_KEY, "not_connected") ?: "not_connected"

    fun markAccessGranted() = preferences.edit().putBoolean(ACCESS_GRANTED_KEY, true).apply()
    fun markConnected() = preferences.edit().putString(LISTENER_STATE_KEY, "connected").apply()
    fun markDisconnected() = preferences.edit().putString(LISTENER_STATE_KEY, "disconnected").apply()

    private companion object {
        const val ACCESS_GRANTED_KEY = "access_granted"
        const val LISTENER_STATE_KEY = "listener_state"
    }
}
