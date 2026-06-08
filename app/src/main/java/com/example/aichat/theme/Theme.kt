package com.example.aichat.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AIChatTheme(
    themeMode: String = "system",
    colorPalette: String = "neon",
    customColor: String = "",
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val primaryColor = if (customColor.isNotBlank()) {
        try {
            Color(android.graphics.Color.parseColor(if (!customColor.startsWith("#")) "#$customColor" else customColor))
        } catch (e: Exception) {
            NeonIndigo
        }
    } else {
        when (colorPalette) {
            "ocean" -> OceanPrimary
            "forest" -> ForestPrimary
            "sunset" -> SunsetPrimary
            else -> NeonIndigo
        }
    }
    
    val secondaryColor = when (colorPalette) {
        "ocean" -> OceanSecondary
        "forest" -> ForestSecondary
        "sunset" -> SunsetSecondary
        else -> NeonPurple
    }

    val tertiaryColor = when (colorPalette) {
        "ocean" -> OceanTertiary
        "forest" -> ForestTertiary
        "sunset" -> SunsetTertiary
        else -> NeonCyan
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = DarkSurfaceVariant,
            onSecondaryContainer = TextPrimary,
            tertiary = tertiaryColor,
            onTertiary = Color.Black,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkSurface,
            onSurface = TextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = TextSecondary,
            error = ErrorRed,
            onError = Color.White,
            outline = TextMuted,
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceVariant,
            onSecondaryContainer = LightTextPrimary,
            tertiary = tertiaryColor,
            onTertiary = Color.White,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            error = ErrorRed,
            onError = Color.White,
            outline = LightTextSecondary,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
