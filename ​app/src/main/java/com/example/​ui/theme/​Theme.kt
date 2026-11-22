package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightStudioColorScheme = lightColorScheme(
    primary = MangoOrange,
    onPrimary = Color.White,
    primaryContainer = MangoOrangeContainer,
    onPrimaryContainer = OnMangoContainer,
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = OnSkyContainer,
    tertiary = LeafEmerald,
    onTertiary = Color.White,
    tertiaryContainer = LeafEmeraldContainer,
    onTertiaryContainer = OnLeafContainer,
    background = WarmCreamBg,
    onBackground = StudioInk,
    surface = WarmSurface,
    onSurface = StudioInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = StudioMutedInk,
    outline = StudioOutline,
    error = BerryPink,
    onError = Color.White
)

private val DarkStudioColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A55),
    onPrimary = Color(0xFF431600),
    primaryContainer = Color(0xFF6B2808),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF002E52),
    secondaryContainer = Color(0xFF0D477A),
    onSecondaryContainer = Color(0xFFD6EAFF),
    tertiary = Color(0xFF66D190),
    onTertiary = Color(0xFF00391B),
    tertiaryContainer = Color(0xFF0E532E),
    onTertiaryContainer = Color(0xFFCFF8DD),
    background = DarkStudioBg,
    onBackground = DarkStudioInk,
    surface = DarkStudioSurface,
    onSurface = DarkStudioInk,
    surfaceVariant = DarkStudioSurfaceVariant,
    onSurfaceVariant = DarkStudioMutedInk,
    outline = DarkStudioOutline,
    error = Color(0xFFFF6B95),
    onError = Color(0xFF4F001C)
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CartoonStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkStudioColorScheme else LightStudioColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
