package dev.dettmer.simplenotes.ui.main.components

import android.view.MotionEvent

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.Folder
import dev.dettmer.simplenotes.models.childrenOf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val EDGE_ZONE_PX = 100f
private const val SCROLL_STEP_PX = 14f
private const val FRAME_MS = 16L

/** Changes are committed only on drop; cancelling a gesture leaves the order untouched. */
internal class FolderDragState(private val list: LazyListState, private val scope: CoroutineScope) {
    var source by mutableStateOf<String?>(null)
        private set
    var target by mutableStateOf<String?>(null)
        private set
    private var pointerY by mutableStateOf(0f)
    private var folders: List<Folder> = emptyList()
    private var scrolling: Job? = null
    private val handles = mutableMapOf<String, LayoutCoordinates>()
    var listCoordinates: LayoutCoordinates? = null
    private val listOrigin get() = listCoordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
    private var holding: Job? = null
    private var captured = false
    private var downPosition = Offset.Zero
    private var lastPosition = Offset.Zero
    private var downTime = 0L
    private var abandoned = false

    fun nativeDown(position: Offset, time: Long, current: List<Folder>, timeout: Long): Boolean {
        if (handles.values.none { it.isAttached && it.boundsInRoot().contains(position + listOrigin) }) return false
        captured = true
        abandoned = false
        downPosition = position
        lastPosition = position
        downTime = time
        holding = scope.launch { delay(timeout); if (!abandoned && captured) startAt(downPosition, current) }
        return true
    }

    fun nativeMove(position: Offset, time: Long, current: List<Folder>, timeout: Long, slop: Float): Boolean {
        if (!captured) return false
        if (!abandoned && source == null) {
            if (time - downTime >= timeout) {
                holding?.cancel()
                startAt(downPosition, current)
            } else if ((position - downPosition).getDistance() > slop) {
                abandoned = true
                holding?.cancel()
            }
        }
        if (source != null) drag(position.y - lastPosition.y)
        lastPosition = position
        return true
    }

    fun nativeEnd(dropped: Boolean, move: (String, Int) -> Unit): Boolean {
        val handled = captured
        if (dropped && source != null) finish(move) else cancel()
        return handled
    }

    fun handleBounds(name: String, bounds: LayoutCoordinates?) {
        if (bounds == null) handles.remove(name) else handles[name] = bounds
    }

    fun handleOrigin(name: String) = handles[name]?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero

    fun startAt(position: Offset, current: List<Folder>) {
        handles.entries.firstOrNull {
            it.value.isAttached && it.value.boundsInRoot().contains(position + listOrigin)
        }?.key?.let { start(it, current) }
    }

    fun start(name: String, current: List<Folder>) {
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == name } ?: return
        folders = current
        source = name
        target = name
        pointerY = item.offset + item.size / 2f
        scrolling = scope.launch {
            while (source != null) {
                val layout = list.layoutInfo
                val delta = when {
                    pointerY < layout.viewportStartOffset + EDGE_ZONE_PX -> -SCROLL_STEP_PX
                    pointerY > layout.viewportEndOffset - EDGE_ZONE_PX -> SCROLL_STEP_PX
                    else -> 0f
                }
                if (delta != 0f) { list.scrollBy(delta); updateTarget() }
                delay(FRAME_MS)
            }
        }
    }

    fun drag(delta: Float) { pointerY += delta; updateTarget() }

    private fun updateTarget() {
        val parent = folders.firstOrNull { it.name == source }?.parentName
        val siblings = folders.childrenOf(parent).map { it.name }.toSet()
        target = list.layoutInfo.visibleItemsInfo.filter { it.key in siblings }
            .minByOrNull { abs(it.offset + it.size / 2f - pointerY) }?.key as? String ?: target
    }

    fun translation(name: String): Float {
        if (source != name) return 0f
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == name } ?: return 0f
        return pointerY - (item.offset + item.size / 2f)
    }

    fun finish(move: (String, Int) -> Unit) {
        val name = source
        val parent = folders.firstOrNull { it.name == name }?.parentName
        val siblings = folders.childrenOf(parent).map { it.name }
        val from = siblings.indexOf(name)
        val to = siblings.indexOf(target)
        if (name != null && from >= 0 && to >= 0 && from != to) move(name, to - from)
        cancel()
    }

    fun cancel() {
        holding?.cancel()
        scrolling?.cancel()
        captured = false
        abandoned = true
        source = null
        target = null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FolderDragHandle(name: String, drag: FolderDragState, folders: List<Folder>, move: (String, Int) -> Unit) {
    val configuration = LocalViewConfiguration.current
    val disallow = remember { RequestDisallowInterceptTouchEvent() }
    DisposableEffect(name) { onDispose { drag.handleBounds(name, null) } }
    Icon(Icons.Default.DragIndicator, stringResource(R.string.folder_drag_handle, name),
        modifier = Modifier.size(48.dp).testTag("folder_drag_$name")
            .onGloballyPositioned { drag.handleBounds(name, it) }
            .pointerInteropFilter(requestDisallowInterceptTouchEvent = disallow) { event ->
                val origin = drag.handleOrigin(name)
                val listOrigin = drag.listCoordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
                val position = Offset(event.x, event.y) + origin - listOrigin
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        disallow(true)
                        drag.nativeDown(position, event.eventTime, folders, configuration.longPressTimeoutMillis)
                    }
                    MotionEvent.ACTION_MOVE -> drag.nativeMove(position, event.eventTime, folders,
                        configuration.longPressTimeoutMillis, configuration.touchSlop)
                    MotionEvent.ACTION_UP -> drag.nativeEnd(true, move)
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> drag.nativeEnd(false, move)
                    else -> false
                }
            })
}
