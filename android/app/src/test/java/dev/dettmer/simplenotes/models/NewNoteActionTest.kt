package dev.dettmer.simplenotes.models

import org.junit.Assert.assertEquals
import org.junit.Test

class NewNoteActionTest {
    @Test fun `creation actions preserve menu order and note types`() {
        assertEquals(
            listOf("IMAGE", "DRAWING", "AUDIO", "CHECKLIST", "TEXT"),
            NewNoteAction.entries.map { it.name }
        )
        assertEquals(NoteType.CHECKLIST, NewNoteAction.CHECKLIST.noteType)
        listOf(NewNoteAction.IMAGE, NewNoteAction.DRAWING, NewNoteAction.AUDIO, NewNoteAction.TEXT)
            .forEach { assertEquals(NoteType.TEXT, it.noteType) }
    }
}
