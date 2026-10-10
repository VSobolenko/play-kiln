package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.domain.BriefCondition
import com.gexetr.lumintralightplanner.domain.BriefFacts
import com.gexetr.lumintralightplanner.domain.BriefNotice
import com.gexetr.lumintralightplanner.domain.BriefOrdering
import com.gexetr.lumintralightplanner.domain.CloudReport
import com.gexetr.lumintralightplanner.domain.ConditionAssessment
import com.gexetr.lumintralightplanner.domain.ConditionInput
import com.gexetr.lumintralightplanner.domain.ConditionKind
import com.gexetr.lumintralightplanner.domain.ConditionSource
import com.gexetr.lumintralightplanner.domain.ConditionState
import com.gexetr.lumintralightplanner.domain.ConditionUnavailableReason
import com.gexetr.lumintralightplanner.domain.CrossingDirection
import com.gexetr.lumintralightplanner.domain.DarkWindowClassifier
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.DarkWindowReport
import com.gexetr.lumintralightplanner.domain.ElementSetAge
import com.gexetr.lumintralightplanner.domain.EquipmentProfile
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.KeyTimeBand
import com.gexetr.lumintralightplanner.domain.KeyTimeRow
import com.gexetr.lumintralightplanner.domain.MoonState
import com.gexetr.lumintralightplanner.domain.MoonlessKind
import com.gexetr.lumintralightplanner.domain.MoonlessWindow
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.SessionBrief
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.domain.SkyBrightnessEstimate
import com.gexetr.lumintralightplanner.domain.SkyGrade
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.SunMeasurements
import com.gexetr.lumintralightplanner.domain.SunriseSunsetTimes
import com.gexetr.lumintralightplanner.domain.TargetFit
import com.gexetr.lumintralightplanner.domain.TargetInterval
import com.gexetr.lumintralightplanner.domain.TargetWindow
import com.gexetr.lumintralightplanner.domain.TwilightKind
import com.gexetr.lumintralightplanner.domain.TwilightWindow
import com.gexetr.lumintralightplanner.domain.WindowBoundary
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Everything one night's computation is a function of.
 *
 * It is a plain value with no context and no clock: the geometry, the rules and the ephemeris are
 * supplied by [NightEngine] rather than read from the device. [nowUtc] is the caller's reference instant
 * and is used only for the **age** reported beside an external value (the forecast) — no astronomical
 * quantity depends on it, which is what makes the same request produce the same computation.
 */
data class NightRequest(
    /** The spot: coordinates, elevation, zone id and equipment. */
    val spot: Scout,
    /** The spot's drawn horizon, or `null` when none has been drawn (drawn as a flat horizon). */
    val horizon: HorizonProfile?,
    /** The equipment the fit statements are computed with. */
    val equipment: EquipmentProfile,
    /** The session window `[localNoon, localNoon + 24 h)` for this spot. */
    val span: NightSpan,
    /** The spot's zone; it must be the one [Scout.tzId] declares, so the two cannot disagree. */
    val zone: ZoneId,
    /** The caller's reference instant. Used for forecast age only; never for a position. */
    val nowUtc: Instant,
    /** The ids the brief treats as favourites (each that is up tonight earns a TARGET condition). */
    val favouriteIds: Set<String> = emptySet(),
    /** The catalogue rows to evaluate: fixed rows plus the computed Solar-System rows. */
    val objects: List<SkyObject> = emptyList(),
    /** The cloud outlook, when a forecast has been fetched. `null` is the unavailable case. */
    val cloud: CloudReport? = null,
    /** Predicted satellite passes for the night, when the element set allowed a computation. */
    val passes: List<SatellitePass> = emptyList(),
    /** The element set's age, when a satellite prediction exists. */
    val elementSetAge: ElementSetAge? = null,
    /** The user's altitude mask in degrees: the effective horizon is never below this. */
    val altitudeMaskDeg: Double = DEFAULT_ALTITUDE_MASK_DEG,
    /** The user's own sky-brightness estimate; carried through, never folded into [SkyGrade]. */
    val skyBrightnessEstimate: SkyBrightnessEstimate = SkyBrightnessEstimate.NONE,
    /** Notices the caller has already discovered (a saved spot unreadable, a stale element set). */
    val notices: List<BriefNotice> = emptyList(),
) {
    init {
        require(zone.id == spot.tzId) {
            "NightRequest zone ${zone.id} disagrees with the spot's zone id ${spot.tzId}"
        }
        require(altitudeMaskDeg in -90.0..90.0) { "altitudeMaskDeg $altitudeMaskDeg outside [-90, 90]" }
    }

    val point: GeodeticPoint get() = spot.point

    companion object {
        /** The default altitude mask, matching the app's own default (`TargetInterval`'s 10°). */
        const val DEFAULT_ALTITUDE_MASK_DEG: Double = 10.0
    }
}

/**
 * One immutable computation for one (spot, night): everything the Night and Brief screens render.
 *
 * It is assembled once by [NightEngine.compute] and read many times. The per-scrub half lives in
 * `SceneBuilder`, which projects facts already in hand rather than re-running a search — so a scrub
 * costs a projection, not a twilight computation.
 *
 * The typed notice list is carried rather than folded into prose: a notice is a domain object and its
 * wording belongs to the screens.
 */
data class NightComputation(
    val request: NightRequest,
    /** What the Sun did over the session: extrema, rise/set and the intervals below each threshold. */
    val sun: SunMeasurements,
    /** The one classified answer, from `DarkWindowClassifier` — never re-derived here. */
    val darkWindow: DarkWindowReport,
    /** The Moon at local midnight, with both its illuminated fraction and its altitude. */
    val moon: MoonState,
    /** The moonless sub-intervals of the dark window, as a typed answer. */
    val moonless: MoonlessWindow,
    /** Every twilight band of the night, all three levels, in chronological order. */
    val twilightWindows: List<TwilightWindow>,
    /** The 90.8333° sunrise/sunset test, kept apart from every twilight row. */
    val sunriseSunset: SunriseSunsetTimes,
    /** The five key-time rows (golden, blue, civil, nautical, astronomical). */
    val keyTimes: List<KeyTimeRow>,
    /** The computed darkness description. */
    val skyGrade: SkyGrade,
    /** Per-target classification, keyed by catalogue id, in catalogue order. */
    val targetWindows: Map<String, TargetWindow>,
    /** Per-target fit statement, keyed by catalogue id. */
    val targetFits: Map<String, TargetFit>,
    /** The assembled brief the Brief screen renders. */
    val brief: SessionBrief,
) {
    /** The catalogue rows the request asked to evaluate. */
    val objects: List<SkyObject> get() = request.objects

    /** The spot's horizon, or `null` when none has been drawn. */
    val horizon: HorizonProfile? get() = request.horizon

    val session: NightSpan get() = request.span

    val darkWindows: List<NightSpan> get() = darkWindow.darkWindows

    val notices: List<BriefNotice> get() = request.notices

    /** The longest dark window, the one a session is planned around, or `null` when there is none. */
    val bestDarkWindow: NightSpan? get() = darkWindow.bestWindow
}

/**
 * The night's numbers, as a **pure function of a [NightRequest]**.
 *
 * Every quantity is computed from the request and the injected [SkyEphemeris], so two calls with the
 * same request produce equal computations (the idempotence the Night screen's memoisation relies on)
 * and the whole engine is testable on the JVM with the real vendored ephemeris and no device.
 *
 * **The classification is not re-implemented here.** The precedence `POLAR_DAY` → `POLAR_NIGHT` → `DARK`
 * → `NO_ASTRO_DARK` → `NO_DARK` belongs to `DarkWindowClassifier.classify`, which is called with the
 * [SunMeasurements] this engine measured. Re-deriving the order here would be the defect the domain type
 * exists to prevent.
 *
 * **The twilight thresholds are geometric centres.** Every −6/−12/−18° interval here comes from
 * [TwilightSearch] over the Sun's `AltitudeModel.GEOMETRIC` altitude (no refraction, no semidiameter);
 * the only semidiameter + refraction test in play is [SkyEphemeris.sunriseSunset], which is the
 * 90.8333° sunrise/sunset pair and is reported separately.
 */
object NightEngine {

    /** The sampled step every search here uses: ten minutes. */
    const val SAMPLE_STEP_SECONDS: Long = TwilightSearch.DEFAULT_STEP_SECONDS

    /** The transit and extremum refinement: one minute. */
    const val TRANSIT_REFINE_SECONDS: Double = 60.0

    /** How many favourite TARGET conditions the brief carries before it stops adding rows. */
    const val TARGET_CONDITION_LIMIT: Int = 6

    /** The instant whose Moon the brief reports: local midnight, twelve hours from local noon. */
    private val MIDNIGHT_OFFSET: Duration = Duration.ofHours(12)

    /**
     * Computes one night. This is the whole entry point; everything else here is a private step.
     */
    fun compute(request: NightRequest, ephemeris: SkyEphemeris): NightComputation {
        val point = request.point
        val session = request.span
        val sunAlt: (Instant) -> Double =
            { ephemeris.sunHorizontal(point, it, AltitudeModel.GEOMETRIC).altitudeDeg }

        val sunriseSunset = ephemeris.sunriseSunset(point, session)

        val darkest = TwilightSearch.extremum(
            span = session,
            findMax = false,
            stepSeconds = SAMPLE_STEP_SECONDS,
            refineSeconds = TwilightSearch.DEFAULT_TOLERANCE_SECONDS,
            f = sunAlt,
        )
        val brightest = TwilightSearch.extremum(
            span = session,
            findMax = true,
            stepSeconds = SAMPLE_STEP_SECONDS,
            refineSeconds = TwilightSearch.DEFAULT_TOLERANCE_SECONDS,
            f = sunAlt,
        )

        val belowThreshold: Map<TwilightKind, List<NightSpan>> = TwilightKind.entries.associateWith { kind ->
            TwilightSearch.intervalsAtOrBelow(session, kind.angleDeg, SAMPLE_STEP_SECONDS, sunAlt)
        }

        val sun = SunMeasurements(
            session = session,
            sunsetUtc = sunriseSunset.sunsetUtc,
            sunriseUtc = sunriseSunset.sunriseUtc,
            sunAltMinDeg = darkest.value,
            sunAltMinInstant = darkest.instantUtc,
            sunAltMaxDeg = brightest.value,
            sunAltMaxInstant = brightest.instantUtc,
            belowThreshold = belowThreshold,
        )

        // The one classification. The precedence lives in the domain and is not restated here.
        val darkWindow = DarkWindowClassifier.classify(sun)

        val twilightWindows = TwilightKind.entries
            .flatMap { kind -> belowThreshold[kind].orEmpty().map { TwilightWindow(kind, it) } }
            .sortedBy { it.span.startUtc }

        val keyTimes = keyTimesOf(session, sunAlt, belowThreshold)

        val midnight = session.startUtc.plus(MIDNIGHT_OFFSET)
        val moon = ephemeris.moonState(point, midnight, AltitudeModel.REFRACTED)
        val moonless = moonlessWindow(session, darkWindow, point, moon, ephemeris)

        val darkDuration = NightSpan.total(belowThreshold[TwilightKind.ASTRONOMICAL].orEmpty())
        val skyGrade = SkyGrade(
            darkestSunAltitudeDeg = sun.sunAltMinDeg,
            darkestInstant = sun.sunAltMinInstant,
            darkWindowKind = darkWindow.kind,
            darkDuration = darkDuration,
            moonIlluminatedFraction = moon.illuminatedFraction,
            moonAltitudeDeg = moon.altitudeDeg,
        )

        val lens = request.equipment.toLensPreset()
        val assessments = LinkedHashMap<String, TargetAssessment>(request.objects.size)
        val windows = LinkedHashMap<String, TargetWindow>(request.objects.size)
        val fits = LinkedHashMap<String, TargetFit>(request.objects.size)
        for (obj in request.objects) {
            val assessment = targetWindowOf(obj, request, darkWindow, session, point, ephemeris)
            assessments[obj.id] = assessment
            windows[obj.id] = assessment.window
            fits[obj.id] = TargetFit.of(obj.sizeMajArcmin, lens)
        }

        val conditions = conditionsOf(request, darkWindow, moon, moonless, assessments)
        val nightDate = SessionClock.dateLabel(session, request.zone)

        val brief = SessionBrief(
            spotId = request.spot.id,
            spotName = request.spot.name,
            tzId = request.spot.tzId,
            latitudeDeg = request.spot.latDeg,
            longitudeDeg = request.spot.lonDeg,
            nightDate = nightDate,
            session = session,
            darkWindow = darkWindow,
            keyTimes = keyTimes,
            sunriseSunset = sunriseSunset,
            moon = moon,
            moonless = moonless,
            targetWindows = windows,
            passes = request.passes,
            cloud = request.cloud,
            skyGrade = skyGrade,
            conditions = BriefOrdering.order(conditions, darkWindow.darkWindows.firstOrNull()?.startUtc),
            notices = request.notices,
            elementSetAge = request.elementSetAge,
        )

        return NightComputation(
            request = request,
            sun = sun,
            darkWindow = darkWindow,
            moon = moon,
            moonless = moonless,
            twilightWindows = twilightWindows,
            sunriseSunset = sunriseSunset,
            keyTimes = keyTimes,
            skyGrade = skyGrade,
            targetWindows = windows,
            targetFits = fits,
            brief = brief,
        )
    }

    // -------------------------------------------------------------------------------------------------
    // Key times
    // -------------------------------------------------------------------------------------------------

    /**
     * The five key-time rows. Golden and blue hour are ranges the product defines relative to the
     * geometric centre; the three twilight rows are the USNO −6/−12/−18° crossings themselves.
     */
    private fun keyTimesOf(
        session: NightSpan,
        sunAlt: (Instant) -> Double,
        belowThreshold: Map<TwilightKind, List<NightSpan>>,
    ): List<KeyTimeRow> {
        val belowGoldenHigh = TwilightSearch.intervalsAtOrBelow(
            session, KeyTimeBand.GOLDEN_HIGH_DEG, SAMPLE_STEP_SECONDS, sunAlt,
        )
        val aboveGoldenLow = TwilightSearch.intervalsAbove(
            session, KeyTimeBand.GOLDEN_LOW_DEG, SAMPLE_STEP_SECONDS, sunAlt,
        )
        val belowGoldenLow = TwilightSearch.intervalsAtOrBelow(
            session, KeyTimeBand.GOLDEN_LOW_DEG, SAMPLE_STEP_SECONDS, sunAlt,
        )
        val aboveBlueLow = TwilightSearch.intervalsAbove(
            session, KeyTimeBand.BLUE_LOW_DEG, SAMPLE_STEP_SECONDS, sunAlt,
        )

        val golden = NightSpan.intersectAll(belowGoldenHigh, aboveGoldenLow)
        val blue = NightSpan.intersectAll(belowGoldenLow, aboveBlueLow)

        return listOf(
            KeyTimeRow.of(KeyTimeBand.GOLDEN_HOUR, golden),
            KeyTimeRow.of(KeyTimeBand.BLUE_HOUR, blue),
            KeyTimeRow.of(KeyTimeBand.CIVIL, belowThreshold[TwilightKind.CIVIL].orEmpty()),
            KeyTimeRow.of(KeyTimeBand.NAUTICAL, belowThreshold[TwilightKind.NAUTICAL].orEmpty()),
            KeyTimeRow.of(KeyTimeBand.ASTRONOMICAL, belowThreshold[TwilightKind.ASTRONOMICAL].orEmpty()),
        )
    }

    // -------------------------------------------------------------------------------------------------
    // Moonless window
    // -------------------------------------------------------------------------------------------------

    /**
     * The moonless sub-intervals of the dark window.
     *
     * `moonless = (!moonUp) || darkMoon`: a dark Moon (`illuminatedFraction <= 0.10`, a product
     * threshold) makes the whole dark window moonless; otherwise the moonless set is where the Moon is
     * below the horizon inside it. Every outcome is a typed [MoonlessKind], never an empty list shown as
     * "moonless all night" and never a fabricated zero-length interval.
     */
    private fun moonlessWindow(
        session: NightSpan,
        report: DarkWindowReport,
        point: GeodeticPoint,
        moon: MoonState,
        ephemeris: SkyEphemeris,
    ): MoonlessWindow {
        val darkWindows = report.darkWindows
        if (darkWindows.isEmpty()) return MoonlessWindow(MoonlessKind.NO_DARK_WINDOW)

        // The dark-Moon threshold: the whole dark window is moonless, by product decision.
        if (moon.isDarkMoon) {
            return MoonlessWindow(kind = MoonlessKind.MOONLESS_ALL_NIGHT, intervals = darkWindows)
        }

        val moonAlt: (Instant) -> Double =
            { ephemeris.moonHorizontal(point, it, AltitudeModel.REFRACTED).altitudeDeg }
        val crossings = TwilightSearch.crossings(
            span = session,
            targetAltitudeDeg = 0.0,
            stepSeconds = SAMPLE_STEP_SECONDS,
            toleranceSeconds = TwilightSearch.DEFAULT_TOLERANCE_SECONDS,
            f = moonAlt,
        )
        val upAtSessionStart = moonAlt(session.startUtc) > 0.0

        if (crossings.isEmpty()) {
            // The Moon never crosses the horizon inside the session.
            return if (upAtSessionStart) {
                val extreme = extremumWithin(session, false, moonAlt)
                MoonlessWindow(
                    kind = MoonlessKind.MOON_UP_ALL_WINDOW,
                    intervals = emptyList(),
                    extremeAltitudeDeg = extreme.value,
                    extremeInstant = extreme.instantUtc,
                    washedOut = moon.isFullMoon,
                )
            } else {
                MoonlessWindow(kind = MoonlessKind.MOON_NEVER_RISES, intervals = darkWindows)
            }
        }

        val below = TwilightSearch.intervalsAtOrBelow(session, 0.0, SAMPLE_STEP_SECONDS, moonAlt)

        // The Moon below the horizon for the whole dark window: the set equals the dark window.
        if (coversAll(below, darkWindows)) {
            return MoonlessWindow(kind = MoonlessKind.MOONLESS_ALL_NIGHT, intervals = darkWindows)
        }

        val intervals = NightSpan.intersectAll(below, darkWindows)
        if (intervals.isEmpty()) {
            // The Moon is up for the whole dark window: report the least-in-the-way instant, or the
            // most-in-the-way one when a full Moon washes the night out. Never swapped.
            val extreme = extremumWithin(session, moon.isFullMoon, moonAlt)
            return MoonlessWindow(
                kind = MoonlessKind.MOON_UP_ALL_WINDOW,
                intervals = emptyList(),
                extremeAltitudeDeg = extreme.value,
                extremeInstant = extreme.instantUtc,
                washedOut = moon.isFullMoon,
            )
        }

        val hasRise = crossings.any { it.direction == CrossingDirection.RISING }
        val hasSet = crossings.any { it.direction == CrossingDirection.FALLING }
        val kind = when {
            !hasRise -> MoonlessKind.MOON_NEVER_RISES
            !hasSet -> MoonlessKind.MOON_NEVER_SETS
            else -> MoonlessKind.MOONLESS_INTERVALS
        }
        return MoonlessWindow(kind = kind, intervals = intervals)
    }

    private fun extremumWithin(
        span: NightSpan,
        findMax: Boolean,
        f: (Instant) -> Double,
    ) = TwilightSearch.extremum(
        span = span,
        findMax = findMax,
        stepSeconds = SAMPLE_STEP_SECONDS,
        refineSeconds = TRANSIT_REFINE_SECONDS,
        f = f,
    )

    /** True when every span in [targets] is contained in some span of [candidates]. */
    private fun coversAll(candidates: List<NightSpan>, targets: List<NightSpan>): Boolean =
        targets.all { target ->
            candidates.any { c -> !c.startUtc.isAfter(target.startUtc) && !c.endUtc.isBefore(target.endUtc) }
        }

    // -------------------------------------------------------------------------------------------------
    // Target windows and fit statements
    // -------------------------------------------------------------------------------------------------

    /** A target's classified window plus the azimuth its peak altitude occurred at. */
    private class TargetAssessment(
        val window: TargetWindow,
        val peakAzimuthDeg: Double?,
    )

    /**
     * Classifies one catalogue row for the night.
     *
     * The altitude is sampled across the **whole session** at the ten-minute step (so a circumpolar or
     * never-rising target is named as such), while the intervals are the parts of the night's observing
     * base in which the target clears its **effective horizon** — `max(drawn horizon, altitude mask)`,
     * with refraction applied, never a bare 0°.
     */
    private fun targetWindowOf(
        obj: SkyObject,
        request: NightRequest,
        report: DarkWindowReport,
        session: NightSpan,
        point: GeodeticPoint,
        ephemeris: SkyEphemeris,
    ): TargetAssessment {
        val coords = obj.equatorial
        val altAt: (Instant) -> Double =
            { ephemeris.fixedHorizontal(point, coords, it, AltitudeModel.REFRACTED).altitudeDeg }

        val lowest = TwilightSearch.extremum(
            span = session,
            findMax = false,
            stepSeconds = SAMPLE_STEP_SECONDS,
            refineSeconds = TRANSIT_REFINE_SECONDS,
            f = altAt,
        )
        val highest = TwilightSearch.extremum(
            span = session,
            findMax = true,
            stepSeconds = SAMPLE_STEP_SECONDS,
            refineSeconds = TRANSIT_REFINE_SECONDS,
            f = altAt,
        )

        val peakAzimuth = ephemeris
            .fixedHorizontal(point, coords, highest.instantUtc, AltitudeModel.REFRACTED)
            .azimuthDeg
        val drawnHorizon = request.horizon?.altitudeAt(peakAzimuth) ?: Double.NEGATIVE_INFINITY
        val effectiveHorizon = maxOf(request.altitudeMaskDeg, drawnHorizon)
        val clearsDrawnHorizon = drawnHorizon > request.altitudeMaskDeg

        val base = observingBase(report, session)
        val above = TwilightSearch.intervalsAbove(session, effectiveHorizon, SAMPLE_STEP_SECONDS, altAt)
        val clipped = NightSpan.intersectAll(above, base)

        val intervals = clipped.map { span ->
            val peak = TwilightSearch.extremum(
                span = span,
                findMax = true,
                stepSeconds = SAMPLE_STEP_SECONDS,
                refineSeconds = TRANSIT_REFINE_SECONDS,
                f = altAt,
            )
            val low = TwilightSearch.extremum(
                span = span,
                findMax = false,
                stepSeconds = SAMPLE_STEP_SECONDS,
                refineSeconds = TRANSIT_REFINE_SECONDS,
                f = altAt,
            )
            TargetInterval(
                startUtc = span.startUtc,
                endUtc = span.endUtc,
                startBoundary = startBoundary(span.startUtc, base, session),
                endBoundary = endBoundary(span.endUtc, base, session),
                transitUtc = peak.instantUtc,
                peakAltitudeDeg = peak.value,
                minimumAltitudeDeg = low.value,
                effectiveHorizonDeg = effectiveHorizon,
                clearsDrawnHorizon = clearsDrawnHorizon,
            )
        }

        val window = TargetWindow.of(
            minimumAltitudeDeg = lowest.value,
            minimumAtInstant = lowest.instantUtc,
            maximumAltitudeDeg = highest.value,
            effectiveHorizonDeg = effectiveHorizon,
            intervals = intervals,
        )
        return TargetAssessment(window = window, peakAzimuthDeg = peakAzimuth)
    }

    /**
     * The part of the night a target's visibility is measured against: the dark window when the night
     * has one, otherwise the deepest twilight actually reached, otherwise the whole session. A night
     * with no darkness therefore narrows nothing silently — it falls back to a stated level.
     */
    private fun observingBase(report: DarkWindowReport, session: NightSpan): List<NightSpan> = when {
        report.darkWindows.isNotEmpty() -> report.darkWindows
        report.twilightWindows.isNotEmpty() -> report.twilightWindows.map { it.span }
        else -> listOf(session)
    }

    private fun startBoundary(start: Instant, base: List<NightSpan>, session: NightSpan): WindowBoundary {
        if (base.none { it.startUtc == start }) return WindowBoundary.RISE
        return if (start == session.startUtc) WindowBoundary.SESSION_START else WindowBoundary.DARK_WINDOW_START
    }

    private fun endBoundary(end: Instant, base: List<NightSpan>, session: NightSpan): WindowBoundary {
        if (base.none { it.endUtc == end }) return WindowBoundary.SET
        return if (end == session.endUtc) WindowBoundary.SESSION_END else WindowBoundary.DARK_WINDOW_END
    }

    // -------------------------------------------------------------------------------------------------
    // Conditions
    // -------------------------------------------------------------------------------------------------

    /**
     * The brief's conditions: the dark window, the Moon, the cloud, the horizon, and one row per
     * favourite that is up tonight. An absent external input becomes a typed `Unavailable` and still
     * appears — a condition is never silently dropped.
     */
    private fun conditionsOf(
        request: NightRequest,
        report: DarkWindowReport,
        moon: MoonState,
        moonless: MoonlessWindow,
        assessments: Map<String, TargetAssessment>,
    ): List<BriefCondition> {
        val out = ArrayList<BriefCondition>()

        out.add(
            BriefCondition(
                kind = ConditionKind.DARK_WINDOW,
                anchorUtc = report.darkWindows.firstOrNull()?.startUtc,
                assessment = ConditionAssessment.Assessed(darkWindowState(report.kind)),
                facts = BriefFacts.DarkWindow(report),
                source = ConditionSource(ConditionInput.EPHEMERIS),
            ),
        )

        out.add(
            BriefCondition(
                kind = ConditionKind.MOON,
                anchorUtc = moonless.intervals.firstOrNull()?.startUtc,
                assessment = ConditionAssessment.Assessed(moonlessState(moonless)),
                facts = BriefFacts.Moon(state = moon, moonless = moonless),
                source = ConditionSource(ConditionInput.EPHEMERIS),
            ),
        )

        val cloud = request.cloud
        val meanCloud = cloud?.meanCloudPercentWithin(request.span)
        out.add(
            BriefCondition(
                kind = ConditionKind.CLOUD,
                anchorUtc = null,
                assessment = if (meanCloud == null) {
                    ConditionAssessment.Unavailable(ConditionUnavailableReason.NOT_FETCHED)
                } else {
                    ConditionAssessment.Assessed(cloudState(meanCloud))
                },
                facts = BriefFacts.Cloud(report = cloud, meanPercent = meanCloud),
                source = cloud?.let { forecastSource(it, request.nowUtc) },
            ),
        )

        val horizon = request.horizon
        val occludes = horizon != null && occludesAnyFavourite(horizon, assessments, request.favouriteIds)
        out.add(
            BriefCondition(
                kind = ConditionKind.HORIZON,
                anchorUtc = null,
                assessment = if (horizon == null) {
                    ConditionAssessment.Unavailable(ConditionUnavailableReason.HORIZON_MISSING)
                } else {
                    ConditionAssessment.Assessed(if (occludes) ConditionState.MARGINAL else ConditionState.GOOD)
                },
                facts = BriefFacts.Horizon(profileId = horizon?.id, occludesChosenTarget = occludes),
                source = horizon?.let { ConditionSource(ConditionInput.HORIZON_PROFILE) },
            ),
        )

        var added = 0
        for (id in request.favouriteIds) {
            if (added >= TARGET_CONDITION_LIMIT) break
            val window = assessments[id]?.window ?: continue
            if (!window.isVisibleTonight) continue
            val visible = window as? TargetWindow.Visible
            val best = visible?.bestInterval
            val peak = best?.peakAltitudeDeg
                ?: (window as? TargetWindow.Circumpolar)?.minimumAltitudeDeg
            val duration = best?.let { Duration.between(it.startUtc, it.endUtc) }
            out.add(
                BriefCondition(
                    kind = ConditionKind.TARGET,
                    anchorUtc = best?.startUtc,
                    assessment = ConditionAssessment.Assessed(
                        if (visible?.onlyGrazing == true) ConditionState.MARGINAL else ConditionState.GOOD,
                    ),
                    facts = BriefFacts.Target(
                        targetId = id,
                        window = window,
                        peakAltitudeDeg = peak,
                        durationAboveMask = duration,
                    ),
                    source = ConditionSource(ConditionInput.CATALOGUE),
                ),
            )
            added++
        }

        return out
    }

    private fun darkWindowState(kind: DarkWindowKind): ConditionState = when (kind) {
        DarkWindowKind.DARK, DarkWindowKind.POLAR_NIGHT -> ConditionState.GOOD
        DarkWindowKind.NO_ASTRO_DARK, DarkWindowKind.NO_DARK -> ConditionState.MARGINAL
        DarkWindowKind.POLAR_DAY -> ConditionState.BLOCKING
    }

    private fun moonlessState(moonless: MoonlessWindow): ConditionState = when {
        moonless.washedOut -> ConditionState.BLOCKING
        moonless.hasUsableWindow -> ConditionState.GOOD
        moonless.kind == MoonlessKind.NO_DARK_WINDOW -> ConditionState.BLOCKING
        else -> ConditionState.MARGINAL
    }

    private fun cloudState(meanPercent: Int): ConditionState = when {
        meanPercent <= 30 -> ConditionState.GOOD
        meanPercent <= 60 -> ConditionState.MARGINAL
        else -> ConditionState.BLOCKING
    }

    private fun forecastSource(cloud: CloudReport, nowUtc: Instant): ConditionSource {
        val raw = Duration.between(cloud.age.fetchedAtUtc, nowUtc)
        val age = if (raw.isNegative) Duration.ZERO else raw
        return ConditionSource(
            input = ConditionInput.FORECAST_MET_NORWAY,
            age = age,
            fetchedAtUtc = cloud.age.fetchedAtUtc,
        )
    }

    /**
     * True when the drawn horizon stands above a favourite's peak altitude **at the azimuth of that
     * peak** — the one place the horizon can actually block the chosen target.
     */
    private fun occludesAnyFavourite(
        horizon: HorizonProfile,
        assessments: Map<String, TargetAssessment>,
        favouriteIds: Set<String>,
    ): Boolean {
        for (id in favouriteIds) {
            val assessment = assessments[id] ?: continue
            val best = (assessment.window as? TargetWindow.Visible)?.bestInterval ?: continue
            val azimuth = assessment.peakAzimuthDeg ?: continue
            if (horizon.altitudeAt(azimuth) > best.peakAltitudeDeg) return true
        }
        return false
    }
}
