package com.gexetr.teachergradebook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Chalk.Indigo40,
    onPrimary = Chalk.Paper,
    primaryContainer = Chalk.Indigo95,
    onPrimaryContainer = Chalk.Indigo10,
    secondary = Chalk.Teal40,
    onSecondary = Chalk.Paper,
    secondaryContainer = Chalk.Teal95,
    onSecondaryContainer = Chalk.Teal10,
    tertiary = Chalk.Amber40,
    onTertiary = Chalk.Paper,
    tertiaryContainer = Chalk.Amber95,
    onTertiaryContainer = Chalk.Amber10,
    error = Chalk.Rose40,
    onError = Chalk.Paper,
    errorContainer = Chalk.Rose95,
    onErrorContainer = Chalk.Rose10,
    background = Chalk.Ink96,
    onBackground = Chalk.Ink10,
    surface = Chalk.Paper,
    onSurface = Chalk.Ink10,
    surfaceVariant = Chalk.Ink92,
    onSurfaceVariant = Chalk.Ink30,
    surfaceContainer = Chalk.Ink92,
    surfaceContainerHigh = Chalk.Ink85,
    outline = Chalk.Ink70,
    outlineVariant = Chalk.Ink85,
    inverseSurface = Chalk.Ink10,
    inverseOnSurface = Chalk.Ink96,
)

private val DarkColorScheme = darkColorScheme(
    primary = Chalk.Indigo80,
    onPrimary = Chalk.Indigo10,
    primaryContainer = Chalk.Indigo30,
    onPrimaryContainer = Chalk.Indigo95,
    secondary = Chalk.Teal80,
    onSecondary = Chalk.Teal10,
    secondaryContainer = Chalk.Teal20,
    onSecondaryContainer = Chalk.Teal95,
    tertiary = Chalk.Amber80,
    onTertiary = Chalk.Amber10,
    tertiaryContainer = Chalk.Amber30,
    onTertiaryContainer = Chalk.Amber95,
    error = Chalk.Rose80,
    onError = Chalk.Rose10,
    errorContainer = Chalk.Rose20,
    onErrorContainer = Chalk.Rose95,
    background = Chalk.Night10,
    onBackground = Chalk.Night90,
    surface = Chalk.Night14,
    onSurface = Chalk.Night90,
    surfaceVariant = Chalk.Night28,
    onSurfaceVariant = Chalk.Night75,
    surfaceContainer = Chalk.Night20,
    surfaceContainerHigh = Chalk.Night28,
    outline = Chalk.Night60,
    outlineVariant = Chalk.Night40,
    inverseSurface = Chalk.Night90,
    inverseOnSurface = Chalk.Night10,
)

/**
 * Generous radii, because a grade book is read at arm's length on a desk and in a corridor.
 * `extraLarge` also drives the corner of the hero panel and the larger tiles.
 */
private val ChalkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Colours the Material scheme has no slot for, because they mean something in this app rather than
 * in a design system: a grade cell's state, and the traffic-light reading of an average.
 *
 * They are provided as a CompositionLocal rather than as top-level constants so dark mode can
 * choose different values (a saturated amber that reads as a warning on white is unreadable on
 * near-black) while every call site stays a plain `MarkBookColors.gradeMissing`.
 */
@Immutable
data class MarkBookColors(
    val present: Color,
    val presentContainer: Color,
    val late: Color,
    val lateContainer: Color,
    val absent: Color,
    val absentContainer: Color,
    val exempt: Color,
    val exemptContainer: Color,
    val unmarked: Color,
    val unmarkedContainer: Color,
    val atRisk: Color,
    val onTrack: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
)

private val LightMarkBookColors = MarkBookColors(
    present = Chalk.Teal40,
    presentContainer = Chalk.Teal95,
    late = Chalk.Amber40,
    lateContainer = Chalk.Amber95,
    absent = Chalk.Rose40,
    absentContainer = Chalk.Rose95,
    exempt = Chalk.Ink50,
    exemptContainer = Chalk.Ink92,
    unmarked = Chalk.Ink50,
    unmarkedContainer = Chalk.Ink92,
    atRisk = Chalk.Amber40,
    onTrack = Chalk.Teal40,
    heroStart = Chalk.Indigo40,
    heroEnd = Chalk.Indigo20,
    onHero = Chalk.Paper,
    onHeroMuted = Chalk.Indigo90,
)

private val DarkMarkBookColors = MarkBookColors(
    present = Chalk.Teal80,
    presentContainer = Chalk.Teal20,
    late = Chalk.Amber80,
    lateContainer = Chalk.Amber30,
    absent = Chalk.Rose80,
    absentContainer = Chalk.Rose20,
    exempt = Chalk.Night75,
    exemptContainer = Chalk.Night28,
    unmarked = Chalk.Night75,
    unmarkedContainer = Chalk.Night28,
    atRisk = Chalk.Amber80,
    onTrack = Chalk.Teal80,
    heroStart = Chalk.Indigo30,
    heroEnd = Chalk.Night20,
    onHero = Chalk.Paper,
    onHeroMuted = Chalk.Indigo80,
)

/**
 * The app's own colour roles. A `CompositionLocal` rather than constants so a call site is a plain
 * `markBookColors.gradeMissing` and dark mode can supply its own values.
 */
val LocalMarkBookColors = staticCompositionLocalOf { LightMarkBookColors }

/** The current palette: `MaterialTheme.colorScheme` for standard roles, this for the app's own. */
val markBookColors: MarkBookColors
    @Composable get() = LocalMarkBookColors.current

@Composable
fun PlayKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkMarkBookColors else LightMarkBookColors

    // Dynamic colour is deliberately not offered. It repaints the app from the device wallpaper,
    // which makes the brand unreproducible - the store screenshots would not match what a buyer
    // sees - and on muted wallpapers it desaturated the whole app toward grey. A paid app's
    // identity should not be a function of someone else's home screen.
    CompositionLocalProvider(LocalMarkBookColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PlayKilnTypography,
            shapes = ChalkShapes,
            content = content,
        )
    }
}
