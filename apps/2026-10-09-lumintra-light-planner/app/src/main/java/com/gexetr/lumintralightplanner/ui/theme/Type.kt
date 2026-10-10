package com.gexetr.lumintralightplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Two voices, both platform families so nothing is downloaded and every Cyrillic glyph is present:
// a serif for the sky's headlines (an almanac, not a dashboard) and a sans for the working text.
// Times are the app's product, so every clock reading uses tabular figures and never jitters as
// the minutes change.

private val Almanac = FontFamily.Serif
private val Working = FontFamily.SansSerif
private const val TABULAR = "tnum"

val LumintraTypography = Typography(
    displayLarge = TextStyle(fontFamily = Almanac, fontWeight = FontWeight.Normal, fontSize = 54.sp, lineHeight = 60.sp, letterSpacing = (-0.01).em),
    displayMedium = TextStyle(fontFamily = Almanac, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.01).em),
    displaySmall = TextStyle(fontFamily = Almanac, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = Almanac, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Almanac, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Almanac, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Working, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Working, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.01.em),
    titleSmall = TextStyle(fontFamily = Working, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
    bodyLarge = TextStyle(fontFamily = Working, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Working, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Working, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Working, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.02.em),
    labelMedium = TextStyle(fontFamily = Working, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    labelSmall = TextStyle(fontFamily = Working, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.06.em),
)

/** Styles the Material type scale has no slot for. */
object LumintraType {
    /** A large clock reading: the start of the dark window, golden hour. */
    val clockLarge = TextStyle(fontFamily = Working, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 46.sp, fontFeatureSettings = TABULAR)

    /** A stat-tile value: a time, a percentage, an altitude. */
    val clock = TextStyle(fontFamily = Working, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp, fontFeatureSettings = TABULAR)

    /** A time inline with text, in a list row or a chip. */
    val clockSmall = TextStyle(fontFamily = Working, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR)

    /** The spaced capital kicker above a hero panel or a section. */
    val kicker = TextStyle(fontFamily = Working, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.14.em)
}
