package com.roxy.app.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.UsageCollectionCursor
import com.roxy.app.data.UsageObservationEntity
import java.security.MessageDigest

data class UsageCollectionWindow(val startEpochMillis: Long, val endEpochMillis: Long)
data class UsageCollectionResult(val observed: Int, val inserted: Int, val totalStored: Int)

object UsageCollector {
    const val OVERLAP_MILLIS = 15 * 60 * 1000L
    const val MAX_WINDOW_MILLIS = 24 * 60 * 60 * 1000L

    fun window(cursor: Long?, now: Long): UsageCollectionWindow {
        val start = (cursor?.minus(OVERLAP_MILLIS) ?: now - MAX_WINDOW_MILLIS).coerceAtLeast(now - MAX_WINDOW_MILLIS)
        return UsageCollectionWindow(start, now)
    }

    @Suppress("DEPRECATION")
    fun collect(context: Context, database: RoxyDatabase, now: Long = System.currentTimeMillis()): UsageCollectionResult? {
        if (!UsageAccess.isAllowed(context)) return null
        val dao = database.usageCollectionDao()
        val range = window(dao.cursor()?.collectedUntilEpochMillis, now)
        val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(range.startEpochMillis, range.endEpochMillis)
        val rows = mutableListOf<UsageObservationEntity>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> "foreground"
                UsageEvents.Event.MOVE_TO_BACKGROUND -> "background"
                else -> null
            } ?: continue
            val packageName = event.packageName ?: continue
            val occurredAt = event.timeStamp
            rows += UsageObservationEntity(observationId(packageName, type, occurredAt), packageName, type, occurredAt, now)
        }
        return database.runInTransaction<UsageCollectionResult> {
            val inserted = dao.insertObservations(rows).count { it != -1L }
            dao.saveCursor(UsageCollectionCursor(collectedUntilEpochMillis = range.endEpochMillis))
            UsageCollectionResult(rows.size, inserted, dao.observationCount())
        }
    }

    fun observationId(packageName: String, eventType: String, occurredAtEpochMillis: Long): String =
        MessageDigest.getInstance("SHA-256").digest("$packageName|$eventType|$occurredAtEpochMillis".toByteArray())
            .joinToString("") { "%02x".format(it) }
}
