package com.gexetr.lawnroundlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// One palette, two schemes, and no third source of colour.
//
// `MaterialTheme` is built from `Color.kt` and nothing else. The Material baseline and dynamic
// (wallpaper) colour are both deliberately absent: a dynamic scheme would repaint this app in the
// user's wallpaper hue, which is the opposite of a slot that owns its identity, and it would make
// the shipped pixels disagree with `store/studio.json`. The public surface is the single
// `LawnRoundLogTheme` callable below, so a screen can never reach a second scheme by accident.

private val LightScheme = lightColorScheme(
    primary = TurfMid,
    onPrimary = Chalk,
    primaryContainer = TurfWash,
    onPrimaryContainer = TurfDeep,
    inversePrimary = CutLime,
    secondary = ChalkSlate,
    onSecondary = Chalk,
    secondaryContainer = SlateWash,
    onSecondaryContainer = SlateDeep,
    tertiary = OliveDeep,
    onTertiary = Chalk,
    tertiaryContainer = CutLimePale,
    onTertiaryContainer = TurfDeep,
    background = Chalk,
    onBackground = TurfDeep,
    surface = Chalk,
    onSurface = TurfDeep,
    surfaceVariant = TurfWash,
    onSurfaceVariant = TurfLift,
    surfaceTint = TurfMid,
    inverseSurface = TurfDeep,
    inverseOnSurface = Chalk,
    error = Copper,
    onError = Chalk,
    errorContainer = ClayMist,
    onErrorContainer = EmberInk,
    outline = ChalkLine,
    outlineVariant = TurfWash,
    scrim = FrameInk,
    surfaceBright = Chalk,
    surfaceDim = ChalkDim,
    surfaceContainerLowest = Chalk,
    surfaceContainerLow = Chalk,
    surfaceContainer = ChalkDim,
    surfaceContainerHigh = TurfWash,
    surfaceContainerHighest = TurfWash,
    primaryFixed = TurfWash,
    primaryFixedDim = TurfLift,
    onPrimaryFixed = TurfDeep,
    onPrimaryFixedVariant = TurfMid,
    secondaryFixed = SlateWash,
    secondaryFixedDim = ChalkSlate,
    onSecondaryFixed = SlateDeep,
    onSecondaryFixedVariant = ChalkSlate,
    tertiaryFixed = CutLimePale,
    tertiaryFixedDim = CutLime,
    onTertiaryFixed = TurfDeep,
    onTertiaryFixedVariant = OliveDeep,
)

private val DarkScheme = darkColorScheme(
    primary = CutLime,
    onPrimary = FrameInk,
    primaryContainer = TurfLift,
    onPrimaryContainer = Mist,
    inversePrimary = TurfMid,
    secondary = SlateLight,
    onSecondary = SlateDeep,
    secondaryContainer = SlateDeep,
    onSecondaryContainer = SlateLight,
    tertiary = CutLimePale,
    onTertiary = FrameInk,
    tertiaryContainer = TurfLift,
    onTertiaryContainer = CutLimePale,
    background = TurfDeep,
    onBackground = Mist,
    surface = TurfSurface,
    onSurface = Mist,
    surfaceVariant = TurfSurfaceLift,
    onSurfaceVariant = ChalkMuted,
    surfaceTint = CutLime,
    inverseSurface = Chalk,
    inverseOnSurface = TurfSurface,
    error = CopperLight,
    onError = FrameInk,
    errorContainer = EmberInk,
    onErrorContainer = CopperLight,
    outline = TurfLine,
    outlineVariant = TurfSurfaceLift,
    scrim = FrameInk,
    surfaceBright = TurfLift,
    surfaceDim = TurfDeep,
    surfaceContainerLowest = FrameInk,
    surfaceContainerLow = TurfDeep,
    surfaceContainer = TurfSurface,
    surfaceContainerHigh = TurfSurfaceLift,
    surfaceContainerHighest = TurfSurfaceLift,
    primaryFixed = CutLimePale,
    primaryFixedDim = CutLime,
    onPrimaryFixed = TurfDeep,
    onPrimaryFixedVariant = TurfMid,
    secondaryFixed = SlateWash,
    secondaryFixedDim = SlateLight,
    onSecondaryFixed = SlateDeep,
    onSecondaryFixedVariant = SlateDeep,
    tertiaryFixed = CutLimePale,
    tertiaryFixedDim = CutLimeDeep,
    onTertiaryFixed = TurfDeep,
    onTertiaryFixedVariant = OliveDeep,
)

/**
 * The one theme host for the slot.
 *
 * [darkTheme] follows the system by default; the caller owns the choice, which is how the settings
 * surface's light/dark/system preference reaches the pixels. There is deliberately no `dynamicColor`
 * parameter - this slot never draws the user's wallpaper.
 */
@Composable
fun LawnRoundLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = RoundTypography,
        shapes = RoundShapes,
        content = content,
    )
}
