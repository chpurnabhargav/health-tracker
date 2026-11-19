package com.healthtracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.healthtracker.app.R

@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val card2: Color,
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
    val water: Color,
    val coral: Color,
    val amber: Color,
    val track: Color,
    val soft: Color,
    val good: Color,
    val dot: Color,
    val grid: Color,
    val axis: Color,
    val isDark: Boolean,
)

/** "Night neon": near-black ground, electric lime for actions, violet and cyan for data. */
val NeonColors = AppColors(
    bg = Color(0xFF07080D), card = Color(0xFF12141D), card2 = Color(0xFF191C28),
    ink = Color(0xFFF5F6FA), ink2 = Color(0xFFA6ABBD), hairline = Color(0x17FFFFFF),
    primary = Color(0xFFC8FF4D), onPrimary = Color(0xFF0B0D12),
    hero = Color(0xFF1B1550), onHero = Color(0xFFFFFFFF), heroMuted = Color(0xFFCBC5FF),
    heroTrack = Color(0x2EFFFFFF), heroFill = Color(0xFFC8FF4D),
    data = Color(0xFF8E7CFF), over = Color(0xFFFF7A45), aqua = Color(0xFF3DE0B0),
    water = Color(0xFF34D3F6), coral = Color(0xFFFF4D6D), amber = Color(0xFFFFB547),
    track = Color(0xFF232738), soft = Color(0xFF1C2030), good = Color(0xFF4BE39B),
    dot = Color(0xFF5A5F73), grid = Color(0xFF1E2130), axis = Color(0xFF30344A), isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { NeonColors }

object App {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

object Brushes {
    val hero = Brush.linearGradient(listOf(Color(0xFF2E2386), Color(0xFF161A4A), Color(0xFF0A3346)))
    val lime = Brush.horizontalGradient(listOf(Color(0xFFC8FF4D), Color(0xFF7CF5C0)))
    val violetCyan = Brush.horizontalGradient(listOf(Color(0xFF8E7CFF), Color(0xFF34D3F6)))
    val danger = Brush.verticalGradient(listOf(Color(0xFF2A0612), Color(0xFF0A0508)))
}

val Display = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val Body = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontSize = 72.sp, lineHeight = 72.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontFamily = Display, fontSize = 60.sp, lineHeight = 62.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontFamily = Display, fontSize = 50.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = Display, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Display, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = Display, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = Display, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = Body, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = Body, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun HealthTheme(content: @Composable () -> Unit) {
    val c = NeonColors
    val scheme = darkColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, background = c.bg, onBackground = c.ink,
        surface = c.card, onSurface = c.ink, onSurfaceVariant = c.ink2, surfaceVariant = c.soft,
        surfaceContainer = c.card, surfaceContainerHigh = c.card2, surfaceContainerHighest = c.soft,
        surfaceContainerLow = c.card, surfaceContainerLowest = c.bg, outline = c.axis, outlineVariant = c.hairline,
        secondaryContainer = c.soft, onSecondaryContainer = c.ink, primaryContainer = c.soft, onPrimaryContainer = c.ink,
        error = c.coral,
    )
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
