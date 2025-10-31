package com.example.logoocr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentSilver,
    onPrimary = Color.Black,
    primaryContainer = DarkGraySecondary,
    onPrimaryContainer = AccentSilver,
    secondary = AccentSilver,
    onSecondary = Color.Black,
    background = DarkGrayPrimary,
    onBackground = AccentSilver,
    surface = DarkGraySecondary,
    onSurface = AccentSilver
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGrayPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentSilver,
    onPrimaryContainer = DarkGrayPrimary,
    secondary = DarkGraySecondary,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = DarkGrayPrimary,
    surface = Color.White,
    onSurface = DarkGrayPrimary
)

@Composable
fun LogoOcrTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
