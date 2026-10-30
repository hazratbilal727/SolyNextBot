package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GptWhite,
    onPrimary = GptBlack,
    primaryContainer = GptSurfaceElevated,
    onPrimaryContainer = GptTextPrimary,
    secondary = GptAccent,
    onSecondary = GptBlack,
    secondaryContainer = GptSurface,
    onSecondaryContainer = GptTextPrimary,
    tertiary = GptAccentBlue,
    onTertiary = GptWhite,
    background = GptBackground,
    onBackground = GptTextPrimary,
    surface = GptSurface,
    onSurface = GptTextPrimary,
    surfaceVariant = GptSurfaceElevated,
    onSurfaceVariant = GptTextSecondary,
    outline = GptBorder,
    error = AlertCrimson,
    onError = GptWhite
)

private val LightColorScheme = lightColorScheme(
    primary = GptBlack,
    onPrimary = GptWhite,
    primaryContainer = Color(0xFFF4F4F4),
    onPrimaryContainer = GptBlack,
    secondary = GptAccent,
    onSecondary = GptWhite,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0D0D0D),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFFEBEBEB),
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFFE0E0E0),
    error = AlertCrimson,
    onError = Color.White
)

@Composable
fun SolyNextTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
