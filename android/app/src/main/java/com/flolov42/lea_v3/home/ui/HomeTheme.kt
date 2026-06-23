package com.flolov42.lea_v3.home.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette Léa Home ──────────────────────────────────────────────────────────

object HomeColors {
    val Background    = Color(0xFF0a0e27)
    val Surface       = Color(0xFF0d1133)
    val SurfaceHigh   = Color(0xFF111640)
    val CardGlass     = Color(0x14FFFFFF)    // white 8%
    val CardGlassBright = Color(0x1EFFFFFF)  // white 12%
    val CardBorder    = Color(0x33FFFFFF)    // white 20%

    val Cyan          = Color(0xFF00E5FF)
    val CyanDim       = Color(0x4D00E5FF)   // 30%
    val CyanGlow      = Color(0x8000E5FF)   // 50%
    val Violet        = Color(0xFFB837FF)
    val VioletDim     = Color(0x4DB837FF)
    val VioletGlow    = Color(0x80B837FF)
    val Blue          = Color(0xFF4361EE)
    val BlueGlow      = Color(0x804361EE)

    val OnBackground  = Color(0xFFE8EAFF)
    val OnSurface     = Color(0xFFCCCFEE)
    val OnSurfaceDim  = Color(0xFF7880A4)
    val StateOn       = Cyan
    val StateOff      = Color(0xFF4A4F6F)
    val Error         = Color(0xFFFF4D4D)
    val Success       = Color(0xFF00E676)

    val deviceColors = mapOf(
        "LIGHT"        to Cyan,
        "SWITCH"       to Blue,
        "THERMOSTAT"   to Color(0xFFFF9100),
        "CLIMATE"      to Color(0xFFFF9100),
        "SENSOR"       to Color(0xFF69FF47),
        "CAMERA"       to Violet,
        "LOCK"         to Color(0xFFFFD600),
        "COVER"        to Color(0xFF40C4FF),
        "FAN"          to Color(0xFF64FFDA),
        "MEDIA_PLAYER" to Color(0xFFFF4081),
        "VACUUM"       to Color(0xFFE040FB),
        "UNKNOWN"      to Color(0xFF7880A4)
    )
}

// ── Material 3 ColorScheme ────────────────────────────────────────────────────

private val LeaHomeDarkColors = darkColorScheme(
    primary          = HomeColors.Cyan,
    onPrimary        = HomeColors.Background,
    primaryContainer = HomeColors.CyanDim,
    secondary        = HomeColors.Violet,
    onSecondary      = Color.White,
    secondaryContainer = HomeColors.VioletDim,
    tertiary         = HomeColors.Blue,
    background       = HomeColors.Background,
    surface          = HomeColors.Surface,
    surfaceVariant   = HomeColors.SurfaceHigh,
    onBackground     = HomeColors.OnBackground,
    onSurface        = HomeColors.OnSurface,
    onSurfaceVariant = HomeColors.OnSurfaceDim,
    error            = HomeColors.Error,
)

// ── Typography ────────────────────────────────────────────────────────────────

private val LeaHomeTypography = Typography(
    headlineLarge = TextStyle(
        color = HomeColors.OnBackground,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        color = HomeColors.OnBackground,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        color = HomeColors.OnBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        color = HomeColors.OnSurface,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        color = HomeColors.OnSurface,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        color = HomeColors.OnSurfaceDim,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        color = HomeColors.OnSurfaceDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun LeaHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeaHomeDarkColors,
        typography  = LeaHomeTypography,
        content     = content
    )
}
