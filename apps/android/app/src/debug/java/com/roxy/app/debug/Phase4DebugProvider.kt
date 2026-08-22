package com.roxy.app.debug

import android.content.ContentProvider
import android.content.ContentValues
import android.app.NotificationChannel
import android.app.NotificationManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.SyncState
import com.roxy.app.data.NotificationMetadataEntity
import com.roxy.app.health.CollectorHealthReader
import com.roxy.app.notifications.NotificationPackagePolicy
import com.roxy.app.notifications.NotificationAccess
import com.roxy.app.notifications.NotificationListenerHealthStore
import com.roxy.app.notifications.NotificationPolicy
import com.roxy.app.notifications.NotificationPolicyStore
import com.roxy.app.notifications.NotificationSummaryReader
import com.roxy.app.notifications.NotificationSummaryResult
import com.roxy.app.notifications.NotificationAnalyticsReader
import com.roxy.app.notifications.NotificationAnalyticsResult
import com.roxy.app.sync.PairingStore
import java.time.LocalDate

class Phase4DebugProvider : ContentProvider() {
    override fun onCreate(): Boolean = context != null
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val appContext = requireNotNull(context).applicationContext
        val database = RoxyDatabase.create(appContext)
        val store = NotificationPolicyStore(appContext)
        return when (method) {
            "set_synthetic_policy" -> {
                val rules = NotificationPolicy.update(
                    NotificationPolicy.update(store.rules(), SYNTHETIC_PACKAGE, NotificationPackagePolicy.METADATA_ONLY),
                    SHELL_PACKAGE,
                    NotificationPackagePolicy.METADATA_ONLY,
                )
                store.saveRules(rules); store.setEnabled(true); Bundle().apply { putBoolean("ok", true) }
            }
            "approve_metadata_package" -> {
                val packageName = arg ?: return Bundle().apply { putBoolean("ok", false) }
                val rule = NotificationPolicy.defaultRule(packageName) ?: return Bundle().apply { putBoolean("ok", false) }
                store.saveRules(NotificationPolicy.update(store.rules(), rule.packageName, NotificationPackagePolicy.METADATA_ONLY))
                store.setEnabled(true)
                Bundle().apply { putBoolean("ok", true) }
            }
            "status" -> Bundle().apply {
                putInt("notificationMetadataCount", database.notificationMetadataDao().count())
                putInt("postedCount", database.notificationMetadataDao().countWithKind("notification.posted"))
                putInt("updatedCount", database.notificationMetadataDao().countWithKind("notification.updated"))
                putInt("removedCount", database.notificationMetadataDao().countWithKind("notification.removed"))
                putInt("pendingSyncCount", database.localEventDao().countWithState(SyncState.PENDING))
                putInt("acknowledgedNotificationCount", database.localEventDao().notificationCountWithState(SyncState.ACKNOWLEDGED))
                putBoolean("notificationEnabled", store.isEnabled())
                putString("syntheticPolicy", NotificationPolicy.policyFor(store.rules(), SYNTHETIC_PACKAGE).name)
                putBoolean("listenerAccessAllowed", NotificationAccess.isAllowed(appContext))
                putString("listenerState", NotificationListenerHealthStore(appContext).listenerState())
            }
            "post_synthetic" -> {
                val manager = appContext.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel("roxy_debug", "Roxy debug", NotificationManager.IMPORTANCE_LOW))
                manager.notify(404, android.app.Notification.Builder(appContext, "roxy_debug").setSmallIcon(com.roxy.app.R.drawable.ic_roxy).setWhen(System.currentTimeMillis()).setShowWhen(false).setContentTitle("Synthetic").setContentText("Synthetic").build())
                Bundle().apply { putBoolean("ok", true) }
            }
            "remove_synthetic" -> {
                appContext.getSystemService(NotificationManager::class.java).cancel(404)
                Bundle().apply { putBoolean("ok", true) }
            }
            "record_synthetic_lifecycle" -> {
                val digest = "0".repeat(64)
                val rows = listOf(
                    NotificationMetadataEntity("debug-lifecycle-posted", "notification.posted", 100, 100, "UTC", SHELL_PACKAGE, digest, createdAtEpochMillis = 100),
                    NotificationMetadataEntity("debug-lifecycle-updated", "notification.posted", 100, 200, "UTC", SHELL_PACKAGE, digest, createdAtEpochMillis = 200),
                    NotificationMetadataEntity("debug-lifecycle-removed", "notification.removed", 100, 300, "UTC", SHELL_PACKAGE, digest, createdAtEpochMillis = 300),
                )
                val accepted = rows.count { database.notificationMetadataDao().insertLifecycle(it) != null }
                Bundle().apply { putInt("acceptedLifecycleCallbacks", accepted) }
            }
            "notification_summary_status" -> {
                val pairing = PairingStore(appContext).read()
                    ?: return Bundle().apply { putString("result", "unpaired") }
                when (val result = NotificationSummaryReader.read(pairing, LocalDate.now().toString())) {
                    is NotificationSummaryResult.Success -> Bundle().apply {
                        putString("result", "success")
                        putInt("count", result.count)
                        putInt("itemCount", result.items.size)
                    }
                    is NotificationSummaryResult.Error -> Bundle().apply { putString("result", result.code) }
                }
            }
            "notification_analytics_status" -> {
                val pairing = PairingStore(appContext).read()
                    ?: return Bundle().apply { putString("result", "unpaired") }
                when (val result = NotificationAnalyticsReader.read(pairing, LocalDate.now().toString(), arg ?: "day")) {
                    is NotificationAnalyticsResult.Success -> Bundle().apply {
                        putString("result", "success")
                        putInt("count", result.value.count)
                        putInt("postedCount", result.value.postedCount)
                        putInt("updatedCount", result.value.updatedCount)
                        putInt("removedCount", result.value.removedCount)
                        putInt("topSourceCount", result.value.topSourceCount)
                        putString("period", result.value.period)
                        putString("completeness", result.value.completeness.reason)
                    }
                    is NotificationAnalyticsResult.Error -> Bundle().apply { putString("result", result.code) }
                }
            }
            "collector_health" -> Bundle().apply {
                CollectorHealthReader.read(appContext, database).forEach { health ->
                    putString("${health.collector}State", health.state)
                    putInt("${health.collector}Pending", health.pendingCount)
                    putString("${health.collector}Failure", health.failureCode)
                }
            }
            "reset_synthetic_policy" -> {
                store.saveRules(store.rules().filterNot { it.packageName == SYNTHETIC_PACKAGE || it.packageName == SHELL_PACKAGE }); store.setEnabled(false)
                Bundle().apply { putInt("deletedSyntheticRows", database.notificationMetadataDao().deleteForPackage(SYNTHETIC_PACKAGE) + database.notificationMetadataDao().deleteForPackage(SHELL_PACKAGE)) }
            }
            else -> Bundle().apply { putBoolean("ok", false) }
        }
    }
    override fun getType(uri: Uri): String? = null
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    private companion object { const val SYNTHETIC_PACKAGE = "com.roxy.app"; const val SHELL_PACKAGE = "com.android.shell" }
}
