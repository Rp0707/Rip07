package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = FarmGreenContainer,
    onPrimaryContainer = FarmGreenOnContainer,
    secondary = FarmEarthSecondary,
    onSecondary = Color.White,
    secondaryContainer = FarmEarthContainer,
    onSecondaryContainer = FarmEarthOnContainer,
    tertiary = FarmHarvestGold,
    onTertiary = Color.White,
    tertiaryContainer = FarmGoldContainer,
    onTertiaryContainer = FarmGoldOnContainer,
    background = FarmBackground,
    surface = FarmSurface,
    surfaceVariant = FarmSurfaceVariant,
    onBackground = FarmTextPrimary,
    onSurface = FarmTextPrimary,
    onSurfaceVariant = FarmTextSecondary,
    outline = FarmOutline
)

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = FarmGreenContainer,
    onPrimaryContainer = FarmGreenOnContainer,
    secondary = FarmEarthSecondary,
    onSecondary = Color.White,
    secondaryContainer = FarmEarthContainer,
    onSecondaryContainer = FarmEarthOnContainer,
    tertiary = FarmHarvestGold,
    onTertiary = Color.White,
    tertiaryContainer = FarmGoldContainer,
    onTertiaryContainer = FarmGoldOnContainer,
    background = FarmBackground,
    surface = FarmSurface,
    surfaceVariant = FarmSurfaceVariant,
    onBackground = FarmTextPrimary,
    onSurface = FarmTextPrimary,
    onSurfaceVariant = FarmTextSecondary,
    outline = FarmOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
