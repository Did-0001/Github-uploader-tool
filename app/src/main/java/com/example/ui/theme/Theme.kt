package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = GhGreenPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF194C24),
  onPrimaryContainer = Color(0xFF7EE787),
  secondary = GhBlue,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF112D55),
  onSecondaryContainer = Color(0xFF79C0FF),
  tertiary = GhOrange,
  background = GhDarkBackground,
  onBackground = GhDarkTextPrimary,
  surface = GhDarkSurface,
  onSurface = GhDarkTextPrimary,
  surfaceVariant = GhDarkSurfaceVariant,
  onSurfaceVariant = GhDarkTextSecondary,
  outline = GhDarkBorder,
  error = GhRed,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = GhLightGreen,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFDCFFE4),
  onPrimaryContainer = Color(0xFF14532D),
  secondary = GhLightBlue,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDDF4FF),
  onSecondaryContainer = Color(0xFF0969DA),
  tertiary = GhOrange,
  background = GhLightBackground,
  onBackground = GhLightTextPrimary,
  surface = GhLightSurface,
  onSurface = GhLightTextPrimary,
  surfaceVariant = GhLightSurfaceVariant,
  onSurfaceVariant = GhLightTextSecondary,
  outline = GhLightBorder,
  error = GhRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set false to prioritize our GitHub tech theme
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
