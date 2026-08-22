package com.roxy.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.roxy.app.data.NotificationMetadataEntity
import com.roxy.app.data.NotificationRedactedTextEntity
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.sync.PairingStore
import com.roxy.app.sync.SyncScheduler
import java.security.SecureRandom
import java.time.ZoneId
import java.util.concurrent.Executors

class RoxyNotificationListener : NotificationListenerService() {
    private val database by lazy { RoxyDatabase.create(applicationContext) }
    private val policyStore by lazy { NotificationPolicyStore(applicationContext) }
    private val healthStore by lazy { NotificationListenerHealthStore(applicationContext) }
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    override fun onListenerConnected() {
        super.onListenerConnected()
        healthStore.markAccessGranted()
        healthStore.markConnected()
    }

    override fun onListenerDisconnected() {
        healthStore.markDisconnected()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = enqueue(sbn, "notification.posted")

    override fun onNotificationRemoved(sbn: StatusBarNotification) = enqueue(sbn, "notification.removed")

    override fun onDestroy() {
        callbackExecutor.shutdown()
        super.onDestroy()
    }

    private fun enqueue(sbn: StatusBarNotification, eventKind: String) {
        callbackExecutor.execute { record(sbn, eventKind) }
    }

    private fun record(sbn: StatusBarNotification, eventKind: String) {
        val now = System.currentTimeMillis()
        val event = NotificationEventGate.create(
            enabled = policyStore.isEnabled(),
            rules = policyStore.rules(),
            packageName = sbn.packageName,
            eventKind = eventKind,
            occurredAtEpochMillis = sbn.postTime,
            recordedAtEpochMillis = now,
            observedTimezone = ZoneId.systemDefault(),
            eventId = newUuidV7(now),
            notificationKey = sbn.key,
            isGroupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        ) ?: return
        val stored = database.notificationMetadataDao().insertLifecycle(
            NotificationMetadataEntity(
                id = event.id,
                eventKind = event.eventKind,
                occurredAtEpochMillis = event.occurredAtEpochMillis,
                recordedAtEpochMillis = event.recordedAtEpochMillis,
                observedTimezone = event.observedTimezone,
                packageName = event.packageName,
                identityDigest = event.identityDigest,
                createdAtEpochMillis = now,
            ),
        )
        if (stored != null) {
            val storedEvent = event.copy(eventKind = stored.eventKind)
            PairingStore(applicationContext).read()?.let { pairing ->
                NotificationMetadataExporter.queue(database, pairing, storedEvent)
                SyncScheduler.enqueue(applicationContext)
            }
        }
        if (stored != null && NotificationPolicy.policyFor(policyStore.rules(), sbn.packageName) == NotificationPackagePolicy.TEXT_REDACTED) {
            val sanitized = NotificationSanitizer.sanitize(FutureNotificationContent(
                title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                body = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                hasActions = !sbn.notification.actions.isNullOrEmpty(),
                hasImage = sbn.notification.largeIcon != null,
                hasRemoteView = sbn.notification.contentView != null,
            )) ?: return
            database.notificationRedactedTextDao().deleteExpired(now)
            database.notificationRedactedTextDao().insert(NotificationRedactedTextEntity(stored.id, stored.identityDigest ?: event.identityDigest, stored.packageName, stored.occurredAtEpochMillis, sanitized.title, sanitized.body, sanitized.redactionCount, NotificationTextRetention.expiresAt(now)))
        }
    }

    private fun newUuidV7(now: Long): String {
        val random = ByteArray(10).also(SecureRandom()::nextBytes)
        val timestamp = now.toString(16).padStart(12, '0')
        val randomHex = random.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        val variant = ((random[0].toInt() and 0x03) or 0x08).toString(16)
        return "${timestamp.substring(0, 8)}-${timestamp.substring(8, 12)}-7${randomHex.substring(0, 3)}-" +
            "$variant${randomHex.substring(3, 6)}-${randomHex.substring(6, 18)}"
    }

}
