package com.roxy.app.health

import android.content.Context
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.SyncState
import com.roxy.app.notifications.NotificationAccess
import com.roxy.app.notifications.NotificationListenerHealthStore
import com.roxy.app.notifications.NotificationPolicyStore
import com.roxy.app.sync.PairingStore
import com.roxy.app.usage.UsageAccess

data class CollectorHealth(
    val collector: String,
    val state: String,
    val lastObservationEpochMillis: Long?,
    val pendingCount: Int,
    val lastSyncEpochMillis: Long?,
    val failureCode: String?,
)

object CollectorHealthReader {
    internal fun usageState(hasAccess: Boolean, paired: Boolean, lastObservation: Long?): String = when {
        !hasAccess -> "needs_usage_access"
        !paired -> "needs_pairing"
        lastObservation == null -> "waiting_for_observation"
        else -> "collecting_automatically"
    }

    internal fun notificationState(enabled: Boolean, hasAccess: Boolean, listenerState: String): String = when {
        !enabled -> "paused_by_owner"
        !hasAccess -> "needs_notification_access"
        listenerState != "connected" -> "listener_unavailable"
        else -> "collecting_metadata"
    }

    fun read(context: Context, database: RoxyDatabase): List<CollectorHealth> {
        val sync = database.syncHealthDao().read()
        val pending = database.localEventDao().countWithState(SyncState.PENDING)
        val usageDao = database.usageCollectionDao()
        val paired = PairingStore(context).read() != null
        return listOf(
            CollectorHealth("app_usage", usageState(UsageAccess.isAllowed(context), paired, usageDao.latestObservationEpochMillis()), usageDao.latestObservationEpochMillis(), pending, sync?.lastSuccessEpochMillis, sync?.lastErrorCode),
            CollectorHealth("notifications", notificationState(NotificationPolicyStore(context).isEnabled(), NotificationAccess.isAllowed(context), NotificationListenerHealthStore(context).listenerState()), null, pending, sync?.lastSuccessEpochMillis, sync?.lastErrorCode),
        )
    }
}
