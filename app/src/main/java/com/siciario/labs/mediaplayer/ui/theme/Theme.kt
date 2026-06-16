package com.siciario.labs.mediaplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light colors
private val LightPrimaryColor = Color(0xFF6200F3)
private val LightSecondaryColor = Color(0xFF03DAC6)
private val LightTertiaryColor = Color(0xFF018786)
private val LightBackgroundColor = Color(0xFFFFFFFF)
private val LightSurfaceColor = Color(0xFFF5F5F5)

// Dark colors
private val DarkPrimaryColor = Color(0xFFBB86FC)
private val DarkSecondaryColor = Color(0xFF03DAC6)
private val DarkTertiaryColor = Color(0xFF03DAC6)
private val DarkBackgroundColor = Color(0xFF121212)
private val DarkSurfaceColor = Color(0xFF1F1F1F)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryColor,
    secondary = LightSecondaryColor,
    tertiary = LightTertiaryColor,
    background = LightBackgroundColor,
    surface = LightSurfaceColor
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryColor,
    secondary = DarkSecondaryColor,
    tertiary = DarkTertiaryColor,
    background = DarkBackgroundColor,
    surface = DarkSurfaceColor
)

@Composable
fun SicarioLabsMediaPlayerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
