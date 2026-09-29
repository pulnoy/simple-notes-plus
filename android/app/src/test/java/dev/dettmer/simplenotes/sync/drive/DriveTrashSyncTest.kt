package dev.dettmer.simplenotes.sync.drive

import dev.dettmer.simplenotes.models.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Simulates sequential exchanges of the exact snapshots used by two installations. */
class DriveTrashSyncTest {
    @Test fun `trash restore and permanent deletion converge on another device`() {
        val note = Note(id = "note", title = "Keep me", content = "Body and [audio](.assets/a.m4a)", deviceId = "A")
        val base = DriveMergePlanner.merge("A", listOf(note), emptyList(), DriveLocalState())
        val trashed = note.copy(trashedAt = 1000L, updatedAt = note.updatedAt + 1L)
        val trashA = DriveMergePlanner.merge("A", listOf(trashed), listOf(snapshot("A", base)), base.nextState)
        val trashB = DriveMergePlanner.merge("B", listOf(note), listOf(snapshot("A", trashA)), base.nextState)
        val onB = trashB.notesToSave.single()
        assertTrue(onB.isTrashed)
        assertEquals(note.content, onB.content)

        val restored = onB.copy(trashedAt = null, updatedAt = onB.updatedAt + 1L)
        val restoredB = DriveMergePlanner.merge("B", listOf(restored), listOf(snapshot("A", trashA)), trashB.nextState)
        val restoredA = DriveMergePlanner.merge("A", listOf(trashed), listOf(snapshot("B", restoredB)), trashA.nextState)
        assertEquals(null, restoredA.notesToSave.single().trashedAt)
        assertEquals(0, restoredA.conflictCount)

        val purgedA = DriveMergePlanner.merge("A", emptyList(), listOf(snapshot("B", restoredB)), restoredA.nextState)
        val purgedB = DriveMergePlanner.merge("B", listOf(restored), listOf(snapshot("A", purgedA)), restoredB.nextState)
        assertTrue(note.id in purgedB.idsToDelete)
        assertEquals(null, purgedB.versionsToPublish.single().note)
    }

    private fun snapshot(device: String, plan: DriveMergePlan) =
        DriveSnapshot(deviceId = device, versions = plan.versionsToPublish)
}
