package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShadowDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = DeepObsidian,
    primaryContainer = DeepCobalt,
    onPrimaryContainer = CyberCyan,
    secondary = NeonCobalt,
    onSecondary = Color.White,
    secondaryContainer = ElevatedCard,
    onSecondaryContainer = SilverAccent,
    tertiary = CrimsonBoost,
    onTertiary = Color.White,
    background = DeepObsidian,
    onBackground = Color.White,
    surface = MidnightSurface,
    onSurface = Color.White,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = SilverAccent,
    outline = BorderGlow
)

private val ShadowShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ShadowRacerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShadowDarkColorScheme,
        typography = ShadowRacerTypography,
        shapes = ShadowShapes,
        content = content
    )
}
