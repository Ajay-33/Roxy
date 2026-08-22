package com.roxy.app.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.provider.Settings
import android.service.notification.NotificationListenerService

object NotificationAccess {
    fun isAllowed(context: Context): Boolean = context
        .getSystemService(NotificationManager::class.java)
        ?.isNotificationListenerAccessGranted(ComponentName(context, RoxyNotificationListener::class.java))
        ?: false

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
