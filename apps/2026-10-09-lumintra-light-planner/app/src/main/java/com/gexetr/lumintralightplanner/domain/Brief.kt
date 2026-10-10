package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** The five key-time rows the brief's table carries. Declaration order is the table's order. */
enum class KeyTimeBand {
    /** Sun geometric altitude between **+6.0° and −4.0°**. A product decision, not a measured fact. */
    GOLDEN_HOUR,

    /** Sun geometric altitude between **−4.0° and −8.0°**. Distinct from golden hour, never a synonym. */
    BLUE_HOUR,

    /** USNO geometric Sun-centre crossing of **−6.0°** — no refraction, no semidiameter. */
    CIVIL,

    /** USNO geometric Sun-centre crossing of **−12.0°**. */
    NAUTICAL,

    /** USNO geometric Sun-centre crossing of **−18.0°**. */
    ASTRONOMICAL;

    /** True for the three twilight bands, which are Sun-centre crossings and never sunrise/sunset. */
    val isTwilight: Boolean get() = this == CIVIL || this == NAUTICAL || this == ASTRONOMICAL

    companion object {
        const val GOLDEN_HIGH_DEG: Double = 6.0
        const val GOLDEN_LOW_DEG: Double = -4.0
        const val BLUE_LOW_DEG: Double = -8.0
    }
}

/**
 * One row of the key-times table.
 *
 * Bands are half-open `[start, end)` and a zero-length interval is **dropped** rather than drawn as a
 * hairline; a band that does not occur on the night (a polar night or day) has no row at all, so
 * [occurs] is what the UI tests rather than a width.
 */
data class KeyTimeRow(
    val band: KeyTimeBand,
    val intervals: List<NightSpan>,
) {
    val occurs: Boolean get() = intervals.isNotEmpty()

    /** The evening occurrence, which is the one a user planning tonight reads first. */
    val evening: NightSpan? get() = intervals.firstOrNull()

    /** The morning occurrence, when the night carries a symmetric one. */
    val morning: NightSpan? get() = intervals.getOrNull(1)

    companion object {
        /** Builds a row, dropping the zero-length intervals rather than drawing them. */
        fun of(band: KeyTimeBand, intervals: List<NightSpan>): KeyTimeRow =
            KeyTimeRow(band, intervals.filterNot { it.isEmpty })
    }
}

/**
 * The sunrise and sunset of the session, computed with the **90.8333° zenith distance** test — a
 * different quantity from every twilight row, and stated apart from them for that reason.
 */
data class SunriseSunsetTimes(
    val sunsetUtc: Instant?,
    val sunriseUtc: Instant?,
) {
    val hasBoth: Boolean get() = sunsetUtc != null && sunriseUtc != null
}

/** The closed set of things the brief can say. Declaration order is the tie-break precedence. */
enum class ConditionKind {
    DARK_WINDOW,
    MOON,
    CLOUD,
    TARGET,
    SATELLITE,
    HORIZON,
}

/** The three assessment states. A condition with no external input carries none of these. */
enum class ConditionState {
    GOOD,
    MARGINAL,
    BLOCKING,
}

/** Why a condition has no value. Always typed, never a free-text apology. */
enum class ConditionUnavailableReason {
    /** No network: the forecast and the element sets cannot be reached. */
    OFFLINE,

    /** The setting that gates this source is off, so no request was made at all. */
    SOURCE_DISABLED,

    /** Nothing has been fetched yet in this session. */
    NOT_FETCHED,

    /** No spot is active, so nothing can be computed. */
    NO_SPOT,

    /** No night has been chosen. */
    NO_NIGHT,

    /** The element set is older than 72 hours: no pass time may be computed. */
    ELEMENTS_EXPIRED,

    /** The night offers no dark window to describe. */
    NO_DARK_WINDOW,

    /** The bundled catalogue asset is missing or unparseable. */
    CATALOGUE_UNREADABLE,

    /** No horizon profile has been drawn for the spot. */
    HORIZON_MISSING,
}

/** A condition's assessment: either one of the three states, or a typed absence. */
sealed interface ConditionAssessment {
    data class Assessed(val state: ConditionState) : ConditionAssessment

    data class Unavailable(val reason: ConditionUnavailableReason) : ConditionAssessment
}

/** Which input a condition's value came from, so its provenance travels with it. */
enum class ConditionInput {
    EPHEMERIS,
    CATALOGUE,
    HORIZON_PROFILE,
    FORECAST_MET_NORWAY,
    ELEMENT_SET_CELESTRAK,
    USER_SETTING,
    USER_ESTIMATE,
}

/**
 * The source of a value: the input it came from, and that input's age when it has one.
 *
 * The wording of the label is the UI's, keyed off [input] — a domain that carried an English label
 * would be a hardcoded string in the one layer that cannot be localised.
 */
data class ConditionSource(
    val input: ConditionInput,
    val age: Duration? = null,
    val fetchedAtUtc: Instant? = null,
)

/** The typed payload each condition kind carries. No strings: the UI maps kind plus payload to copy. */
sealed interface BriefFacts {
    data class DarkWindow(val report: DarkWindowReport) : BriefFacts

    data class Moon(val state: MoonState, val moonless: MoonlessWindow) : BriefFacts

    data class Cloud(val report: CloudReport?, val meanPercent: Int?) : BriefFacts

    data class Target(
        val targetId: String,
        val window: TargetWindow,
        /** The peak altitude above the mask, and how long it held it — the MARGINAL numbers. */
        val peakAltitudeDeg: Double?,
        val durationAboveMask: Duration?,
    ) : BriefFacts

    data class Satellite(val pass: SatellitePass) : BriefFacts

    data class Horizon(val profileId: String?, val occludesChosenTarget: Boolean) : BriefFacts
}

/**
 * One named condition of the brief: what it is about, when it attaches, how it is assessed, and the
 * facts behind it.
 *
 * A condition is **never silently dropped** — an input that is absent becomes
 * [ConditionAssessment.Unavailable] and still appears in the list.
 */
data class BriefCondition(
    val kind: ConditionKind,
    /** The instant this condition attaches to; `null` for an all-night value, anchored by the brief. */
    val anchorUtc: Instant?,
    val assessment: ConditionAssessment,
    val facts: BriefFacts,
    val source: ConditionSource? = null,
) {
    /** The three-way state, or `null` when the condition is unavailable. */
    val state: ConditionState? get() = (assessment as? ConditionAssessment.Assessed)?.state

    val isUnavailable: Boolean get() = assessment is ConditionAssessment.Unavailable

    val unavailableReason: ConditionUnavailableReason?
        get() = (assessment as? ConditionAssessment.Unavailable)?.reason

    /** The precedence used to break a tie between two conditions anchored to the same instant. */
    val precedence: Int get() = kind.ordinal
}

/** A non-condition notice the brief shows once, beside the answer rather than instead of it. */
sealed interface BriefNotice {
    /** The device clock disagrees with a response's `Date` header by more than a few minutes. */
    data class DeviceClockSkew(val deltaMinutes: Long) : BriefNotice

    /** One saved spot could not be read; the rest loaded. */
    data class SavedSpotUnreadable(val count: Int) : BriefNotice

    /** A saved reference points at a target the compiled catalogue no longer contains. */
    data class SavedTargetMissing(val count: Int) : BriefNotice

    /** The bundled catalogue is missing or unparseable: the app runs, targets are omitted. */
    data object CatalogueUnreadable : BriefNotice
}

/**
 * The assembled answer for one (spot, night).
 *
 * [conditions] is already ordered — the ordering rule lives in [BriefOrdering] so that the same order
 * is produced wherever a brief is built, rather than being re-derived by each renderer.
 */
data class SessionBrief(
    val spotId: String,
    val spotName: String,
    val tzId: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    /** The local date of the session's **sunset** — never the device's current date. */
    val nightDate: LocalDate,
    val session: NightSpan,
    val darkWindow: DarkWindowReport,
    val keyTimes: List<KeyTimeRow>,
    val sunriseSunset: SunriseSunsetTimes,
    val moon: MoonState,
    val moonless: MoonlessWindow,
    val targetWindows: Map<String, TargetWindow>,
    val passes: List<SatellitePass>,
    val cloud: CloudReport?,
    val skyGrade: SkyGrade,
    val conditions: List<BriefCondition>,
    val notices: List<BriefNotice> = emptyList(),
    /** The session's own element-set age, when a satellite prediction exists. */
    val elementSetAge: ElementSetAge? = null,
) {
    /** The targets that are up on this night, in catalogue order, for "what is up tonight". */
    val visibleTargetIds: Set<String>
        get() = targetWindows.filterValues { it.isVisibleTonight }.keys

    val visiblePasses: List<SatellitePass> get() = passes.filter { it.observable }

    /** True when the brief has no dark window at all, so the reminder is not offered. */
    val offersReminder: Boolean get() = darkWindow.reminderEligible

    val hasTargets: Boolean get() = targetWindows.isNotEmpty()

    /** The one row a band produces, or `null` when the band does not occur on this night. */
    fun keyTimeRow(band: KeyTimeBand): KeyTimeRow? = keyTimes.firstOrNull { it.band == band && it.occurs }
}

/** The brief's own states, so that "no spot" is never a fabricated sky and never a blank panel. */
sealed interface BriefState {
    /** No saved spot: a scoped empty state with one call to action. No sky is computed. */
    data object NoSpot : BriefState

    /** No night chosen: the typed missing-night state rather than a stale instant. */
    data object NoNight : BriefState

    /** A complete answer. */
    data class Ready(val brief: SessionBrief) : BriefState
}

/**
 * The ordering rule, in one place.
 *
 * Conditions are ordered **chronologically by the instant each one attaches to, ascending**; a
 * condition not attached to a single instant is anchored to the dark window's start; and two
 * conditions at the same instant are broken by the fixed precedence
 * `DARK_WINDOW`, `MOON`, `CLOUD`, `TARGET`, `SATELLITE`, `HORIZON`.
 */
object BriefOrdering {

    fun order(conditions: List<BriefCondition>, darkWindowStartUtc: Instant?): List<BriefCondition> {
        val anchored = conditions.map { condition ->
            condition to (condition.anchorUtc ?: darkWindowStartUtc ?: Instant.MAX)
        }
        return anchored
            .sortedWith(compareBy({ it.second }, { it.first.precedence }))
            .map { it.first }
    }
}
