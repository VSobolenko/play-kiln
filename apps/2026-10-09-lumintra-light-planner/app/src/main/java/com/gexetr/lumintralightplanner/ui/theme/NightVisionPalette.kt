package com.gexetr.lumintralightplanner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// The true night-vision palette: red on black, a full remapping of every role rather than a filter.
// Every colour below has a blue channel of 0x00 and a green channel of 0x00, because rod vision is
// least sensitive to long-wavelength red and any green or blue light costs the dark adaptation the
// user spent twenty minutes building. Hierarchy comes from weight and size, not from hue.
// Text roles stay at or above 4.5:1 on the surface they sit on (WCAG 1.4.3), which is why the dimmest
// text red is 0xF5 and not lower.

internal val RedBlack = Color(0xFF000000)
internal val RedContainerLow = Color(0xFF0A0000)
internal val RedContainer = Color(0xFF100000)
internal val RedContainerHigh = Color(0xFF160000)
internal val RedContainerHighest = Color(0xFF1C0000)
internal val RedVariant = Color(0xFF140000)
internal val RedPrimaryContainer = Color(0xFF330000)
internal val RedSecondaryContainer = Color(0xFF260000)
internal val RedOutline = Color(0xFF8C0000)
internal val RedOutlineVariant = Color(0xFF4D0000)
internal val RedHairline = Color(0xFF3A0000)
internal val RedText = Color(0xFFFF0000)
internal val RedTextMuted = Color(0xFFF50000)
internal val RedShadow = Color(0xFF240000)

internal val NightVisionScheme: ColorScheme = darkColorScheme(
    primary = RedText,
    onPrimary = RedBlack,
    primaryContainer = RedPrimaryContainer,
    onPrimaryContainer = RedText,
    inversePrimary = RedPrimaryContainer,
    secondary = RedText,
    onSecondary = RedBlack,
    secondaryContainer = RedSecondaryContainer,
    onSecondaryContainer = RedText,
    tertiary = RedText,
    onTertiary = RedBlack,
    tertiaryContainer = RedSecondaryContainer,
    onTertiaryContainer = RedText,
    error = RedText,
    onError = RedBlack,
    errorContainer = RedText,
    onErrorContainer = RedBlack,
    background = RedBlack,
    onBackground = RedText,
    surface = RedBlack,
    onSurface = RedText,
    surfaceVariant = RedVariant,
    onSurfaceVariant = RedTextMuted,
    surfaceTint = RedBlack,
    inverseSurface = RedText,
    inverseOnSurface = RedBlack,
    outline = RedOutline,
    outlineVariant = RedOutlineVariant,
    scrim = RedBlack,
    surfaceBright = RedContainerHighest,
    surfaceDim = RedBlack,
    surfaceContainerLowest = RedBlack,
    surfaceContainerLow = RedContainerLow,
    surfaceContainer = RedContainer,
    surfaceContainerHigh = RedContainerHigh,
    surfaceContainerHighest = RedContainerHighest,
)

internal val NightVisionColors = LumintraColors(
    skyZenith = RedBlack,
    skyHorizon = RedContainerLow,
    onSky = RedText,
    onSkyMuted = RedTextMuted,
    skyGlow = RedContainer,
    firstLight = RedText,
    onFirstLight = RedBlack,
    blueHour = RedTextMuted,
    moonlight = RedTextMuted,
    darkWindow = RedText,
    cloud = RedOutline,
    caution = RedText,
    hairline = RedHairline,
    markInk = RedText,
    markBody = RedText,
    moonLit = RedTextMuted,
    moonShadow = RedShadow,
)
