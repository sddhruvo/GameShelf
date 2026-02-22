package com.gamevault.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// GameShelf brand colors
val Purple = Color(0xFF6C63FF)
val PurpleDark = Color(0xFF3D37A5)
val PurpleGlow = Color(0xFF8B83FF)
val Accent = Color(0xFF00D4AA)
val AccentGlow = Color(0xFF00FFD0)
val Neon = Color(0xFFFF2E63)
val NeonYellow = Color(0xFFFFD600)
val BackgroundDark = Color(0xFF0F0F1A)
val SurfaceDark = Color(0xFF161625)
val BackgroundLight = Color(0xFFF5F5F9)
val SurfaceLight = Color(0xFFFFFFFF)
val AmoledBlack = Color(0xFF000000)

// Glass colors for AMOLED
val GlassSurface = Color(0x1AFFFFFF)       // 10% white
val GlassSurfaceHover = Color(0x26FFFFFF)  // 15% white
val GlassBorder = Color(0x33FFFFFF)        // 20% white

// Extra theme info not in MaterialTheme
data class GameShelfColors(
    val isGlass: Boolean = false,
    val glassSurface: Color = GlassSurface,
    val glassBorder: Color = GlassBorder,
    val accentGlow: Color = AccentGlow,
    val neon: Color = Neon
)

val LocalGameShelfColors = staticCompositionLocalOf { GameShelfColors() }

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    secondary = Accent,
    tertiary = PurpleDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C30),
    onSurfaceVariant = Color(0xFF9999B0),
    surfaceContainer = Color(0xFF1A1A2E),
    outline = Color(0xFF2A2A40)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple,
    secondary = Accent,
    tertiary = PurpleDark,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFF4A4A5A)
)

private val AmoledGlassColorScheme = darkColorScheme(
    primary = PurpleGlow,
    secondary = AccentGlow,
    tertiary = Purple,
    background = AmoledBlack,
    surface = Color(0xFF050508),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0F0),
    onSurface = Color(0xFFE0E0F0),
    surfaceVariant = GlassSurface,
    onSurfaceVariant = Color(0xFF9999B0),
    surfaceContainer = Color(0x0DFFFFFF),
    outline = GlassBorder
)

enum class ThemeMode { LIGHT, DARK, AMOLED, SYSTEM }

private val GamerTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.5.sp)
)

@Composable
fun GameShelfTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == ThemeMode.AMOLED -> AmoledGlassColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val gameShelfColors = when (themeMode) {
        ThemeMode.AMOLED -> GameShelfColors(isGlass = true)
        else -> GameShelfColors(isGlass = false)
    }

    CompositionLocalProvider(LocalGameShelfColors provides gameShelfColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GamerTypography,
            content = content
        )
    }
}
