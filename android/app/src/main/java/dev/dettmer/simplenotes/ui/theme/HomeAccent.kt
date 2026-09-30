package dev.dettmer.simplenotes.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Accent used by the home controls; supplied by the selected application palette. */
data class HomeAccent(val color: Color, val onColor: Color, val yellow: Boolean)

val LocalHomeAccent = staticCompositionLocalOf { HomeAccent(SunnyColors.Yellow, SunnyColors.Ink, true) }
