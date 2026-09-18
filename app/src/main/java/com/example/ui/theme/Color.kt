package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Sky Blue Theme Accents
val SkyBluePrimaryLight = Color(0xFF0284C7) // Sky-600
val SkyBluePrimaryDark = Color(0xFF38BDF8) // Sky-400

// Backgrounds (Dark Mode)
val DeskDarkColor = Color(0xFF0F172A)
val DeskMediumColor = Color(0xFF1E293B)
val DeskHighlightColor = Color(0xFF334155)

// Backgrounds (Light Mode)
val DeskLightPrimary = Color(0xFFF1F5F9)
val DeskLightSecondary = Color(0xFFE2E8F0)
val DeskLightHighlight = Color(0xFFCBD5E1)

// HUD Colors (Dark)
val HudGlassBackgroundDark = Color(0xEB0F172A)
val HudBorderColorDark = Color(0x33FFFFFF)

// HUD Colors (Light)
val HudGlassBackgroundLight = Color(0xEBFFFFFF)
val HudBorderColorLight = Color(0x33000000)

val HudActiveGreen = Color(0xFF10B981)

// Text Colors (Dark)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Text Colors (Light)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)

// Paper Textures
val PaperClean = Color(0xFFFFFFFF)
val PaperMatte = Color(0xFFF9F7F1)
val PaperMangaPulp = Color(0xFFF3E8CE)
val PaperVintage = Color(0xFFEFE0B9)

data class AppColors(
    val primary: Color,
    val deskDark: Color,
    val deskMedium: Color,
    val deskHighlight: Color,
    val hudGlass: Color,
    val hudBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isLight: Boolean
)

val lightAppColors = AppColors(
    primary = SkyBluePrimaryLight,
    deskDark = DeskLightPrimary,
    deskMedium = DeskLightSecondary,
    deskHighlight = DeskLightHighlight,
    hudGlass = HudGlassBackgroundLight,
    hudBorder = HudBorderColorLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    isLight = true
)

val darkAppColors = AppColors(
    primary = SkyBluePrimaryDark,
    deskDark = DeskDarkColor,
    deskMedium = DeskMediumColor,
    deskHighlight = DeskHighlightColor,
    hudGlass = HudGlassBackgroundDark,
    hudBorder = HudBorderColorDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    isLight = false
)

val LocalAppColors = staticCompositionLocalOf { darkAppColors }


