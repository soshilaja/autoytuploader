package com.autoyt.uploader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Pocket Archive tokens. Names follow the design spec; the dark set is derived from the same palette.
 * Muted, green, rust and rust dark are darkened from the spec so text reaches WCAG AA 4.5:1 on paper,
 * cream and mint.
 */
@Immutable
data class AutoColors(
    val paper: Color,
    val cream: Color,
    val rust: Color,
    val rustDark: Color,
    val green: Color,
    val deep: Color,
    val mint: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val onRust: Color,
    val onDeep: Color,
    val onDeepSoft: Color,
)

private val LightColors = AutoColors(
    paper = Color(0xFFF3E9D2),
    cream = Color(0xFFFFFAF0),
    rust = Color(0xFFA84E33),
    rustDark = Color(0xFF96442C),
    green = Color(0xFF0F6B54),
    deep = Color(0xFF0E5C4A),
    mint = Color(0xFFD7E8D8),
    ink = Color(0xFF213129),
    muted = Color(0xFF556056),
    line = Color(0x29213129),
    onRust = Color(0xFFFFFAF0),
    onDeep = Color(0xFFFFFAF0),
    onDeepSoft = Color(0xD9FFFAF0),
)

private val DarkColors = AutoColors(
    paper = Color(0xFF161D19),
    cream = Color(0xFF1F2823),
    rust = Color(0xFFE08A6C),
    rustDark = Color(0xFFF0A98F),
    green = Color(0xFF5CC3A4),
    deep = Color(0xFF0E5C4A),
    mint = Color(0xFF263B31),
    ink = Color(0xFFF3E9D2),
    muted = Color(0xFFA3AC9F),
    line = Color(0x29F3E9D2),
    onRust = Color(0xFF213129),
    onDeep = Color(0xFFFFFAF0),
    onDeepSoft = Color(0xD9FFFAF0),
)

private val LocalColors = staticCompositionLocalOf { LightColors }

val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

val DmMono = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)

private val AutoTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.04).em),
    titleLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 25.sp, letterSpacing = (-0.02).em),
    titleMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em),
    bodyLarge = TextStyle(fontFamily = Atkinson, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Atkinson, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Atkinson, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Atkinson, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = DmMono, fontSize = 11.sp, lineHeight = 15.sp),
)

object AutoTheme {
    val colors: AutoColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
}

@Composable
fun AutoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColors else LightColors
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = colors.rust,
        onPrimary = colors.onRust,
        secondary = colors.green,
        background = colors.paper,
        onBackground = colors.ink,
        surface = colors.cream,
        onSurface = colors.ink,
        surfaceVariant = colors.paper,
        onSurfaceVariant = colors.muted,
        surfaceContainer = colors.cream,
        surfaceContainerHigh = colors.cream,
        outline = colors.muted,
        outlineVariant = colors.line,
        error = colors.rustDark,
    )
    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = AutoTypography) {
            CompositionLocalProvider(LocalContentColor provides colors.ink, content = content)
        }
    }
}
