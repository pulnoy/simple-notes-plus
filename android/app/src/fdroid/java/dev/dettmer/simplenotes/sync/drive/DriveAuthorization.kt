package dev.dettmer.simplenotes.sync.drive

import android.content.Context
import android.content.Intent

object DriveAuthorization {
    const val AVAILABLE = false

    fun start(context: Context, onSuccess: (DriveAuthResult) -> Unit, onError: (Exception) -> Unit) {
        onError(UnsupportedOperationException("Google Drive requires Google Play services"))
    }

    fun finish(context: Context, data: Intent): DriveAuthResult =
        throw UnsupportedOperationException("Google Drive requires Google Play services")

    suspend fun accountEmail(token: String): String? = null

    suspend fun token(context: Context): String? = null
}
