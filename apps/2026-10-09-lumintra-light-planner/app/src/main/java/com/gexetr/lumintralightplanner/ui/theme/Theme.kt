package com.gexetr.lumintralightplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// Lumintra owns its schemes. There is deliberately no dynamic colour and no Material seed: the
// identity in store/studio.json is the identity on screen, and a wallpaper-derived palette would
// make the pack a screenshot of a different app.

private val LumintraLightScheme = lightColorScheme(
    primary = EmberDeep,
    onPrimary = DayContainerLowest,
    primaryContainer = EmberPale,
    onPrimaryContainer = EmberInk,
    inversePrimary = Ember,
    secondary = MoonDay,
    onSecondary = DayContainerLowest,
    secondaryContainer = MoonPale,
    onSecondaryContainer = MoonInk,
    tertiary = AuroraDeep,
    onTertiary = DayContainerLowest,
    tertiaryContainer = AuroraPale,
    onTertiaryContainer = AuroraInk,
    error = AlertDay,
    onError = DayContainerLowest,
    errorContainer = AlertDayContainer,
    onErrorContainer = AlertDayInk,
    background = DayPaper,
    onBackground = DayInk,
    surface = DaySurface,
    onSurface = DayInk,
    surfaceVariant = DayVariant,
    onSurfaceVariant = DayInkMuted,
    surfaceTint = EmberDeep,
    inverseSurface = DayInverse,
    inverseOnSurface = DayInverseInk,
    outline = DayOutline,
    outlineVariant = DayOutlineVariant,
    scrim = Scrim,
    surfaceBright = DayContainerLowest,
    surfaceDim = DayContainerHighest,
    surfaceContainerLowest = DayContainerLowest,
    surfaceContainerLow = DayContainerLow,
    surfaceContainer = DayContainer,
    surfaceContainerHigh = DayContainerHigh,
    surfaceContainerHighest = DayContainerHighest,
)

private val LumintraDarkScheme = darkColorScheme(
    primary = Ember,
    onPrimary = EmberInk,
    primaryContainer = EmberNight,
    onPrimaryContainer = EmberPale,
    inversePrimary = EmberDeep,
    secondary = Moonlight,
    onSecondary = MoonInk,
    secondaryContainer = MoonDeep,
    onSecondaryContainer = MoonPale,
    tertiary = Aurora,
    onTertiary = AuroraInk,
    tertiaryContainer = AuroraNight,
    onTertiaryContainer = AuroraPale,
    error = AlertNight,
    onError = AlertNightInk,
    errorContainer = AlertNightContainer,
    onErrorContainer = AlertNightPale,
    background = NightInk,
    onBackground = Starlight,
    surface = NightInkSoft,
    onSurface = Starlight,
    surfaceVariant = NightInkLift,
    onSurfaceVariant = StarlightMuted,
    surfaceTint = Ember,
    inverseSurface = Starlight,
    inverseOnSurface = NightInkSoft,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    scrim = Scrim,
    surfaceBright = NightContainerHighest,
    surfaceDim = NightInk,
    surfaceContainerLowest = NightInk,
    surfaceContainerLow = NightContainerLow,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightContainerHigh,
    surfaceContainerHighest = NightContainerHighest,
)

/** Which of the four identity states is being drawn. Red mode remaps every role of both. */
enum class LumintraMode { Day, Night, RedDay, RedNight }

private val LocalLumintraColors = staticCompositionLocalOf { LumintraDarkColors }
private val LocalLumintraMode = staticCompositionLocalOf { LumintraMode.Night }

/**
 * The slot's theme. [redMode] wins over [darkTheme]: the true night-vision palette replaces every
 * colour role of both schemes, so nothing on any screen can emit blue or green light.
 */
@Composable
fun LumintraTheme(
    redMode: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val mode = when {
        redMode && darkTheme -> LumintraMode.RedNight
        redMode -> LumintraMode.RedDay
        darkTheme -> LumintraMode.Night
        else -> LumintraMode.Day
    }
    val colorScheme: ColorScheme = when (mode) {
        LumintraMode.Day -> LumintraLightScheme
        LumintraMode.Night -> LumintraDarkScheme
        LumintraMode.RedDay, LumintraMode.RedNight -> NightVisionScheme
    }
    val extended: LumintraColors = when (mode) {
        LumintraMode.Day -> LumintraLightColors
        LumintraMode.Night -> LumintraDarkColors
        LumintraMode.RedDay, LumintraMode.RedNight -> NightVisionColors
    }
    CompositionLocalProvider(
        LocalLumintraColors provides extended,
        LocalLumintraMode provides mode,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LumintraTypography,
            shapes = LumintraShapes,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onBackground,
                content = content,
            )
        }
    }
}

/**
 * Compatibility entry point for the scaffold's call sites until they move to [LumintraTheme].
 * `dynamicColor` is accepted and ignored: Lumintra never derives its palette from the wallpaper.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun PlayKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    redMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    LumintraTheme(redMode = redMode, darkTheme = darkTheme, content = content)
}

/** Reads the slot's extended roles and mode: `LumintraTheme.colors.dawnGlow`, `LumintraTheme.mode`. */
object LumintraTheme {
    val colors: LumintraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLumintraColors.current

    val mode: LumintraMode
        @Composable
        @ReadOnlyComposable
        get() = LocalLumintraMode.current

    val isRedMode: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalLumintraMode.current == LumintraMode.RedDay ||
            LocalLumintraMode.current == LumintraMode.RedNight
}
