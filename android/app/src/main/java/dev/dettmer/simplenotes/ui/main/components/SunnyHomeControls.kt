package dev.dettmer.simplenotes.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.Folder
import dev.dettmer.simplenotes.ui.theme.SunnyColors


@Composable
fun SunnyHomeControls(
    query: String,
    onQuery: (String) -> Unit,
    folders: List<Folder>,
    currentFolder: String?,
    actions: HomeFolderActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        HomeSearchField(query, onQuery)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeFolderChip(stringResource(R.string.home_all_notes), currentFolder == null, { actions.select(null) })
            folders.forEach { folder ->
                HomeFolderChip(folder.name, currentFolder == folder.name, { actions.select(folder.name) },
                    onRename = { actions.rename(folder.name) }, onDelete = { actions.delete(folder.name) })
            }
            Surface(onClick = actions.add, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, stringResource(R.string.fab_create_folder))
                }
            }
        }
    }
}

@Composable
private fun HomeSearchField(query: String, onQuery: (String) -> Unit) {
    val focus = LocalFocusManager.current
    TextField(
        value = query, onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text(stringResource(R.string.home_search)) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                Icon(Icons.Default.Close, stringResource(R.string.home_clear_search))
            }
        },
        shape = RoundedCornerShape(32.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun HomeFolderChip(
    name: String, selected: Boolean, onSelect: () -> Unit,
    onRename: (() -> Unit)? = null, onDelete: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    val menuLabel = stringResource(R.string.home_folder_menu, name)
    Box {
        Row(
            modifier = Modifier.testTag("home_folder_$name").background(
                if (selected) SunnyColors.Yellow else MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(28.dp)
            ).combinedClickable(onClick = onSelect, onLongClick = { if (onRename != null) menuOpen = true })
                .padding(start = 16.dp, end = if (onRename == null) 16.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(20.dp),
                tint = if (selected) SunnyColors.Ink else MaterialTheme.colorScheme.onSurface)
            Text(name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) SunnyColors.Ink else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 14.dp))
            if (onRename != null) IconButton(onClick = { menuOpen = true }, modifier = Modifier.semantics {
                contentDescription = menuLabel
            }) { Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(18.dp),
                tint = if (selected) SunnyColors.Ink else MaterialTheme.colorScheme.onSurface) }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.home_rename)) }, onClick = {
                menuOpen = false; onRename?.invoke()
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.home_delete_folder)) }, onClick = {
                menuOpen = false; onDelete?.invoke()
            })
        }
    }
}
