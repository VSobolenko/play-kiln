package com.gexetr.lawnroundlog.ui.theme

import androidx.compose.ui.graphics.Color

// The palette of a mowing round, not of a dashboard.
//
// Read from two places and declared once: this file is the app's own theme and
// `store/studio.json` is the pack the studio renders, and the eleven role values below are the
// eleven values there, hex for hex (`.cursor/rules/visual-identity.mdc` - "the palette is one
// source of truth, shared with the app"). A pack whose palette has drifted from the app it
// describes is a screenshot of a different app.
//
// The ground is a cut lawn: a deep turf green field, chalk-off-white ink, and exactly one lime
// reserved for work that has just been done. That is a decision about the user, not a mood - he
// stands at a kerb in direct sunlight with the mower running and gloves on, so the contrast is
// deliberately far above the Material baseline, and the first thing he needs to read at arm's
// length is which rows are already cut.
//
// The Material baseline seed and dynamic (wallpaper) colour are both off, on purpose: the same
// round has to look the same on every phone the operator picks up.

/** studio role `ink` - the deepest turf; the field's dark end. */
internal val TurfDeep = Color(0xFF12301C)

/** studio role `ink_soft` - the mid turf. */
internal val TurfMid = Color(0xFF1B4126)

/** studio role `ink_lift` - the lifted turf; the field's light end. */
internal val TurfLift = Color(0xFF275B33)

/** studio role `amber` - the cut accent: the colour of a stripe that has just been mown. */
internal val CutLime = Color(0xFFC9F04A)

/** studio role `amber_deep` - the accent's darker end, for glows and for light-theme ink. */
internal val CutLimeDeep = Color(0xFF6E9A22)

/** studio role `amber_pale` - the accent's palest wash. */
internal val CutLimePale = Color(0xFFEAF9C6)

/** studio role `accent_deep` - a cool chalk-slate; the second hue that gives the field depth. */
internal val ChalkSlate = Color(0xFF1E6E6A)

/** studio role `paper` - chalk dust: the mark drawn on the field, and the display text on it. */
internal val Chalk = Color(0xFFF6F4E4)

/** studio role `muted` - secondary text on the turf field. */
internal val ChalkMuted = Color(0xFFBBD0B2)

/** studio role `bezel` - the device mock-up's frame. */
internal val FrameInk = Color(0xFF0A1A10)

/** studio role `bezel_rim` - the frame's rim highlight. */
internal val FrameRim = Color(0xFF7C9A82)

// Supporting tones. Derived from the same ground so the light and dark schemes read as one product
// rather than two, and declared here rather than left to `lightColorScheme`'s defaults - those
// defaults are the Material baseline, and a slot that leaves them in place ships the purple seed it
// never chose.

/** A pale turf wash: containers on the light scheme. */
internal val TurfWash = Color(0xFFDCEBD3)

/** The light scheme's dim surface - chalk one step down. */
internal val ChalkDim = Color(0xFFE7E8D6)

/** The light scheme's outline - a hedge line, not a hairline of pure grey. */
internal val ChalkLine = Color(0xFF6F8B70)

/** A pale chalk-slate wash: the secondary container on the light scheme. */
internal val SlateWash = Color(0xFFCFE7E2)

/** Deep chalk-slate: ink on [SlateWash], and the dark scheme's secondary container. */
internal val SlateDeep = Color(0xFF0B3A38)

/** A light chalk-slate for the dark scheme's secondary. */
internal val SlateLight = Color(0xFF7FD3CB)

/** Deep olive-lime: the light scheme's tertiary ink, legible on chalk. */
internal val OliveDeep = Color(0xFF3F5E0C)

/** The dark scheme's surface - the field one step up from the background. */
internal val TurfSurface = Color(0xFF16381F)

/** The dark scheme's raised surface and variant. */
internal val TurfSurfaceLift = Color(0xFF1F4B29)

/** The dark scheme's outline. */
internal val TurfLine = Color(0xFF4E7355)

/** Chalk-green ink for the dark scheme. */
internal val Mist = Color(0xFFDBECCE)

/** A pale clay wash: the light scheme's error container. */
internal val ClayMist = Color(0xFFF6D9D1)

/** Deep ember: ink on [ClayMist] and the dark scheme's error container. */
internal val EmberInk = Color(0xFF3E0B03)

/** The light scheme's error - a rust red that belongs to a field, not a warning banner. */
internal val Copper = Color(0xFFA03322)

/** The dark scheme's error. */
internal val CopperLight = Color(0xFFFFB4A4)
