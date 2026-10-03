package com.gexetr.laneset.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Laneset's own theme host.
 *
 * Two schemes, both declared by hand from `Color.kt`, and nothing else. There is no purple seed —
 * the Material 3 baseline palette belongs to no app and this slot's whole field/ink/accent system is
 * its own — and there is no dynamic colour *default*: Material You is opt-in from Settings, because
 * the identity is what the store pack was composed from and a wallpaper must not repaint it without
 * the swimmer asking.
 *
 * The palette below is the one `store/studio.json` declares, and `scripts/core/palette.mjs` compares
 * the two by value on every `verify.mjs --slot laneset`. Every role resolves to a constant in
 * `Color.kt`, so the pack and the app cannot drift: there is no literal in this file.
 */

/**
 * The roles Material 3 does not have a slot for.
 *
 * A swimming set needs to say five things M3's vocabulary cannot: the target time (pace), the
 * recovery interval (rest), the lane rope (lane), a set that cannot be held (alarm), and the hero
 * panel's own gradient. They are read out of the composition through [laneSetColors] rather than
 * being pushed into `ColorScheme` fields whose names would then lie about what they mean.
 *
 * Every one of them is a declared studio role or a constant derived from one, and every pair that is
 * read as text clears WCAG AA in both schemes.
 */
@Immutable
data class LaneSetColors(
    /** The target time on the clock. Nothing else in the app is this colour. */
    val pace: Color,
    /** Ink for a target time set on [pace]. */
    val onPace: Color,
    /** The warm wash a pace chip or a target row is drawn on. */
    val paceContainer: Color,
    /** Ink for text on [paceContainer]. */
    val onPaceContainer: Color,
    /** The recovery mint, as a fill — a rest interval's bar, a completed block. */
    val rest: Color,
    /** Ink for text set on [rest]. */
    val onRest: Color,
    /** The recovery green read as *text* on the ordinary surface, where the mint itself is too pale. */
    val restOnSurface: Color,
    /** The recovery wash a rest chip is drawn on. */
    val restContainer: Color,
    /** Ink for text on [restContainer]. */
    val onRestContainer: Color,
    /** The lane-rope blue, as a fill — a section chip, a progress track. */
    val lane: Color,
    /** Ink for text set on [lane]. */
    val onLane: Color,
    /** The lane blue read as text on the ordinary surface. */
    val laneOnSurface: Color,
    /** A set the swimmer cannot hold at the pace they entered. */
    val alarm: Color,
    /** Ink for text set on [alarm]. */
    val onAlarm: Color,
    /** The hero panel's gradient: the deep end and the shallow end. */
    val heroStart: Color,
    val heroEnd: Color,
    /** The hero's primary ink, and the quieter second line under it. */
    val onHero: Color,
    val onHeroMuted: Color,
)

/**
 * The light register: a whiteboard at dawn.
 *
 * The field is `paper` and the ink is the deep end of the pool, so a set list reads the way a
 * coach's marker does. `primary` is that same deep pool blue rather than the pale sky blue, because
 * a filled control needs ink that survives on it; `secondary` is the mint and is only ever a fill,
 * with [LaneSetColors.restOnSurface] carrying mint-as-text on light.
 */
private val LightColors = lightColorScheme(
    primary = PoolDeep,
    onPrimary = DeckPaper,
    primaryContainer = PoolMist,
    onPrimaryContainer = PoolMistInk,
    secondary = RestTide,
    onSecondary = RestDeep,
    secondaryContainer = RestPale,
    onSecondaryContainer = RestDeep,
    tertiary = PaceDeep,
    onTertiary = DeckPaper,
    tertiaryContainer = PacePale,
    onTertiaryContainer = PaceInk,
    error = AlarmDeep,
    onError = DeckWhite,
    errorContainer = AlarmPale,
    onErrorContainer = AlarmInk,
    background = DeckPaper,
    onBackground = PoolDeep,
    surface = DeckWhite,
    onSurface = PoolDeep,
    surfaceVariant = DeckWash,
    onSurfaceVariant = DeckMuted,
    surfaceContainerLowest = DeckWhite,
    surfaceContainerLow = DeckPaper,
    surfaceContainer = DeckWash,
    surfaceContainerHigh = DeckWashHigh,
    surfaceContainerHighest = DeckWashHigh,
    surfaceDim = DeckWash,
    surfaceBright = DeckWhite,
    inverseSurface = PoolDeep,
    inverseOnSurface = PoolMist,
    inversePrimary = SkyLane,
    outline = DeckOutline,
    outlineVariant = DeckSeam,
    scrim = BezelFrame,
)

/**
 * The dark register: the deck before the lights come on.
 *
 * `surface` is the unlit deck and `background` is a step below it, so a panel is a raised thing
 * rather than a hole. `primary` is the sky blue read off the water — the lit lane rope — and
 * `tertiary` is the pace-clock gold at its full chroma, which is the brightest ink in either scheme
 * on purpose: the target time is the one number the app exists to print.
 */
private val DarkColors = darkColorScheme(
    primary = SkyLane,
    onPrimary = SkyLaneInk,
    primaryContainer = PoolMid,
    onPrimaryContainer = PoolMist,
    secondary = RestMint,
    onSecondary = RestDeep,
    secondaryContainer = RestHold,
    onSecondaryContainer = RestPale,
    tertiary = PaceGold,
    onTertiary = PaceInk,
    tertiaryContainer = PaceHold,
    onTertiaryContainer = PacePale,
    error = AlarmBright,
    onError = AlarmBrightInk,
    errorContainer = AlarmHold,
    onErrorContainer = AlarmHoldInk,
    background = NightDeep,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightVariant,
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = NightDeep,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHigh,
    surfaceDim = NightDeep,
    surfaceBright = NightHigh,
    inverseSurface = NightInk,
    inverseOnSurface = NightDeep,
    inversePrimary = PoolDeep,
    outline = PoolSteel,
    outlineVariant = NightHigh,
    scrim = BezelFrame,
)

private val LightLaneSet = LaneSetColors(
    pace = PaceDeep,
    onPace = DeckPaper,
    paceContainer = PacePale,
    onPaceContainer = PaceInk,
    rest = RestTide,
    onRest = RestDeep,
    restOnSurface = RestHold,
    restContainer = RestPale,
    onRestContainer = RestDeep,
    lane = LaneRope,
    onLane = SkyLaneInk,
    laneOnSurface = PoolLift,
    alarm = AlarmWarn,
    onAlarm = AlarmInk,
    heroStart = PoolDeep,
    heroEnd = PoolMid,
    onHero = DeckPaper,
    onHeroMuted = PoolMist,
)

private val DarkLaneSet = LaneSetColors(
    pace = PaceGold,
    onPace = PaceInk,
    paceContainer = PaceHold,
    onPaceContainer = PacePale,
    rest = RestMint,
    onRest = RestDeep,
    restOnSurface = RestMint,
    restContainer = RestHold,
    onRestContainer = RestPale,
    lane = LaneRope,
    onLane = SkyLaneInk,
    laneOnSurface = SkyLane,
    alarm = AlarmBright,
    onAlarm = AlarmBrightInk,
    heroStart = PoolMid,
    heroEnd = PoolDeep,
    onHero = DeckPaper,
    onHeroMuted = PoolMist,
)

/**
 * Where the domain roles live.
 *
 * A `staticCompositionLocalOf` rather than a `compositionLocalOf`, because the value changes only
 * when the scheme changes — once per process, not once per frame — and the static form skips the
 * read-tracking a per-frame local would pay for on every one of these eighteen colours.
 */
private val LocalLaneSetColors = staticCompositionLocalOf { LightLaneSet }

/** The slot's domain roles, in the composition the slot's own components read them from. */
val laneSetColors: LaneSetColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLaneSetColors.current

/**
 * The theme host.
 *
 * [dynamicColor] is `false` by default and the app is built to be read without it: the palette in
 * `store/studio.json` is the identity the store pack was composed from, and a Material You scheme
 * generated from a wallpaper would repaint the app in colours no gate has measured. The preference
 * exists and is honoured when the swimmer turns it on, and the Settings screen says what it does —
 * a control that changed nothing would be decoration. Without it this host is the only source of
 * colour in the app, which is what makes the palette a build input rather than a suggestion.
 */
@Composable
fun PlayKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val platformDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme = when {
        platformDynamic && darkTheme -> dynamicDarkColorScheme(context)
        platformDynamic -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    // Dynamic colour at the platform level brings its own domain roles through `ColorScheme` alone,
    // so the slot's extended roles stay on the declared palette rather than following Material You:
    // the target-time gold is the app's own signal and must not become a wallpaper accent.
    val laneSet = if (darkTheme) DarkLaneSet else LightLaneSet

    CompositionLocalProvider(
        LocalLaneSetColors provides laneSet,
        LocalLaneSetType provides LaneSetNumericType,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LanesetTypography,
            shapes = LanesetShapes,
            content = content,
        )
    }
}
