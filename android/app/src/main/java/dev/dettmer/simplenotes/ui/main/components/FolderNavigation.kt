package dev.dettmer.simplenotes.ui.main.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.core.content.edit
import dev.dettmer.simplenotes.utils.Constants
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.Folder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderNavigation(
    enabled: Boolean,
    folders: List<Folder>,
    currentFolder: String?,
    actions: HomeFolderActions,
    content: @Composable (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }
    var locked by remember(prefs) { mutableStateOf(prefs.getBoolean("folder_drawer_locked", true)) }
    val state = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    LaunchedEffect(enabled) { if (!enabled) state.close() }
    BackHandler(state.isOpen) { scope.launch { state.close() } }
    fun closeThen(action: () -> Unit) { scope.launch { state.close(); action() } }
    val drawerActions = HomeFolderActions(
        select = { closeThen { actions.select(it) } },
        add = { closeThen(actions.add) },
        rename = { closeThen { actions.rename(it) } },
        delete = { closeThen { actions.delete(it) } },
        customize = { closeThen { actions.customize(it) } },
        move = actions.move,
        addChild = { closeThen { actions.addChild(it) } }
    )
    ModalNavigationDrawer(
        drawerState = state, gesturesEnabled = enabled,
        drawerContent = {
            if (enabled) ModalDrawerSheet {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_folders_title), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(if (locked) R.string.folder_drawer_locked else R.string.folder_drawer_unlocked),
                            style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { locked = !locked; prefs.edit { putBoolean("folder_drawer_locked", locked) } }) {
                        Icon(if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                            stringResource(if (locked) R.string.folder_drawer_unlock else R.string.folder_drawer_lock))
                    }
                }
                if (!locked) Text(stringResource(R.string.folder_drag_hint),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                FolderDestinations(folders, currentFolder, drawerActions,
                    Modifier.fillMaxWidth().testTag("folder_drawer"), highlightAll = true, reorderEnabled = !locked)
            }
        }
    ) { content { scope.launch { if (enabled) state.open() } } }
}
