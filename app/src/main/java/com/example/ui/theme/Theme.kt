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

private val LightColorScheme = lightColorScheme(
    primary = DeepSlateTeal,
    onPrimary = Color.White,
    primaryContainer = SlateTealContainer,
    onPrimaryContainer = OnSlateTealContainer,
    secondary = TerracottaCoral,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = AmberGold,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = IvoryBackground,
    onBackground = InkCharcoal,
    surface = WarmPaperSurface,
    onSurface = InkCharcoal,
    surfaceVariant = LinenSurfaceVariant,
    onSurfaceVariant = MutedInk,
    outline = WarmStoneOutline,
    outlineVariant = SoftStoneDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = BrightAquaPrimary,
    onPrimary = Color(0xFF062523),
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = Color(0xFFC1F2EE),
    secondary = WarmCoralDark,
    onSecondary = Color(0xFF47100A),
    secondaryContainer = DarkCoralContainer,
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = WarmGoldDark,
    onTertiary = Color(0xFF3E2600),
    tertiaryContainer = DarkGoldContainer,
    onTertiaryContainer = Color(0xFFFFDFAA),
    background = MidnightBackground,
    onBackground = CloudText,
    surface = MidnightSurface,
    onSurface = CloudText,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MutedCloudText,
    outline = MidnightOutline,
    outlineVariant = Color(0xFF2B3437)
)

val CadenceShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CadenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CadenceTypography,
        shapes = CadenceShapes,
        content = content
    )
}
