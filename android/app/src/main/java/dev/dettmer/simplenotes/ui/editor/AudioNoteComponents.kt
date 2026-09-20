package dev.dettmer.simplenotes.ui.editor

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.dettmer.simplenotes.storage.AssetStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch

private val AUDIO_REFERENCE_REGEX = Regex(
    """\[audio]\(\.assets/([A-Za-z0-9][A-Za-z0-9._-]*\.(?:m4a|mp4|aac|wav|ogg))\)""",
    RegexOption.IGNORE_CASE
)

fun audioAssetNames(content: String): List<String> =
    AUDIO_REFERENCE_REGEX.findAll(content).map { it.groupValues[1] }.distinct().toList()

fun audioMarkdown(assetName: String): String = "[audio](.assets/$assetName)"

@Suppress("DEPRECATION")
@Composable
fun AudioRecorderDialog(onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { AssetStore(context) }
    val scope = rememberCoroutineScope()
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun stopAndRelease(): File? {
        val file = outputFile
        runCatching { recorder?.stop() }.onFailure { error = "Enregistrement trop court" }
        recorder?.release()
        recorder = null
        recording = false
        return file?.takeIf { it.exists() && it.length() > 0L }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recording) runCatching { recorder?.stop() }
            recorder?.release()
            outputFile?.delete()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!recording && !saving) onDismiss() },
        icon = { Icon(Icons.Default.Mic, contentDescription = null) },
        title = { Text(if (recording) "Enregistrement en cours…" else "Note audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (recording) "Parle normalement, puis touche Arrêter." else "L’audio sera joint à la note.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !saving,
                    onClick = {
                        if (!recording) {
                            error = null
                            val file = File(context.cacheDir, "audio-${UUID.randomUUID()}.m4a")
                            val next = MediaRecorder().apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                setAudioEncodingBitRate(128_000)
                                setAudioSamplingRate(44_100)
                                setOutputFile(file.absolutePath)
                                prepare()
                                start()
                            }
                            outputFile = file
                            recorder = next
                            recording = true
                        } else {
                            val file = stopAndRelease() ?: return@Button
                            saving = true
                            scope.launch {
                                val name = store.saveAsset(file.readBytes(), "m4a")
                                file.delete()
                                saving = false
                                onSaved(name)
                            }
                        }
                    }
                ) {
                    Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                    Text(if (recording) " Arrêter et joindre" else " Enregistrer")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(enabled = !saving, onClick = {
                if (recording) stopAndRelease()?.delete()
                onDismiss()
            }) { Text("Annuler") }
        }
    )
}

@Composable
fun AudioAttachments(content: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { AssetStore(context) }
    val names = remember(content) { audioAssetNames(content) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEach { name ->
            val file = remember(name) { store.getAssetFile(name) }
            if (!file.exists()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Audio en attente de synchronisation", modifier = Modifier.padding(16.dp))
                }
                return@forEach
            }
            var playing by remember(name) { mutableStateOf(false) }
            val player = remember(name) {
                MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener { playing = false }
                }
            }
            DisposableEffect(player) { onDispose { player.release() } }
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (playing) player.pause() else player.start()
                        playing = !playing
                    }) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Lire"
                        )
                    }
                    Column {
                        Text("Note audio", style = MaterialTheme.typography.titleSmall)
                        Text(name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
