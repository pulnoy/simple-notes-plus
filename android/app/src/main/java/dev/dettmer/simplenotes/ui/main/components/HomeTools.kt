package dev.dettmer.simplenotes.ui.main.components

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.Note
import dev.dettmer.simplenotes.ui.settings.ComposeSettingsActivity
import dev.dettmer.simplenotes.ui.settings.SettingsRoute
import dev.dettmer.simplenotes.widget.NewNoteWidgetReceiver
import dev.dettmer.simplenotes.widget.NoteWidgetReceiver
import dev.dettmer.simplenotes.widget.WidgetPinReceiver

@Composable
fun HomeTools(notes: List<Note>, onSearch: () -> Unit) {
    val context = LocalContext.current
    var showWidgets by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        TextButton(onClick = onSearch) { Text(stringResource(R.string.home_search)) }
        TextButton(onClick = {
            context.startActivity(Intent(context, ComposeSettingsActivity::class.java).apply {
                putExtra(ComposeSettingsActivity.EXTRA_INITIAL_ROUTE, SettingsRoute.Trash.route)
            })
        }) { Text(stringResource(R.string.trash_title)) }
        TextButton(onClick = { showWidgets = true }) { Text(stringResource(R.string.home_widgets)) }
    }
    if (showWidgets) WidgetPicker(notes) { showWidgets = false }
}

@Composable
private fun WidgetPicker(notes: List<Note>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    val visibleNotes = remember(notes) {
        notes.filterNot { it.isTrashed || it.isArchived }
            .sortedWith(compareByDescending<Note> { it.isPinned == true }.thenByDescending { it.updatedAt })
    }
    var refused by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_picker_title)) },
        text = {
            Column {
                Text(stringResource(if (supported && !refused) {
                    R.string.widget_picker_hint
                } else {
                    R.string.widget_picker_manual
                }))
                if (supported) {
                    TextButton(onClick = {
                        if (requestWidget(context, null)) onDismiss() else refused = true
                    }) { Text(stringResource(R.string.widget_picker_new)) }
                    WidgetNoteChoices(visibleNotes) { note ->
                        if (requestWidget(context, note.id)) onDismiss() else refused = true
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
private fun WidgetNoteChoices(notes: List<Note>, onChoose: (Note) -> Unit) {
    Text(stringResource(R.string.widget_picker_choose_note), style = MaterialTheme.typography.titleSmall)
    if (notes.isEmpty()) Text(stringResource(R.string.widget_picker_empty))
    LazyColumn(Modifier.heightIn(max = 280.dp)) {
        items(notes, key = { it.id }) { note ->
            TextButton(onClick = { onChoose(note) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (note.isPinned == true) R.string.widget_picker_pinned else R.string.widget_picker_note,
                        note.title.ifBlank { stringResource(R.string.widget_picker_untitled) }),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun requestWidget(context: Context, noteId: String?): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val receiver = if (noteId == null) NewNoteWidgetReceiver::class.java else NoteWidgetReceiver::class.java
    val callback = noteId?.let {
        val intent = Intent(context, WidgetPinReceiver::class.java).putExtra(WidgetPinReceiver.EXTRA_NOTE_ID, it)
        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        PendingIntent.getBroadcast(context, it.hashCode(), intent, flags)
    }
    return AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, receiver), null, callback)
}
