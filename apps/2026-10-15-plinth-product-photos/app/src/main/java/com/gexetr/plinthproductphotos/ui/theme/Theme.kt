package com.gexetr.plinthproductphotos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PlinthShapes = Shapes(
    small = CircleShape,
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

internal val LightColorScheme = lightColorScheme(
    primary = PlinthPalette.Amber,
    onPrimary = PlinthPalette.Ink,
    primaryContainer = PlinthPalette.AmberPale,
    onPrimaryContainer = PlinthPalette.Ink,
    secondary = PlinthPalette.AccentDeep,
    onSecondary = PlinthPalette.Paper,
    secondaryContainer = PlinthPalette.AmberPale,
    onSecondaryContainer = PlinthPalette.Ink,
    tertiary = PlinthPalette.AmberDeep,
    onTertiary = PlinthPalette.Paper,
    tertiaryContainer = PlinthPalette.AmberPale,
    onTertiaryContainer = PlinthPalette.Ink,
    background = PlinthPalette.Paper,
    onBackground = PlinthPalette.Ink,
    surface = PlinthPalette.Paper,
    surfaceTint = PlinthPalette.Paper,
    onSurface = PlinthPalette.Ink,
    surfaceVariant = PlinthPalette.ContainerLight,
    onSurfaceVariant = PlinthPalette.Muted,
    surfaceDim = PlinthPalette.RaisedLight,
    surfaceBright = PlinthPalette.Paper,
    surfaceContainerLowest = PlinthPalette.Paper,
    surfaceContainerLow = PlinthPalette.BenchLight,
    surfaceContainer = PlinthPalette.ContainerLight,
    surfaceContainerHigh = PlinthPalette.ContainerHighLight,
    surfaceContainerHighest = PlinthPalette.RaisedLight,
    outline = PlinthPalette.Muted,
    outlineVariant = PlinthPalette.DividerLight,
    inverseSurface = PlinthPalette.Ink,
    inverseOnSurface = PlinthPalette.Paper,
    inversePrimary = PlinthPalette.Lane,
    error = PlinthPalette.Error,
    onError = PlinthPalette.Paper,
    errorContainer = PlinthPalette.ErrorPale,
    onErrorContainer = PlinthPalette.Ink,
    scrim = PlinthPalette.Bezel,
)

internal val DarkColorScheme = darkColorScheme(
    primary = PlinthPalette.Lane,
    onPrimary = PlinthPalette.Ink,
    primaryContainer = PlinthPalette.AccentDeep,
    onPrimaryContainer = PlinthPalette.Paper,
    secondary = PlinthPalette.AmberPale,
    onSecondary = PlinthPalette.Ink,
    secondaryContainer = PlinthPalette.AmberDeep,
    onSecondaryContainer = PlinthPalette.AmberPale,
    tertiary = PlinthPalette.AmberPale,
    onTertiary = PlinthPalette.Ink,
    tertiaryContainer = PlinthPalette.AmberDeep,
    onTertiaryContainer = PlinthPalette.AmberPale,
    background = PlinthPalette.Ink,
    onBackground = PlinthPalette.Paper,
    surface = PlinthPalette.InkSoft,
    surfaceTint = PlinthPalette.InkSoft,
    onSurface = PlinthPalette.Paper,
    surfaceVariant = PlinthPalette.InkLift,
    onSurfaceVariant = PlinthPalette.BezelRim,
    surfaceDim = PlinthPalette.Ink,
    surfaceBright = PlinthPalette.RaisedDark,
    surfaceContainerLowest = PlinthPalette.Ink,
    surfaceContainerLow = PlinthPalette.ContainerLowDark,
    surfaceContainer = PlinthPalette.ContainerDark,
    surfaceContainerHigh = PlinthPalette.ContainerHighDark,
    surfaceContainerHighest = PlinthPalette.RaisedDark,
    outline = PlinthPalette.BezelRim,
    outlineVariant = PlinthPalette.DividerDark,
    inverseSurface = PlinthPalette.Paper,
    inverseOnSurface = PlinthPalette.Ink,
    inversePrimary = PlinthPalette.AccentDeep,
    error = PlinthPalette.ErrorPale,
    onError = PlinthPalette.Ink,
    errorContainer = PlinthPalette.ErrorDeep,
    onErrorContainer = PlinthPalette.Paper,
    scrim = PlinthPalette.Bezel,
)

@Composable
fun PlayKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalPlinthAccentInk provides if (darkTheme) PlinthPalette.Amber else PlinthPalette.AmberDeep,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = PlinthShapes,
            typography = PlayKilnTypography,
            content = content,
        )
    }
}

private val LocalPlinthAccentInk = staticCompositionLocalOf { PlinthPalette.AmberDeep }

internal object PlinthTheme {
    val accentInk: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalPlinthAccentInk.current
}
