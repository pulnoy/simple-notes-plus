package dev.dettmer.simplenotes.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import dev.dettmer.simplenotes.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The launcher supplies the allocated widget ID; selected content stays in the explicit callback. */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(id)
                updateAppWidgetState(context, glanceId) { state -> state[NoteWidgetState.KEY_NOTE_ID] = noteId }
                NoteWidget().update(context, glanceId)
            } catch (error: Exception) {
                Logger.w("WidgetPinReceiver", "Could not configure pinned widget: ${error.message}")
            } finally {
                pending.finish()
            }
        }
    }

    companion object { const val EXTRA_NOTE_ID = "pinned_note_id" }
}
