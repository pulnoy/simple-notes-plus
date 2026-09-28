package dev.dettmer.simplenotes.ui.editor

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

private val AUDIO_REFERENCE_REGEX = Regex(
    """\[audio]\(\.assets/([A-Za-z0-9][A-Za-z0-9._-]*\.(?:m4a|mp4|aac|wav|ogg))\)""",
    RegexOption.IGNORE_CASE
)
private const val RECORDING_TIMER_INTERVAL_MS = 250L
private const val AUDIO_ENCODING_BIT_RATE = 128_000
private const val AUDIO_SAMPLING_RATE = 44_100

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
    var startedAt by remember { mutableStateOf(0L) }
    var elapsedSeconds by remember { mutableStateOf(0L) }

    RecordingTimer(recording, startedAt) { elapsedSeconds = it }

    fun stopAndRelease(): File? {
        val file = outputFile
        val stopped = runCatching { recorder?.stop() }.isSuccess
        runCatching { recorder?.release() }
        recorder = null
        recording = false
        outputFile = null
        if (!isValidRecording(file, stopped, SystemClock.elapsedRealtime() - startedAt)) {
            file?.delete()
            error = "Enregistrement trop court ou interrompu. Réessaie."
            return null
        }
        return file
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recording) runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
            outputFile?.delete()
        }
    }

    AlertDialog(
        onDismissRequest = { if (canDismissRecorder(recording, saving)) onDismiss() },
        icon = { Icon(Icons.Default.Mic, contentDescription = null) },
        title = { Text(if (recording) "Enregistrement en cours…" else "Note audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (recording) "Enregistrement · ${elapsedSeconds} s" else "L’audio sera joint à la note.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !saving,
                    onClick = {
                        if (!recording) {
                            error = null
                            val file = File(context.cacheDir, "audio-${UUID.randomUUID()}.m4a")
                            val next = MediaRecorder()
                            val started = startRecorder(next, file)
                            if (!started) {
                                runCatching { next.release() }
                                file.delete()
                                error = "Impossible de démarrer l’enregistrement."
                            } else {
                                outputFile = file
                                recorder = next
                                startedAt = SystemClock.elapsedRealtime()
                                elapsedSeconds = 0
                                recording = true
                            }
                        } else {
                            val file = stopAndRelease() ?: return@Button
                            saving = true
                            scope.launch {
                                val result = runCatching { store.saveAsset(file.readBytes(), "m4a") }
                                file.delete()
                                saving = false
                                result.onSuccess(onSaved).onFailure { error = "Impossible de joindre l’audio." }
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
private fun RecordingTimer(recording: Boolean, startedAt: Long, onElapsed: (Long) -> Unit) {
    LaunchedEffect(recording) {
        while (recording) {
            onElapsed((SystemClock.elapsedRealtime() - startedAt) / 1000)
            delay(RECORDING_TIMER_INTERVAL_MS)
        }
    }
}

private fun isValidRecording(file: File?, stopped: Boolean, elapsedMs: Long): Boolean {
    if (!stopped || elapsedMs < 1000) return false
    return file != null && file.exists() && file.length() > 0L
}

private fun canDismissRecorder(recording: Boolean, saving: Boolean) = !recording && !saving

private fun startRecorder(recorder: MediaRecorder, file: File): Boolean = runCatching {
    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    recorder.setAudioEncodingBitRate(AUDIO_ENCODING_BIT_RATE)
    recorder.setAudioSamplingRate(AUDIO_SAMPLING_RATE)
    recorder.setOutputFile(file.absolutePath)
    recorder.prepare()
    recorder.start()
}.isSuccess

internal fun insertAudioMarkdown(state: TextFieldState, assetName: String) {
    state.edit {
        val prefix = if (length == 0 || asCharSequence()[length - 1] == '\n') "" else "\n"
        val token = prefix + audioMarkdown(assetName)
        insert(length, token)
        placeCursorAtEnd()
    }
}

internal fun removeAudioMarkdown(state: TextFieldState, assetName: String) {
    val token = audioMarkdown(assetName)
    state.edit {
        val index = asCharSequence().indexOf(token)
        if (index >= 0) replace(index, index + token.length, "")
    }
}

@Composable
fun AudioAttachments(content: String, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
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
            val player = remember(name, file.absolutePath) {
                val candidate = MediaPlayer()
                runCatching {
                    candidate.setDataSource(file.absolutePath)
                    candidate.prepare()
                    candidate.setOnCompletionListener { playing = false }
                    candidate
                }.onFailure { candidate.release() }.getOrNull()
            }
            DisposableEffect(player) { onDispose { player?.release() } }
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(enabled = player != null, onClick = {
                        val success = runCatching { if (playing) player?.pause() else player?.start() }.isSuccess
                        if (success) playing = !playing
                    }) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Lire"
                        )
                    }
                    Column {
                        Text("Note audio", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (player == null) "Lecture indisponible" else "${(player.duration / 1000)} s",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { onRemove(name) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer l’audio")
                    }
                }
            }
        }
    }
}
