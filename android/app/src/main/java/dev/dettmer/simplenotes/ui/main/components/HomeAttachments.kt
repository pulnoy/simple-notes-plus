package dev.dettmer.simplenotes.ui.main.components

import dev.dettmer.simplenotes.ui.editor.audioAssetNames
import java.util.Locale

/** Only local, validated attachment paths are allowed; never requests remote image URLs. */
object HomeAttachments {
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private val imagePattern = Regex("""!\[[^\]]*]\(\.assets/([A-Za-z0-9][A-Za-z0-9._-]*)\)""")
    private val localAttachmentPattern = Regex(
        """(?:!\[[^\]]*]|\[audio])\(\.assets/[A-Za-z0-9][A-Za-z0-9._-]*\)""", RegexOption.IGNORE_CASE
    )
    fun firstImage(content: String): String? = imagePattern.find(content)?.groupValues?.get(1)
    fun firstAudio(content: String): String? = audioAssetNames(content).firstOrNull()
    fun previewText(content: String): String = localAttachmentPattern.replace(content, "").trim()
    fun durationLabel(milliseconds: Long): String {
        val seconds = milliseconds.coerceAtLeast(0L) / MILLIS_PER_SECOND
        return String.format(Locale.ROOT, "%d:%02d", seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }
}
