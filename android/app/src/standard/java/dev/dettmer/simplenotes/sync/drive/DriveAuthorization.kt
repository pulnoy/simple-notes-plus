package dev.dettmer.simplenotes.sync.drive

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import dev.dettmer.simplenotes.utils.Constants
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DriveAuthorization {
    const val AVAILABLE = true
    private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    private fun request(email: String? = null): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE)))
            .apply { email?.let { setAccount(Account(it, "com.google")) } }
            .build()

    fun start(context: Context, onSuccess: (DriveAuthResult) -> Unit, onError: (Exception) -> Unit) {
        Identity.getAuthorizationClient(context).authorize(request())
            .addOnSuccessListener { result ->
                onSuccess(
                    DriveAuthResult(
                        pendingIntent = result.pendingIntent,
                        email = result.toGoogleSignInAccount()?.email,
                        accessToken = result.accessToken
                    )
                )
            }
            .addOnFailureListener { onError(it) }
    }

    fun finish(context: Context, data: Intent): DriveAuthResult {
        val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
        return DriveAuthResult(
            email = result.toGoogleSignInAccount()?.email,
            accessToken = result.accessToken
        )
    }

    suspend fun accountEmail(token: String): String? = withContext(Dispatchers.IO) {
        DriveApi(token).accountEmail()
    }

    suspend fun token(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(Constants.KEY_DRIVE_ACCOUNT_EMAIL, null) ?: return@withContext null
        val result = Tasks.await(
            Identity.getAuthorizationClient(context).authorize(request(email)),
            30,
            TimeUnit.SECONDS
        )
        if (result.hasResolution()) null else result.accessToken
    }
}
