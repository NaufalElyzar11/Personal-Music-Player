package com.noirmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NoirColorScheme = darkColorScheme(
    primary = NoirAccent,
    onPrimary = NoirOnAccent,
    primaryContainer = NoirSurfaceHigh,
    onPrimaryContainer = NoirOnBackground,
    secondary = NoirAccent,
    onSecondary = NoirOnAccent,
    secondaryContainer = NoirSurfaceHigh,
    onSecondaryContainer = NoirOnBackground,
    background = NoirBackground,
    onBackground = NoirOnBackground,
    surface = NoirSurface,
    onSurface = NoirOnBackground,
    surfaceVariant = NoirSurfaceHigh,
    onSurfaceVariant = NoirOnSurfaceVariant,
    surfaceContainer = NoirSurface,
    surfaceContainerHigh = NoirSurfaceHigh,
    surfaceContainerHighest = NoirSurfaceHigh,
    outline = NoirOutlineVariant,
    outlineVariant = NoirOutlineVariant,
    error = NoirError,
)

@Composable
fun NoirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoirColorScheme,
        typography = NoirTypography,
        content = content,
    )
}
