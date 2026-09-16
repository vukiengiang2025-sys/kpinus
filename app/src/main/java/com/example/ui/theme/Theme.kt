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

private val NamDuocDarkColorScheme = darkColorScheme(
    primary = Blue500,
    secondary = Purple500,
    tertiary = Emerald500,
    background = Slate950,
    surface = Slate900,
    surfaceVariant = Slate800,
    onPrimary = Slate950,
    onSecondary = Slate950,
    onTertiary = Slate950,
    onBackground = Slate200,
    onSurface = Slate200,
    onSurfaceVariant = Slate400,
    error = Red500
)

private val NamDuocLightColorScheme = lightColorScheme(
    primary = Color(0xFF1769AA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF6750A4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF211047),
    tertiary = Color(0xFF147D64),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA5F2D8),
    onTertiaryContainer = Color(0xFF002019),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EEF5),
    onBackground = Color(0xFF17202A),
    onSurface = Color(0xFF17202A),
    onSurfaceVariant = Color(0xFF4B5B6B),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NamDuocDarkColorScheme else NamDuocLightColorScheme,
        typography = Typography,
        content = content
    )
}
