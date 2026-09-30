package dev.dettmer.simplenotes.ui.main

import android.content.Context
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import dev.dettmer.simplenotes.ui.theme.ColorPalettes
import dev.dettmer.simplenotes.ui.theme.ColorTheme
import dev.dettmer.simplenotes.ui.theme.LocalHomeAccent
import dev.dettmer.simplenotes.ui.theme.SimpleNotesTheme
import dev.dettmer.simplenotes.ui.theme.SunnyColors
import dev.dettmer.simplenotes.ui.theme.ThemeMode
import dev.dettmer.simplenotes.ui.theme.ThemePreferences
import dev.dettmer.simplenotes.ui.settings.SettingsViewModel
import dev.dettmer.simplenotes.ui.settings.screens.DisplaySettingsScreen
import dev.dettmer.simplenotes.utils.Constants
import dev.dettmer.simplenotes.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemeConsistencyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun appearanceSettingPersistsFolderNavigationChoice() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        ThemePreferences.setColorTheme(prefs, ColorTheme.YELLOW)
        ThemePreferences.setThemeMode(prefs, ThemeMode.LIGHT)
        ThemePreferences.setFolderDrawer(prefs, true)
        val model = SettingsViewModel(app)
        compose.setContent {
            val theme by model.colorTheme.collectAsState()
            SimpleNotesTheme(themeMode = ThemeMode.LIGHT, colorTheme = theme) {
                DisplaySettingsScreen(model, {})
            }
        }
        val label = app.getString(R.string.home_drawer_setting)
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false, ThemePreferences.getFolderDrawer(prefs)) }
        compose.onNodeWithText(label).performClick()
        compose.runOnIdle { assertEquals(true, ThemePreferences.getFolderDrawer(prefs)) }
        compose.onRoot().captureToImage().asAndroidBitmap().let { image ->
            File(app.cacheDir, "appearance-yellow.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun homeAccentFollowsSelectedPaletteIncludingDynamicColors() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val selection = mutableStateOf(ColorTheme.YELLOW)
        var accent = Color.Unspecified
        var primary = Color.Unspecified
        compose.setContent {
            SimpleNotesTheme(themeMode = ThemeMode.LIGHT, colorTheme = selection.value) {
                accent = LocalHomeAccent.current.color
                primary = MaterialTheme.colorScheme.primary
            }
        }
        compose.waitForIdle()
        assertEquals(SunnyColors.Yellow, accent)
        assertEquals(SunnyColors.ActionGold, primary)
        assertEquals(SunnyColors.Cream,
            ColorPalettes.getColorScheme(ColorTheme.YELLOW, false, false, context).surfaceContainerLow)
        compose.runOnIdle { selection.value = ColorTheme.BLUE }
        compose.waitForIdle()
        val blue = ColorPalettes.getColorScheme(ColorTheme.BLUE, false, false, context)
        assertEquals(blue.primary, primary)
        assertEquals(blue.primary, accent)
        compose.runOnIdle { selection.value = ColorTheme.DYNAMIC }
        compose.waitForIdle()
        val dynamic = ColorPalettes.getColorScheme(ColorTheme.DYNAMIC, false, false, context)
        assertEquals(dynamic.primary, primary)
        assertEquals(dynamic.primary, accent)
    }
}
