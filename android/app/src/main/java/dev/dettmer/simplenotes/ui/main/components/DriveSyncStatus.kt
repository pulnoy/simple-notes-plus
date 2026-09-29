package dev.dettmer.simplenotes.ui.main.components

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.sync.SyncStateManager
import dev.dettmer.simplenotes.sync.drive.DriveSyncProblem
import dev.dettmer.simplenotes.sync.drive.DriveSyncRecord
import dev.dettmer.simplenotes.sync.drive.DriveSyncStatusStore
import dev.dettmer.simplenotes.sync.drive.DriveSyncWorker
import dev.dettmer.simplenotes.ui.settings.ComposeSettingsActivity
import dev.dettmer.simplenotes.ui.settings.SettingsRoute
import dev.dettmer.simplenotes.utils.Constants
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

private data class DriveConfiguration(val enabled: Boolean, val offline: Boolean, val account: String)
private data class DriveActivity(val running: Boolean, val queued: Boolean, val connected: Boolean, val offline: Boolean)

@Composable
fun DriveSyncStatus(fallback: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val configuration = rememberDriveConfiguration(context)
    if (!configuration.enabled) {
        fallback()
        return
    }
    val store = remember(context) { DriveSyncStatusStore(context) }
    val record by remember(configuration.account) { store.observe(configuration.account) }
        .collectAsState(store.read(configuration.account))
    val work by remember(context) {
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("google-drive-sync")
    }.collectAsState(emptyList())
    val sync by SyncStateManager.syncStatus.collectAsState()
    val connected by remember(context) { connectionFlow(context) }.collectAsState(true)
    val running = (sync.source?.startsWith("drive") == true &&
        sync.state in setOf(SyncStateManager.SyncState.SYNCING, SyncStateManager.SyncState.SYNCING_SILENT)) ||
        work.any { it.state == WorkInfo.State.RUNNING }
    val queued = work.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
    val activity = DriveActivity(running, queued, connected, configuration.offline)
    DriveStatusContent(statusMessage(activity, record), record.lastSuccess, hasError(activity, record)) {
        DriveRetryButton(record.problem, !running && !configuration.offline)
    }
}

@Composable
private fun statusMessage(activity: DriveActivity, record: DriveSyncRecord): String = when {
        activity.offline -> stringResource(R.string.drive_status_offline)
        activity.running -> stringResource(R.string.drive_status_running)
        !activity.connected -> stringResource(R.string.drive_status_waiting_connection)
        record.problem != null -> stringResource(problemMessage(record.problem))
        activity.queued -> stringResource(R.string.drive_status_queued)
        record.lastSuccess == 0L -> stringResource(R.string.drive_status_never)
        else -> stringResource(R.string.drive_status_current)
}

private fun hasError(activity: DriveActivity, record: DriveSyncRecord): Boolean =
    record.problem != null && !activity.running && activity.connected && !activity.offline

@Composable
private fun DriveRetryButton(problem: DriveSyncProblem?, enabled: Boolean) {
    val context = LocalContext.current
    TextButton(enabled = enabled, onClick = {
        if (problem == DriveSyncProblem.AUTHORIZATION) {
            context.startActivity(Intent(context, ComposeSettingsActivity::class.java).apply {
                putExtra(ComposeSettingsActivity.EXTRA_INITIAL_ROUTE, SettingsRoute.Sync.route)
            })
        } else {
            DriveSyncWorker.enqueue(context)
        }
    }) {
        Text(stringResource(if (problem == DriveSyncProblem.AUTHORIZATION) {
            R.string.drive_status_reconnect
        } else {
            R.string.drive_status_retry
        }))
    }
}

@Composable
private fun DriveStatusContent(message: String, lastSuccess: Long, error: Boolean, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.drive_status_label, message),
                style = MaterialTheme.typography.labelMedium,
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (lastSuccess > 0L) {
                Text(
                    stringResource(
                        R.string.drive_status_last_success,
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSuccess))
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        action()
    }
}

private fun problemMessage(problem: DriveSyncProblem?): Int = when (problem) {
    DriveSyncProblem.AUTHORIZATION -> R.string.drive_status_authorization_error
    DriveSyncProblem.DATA -> R.string.drive_status_data_error
    DriveSyncProblem.CONNECTION -> R.string.drive_status_connection_error
    else -> R.string.drive_status_other_error
}

@Composable
private fun rememberDriveConfiguration(context: Context): DriveConfiguration {
    val prefs = remember(context) { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }
    fun read() = DriveConfiguration(
        prefs.getBoolean(Constants.KEY_DRIVE_SYNC_ENABLED, false),
        prefs.getBoolean(Constants.KEY_OFFLINE_MODE, Constants.DEFAULT_OFFLINE_MODE),
        prefs.getString(Constants.KEY_DRIVE_ACCOUNT_EMAIL, "").orEmpty()
    )
    var configuration by remember(prefs) { mutableStateOf(read()) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> configuration = read() }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        configuration = read()
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return configuration
}

private fun connectionFlow(context: Context) = callbackFlow {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    fun connected() = manager.getNetworkCapabilities(manager.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) { trySend(false) }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        }
    }
    manager.registerDefaultNetworkCallback(callback)
    trySend(connected())
    awaitClose { manager.unregisterNetworkCallback(callback) }
}
