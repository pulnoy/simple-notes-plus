package dev.dettmer.simplenotes.ui.main

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.models.ChecklistItem
import dev.dettmer.simplenotes.models.Note
import dev.dettmer.simplenotes.models.NoteType
import dev.dettmer.simplenotes.storage.FolderStore
import dev.dettmer.simplenotes.storage.NotesStorage
import dev.dettmer.simplenotes.utils.Constants
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

/** Runs only against the separate debug installation in the configured Android emulator. */
class SunnyHomeTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var model: MainViewModel

    @Before fun prepareHome() {
        prepareMediaFixtures()
        runBlocking {
            FolderStore(app).addFolder("Personnel")
            val store = NotesStorage(app)
            store.saveNote(Note(id = "sunny-demo-pinned", title = "Idées du week-end",
                content = "- Balade au bord de l’eau\n- Tester une nouvelle recette", deviceId = "ui-test",
                isPinned = true, folderName = "Personnel"))
            store.saveNote(Note(id = "sunny-demo-checklist", title = "Courses", content = "", deviceId = "ui-test",
                noteType = NoteType.CHECKLIST, checklistItems = listOf(
                    ChecklistItem(text = "Pain", isChecked = true), ChecklistItem(text = "Tomates", isChecked = true),
                    ChecklistItem(text = "Café")
                )))
            store.saveNote(Note(id = "sunny-demo-photo", title = "Souvenirs", content = "![photo](.assets/sunny-demo.jpg)",
                deviceId = "ui-test", folderName = "Personnel"))
            store.saveNote(Note(id = "sunny-demo-audio", title = "Mémo vocal", content = "[audio](.assets/sunny-demo.wav)",
                deviceId = "ui-test"))
        }
        model = MainViewModel(app)
        compose.setContent { MaterialTheme { MainScreen(model, {}, {}, { _, _ -> }) } }
        compose.waitUntil(15_000) { model.isReady.value && model.sortedNotesUnfoldered.value.size >= 4 }
    }

    @Test fun homeShowsMediaAndFoldersAndSupportsFolderLifecycle() {
        compose.onNodeWithText(app.getString(R.string.home_all_notes)).assertIsDisplayed()
        compose.onNodeWithTag("home_folder_Personnel").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.home_photo_preview)).assertIsDisplayed()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("0:04").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onRoot().captureToImage().asAndroidBitmap().let { image ->
            File(app.cacheDir, "sunny-home.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithContentDescription(app.getString(R.string.home_play_audio)).performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription(app.getString(R.string.home_pause_audio)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(app.getString(R.string.home_pause_audio)).performClick()
        compose.onNodeWithTag("home_folder_Personnel").performClick()
        compose.onNodeWithText("Courses").assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.home_all_notes)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.fab_create_folder)).performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("Essai dossier")
        compose.onNodeWithText(app.getString(R.string.folder_create_action)).performClick()
        compose.waitUntil(10_000) { model.folders.value.any { it.name == "Essai dossier" } }
        runBlocking {
            NotesStorage(app).saveNote(Note(id = "sunny-demo-folder-lifecycle", title = "Note à conserver",
                content = "Cette note doit survivre à la suppression du dossier.", deviceId = "ui-test",
                folderName = "Essai dossier"))
        }
        model.loadNotes(forceReload = true)
        compose.waitUntil(10_000) { model.notes.value.any { it.id == "sunny-demo-folder-lifecycle" } }
        compose.onNodeWithContentDescription(app.getString(R.string.home_folder_menu, "Essai dossier")).performClick()
        compose.onNodeWithText(app.getString(R.string.home_rename)).performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("Essai renommé")
        compose.onNodeWithText(app.getString(R.string.folder_rename_action)).performClick()
        compose.waitUntil(10_000) { model.folders.value.any { it.name == "Essai renommé" } }
        compose.waitUntil(10_000) { model.notes.value.any {
            it.id == "sunny-demo-folder-lifecycle" && it.folderName == "Essai renommé"
        } }
        compose.onNodeWithContentDescription(app.getString(R.string.home_folder_menu, "Essai renommé")).performClick()
        compose.onNodeWithText(app.getString(R.string.home_delete_folder)).performClick()
        compose.onNode(hasText(app.getString(R.string.home_delete_folder)) and hasClickAction()).performClick()
        compose.waitUntil(10_000) { model.folders.value.none { it.name == "Essai renommé" } }
        compose.waitUntil(10_000) { model.notes.value.any {
            it.id == "sunny-demo-folder-lifecycle" && it.folderName == null
        } }
        model.selectAllNotes()
        assertTrue(model.selectedNotes.value.contains("sunny-demo-pinned"))
        assertTrue(model.selectedNotes.value.contains("sunny-demo-folder-lifecycle"))
        assertTrue(model.selectedFolders.value.isEmpty())
    }

    @Test fun listModeAlsoShowsLocalMediaPreviews() {
        val prefs = app.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val previousMode = prefs.getString(Constants.KEY_DISPLAY_MODE, Constants.DEFAULT_DISPLAY_MODE)
        try {
            prefs.edit().putString(Constants.KEY_DISPLAY_MODE, "list").apply()
            model.refreshDisplayMode()
            model.setSearchQuery("Mémo vocal")
            compose.waitUntil(10_000) { model.sortedNotesUnfoldered.value.size == 1 }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("0:04").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription(app.getString(R.string.home_play_audio)).assertIsDisplayed()
            model.setSearchQuery("Souvenirs")
            compose.waitUntil(10_000) { model.sortedNotesUnfoldered.value.singleOrNull()?.title == "Souvenirs" }
            compose.onNodeWithContentDescription(app.getString(R.string.home_photo_preview)).assertIsDisplayed()
        } finally {
            prefs.edit().putString(Constants.KEY_DISPLAY_MODE, previousMode).apply()
            model.refreshDisplayMode()
        }
    }

    /** Valid local media fixtures; never ships sample notes or media in the release APK. */
    private fun prepareMediaFixtures() {
        val directory = File(app.filesDir, "assets").apply { mkdirs() }
        val photo = File(directory, "sunny-demo.jpg")
        if (!photo.exists()) {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.GREEN)
            photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
        }
        val audio = File(directory, "sunny-demo.wav")
        if (!audio.exists()) {
            val pcmBytes = 64_000
            val buffer = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVEfmt ".toByteArray()).putInt(16)
            buffer.putShort(1).putShort(1).putInt(8_000).putInt(16_000).putShort(2).putShort(16)
            buffer.put("data".toByteArray()).putInt(pcmBytes)
            audio.writeBytes(buffer.array())
        }
    }
}
