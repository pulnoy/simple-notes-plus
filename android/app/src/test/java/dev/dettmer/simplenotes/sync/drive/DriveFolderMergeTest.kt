package dev.dettmer.simplenotes.sync.drive

import com.google.gson.Gson
import dev.dettmer.simplenotes.storage.FolderMeta
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveFolderMergeTest {
    @Test fun `snapshot transport and second device merge preserve all folder metadata`() {
        val root = FolderMeta("Personal", color = "#FFF475", updatedAt = 10, icon = "home", order = 1)
        val child = FolderMeta("Trips", color = "#AECBFA", updatedAt = 10, icon = "travel", parentName = "Personal", order = 0)
        val json = Gson().toJson(DriveSnapshot(deviceId = "A", versions = emptyList(), folders = listOf(root, child)))
        val transported = Gson().fromJson(json, DriveSnapshot::class.java)
        val second = mergeDriveFolderMetadata(listOf(transported), listOf(child.copy(icon = null, updatedAt = 1)), emptySet())
        assertEquals(setOf(root, child), second.toSet())
        val changed = child.copy(color = "#CCFF90", order = 2, updatedAt = 20)
        val backToFirst = mergeDriveFolderMetadata(
            listOf(DriveSnapshot(deviceId = "B", versions = emptyList(), folders = listOf(changed))),
            listOf(root, child), emptySet()
        )
        assertEquals(changed, backToFirst.first { it.name == "Trips" })
    }

    @Test fun `local only folder cannot be overwritten and tombstone wins equal revisions`() {
        val local = FolderMeta("Private", updatedAt = 1, icon = "star", parentName = "Root")
        val remote = local.copy(updatedAt = 100, deleted = true)
        val snapshot = DriveSnapshot(deviceId = "B", versions = emptyList(), folders = listOf(remote))
        assertEquals(local, mergeDriveFolderMetadata(listOf(snapshot), listOf(local), setOf("Private")).single())
        assertEquals(remote, mergeDriveFolderMetadata(listOf(snapshot), listOf(remote.copy(deleted = false)), emptySet()).single())
    }
}
