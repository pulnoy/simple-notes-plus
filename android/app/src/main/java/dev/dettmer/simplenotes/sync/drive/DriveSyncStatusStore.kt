package dev.dettmer.simplenotes.sync.drive

import android.content.Context
import androidx.core.content.edit
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

data class DriveSyncRecord(val lastSuccess: Long = 0L, val problem: DriveSyncProblem? = null)

enum class DriveSyncProblem { CONNECTION, AUTHORIZATION, DATA, OTHER }

/** Account-scoped result, persisted even when Android restarts the process. No tokens or raw errors. */
class DriveSyncStatusStore(context: Context) {
    private val prefs = context.getSharedPreferences("drive_sync_results", Context.MODE_PRIVATE)

    fun observe(account: String) = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(read(account))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read(account))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun read(account: String) = DriveSyncRecord(
        lastSuccess = prefs.getLong("$account.success", 0L),
        problem = prefs.getString("$account.problem", null)?.let { value ->
            DriveSyncProblem.entries.find { it.name == value }
        }
    )

    suspend fun <T> track(account: String, action: suspend () -> T): T = try {
        val result = action()
        prefs.edit {
            putLong("$account.success", System.currentTimeMillis())
            remove("$account.problem")
        }
        result
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        prefs.edit { putString("$account.problem", classify(error).name) }
        throw error
    }

    private fun classify(error: Exception): DriveSyncProblem {
        val message = error.message.orEmpty().lowercase()
        return when {
            listOf("reconnect", "account", "401", "403", "authorization").any { it in message } ->
                DriveSyncProblem.AUTHORIZATION
            listOf("invalid", "unsupported", "missing note attachment").any { it in message } ->
                DriveSyncProblem.DATA
            error is IOException -> DriveSyncProblem.CONNECTION
            else -> DriveSyncProblem.OTHER
        }
    }
}
