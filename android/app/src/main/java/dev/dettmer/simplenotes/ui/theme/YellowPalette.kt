package dev.dettmer.simplenotes.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object YellowPalette {
    private val light = lightColorScheme(
        primary = SunnyColors.ActionGold, onPrimary = Color.White,
        primaryContainer = SunnyColors.Butter, onPrimaryContainer = SunnyColors.Ink,
        secondary = SunnyColors.Yellow, onSecondary = SunnyColors.Ink,
        secondaryContainer = SunnyColors.Peach, onSecondaryContainer = SunnyColors.Ink,
        tertiary = SunnyColors.ActionGold, onTertiary = Color.White,
        tertiaryContainer = SunnyColors.Sage, onTertiaryContainer = SunnyColors.Ink,
        surface = SunnyColors.Cream, background = SunnyColors.Cream,
        onBackground = SunnyColors.Ink, onSurface = SunnyColors.Ink,
        surfaceContainerLow = SunnyColors.Cream, surfaceContainerLowest = Color.White,
        surfaceDim = Color(0xFFE0DBD0), surfaceBright = SunnyColors.Cream,
        surfaceVariant = Color(0xFFF0ECE3), surfaceTint = SunnyColors.ActionGold,
        inversePrimary = SunnyColors.Yellow,
        surfaceContainer = Color(0xFFF5F1E8), surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Color(0xFFF0ECE3), onSurfaceVariant = Color(0xFF66666E),
        outline = Color(0xFF898984), outlineVariant = Color(0xFFE8E4DA)
    )
    private val dark = darkColorScheme(
        primary = SunnyColors.Yellow, onPrimary = SunnyColors.Ink,
        primaryContainer = Color(0xFF55451B), onPrimaryContainer = Color(0xFFFFE7A1),
        secondary = SunnyColors.Yellow, onSecondary = SunnyColors.Ink,
        secondaryContainer = Color(0xFF574336), onSecondaryContainer = SunnyColors.Peach,
        tertiary = SunnyColors.Yellow, onTertiary = SunnyColors.Ink,
        tertiaryContainer = Color(0xFF374431), onTertiaryContainer = SunnyColors.Sage,
        surface = Color(0xFF181714), background = Color(0xFF181714),
        surfaceContainerLow = Color(0xFF1C1B17), surfaceContainerLowest = Color(0xFF12110F),
        surfaceDim = Color(0xFF181714), surfaceBright = Color(0xFF37332B),
        surfaceVariant = Color(0xFF49443A), surfaceTint = SunnyColors.Yellow,
        inversePrimary = SunnyColors.ActionGold,
        onSurface = Color(0xFFF0ECE3), onBackground = Color(0xFFF0ECE3),
        surfaceContainer = Color(0xFF211F1B), surfaceContainerHigh = Color(0xFF2C2923),
        surfaceContainerHighest = Color(0xFF37332B), onSurfaceVariant = Color(0xFFD0C8BA),
        outline = Color(0xFF999184), outlineVariant = Color(0xFF49443A)
    )

    fun scheme(isDark: Boolean, isAmoled: Boolean): ColorScheme = when {
        isAmoled -> dark.copy(surface = Color.Black, background = Color.Black)
        isDark -> dark
        else -> light
    }
}
