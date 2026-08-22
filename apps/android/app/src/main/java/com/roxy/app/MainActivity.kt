package com.roxy.app

import android.os.Bundle
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.roxy.app.timeline.TimelineShell
import com.roxy.app.timeline.TimelineReader
import com.roxy.app.timeline.TimelineReadResult
import com.roxy.app.timeline.UsageSummaryReader
import com.roxy.app.timeline.UsageSummaryResult
import com.roxy.app.notifications.NotificationCollectorStatus
import com.roxy.app.notifications.NotificationPackagePolicy
import com.roxy.app.notifications.NotificationPolicy
import com.roxy.app.notifications.NotificationPolicyStore
import com.roxy.app.notifications.NotificationAccess
import com.roxy.app.notifications.NotificationListenerHealthStore
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
    var pairingStatus by remember { mutableStateOf(if (pairingStore?.read() == null) "Connect Roxy before sending totals" else "Connected; ready to sync when needed") }
    var usageAccessAllowed by remember { mutableStateOf(context?.let(UsageAccess::isAllowed) ?: false) }
    var usageQueryStatus by remember { mutableStateOf("No app-usage query has run") }
    var aggregationStatus by remember { mutableStateOf("No app-usage aggregation has run") }
    var exportStatus by remember { mutableStateOf("No app-usage buckets are queued") }
    var timelineState by remember { mutableStateOf(TimelineShell.initial()) }
    var timelineReadStatus by remember { mutableStateOf("Timeline has not been read") }
    var todaySummary by remember { mutableStateOf<UsageSummaryResult?>(null) }
    var todaySummaryStatus by remember { mutableStateOf("Refresh to view this date's aggregate activity.") }
    val notificationStore = remember(context) { context?.let(::NotificationPolicyStore) }
    var notificationsEnabled by remember { mutableStateOf(notificationStore?.isEnabled() ?: false) }
    var notificationRules by remember { mutableStateOf(notificationStore?.rules().orEmpty()) }
    var notificationPackageInput by remember { mutableStateOf("") }
    var notificationPolicyStatus by remember { mutableStateOf("") }
    var notificationAccessAllowed by remember { mutableStateOf(context?.let(NotificationAccess::isAllowed) ?: false) }
    var notificationAccessWasAllowed by remember { mutableStateOf(context?.let(::NotificationListenerHealthStore)?.wasAccessGranted() ?: false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refreshUsageAccess() {
        usageAccessAllowed = context?.let(UsageAccess::isAllowed) ?: false
    }

    fun refreshNotificationAccess() {
        notificationAccessAllowed = context?.let(NotificationAccess::isAllowed) ?: false
        val health = context?.let(::NotificationListenerHealthStore)
        if (notificationAccessAllowed) health?.markAccessGranted()
        notificationAccessWasAllowed = health?.wasAccessGranted() ?: false
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshUsageAccess()
                refreshNotificationAccess()
            }
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
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Roxy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Private timeline", style = MaterialTheme.typography.titleMedium)
                Text("Roxy prepares aggregate app-duration totals on this phone. It never reads app content, notifications, keystrokes, microphone, or camera data.")
                Section("Today", TimelineShell.detail(timelineState.availability)) {
                    Text("Selected local date: ${timelineState.selectedDate}")
                    Text("Data status: incomplete until Roxy reads verified aggregate data.", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { timelineState = TimelineShell.previousDay(timelineState) }) { Text("Previous day") }
                        Button(enabled = TimelineShell.canMoveToNextDay(timelineState), onClick = { timelineState = TimelineShell.nextDay(timelineState) }) { Text("Next day") }
                    }
                    Button(onClick = {
                        val pairing = pairingStore?.read() ?: run { timelineReadStatus = "Connect Roxy before reading the timeline"; return@Button }
                        executor.execute {
                            val result = TimelineReader.read(pairing, timelineState.selectedDate.toString())
                            val status = when (result) {
                                is TimelineReadResult.Success -> "Read ${result.itemCount} aggregate timeline entries; coverage is ${result.incompleteReason}"
                                is TimelineReadResult.Error -> "Timeline read could not complete: ${result.code}"
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).post { timelineReadStatus = status }
                        }
                    }) { Text("Read timeline") }
                    Text(timelineReadStatus)
                    Button(onClick = {
                        val pairing = pairingStore?.read() ?: run {
                            todaySummary = UsageSummaryResult.Error("Connect Roxy before refreshing Today")
                            todaySummaryStatus = "Connect Roxy before refreshing Today."
                            return@Button
                        }
                        todaySummary = null
                        todaySummaryStatus = "Refreshing Today…"
                        executor.execute {
                            val result = UsageSummaryReader.read(pairing, timelineState.selectedDate.toString())
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                todaySummary = result
                                todaySummaryStatus = when (result) {
                                    is UsageSummaryResult.Success -> "Today refreshed with aggregate data."
                                    is UsageSummaryResult.Error -> "Today could not be refreshed."
                                }
                            }
                        }
                    }) { Text("Refresh Today") }
                    Text(todaySummaryStatus)
                    when (val summary = todaySummary) {
                        is UsageSummaryResult.Success -> {
                            Text("${summary.totalMillis / 60_000} min", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Recorded activity", style = MaterialTheme.typography.labelLarge)
                            Text("Top apps on this phone", fontWeight = FontWeight.SemiBold)
                            summary.apps.forEach { app ->
                                val label = context?.let { UsageSummaryReader.resolveLabel(it, app.id) }
                                    ?: com.roxy.app.timeline.TodayAppLabel(app.id, resolvedLocally = false)
                                Text("${label.text} · ${app.durationMillis / 60_000} min", fontWeight = FontWeight.Medium)
                                if (!label.resolvedLocally) {
                                    Text("No local app label is available; showing its identifier.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text("Evidence is available through the timeline. Data status: ${summary.reason}.")
                        }
                        is UsageSummaryResult.Error -> Text("Today summary could not load: ${summary.code}")
                        null -> Text("Refresh to view exact aggregate totals for this date.")
                    }
                }
                Section("1. Permission", if (usageAccessAllowed) "App usage access is ready." else "Allow app usage access to continue.") {
                    Button(onClick = {
                    activity?.startActivity(UsageAccess.settingsIntent())
                    }) { Text("Open usage access settings") }
                    Button(onClick = { refreshUsageAccess() }) { Text("Refresh permission") }
                }
                val notificationStatus = NotificationPolicy.collectorStatus(
                    enabled = notificationsEnabled,
                    listenerInstalled = true,
                    accessGranted = notificationAccessAllowed,
                    wasAccessGranted = notificationAccessWasAllowed,
                )
                Section("Notifications", NotificationPolicy.statusDetail(notificationStatus)) {
                    Text("When available, notification metadata can help complete your private timeline. This setup never reads or stores notification content.")
                    Text("If enabled later, Roxy will use metadata only by default. It will never keep notification text, actions, images, tokens, or remote-view data in this setup step.")
                    Text("Authenticators, password managers, banking apps, and any package you add start blocked.", fontWeight = FontWeight.SemiBold)
                    Text("You can revoke Notification Access later in Android Settings under Notifications and Notification access. Turning this control off immediately keeps Roxy from collecting notifications.")
                    Text("Redacted text is off by default. Enable it only for a package you choose after reviewing that Roxy removes likely sensitive strings, retains only the redacted result for seven days, and never uploads it.")
                    Button(onClick = {
                        notificationsEnabled = !notificationsEnabled
                        notificationStore?.setEnabled(notificationsEnabled)
                        notificationPolicyStatus = if (notificationsEnabled) "Notification preparation is on locally; no notifications are being collected." else "Notifications are off; no notifications are being collected."
                    }) { Text(if (notificationsEnabled) "Turn notifications off" else "Enable notification preparation") }
                    Button(onClick = { activity?.startActivity(NotificationAccess.settingsIntent()) }) { Text("Open Notification Access settings") }
                    Button(onClick = { refreshNotificationAccess() }) { Text("Refresh Notification Access") }
                    OutlinedTextField(
                        value = notificationPackageInput,
                        onValueChange = { notificationPackageInput = it },
                        label = { Text("Package identifier to block") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Button(onClick = {
                        val rule = NotificationPolicy.defaultRule(notificationPackageInput)
                        if (rule == null) {
                            notificationPolicyStatus = "Enter a valid package identifier to add a blocked policy."
                        } else {
                            notificationRules = NotificationPolicy.update(notificationRules, rule.packageName, NotificationPackagePolicy.BLOCKED)
                            notificationStore?.saveRules(notificationRules)
                            notificationPackageInput = ""
                            notificationPolicyStatus = "The package is blocked locally."
                        }
                    }) { Text("Add blocked package") }
                    notificationRules.forEach { rule ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(rule.packageName, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                val next = if (rule.policy == NotificationPackagePolicy.BLOCKED) NotificationPackagePolicy.METADATA_ONLY else NotificationPackagePolicy.BLOCKED
                                notificationRules = NotificationPolicy.update(notificationRules, rule.packageName, next)
                                notificationStore?.saveRules(notificationRules)
                                notificationPolicyStatus = if (next == NotificationPackagePolicy.BLOCKED) "The package is blocked locally." else "The package is metadata-only locally; text remains disabled."
                            }) { Text(if (rule.policy == NotificationPackagePolicy.BLOCKED) "Blocked" else "Block") }
                            if (rule.policy != NotificationPackagePolicy.TEXT_REDACTED) {
                                Button(onClick = {
                                    notificationRules = NotificationPolicy.update(notificationRules, rule.packageName, NotificationPackagePolicy.TEXT_REDACTED)
                                    notificationStore?.saveRules(notificationRules)
                                    notificationPolicyStatus = "Redacted text is enabled only for this package. Roxy removes sensitive strings and deletes retained text after seven days; it never uploads it."
                                }) { Text("Enable redacted text") }
                            } else {
                                Button(onClick = {
                                    notificationRules = NotificationPolicy.update(notificationRules, rule.packageName, NotificationPackagePolicy.METADATA_ONLY)
                                    notificationStore?.saveRules(notificationRules)
                                    notificationPolicyStatus = "Redacted text is off for this package; metadata-only remains."
                                }) { Text("Text off") }
                            }
                        }
                    }
                    if (notificationRules.isEmpty()) Text("No package rules have been added. No installed apps are listed or inspected.")
                    if (notificationPolicyStatus.isNotBlank()) Text(notificationPolicyStatus)
                }
                Section("2. Prepare local totals", "Read the previous 24 hours, then make 15-minute totals on this phone.") {
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
                    ) { Text("Read previous 24 hours") }
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
                    }) { Text("Create local 15-minute totals") }
                    Text(aggregationStatus)
                }
                Section("3. Send aggregate totals", "Only aggregate duration buckets can be queued for sync.") {
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
                    }) { Text("Queue aggregate totals") }
                    Text(exportStatus)
                    Text("Pending uploads: $pendingCount")
                    Text(queueHealth)
                    Button(onClick = { refreshPendingCount() }) { Text("Refresh upload status") }
                    Button(onClick = {
                        if (pairingStore?.read() != null) {
                            SyncScheduler.enqueue(context ?: return@Button)
                            pairingStatus = "Sync scheduled; it will run when network is available"
                        } else {
                            pairingStatus = "Connect Roxy before syncing totals"
                        }
                    }) { Text("Sync now") }
                }
                Section("4. Connection", "Pair once with your Roxy server. The credential is encrypted on this phone.") {
                    OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Roxy server address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = credential, onValueChange = { credential = it }, label = { Text("Pairing credential") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                    if (pairingStore == null) return@Button
                    runCatching { pairingStore.save(endpoint, credential); SyncScheduler.enqueue(context ?: return@Button) }
                        .onSuccess { pairingStatus = "Paired; sync is scheduled only when network is available" }
                        .onFailure { pairingStatus = "Pairing needs an http(s) endpoint and a 32+ character credential" }
                    }) { Text("Save connection") }
                    Text(pairingStatus)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, detail: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
            content()
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
