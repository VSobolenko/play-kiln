package com.gexetr.laneset.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Laneset's typography.
 *
 * A swim set is a table the swimmer reads off a whiteboard at arm's length with wet hands, and the
 * one number that matters is the target time on every interval. That is a data-typography problem,
 * not a display-typography one, so the numeric treatment is the part of this file that was actually
 * designed:
 *
 * - **Tabular figures everywhere a number can change.** `fontFeatureSettings = "tnum"` is the only
 *   knob that matters here. A proportional `1` is narrower than a `9`, so a column of target times
 *   (`1:45`, `2:00`, `1:15`) shimmers as the session is recomputed and a set list stops reading as
 *   a table. Tampering with `tnum` (`FontFeature` or the `"tnum"` string both work; the string is
 *   used because it keeps every style in this file a plain `TextStyle` literal) is what makes
 *   `0:59` and `1:00` the same width.
 * - **A hero target that is unmistakably the datum.** [LaneSetTypography.intervalTarget] is set
 *   larger than any M3 role this app uses and with no letter-spacing, so the colon of `1:45` sits
 *   tight and the digits carry the weight. It is the only style in either file at 44sp.
 * - **Rows that line up.** [LaneSetTypography.metricsRow] and [LaneSetTypography.paceSplit] are
 *   tuned so a rep table's columns align across rows without a `Row` of fixed widths, which is how
 *   a bad number gets read as a good one.
 *
 * The M3 scale is declared in full rather than partially, so a component that reads
 * `MaterialTheme.typography.titleMedium` gets this slot's type rather than the Material baseline's.
 * `FontFamily.SansSerif` is Roboto on Android, which is the family the platform ships and the one
 * whose `tnum` feature is guaranteed present.
 */

/** The style-shaping feature that makes every digit the same width, in Roboto and in every fallback. */
private const val TABULAR_FIGURES = "tnum"

/** Roboto, named once rather than in fifteen places, so the family is one decision. */
private val Sans = FontFamily.SansSerif

/** The M3 scale, sized for a phone read on a pool deck. */
internal val LanesetTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    displayMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    headlineLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    ),
)

/**
 * The numeric styles — the reason this file exists rather than the M3 defaults.
 *
 * Every one of them carries [TABULAR_FIGURES], so a number that changes does not change the shape
 * of the line it sits on. They are read through [MaterialTheme.laneSetTypography], one accessor for
 * the same reason the domain colours have one: a component asks for `intervalTarget` and gets the
 * slot's decision about how a target time is set, not fifteen separate call sites guessing.
 */
@Immutable
data class LaneSetTypography(
    /** The target time on an interval — the largest number in the app, `1:45`. */
    val intervalTarget: TextStyle,
    /** The rep line under a target: `8 × 50 free on 1:45`. */
    val intervalHeadline: TextStyle,
    /** A per-100 pace derived from the two trials: `1:38 /100m`. */
    val paceSplit: TextStyle,
    /** One row of a rep table: reps, distance, stroke, target. Fixed figures, so columns align. */
    val metricsRow: TextStyle,
    /** A section number or a count on a chip: `SET 3`, `6 × 100`. */
    val countChip: TextStyle,
)

/** The declared numeric styles. Values live in [LanesetTypography]'s slots where they are the same. */
internal val LaneSetNumericType = LaneSetTypography(
    intervalTarget = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    intervalHeadline = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    paceSplit = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    metricsRow = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    countChip = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
)

/**
 * The provider `PlayKilnTheme` wraps its content in. `internal` rather than private because the
 * theme host is in another file and the alternative — a helper that returns the local — would erase
 * its type and accept a value of the wrong shape at compile time.
 */
internal val LocalLaneSetType = staticCompositionLocalOf { LaneSetNumericType }

/** The slot's numeric type styles, read the way the domain colours are: through `MaterialTheme`. */
val MaterialTheme.laneSetTypography: LaneSetTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalLaneSetType.current
