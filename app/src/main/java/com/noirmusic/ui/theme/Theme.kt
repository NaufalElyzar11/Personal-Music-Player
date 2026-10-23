package com.noirmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NoirColorScheme = darkColorScheme(
    primary = NoirPrimary,
    onPrimary = NoirOnPrimary,
    primaryContainer = NoirPrimaryContainer,
    onPrimaryContainer = NoirOnPrimaryContainer,
    secondary = NoirSecondary,
    onSecondary = NoirOnSecondary,
    secondaryContainer = NoirSurfaceVariant,
    onSecondaryContainer = NoirOnSurface,
    background = NoirBackground,
    onBackground = NoirOnSurface,
    surface = NoirSurface,
    onSurface = NoirOnSurface,
    surfaceVariant = NoirSurfaceVariant,
    onSurfaceVariant = NoirOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0D0F0E),
    surfaceContainerLow = NoirSurfaceLow,
    surfaceContainer = NoirSurfaceContainer,
    surfaceContainerHigh = NoirSurfaceHigh,
    surfaceContainerHighest = NoirSurfaceHighest,
    outline = NoirOutline,
    outlineVariant = NoirOutlineVariant,
    error = NoirError,
    onError = NoirOnError,
    errorContainer = NoirErrorContainer,
)

@Composable
fun NoirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoirColorScheme,
        typography = NoirTypography,
        content = content,
    )
}
