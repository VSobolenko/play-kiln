package com.gexetr.unitwalk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * UnitWalk's typography. The app's output is a document — a condition record a landlord signs and
 * sends — so the headings are set in the platform serif and the interface is set in the platform
 * sans. That split is the identity: a serif title reads as printed paperwork, the sans labels read as
 * the controls that produced it, and neither is the Material 3 default that every slot shares.
 *
 * Both families are the platform's own (no font asset is bundled), which is also why the type survives
 * localisation: `FontFamily.Serif` and `FontFamily.SansSerif` resolve to the system's own faces, so a
 * Cyrillic string in `values-ru` is drawn by a face that was designed for it rather than by a Latin
 * face missing half its glyphs.
 *
 * Every style the slot's components ask for is declared explicitly. A style left out of this list
 * falls back to the Material default, and a default that silently sets one screen is exactly how a
 * slot ends up with two typographic voices.
 */

/** The platform faces this slot draws with. Neither is bundled, so both are present in every locale. */
private val Display = FontFamily.Serif
private val Interface = FontFamily.SansSerif

/** The slot's Material 3 type set. `UnitWalkTheme` passes it to `MaterialTheme`. */
val UnitWalkTypography = Typography(
    // The number on a hero: a photo count, a room count, a total.
    displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
    ),
    // A screen title, when the screen has room for one.
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    // A section heading inside a screen.
    headlineSmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    // The title of a hero panel.
    titleLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    // A list row's own title — a unit, a room, a photo.
    titleMedium = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    // A supporting title: the second line under a row's own.
    titleSmall = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    // Reading text, and the body of an empty state.
    bodyLarge = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.3.sp,
    ),
    // The default supporting paragraph.
    bodyMedium = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.25.sp,
    ),
    // A caption under a thumbnail, or a footnote on a report.
    bodySmall = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.3.sp,
    ),
    // A button, and the label of a selected section.
    labelLarge = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    // A status pill, a stat tile's caption, a field name.
    labelMedium = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    ),
    // The tightest label: an overlay caption on a photo, or a badge.
    labelSmall = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.9.sp,
    ),
)

/** The scaffold's name for the same set, kept so a caller written against the scaffold compiles. */
val PlayKilnTypography: Typography = UnitWalkTypography
