package dev.dettmer.simplenotes.ui.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.dettmer.simplenotes.R
import dev.dettmer.simplenotes.backup.BackupManager
import dev.dettmer.simplenotes.backup.RestoreMode
import dev.dettmer.simplenotes.images.ImageCompressionMode
import dev.dettmer.simplenotes.images.ImageProcessor
import dev.dettmer.simplenotes.ui.editor.NoteEditorViewModel
import dev.dettmer.simplenotes.ui.editor.components.WordCounterVisibility
import dev.dettmer.simplenotes.ui.theme.ColorTheme
import dev.dettmer.simplenotes.ui.theme.FontSizeScale
import dev.dettmer.simplenotes.ui.theme.NotePreviewLength
import dev.dettmer.simplenotes.ui.theme.SimpleNotesTheme
import dev.dettmer.simplenotes.ui.theme.ThemeMode
import dev.dettmer.simplenotes.ui.theme.ThemePreferences
import dev.dettmer.simplenotes.utils.Constants
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Runs only against the separate debug application, never the production app's data. */
class SettingsRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val prefs get() = app.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    @Test fun settingsSurviveReopeningAndAreReadByEditor() {
        lateinit var restored: SettingsViewModel
        compose.runOnUiThread {
            val settings = SettingsViewModel(app)
            settings.setOfflineMode(true)
            settings.setFolderDrawer(false)
            settings.setColorTheme(ColorTheme.BLUE)
            settings.setThemeMode(ThemeMode.DARK)
            settings.setFontSizeScale(FontSizeScale.LARGE)
            settings.setDisplayMode("list")
            settings.setGridAdaptiveScaling(false)
            settings.setGridManualColumns(3)
            settings.setNotePreviewLength(NotePreviewLength.TITLE_ONLY)
            settings.setShowNoteTimestamp(false)
            settings.setShowNoteTypeIcon(false)
            settings.setCustomAppTitle("Mes notes")
            settings.setAutosaveEnabled(false)
            settings.setDefaultStartInPreviewMode(true)
            settings.setNewNoteFocusContent(true)
            settings.setChecklistScrollTopOnUncheck(false)
            settings.setWordCounterVisibility(WordCounterVisibility.OFF)
            settings.setImageCompressionMode(ImageCompressionMode.LOSSLESS)
            settings.setDefaultImageSizePercent(50)
            settings.setTriggerOnSave(false)
            settings.setTriggerOnResume(false)
            settings.setTriggerWifiConnect(false)
            settings.setTriggerPeriodic(false)
            settings.setTriggerBoot(false)
            settings.setWifiOnlySync(true)
            settings.setSyncInterval(30L)
            settings.setMaxParallelConnections(3)
            settings.setNotificationsEnabled(false)
            settings.setNotificationsErrorsOnly(true)
            settings.setNotificationsServerWarning(false)
            restored = SettingsViewModel(app)
            val editor = NoteEditorViewModel(app, SavedStateHandle())
            assertTrue(editor.uiState.value.defaultStartInPreviewMode)
            assertTrue(editor.uiState.value.newNoteFocusContent)
            assertEquals(WordCounterVisibility.OFF, editor.uiState.value.wordCounterVisibility)
        }
        assertFalse(restored.folderDrawer.value)
        assertEquals(ColorTheme.BLUE, restored.colorTheme.value)
        assertEquals(ThemeMode.DARK, restored.themeMode.value)
        assertEquals(FontSizeScale.LARGE, restored.fontSizeScale.value)
        assertEquals("list", restored.displayMode.value)
        assertFalse(restored.gridAdaptiveScaling.value)
        assertEquals(3, restored.gridManualColumns.value)
        assertEquals(NotePreviewLength.TITLE_ONLY, restored.notePreviewLength.value)
        assertFalse(restored.showNoteTimestamp.value)
        assertFalse(restored.showNoteTypeIcon.value)
        assertEquals("Mes notes", restored.customAppTitle.value)
        assertFalse(restored.autosaveEnabled.value)
        assertFalse(restored.checklistScrollTopOnUncheck.value)
        assertEquals(ImageCompressionMode.LOSSLESS, restored.imageCompressionMode.value)
        assertEquals(50, restored.defaultImageSizePercent.value)
        assertFalse(restored.triggerOnSave.value)
        assertFalse(restored.triggerOnResume.value)
        assertFalse(restored.triggerWifiConnect.value)
        assertFalse(restored.triggerPeriodic.value)
        assertFalse(restored.triggerBoot.value)
        assertTrue(restored.wifiOnlySync.value)
        assertEquals(30L, restored.syncInterval.value)
        assertEquals(3, restored.maxParallelConnections.value)
        assertFalse(restored.notificationsEnabled.value)
        assertTrue(restored.notificationsErrorsOnly.value)
        assertFalse(restored.notificationsServerWarning.value)
    }

    @Test fun settingsScreensStillOpenAndReturnToOverview() {
        lateinit var navigation: NavHostController
        val model = SettingsViewModel(app)
        model.setOfflineMode(true)
        compose.setContent {
            navigation = rememberNavController()
            SimpleNotesTheme(themeMode = ThemeMode.LIGHT, colorTheme = ColorTheme.YELLOW) {
                SettingsNavHost(navigation, model, {})
            }
        }
        compose.onNodeWithText(app.getString(R.string.display_settings_title)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.display_settings_title)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(SettingsRoute.Display.route, navigation.currentDestination?.route) }
        compose.runOnIdle { navigation.popBackStack() }
        val routes = listOf(SettingsRoute.Server, SettingsRoute.Sync, SettingsRoute.Backup,
            SettingsRoute.Security, SettingsRoute.Import, SettingsRoute.Trash, SettingsRoute.ActivityLog,
            SettingsRoute.Language, SettingsRoute.Debug, SettingsRoute.CalendarParsingExperiment,
            SettingsRoute.About, SettingsRoute.Changelog, SettingsRoute.Contributors)
        routes.forEach { route ->
            compose.runOnIdle { navigation.navigate(route.route) }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(route.route, navigation.currentDestination?.route) }
            if (route == SettingsRoute.Changelog) {
                compose.waitUntil(10_000) { compose.onAllNodesWithText("v0.1.8").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("v0.1.8").assertIsDisplayed()
            }
            compose.runOnIdle { navigation.popBackStack() }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(SettingsRoute.Main.route, navigation.currentDestination?.route) }
        }
    }

    @Test fun backupAndRestoreKeepFolderNavigationPreference() = runBlocking {
        ThemePreferences.setFolderDrawer(prefs, false)
        val file = File(app.cacheDir, "settings-regression-backup.json")
        val uri = Uri.fromFile(file)
        val manager = BackupManager(app)
        assertTrue(manager.createBackup(uri, includeServerSettings = true).success)
        ThemePreferences.setFolderDrawer(prefs, true)
        assertTrue(manager.restoreBackup(uri, RestoreMode.MERGE, restoreServerSettings = true).success)
        assertFalse(ThemePreferences.getFolderDrawer(prefs))
    }

    @Test fun allThreeImageCompressionModesProcessRealImages() = runBlocking {
        val file = File(app.cacheDir, "settings-regression-image.png")
        val bitmap = Bitmap.createBitmap(2200, 1100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val processor = ImageProcessor(app)
        ImageCompressionMode.entries.forEach { mode ->
            val processed = processor.process(Uri.fromFile(file), mode)
            val decoded = BitmapFactory.decodeByteArray(processed.bytes, 0, processed.bytes.size)
            assertNotNull(decoded)
            if (mode == ImageCompressionMode.ORIGINAL) {
                assertArrayEquals(file.readBytes(), processed.bytes)
                assertEquals(2200, decoded.width)
            } else {
                assertEquals("webp", processed.ext)
                assertTrue(decoded.width <= 1920)
            }
            decoded.recycle()
        }
    }
}
