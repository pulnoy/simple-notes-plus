package dev.dettmer.simplenotes.ui.main.components

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.storage.AssetStore
import dev.dettmer.simplenotes.ui.theme.SunnyColors
import dev.dettmer.simplenotes.sync.SyncEventBus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext


@Composable
private fun attachmentFile(name: String): Pair<File, Long> {
    val context = LocalContext.current
    val file = remember(context, name) { AssetStore(context).getAssetFile(name) }
    val revision by produceState(0L, name) {
        SyncEventBus.events.collect { value += 1L }
    }
    return file to revision
}

@Composable
fun HomePhotoPreview(name: String) {
    val (file, revision) = attachmentFile(name)
    var failed by remember(name, revision) { mutableStateOf(false) }
    if (!file.exists() || failed) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Image, null)
                Text(stringResource(R.string.home_photo_pending), style = MaterialTheme.typography.labelSmall)
            }
        }
    } else {
        androidx.compose.runtime.key(name, revision) {
            AsyncImage(model = file, contentDescription = stringResource(R.string.home_photo_preview),
                contentScale = ContentScale.Crop, onError = { failed = true },
                modifier = Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(14.dp)))
        }
    }
}

@Composable
fun HomeAudioPreview(name: String) {
    val (file, revision) = attachmentFile(name)
    val duration by produceState<Long?>(null, name, revision) {
        value = withContext(Dispatchers.IO) { readAudioDuration(file) }
    }
    val current by HomeAudioPlayer.playing.collectAsState()
    val failure by HomeAudioPlayer.failed.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(name, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) HomeAudioPlayer.stop(name)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); HomeAudioPlayer.stop(name) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(onClick = { HomeAudioPlayer.toggle(name, file) }, enabled = duration != null,
            shape = CircleShape, color = SunnyColors.Yellow,
            contentColor = SunnyColors.Ink) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(if (current == name) Icons.Default.Pause else Icons.Default.PlayArrow,
                    stringResource(if (current == name) R.string.home_pause_audio else R.string.home_play_audio))
            }
        }
        AudioVisualIndicator(Modifier.weight(1f))
        Text(duration?.let(HomeAttachments::durationLabel) ?: "—:—", style = MaterialTheme.typography.labelMedium)
    }
    if (duration == null || failure == name) {
        Text(stringResource(if (!file.exists()) R.string.home_audio_pending else R.string.home_audio_unavailable),
            style = MaterialTheme.typography.labelSmall)
    }
}

/** Decorative audio indicator, not a claim about the recording's actual amplitudes. */
@Composable
private fun AudioVisualIndicator(modifier: Modifier) {
    val color = SunnyColors.Yellow
    Canvas(modifier.height(32.dp)) {
        val heights = listOf(0.2f, 0.45f, 0.7f, 0.4f, 0.9f, 0.6f, 1f, 0.45f, 0.75f, 0.3f, 0.6f, 0.25f)
        val step = size.width / heights.size
        heights.forEachIndexed { index, fraction ->
            val x = step * (index + 0.5f)
            val halfHeight = size.height * fraction / 2f
            drawLine(color, Offset(x, center.y - halfHeight), Offset(x, center.y + halfHeight),
                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

private fun readAudioDuration(file: File): Long? {
    if (!file.isFile) return null
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } finally { retriever.release() }
    }.getOrNull()
}

/** A single owner prevents two cards from playing at the same time. */
private object HomeAudioPlayer {
    val playing = MutableStateFlow<String?>(null)
    val failed = MutableStateFlow<String?>(null)
    private var player: MediaPlayer? = null
    private var owner: String? = null
    private var prepared = false

    fun toggle(name: String, file: File) {
        if (owner == name) {
            if (prepared) runCatching {
                if (playing.value == name) { player?.pause(); playing.value = null }
                else { player?.start(); playing.value = name }
            }.onFailure { failed.value = name; stop(name) }
            return
        }
        release()
        owner = name
        failed.value = null
        val candidate = MediaPlayer()
        player = candidate
        candidate.setOnPreparedListener {
            if (owner == name) runCatching {
                prepared = true; it.start(); playing.value = name
            }.onFailure { failed.value = name; stop(name) }
        }
        candidate.setOnCompletionListener { stop(name) }
        candidate.setOnErrorListener { _, _, _ -> failed.value = name; stop(name); true }
        runCatching {
            candidate.setDataSource(file.absolutePath)
            candidate.prepareAsync()
        }.onFailure { failed.value = name; stop(name) }
    }

    fun stop(name: String) { if (owner == name) release() }

    private fun release() {
        player?.release()
        player = null
        owner = null
        prepared = false
        playing.value = null
    }
}
