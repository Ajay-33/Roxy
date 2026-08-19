package com.roxy.app.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

object UsageAccess {
    @Suppress("DEPRECATION") // `unsafeCheckOpNoThrow` is unavailable on the min Android API level.
    fun isAllowed(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
