package dev.dettmer.simplenotes.ui.main.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.Folder

@Composable
fun FolderBreadcrumb(folders: List<Folder>, current: String?, onUp: () -> Unit) {
    val path = mutableListOf<String>()
    var name = current
    while (name != null && name !in path) {
        path.add(name)
        name = folders.firstOrNull { it.name == name }?.parentName
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onUp) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.folder_go_parent))
        }
        Text(path.asReversed().joinToString(" › "), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge)
    }
}
