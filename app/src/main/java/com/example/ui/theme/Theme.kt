package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VectraDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = VoidBlack,
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = ShieldEmerald,
    onSecondary = VoidBlack,
    secondaryContainer = ShieldEmeraldContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = IncognitoViolet,
    onTertiary = VoidBlack,
    tertiaryContainer = IncognitoContainer,
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = VoidBlack,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianOutline,
    error = CrimsonError,
    onError = Color.White
)

private val VectraLightColorScheme = lightColorScheme(
    primary = DeepCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = OnDeepCyanContainer,
    secondary = ShieldEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF2E1065),
    background = CrispBackground,
    onBackground = TextPrimaryLight,
    surface = CrispSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CrispElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = CrispOutline,
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun VectraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme || isIncognito) VectraDarkColorScheme else VectraLightColorScheme
    val colorScheme = if (isIncognito) {
        baseScheme.copy(
            primary = IncognitoViolet,
            primaryContainer = IncognitoContainer,
            onPrimaryContainer = Color(0xFFEDE9FE)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VectraTypography,
        content = content
    )
}
