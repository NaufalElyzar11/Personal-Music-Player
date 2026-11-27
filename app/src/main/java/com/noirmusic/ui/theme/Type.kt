package com.noirmusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Expressive anime italic display headers paired with crisp geometric UI sans and tabular numbers for timecodes.
val NoirTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyMedium = TextStyle(
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        fontFeatureSettings = "tnum",
    ),
)

val LyricLineStyle = TextStyle(
    fontSize = 21.sp,
    lineHeight = 31.sp,
    fontWeight = FontWeight.Bold,
)

val ActiveLyricLineStyle = TextStyle(
    fontSize = 24.sp,
    lineHeight = 33.sp,
    fontWeight = FontWeight.Black,
    fontStyle = FontStyle.Italic,
)
