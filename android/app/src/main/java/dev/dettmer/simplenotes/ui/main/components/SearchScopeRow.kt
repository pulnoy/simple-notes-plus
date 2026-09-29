package dev.dettmer.simplenotes.ui.main.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.NoteFilter

/** Null = all folders, empty name = notes at root. */
@Composable
fun SearchScopeRow(
    filter: NoteFilter,
    onFilter: (NoteFilter) -> Unit,
    folder: String?,
    folderNames: List<String>,
    onFolder: (String?) -> Unit
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaChip(filter, NoteFilter.IMAGE_ONLY, stringResource(R.string.filter_drawings), onFilter)
        MediaChip(filter, NoteFilter.AUDIO_ONLY, stringResource(R.string.filter_audio), onFilter)
        FolderFilter(folder, folderNames, onFolder)
    }
}

@Composable
private fun MediaChip(current: NoteFilter, type: NoteFilter, label: String, onFilter: (NoteFilter) -> Unit) {
    FilterChip(
        selected = current == type,
        onClick = { onFilter(if (current == type) NoteFilter.ALL else type) },
        label = { Text(label) }
    )
}

@Composable
private fun FolderFilter(folder: String?, names: List<String>, onFolder: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val all = stringResource(R.string.search_all_folders)
    val root = stringResource(R.string.search_root_folder)
    Column {
        FilterChip(
            selected = folder != null,
            onClick = { expanded = true },
            label = { Text(folder?.ifEmpty { root } ?: all) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf(null to all, "" to root) + names.distinct().sorted().map { it to it }).forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onFolder(value)
                    expanded = false
                })
            }
        }
    }
}
