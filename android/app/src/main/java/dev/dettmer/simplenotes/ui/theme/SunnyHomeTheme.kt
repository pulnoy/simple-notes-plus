package dev.dettmer.simplenotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance


@Composable
fun SunnyHomeTheme(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = base.surface.luminance() < 0.5f
    val scheme = if (dark) {
        base.copy(primary = SunnyColors.Yellow, onPrimary = SunnyColors.Ink,
            primaryContainer = Color(0xFF55451B), onPrimaryContainer = Color(0xFFFFE7A1))
    } else {
        base.copy(
            primary = SunnyColors.ActionGold, onPrimary = Color.White,
            primaryContainer = SunnyColors.Butter, onPrimaryContainer = SunnyColors.Ink,
            surface = SunnyColors.Cream, background = SunnyColors.Cream,
            surfaceContainer = Color(0xFFF5F1E8), surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color(0xFFF0ECE3),
            onSurface = SunnyColors.Ink, onSurfaceVariant = Color(0xFF66666E),
            outline = Color(0xFF898984), outlineVariant = Color(0xFFE8E4DA)
        )
    }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
}
