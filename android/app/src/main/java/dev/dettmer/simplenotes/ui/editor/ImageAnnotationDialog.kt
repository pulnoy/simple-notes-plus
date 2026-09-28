package dev.dettmer.simplenotes.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.dettmer.simplenotes.storage.AssetStore
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_STROKE_WIDTH = 0.008f
private const val MIN_STROKE_WIDTH = 0.002f
private const val MAX_STROKE_WIDTH = 0.025f
private const val ERASER_WIDTH_MULTIPLIER = 4f
private const val HIGHLIGHTER_WIDTH_MULTIPLIER = 3f
private const val HIGHLIGHTER_ALPHA = 0.35f
private const val TEXT_HIT_RADIUS = 0.15f
private const val TEXT_SIZE_FRACTION = 0.07f
private const val TEXT_SHADOW_RADIUS = 3f
private const val COLOR_CHANNEL_MAX = 255
const val NEW_DRAWING_ASSET = "__simple_notes_plus_new_drawing__"

private sealed interface AnnotationAction {
    data class StrokeAction(
        val points: List<Offset>,
        val color: Color,
        val widthFraction: Float,
        val alpha: Float
    ) : AnnotationAction

    data class TextAction(val text: String, val position: Offset, val color: Color) : AnnotationAction
    data class EraserAction(val points: List<Offset>, val widthFraction: Float) : AnnotationAction
}

private class AnnotationEditorState {
    val actions = mutableStateListOf<AnnotationAction>()
    val undo = mutableStateListOf<List<AnnotationAction>>()
    val redo = mutableStateListOf<List<AnnotationAction>>()
    var currentPoints by mutableStateOf<List<Offset>>(emptyList())
    var selectedColor by mutableStateOf(Color.Red)
    var highlighter by mutableStateOf(false)
    var eraser by mutableStateOf(false)
    var movingText by mutableStateOf(false)
    var strokeWidth by mutableStateOf(DEFAULT_STROKE_WIDTH)
    var saveError by mutableStateOf(false)
    var showTextDialog by mutableStateOf(false)
    var pendingText by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)

    fun undoLast() {
        if (undo.isNotEmpty()) {
            redo += actions.toList()
            val previous = undo.removeAt(undo.lastIndex)
            actions.clear()
            actions.addAll(previous)
        }
    }

    fun redoLast() {
        if (redo.isNotEmpty()) {
            undo += actions.toList()
            val next = redo.removeAt(redo.lastIndex)
            actions.clear()
            actions.addAll(next)
        }
    }

    fun rememberChanges() {
        undo += actions.toList()
        redo.clear()
    }

    fun placeText(position: Offset) {
        pendingText?.let { text ->
            rememberChanges()
            actions += AnnotationAction.TextAction(text, position, selectedColor)
        }
        pendingText = null
    }

    fun finishStroke(points: List<Offset>) {
        if (points.size > 1) {
            rememberChanges()
            actions += if (eraser) {
                AnnotationAction.EraserAction(points, strokeWidth * ERASER_WIDTH_MULTIPLIER)
            } else {
                AnnotationAction.StrokeAction(
                    points, selectedColor,
                    strokeWidth * if (highlighter) HIGHLIGHTER_WIDTH_MULTIPLIER else 1f,
                    if (highlighter) HIGHLIGHTER_ALPHA else 1f
                )
            }
        }
        currentPoints = emptyList()
    }

    fun moveText(index: Int, position: Offset) {
        val current = actions.getOrNull(index) as? AnnotationAction.TextAction
        if (current != null) actions[index] = current.copy(position = position)
    }
}

/** Éditeur non destructif : l'image annotée est toujours enregistrée comme un nouvel asset. */
@Composable
fun ImageAnnotationDialog(assetName: String, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { AssetStore(context) }
    val bitmap = remember(assetName) {
        if (assetName == NEW_DRAWING_ASSET) {
            Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
        } else {
            BitmapFactory.decodeFile(store.getAssetFile(assetName).absolutePath)
        }
    } ?: run {
        onDismiss()
        return
    }
    val editor = remember { AnnotationEditorState() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnnotationScaffold(bitmap, store, editor, onDismiss, onSaved)
    }
    if (editor.showTextDialog) {
        AddAnnotationTextDialog(
            onDismiss = { editor.showTextDialog = false },
            onAdd = { text ->
                editor.pendingText = text
                editor.showTextDialog = false
            }
        )
    }
}

@Composable
private fun AnnotationScaffold(
    bitmap: Bitmap,
    store: AssetStore,
    editor: AnnotationEditorState,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            AnnotationTopBar(editor, onDismiss) {
                editor.saving = true
                scope.launch {
                    val result = runCatching { saveAnnotatedBitmap(bitmap, editor.actions.toList(), store) }
                    editor.saving = false
                    result.onSuccess(onSaved).onFailure { editor.saveError = true }
                }
            }
        },
        bottomBar = { AnnotationToolbar(editor) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val scale = minOf(maxWidth.value / image.width, maxHeight.value / image.height)
                AnnotationCanvas(
                    image = image,
                    editor = editor,
                    modifier = Modifier.size((image.width * scale).dp, (image.height * scale).dp)
                )
            }
            if (editor.saveError) {
                Text(
                    "Impossible d’enregistrer le dessin.", color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnnotationTopBar(editor: AnnotationEditorState, onDismiss: () -> Unit, onSave: () -> Unit) {
    TopAppBar(
        title = { Text("Annoter l’image") },
        navigationIcon = {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
        },
        actions = {
            IconButton(enabled = editor.undo.isNotEmpty(), onClick = editor::undoLast) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler")
            }
            IconButton(enabled = editor.redo.isNotEmpty(), onClick = editor::redoLast) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rétablir")
            }
            IconButton(enabled = !editor.saving && editor.actions.isNotEmpty(), onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = "Enregistrer")
            }
        }
    )
}

@Composable
private fun AnnotationCanvas(image: ImageBitmap, editor: AnnotationEditorState, modifier: Modifier) {
    val actions = editor.actions
    val currentPoints = editor.currentPoints
    val selectedColor = editor.selectedColor
    val highlighter = editor.highlighter
    val eraser = editor.eraser
    val movingText = editor.movingText
    val strokeWidth = editor.strokeWidth
    val pendingText = editor.pendingText
    val onCurrentPoints: (List<Offset>) -> Unit = { editor.currentPoints = it }
    val onTextPlaced = editor::placeText
    val onStrokeFinished = editor::finishStroke
    val onMoveTextStart = editor::rememberChanges
    val onMoveText = editor::moveText
    Canvas(
        modifier = modifier
            .pointerInput(selectedColor, highlighter, eraser, movingText, pendingText) {
                if (pendingText != null) {
                    detectTapGestures { position ->
                        onTextPlaced(normalize(position, size.width, size.height))
                    }
                } else if (movingText) {
                    var selectedIndex: Int? = null
                    detectDragGestures(
                        onDragStart = { p ->
                            val pos = normalize(p, size.width, size.height)
                            selectedIndex = actions.indices.lastOrNull { index ->
                                val action = actions[index] as? AnnotationAction.TextAction
                                action != null && (action.position - pos).getDistance() < TEXT_HIT_RADIUS
                            }
                            if (selectedIndex != null) onMoveTextStart()
                        },
                        onDrag = { change, _ ->
                            selectedIndex?.let { onMoveText(it, normalize(change.position, size.width, size.height)) }
                            change.consume()
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                } else {
                    val dragPoints = mutableListOf<Offset>()
                    detectDragGestures(
                        onDragStart = { p ->
                            dragPoints.clear()
                            dragPoints += normalize(p, size.width, size.height)
                            onCurrentPoints(dragPoints.toList())
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragPoints += normalize(change.position, size.width, size.height)
                            onCurrentPoints(dragPoints.toList())
                        },
                        onDragEnd = { onStrokeFinished(dragPoints.toList()) },
                        onDragCancel = { onStrokeFinished(dragPoints.toList()) }
                    )
                }
            }
    ) {
        drawImage(image, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
        drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), Paint())
        actions.forEach { drawAnnotation(it) }
        if (currentPoints.size > 1) {
            drawNormalizedPath(
                currentPoints,
                selectedColor,
                strokeWidth * when {
                    eraser -> ERASER_WIDTH_MULTIPLIER
                    highlighter -> HIGHLIGHTER_WIDTH_MULTIPLIER
                    else -> 1f
                },
                if (highlighter) HIGHLIGHTER_ALPHA else 1f,
                if (eraser) BlendMode.Clear else BlendMode.SrcOver
            )
        }
        drawContext.canvas.restore()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotation(action: AnnotationAction) {
    when (action) {
        is AnnotationAction.StrokeAction -> drawNormalizedPath(
            action.points, action.color, action.widthFraction, action.alpha
        )
        is AnnotationAction.TextAction -> drawContext.canvas.nativeCanvas.drawText(
            action.text,
            action.position.x * size.width,
            action.position.y * size.height,
            AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = action.color.toArgb()
                textSize = size.minDimension * TEXT_SIZE_FRACTION
                setShadowLayer(TEXT_SHADOW_RADIUS, 1f, 1f, android.graphics.Color.BLACK)
            }
        )
        is AnnotationAction.EraserAction -> drawNormalizedPath(
            action.points, Color.Transparent, action.widthFraction, 1f, BlendMode.Clear
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNormalizedPath(
    points: List<Offset>, color: Color, widthFraction: Float, alpha: Float,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x * size.width, points.first().y * size.height)
        points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
    }
    drawPath(
        path,
        color.copy(alpha = alpha),
        style = Stroke(width = size.minDimension * widthFraction, cap = StrokeCap.Round),
        blendMode = blendMode
    )
}

@Composable
private fun AnnotationToolbar(editor: AnnotationEditorState) {
    val selectedColor = editor.selectedColor
    val highlighter = editor.highlighter
    val eraser = editor.eraser
    val movingText = editor.movingText
    val strokeWidth = editor.strokeWidth
    val onColor: (Color) -> Unit = { editor.selectedColor = it }
    val onHighlighter = { editor.highlighter = !editor.highlighter; editor.eraser = false }
    val onEraser = { editor.eraser = !editor.eraser; editor.highlighter = false }
    val onMoveText = { editor.movingText = !editor.movingText; editor.eraser = false; editor.highlighter = false }
    val onStrokeWidth: (Float) -> Unit = { editor.strokeWidth = it }
    val onText = { editor.showTextDialog = true }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Text("Épaisseur du trait")
        Slider(value = strokeWidth, onValueChange = onStrokeWidth, valueRange = MIN_STROKE_WIDTH..MAX_STROKE_WIDTH)
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.White, Color.Black).forEach { color ->
            Button(
                onClick = { onColor(color) },
                modifier = Modifier.size(if (selectedColor == color) 38.dp else 32.dp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) { Box(Modifier.fillMaxSize().background(color, CircleShape)) }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onHighlighter) {
            Icon(
                Icons.Default.Brush,
                contentDescription = "Surligneur",
                tint = if (highlighter) Color.Yellow else MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onText) {
            Icon(Icons.Default.TextFields, contentDescription = "Ajouter du texte")
        }
        TextButton(onClick = onEraser) {
            Text("Gomme", color = if (eraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        TextButton(onClick = onMoveText) {
            Text("Déplacer", color = if (movingText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
    }
}

@Composable
private fun AddAnnotationTextDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter du texte") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onAdd(text.trim()) }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private fun normalize(offset: Offset, width: Int, height: Int) = Offset(
    (offset.x / width.coerceAtLeast(1)).coerceIn(0f, 1f),
    (offset.y / height.coerceAtLeast(1)).coerceIn(0f, 1f)
)

private suspend fun saveAnnotatedBitmap(
    source: Bitmap,
    actions: List<AnnotationAction>,
    store: AssetStore
): String = withContext(Dispatchers.Default) {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(output)
    val overlay = Bitmap.createBitmap(output.width, output.height, Bitmap.Config.ARGB_8888)
    val overlayCanvas = AndroidCanvas(overlay)
    actions.forEach { action ->
        when (action) {
            is AnnotationAction.StrokeAction -> {
                val path = android.graphics.Path().apply {
                    moveTo(action.points.first().x * output.width, action.points.first().y * output.height)
                    action.points.drop(1).forEach { lineTo(it.x * output.width, it.y * output.height) }
                }
                overlayCanvas.drawPath(path, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = action.color.toArgb()
                    alpha = (action.alpha * COLOR_CHANNEL_MAX).toInt()
                    style = AndroidPaint.Style.STROKE
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                    strokeWidth = output.width.coerceAtMost(output.height) * action.widthFraction
                })
            }
            is AnnotationAction.TextAction -> overlayCanvas.drawText(
                action.text,
                action.position.x * output.width,
                action.position.y * output.height,
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = action.color.toArgb()
                    textSize = output.width.coerceAtMost(output.height) * TEXT_SIZE_FRACTION
                    setShadowLayer(TEXT_SHADOW_RADIUS, 1f, 1f, android.graphics.Color.BLACK)
                }
            )
            is AnnotationAction.EraserAction -> {
                val path = android.graphics.Path().apply {
                    moveTo(action.points.first().x * output.width, action.points.first().y * output.height)
                    action.points.drop(1).forEach { lineTo(it.x * output.width, it.y * output.height) }
                }
                overlayCanvas.drawPath(path, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    style = AndroidPaint.Style.STROKE
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                    strokeWidth = output.width.coerceAtMost(output.height) * action.widthFraction
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                })
            }
        }
    }
    canvas.drawBitmap(overlay, 0f, 0f, null)
    overlay.recycle()
    val bytes = ByteArrayOutputStream().use { stream ->
        output.compress(Bitmap.CompressFormat.WEBP, 92, stream)
        stream.toByteArray()
    }
    output.recycle()
    store.saveAsset(bytes, "webp")
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * COLOR_CHANNEL_MAX).toInt(), (red * COLOR_CHANNEL_MAX).toInt(),
    (green * COLOR_CHANNEL_MAX).toInt(), (blue * COLOR_CHANNEL_MAX).toInt()
)
