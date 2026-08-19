package com.roxy.app.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.SyncState
import com.roxy.app.data.SyncHealthEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val database = RoxyDatabase.create(applicationContext)
        val healthDao = database.syncHealthDao()
        val previousHealth = healthDao.read() ?: SyncHealthEntity()
        healthDao.save(previousHealth.copy(lastAttemptEpochMillis = System.currentTimeMillis(), lastErrorCode = null))
        val pairing = PairingStore(applicationContext).read() ?: run {
            Log.i("RoxySync", "No pairing; sync skipped")
            return Result.success()
        }
        val events = database.localEventDao().eventsWithState(SyncState.PENDING, 250)
        if (events.isEmpty()) {
            Log.i("RoxySync", "No pending events; sync skipped")
            return Result.success()
        }
        Log.i("RoxySync", "Submitting ${events.size} queued event(s)")
        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().put("events", JSONArray(events.map { event ->
                    JSONObject().put("id", event.id).put("schemaVersion", event.schemaVersion).put("deviceId", event.deviceId)
                        .put("type", event.eventType).put("occurredAt", java.time.Instant.ofEpochMilli(event.occurredAtEpochMillis).toString())
                        .put("recordedAt", java.time.Instant.ofEpochMilli(event.recordedAtEpochMillis).toString()).put("timezone", event.observedTimezone)
                        .put("source", event.source).put("sensitivity", event.sensitivity).put("payload", JSONObject(event.payloadJson))
                        .put("quality", JSONObject().put("confidence", event.confidence).put("isDerived", event.isDerived))
                })).toString()
                val connection = (URL("${pairing.endpoint}/v1/sync/events").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 10_000; doOutput = true
                    setRequestProperty("Authorization", "Bearer ${pairing.credential}"); setRequestProperty("Content-Type", "application/json")
                }
                connection.outputStream.bufferedWriter().use { it.write(body) }
                if (connection.responseCode !in 200..299) error("sync_http_${connection.responseCode}")
                val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val dao = database.localEventDao()
                val acknowledgements = response.optJSONArray("acknowledgements") ?: JSONArray()
                for (index in 0 until acknowledgements.length()) {
                    val acknowledgement = acknowledgements.getJSONObject(index)
                    dao.updateSyncState(acknowledgement.getString("id"), SyncState.ACKNOWLEDGED)
                }
                val rejected = response.optJSONArray("rejected") ?: JSONArray()
                for (index in 0 until rejected.length()) {
                    val rejection = rejected.getJSONObject(index)
                    events.getOrNull(rejection.getInt("index"))?.let { dao.updateSyncState(it.id, SyncState.REJECTED, rejection.getString("code")) }
                }
                val rejectedFields = (0 until rejected.length()).flatMap { index ->
                    val fields = rejected.getJSONObject(index).optJSONArray("fields") ?: JSONArray()
                    (0 until fields.length()).map { fields.getString(it) }
                }.distinct()
                Log.i("RoxySync", "Sync response acknowledged=${acknowledgements.length()} rejected=${rejected.length()} fields=$rejectedFields")
                healthDao.save((healthDao.read() ?: SyncHealthEntity()).copy(lastSuccessEpochMillis = System.currentTimeMillis(), lastErrorCode = null))
                if (dao.countWithState(SyncState.PENDING) > 0) {
                    Log.i("RoxySync", "More queued events remain; scheduling the next batch")
                    SyncScheduler.enqueue(applicationContext)
                }
            }.fold(onSuccess = { Result.success() }, onFailure = {
                healthDao.save((healthDao.read() ?: SyncHealthEntity()).copy(lastErrorCode = "transient_sync_failure"))
                Log.w("RoxySync", "Sync failed; WorkManager will retry")
                Result.retry()
            })
        }
    }
}
