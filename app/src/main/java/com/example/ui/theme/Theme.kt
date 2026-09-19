package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.settings.AppThemeMode

private val DarkColorScheme =
  darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = IndigoDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = CyanAccent,
    onSecondary = Color(0xFF00363A),
    tertiary = AmberAccent,
    background = DarkBackground,
    onBackground = Color(0xFFF3F4F6),
    surface = DarkSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = DarkOutline,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = AmberAccent,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = LightOutline,
  )

/**
 * Construye un ColorScheme personalizado a partir de un color primario elegido por el usuario,
 * preservando la coherencia visual, contraste y diseño estético de la aplicación.
 */
fun buildCustomColorScheme(primary: Color, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        DarkColorScheme.copy(
            primary = primary,
            primaryContainer = primary.copy(alpha = 0.30f),
            onPrimaryContainer = Color(0xFFF8FAFC)
        )
    } else {
        LightColorScheme.copy(
            primary = primary,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary
        )
    }
}

@Composable
fun MyApplicationTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  customPrimaryColor: Color = IndigoPrimary,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    customPrimaryColor != IndigoPrimary -> {
      buildCustomColorScheme(customPrimaryColor, isDark)
    }
    isDark -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MyApplicationTheme(
    themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
    dynamicColor = dynamicColor,
    customPrimaryColor = IndigoPrimary,
    content = content
  )
}

