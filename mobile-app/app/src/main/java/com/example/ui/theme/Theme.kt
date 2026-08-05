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

private val DarkColorScheme =
  darkColorScheme(
    primary = OrangePrimaryLight,
    primaryContainer = OrangePrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = OrangePrimary,
    tertiary = SuccessGreen,
    background = DarkBackground,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF2C2C2E),
    outlineVariant = Color(0xFF3A3A3C),
    error = DangerRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA1A1A6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = OrangePrimary,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary = OrangePrimaryDark,
    tertiary = SuccessGreen,
    background = LightBackground,
    surface = White,
    surfaceVariant = Color(0xFFF4F4F6),
    outlineVariant = BorderLight,
    error = DangerRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
  )

@Composable
fun SPOTVTheme(
  darkTheme: Boolean = false,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Disabled dynamic color to enforce GVMC branding
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
