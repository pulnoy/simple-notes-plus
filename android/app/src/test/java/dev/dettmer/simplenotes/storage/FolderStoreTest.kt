package dev.dettmer.simplenotes.storage

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FolderStoreTest {
    @Test fun `hierarchy style and order survive reload rename and undo`() = runBlocking {
        store.addFolder("Work")
        store.addFolder("Personal")
        store.addFolder("Projects", parentName = "Work")
        store.addFolder("Archive", parentName = "Work")
        store.addFolder("Ideas", parentName = "Projects")
        store.setAppearance("Work", "work", "#FFF475")
        store.setAppearance("Projects", "star", "#AECBFA")
        store.moveFolder("Work", -1)
        store.moveFolder("Projects", -1)
        var folders = store.loadFolders()
        assertEquals(listOf("Work", "Personal"), folders.filter { it.parentName == null }.map { it.name })
        assertEquals(listOf("Projects", "Archive"), folders.filter { it.parentName == "Work" }.map { it.name })
        store.rename("Work", "Office")
        folders = store.loadFolders()
        assertEquals("work", folders.first { it.name == "Office" }.icon)
        assertEquals("#FFF475", folders.first { it.name == "Office" }.color)
        assertEquals("Office", folders.first { it.name == "Projects" }.parentName)
        assertEquals("Projects", folders.first { it.name == "Ideas" }.parentName)
        val snapshot = folders.filter { it.name != "Personal" }
        snapshot.forEach { store.deleteFolder(it.name) }
        store.restoreFolders(snapshot)
        assertEquals(snapshot.toSet(), store.loadFolders().filter { it.name != "Personal" }.toSet())
        assertTrue(store.loadMeta().first { it.name == "Work" }.deleted)
    }

    @Test fun `old metadata does not invent order or parent and discovery preserves customization`() = runBlocking {
        File(tmpDir, FolderStore.FILE_NAME).writeText("""[{"name":"Legacy","updatedAt":1}]""")
        val legacy = store.loadFolders().single()
        assertNull(legacy.parentName)
        assertNull(legacy.icon)
        assertNull(legacy.order)
        store.setAppearance("Legacy", "home", "#FBBC04")
        store.addFolders(listOf("Legacy", "Discovered"))
        assertEquals("home", store.loadFolders().first { it.name == "Legacy" }.icon)
    }

    @Test fun `reordering never crosses a parent and bounds are no ops`() = runBlocking {
        store.addFolder("Root")
        store.addFolder("Child", parentName = "Root")
        val before = store.loadMeta()
        store.moveFolder("Child", -1)
        store.moveFolder("Root", 1)
        assertEquals(before, store.loadMeta())
    }
    private lateinit var tmpDir: File
    private lateinit var store: FolderStore
    private lateinit var prefs: SharedPreferences

    @Before fun setUp() {
        tmpDir = Files.createTempDirectory("folderstore-test").toFile()
        prefs = mockk<SharedPreferences>(relaxed = true) {
            every { edit() } returns mockk(relaxed = true)
        }
        val context = mockk<Context> {
            every { filesDir } returns tmpDir
            every { getSharedPreferences(any(), any()) } returns prefs
        }
        store = FolderStore(context)
    }

    @After fun tearDown() {
        tmpDir.deleteRecursively()
    }

    @Test fun `add and load deduplicates case-insensitively`() = runBlocking {
        store.addFolder("Rezepte")
        store.addFolder("rezepte")
        store.addFolder("Arbeit")
        val folders = store.load()
        assertEquals(2, folders.size)
        assertTrue(folders.any { it.equals("Rezepte", ignoreCase = true) })
        assertTrue(folders.contains("Arbeit"))
    }

    @Test fun `addFolders bulk merges new only`() = runBlocking {
        store.addFolder("A")
        store.addFolders(listOf("A", "B", "C"))
        assertEquals(setOf("A", "B", "C"), store.load().toSet())
    }

    @Test fun `addFolders does not overwrite existing color`() = runBlocking {
        store.addFolder("A")
        store.setColor("A", "#FF0000")
        // addFolders should not touch existing entry
        store.addFolders(listOf("A"))
        val meta = store.loadMeta().first { it.name == "A" }
        assertEquals("#FF0000", meta.color)
    }

    @Test fun `deleteFolder creates tombstone`() = runBlocking {
        store.addFolders(listOf("A", "B"))
        store.deleteFolder("A")
        // load() hides tombstones
        assertEquals(listOf("B"), store.load())
        // loadMeta() shows tombstone
        val meta = store.loadMeta()
        val aEntry = meta.first { it.name.equals("A", ignoreCase = true) }
        assertTrue(aEntry.deleted)
    }

    @Test fun `setColor stores color and updatedAt`() = runBlocking {
        store.addFolder("Foo")
        store.setColor("Foo", "#123456")
        val folders = store.loadFolders()
        val foo = folders.first { it.name == "Foo" }
        assertEquals("#123456", foo.color)
    }

    @Test fun `loadFolders returns color`() = runBlocking {
        store.addFolder("Colored")
        store.setColor("Colored", "#AABBCC")
        val result = store.loadFolders()
        assertEquals("#AABBCC", result.first().color)
    }

    @Test fun `rename tombstones old and creates new with old color`() = runBlocking {
        store.addFolder("Old")
        store.setColor("Old", "#FF0000")
        store.rename("Old", "New")
        // Old is gone from visible list
        assertTrue(store.load().contains("New"))
        assertFalse(store.load().contains("Old"))
        // Old is tombstoned in meta
        val oldMeta = store.loadMeta().first { it.name.equals("Old", ignoreCase = true) }
        assertTrue(oldMeta.deleted)
        // New inherits color
        val newFolder = store.loadFolders().first { it.name == "New" }
        assertEquals("#FF0000", newFolder.color)
    }

    @Test fun `load corrupt json returns empty`() = runBlocking {
        File(tmpDir, FolderStore.FILE_NAME).writeText("{not json")
        assertTrue(store.load().isEmpty())
    }

    @Test fun `empty name is ignored`() = runBlocking {
        store.addFolder("   ")
        assertFalse(File(tmpDir, FolderStore.FILE_NAME).exists())
    }

    @Test fun `backward compat reads old string array format`() = runBlocking {
        // Write old format
        File(tmpDir, FolderStore.FILE_NAME).writeText("[\"Alpha\",\"Beta\"]")
        val folders = store.load()
        assertEquals(setOf("Alpha", "Beta"), folders.toSet())
        // Meta should have updatedAt=0
        val meta = store.loadMeta()
        assertTrue(meta.all { it.updatedAt == 0L })
    }

    @Test fun `loadMeta includes tombstones`() = runBlocking {
        store.addFolder("Keep")
        store.addFolder("Delete")
        store.deleteFolder("Delete")
        val meta = store.loadMeta()
        assertEquals(2, meta.size)
        assertTrue(meta.any { it.name == "Delete" && it.deleted })
        assertTrue(meta.any { it.name == "Keep" && !it.deleted })
    }

    @Test fun `setColor on unknown folder is no-op`() = runBlocking {
        store.setColor("NonExistent", "#000000")
        assertTrue(store.loadMeta().isEmpty())
    }

    @Test fun `addFolder reactivates tombstone`() = runBlocking {
        store.addFolder("A")
        store.deleteFolder("A")
        assertTrue(store.load().isEmpty())
        store.addFolder("A")
        assertEquals(listOf("A"), store.load())
        val meta = store.loadMeta().first { it.name == "A" }
        assertFalse(meta.deleted)
    }

    @Test fun `loadFolders returns null color when not set`() = runBlocking {
        store.addFolder("NoColor")
        val folder = store.loadFolders().first()
        assertNull(folder.color)
    }

    @Test fun `clear removes folders and local-only markers`() = runBlocking {
        store.addFolders(listOf("A", "B"))
        store.setLocalOnly("A", true)
        assertTrue(store.load().isNotEmpty())

        store.clear()

        assertTrue(store.load().isEmpty())
        assertFalse(store.isLocalOnly("A"))
    }

    @Test fun `FolderMeta uses stable JSON wire keys`() {
        val json = com.google.gson.Gson().toJson(
            FolderMeta(name = "Arbeit", color = "#FF0000", updatedAt = 42L, deleted = true)
        )
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"color\""))
        assertTrue(json.contains("\"updatedAt\""))
        assertTrue(json.contains("\"deleted\""))
    }
}
