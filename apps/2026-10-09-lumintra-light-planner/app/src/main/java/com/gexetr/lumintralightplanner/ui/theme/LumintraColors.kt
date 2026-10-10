package com.gexetr.lumintralightplanner.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The roles Material 3's ColorScheme has no name for, each tied to a decision the app makes again
 * and again. Read through `LumintraTheme.colors`; red mode swaps the whole set for
 * [NightVisionColors].
 */
@Immutable
data class LumintraColors(
    /** Top of the sky field behind hero panels and the Night scene chrome. */
    val skyZenith: Color,
    /** Bottom of the sky field, where it meets the horizon. */
    val skyHorizon: Color,
    /** Text and glyphs drawn on the sky field. */
    val onSky: Color,
    /** Secondary text on the sky field. */
    val onSkyMuted: Color,
    /** The cool glow that gives the sky field depth. */
    val skyGlow: Color,
    /** The light being planned around: the Sun or Moon on its arc, golden hour, the call to action. */
    val firstLight: Color,
    /** Ink on a [firstLight] fill. */
    val onFirstLight: Color,
    /** Blue hour. */
    val blueHour: Color,
    /** The Moon's light, and the "moonlit" state of a night. */
    val moonlight: Color,
    /** The dark window is open: astronomical darkness with the Moon down. */
    val darkWindow: Color,
    /** Cloud cover and anything forecast-based. */
    val cloud: Color,
    /** A forecast that is stale, a target that will not fit, a pass that is gone. */
    val caution: Color,
    /** Hairlines, dial ticks and dividers drawn on the sky field. */
    val hairline: Color,
    /** The mark's arc, horizon and star. */
    val markInk: Color,
    /** The mark's body. */
    val markBody: Color,
    /** The disc of a lit Moon in the phase dial. */
    val moonLit: Color,
    /** The unlit part of the Moon in the phase dial. */
    val moonShadow: Color,
)

internal val LumintraDarkColors = LumintraColors(
    skyZenith = SkyZenith,
    skyHorizon = SkyHorizon,
    onSky = Starlight,
    onSkyMuted = StarlightMuted,
    skyGlow = MoonDeep,
    firstLight = Ember,
    onFirstLight = EmberInk,
    blueHour = BlueHour,
    moonlight = Moonlight,
    darkWindow = Aurora,
    cloud = CloudVeil,
    caution = AlertNight,
    hairline = NightOutlineVariant,
    markInk = Starlight,
    markBody = Ember,
    moonLit = Starlight,
    moonShadow = NightInkLift,
)

/** Day chrome keeps the night sky on its hero panels; only the roles off the sky change. */
internal val LumintraLightColors = LumintraDarkColors.copy(
    caution = AlertDay,
)
