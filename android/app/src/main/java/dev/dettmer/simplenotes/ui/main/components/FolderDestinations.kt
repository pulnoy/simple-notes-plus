package dev.dettmer.simplenotes.ui.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.Folder

@Composable
fun FolderDestinations(
    folders: List<Folder>, currentFolder: String?, actions: HomeFolderActions,
    modifier: Modifier = Modifier, highlightAll: Boolean = false
) {
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FolderDestination(stringResource(R.string.home_all_notes), highlightAll && currentFolder == null,
                { actions.select(null) })
        }
        items(folders, key = { it.name }) { folder ->
            FolderDestination(folder.name, currentFolder == folder.name, { actions.select(folder.name) }, actions)
        }
        item {
            Surface(onClick = actions.add, shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.fab_create_folder))
                }
            }
        }
    }
}

@Composable
private fun FolderDestination(
    name: String, selected: Boolean, onClick: () -> Unit, actions: HomeFolderActions? = null
) {
    var menu by remember { mutableStateOf(false) }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("home_folder_$name"),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary)
            Text(name, modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (actions != null) Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.home_folder_menu, name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_rename)) },
                        onClick = { menu = false; actions.rename(name) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.home_delete_folder)) },
                        onClick = { menu = false; actions.delete(name) })
                }
            }
        }
    }
}
