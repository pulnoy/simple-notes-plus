package dev.dettmer.simplenotes.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchTest {
    private fun note(content: String = "", title: String = "", folder: String? = null) =
        Note(title = title, content = content, deviceId = "test", folderName = folder)

    @Test fun `search ignores accents case and word order across title and body`() {
        val match = note("Rendez-vous à préparer", "École")
        assertEquals(listOf(match), NoteSearch.search(listOf(match, note("rendez-vous")), "PREPARER ecole"))
    }

    @Test fun `search includes folders and checklist items`() {
        val match = note(folder = "Vacances").copy(
            noteType = NoteType.CHECKLIST,
            checklistItems = listOf(ChecklistItem(text = "Réserver le train"))
        )
        assertEquals(listOf(match), NoteSearch.search(listOf(match), "vacances reserver"))
    }

    @Test fun `mixed attachments match both media filters without changing note type`() {
        val mixed = note("![size=100](.assets/drawing.png)\n[audio](.assets/voice.m4a)")
        assertTrue(NoteSearch.matchesType(mixed, NoteFilter.IMAGE_ONLY))
        assertTrue(NoteSearch.matchesType(mixed, NoteFilter.AUDIO_ONLY))
        assertTrue(NoteSearch.matchesType(mixed, NoteFilter.TEXT_ONLY))
        assertFalse(NoteSearch.matchesType(note("audio as plain text"), NoteFilter.AUDIO_ONLY))
        assertFalse(NoteSearch.matchesType(note("[link](.assets/document.pdf)"), NoteFilter.IMAGE_ONLY))
    }

    @Test fun `blank search keeps all notes`() {
        val notes = listOf(note("one"), note("two"))
        assertEquals(notes, NoteSearch.search(notes, "  "))
    }
}
