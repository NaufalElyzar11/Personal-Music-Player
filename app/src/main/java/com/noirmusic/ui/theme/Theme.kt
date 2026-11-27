package com.noirmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class SakugaThemeMode {
    DAYLIGHT,
    MIDNIGHT,
}

/** Extended cel-shading and VFX color tokens shared across all Sakuga controls. */
@Immutable
data class SakugaTokens(
    val mode: SakugaThemeMode,
    val inkBorder: Color,
    val celShadow: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val cyan: Color,
    val cyanFill: Color,
    val onCyanFill: Color,
    val violetFill: Color,
    val onVioletFill: Color,
    val vfxFill: Color,
    val vfxShade: Color,
    val vfxLine: Color,
    val vfxAccent: Color,
    val vfxCyan: Color,
    val sfxStroke: Color,
    val sfxFill: Color,
    val halftoneDot: Color,
    val speedLine: Color,
) {
    val isDark: Boolean get() = mode == SakugaThemeMode.MIDNIGHT
}

private val DaylightSakugaTokens = SakugaTokens(
    mode = SakugaThemeMode.DAYLIGHT,
    inkBorder = SakugaDayInk,
    celShadow = SakugaDayCelShadow,
    accent = SakugaDayAccent,
    accentText = SakugaDayAccentText,
    onAccent = SakugaDayOnAccent,
    cyan = SakugaDaySecondary,
    cyanFill = SakugaDaySecondaryContainer,
    onCyanFill = SakugaDayOnSecondaryContainer,
    violetFill = SakugaDayPrimaryContainer,
    onVioletFill = SakugaDayOnPrimaryContainer,
    vfxFill = SakugaVfxFillLight,
    vfxShade = SakugaVfxShadeLight,
    vfxLine = SakugaVfxLineLight,
    vfxAccent = SakugaVfxAccent,
    vfxCyan = SakugaVfxCyan,
    sfxStroke = SakugaVfxInkDark,
    sfxFill = SakugaVfxFillLight,
    halftoneDot = SakugaDayCelShadow.copy(alpha = 0.48f),
    speedLine = SakugaDayPrimaryContainer.copy(alpha = 0.34f),
)

private val MidnightSakugaTokens = SakugaTokens(
    mode = SakugaThemeMode.MIDNIGHT,
    inkBorder = SakugaNightOutline,
    celShadow = SakugaNightCelShadow,
    accent = SakugaNightAccent,
    accentText = SakugaNightAccentText,
    onAccent = SakugaNightOnAccent,
    cyan = SakugaNightSecondary,
    cyanFill = SakugaNightSecondary,
    onCyanFill = SakugaNightOnSecondary,
    violetFill = SakugaNightPrimary,
    onVioletFill = SakugaNightOnPrimary,
    vfxFill = SakugaVfxFillNight,
    vfxShade = SakugaVfxShadeNight,
    vfxLine = SakugaVfxLineNight,
    vfxAccent = SakugaVfxAccent,
    vfxCyan = SakugaVfxCyan,
    sfxStroke = Color(0xFF0A081A),
    sfxFill = SakugaVfxAccent,
    halftoneDot = SakugaNightOutlineVariant.copy(alpha = 0.42f),
    speedLine = SakugaNightPrimary.copy(alpha = 0.22f),
)

val LocalSakugaTokens = staticCompositionLocalOf { DaylightSakugaTokens }

object SakugaTheme {
    val tokens: SakugaTokens
        @Composable
        get() = LocalSakugaTokens.current
}

private val SakugaDaylightColorScheme = lightColorScheme(
    primary = SakugaDayPrimary,
    onPrimary = SakugaDayOnPrimary,
    primaryContainer = SakugaDayPrimaryContainer,
    onPrimaryContainer = SakugaDayOnPrimaryContainer,
    secondary = SakugaDaySecondary,
    onSecondary = SakugaDayOnSecondary,
    secondaryContainer = SakugaDaySecondaryContainer,
    onSecondaryContainer = SakugaDayOnSecondaryContainer,
    tertiary = SakugaDayAccentText,
    onTertiary = Color.White,
    tertiaryContainer = SakugaDayAccent,
    onTertiaryContainer = SakugaDayOnAccent,
    background = SakugaDayBackground,
    onBackground = SakugaDayInk,
    surface = SakugaDaySurface,
    onSurface = SakugaDayInk,
    surfaceVariant = SakugaDaySurfaceVariant,
    onSurfaceVariant = SakugaDayMutedInk,
    surfaceContainerLowest = SakugaDaySurfaceLowest,
    surfaceContainerLow = SakugaDaySurfaceLow,
    surfaceContainer = SakugaDaySurfaceContainer,
    surfaceContainerHigh = SakugaDaySurfaceHigh,
    surfaceContainerHighest = SakugaDaySurfaceHighest,
    outline = SakugaDayOutline,
    outlineVariant = SakugaDayOutlineVariant,
    error = SakugaDayError,
    onError = SakugaDayOnError,
    errorContainer = SakugaDayErrorContainer,
    onErrorContainer = SakugaDayOnErrorContainer,
)

private val SakugaMidnightColorScheme = darkColorScheme(
    primary = SakugaNightPrimary,
    onPrimary = SakugaNightOnPrimary,
    primaryContainer = SakugaNightPrimaryContainer,
    onPrimaryContainer = SakugaNightOnPrimaryContainer,
    secondary = SakugaNightSecondary,
    onSecondary = SakugaNightOnSecondary,
    secondaryContainer = SakugaNightSecondaryContainer,
    onSecondaryContainer = SakugaNightOnSecondaryContainer,
    tertiary = SakugaNightAccentText,
    onTertiary = SakugaNightOnAccent,
    tertiaryContainer = SakugaNightAccent,
    onTertiaryContainer = SakugaNightOnAccent,
    background = SakugaNightBackground,
    onBackground = SakugaNightInk,
    surface = SakugaNightSurface,
    onSurface = SakugaNightInk,
    surfaceVariant = SakugaNightSurfaceVariant,
    onSurfaceVariant = SakugaNightMutedInk,
    surfaceContainerLowest = SakugaNightSurfaceLowest,
    surfaceContainerLow = SakugaNightSurfaceLow,
    surfaceContainer = SakugaNightSurfaceContainer,
    surfaceContainerHigh = SakugaNightSurfaceHigh,
    surfaceContainerHighest = SakugaNightSurfaceHighest,
    outline = SakugaNightOutline,
    outlineVariant = SakugaNightOutlineVariant,
    error = SakugaNightError,
    onError = SakugaNightOnError,
    errorContainer = SakugaNightErrorContainer,
    onErrorContainer = SakugaNightOnErrorContainer,
)

@Composable
fun NoirTheme(
    themeMode: SakugaThemeMode = SakugaThemeMode.DAYLIGHT,
    content: @Composable () -> Unit,
) {
    val isDark = themeMode == SakugaThemeMode.MIDNIGHT
    val colorScheme = if (isDark) SakugaMidnightColorScheme else SakugaDaylightColorScheme
    val tokens = if (isDark) MidnightSakugaTokens else DaylightSakugaTokens

    CompositionLocalProvider(LocalSakugaTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoirTypography,
            content = content,
        )
    }
}
