package dev.dettmer.simplenotes.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.markdown.noteCardMarkdownPreview
import dev.dettmer.simplenotes.models.Note
import dev.dettmer.simplenotes.models.NoteType
import dev.dettmer.simplenotes.ui.theme.NoteColorPalette
import dev.dettmer.simplenotes.ui.theme.NotePreviewLength
import dev.dettmer.simplenotes.ui.theme.LocalHomeAccent
import dev.dettmer.simplenotes.utils.toReadableTime

private const val NOTE_COLOR_STRENGTH = 0.4f

private data class CardDisplay(val lines: Int, val itemLines: Int, val timestamp: Boolean, val folder: Boolean)

@Suppress("LongParameterList")
@Composable
fun NoteCardGrid(
    note: Note,
    showSyncStatus: Boolean,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    timestampTicker: Long = 0L,
    previewLength: NotePreviewLength = NotePreviewLength.STANDARD,
    showTimestamp: Boolean = true,
    showTypeIcon: Boolean = true,
    showFolderLabel: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val display = remember(previewLength, showTimestamp, showFolderLabel, timestampTicker) {
        CardDisplay(previewLength.gridLargeLines, previewLength.itemMaxLines, showTimestamp, showFolderLabel)
    }
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier.fillMaxWidth().then(
            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
        ).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape, colors = CardDefaults.cardColors(containerColor = homeCardColor(note)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeCardTitle(note, isSelected, isSelectionMode, showTypeIcon, onLongClick)
            HomeCardBody(note, display)
            HomeCardFooter(note, display, showSyncStatus)
        }
    }
}

@Composable
private fun homeCardColor(note: Note): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val chosen = NoteColorPalette.resolveContainer(note.color, dark)
    return chosen.takeOrElse {
        when {
            dark -> MaterialTheme.colorScheme.surfaceContainerHigh
            note.isPinned == true -> MaterialTheme.colorScheme.primaryContainer
            HomeAttachments.firstAudio(note.content) != null -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.White
        }
    }.let { if (!dark && chosen != Color.Unspecified) lerp(Color.White, it, NOTE_COLOR_STRENGTH) else it }
}

@Composable
private fun HomeCardTitle(note: Note, selected: Boolean, selectionMode: Boolean, showPin: Boolean, onMenu: () -> Unit) {
    val fallback = noteCardMarkdownPreview(HomeAttachments.previewText(note.content))
        .text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    val title = note.title.ifBlank {
        when {
            HomeAttachments.firstAudio(note.content) != null -> stringResource(R.string.home_voice_memo)
            HomeAttachments.firstImage(note.content) != null && fallback.isBlank() -> stringResource(R.string.fab_image_note)
            else -> fallback
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (note.isPinned == true && showPin) Icon(Icons.Default.PushPin, stringResource(R.string.section_pinned),
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        IconButton(onClick = onMenu, modifier = Modifier.size(28.dp)) {
            Icon(if (selectionMode && selected) Icons.Default.Check else Icons.Default.MoreVert,
                stringResource(R.string.home_note_actions), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HomeCardBody(note: Note, display: CardDisplay) {
    if (display.lines == 0) return
    val image = remember(note.content) { HomeAttachments.firstImage(note.content) }
    val audio = remember(note.content) { HomeAttachments.firstAudio(note.content) }
    if (note.noteType == NoteType.CHECKLIST) {
        HomeChecklist(note, display)
    } else {
        val text = HomeAttachments.previewText(note.content)
        val body = if (note.title.isBlank() && image == null && audio == null) text.substringAfter('\n', "") else text
        val preview = noteCardMarkdownPreview(body)
        if (preview.isNotBlank()) Text(preview, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = display.lines, overflow = TextOverflow.Ellipsis)
    }
    if (image != null) HomePhotoPreview(image)
    if (audio != null) HomeAudioPreview(audio)
}

@Composable
private fun HomeChecklist(note: Note, display: CardDisplay) {
    val items = remember(note.checklistItems, note.checklistSortOption) {
        sortChecklistItemsForPreview(note.checklistItems.orEmpty(), note.checklistSortOption)
    }
    val visible = items.take(display.lines)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        visible.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val shape = RoundedCornerShape(5.dp)
                Box(Modifier.size(22.dp).background(
                    if (item.isChecked) LocalHomeAccent.current.color else Color.Transparent, shape
                ).border(1.dp, if (item.isChecked) LocalHomeAccent.current.color else MaterialTheme.colorScheme.outline, shape),
                    contentAlignment = Alignment.Center) {
                    if (item.isChecked) Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp),
                        tint = LocalHomeAccent.current.onColor)
                }
                Text(item.text, style = MaterialTheme.typography.bodyMedium, maxLines = display.itemLines,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        if (items.size > visible.size) Text(stringResource(R.string.checklist_items_more, items.size - visible.size),
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HomeCardFooter(note: Note, display: CardDisplay, sync: Boolean) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (display.timestamp) Text(note.updatedAt.toReadableTime(context), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
        if (display.folder && note.folderName != null) Text(note.folderName,
            style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 96.dp).background(
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp))
        if (sync && note.syncStatus != dev.dettmer.simplenotes.models.SyncStatus.SYNCED) {
            Icon(syncStatusIcon(note.syncStatus), syncStatusDescription(note.syncStatus),
                tint = syncStatusTint(note.syncStatus), modifier = Modifier.size(14.dp))
        }
    }
}
