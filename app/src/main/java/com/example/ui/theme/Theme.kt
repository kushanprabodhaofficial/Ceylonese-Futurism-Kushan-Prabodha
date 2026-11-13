package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

private val DarkColorScheme = darkColorScheme(
    primary = CosmicCyan,
    onPrimary = CosmicOnCyan,
    primaryContainer = CosmicCyanContainer,
    onPrimaryContainer = CosmicOnCyanContainer,
    secondary = SolarAmber,
    onSecondary = CosmicOnAmber,
    secondaryContainer = SolarAmberContainer,
    onSecondaryContainer = CosmicOnAmberContainer,
    tertiary = NebulaViolet,
    onTertiary = CosmicOnViolet,
    tertiaryContainer = NebulaVioletContainer,
    onTertiaryContainer = CosmicOnVioletContainer,
    background = CosmicBackground,
    onBackground = CosmicTextPrimary,
    surface = CosmicSurface,
    onSurface = CosmicTextPrimary,
    surfaceVariant = CosmicSurfaceHigh,
    onSurfaceVariant = CosmicTextSecondary,
    surfaceContainerLowest = CosmicBackground,
    surfaceContainerLow = CosmicSurfaceLow,
    surfaceContainer = CosmicSurface,
    surfaceContainerHigh = CosmicSurfaceHigh,
    surfaceContainerHighest = CosmicSurfaceHighest,
    outline = CosmicOutline,
    outlineVariant = CosmicOutlineVariant,
    error = CosmicError
)

private val LightColorScheme = lightColorScheme(
    primary = OceanCyan,
    onPrimary = ParchmentOnCyan,
    primaryContainer = OceanCyanContainer,
    onPrimaryContainer = ParchmentOnCyanContainer,
    secondary = TerracottaAmber,
    onSecondary = ParchmentOnAmber,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = ParchmentOnAmberContainer,
    tertiary = DuskViolet,
    onTertiary = ParchmentOnViolet,
    tertiaryContainer = DuskVioletContainer,
    onTertiaryContainer = ParchmentOnVioletContainer,
    background = ParchmentBackground,
    onBackground = ParchmentTextPrimary,
    surface = ParchmentSurface,
    onSurface = ParchmentTextPrimary,
    surfaceVariant = ParchmentSurfaceHigh,
    onSurfaceVariant = ParchmentTextSecondary,
    surfaceContainerLowest = ParchmentSurface,
    surfaceContainerLow = ParchmentSurfaceLow,
    surfaceContainer = ParchmentSurfaceLow,
    surfaceContainerHigh = ParchmentSurfaceHigh,
    surfaceContainerHighest = ParchmentSurfaceHighest,
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineVariant,
    error = ParchmentError
)

val CeyloneseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CeyloneseFuturismTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CeyloneseTypography,
        shapes = CeyloneseShapes,
        content = content
    )
}
