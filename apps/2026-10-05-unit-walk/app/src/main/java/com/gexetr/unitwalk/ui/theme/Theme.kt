package com.gexetr.unitwalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/*
 * UnitWalk's theme. Two schemes, both declared here and both measured: `scripts/core/palette.mjs`
 * resolves the `lightColorScheme` / `darkColorScheme` blocks below against `Color.kt` and holds every
 * `on-X` / `X` pair to WCAG AA 4.5:1 in **both** configurations, which is why no colour in this file
 * is chosen by eye.
 *
 * There is deliberately no dynamic colour. `dynamicLightColorScheme` / `dynamicDarkColorScheme` derive
 * a scheme from the phone's wallpaper: the app would then paint a system nobody chose, the store pack
 * could not have been rendered from it, and the palette gate would be measuring a system that is not
 * the one on the user's screen. The `dynamicColor` parameter below is accepted so a caller can hold
 * one signature, and it is not applied — the app's identity is fixed, and that is the point of having
 * one.
 */

private val UnitWalkLightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
    outline = outlineLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
)

private val UnitWalkDarkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
    outline = outlineDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
)

/**
 * The slot's theme: the app's own palette, its own type set and its own shapes, and nothing else.
 *
 * [dynamicColor] is accepted and not applied — see the file header. Passing it is not an error and
 * changes nothing, which is why the slot's settings surface can carry the switch without the app ever
 * painting a colour the store pack was not rendered from.
 */
@Composable
fun UnitWalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // `dynamicColor` is part of the signature and is deliberately not consulted; the value is
    // ignored by design rather than by omission, which the parameter's own KDoc states.
    MaterialTheme(
        colorScheme = if (darkTheme) UnitWalkDarkScheme else UnitWalkLightScheme,
        typography = UnitWalkTypography,
        shapes = UnitWalkShapes,
        content = content,
    )
}

/**
 * The scaffold's name for [UnitWalkTheme], kept because the app's host and its previews were written
 * against it. New slot code should call [UnitWalkTheme].
 */
@Composable
fun PlayKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    UnitWalkTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
