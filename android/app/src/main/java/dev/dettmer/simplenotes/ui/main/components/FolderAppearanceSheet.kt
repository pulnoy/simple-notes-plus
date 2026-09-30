package dev.dettmer.simplenotes.ui.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.Folder
import dev.dettmer.simplenotes.ui.theme.NoteColorPalette

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FolderAppearanceSheet(folder: Folder, onSave: (String?, String?) -> Unit, onDismiss: () -> Unit) {
    var icon by remember(folder.name) { mutableStateOf(folder.icon ?: "folder") }
    var color by remember(folder.name) { mutableStateOf(folder.color) }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState())
            .padding(20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(folder.name, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.folder_icon_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                folderIcons.forEach { (key, value) ->
                    FilterChip(selected = icon == key, onClick = { icon = key },
                        label = { Text(stringResource(value.second)) },
                        leadingIcon = { Icon(value.first, null) })
                }
            }
            Text(stringResource(R.string.folder_background_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorSwatch(MaterialTheme.colorScheme.surfaceContainerHigh, color == null, true,
                    stringResource(R.string.note_color_none), { color = null })
                NoteColorPalette.slots.forEach { slot ->
                    ColorSwatch(if (dark) slot.containerColorDark else slot.containerColor, color == slot.hex, false,
                        stringResource(slot.labelRes()), { color = slot.hex })
                }
            }
            TextButton(onClick = { onSave(icon, color) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.folder_save_appearance))
            }
        }
    }
}
