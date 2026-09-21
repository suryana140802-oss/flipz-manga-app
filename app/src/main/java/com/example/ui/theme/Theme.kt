package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colors = if (darkTheme) darkAppColors else lightAppColors
  val colorScheme = if (darkTheme) {
    darkColorScheme(
      primary = colors.primary,
      secondary = HudActiveGreen,
      tertiary = colors.deskHighlight,
      background = colors.deskDark,
      surface = colors.cardBackground,
      surfaceVariant = colors.deskHighlight,
      onPrimary = Color.White,
      onSecondary = Color.White,
      onBackground = colors.textPrimary,
      onSurface = colors.textPrimary,
    )
  } else {
    lightColorScheme(
      primary = colors.primary,
      secondary = HudActiveGreen,
      tertiary = colors.deskHighlight,
      background = colors.deskDark,
      surface = colors.cardBackground,
      surfaceVariant = colors.deskHighlight,
      onPrimary = Color.White,
      onSecondary = Color.White,
      onBackground = colors.textPrimary,
      onSurface = colors.textPrimary,
    )
  }

  CompositionLocalProvider(LocalAppColors provides colors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
    )
  }
}


