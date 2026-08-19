package com.roxy.app

import android.os.Bundle
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roxy.app.data.LocalEventEntity
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.data.SyncState
import com.roxy.app.sync.PairingStore
import com.roxy.app.sync.SyncScheduler
import com.roxy.app.usage.UsageAccess
import com.roxy.app.usage.UsageQueryResult
import com.roxy.app.usage.UsageStatsReader
import com.roxy.app.usage.UsageCollector
import com.roxy.app.usage.UsageAggregation
import com.roxy.app.usage.UsageObservation
import com.roxy.app.data.UsageBucketEntity
import com.roxy.app.usage.UsageBucketExporter
import java.security.SecureRandom
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = RoxyDatabase.create(applicationContext)
        setContent { RoxyApp(database, PairingStore(applicationContext), applicationContext) }
    }
}

@Composable
private fun RoxyApp(database: RoxyDatabase? = null, pairingStore: PairingStore? = null, context: Context? = null) {
    var pendingCount by remember { mutableIntStateOf(0) }
    var queueHealth by remember { mutableStateOf("Queue health has not been checked") }
    var endpoint by remember { mutableStateOf(pairingStore?.read()?.endpoint ?: "http://127.0.0.1:4100") }
    var credential by remember { mutableStateOf("") }
    var pairingStatus by remember { mutableStateOf(if (pairingStore?.read() == null) "Pairing required before sync" else "Paired for local development") }
    var usageAccessAllowed by remember { mutableStateOf(context?.let(UsageAccess::isAllowed) ?: false) }
    var usageQueryStatus by remember { mutableStateOf("No app-usage query has run") }
    var aggregationStatus by remember { mutableStateOf("No app-usage aggregation has run") }
    var exportStatus by remember { mutableStateOf("No app-usage buckets are queued") }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refreshUsageAccess() {
        usageAccessAllowed = context?.let(UsageAccess::isAllowed) ?: false
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshUsageAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun refreshPendingCount() {
        if (database == null) return
        executor.execute {
            val count = database.localEventDao().countWithState(SyncState.PENDING)
            val oldest = database.localEventDao().oldestRecordedAt(SyncState.PENDING)
            val sync = database.syncHealthDao().read()
            val oldestAge = oldest?.let { ((System.currentTimeMillis() - it) / 60_000).coerceAtLeast(0) }
            val summary = "Queue health: oldest pending ${oldestAge?.let { "$it min" } ?: "none"}; last success ${sync?.lastSuccessEpochMillis?.let { "recorded" } ?: "none"}; last error ${sync?.lastErrorCode ?: "none"}"
            android.os.Handler(android.os.Looper.getMainLooper()).post { pendingCount = count; queueHealth = summary }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            ) {
                Text(text = "Roxy")
                Text(text = "Foundation check")
                Text(text = "No collection is active")
                Text(text = "App usage access: ${if (usageAccessAllowed) "allowed" else "not allowed"}")
                Text(text = if (usageAccessAllowed) {
                    "Usage Access is allowed, but Roxy does not collect app usage yet."
                } else {
                    "App-usage collection is off. Allow Usage Access in Android settings before it can be enabled."
                })
                Text(text = "When enabled in a later step, Roxy will use it only for on-device app-duration totals. It does not grant access to app content, notifications, keystrokes, the microphone, or camera.")
                Button(onClick = {
                    activity?.startActivity(UsageAccess.settingsIntent())
                }) { Text("Open Usage Access settings") }
                Button(onClick = { refreshUsageAccess() }) { Text("Check app usage access") }
                Button(
                    enabled = usageAccessAllowed,
                    onClick = {
                        val queryContext = context ?: return@Button
                        executor.execute {
                            val count = database?.let { UsageCollector.collect(queryContext, it) }
                            val status = count?.let { "Observed ${it.observed}; stored ${it.inserted} new usage observations (${it.totalStored} total); nothing was synced" }
                                ?: "App-usage query was not run because access is not allowed"
                            android.os.Handler(android.os.Looper.getMainLooper()).post { usageQueryStatus = status }
                        }
                    },
                ) { Text("Collect previous 24 hours") }
                Text(usageQueryStatus)
                Button(onClick = {
                    val localDatabase = database ?: return@Button
                    executor.execute {
                        val dao = localDatabase.usageCollectionDao(); val buckets = UsageAggregation.aggregate(dao.observations().map {
                            UsageObservation(it.packageName, it.eventType, it.occurredAtEpochMillis)
                        })
                        localDatabase.runInTransaction { dao.clearBuckets(); dao.saveBuckets(buckets.map { UsageBucketEntity(it.packageName, it.bucketStart, it.durationMillis) }) }
                        val bucketCount = dao.bucketCount()
                        android.os.Handler(android.os.Looper.getMainLooper()).post { aggregationStatus = "Stored $bucketCount local 15-minute app-usage buckets; nothing was synced" }
                    }
                }) { Text("Calculate 15-minute app usage") }
                Text(aggregationStatus)
                Button(onClick = {
                    val localDatabase = database ?: return@Button
                    val pairing = pairingStore?.read() ?: run {
                        exportStatus = "Pair Roxy before queueing aggregate app usage"
                        return@Button
                    }
                    executor.execute {
                        val queued = UsageBucketExporter.queue(localDatabase, pairing.deviceId)
                        refreshPendingCount()
                        android.os.Handler(android.os.Looper.getMainLooper()).post { exportStatus = "Queued $queued aggregate app-usage buckets; raw observations remain on this phone" }
                    }
                }) { Text("Queue aggregate app usage") }
                Text(exportStatus)
                Text(text = "Pending synthetic events: $pendingCount")
                Text(text = queueHealth)
                Button(
                    onClick = {
                        if (database == null) return@Button
                        executor.execute {
                            database.localEventDao().insert(
                                LocalEventEntity(
                                    id = newUuidV7(), schemaVersion = 1, deviceId = "local-debug-device",
                                    eventType = "system.test_event", occurredAtEpochMillis = System.currentTimeMillis(),
                                    recordedAtEpochMillis = System.currentTimeMillis(), observedTimezone = "Asia/Calcutta",
                                    source = "android.manual_test", sensitivity = "private", payloadJson = "{\"synthetic\":true}",
                                    confidence = 1.0, isDerived = false, syncState = SyncState.PENDING,
                                    rejectionCode = null, createdAtEpochMillis = System.currentTimeMillis(),
                                ),
                            )
                            refreshPendingCount()
                        }
                    },
                ) { Text("Create synthetic test event") }
                Button(onClick = { refreshPendingCount() }) { Text("Refresh queue") }
                OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Development API endpoint") })
                OutlinedTextField(value = credential, onValueChange = { credential = it }, label = { Text("One-time device credential") })
                Button(onClick = {
                    if (pairingStore == null) return@Button
                    runCatching { pairingStore.save(endpoint, credential); SyncScheduler.enqueue(context ?: return@Button) }
                        .onSuccess { pairingStatus = "Paired; sync is scheduled only when network is available" }
                        .onFailure { pairingStatus = "Pairing needs an http(s) endpoint and a 32+ character credential" }
                }) { Text("Save pairing and schedule sync") }
                Button(onClick = {
                    if (pairingStore?.read() != null) {
                        SyncScheduler.enqueue(context ?: return@Button)
                        pairingStatus = "Sync scheduled; it will run when network is available"
                    }
                }) { Text("Sync queued data") }
                Text(pairingStatus)
            }
        }
    }
}

private fun newUuidV7(): String {
    val random = ByteArray(10).also(SecureRandom()::nextBytes)
    val timestamp = System.currentTimeMillis().toString(16).padStart(12, '0')
    val randomHex = random.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    val variant = ((random[0].toInt() and 0x03) or 0x08).toString(16)
    return "${timestamp.substring(0, 8)}-${timestamp.substring(8, 12)}-7${randomHex.substring(0, 3)}-" +
        "$variant${randomHex.substring(3, 6)}-${randomHex.substring(6, 18)}"
}

@Preview(showBackground = true)
@Composable
private fun RoxyPreview() {
    RoxyApp()
}
