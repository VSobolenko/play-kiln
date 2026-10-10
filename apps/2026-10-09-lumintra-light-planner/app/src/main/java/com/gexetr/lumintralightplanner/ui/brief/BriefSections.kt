package com.gexetr.lumintralightplanner.ui.brief

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.*
import com.gexetr.lumintralightplanner.ui.components.ChipState
import com.gexetr.lumintralightplanner.ui.components.ConditionChip
import com.gexetr.lumintralightplanner.ui.components.KeyTimeRow as KeyTimeRowCell
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.PhaseDial
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.components.SourceBadge
import com.gexetr.lumintralightplanner.ui.components.ValueRow
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

// =================================================================================================
// 1. The rendering contract of one instant, and the numbers the resource strings interpolate
// =================================================================================================

/**
 * The zone and locale every instant in the brief is rendered with.
 *
 * It is a value rather than a composable read because the brief has an **explicit zone toggle**: the
 * default is the spot's IANA zone and the other is the device's, and "which zone a label came from" has
 * to be a decision the caller makes once rather than something each row re-derives from the
 * environment. Nothing about the *geometry* depends on it — instants are absolute (E63) — only the
 * words are.
 */
@Immutable
data class BriefTimeFormat(
    val zoneId: ZoneId,
    val locale: Locale,
    val use24Hour: Boolean,
) {
    fun time(instant: Instant): String = com.gexetr.lumintralightplanner.ui.format.formatTime(
        instant,
        zoneId,
        locale,
        use24Hour,
    )

    fun duration(duration: Duration): String =
        com.gexetr.lumintralightplanner.ui.format.formatDurationShort(duration, locale)
}

/**
 * The numeric formatters the brief interpolates into strings that print their own unit.
 *
 * They exist as a deliberate, documented pair with `ui/format/Formatters.kt` rather than an oversight:
 * the shared formatters **embed the unit** (`formatDegrees(12.4) == "12.4°"`,
 * `formatPercent(0.62) == "62%"`), while the string resources this screen renders also print it
 * (`brief_sun_altitude` is `"Sun %1$s° at %2$s"`, `brief_moon_value` is `"%1$s%% illuminated, %2$s"`).
 * Passing one into the other would render `"Sun +12.4°°"` and `"62%% illuminated"`. So the brief passes
 * a bare number to those keys and uses the shared formatters everywhere the unit is not already in the
 * string. Reported as a contract gap rather than silently worked around.
 */
internal fun signedNumber(value: Double): String =
    if (!value.isFinite()) "" else String.format(Locale.ROOT, "%+.1f", normalisedZero(value))

internal fun unsignedNumber(value: Double): String =
    if (!value.isFinite()) "" else String.format(Locale.ROOT, "%.1f", normalisedZero(value))

internal fun wholeNumber(value: Double): String =
    if (!value.isFinite()) "" else String.format(Locale.ROOT, "%d", value.toLong())

internal fun percentNumber(fraction: Double): String =
    if (!fraction.isFinite()) "" else wholeNumber(fraction.coerceIn(0.0, 1.0) * 100.0)

/** `-0.04` must not print as `-0.0`: the sign of a rounded-away value is a lie about the value. */
private fun normalisedZero(value: Double): Double {
    val rounded = (value * 10.0).toLong() / 10.0
    return if (rounded == 0.0) 0.0 else rounded
}

/** One half-open interval written as two instants, in the brief's one range format. */
@Composable
internal fun rangeText(time: BriefTimeFormat, span: NightSpan): String =
    stringResource(R.string.brief_range, time.time(span.startUtc), time.time(span.endUtc))

/** "20:41 → 03:12 (6 h 31 min)" — the shape every interval row in the brief uses. */
@Composable
internal fun windowValue(time: BriefTimeFormat, span: NightSpan): String =
    stringResource(
        R.string.brief_window_value,
        time.time(span.startUtc),
        time.time(span.endUtc),
        time.duration(span.duration),
    )

// =================================================================================================
// 2. The render models — facts, never prose
// =================================================================================================

/**
 * The dark-window answer.
 *
 * [deepestTwilight] is the one derived fact the words need: the `NO_ASTRO_DARK` and `NO_DARK`
 * sentences name the *level* the sky reached, and the level is not a field of the report — it is chosen
 * from the Sun's lowest altitude, which is, at the boundary `TwilightKind` itself carries.
 */
@Immutable
data class DarkAnswer(val report: DarkWindowReport) {

    /** The interval the brief states first: the longest, which is the one to plan on. */
    val primary: NightSpan? get() = report.bestWindow

    /** The second window, when the night has two — stated in words, not a rendering accident (E10). */
    val second: NightSpan? get() = report.darkWindows.getOrNull(1)

    val hasTwo: Boolean get() = report.darkWindows.size >= 2

    /**
     * The level the words name for a night without astronomical darkness: nautical when the Sun fell
     * past −12.0°, civil otherwise — the spec's own boundary, read from the domain's constant.
     */
    val deepestTwilight: TwilightKind
        get() = if (report.sunAltMinDeg < TwilightKind.NAUTICAL.angleDeg) {
            TwilightKind.NAUTICAL
        } else {
            TwilightKind.CIVIL
        }

    /** The deepest twilight window actually reached, when one exists. */
    val deepestWindow: NightSpan? get() = report.twilightWindows.firstOrNull()?.span

    /** The midday twilight dip of a polar night, when the night has one. */
    val middayDip: NightSpan? get() = report.middayTwilightDip.firstOrNull()
}

/** The Moon's state, with **both** numbers the brief must always state. */
@Immutable
data class MoonPanel(
    val fraction: Double,
    val phase: MoonPhaseName,
    /** The Moon's altitude at the brief's current instant — apparent, refraction-corrected. */
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val magnitude: Double,
    /** A rise or set inside the session, when the Moon has one. Never invented. */
    val riseUtc: Instant?,
    val setUtc: Instant?,
    val moonless: MoonlessWindow,
    val isWashedOut: Boolean,
    /** The Moon's lowest altitude inside the dark window: the all-night-crescent case (E14). */
    val lowestAltitudeDeg: Double?,
    val lowestInstant: Instant?,
    /** The Moon's highest altitude inside the dark window: the washed-out case (E17). */
    val highestAltitudeDeg: Double?,
    val highestInstant: Instant?,
)

/** The cloud outlook as the brief renders it: the report, its mean, or the typed reason it has none. */
@Immutable
data class CloudRow(
    /**
     * The reading to show: the fresh one, or the cached last reading when the device is offline. It is
     * `null` only when there is nothing to show at all, which is a different sentence.
     */
    val report: CloudReport?,
    val meanPercent: Int?,
    val reason: ConditionUnavailableReason?,
    val state: ConditionState?,
    /** The reading was served from cache with no network, so it is labelled offline with its age. */
    val offline: Boolean,
    /** The instants the response actually covered, for the "no hours cover this window" sentence. */
    val coveredRange: NightSpan?,
)

/** One target row: its window, its fit and — at the brief's current instant — where it is. */
@Immutable
data class BriefTargetRow(
    val id: String,
    val name: String,
    val designation: String,
    val type: ObjectType,
    val magnitude: Double?,
    val magnitudeIsDisputed: Boolean,
    val sizeMajArcmin: Double?,
    val window: TargetWindow,
    val fit: TargetFit?,
    val lens: LensPreset?,
    val nowAltitudeDeg: Double?,
    val nowAzimuthDeg: Double?,
    /** The Moon washes the sky out tonight, so the row carries that condition rather than vanishing. */
    val washedOut: Boolean,
)

/** One satellite pass row. The pass carries its own honesty: its visibility and element-set age. */
@Immutable
data class BriefPassRow(val pass: SatellitePass)

// =================================================================================================
// 3. The dark window — the answer, one sentence per typed outcome
// =================================================================================================

/**
 * The dark-window section: the answer to the question the app exists to answer.
 *
 * It renders **one** of the five typed outcomes as the spec's own sentence, and for every outcome it
 * also states what the sky actually does — the Sun's lowest and highest altitude and their instants, the
 * deepest twilight window, the midday dip — because a non-`DARK` night is not an absence of an answer,
 * it is a different one (E02…E05).
 */
@Composable
fun BriefDarkWindowSection(
    answer: DarkAnswer,
    spotName: String,
    darkWindow: NightSpan?,
    time: BriefTimeFormat,
    modifier: Modifier = Modifier,
) {
    val report = answer.report
    LumintraPanel(modifier = modifier, accent = report.kind == DarkWindowKind.DARK) {
        SectionStrip(title = stringResource(R.string.brief_dark_window_header))

        when (report.kind) {
            DarkWindowKind.DARK -> {
                darkWindow?.let { window ->
                    ValueRow(
                        label = stringResource(R.string.brief_dark_window_header),
                        value = stringResource(
                            R.string.brief_dark_window_value,
                            time.time(window.startUtc),
                            time.time(window.endUtc),
                            stringResource(R.string.brief_dark_kind_dark),
                        ),
                        secondary = time.duration(window.duration),
                    )
                }
                answer.second?.let { second ->
                    Text(
                        text = stringResource(
                            R.string.brief_dark_window_second,
                            time.time(second.startUtc),
                            time.time(second.endUtc),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (answer.hasTwo) {
                    Text(
                        text = stringResource(R.string.brief_dark_two_windows, report.darkWindows.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            DarkWindowKind.POLAR_DAY -> {
                Text(
                    text = stringResource(R.string.brief_no_sunset_tonight, spotName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.brief_no_dark_window_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            DarkWindowKind.POLAR_NIGHT -> {
                Text(
                    text = stringResource(R.string.brief_no_sunrise_today, spotName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                darkWindow?.let { window ->
                    ValueRow(
                        label = stringResource(R.string.brief_dark_window_header),
                        value = rangeText(time, window),
                        secondary = time.duration(window.duration),
                    )
                }
                answer.middayDip?.let { dip ->
                    Text(
                        text = stringResource(R.string.brief_midday_dip, rangeText(time, dip)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            DarkWindowKind.NO_ASTRO_DARK -> {
                Text(
                    text = stringResource(
                        R.string.brief_no_astro_dark,
                        spotName,
                        stringResource(twilightLabelRes(answer.deepestTwilight)),
                        signedNumber(report.sunAltMinDeg),
                        time.time(report.sunAltMinInstant),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                answer.deepestWindow?.let { window ->
                    ValueRow(
                        label = stringResource(twilightLabelRes(answer.deepestTwilight)),
                        value = rangeText(time, window),
                        secondary = time.duration(window.duration),
                    )
                }
                Text(
                    text = stringResource(R.string.brief_no_dark_window_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            DarkWindowKind.NO_DARK -> {
                Text(
                    text = stringResource(
                        R.string.brief_no_dark,
                        spotName,
                        signedNumber(report.sunAltMinDeg),
                        time.time(report.sunAltMinInstant),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                answer.deepestWindow?.let { window ->
                    ValueRow(
                        label = stringResource(R.string.brief_twilight_civil),
                        value = rangeText(time, window),
                        secondary = time.duration(window.duration),
                    )
                }
                Text(
                    text = stringResource(R.string.brief_no_dark_window_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // What the Sun actually does, in every one of the five cases: a non-DARK night states its own
        // numbers rather than leaving the reader with a missing window.
        ValueRow(
            label = stringResource(R.string.brief_sun_lowest_label),
            value = stringResource(
                R.string.brief_sun_altitude,
                signedNumber(report.sunAltMinDeg),
                time.time(report.sunAltMinInstant),
            ),
        )
        ValueRow(
            label = stringResource(R.string.brief_sun_highest_label),
            value = stringResource(
                R.string.brief_sun_altitude,
                signedNumber(report.sunAltMaxDeg),
                time.time(report.sunAltMaxInstant),
            ),
        )
        if (!report.reminderEligible) {
            Text(
                text = stringResource(R.string.brief_reminder_not_offered),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The resource of a twilight band's name, so no band is ever printed by its enum name. */
internal fun twilightLabelRes(kind: TwilightKind): Int = when (kind) {
    TwilightKind.CIVIL -> R.string.brief_twilight_civil
    TwilightKind.NAUTICAL -> R.string.brief_twilight_nautical
    TwilightKind.ASTRONOMICAL -> R.string.brief_twilight_astro
}

/** The resource of a key-time band's name. Golden and blue are the product's own two bands. */
internal fun keyTimeLabelRes(band: KeyTimeBand): Int = when (band) {
    KeyTimeBand.GOLDEN_HOUR -> R.string.brief_twilight_golden
    KeyTimeBand.BLUE_HOUR -> R.string.brief_twilight_blue
    KeyTimeBand.CIVIL -> R.string.brief_twilight_civil
    KeyTimeBand.NAUTICAL -> R.string.brief_twilight_nautical
    KeyTimeBand.ASTRONOMICAL -> R.string.brief_twilight_astro
}

// =================================================================================================
// 4. The key-times table
// =================================================================================================

/**
 * The night's key times, one row per band, plus sunrise and sunset as their own rows.
 *
 * The statements the requirement is explicit about are made in words rather than implied: the three
 * twilights are three distinct tests at the Sun's **centre** (−6.0°, −12.0°, −18.0°), sunrise and sunset
 * are a **different** test at the 90.8333° zenith distance and therefore different instants, and golden
 * and blue hour are the app's own product bands. A band that does not occur is absent, never a
 * zero-width row (E03, E11).
 */
@Composable
fun BriefKeyTimesSection(
    keyTimes: List<KeyTimeRow>,
    sunriseSunset: SunriseSunsetTimes,
    time: BriefTimeFormat,
    modifier: Modifier = Modifier,
) {
    val occurring = keyTimes.filter { it.occurs }
    LumintraPanel(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.brief_twilight_table_header))

        if (occurring.isEmpty() && !sunriseSunset.hasBoth) {
            Text(
                text = stringResource(R.string.brief_key_times_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@LumintraPanel
        }

        occurring.forEach { row ->
            val evening = row.evening
            KeyTimeRowCell(
                bandLabel = stringResource(keyTimeLabelRes(row.band)),
                startLabel = evening?.let { time.time(it.startUtc) },
                endLabel = evening?.let { time.time(it.endUtc) },
                twilight = row.band.isTwilight,
            )
            row.morning?.let { second ->
                KeyTimeRowCell(
                    bandLabel = stringResource(keyTimeLabelRes(row.band)),
                    startLabel = time.time(second.startUtc),
                    endLabel = time.time(second.endUtc),
                    twilight = row.band.isTwilight,
                )
            }
        }

        sunriseSunset.sunsetUtc?.let { instant ->
            KeyTimeRowCell(
                bandLabel = stringResource(R.string.brief_sunset_label),
                startLabel = time.time(instant),
                endLabel = null,
                twilight = false,
            )
        }
        sunriseSunset.sunriseUtc?.let { instant ->
            KeyTimeRowCell(
                bandLabel = stringResource(R.string.brief_sunrise_label),
                startLabel = time.time(instant),
                endLabel = null,
                twilight = false,
            )
        }

        Text(
            text = stringResource(R.string.brief_twilight_table_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = stringResource(R.string.brief_key_time_definition_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// =================================================================================================
// 5. The Moon
// =================================================================================================

/**
 * The Moon panel: the dial, the phase's name, and **always both numbers** — the illuminated fraction
 * and the altitude.
 *
 * The two extremes of the one constant are rendered as the opposite cases they are: an all-night
 * crescent reports its **minimum** altitude inside the window (the instant it is least in the way) and a
 * full Moon reports its **maximum** (the instant it is most in the way), and neither statement is made
 * for the other (E14, E17).
 */
@Composable
fun BriefMoonSection(panel: MoonPanel, time: BriefTimeFormat, modifier: Modifier = Modifier) {
    LumintraPanel(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.brief_moon_header))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PhaseDial(
                fraction = panel.fraction,
                phaseName = stringResource(moonPhaseLabelRes(panel.phase)),
                modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 72.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.brief_moon_value,
                        percentNumber(panel.fraction),
                        stringResource(moonPhaseLabelRes(panel.phase)),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.target_alt_az,
                        signedNumber(panel.altitudeDeg),
                        unsignedNumber(panel.azimuthDeg),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Rise and set, or the typed statement that there is none. Never an invented time (E20).
        when {
            panel.riseUtc != null && panel.setUtc != null -> Text(
                text = stringResource(
                    R.string.brief_moon_rises_sets,
                    time.time(panel.riseUtc),
                    time.time(panel.setUtc),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            panel.riseUtc != null -> Text(
                text = stringResource(R.string.brief_moon_rises, time.time(panel.riseUtc)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            panel.setUtc != null -> Text(
                text = stringResource(R.string.brief_moon_sets, time.time(panel.setUtc)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> Text(
                text = stringResource(
                    if (panel.altitudeDeg > 0.0) {
                        R.string.brief_moon_up_all_night
                    } else {
                        R.string.brief_moon_below_all_night
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(
            color = LumintraTheme.colors.hairline,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        // The washed-out extreme first: it is the one that changes what the user can do tonight, and it
        // describes the sky qualitatively rather than asserting a measured brightness.
        if (panel.isWashedOut) {
            Text(
                text = stringResource(R.string.brief_moon_washed_out),
                style = MaterialTheme.typography.bodyMedium,
                color = LumintraTheme.colors.caution,
            )
            if (panel.highestAltitudeDeg != null && panel.highestInstant != null) {
                Text(
                    text = stringResource(
                        R.string.brief_moon_max_altitude,
                        signedNumber(panel.highestAltitudeDeg),
                        time.time(panel.highestInstant),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.brief_moon_washed_out_no_window),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@LumintraPanel
        }

        when (panel.moonless.kind) {
            MoonlessKind.NO_DARK_WINDOW -> Text(
                text = stringResource(R.string.brief_moonless_no_dark_window),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MoonlessKind.MOON_NEVER_RISES -> {
                Text(
                    text = stringResource(R.string.brief_moon_never_rises),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.brief_moonless_all_night),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                moonlessRows(panel, time)
            }

            MoonlessKind.MOON_NEVER_SETS -> {
                Text(
                    text = stringResource(R.string.brief_moon_never_sets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                moonlessRows(panel, time)
            }

            MoonlessKind.MOONLESS_ALL_NIGHT -> {
                Text(
                    text = stringResource(R.string.brief_moonless_all_night),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                moonlessRows(panel, time)
            }

            MoonlessKind.MOONLESS_INTERVALS -> moonlessRows(panel, time)

            MoonlessKind.MOON_UP_ALL_WINDOW -> {
                Text(
                    text = stringResource(R.string.brief_moon_up_all_night),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (panel.lowestAltitudeDeg != null && panel.lowestInstant != null) {
                    Text(
                        text = stringResource(
                            R.string.brief_moon_min_altitude,
                            signedNumber(panel.lowestAltitudeDeg),
                            time.time(panel.lowestInstant),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.brief_moonless_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The moonless intervals, one row per interval; an empty list is never rendered as a blank row. */
@Composable
private fun moonlessRows(panel: MoonPanel, time: BriefTimeFormat) {
    val intervals = panel.moonless.intervals.filterNot { it.isEmpty }
    if (intervals.isEmpty()) {
        Text(
            text = stringResource(R.string.brief_moonless_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    intervals.forEach { interval ->
        Text(
            text = stringResource(
                R.string.brief_moonless_window,
                time.time(interval.startUtc),
                time.time(interval.endUtc),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = LumintraTheme.colors.moonlight,
        )
    }
}

/** The Moon's eight named phases, as resources in both locales (E19). */
internal fun moonPhaseLabelRes(phase: MoonPhaseName): Int = when (phase) {
    MoonPhaseName.NEW_MOON -> R.string.moon_phase_new_moon
    MoonPhaseName.WAXING_CRESCENT -> R.string.moon_phase_waxing_crescent
    MoonPhaseName.FIRST_QUARTER -> R.string.moon_phase_first_quarter
    MoonPhaseName.WAXING_GIBBOUS -> R.string.moon_phase_waxing_gibbous
    MoonPhaseName.FULL_MOON -> R.string.moon_phase_full_moon
    MoonPhaseName.WANING_GIBBOUS -> R.string.moon_phase_waning_gibbous
    MoonPhaseName.LAST_QUARTER -> R.string.moon_phase_last_quarter
    MoonPhaseName.WANING_CRESCENT -> R.string.moon_phase_waning_crescent
}

// =================================================================================================
// 6. The cloud outlook — a forecast, with its source, its age and its licence
// =================================================================================================

/**
 * The cloud row: the source's total cloud cover, its retrieval time, the word **forecast**, the age of
 * the reading, and the MET Norway attribution beside it.
 *
 * The row's *extent* — the hourly points drawn across the whole session window with the dark window
 * marked inside them — is `TimelineStrip`'s, drawn by `BriefScreen`, so this panel states the numbers
 * the strip draws and never re-lays the geometry.
 *
 * No wording here turns a forecast into a measurement: a value is a forecast from a named source, a
 * cached reading is labelled offline with its age, and an absent value is a typed reason (E48, E49).
 */
@Composable
fun BriefCloudSection(
    row: CloudRow,
    darkWindow: NightSpan?,
    time: BriefTimeFormat,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val report = row.report
    LumintraPanel(modifier = modifier) {
        SectionStrip(
            title = stringResource(R.string.brief_cloud_header),
            trailing = {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_refresh_cw),
                        contentDescription = stringResource(R.string.brief_cloud_refresh),
                    )
                }
            },
        )

        if (report == null) {
            Text(
                text = stringResource(R.string.cloud_no_forecast),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            row.reason?.let { reason ->
                Text(
                    text = unavailableReasonText(reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@LumintraPanel
        }

        SourceBadge(
            text = stringResource(
                R.string.cloud_source_reading,
                time.time(report.age.fetchedAtUtc),
                time.duration(report.age.age),
            ),
            stale = report.age.isStale,
        )
        if (row.offline) {
            Text(
                text = stringResource(R.string.state_offline),
                style = MaterialTheme.typography.bodySmall,
                color = LumintraTheme.colors.caution,
            )
        }

        val mean = row.meanPercent
        if (mean != null) {
            Text(
                text = stringResource(R.string.cloud_mean_over_window, percentNumber(mean / 100.0)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            // A cached reading that no longer covers the requested window: its value and its age, and
            // never interpolated as if it covered the window (E49, E51).
            val last = report.hours.lastOrNull()
            if (last != null) {
                Text(
                    text = stringResource(
                        R.string.cloud_offline_from,
                        time.duration(report.age.age),
                        wholeNumber(last.cloudCoverPercent.toDouble()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val within = darkWindow?.let { report.hoursWithin(it) }.orEmpty()
        if (within.isEmpty()) {
            Text(
                text = stringResource(R.string.cloud_no_hours),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                within.forEach { hour ->
                    Text(
                        text = stringResource(
                            R.string.cloud_hour_label,
                            time.time(hour.hourUtc),
                            wholeNumber(hour.cloudCoverPercent.toDouble()),
                            stringResource(cloudWordRes(hour.cloudCoverPercent)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        row.coveredRange?.let { covered ->
            Text(
                text = stringResource(R.string.brief_cloud_covered_range, rangeText(time, covered)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = stringResource(R.string.cloud_forecast_disclosure),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = stringResource(R.string.cloud_forecast_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.brief_forecast_unmodified),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = { uriHandler.openUri(MET_NORWAY_LICENCE_URL) },
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Text(text = stringResource(R.string.brief_forecast_licence_link))
        }
    }
}

/** The word the row uses for a cloud amount: clear, partly cloudy, overcast. */
internal fun cloudWordRes(percent: Int): Int = when {
    percent <= 20 -> R.string.cloud_clear
    percent <= 70 -> R.string.cloud_partly
    else -> R.string.cloud_overcast
}

/** The licence the attribution links to. A URL is data, not a user-facing sentence (E55). */
internal const val MET_NORWAY_LICENCE_URL: String = "https://www.met.no/en/free-meteorological-data/Licensing-and-crediting"

// =================================================================================================
// 7. Targets
// =================================================================================================

/**
 * The target rows.
 *
 * A target that is up gets its window with the peak altitude and the instant of it; a circumpolar
 * target gets its lowest altitude and when; a target that never rises gets that sentence and no flat
 * curve (E22, E23); a grazing or clipped window is stated rather than dressed as a rise and a set a
 * second apart. No row is a blank panel, and a washed-out sky is a condition on the row rather than a
 * reason to drop it.
 */
@Composable
fun BriefTargetsSection(
    rows: List<BriefTargetRow>,
    time: BriefTimeFormat,
    onOpenTarget: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LumintraPanel(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.brief_targets_header))
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.brief_targets_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@LumintraPanel
        }
        rows.forEach { row ->
            TargetRowBlock(row = row, time = time, onOpenTarget = onOpenTarget)
        }
    }
}

@Composable
private fun TargetRowBlock(row: BriefTargetRow, time: BriefTimeFormat, onOpenTarget: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.target_row_metrics,
                        targetTypeText(row.type),
                        row.magnitude?.let {
                            stringResource(R.string.target_filter_magnitude_value, unsignedNumber(it))
                        } ?: stringResource(R.string.target_magnitude_unknown),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (row.washedOut) {
                ConditionChip(
                    text = stringResource(R.string.brief_target_washed_out),
                    state = ChipState.MARGINAL,
                )
            }
            IconButton(
                onClick = { onOpenTarget(row.id) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_telescope),
                    contentDescription = stringResource(R.string.brief_target_open, row.name),
                )
            }
        }

        when (val window = row.window) {
            is TargetWindow.NeverRises -> Text(
                text = stringResource(R.string.target_never_rises, row.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            is TargetWindow.Circumpolar -> Text(
                text = stringResource(
                    R.string.target_circumpolar,
                    row.name,
                    signedNumber(window.minimumAltitudeDeg),
                    time.time(window.atInstant),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            is TargetWindow.Visible -> {
                val best = window.bestInterval
                if (best == null) {
                    Text(
                        text = stringResource(R.string.target_visible_no),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = if (best.transitUtc == null) {
                            stringResource(
                                R.string.target_interval_no_transit,
                                time.time(best.startUtc),
                                time.time(best.endUtc),
                                signedNumber(best.peakAltitudeDeg),
                            )
                        } else {
                            stringResource(
                                R.string.target_interval_line,
                                time.time(best.startUtc),
                                time.time(best.endUtc),
                                signedNumber(best.peakAltitudeDeg),
                                time.time(best.transitUtc),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (best.isPartial) {
                        Text(
                            text = stringResource(
                                R.string.target_partial_window,
                                row.name,
                                time.time(best.startUtc),
                                time.time(best.endUtc),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (best.isGrazing) {
                        Text(
                            text = stringResource(
                                R.string.target_graze,
                                row.name,
                                signedNumber(best.peakAltitudeDeg),
                                time.duration(best.duration),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = LumintraTheme.colors.caution,
                        )
                    }
                    if (window.intervals.size > 1) {
                        Text(
                            text = stringResource(R.string.target_multiple_intervals, window.intervals.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (row.nowAltitudeDeg != null && row.nowAzimuthDeg != null) {
            Text(
                text = stringResource(
                    R.string.target_alt_az,
                    signedNumber(row.nowAltitudeDeg),
                    unsignedNumber(row.nowAzimuthDeg),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        row.fit?.let { fit -> TargetFitLine(fit = fit, lens = row.lens) }
        HorizontalDivider(
            color = LumintraTheme.colors.hairline,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** The fit statement: three verdicts, and no statement at all for a target with no stated size. */
@Composable
private fun TargetFitLine(fit: TargetFit, lens: LensPreset?) {
    Text(
        text = stringResource(
            when (fit.verdict) {
                FitVerdict.FITS -> R.string.target_fit_fits
                FitVerdict.MARGINAL -> R.string.target_fit_marginal
                FitVerdict.TOO_LARGE -> R.string.target_fit_too_large
                FitVerdict.UNKNOWN_SIZE -> R.string.target_fit_point_source
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = when (fit.verdict) {
            FitVerdict.TOO_LARGE -> LumintraTheme.colors.caution
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    if (fit.objectAngularSizeDeg != null && lens != null) {
        Text(
            text = stringResource(
                R.string.target_fit_numbers,
                unsignedNumber(fit.objectAngularSizeDeg),
                unsignedNumber(fit.frameFovShortSideDeg),
                wholeNumber(lens.focalLengthMm),
                wholeNumber(lens.sensorWidthMm),
                wholeNumber(lens.sensorHeightMm),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    fit.pixelsOnSensor?.let { pixels ->
        Text(
            text = stringResource(R.string.target_fit_pixels, wholeNumber(pixels)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** An object type's name, from resources; the enum's own name is never shown to a user. */
@Composable
internal fun targetTypeText(type: ObjectType): String = stringResource(
    when (type) {
        ObjectType.GALAXY -> R.string.target_type_galaxy
        ObjectType.GALAXY_GROUP -> R.string.target_type_galaxy_group
        ObjectType.GLOBULAR_CLUSTER -> R.string.target_type_globular_cluster
        ObjectType.OPEN_CLUSTER -> R.string.target_type_open_cluster
        ObjectType.EMISSION_NEBULA -> R.string.target_type_emission_nebula
        ObjectType.REFLECTION_NEBULA -> R.string.target_type_reflection_nebula
        ObjectType.PLANETARY_NEBULA -> R.string.target_type_planetary_nebula
        ObjectType.SUPERNOVA_REMNANT -> R.string.target_type_supernova_remnant
        ObjectType.DARK_NEBULA -> R.string.target_type_dark_nebula
        ObjectType.ASTERISM -> R.string.target_type_asterism
        ObjectType.DOUBLE_STAR -> R.string.target_type_double_star
        ObjectType.PLANET -> R.string.target_type_planet
        ObjectType.MOON -> R.string.target_type_moon
        ObjectType.SUN -> R.string.target_type_sun
    },
)

// =================================================================================================
// 8. Satellite passes
// =================================================================================================

/**
 * The pass rows, each a **prediction** with its element set's age, and each stating the pass's own
 * visibility: a pass in the Earth's shadow or under a bright sky occurred and was not visible, which is
 * a different sentence from no pass at all (E36).
 */
@Composable
fun BriefPassesSection(
    rows: List<BriefPassRow>,
    elementSetAge: ElementSetAge?,
    time: BriefTimeFormat,
    modifier: Modifier = Modifier,
) {
    LumintraPanel(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.brief_passes_header))

        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.pass_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row ->
                val pass = row.pass
                Text(
                    text = stringResource(
                        R.string.pass_row,
                        pass.satelliteName,
                        time.time(pass.aosUtc),
                        time.time(pass.losUtc),
                        signedNumber(pass.maxElevationDeg),
                        passNoteText(pass.visibility),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            Text(
                text = stringResource(R.string.pass_prediction_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        elementSetAge?.let { age ->
            Text(
                text = stringResource(R.string.pass_age_days, age.ageDays.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = if (age.showBadge) {
                    LumintraTheme.colors.caution
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** The pass's own state, in the row that states it. */
@Composable
private fun passNoteText(visibility: PassVisibility): String = stringResource(
    when (visibility) {
        PassVisibility.OBSERVABLE -> R.string.brief_pass_visible
        PassVisibility.IN_EARTH_SHADOW -> R.string.pass_shadow_note
        PassVisibility.TOO_BRIGHT -> R.string.pass_bright_note
    },
)

// =================================================================================================
// 9. The ordered conditions, and the notices
// =================================================================================================

/**
 * The brief as one ordered answer: every named condition with its state and the source and age of its
 * value, or the typed reason it has none.
 *
 * The ordering is the domain's (`BriefOrdering.order`), never re-derived here, and an unavailable
 * condition is **rendered with its reason** rather than dropped — a condition that vanished would be
 * indistinguishable from one that was never computed.
 */
@Composable
fun BriefConditionsSection(
    conditions: List<BriefCondition>,
    time: BriefTimeFormat,
    modifier: Modifier = Modifier,
) {
    if (conditions.isEmpty()) return
    LumintraPanel(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.brief_conditions_header))
        conditions.forEach { condition ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(conditionKindRes(condition.kind)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    ConditionChip(
                        text = conditionStateText(condition.state),
                        state = condition.state?.let { chipStateOf(it) } ?: ChipState.UNAVAILABLE,
                    )
                }
                condition.anchorUtc?.let { anchor ->
                    Text(
                        text = time.time(anchor),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                condition.unavailableReason?.let { reason ->
                    Text(
                        text = unavailableReasonText(reason),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                condition.source?.let { source ->
                    SourceBadge(
                        text = source.sourceText(time),
                        stale = false,
                    )
                }
                BriefFactsLine(condition.facts, time)
            }
        }
    }
}

/** A condition's state, as the chip's text. Never colour alone (E89). */
@Composable
private fun conditionStateText(state: ConditionState?): String = stringResource(
    when (state) {
        ConditionState.GOOD -> R.string.brief_condition_state_good
        ConditionState.MARGINAL -> R.string.brief_condition_state_marginal
        ConditionState.BLOCKING -> R.string.brief_condition_state_blocking
        null -> R.string.brief_condition_unavailable
    },
)

/** The source line: the input's name, and its age when it has one. */
@Composable
private fun ConditionSource.sourceText(time: BriefTimeFormat): String {
    val input = stringResource(conditionInputRes(this.input))
    val age = this.age
    return if (age == null) {
        stringResource(R.string.brief_source_plain, input)
    } else {
        stringResource(R.string.brief_source_with_age, input, time.duration(age))
    }
}

/** The facts behind a condition, so the row states a number and not only a verdict. */
@Composable
private fun BriefFactsLine(facts: BriefFacts, time: BriefTimeFormat) {
    when (facts) {
        is BriefFacts.DarkWindow -> Text(
            text = stringResource(
                R.string.brief_condition_fact_dark,
                stringResource(darkKindRes(facts.report.kind)),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is BriefFacts.Moon -> Text(
            text = stringResource(
                R.string.brief_condition_fact_moon,
                percentNumber(facts.state.illuminatedFraction),
                signedNumber(facts.state.altitudeDeg),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is BriefFacts.Cloud -> Text(
            text = facts.meanPercent?.let { mean ->
                stringResource(R.string.brief_condition_fact_cloud, percentNumber(mean / 100.0))
            } ?: stringResource(R.string.cloud_no_forecast),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is BriefFacts.Target -> {
            val best = facts.window.bestIntervalOrNull()
            Text(
                text = if (best == null) {
                    stringResource(R.string.target_visible_no)
                } else if (best.transitUtc == null) {
                    stringResource(
                        R.string.target_interval_no_transit,
                        time.time(best.startUtc),
                        time.time(best.endUtc),
                        signedNumber(best.peakAltitudeDeg),
                    )
                } else {
                    stringResource(
                        R.string.target_interval_line,
                        time.time(best.startUtc),
                        time.time(best.endUtc),
                        signedNumber(best.peakAltitudeDeg),
                        time.time(best.transitUtc),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is BriefFacts.Satellite -> Text(
            text = stringResource(
                R.string.pass_row,
                facts.pass.satelliteName,
                time.time(facts.pass.aosUtc),
                time.time(facts.pass.losUtc),
                signedNumber(facts.pass.maxElevationDeg),
                passNoteText(facts.pass.visibility),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is BriefFacts.Horizon -> Text(
            text = if (facts.occludesChosenTarget) {
                stringResource(R.string.target_occluded_short)
            } else {
                stringResource(R.string.brief_condition_fact_horizon)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The longest interval of a visible window, or `null` for the two non-interval outcomes. */
private fun TargetWindow.bestIntervalOrNull(): TargetInterval? =
    (this as? TargetWindow.Visible)?.bestInterval

internal fun chipStateOf(state: ConditionState): ChipState = when (state) {
    ConditionState.GOOD -> ChipState.GOOD
    ConditionState.MARGINAL -> ChipState.MARGINAL
    ConditionState.BLOCKING -> ChipState.BLOCKING
}

internal fun conditionKindRes(kind: ConditionKind): Int = when (kind) {
    ConditionKind.DARK_WINDOW -> R.string.brief_condition_dark_window
    ConditionKind.MOON -> R.string.brief_condition_moon
    ConditionKind.CLOUD -> R.string.brief_condition_cloud
    ConditionKind.TARGET -> R.string.brief_condition_target
    ConditionKind.SATELLITE -> R.string.brief_condition_satellite
    ConditionKind.HORIZON -> R.string.brief_condition_horizon
}

/** Which input a value came from, in the source badge. */
internal fun conditionInputRes(input: ConditionInput): Int = when (input) {
    ConditionInput.EPHEMERIS -> R.string.brief_source_ephemeris
    ConditionInput.CATALOGUE -> R.string.brief_source_catalogue
    ConditionInput.HORIZON_PROFILE -> R.string.brief_source_horizon
    ConditionInput.FORECAST_MET_NORWAY -> R.string.brief_source_forecast
    ConditionInput.ELEMENT_SET_CELESTRAK -> R.string.brief_source_elements
    ConditionInput.USER_SETTING -> R.string.brief_source_setting
    ConditionInput.USER_ESTIMATE -> R.string.brief_source_user_estimate
}

/** The dark-window kind, in the same five names the answer itself uses. */
internal fun darkKindRes(kind: DarkWindowKind): Int = when (kind) {
    DarkWindowKind.DARK -> R.string.brief_dark_kind_dark
    DarkWindowKind.POLAR_DAY -> R.string.brief_dark_kind_polar_day
    DarkWindowKind.POLAR_NIGHT -> R.string.brief_dark_kind_polar_night
    DarkWindowKind.NO_ASTRO_DARK -> R.string.brief_dark_kind_no_astro
    DarkWindowKind.NO_DARK -> R.string.brief_dark_kind_no_dark
}

/** The typed reason a condition has no value. Always a sentence, never a dash. */
@Composable
internal fun unavailableReasonText(reason: ConditionUnavailableReason): String = stringResource(
    when (reason) {
        ConditionUnavailableReason.OFFLINE -> R.string.brief_reason_offline
        ConditionUnavailableReason.SOURCE_DISABLED -> R.string.brief_reason_source_disabled
        ConditionUnavailableReason.NOT_FETCHED -> R.string.brief_reason_not_fetched
        ConditionUnavailableReason.NO_SPOT -> R.string.brief_reason_no_spot
        ConditionUnavailableReason.NO_NIGHT -> R.string.brief_reason_no_night
        ConditionUnavailableReason.ELEMENTS_EXPIRED -> R.string.brief_reason_elements_expired
        ConditionUnavailableReason.NO_DARK_WINDOW -> R.string.brief_reason_no_dark_window
        ConditionUnavailableReason.CATALOGUE_UNREADABLE -> R.string.brief_reason_catalogue_unreadable
        ConditionUnavailableReason.HORIZON_MISSING -> R.string.brief_reason_horizon_missing
    },
)

/**
 * The notices: the one-line states that are not conditions — a device clock that disagrees with a
 * response, a quarantined spot, a target reference the catalogue no longer holds, a bundle that fell
 * back, a horizon that is flat by default.
 *
 * Each is rendered once, beside the answer rather than instead of it (E97, E98, E99, E108).
 */
@Composable
fun BriefNoticesSection(
    notices: List<BriefNotice>,
    elementSetAge: ElementSetAge?,
    cloudReason: ConditionUnavailableReason?,
    lutFallback: Boolean,
    horizonDrawn: Boolean,
    catalogueUnreadable: Boolean,
    time: BriefTimeFormat,
    modifier: Modifier = Modifier,
) {
    val lines = ArrayList<String>(notices.size + 4)
    notices.forEach { notice ->
        when (notice) {
            is BriefNotice.DeviceClockSkew -> lines += stringResource(
                R.string.brief_clock_warning,
                time.duration(Duration.ofMinutes(kotlin.math.abs(notice.deltaMinutes))),
            )

            is BriefNotice.SavedSpotUnreadable -> lines +=
                stringResource(R.string.brief_saved_spot_unreadable)

            is BriefNotice.SavedTargetMissing -> lines +=
                stringResource(R.string.brief_saved_target_missing)

            BriefNotice.CatalogueUnreadable -> lines +=
                stringResource(R.string.brief_catalogue_unreadable)
        }
    }
    if (!horizonDrawn) lines += stringResource(R.string.brief_horizon_flat_note)
    if (catalogueUnreadable && notices.none { it is BriefNotice.CatalogueUnreadable }) {
        lines += stringResource(R.string.brief_catalogue_unreadable)
    }
    if (lutFallback) lines += stringResource(R.string.night_lut_fallback_note)
    if (cloudReason == ConditionUnavailableReason.OFFLINE) {
        lines += stringResource(R.string.brief_offline_note)
    }
    elementSetAge?.let { age ->
        if (age.showBadge) lines += stringResource(R.string.pass_age_days, age.ageDays.toInt())
    }

    if (lines.isEmpty()) return
    LumintraPanel(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.brief_notices_header))
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}
