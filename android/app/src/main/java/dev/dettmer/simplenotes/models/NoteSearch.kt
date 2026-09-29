package dev.dettmer.simplenotes.models

import java.text.Normalizer
import java.util.Locale

/** Shared by the result list and filter counts. Media remain attachments, not new note formats. */
object NoteSearch {
    private val image = Regex("""!\[[^\]]*]\(\.assets/[^)]+\)""")
    private val audio = Regex("""\[audio]\(\.assets/[^)]+\)""", RegexOption.IGNORE_CASE)
    private val accents = Regex("\\p{M}+")

    fun matchesType(note: Note, filter: NoteFilter): Boolean = when (filter) {
        NoteFilter.ALL -> true
        NoteFilter.TEXT_ONLY -> note.noteType == NoteType.TEXT
        NoteFilter.CHECKLIST_ONLY -> note.noteType == NoteType.CHECKLIST
        NoteFilter.IMAGE_ONLY -> image.containsMatchIn(note.content)
        NoteFilter.AUDIO_ONLY -> audio.containsMatchIn(note.content)
    }

    fun search(notes: List<Note>, query: String): List<Note> {
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return notes
        return notes.filter { note ->
            val text = normalize(
                listOf(note.title, note.content, note.folderName.orEmpty())
                    .plus(note.checklistItems.orEmpty().map { it.text })
                    .plus(note.labels.orEmpty()).joinToString(" ")
            )
            words.all { it in text }
        }
    }

    private fun normalize(text: String): String =
        accents.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)
}
