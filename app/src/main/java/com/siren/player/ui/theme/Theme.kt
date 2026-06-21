package com.siren.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1a1a2e),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3a3a5e),
    secondary = Color(0xFFe94560),
    onSecondary = Color.White,
    tertiary = Color(0xFF0f3460),
    background = Color(0xFFf5f5f5),
    surface = Color.White,
    onBackground = Color(0xFF1a1a2e),
    onSurface = Color(0xFF1a1a2e),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFe94560),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFb8001f),
    secondary = Color(0xFF0f3460),
    onSecondary = Color.White,
    tertiary = Color(0xFF16213e),
    background = Color(0xFF0a0a1a),
    surface = Color(0xFF121225),
    onBackground = Color.White,
    onSurface = Color.White,
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

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
