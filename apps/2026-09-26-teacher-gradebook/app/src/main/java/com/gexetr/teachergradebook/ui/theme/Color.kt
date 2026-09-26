package com.gexetr.teachergradebook.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette, and the reason it is not the Material default.
 *
 * The first build used a Material green scheme with dynamic colour enabled, which produced the two
 * complaints that matter most for a paid app: on Android 12 and later the whole app was repainted
 * from the device wallpaper, so the brand was whatever the owner's screen happened to be, and on
 * muted wallpapers that landed on a desaturated grey. A grade book that looks like every other app
 * has nothing to sell.
 *
 * So: an explicit indigo-and-amber identity, dynamic colour off, and cool paper-grey surfaces rather
 * than pure white, so cards and tiles read as raised without needing heavy shadows.
 *
 * Indigo carries the product and the actions. Amber is reserved for work that needs attention —
 * missing work, an unmarked assignment — so the accent means something instead of decorating. Teal
 * means present, submitted, done.
 */
internal object Chalk {
    // Indigo — the brand and every primary action.
    val Indigo10 = Color(0xFF1E1B4B)
    val Indigo20 = Color(0xFF312E81)
    val Indigo30 = Color(0xFF3730A3)
    val Indigo40 = Color(0xFF4F46E5)
    val Indigo60 = Color(0xFF6366F1)
    val Indigo80 = Color(0xFFA5B4FC)
    val Indigo90 = Color(0xFFC7D2FE)
    val Indigo95 = Color(0xFFE0E7FF)

    // Teal — present, submitted, on track.
    val Teal10 = Color(0xFF042F2E)
    val Teal20 = Color(0xFF115E59)
    val Teal40 = Color(0xFF0D9488)
    val Teal80 = Color(0xFF5EEAD4)
    val Teal90 = Color(0xFF99F6E4)
    val Teal95 = Color(0xFFCCFBF1)

    // Amber — late, missing, unmarked. The accent that signals work to do.
    val Amber10 = Color(0xFF451A03)
    val Amber30 = Color(0xFF92400E)
    val Amber40 = Color(0xFFD97706)
    val Amber80 = Color(0xFFFCD34D)
    val Amber90 = Color(0xFFFDE68A)
    val Amber95 = Color(0xFFFEF3C7)

    val Rose10 = Color(0xFF450A0A)
    val Rose20 = Color(0xFF7F1D1D)
    val Rose40 = Color(0xFFDC2626)
    val Rose80 = Color(0xFFFCA5A5)
    val Rose90 = Color(0xFFFECACA)
    val Rose95 = Color(0xFFFFE4E6)

    // Neutral, very slightly blue so it sits with the indigo instead of fighting it.
    val Ink10 = Color(0xFF1B1B22)
    val Ink30 = Color(0xFF494D5C)
    val Ink50 = Color(0xFF767A8C)
    val Ink70 = Color(0xFFC3C6D4)
    val Ink85 = Color(0xFFE2E4EC)
    val Ink92 = Color(0xFFF1F2F8)
    val Ink96 = Color(0xFFF7F7FB)
    val Paper = Color(0xFFFFFFFF)

    val Night10 = Color(0xFF101118)
    val Night14 = Color(0xFF14151D)
    val Night20 = Color(0xFF1C1E28)
    val Night28 = Color(0xFF2A2C38)
    val Night40 = Color(0xFF33364A)
    val Night60 = Color(0xFF6B6F80)
    val Night75 = Color(0xFF9CA0B0)
    val Night90 = Color(0xFFE4E5EC)
}
