package com.healthtracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val ink2: Color,
    val hairline: Color,
    val primary: Color,
    val onPrimary: Color,
    val hero: Color,
    val onHero: Color,
    val heroMuted: Color,
    val heroTrack: Color,
    val heroFill: Color,
    val data: Color,
    val over: Color,
    val aqua: Color,
    val track: Color,
    val soft: Color,
    val good: Color,
    val dot: Color,
    val grid: Color,
    val axis: Color,
    val isDark: Boolean,
)

val LightColors = AppColors(
    bg = Color(0xFFF6F6F4), card = Color(0xFFFFFFFF), ink = Color(0xFF0B0B0B), ink2 = Color(0xFF52514E),
    hairline = Color(0x1A0B0B0B), primary = Color(0xFF1C5CAB), onPrimary = Color(0xFFFFFFFF),
    hero = Color(0xFF0D366B), onHero = Color(0xFFFFFFFF), heroMuted = Color(0xFFCDE2FB),
    heroTrack = Color(0x33FFFFFF), heroFill = Color(0xFF9EC5F4),
    data = Color(0xFF2A78D6), over = Color(0xFFEB6834), aqua = Color(0xFF1BAF7A),
    track = Color(0xFFCDE2FB), soft = Color(0xFFEDF3FC), good = Color(0xFF006300),
    dot = Color(0xFFA8A7A1), grid = Color(0xFFE8E7E1), axis = Color(0xFFC3C2B7), isDark = false,
)

val DarkColors = AppColors(
    bg = Color(0xFF0E0F11), card = Color(0xFF1A1B1E), ink = Color(0xFFF0EFEC), ink2 = Color(0xFFC3C2B7),
    hairline = Color(0x1FFFFFFF), primary = Color(0xFF86B6EF), onPrimary = Color(0xFF0B1B30),
    hero = Color(0xFF123F78), onHero = Color(0xFFFFFFFF), heroMuted = Color(0xFFCDE2FB),
    heroTrack = Color(0x33FFFFFF), heroFill = Color(0xFF9EC5F4),
    data = Color(0xFF3987E5), over = Color(0xFFD95926), aqua = Color(0xFF199E70),
    track = Color(0xFF233A57), soft = Color(0xFF1E2A3A), good = Color(0xFF3FBF3F),
    dot = Color(0xFF6E6D68), grid = Color(0xFF2C2C2A), axis = Color(0xFF45453F), isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

object App {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun HealthTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, background = c.bg, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, onSurfaceVariant = c.ink2, surfaceVariant = c.soft,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.soft,
            surfaceContainerLow = c.card, outline = c.axis, outlineVariant = c.hairline,
            secondaryContainer = c.soft, onSecondaryContainer = c.ink, primaryContainer = c.soft, onPrimaryContainer = c.ink,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, background = c.bg, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, onSurfaceVariant = c.ink2, surfaceVariant = c.soft,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.soft,
            surfaceContainerLow = c.card, outline = c.axis, outlineVariant = c.hairline,
            secondaryContainer = c.soft, onSecondaryContainer = c.ink, primaryContainer = c.soft, onPrimaryContainer = c.ink,
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
