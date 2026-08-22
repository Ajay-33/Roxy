package com.roxy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roxy.app.data.RoxyDatabase
import com.roxy.app.health.*
import com.roxy.app.notifications.*
import com.roxy.app.sync.*
import com.roxy.app.timeline.*
import com.roxy.app.usage.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() { override fun onCreate(s: Bundle?) { super.onCreate(s); UsageCollectionScheduler.reconcile(applicationContext); setContent { RoxyApp(RoxyDatabase.create(applicationContext), PairingStore(applicationContext), this) } } }
private enum class Tab(val label: String) { HOME("Home"), ACTIVITY("Activity"), ALERTS("Alerts"), HEALTH("Health"), SETTINGS("Settings") }

@Composable private fun RoxyApp(db: RoxyDatabase, pairing: PairingStore, activity: ComponentActivity) {
    var tab by remember { mutableStateOf(Tab.HOME) }; var health by remember { mutableStateOf<List<CollectorHealth>>(emptyList()) }; var summary by remember { mutableStateOf<UsageSummaryResult?>(null) }; var summaryStatus by remember { mutableStateOf("Refreshing your private dashboard…") }
    var endpoint by remember { mutableStateOf(pairing.read()?.endpoint ?: "") }; var credential by remember { mutableStateOf("") }; var connection by remember { mutableStateOf(if (pairing.read()==null) "Not connected" else "Connected") }; var notifications by remember { mutableStateOf(NotificationPolicyStore(activity).isEnabled()) }
    val work = remember { Executors.newSingleThreadExecutor() }
    fun refreshHealth() = work.execute { val x=CollectorHealthReader.read(activity,db); android.os.Handler(android.os.Looper.getMainLooper()).post { health=x } }
    fun refreshSummary() { val p=pairing.read() ?: run { summaryStatus="Connect Roxy in Settings to load activity."; return }; work.execute { val x=UsageSummaryReader.read(p,java.time.LocalDate.now().toString()); android.os.Handler(android.os.Looper.getMainLooper()).post { summary=x; summaryStatus=if(x is UsageSummaryResult.Success) "Updated from your private Roxy service." else "Activity is temporarily unavailable." } } }
    LaunchedEffect(Unit) { refreshHealth(); refreshSummary() }
    MaterialTheme(colorScheme=darkColorScheme(primary=androidx.compose.ui.graphics.Color(0xFFAA9CFF),secondary=androidx.compose.ui.graphics.Color(0xFF51E4C0),surface=androidx.compose.ui.graphics.Color(0xFF15131F))) { Scaffold(bottomBar={ NavigationBar { Tab.entries.forEach { t -> NavigationBarItem(selected=tab==t,onClick={tab=t;if(t==Tab.HEALTH)refreshHealth()},icon={Text(if(tab==t)"●" else "○")},label={Text(t.label)}) } } }) { pad -> Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) { Text("ROXY",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black); Text("PRIVATE CONTEXT · LIVE",color=MaterialTheme.colorScheme.secondary)
        when(tab) { Tab.HOME -> Home(summary,summaryStatus,health,::refreshSummary,::refreshHealth,activity); Tab.ACTIVITY -> Activity(summary,summaryStatus,::refreshSummary,activity); Tab.ALERTS -> Alerts(health.firstOrNull{it.collector=="notifications"},notifications,{notifications=it;NotificationPolicyStore(activity).setEnabled(it);refreshHealth()},activity); Tab.HEALTH -> Health(health,::refreshHealth); Tab.SETTINGS -> Settings(endpoint,{endpoint=it},credential,{credential=it},connection,{runCatching{pairing.save(endpoint,credential);UsageCollectionScheduler.reconcile(activity);SyncScheduler.enqueue(activity)}.onSuccess{connection="Connected. Automatic collection is active.";refreshHealth()}.onFailure{connection="Enter a valid server address and pairing credential."}},activity) }
    } } }
}
@Composable private fun Home(s:UsageSummaryResult?,status:String,h:List<CollectorHealth>,refresh:()->Unit,health:()->Unit,a:ComponentActivity){ Box("Today","Real activity and system context, refreshed only when you ask."){Button(onClick=refresh){Text("Refresh dashboard")};Text(status);if(s is UsageSummaryResult.Success){Text("${s.totalMillis/60_000} min",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Text("Recorded activity today");s.apps.take(5).forEach{ x->Text("${UsageSummaryReader.resolveLabel(a,x.id).text}  ·  ${x.durationMillis/60_000} min")}}};Box("System pulse","Your collectors run automatically in the background."){Button(onClick=health){Text("Refresh health")};h.forEach{Text("${it.collector.replace('_',' ')} · ${it.state.replace('_',' ')}")}}}
@Composable private fun Activity(s:UsageSummaryResult?,status:String,refresh:()->Unit,a:ComponentActivity){Box("Activity analysis","Aggregate usage only — never app content."){Button(onClick=refresh){Text("Refresh activity")};Text(status);if(s is UsageSummaryResult.Success)s.apps.forEach{x->Text("${UsageSummaryReader.resolveLabel(a,x.id).text} — ${x.durationMillis/60_000} min",fontWeight=FontWeight.SemiBold)}else Text("No synced activity is available yet.")}}
@Composable private fun Alerts(h:CollectorHealth?,enabled:Boolean,set:(Boolean)->Unit,a:ComponentActivity){Box("Notification context","Metadata only. Roxy never shows notification title or message text here."){Text("Status: ${h?.state?.replace('_',' ') ?: "checking"}");Button(onClick={set(!enabled)}){Text(if(enabled)"Pause notification metadata" else "Enable notification metadata")};Button(onClick={a.startActivity(NotificationAccess.settingsIntent())}){Text("Manage notification access")};Text("Only packages you explicitly allow can contribute metadata.")}}
@Composable private fun Health(h:List<CollectorHealth>,refresh:()->Unit){Box("Collector health","Safe state, queue, and failure information — no personal activity details."){Button(onClick=refresh){Text("Refresh health")};h.forEach{x->HorizontalDivider();Text(x.collector.replace('_',' '),fontWeight=FontWeight.Bold);Text("${x.state.replace('_',' ')} · ${x.pendingCount} waiting to sync");Text(x.failureCode ?: "No current failure")}}}
@Composable private fun Settings(endpoint:String,onEndpoint:(String)->Unit,credential:String,onCredential:(String)->Unit,status:String,connect:()->Unit,a:ComponentActivity){Box("Connection","One-time setup for your private Roxy service."){OutlinedTextField(endpoint,onEndpoint,label={Text("Roxy server address")},modifier=Modifier.fillMaxWidth());OutlinedTextField(credential,onCredential,label={Text("Pairing credential")},modifier=Modifier.fillMaxWidth());Button(onClick=connect){Text("Connect Roxy")};Text(status)};Box("Permissions","You remain in control of each collector."){Button(onClick={a.startActivity(UsageAccess.settingsIntent())}){Text("Manage usage access")};Button(onClick={a.startActivity(NotificationAccess.settingsIntent())}){Text("Manage notification access")};Text("Technical diagnostics are not part of the everyday dashboard.",style=MaterialTheme.typography.bodySmall)}}
@Composable private fun Box(title:String,detail:String,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(detail);content()}}}
