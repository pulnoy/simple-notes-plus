package dev.dettmer.simplenotes.sync.drive

import dev.dettmer.simplenotes.models.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveMergePlannerTest {
    private fun note(content: String, id: String = "n1") = Note(
        id = id,
        title = "Note",
        content = content,
        deviceId = "A",
        createdAt = 1L,
        updatedAt = 1L
    )

    @Test fun `first device publishes and second device downloads`() {
        val first = DriveMergePlanner.merge("A", listOf(note("hello")), emptyList(), DriveLocalState())
        assertEquals(1, first.versionsToPublish.size)
        val second = DriveMergePlanner.merge(
            "B", emptyList(),
            listOf(DriveSnapshot(deviceId = "A", versions = first.versionsToPublish)),
            DriveLocalState()
        )
        assertEquals("hello", second.notesToSave.single().content)
        assertEquals(0, second.conflictCount)
    }

    @Test fun `concurrent edits preserve both versions with a stable conflict copy`() {
        val base = DriveMergePlanner.merge("A", listOf(note("base")), emptyList(), DriveLocalState())
        val remote = DriveMergePlanner.merge(
            "B", listOf(note("B edit")),
            listOf(DriveSnapshot(deviceId = "A", versions = base.versionsToPublish)),
            base.nextState
        )
        val local = DriveMergePlanner.merge(
            "A", listOf(note("A edit")),
            listOf(DriveSnapshot(deviceId = "B", versions = remote.versionsToPublish)),
            base.nextState
        )
        assertEquals(1, local.conflictCount)
        assertTrue(local.versionsToPublish.any { it.note?.title?.endsWith("(conflit)") == true })
        val again = DriveMergePlanner.merge(
            "A", local.versionsToPublish.mapNotNull { it.note },
            listOf(DriveSnapshot(deviceId = "A", versions = local.versionsToPublish)),
            local.nextState
        )
        assertEquals(0, again.conflictCount)
        assertEquals(local.versionsToPublish.map { it.id }.toSet(), again.versionsToPublish.map { it.id }.toSet())
    }

    @Test fun `deletion remains a tombstone and removes an unchanged remote note`() {
        val first = DriveMergePlanner.merge("A", listOf(note("base")), emptyList(), DriveLocalState())
        val deleted = DriveMergePlanner.merge(
            "A", emptyList(),
            listOf(DriveSnapshot(deviceId = "A", versions = first.versionsToPublish)),
            first.nextState
        )
        assertEquals(null, deleted.versionsToPublish.single().note)
        val other = DriveMergePlanner.merge(
            "B", listOf(note("base")),
            listOf(DriveSnapshot(deviceId = "A", versions = deleted.versionsToPublish)),
            first.nextState
        )
        assertTrue("n1" in other.idsToDelete)
    }

    @Test fun `excluded local note is neither uploaded nor deleted`() {
        val first = DriveMergePlanner.merge("A", listOf(note("base")), emptyList(), DriveLocalState())
        val excluded = DriveMergePlanner.merge(
            "A", emptyList(),
            listOf(DriveSnapshot(deviceId = "A", versions = first.versionsToPublish)),
            first.nextState,
            excludedIds = setOf("n1")
        )
        assertFalse(excluded.idsToDelete.contains("n1"))
        assertTrue(excluded.versionsToPublish.isEmpty())
    }
}
