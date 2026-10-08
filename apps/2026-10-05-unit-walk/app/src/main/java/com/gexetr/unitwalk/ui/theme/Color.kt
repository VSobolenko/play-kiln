package com.gexetr.unitwalk.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * UnitWalk's colour system, declared once, in one file, for one reason: it is measured.
 *
 * `scripts/core/palette.mjs` reads this file and `store/studio.json` together, and two of its legs
 * decide whether the app looks like the app the owner approved:
 *
 *   - `palette.agreement` requires every role the identity declares to be carried by a constant
 *     here. The eleven `brand*` constants below are those roles, hex for hex — the palette in
 *     `studio.json` is what `scripts/store-studio.py` renders the launcher mark, the feature graphic
 *     and the store compositions from, so a colour edited here alone is a store pack advertising a
 *     different app.
 *   - `palette.contrast` measures every `on-X` / `X` pair below in **both** schemes at WCAG AA
 *     (4.5:1), and `palette.field-chroma` bounds every field and surface role at HCT chroma 12.
 *     Those are the numbers, and they are why a value here is not a matter of taste.
 *
 * There is no seed and no `dynamicColorScheme` in this slot. Material You would derive a scheme from
 * the phone's wallpaper, which is a colour system nobody chose and which no store screenshot could
 * have been rendered from — so `dynamicColor` reaches `UnitWalkTheme` and is deliberately not
 * applied (see `Theme.kt`).
 *
 * The two families, and what each answers:
 *
 *   - `brand*` — the identity. Slate fields (`ink`, `ink_soft`, `ink_lift`), paper (`paper`), and
 *     amber reserved for one thing: a walk that has been completed and signed. The warm near-black
 *     `bezel` and its rim are the device frame the pack draws a screenshot inside.
 *   - `*Light` / `*Dark` — the Material 3 scheme the app paints with, derived against those fields
 *     and kept neutral (chroma <= 12) so the amber accent is the only saturated thing on screen.
 */

// ---------------------------------------------------------------------------------------------
// The identity — the eleven roles `store/studio.json` declares (palette.agreement compares these)
// ---------------------------------------------------------------------------------------------

/** The field the store pack is composed on and the darkest step of the launcher gradient. */
internal val brandInk = Color(0xFF0E1A24)

/** One step up from the field: the panel a hero or a comparison sits on. */
internal val brandInkSoft = Color(0xFF16283A)

/** The lit end of the launcher gradient; also the app's dark `primaryContainer` family. */
internal val brandInkLift = Color(0xFF24425E)

/** The accent. It means exactly one thing: a walk that is done and signed. */
internal val brandAmber = Color(0xFFE8A93C)

/** The accent's shadowed step, for amber text or an amber rule on paper. */
internal val brandAmberDeep = Color(0xFFC87A1E)

/** The accent at a tint strength — a container behind amber ink, never ink itself. */
internal val brandAmberPale = Color(0xFFF2D3A0)

/** The second family: the teal a room's condition is recorded in. */
internal val brandAccentDeep = Color(0xFF2E6E80)

/** Paper. The light field, the app's light `background`, and the launcher mark's own stroke. */
internal val brandPaper = Color(0xFFF7F9FB)

/** Secondary ink: a label that supports a value rather than being it. */
internal val brandMuted = Color(0xFFA7B4C2)

/** The device frame the pack draws a screenshot inside, and its rim. */
internal val brandBezel = Color(0xFF14100C)

/** The rim highlight of that frame. */
internal val brandBezelRim = Color(0xFF8D8174)

// ---------------------------------------------------------------------------------------------
// The light scheme
// ---------------------------------------------------------------------------------------------

/** Light `primary`: the slate the app's bars, buttons and links are cut from. */
internal val primaryLight = Color(0xFF1B3F60)

/** Light `onPrimary`: paper on slate, 10.33:1 (measured by `scripts/core/palette.mjs`'s contrast leg). */
internal val onPrimaryLight = Color(0xFFF7F9FB)

/** Light `primaryContainer`: a slate so pale it reads as paper with a temperature. */
internal val primaryContainerLight = Color(0xFFD9E5F2)

/** Light `onPrimaryContainer`: ink, for a label inside the pale slate. */
internal val onPrimaryContainerLight = Color(0xFF071A2B)

/** Light `secondary`: the teal, one step warmer than the slate so the two are tellable apart. */
internal val secondaryLight = Color(0xFF1F5A66)

/** Light `onSecondary`. */
internal val onSecondaryLight = Color(0xFFF7F9FB)

/** Light `secondaryContainer`: the wash a recorded condition sits on. */
internal val secondaryContainerLight = Color(0xFFCDE6EA)

/** Light `onSecondaryContainer`. */
internal val onSecondaryContainerLight = Color(0xFF04262B)

/** Light `tertiary`: the amber, dark enough to be read as text on paper (5.6:1). */
internal val tertiaryLight = Color(0xFF8A5A12)

/** Light `onTertiary`. */
internal val onTertiaryLight = Color(0xFFF7F9FB)

/** Light `tertiaryContainer`: the amber wash behind a completed-walk label. */
internal val tertiaryContainerLight = Color(0xFFF6E1B7)

/** Light `onTertiaryContainer`. */
internal val onTertiaryContainerLight = Color(0xFF2E1D04)

/** Light `error`: a defect that stops a unit being let. */
internal val errorLight = Color(0xFFA3221C)

/** Light `onError`. */
internal val onErrorLight = Color(0xFFFFFFFF)

/** Light `errorContainer`. */
internal val errorContainerLight = Color(0xFFF9DAD6)

/** Light `onErrorContainer`. */
internal val onErrorContainerLight = Color(0xFF3F0300)

/** Light `background` — the same colour as the palette's `paper`, and as the window background XML. */
internal val backgroundLight = Color(0xFFF7F9FB)

/** Light `onBackground`. */
internal val onBackgroundLight = Color(0xFF121A21)

/** Light `surface`. */
internal val surfaceLight = Color(0xFFF7F9FB)

/** Light `onSurface`. */
internal val onSurfaceLight = Color(0xFF121A21)

/** Light `surfaceVariant`: a cool grey a list row can sit on without an elevation. */
internal val surfaceVariantLight = Color(0xFFE1E5EA)

/** Light `onSurfaceVariant`: the colour of a supporting label. */
internal val onSurfaceVariantLight = Color(0xFF45515C)

/** Light `outline`. */
internal val outlineLight = Color(0xFF6E7A86)

/** Light `inverseSurface`, for a snackbar that has to read on paper. */
internal val inverseSurfaceLight = Color(0xFF2A333C)

/** Light `inverseOnSurface`. */
internal val inverseOnSurfaceLight = Color(0xFFEFF2F5)

// ---------------------------------------------------------------------------------------------
// The dark scheme
// ---------------------------------------------------------------------------------------------

/** Dark `primary`: the slate lifted until it glows against the field. */
internal val primaryDark = Color(0xFFA8C8E8)

/** Dark `onPrimary`: ink on the lifted slate. */
internal val onPrimaryDark = Color(0xFF0B2C46)

/** Dark `primaryContainer`: a slate panel on the dark field. */
internal val primaryContainerDark = Color(0xFF1E4260)

/** Dark `onPrimaryContainer`. */
internal val onPrimaryContainerDark = Color(0xFFD9E5F2)

/** Dark `secondary`: the teal lifted. */
internal val secondaryDark = Color(0xFF9CCFD8)

/** Dark `onSecondary`. */
internal val onSecondaryDark = Color(0xFF0A3038)

/** Dark `secondaryContainer`. */
internal val secondaryContainerDark = Color(0xFF16464F)

/** Dark `onSecondaryContainer`. */
internal val onSecondaryContainerDark = Color(0xFFCDE6EA)

/** Dark `tertiary`: the amber lifted — the one warm thing in the dark scheme. */
internal val tertiaryDark = Color(0xFFEFC178)

/** Dark `onTertiary`. */
internal val onTertiaryDark = Color(0xFF4E3406)

/** Dark `tertiaryContainer`: a deep amber, so the container reads as the same family as the accent. */
internal val tertiaryContainerDark = Color(0xFF4E3406)

/** Dark `onTertiaryContainer`. */
internal val onTertiaryContainerDark = Color(0xFFF6E1B7)

/** Dark `error`. */
internal val errorDark = Color(0xFFFFB4AB)

/** Dark `onError`. */
internal val onErrorDark = Color(0xFF690005)

/** Dark `errorContainer`. */
internal val errorContainerDark = Color(0xFF93000A)

/** Dark `onErrorContainer`. */
internal val onErrorContainerDark = Color(0xFFFFDAD6)

/** Dark `background`: the ink field of the launcher gradient. */
internal val backgroundDark = Color(0xFF111A23)

/** Dark `onBackground`. */
internal val onBackgroundDark = Color(0xFFDCE2E8)

/** Dark `surface`. */
internal val surfaceDark = Color(0xFF141E26)

/** Dark `onSurface`. */
internal val onSurfaceDark = Color(0xFFDCE2E8)

/** Dark `surfaceVariant`. */
internal val surfaceVariantDark = Color(0xFF2A343C)

/** Dark `onSurfaceVariant`. */
internal val onSurfaceVariantDark = Color(0xFFB8C3CD)

/** Dark `outline`. */
internal val outlineDark = Color(0xFF84919C)

/** Dark `inverseSurface`. */
internal val inverseSurfaceDark = Color(0xFFDCE2E8)

/** Dark `inverseOnSurface`. */
internal val inverseOnSurfaceDark = Color(0xFF2A333C)

// ---------------------------------------------------------------------------------------------
// The surface family — depth without a single shadow. Every role below is a field role, so each is
// bounded at HCT chroma 12 by `palette.field-chroma`; the tonal ladder is what gives a panel an
// edge instead of a drop shadow.
// ---------------------------------------------------------------------------------------------

/** Light `surfaceDim`: a recessed list background. */
internal val surfaceDimLight = Color(0xFFDCE3EA)

/** Light `surfaceBright`: the top of the ladder, for the surface nearest the reader. */
internal val surfaceBrightLight = Color(0xFFFFFFFF)

/** Light `surfaceContainerLowest`. */
internal val surfaceContainerLowestLight = Color(0xFFFFFFFF)

/** Light `surfaceContainerLow`. */
internal val surfaceContainerLowLight = Color(0xFFF3F7FA)

/** Light `surfaceContainer`: the default panel. */
internal val surfaceContainerLight = Color(0xFFEDF2F7)

/** Light `surfaceContainerHigh`: a panel raised above another panel. */
internal val surfaceContainerHighLight = Color(0xFFE7EDF3)

/** Light `surfaceContainerHighest`: the top card of a stack. */
internal val surfaceContainerHighestLight = Color(0xFFE1E8EF)

/** Dark `surfaceDim`: darker than the field, for a recessed area. */
internal val surfaceDimDark = Color(0xFF0E161E)

/** Dark `surfaceBright`: the lightest dark surface, for the card nearest the reader. */
internal val surfaceBrightDark = Color(0xFF242E36)

/** Dark `surfaceContainerLowest`. */
internal val surfaceContainerLowestDark = Color(0xFF0C141C)

/** Dark `surfaceContainerLow`. */
internal val surfaceContainerLowDark = Color(0xFF121C23)

/** Dark `surfaceContainer`: the default dark panel. */
internal val surfaceContainerDark = Color(0xFF172129)

/** Dark `surfaceContainerHigh`. */
internal val surfaceContainerHighDark = Color(0xFF1C252D)

/** Dark `surfaceContainerHighest`. */
internal val surfaceContainerHighestDark = Color(0xFF212A32)
