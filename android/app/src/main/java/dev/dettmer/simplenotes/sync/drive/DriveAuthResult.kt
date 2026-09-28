package dev.dettmer.simplenotes.sync.drive

import android.app.PendingIntent

data class DriveAuthResult(
    val pendingIntent: PendingIntent? = null,
    val email: String? = null,
    val accessToken: String? = null
)
