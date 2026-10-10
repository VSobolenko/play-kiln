package com.gexetr.lumintralightplanner.ui.targets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.sky.AltitudeModel
import com.gexetr.lumintralightplanner.data.sky.SkyEphemeris
import com.gexetr.lumintralightplanner.domain.CrossingSearch
import com.gexetr.lumintralightplanner.domain.DarkWindowClassifier
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.LensPreset
import com.gexetr.lumintralightplanner.domain.RowVerification
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.SunMeasurements
import com.gexetr.lumintralightplanner.domain.TargetFit
import com.gexetr.lumintralightplanner.domain.TargetInterval
import com.gexetr.lumintralightplanner.domain.TargetWindow
import com.gexetr.lumintralightplanner.domain.TwilightKind
import com.gexetr.lumintralightplanner.domain.FitVerdict
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.components.ValueRow
import com.gexetr.lumintralightplanner.ui.format.formatTime
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * The window a target has on the chosen night, typed.
 *
 * The three shapes that are **not** a rise-to-set interval are separate cases with a sentence of their
 * own, because an empty interval list rendered as a blank panel is exactly the defect
 * `TargetWindow` exists to make unrepresentable. [NoDarkNight] is the fourth: a night with no
 * astronomical darkness has no dark window to clip against, so the surface says that rather than
 * inventing a window out of the session bounds.
 */
sealed interface TargetWindowFacts {
    /** No saved spot is active, so no rise/transit/set exists to report. */
    data object NoSpot : TargetWindowFacts

    /** No night is chosen, so nothing is computed for the instant. */
    data object NoNight : TargetWindowFacts

    /** A night without astronomical darkness: no window is claimed. */
    data class NoDarkNight(val kind: DarkWindowKind) : TargetWindowFacts

    /** One or more ordered intervals above the effective horizon, already clipped to the dark window. */
    data class Visible(val intervals: List<TargetInterval>, val tonight: Boolean) : TargetWindowFacts

    /** Above the effective horizon for the whole session: no rise and no set. */
    data class Circumpolar(
        val minimumAltitudeDeg: Double,
        val atInstant: Instant,
        val effectiveHorizonDeg: Double,
    ) : TargetWindowFacts

    /** Never reaches the effective horizon from this spot at this latitude. */
    data class NeverRises(val effectiveHorizonDeg: Double, val maxAltitudeDeg: Double) : TargetWindowFacts
}

/**
 * The fit statement's numbers, with the lens they were computed from.
 *
 * A magnitude never produces a fit statement and a target with no stated size produces none either —
 * that case arrives as `FitVerdict.UNKNOWN_SIZE` and is rendered as the stated absence.
 */
@Immutable
data class TargetFitFacts(val fit: TargetFit, val lens: LensPreset)

/** The target's position at the reference instant, refraction-corrected. */
@Immutable
data class TargetPositionFacts(val altitudeDeg: Double, val azimuthDeg: Double)

/**
 * Everything the detail surface renders, computed once when a row is opened.
 *
 * The catalogue facts travel as a [TargetRowModel] so the row and the detail cannot state a different
 * magnitude, size or type; the per-night half travels as typed facts so "no spot", "no night" and
 * "no dark window" are states the renderer must handle rather than null dereferences during layout.
 */
@Immutable
data class TargetDetailUiState(
    val model: TargetRowModel,
    val designations: List<String>,
    val aliases: List<String>,
    val verification: RowVerification?,
    val window: TargetWindowFacts,
    val fit: TargetFitFacts?,
    val position: TargetPositionFacts?,
    /** The zone the night's instants are rendered in; empty when nothing was computed. */
    val zoneId: String,
    val locale: Locale,
) {
    /** The catalogue id, which is the only field the list needs from this state. */
    val targetId: String get() = model.id
}

/**
 * Builds the detail state for one catalogue row.
 *
 * The dark window is computed here from the same domain pieces the brief uses
 * (`CrossingSearch` → `SunMeasurements` → `DarkWindowClassifier`), so the window this surface states
 * is the brief's window and not a second answer. The fit statement compares the object's **major
 * axis** against the frame's **shorter side** through `TargetFit.of`, which also owns the 0.8 / 1.0
 * verdict split — nothing here re-derives it. Focal length and sensor extents stay in millimetres.
 */
fun buildTargetDetailState(
    obj: SkyObject,
    favourite: Boolean,
    spot: Scout?,
    horizon: HorizonProfile?,
    ephemeris: SkyEphemeris,
    nowUtc: Instant,
    locale: Locale,
): TargetDetailUiState {
    val model = TargetRowModel(
        id = obj.id,
        designation = obj.designation,
        name = obj.name,
        nameRu = obj.nameRu,
        type = obj.type,
        magnitude = obj.displayMagnitude,
        magnitudeSystem = obj.magSystem,
        magnitudeKind = obj.magKind,
        magnitudeDisputed = obj.magDisputed,
        sizeMajArcmin = obj.sizeMajArcmin,
        sizeMinArcmin = obj.sizeMinArcmin,
        favourite = favourite,
        visibleTonight = null,
    )
    val designations = buildList {
        add(obj.id)
        obj.messier?.let { add("M$it") }
        obj.ngc?.let { add("NGC $it") }
        obj.ic?.let { add("IC $it") }
    }.distinct()
    val aliases = (obj.aliases + obj.commonNames).distinct()

    if (spot == null) {
        return TargetDetailUiState(
            model = model,
            designations = designations,
            aliases = aliases,
            verification = obj.verification,
            window = TargetWindowFacts.NoSpot,
            fit = null,
            position = null,
            zoneId = "",
            locale = locale,
        )
    }

    val zone = runCatching { ZoneId.of(spot.tzId) }.getOrElse { ZoneId.systemDefault() }
    val point = spot.point
    val nightDate = SessionClock.nightOf(nowUtc, zone)
    val session = SessionClock.sessionWindow(nightDate, zone, spot.id)

    val sunAlt: (Instant) -> Double =
        { instant -> ephemeris.sunHorizontal(point, instant, AltitudeModel.GEOMETRIC).altitudeDeg }
    val belowThreshold = TwilightKind.entries.associateWith { kind ->
        CrossingSearch.intervalsAtOrBelow(session, kind.angleDeg, f = sunAlt)
    }
    val sunMin = CrossingSearch.extremum(session, findMax = false, f = sunAlt)
    val sunMax = CrossingSearch.extremum(session, findMax = true, f = sunAlt)
    val sunriseSunset = ephemeris.sunriseSunset(point, session)
    val report = DarkWindowClassifier.classify(
        SunMeasurements(
            session = session,
            sunsetUtc = sunriseSunset.sunsetUtc,
            sunriseUtc = sunriseSunset.sunriseUtc,
            sunAltMinDeg = sunMin.value,
            sunAltMinInstant = sunMin.instantUtc,
            sunAltMaxDeg = sunMax.value,
            sunAltMaxInstant = sunMax.instantUtc,
            belowThreshold = belowThreshold,
        ),
    )

    val lens = spot.equipment.toLensPreset()
    val position = runCatching {
        ephemeris.fixedHorizontal(point, obj.equatorial, nowUtc, AltitudeModel.REFRACTED)
    }.getOrNull()

    val windowFacts: TargetWindowFacts = if (!report.hasDarkWindow) {
        TargetWindowFacts.NoDarkNight(report.kind)
    } else {
        when (val window = TargetWindows.classify(obj, point, horizon, session, report.darkWindows, ephemeris)) {
            is TargetWindow.Visible -> TargetWindowFacts.Visible(window.intervals, window.isVisibleTonight)
            is TargetWindow.Circumpolar -> TargetWindowFacts.Circumpolar(
                minimumAltitudeDeg = window.minimumAltitudeDeg,
                atInstant = window.atInstant,
                effectiveHorizonDeg = window.effectiveHorizonDeg,
            )

            is TargetWindow.NeverRises -> TargetWindowFacts.NeverRises(
                effectiveHorizonDeg = window.effectiveHorizonDeg,
                maxAltitudeDeg = window.maxAltitudeDeg,
            )
        }
    }

    return TargetDetailUiState(
        model = model,
        designations = designations,
        aliases = aliases,
        verification = obj.verification,
        window = windowFacts,
        fit = TargetFitFacts(TargetFit.of(obj.sizeMajArcmin, lens), lens),
        position = position?.let { TargetPositionFacts(it.altitudeDeg, it.azimuthDeg) },
        zoneId = zone.id,
        locale = locale,
    )
}

// =================================================================================================
// Rendering
// =================================================================================================

/**
 * The detail surface inside the wide window's second pane: the list stays visible beside it, so this
 * form has no dismissal of its own and takes its close control from the caller's own chrome.
 */
@Composable
fun TargetDetailPane(
    state: TargetDetailUiState,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LumintraPanel(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            DetailHeader(state = state, onToggleFavourite = onToggleFavourite, onClose = null)
            DetailBody(state = state)
        }
    }
}

/**
 * The detail surface on a narrow window: a modal sheet with its own dismissal, so the catalogue is
 * never covered by a pane it cannot close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetDetailSheet(
    state: TargetDetailUiState,
    onToggleFavourite: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            DetailHeader(state = state, onToggleFavourite = onToggleFavourite, onClose = onDismiss)
            DetailBody(state = state)
        }
    }
}

@Composable
private fun DetailHeader(
    state: TargetDetailUiState,
    onToggleFavourite: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val name = displayName(state.model)
    val favouriteDescription = stringResource(
        if (state.model.favourite) R.string.target_favourite_remove else R.string.target_favourite_add,
        name,
    )
    LumintraScaffoldHeader(
        title = state.model.designation,
        onBack = onClose,
        modifier = Modifier.fillMaxWidth(),
        actions = {
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = favouriteDescription },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_star),
                    contentDescription = null,
                    tint = if (state.model.favourite) {
                        LumintraTheme.colors.firstLight
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
    )
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
private fun DetailBody(state: TargetDetailUiState) {
    CatalogueSection(state = state)
    Spacer(Modifier.height(12.dp))
    WindowSection(state = state)
    Spacer(Modifier.height(12.dp))
    FitSection(state = state)
    Spacer(Modifier.height(12.dp))
    PositionSection(state = state)
}

@Composable
private fun CatalogueSection(state: TargetDetailUiState) {
    val model = state.model
    SectionStrip(title = stringResource(R.string.target_detail_catalogue_facts))
    ValueRow(
        label = stringResource(R.string.target_detail_designations),
        value = state.designations.joinToString(" · "),
    )
    ValueRow(
        label = stringResource(R.string.target_filter_type),
        value = stringResource(objectTypeLabel(model.type)),
    )
    ValueRow(label = stringResource(R.string.target_dimension_magnitude), value = magnitudeText(model))
    ValueRow(label = stringResource(R.string.target_dimension_size), value = sizeText(model))
    ValueRow(
        label = stringResource(R.string.target_detail_aliases),
        value = if (state.aliases.isEmpty()) {
            stringResource(R.string.target_aliases_none)
        } else {
            state.aliases.joinToString(", ")
        },
    )
    state.verification?.let { verification ->
        ValueRow(
            label = stringResource(R.string.target_detail_sources),
            value = stringResource(
                R.string.target_detail_sources_value,
                verification.srcA,
                verification.srcB,
                detailNumber(state.locale, verification.deltaArcmin, 2),
            ),
        )
    }
}

@Composable
private fun WindowSection(state: TargetDetailUiState) {
    SectionStrip(title = stringResource(R.string.target_detail_window))
    val time = rememberDetailTime(state)
    val name = displayName(state.model)
    when (val window = state.window) {
        TargetWindowFacts.NoSpot -> {
            Text(stringResource(R.string.target_detail_needs_spot), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.target_detail_needs_spot_body), style = MaterialTheme.typography.bodyMedium)
        }

        TargetWindowFacts.NoNight -> {
            Text(stringResource(R.string.target_detail_needs_night), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.target_detail_needs_night_body), style = MaterialTheme.typography.bodyMedium)
        }

        is TargetWindowFacts.NoDarkNight -> {
            Text(stringResource(R.string.target_detail_no_dark_night, darkKindWord(window.kind)))
        }

        is TargetWindowFacts.Circumpolar -> {
            Text(
                stringResource(
                    R.string.target_circumpolar,
                    name,
                    detailNumber(state.locale, window.minimumAltitudeDeg, 1),
                    time(window.atInstant),
                ),
            )
            Text(stringResource(R.string.target_circumpolar_short), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.target_visible_yes),
                style = MaterialTheme.typography.bodySmall,
                color = LumintraTheme.colors.darkWindow,
            )
        }

        is TargetWindowFacts.NeverRises -> {
            Text(stringResource(R.string.target_never_rises, name))
            Text(
                stringResource(R.string.target_never_rises_short),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.target_visible_no), style = MaterialTheme.typography.bodySmall)
        }

        is TargetWindowFacts.Visible -> {
            if (window.intervals.isEmpty()) {
                Text(stringResource(R.string.target_never_rises, name))
                Text(stringResource(R.string.target_visible_no), style = MaterialTheme.typography.bodySmall)
            } else {
                if (window.intervals.size > 1) {
                    Text(stringResource(R.string.target_multiple_intervals, window.intervals.size))
                }
                window.intervals.forEach { interval -> IntervalLine(state = state, interval = interval, name = name) }
                Text(
                    text = stringResource(
                        if (window.tonight) R.string.target_visible_yes else R.string.target_visible_no,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (window.tonight) {
                        LumintraTheme.colors.darkWindow
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun IntervalLine(state: TargetDetailUiState, interval: TargetInterval, name: String) {
    val time = rememberDetailTime(state)
    val locale = state.locale
    val start = time(interval.startUtc)
    val end = time(interval.endUtc)
    val peak = detailNumber(locale, interval.peakAltitudeDeg, 1)
    val transit = interval.transitUtc
    Text(
        text = if (transit == null) {
            stringResource(R.string.target_interval_no_transit, start, end, peak)
        } else {
            stringResource(R.string.target_interval_line, start, end, peak, time(transit))
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (interval.isPartial) {
        Text(
            text = stringResource(R.string.target_partial_window, name, start, end),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (interval.isGrazing) {
        Text(
            text = stringResource(
                R.string.target_graze,
                name,
                detailNumber(locale, interval.peakAltitudeDeg, 2),
                detailSpan(interval.durationSeconds(), locale),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = LumintraTheme.colors.caution,
        )
    } else if (interval.clearsDrawnHorizon) {
        // The drawn horizon, not the altitude mask, set this boundary — stated, so a window the user's
        // own skyline limits is never read as one the target produced.
        Text(
            text = stringResource(
                R.string.target_at_horizon,
                name,
                time(interval.transitUtc ?: interval.startUtc),
                detailNumber(locale, interval.effectiveHorizonDeg, 1),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FitSection(state: TargetDetailUiState) {
    SectionStrip(title = stringResource(R.string.target_detail_fit))
    val facts = state.fit
    if (facts == null) {
        Text(stringResource(R.string.target_detail_needs_spot_body), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val fit = facts.fit
    Text(
        text = stringResource(
            when (fit.verdict) {
                FitVerdict.FITS -> R.string.target_fit_fits
                FitVerdict.MARGINAL -> R.string.target_fit_marginal
                FitVerdict.TOO_LARGE -> R.string.target_fit_too_large
                FitVerdict.UNKNOWN_SIZE -> R.string.target_fit_point_source
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    val objectSize = fit.objectAngularSizeDeg
    if (objectSize == null) {
        return
    }
    Text(
        text = stringResource(
            R.string.target_fit_numbers,
            detailNumber(state.locale, objectSize, 2),
            detailNumber(state.locale, fit.frameFovShortSideDeg, 2),
            detailNumber(state.locale, facts.lens.focalLengthMm, 0),
            detailNumber(state.locale, facts.lens.sensorWidthMm, 1),
            detailNumber(state.locale, facts.lens.sensorHeightMm, 1),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    fit.pixelsOnSensor?.let { pixels ->
        Text(
            text = stringResource(R.string.target_fit_pixels, detailNumber(state.locale, pixels, 0)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    fit.frameFrac?.let { fraction ->
        ValueRow(
            label = stringResource(R.string.target_detail_fit),
            value = stringResource(
                R.string.target_fit_frame_fraction,
                detailNumber(state.locale, fraction * 100.0, 0) + "%",
                facts.lens.name,
            ),
        )
    }
}

@Composable
private fun PositionSection(state: TargetDetailUiState) {
    SectionStrip(title = stringResource(R.string.target_detail_position))
    val position = state.position
    if (position == null) {
        Text(stringResource(R.string.target_detail_needs_night_body), style = MaterialTheme.typography.bodyMedium)
        return
    }
    Text(
        text = stringResource(
            R.string.target_alt_az,
            detailNumber(state.locale, position.altitudeDeg, 1),
            detailNumber(state.locale, position.azimuthDeg, 1),
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
}

// =================================================================================================
// Small local helpers
// =================================================================================================

/** A number with [digits] decimals in [locale]; every template that needs one already appends its unit. */
internal fun detailNumber(locale: Locale, value: Double, digits: Int): String =
    String.format(locale, "%.${digits}f", value)

/** "1 h 20 min" for a duration in seconds, in the locale's words. */
internal fun detailSpan(seconds: Long, locale: Locale): String =
    com.gexetr.lumintralightplanner.ui.format.formatDurationShort(
        java.time.Duration.ofSeconds(seconds),
        locale,
    )

private fun TargetInterval.durationSeconds(): Long = java.time.Duration.between(startUtc, endUtc).seconds

@Composable
private fun darkKindWord(kind: DarkWindowKind): String = stringResource(
    when (kind) {
        DarkWindowKind.DARK -> R.string.share_dark_kind_dark
        DarkWindowKind.POLAR_DAY -> R.string.share_dark_kind_polar_day
        DarkWindowKind.POLAR_NIGHT -> R.string.share_dark_kind_polar_night
        DarkWindowKind.NO_ASTRO_DARK -> R.string.share_dark_kind_no_astro
        DarkWindowKind.NO_DARK -> R.string.share_dark_kind_no_dark
    },
)

/**
 * The clock the detail surface renders with, in the zone the state carries.
 *
 * The 12/24-hour choice is read from the platform at the call site rather than stored in the state,
 * because it is a device preference and not part of the computed answer.
 */
@Composable
internal fun rememberDetailTime(state: TargetDetailUiState): (Instant) -> String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val use24Hour = remember(configuration) { android.text.format.DateFormat.is24HourFormat(context) }
    val zone = remember(state.zoneId) {
        if (state.zoneId.isEmpty()) ZoneId.systemDefault() else runCatching { ZoneId.of(state.zoneId) }
            .getOrElse { ZoneId.systemDefault() }
    }
    val locale = state.locale
    return remember(zone, locale, use24Hour) {
        { instant: Instant -> formatTime(instant, zone, locale, use24Hour) }
    }
}
