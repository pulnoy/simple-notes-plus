package dev.dettmer.simplenotes.ui.main

import dev.dettmer.simplenotes.models.Folder
import dev.dettmer.simplenotes.models.childrenOf
import dev.dettmer.simplenotes.models.folderTree
import dev.dettmer.simplenotes.models.subtreeNames
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderHierarchyTest {
    private val folders = listOf(
        Folder("Root", order = 1), Folder("First", order = 0),
        Folder("Child", parentName = "Root"), Folder("Grandchild", parentName = "Child"),
        Folder("Orphan", parentName = "Missing")
    )

    @Test fun `parent view lists only its immediate children`() {
        assertEquals(listOf("First", "Root", "Orphan"), folders.childrenOf(null).map { it.name })
        assertEquals(listOf("Child"), folders.childrenOf("Root").map { it.name })
    }

    @Test fun `deletion and sync exclusions include the whole subtree`() {
        assertEquals(setOf("Root", "Child", "Grandchild"), folders.subtreeNames(setOf("Root")))
        assertEquals(setOf("Child", "Grandchild"), folders.subtreeNames(setOf("Child")))
    }

    @Test fun `tree follows sibling order and displays each folder once even in corrupt cycles`() {
        assertEquals(listOf("First" to 0, "Root" to 0, "Child" to 1, "Grandchild" to 2, "Orphan" to 0),
            folders.folderTree().map { it.first.name to it.second })
        val corrupt = listOf(Folder("A", parentName = "B"), Folder("B", parentName = "A"))
        assertEquals(setOf("A", "B"), corrupt.subtreeNames(setOf("A")))
        assertEquals(2, corrupt.folderTree().size)
    }
}
