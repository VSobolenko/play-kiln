package com.gexetr.laneset.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The Laneset palette, one colour at a time.
 *
 * These constants ARE the palette `store/studio.json` declares, and `scripts/core/palette.mjs`
 * compares the two sets by value on every `verify.mjs --slot laneset`: a role that drifts here and
 * not there is reported as the pack and the app disagreeing. The 24 roles the studio draws with are
 * declared at the top of each group; the remaining Material 3 slots (the `on-` pairs, the containers,
 * `inverseSurface`) are declared beside them so the two schemes below resolve to a constant rather
 * than to a literal.
 *
 * Names are the slot's own vocabulary rather than Material's tone ladder, because a name that says
 * `DeckPaper` carries why the colour exists and a name that says `Neutral96` does not. The one rule
 * the names must keep is that a colour is declared once: `PoolDeep` is both the studio's `ink` and the
 * light scheme's `onSurface`, so there is no second copy of `#0A4F60` to drift.
 *
 * Every hex below is either a studio role or a value derived from one; none of them is a Material
 * default and none of them is the purple seed the scaffold shipped.
 */

// ---------------------------------------------------------------------------------------------
// The pool - the field the app is painted on, and the ink that sits on it
// ---------------------------------------------------------------------------------------------

/** studio `ink`: the deep end of the background gradient, and the light scheme's text. */
internal val PoolDeep = Color(0xFF0A4F60)

/** studio `ink_soft`: the middle of the background gradient. */
internal val PoolMid = Color(0xFF0E596B)

/** studio `ink_lift`: the shallow end of the background gradient. */
internal val PoolLift = Color(0xFF1F7488)

/** studio `accent_deep`: the cool glow painted behind the hero when it needs depth. */
internal val PoolGlow = Color(0xFF1E8AA8)

/** The light scheme's `primaryContainer`: pool water thinned to a wash. */
internal val PoolMist = Color(0xFFB7E6F2)

/** The light scheme's `onPrimaryContainer`: the deepest readable pool blue. */
internal val PoolMistInk = Color(0xFF06313C)

/** studio `outline`: the hairline between two surfaces on the dark field. */
internal val PoolSteel = Color(0xFF7C8F97)

// ---------------------------------------------------------------------------------------------
// The lane - the rope, the sky on the water, the colour the dark scheme runs with
// ---------------------------------------------------------------------------------------------

/** studio `primary`: a lane rope lit from above; the dark scheme's accent. */
internal val SkyLane = Color(0xFF63CDE8)

/** studio `onPrimary`: the ink that sits on a lit lane rope. */
internal val SkyLaneInk = Color(0xFF00323E)

/** studio `lane`: the rope itself, one step under the lit one. */
internal val LaneRope = Color(0xFF3FA3C0)

/** studio `muted`: secondary text, and the quiet blue a disabled control takes. */
internal val SkyMuted = Color(0xFF8FA8B2)

// ---------------------------------------------------------------------------------------------
// The deck - the light register: a whiteboard at dawn, and the surface the phone is photographed on
// ---------------------------------------------------------------------------------------------

/** studio `paper`: the whiteboard, the display text, and the light scheme's own background. */
internal val DeckPaper = Color(0xFFF4FBFD)

/** The light scheme's `surface`: the brightest sheet on the deck. */
internal val DeckWhite = Color(0xFFFFFFFF)

/** The light scheme's `surfaceVariant` and `surfaceContainer`: the chalked-in panel. */
internal val DeckWash = Color(0xFFDCE9EE)

/** The light scheme's `surfaceContainerHigh`: one step of chalk above a panel. */
internal val DeckWashHigh = Color(0xFFD4DEE2)

/** The light scheme's `outlineVariant`: the faint seam between two chalked panels. */
internal val DeckSeam = Color(0xFFC8D5DA)

/** The light scheme's `onSurfaceVariant`: the coach's handwriting, not the printed title. */
internal val DeckMuted = Color(0xFF3E5A63)

/** The light scheme's `outline`: the marker line round a control. */
internal val DeckOutline = Color(0xFF6B838C)

// ---------------------------------------------------------------------------------------------
// The night - the pre-dawn register, before the deck lights come on
// ---------------------------------------------------------------------------------------------

/** studio `background`: the dark scheme's own background, and every store mock-up's field. */
internal val NightDeep = Color(0xFF0F151C)

/** studio `surface`: a panel on the unlit deck. */
internal val NightSurface = Color(0xFF1F2831)

/** The dark scheme's `surfaceContainer`: one tonal step above the panel. */
internal val NightRaised = Color(0xFF29323A)

/** The dark scheme's `surfaceContainerHigh`: the highest panel on the dark field. */
internal val NightHigh = Color(0xFF343E47)

/** studio `surfaceVariant`: the recessed trough a set list is written into. */
internal val NightVariant = Color(0xFF2C353D)

/** studio `onSurface`: the pool light reflected off the water; the dark scheme's text. */
internal val NightInk = Color(0xFFC9DBE2)

/** studio `onSurfaceVariant`: the same light, one step quieter. */
internal val NightMuted = Color(0xFFB7C6CC)

// ---------------------------------------------------------------------------------------------
// The rest - the recovery interval, and the mint a lane rope takes between efforts
// ---------------------------------------------------------------------------------------------

/** studio `secondary`: the light scheme's accent - the recovery colour a set is built around. */
internal val RestTide = Color(0xFF4AAB99)

/** The light scheme's `secondaryContainer`: recovery washed onto the whiteboard. */
internal val RestPale = Color(0xFFB2E8D9)

/** studio `rest` and the dark scheme's `secondary`: the mint read off the rope. */
internal val RestMint = Color(0xFF6FCFB4)

/** The light scheme's: ink on a mint wash, and the dark scheme's `onSecondary`. */
internal val RestDeep = Color(0xFF00271E)

/** The dark scheme's `secondaryContainer`: recovery held under the dark field. */
internal val RestHold = Color(0xFF1E5A4C)

// ---------------------------------------------------------------------------------------------
// The pace - the one number the app exists to print
// ---------------------------------------------------------------------------------------------

/** studio `pace`: the target time on the clock. Nothing else in the app is this gold. */
internal val PaceGold = Color(0xFFFFC246)

/** The light scheme's `tertiary`: the same target, dark enough to read on the whiteboard. */
internal val PaceDeep = Color(0xFF8F5200)

/** The light scheme's `tertiaryContainer` and the dark scheme's `onTertiaryContainer`. */
internal val PacePale = Color(0xFFFFE7B8)

/** The dark scheme's `tertiaryContainer`: the target held under the dark field. */
internal val PaceHold = Color(0xFF6E4A00)

/** The instruction on a printed target time: ink on gold, not gold on ink. */
internal val PaceInk = Color(0xFF3D2A00)

// ---------------------------------------------------------------------------------------------
// The dawn - the amber the mark's second hand is drawn in, and the sky it is drawn against
// ---------------------------------------------------------------------------------------------

/** studio `amber`: the pace clock's second hand, and the store graphic's warm note. */
internal val DawnAmber = Color(0xFFF2A25A)

/** studio `amber_deep`: the same hand in shadow, and the graphic's darker warm stop. */
internal val DawnAmberDeep = Color(0xFFBE6418)

/** studio `amber_pale`: dawn on the water; a warm wash that is not the target gold. */
internal val DawnAmberPale = Color(0xFFFBDDBE)

// ---------------------------------------------------------------------------------------------
// The alarm - a set the swimmer cannot hold at the pace they entered
// ---------------------------------------------------------------------------------------------

/** studio `warning`: a set that is not buildable from the constraints as entered. */
internal val AlarmWarn = Color(0xFFE8894A)

/** The light scheme's `error`. */
internal val AlarmDeep = Color(0xFFB3261E)

/** The light scheme's `errorContainer`. */
internal val AlarmPale = Color(0xFFF9DEDC)

/** The light scheme's `onErrorContainer`. */
internal val AlarmInk = Color(0xFF410E0B)

/** The dark scheme's `error`. */
internal val AlarmBright = Color(0xFFFFB4AB)

/** The dark scheme's `onError`. */
internal val AlarmBrightInk = Color(0xFF690005)

/** The dark scheme's `errorContainer`. */
internal val AlarmHold = Color(0xFF93000A)

/** The dark scheme's `onErrorContainer`. */
internal val AlarmHoldInk = Color(0xFFFFDAD6)

// ---------------------------------------------------------------------------------------------
// The frame - what the store pack photographs the phone inside
// ---------------------------------------------------------------------------------------------

/** studio `bezel`: the device frame in every composed store shot. */
internal val BezelFrame = Color(0xFF1A0F22)

/** studio `bezel_rim`: the highlight along the frame. */
internal val BezelRim = Color(0xFF5E6A70)
