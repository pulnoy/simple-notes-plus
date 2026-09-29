package dev.dettmer.simplenotes.ui.settings.screens

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.ui.main.components.DriveSyncStatus
import dev.dettmer.simplenotes.sync.drive.DriveAuthorization
import dev.dettmer.simplenotes.sync.drive.DriveAuthResult
import dev.dettmer.simplenotes.ui.settings.SettingsViewModel
import dev.dettmer.simplenotes.ui.settings.components.SettingsButton
import dev.dettmer.simplenotes.ui.settings.components.SettingsHint
import dev.dettmer.simplenotes.ui.settings.components.SettingsOutlinedButton
import dev.dettmer.simplenotes.ui.settings.components.SettingsSectionCard
import kotlinx.coroutines.launch

@Composable
internal fun DriveSyncSection(viewModel: SettingsViewModel) {
    val driveSyncEnabled by viewModel.driveSyncEnabled.collectAsState()
    val driveAccountEmail by viewModel.driveAccountEmail.collectAsState()
    var driveError by remember { mutableStateOf<String?>(null) }
    var driveConnecting by remember { mutableStateOf(false) }
    val driveAccountMissing = stringResource(R.string.drive_sync_account_missing)
    val driveAuthorizationIncomplete = stringResource(R.string.drive_sync_authorization_incomplete)
    val driveConnectionError = stringResource(R.string.drive_sync_connection_error)
    val driveScope = rememberCoroutineScope()
    val completeDriveConnection: (DriveAuthResult) -> Unit = { authorization ->
        driveScope.launch {
            driveError = connectDriveAccount(
                viewModel, authorization, driveAuthorizationIncomplete, driveAccountMissing, driveConnectionError
            )
            driveConnecting = false
        }
    }
    val driveConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                completeDriveConnection(DriveAuthorization.finish(viewModel.getApplication(), result.data!!))
            } catch (error: Exception) {
                driveConnecting = false
                driveError = error.localizedMessage ?: driveConnectionError
            }
        } else {
            driveConnecting = false
        }
    }

    if (DriveAuthorization.AVAILABLE) {
        SettingsSectionCard(title = stringResource(R.string.drive_sync_title)) {
            SettingsHint(
                text = if (driveSyncEnabled) {
                    stringResource(R.string.drive_sync_connected, driveAccountEmail.orEmpty())
                } else {
                    stringResource(R.string.drive_sync_description)
                }
            )
            if (driveSyncEnabled) {
                DriveSyncStatus()
                SettingsOutlinedButton(
                    text = stringResource(R.string.drive_sync_disconnect),
                    onClick = { viewModel.disableDriveSync() }
                )
            }
            SettingsButton(
                text = stringResource(if (driveSyncEnabled) {
                    R.string.drive_sync_reauthorize
                } else {
                    R.string.drive_sync_connect
                }),
                isLoading = driveConnecting,
                onClick = {
                    driveConnecting = true
                    driveError = null
                    beginDriveAuthorization(
                        viewModel.getApplication(),
                        onConsent = { driveConsentLauncher.launch(it) },
                        onSuccess = completeDriveConnection,
                        onError = { error ->
                            driveConnecting = false
                            driveError = error.localizedMessage ?: driveConnectionError
                        }
                    )
                }
            )
            driveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

}

private fun beginDriveAuthorization(
    context: Context,
    onConsent: (IntentSenderRequest) -> Unit,
    onSuccess: (DriveAuthResult) -> Unit,
    onError: (Exception) -> Unit
) {
    DriveAuthorization.start(context, onSuccess = { authorization ->
        val pending = authorization.pendingIntent
        if (pending != null) {
            onConsent(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            onSuccess(authorization)
        }
    }, onError = onError)
}

private suspend fun connectDriveAccount(
    viewModel: SettingsViewModel,
    authorization: DriveAuthResult,
    incompleteMessage: String,
    missingMessage: String,
    connectionError: String
): String? {
    val token = authorization.accessToken
    if (token.isNullOrBlank()) return incompleteMessage
    return try {
        val email = authorization.email ?: DriveAuthorization.accountEmail(token)
        if (email.isNullOrBlank()) {
            missingMessage
        } else {
            viewModel.enableDriveSync(email)
            null
        }
    } catch (error: Exception) {
        error.localizedMessage ?: connectionError
    }
}
