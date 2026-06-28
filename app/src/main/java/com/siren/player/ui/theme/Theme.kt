package com.siren.player.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1a1a2e),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1a1a2e),
    secondary = Color(0xFFe94560),
    onSecondary = Color.White,
    tertiary = Color(0xFF0f3460),
    background = Color(0xFFf5f5f5),
    surface = Color(0xFFfafafa),
    surfaceContainer = Color(0xFFf0f0f5),
    onSurface = Color(0xFF1a1a2e),
    onSurfaceVariant = Color(0xFF4a4a5e),
    onBackground = Color(0xFF1a1a2e),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFe94560),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1e1e35),
    secondary = Color(0xFF0f3460),
    onSecondary = Color.White,
    tertiary = Color(0xFF16213e),
    background = Color(0xFF0a0a1a),
    surface = Color(0xFF121225),
    surfaceContainer = Color(0xFF1a1a30),
    onSurface = Color(0xFFe0e0e0),
    onSurfaceVariant = Color(0xFFb0b0c0),
    onBackground = Color.White,
)

@Composable
fun SirenTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.SYSTEM -> {
            if (isSystemDark) DarkColorScheme else LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Light status bar icons (dark icons) when the app is in light theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
