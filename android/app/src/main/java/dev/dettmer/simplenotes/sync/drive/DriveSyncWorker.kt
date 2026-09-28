package dev.dettmer.simplenotes.sync.drive

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.dettmer.simplenotes.sync.SyncEvent
import dev.dettmer.simplenotes.sync.SyncEventBus
import dev.dettmer.simplenotes.sync.SyncStateManager
import dev.dettmer.simplenotes.utils.Constants
import dev.dettmer.simplenotes.utils.Logger
import dev.dettmer.simplenotes.widget.WidgetUpdateHelper
import java.io.IOException
import java.util.concurrent.TimeUnit

class DriveSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Constants.KEY_DRIVE_SYNC_ENABLED, false) ||
            prefs.getBoolean(Constants.KEY_OFFLINE_MODE, Constants.DEFAULT_OFFLINE_MODE)
        ) return Result.success()

        if (!SyncStateManager.tryStartSync("drive-worker", silent = true)) return Result.retry()
        return try {
            val outcome = DriveSyncEngine(applicationContext).sync()
            SyncEventBus.emit(SyncEvent.SyncCompleted(true, outcome.downloaded))
            if (outcome.downloaded > 0) WidgetUpdateHelper.refreshAllWidgets(applicationContext)
            SyncStateManager.markCompleted("Google Drive: synchronisation terminée")
            Result.success()
        } catch (e: IOException) {
            Logger.w("DriveSyncWorker", "Google Drive sync failed: ${e.message}")
            SyncStateManager.errorIfVisible(e.message)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Logger.e("DriveSyncWorker", "Google Drive sync failed", e)
            SyncStateManager.errorIfVisible(e.message)
            Result.failure()
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val work = OneTimeWorkRequestBuilder<DriveSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "google-drive-sync",
                ExistingWorkPolicy.KEEP,
                work
            )
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val work = PeriodicWorkRequestBuilder<DriveSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "google-drive-periodic-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("google-drive-sync")
            WorkManager.getInstance(context).cancelUniqueWork("google-drive-periodic-sync")
        }
    }
}
