package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    secondary = SpotifyGreen,
    tertiary = SurfaceColor,
    background = DarkBackground,
    surface = SurfaceColor,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = LightText,
    onBackground = LightText,
    onSurface = LightText,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = GrayText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
