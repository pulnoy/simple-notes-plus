package dev.dettmer.simplenotes.ui.main.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
                Text(stringResource(R.string.home_folders_title),
                    style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(24.dp))
                FolderDestinations(folders, currentFolder, drawerActions,
                    Modifier.fillMaxWidth().testTag("folder_drawer"), highlightAll = true)
            }
        }
    ) { content { scope.launch { if (enabled) state.open() } } }
}
