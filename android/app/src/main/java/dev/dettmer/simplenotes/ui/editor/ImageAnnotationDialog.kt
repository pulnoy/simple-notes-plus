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
import androidx.compose.foundation.layout.aspectRatio
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

/** Éditeur non destructif : l'image annotée est toujours enregistrée comme un nouvel asset. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImageAnnotationDialog(
    assetName: String,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
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
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val actions = remember { mutableStateListOf<AnnotationAction>() }
    val undo = remember { mutableStateListOf<List<AnnotationAction>>() }
    val redo = remember { mutableStateListOf<List<AnnotationAction>>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var highlighter by remember { mutableStateOf(false) }
    var eraser by remember { mutableStateOf(false) }
    var movingText by remember { mutableStateOf(false) }
    var strokeWidth by remember { mutableStateOf(DEFAULT_STROKE_WIDTH) }
    var saveError by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Annoter l’image") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = undo.isNotEmpty(),
                            onClick = {
                                if (undo.isNotEmpty()) {
                                    redo += actions.toList()
                                    val previous = undo.removeAt(undo.lastIndex)
                                    actions.clear()
                                    actions.addAll(previous)
                                }
                            }
                        ) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler") }
                        IconButton(
                            enabled = redo.isNotEmpty(),
                            onClick = {
                                if (redo.isNotEmpty()) {
                                    undo += actions.toList()
                                    val next = redo.removeAt(redo.lastIndex)
                                    actions.clear()
                                    actions.addAll(next)
                                }
                            }
                        ) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rétablir") }
                        IconButton(
                            enabled = !saving && actions.isNotEmpty(),
                            onClick = {
                                saving = true
                                scope.launch {
                                    val result = runCatching { saveAnnotatedBitmap(bitmap, actions.toList(), store) }
                                    saving = false
                                    result.onSuccess(onSaved).onFailure { saveError = true }
                                }
                            }
                        ) { Icon(Icons.Default.Save, contentDescription = "Enregistrer") }
                    }
                )
            },
            bottomBar = {
                AnnotationToolbar(
                    selectedColor = selectedColor,
                    highlighter = highlighter,
                    eraser = eraser,
                    movingText = movingText,
                    strokeWidth = strokeWidth,
                    onColor = { selectedColor = it },
                    onHighlighter = { highlighter = !highlighter; eraser = false },
                    onEraser = { eraser = !eraser; highlighter = false },
                    onMoveText = { movingText = !movingText; eraser = false; highlighter = false },
                    onStrokeWidth = { strokeWidth = it },
                    onText = { showTextDialog = true }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val scale = minOf(
                        maxWidth.value / image.width,
                        maxHeight.value / image.height
                    )
                    AnnotationCanvas(
                    image = image,
                    actions = actions,
                    currentPoints = currentPoints,
                    selectedColor = selectedColor,
                    highlighter = highlighter,
                    eraser = eraser,
                    movingText = movingText,
                    strokeWidth = strokeWidth,
                    modifier = Modifier.size((image.width * scale).dp, (image.height * scale).dp),
                    pendingText = pendingText,
                    onCurrentPoints = { currentPoints = it },
                    onTextPlaced = { position ->
                        pendingText?.let { text ->
                            undo += actions.toList()
                            actions += AnnotationAction.TextAction(text, position, selectedColor)
                            redo.clear()
                        }
                        pendingText = null
                    },
                    onStrokeFinished = { finishedPoints ->
                        if (finishedPoints.size > 1) {
                            undo += actions.toList()
                            actions += if (eraser) {
                                AnnotationAction.EraserAction(finishedPoints, strokeWidth * 4f)
                            } else {
                                AnnotationAction.StrokeAction(
                                    finishedPoints, selectedColor,
                                    strokeWidth * if (highlighter) 3f else 1f,
                                    if (highlighter) 0.35f else 1f
                                )
                            }
                            redo.clear()
                        }
                        currentPoints = emptyList()
                    },
                    onMoveTextStart = { undo += actions.toList(); redo.clear() },
                    onMoveText = { index, position ->
                        val current = actions.getOrNull(index) as? AnnotationAction.TextAction
                        if (current != null) actions[index] = current.copy(position = position)
                    }
                )
                }
                if (saveError) {
                    Text("Impossible d’enregistrer le dessin.", color = Color.White, modifier = Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }

    if (showTextDialog) {
        AddAnnotationTextDialog(
            onDismiss = { showTextDialog = false },
            onAdd = { text ->
                pendingText = text
                showTextDialog = false
            }
        )
    }
}

@Composable
private fun AnnotationCanvas(
    image: ImageBitmap,
    actions: List<AnnotationAction>,
    currentPoints: List<Offset>,
    selectedColor: Color,
    highlighter: Boolean,
    eraser: Boolean,
    movingText: Boolean,
    strokeWidth: Float,
    modifier: Modifier,
    pendingText: String?,
    onCurrentPoints: (List<Offset>) -> Unit,
    onTextPlaced: (Offset) -> Unit,
    onStrokeFinished: (List<Offset>) -> Unit,
    onMoveTextStart: () -> Unit,
    onMoveText: (Int, Offset) -> Unit
) {
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
                                action != null && (action.position - pos).getDistance() < 0.15f
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
                strokeWidth * if (eraser) 4f else if (highlighter) 3f else 1f,
                if (highlighter) 0.35f else 1f,
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
                textSize = size.minDimension * 0.07f
                setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
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
private fun AnnotationToolbar(
    selectedColor: Color,
    highlighter: Boolean,
    eraser: Boolean,
    movingText: Boolean,
    strokeWidth: Float,
    onColor: (Color) -> Unit,
    onHighlighter: () -> Unit,
    onEraser: () -> Unit,
    onMoveText: () -> Unit,
    onStrokeWidth: (Float) -> Unit,
    onText: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Text("Épaisseur du trait")
        Slider(value = strokeWidth, onValueChange = onStrokeWidth, valueRange = 0.002f..0.025f)
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
            Icon(Icons.Default.Brush, contentDescription = "Surligneur", tint = if (highlighter) Color.Yellow else MaterialTheme.colorScheme.onSurface)
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
                    alpha = (action.alpha * 255).toInt()
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
                    textSize = output.width.coerceAtMost(output.height) * 0.07f
                    setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
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
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
