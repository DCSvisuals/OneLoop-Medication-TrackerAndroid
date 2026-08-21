package com.davidcarranco.oneloop.medtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalOneLoopPalette = staticCompositionLocalOf { LightPalette }

private fun paletteToScheme(palette: OneLoopPalette, dark: Boolean): ColorScheme {
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = palette.blue,
        onPrimary = Color.White,
        secondary = palette.lime,
        onSecondary = palette.scheduleSelectionText,
        tertiary = palette.teal,
        background = palette.softBackground,
        onBackground = palette.navy,
        surface = palette.softBackground,
        onSurface = palette.navy,
        surfaceVariant = palette.cardBackground,
        onSurfaceVariant = palette.mutedText,
        outline = palette.cardBorder,
        error = palette.warning,
        onError = Color.White,
    )
}

@Composable
fun OneLoopTheme(
    useSystemAppearance: Boolean = true,
    useDarkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = if (useSystemAppearance) isSystemInDarkTheme() else useDarkMode
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalOneLoopPalette provides palette) {
        MaterialTheme(
            colorScheme = paletteToScheme(palette, dark),
            content = content,
        )
    }
}

object OneLoopTheme {
    val colors: OneLoopPalette
        @Composable get() = LocalOneLoopPalette.current
}
