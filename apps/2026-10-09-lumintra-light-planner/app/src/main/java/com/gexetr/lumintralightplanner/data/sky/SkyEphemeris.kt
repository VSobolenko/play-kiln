package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.domain.Crossing
import com.gexetr.lumintralightplanner.domain.EquatorialCoords
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.HorizontalCoords
import com.gexetr.lumintralightplanner.domain.MoonLibration
import com.gexetr.lumintralightplanner.domain.MoonState
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SunriseSunsetTimes
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.libration
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.rotationEqjEqd
import io.github.cosinekitty.astronomy.searchRiseSet
import io.github.cosinekitty.astronomy.siderealTime
import java.time.Instant

/**
 * Which refraction convention an altitude request uses.
 *
 * The temporal-geometry section of the design makes the toggle **per use, never global**: the twilight
 * boundaries at −6/−12/−18° are the Sun's **geometric** centre and must be computed with no refraction
 * and no semidiameter, while a target's displayed altitude, the user's altitude mask and the Moon's
 * drawn position use [REFRACTED] (`Refraction.Normal`, Saemundsson). Passing the wrong one shifts the
 * dark window by minutes, which is why the choice is an explicit argument at every call site here
 * rather than a default hidden inside the engine.
 */
enum class AltitudeModel {
    /** `Refraction.None`: the body's geometric centre, as the twilight and dark-window tests require. */
    GEOMETRIC,

    /** `Refraction.Normal`: the apparent altitude, as target display, horizon masking and the Moon use. */
    REFRACTED,
}

/** The seven planets other than the Earth, for which the engine can compute a geocentric position. */
enum class SolarSystemBody {
    MERCURY,
    VENUS,
    MARS,
    JUPITER,
    SATURN,
    URANUS,
    NEPTUNE;

    /** The vendored engine's own enumeration value. */
    internal fun toEngine(): Body = when (this) {
        MERCURY -> Body.Mercury
        VENUS -> Body.Venus
        MARS -> Body.Mars
        JUPITER -> Body.Jupiter
        SATURN -> Body.Saturn
        URANUS -> Body.Uranus
        NEPTUNE -> Body.Neptune
    }
}

/** One sampled value of a time-varying altitude, with the instant it belongs to. */
data class SunAltitudeSample(
    val instantUtc: Instant,
    val altitudeDeg: Double,
)

/**
 * A geocentric direction in the ecliptic frame: latitude north (+) or south (−) of the ecliptic, and
 * longitude prograde from the equinox, both in degrees.
 *
 * It exists because the catalogue's computed Solar-System rows are the only place an ecliptic longitude
 * is meaningful (phase and elongation arithmetic). Every target position is built from
 * [EquatorialCoords] instead; nothing should invent an ecliptic coordinate of its own.
 */
data class EclipticDirection(
    val latDeg: Double,
    val lonDeg: Double,
)

/**
 * The astronomical engine the whole app reads a night from.
 *
 * Every method is a **pure function of its arguments**: the instant is always a parameter, never a
 * wall-clock read, and nothing here touches the device, the network or the file system. That is what
 * lets `NightEngine` be a pure function and the JVM unit tests run without a device.
 *
 * Three conventions are fixed here rather than left to each caller, because each one silently mirrors
 * or shifts the sky when it is wrong:
 *
 * - **Azimuth is `0° = North, 90° = East`, clockwise, in `[0, 360)`**; altitude is `−90…+90`, positive
 *   above the horizon. Both pass through [HorizontalCoords] so no caller has to remember the engine's
 *   own ±180 wrap.
 * - **Right ascension is DEGREES `[0, 360)` everywhere in this module.** The engine takes RA in *hours*
 *   and the only conversion is [EquatorialCoords.raHours] inside [AstronomySkyEphemeris]; hours are
 *   never stored, returned or accepted by this interface.
 * - **Observer is latitude N+/S−, longitude E+/W−, height in metres**, which is the vendored
 *   `Observer`'s own convention and [GeodeticPoint]'s, so the mapping is direct and sign-free.
 */
interface SkyEphemeris {

    /**
     * **Local** apparent sidereal time in hours `[0, 24)`, for [point].
     *
     * This is the sky's rotation angle for the instant and the place: the star field and the galactic
     * band are rotated by the same value, so the band follows the stars. It is what
     * `SceneFrame.skyRotationRad` carries (in radians), and two spots at different longitudes must not
     * produce the same value.
     *
     * The vendored `siderealTime()` is **Greenwich** apparent sidereal time in hours, so the east
     * longitude is added here as `lonDeg / 15.0` — an hours/degrees boundary, stated at the call site.
     */
    fun localSiderealHours(point: GeodeticPoint, instantUtc: Instant): Double

    /**
     * The Sun's **apparent topocentric** equatorial coordinates, epoch **J2000.0 / ICRS**, in degrees.
     *
     * `raDeg ∈ [0, 360)`. Topocentric: it is the direction from [point], not from the Earth's centre.
     */
    fun sunEquatorialJ2000(point: GeodeticPoint, instantUtc: Instant): EquatorialCoords

    /**
     * The Sun's topocentric horizontal direction for [model].
     *
     * [AltitudeModel.GEOMETRIC] is the twilight and dark-window input; [AltitudeModel.REFRACTED] is the
     * drawn position.
     */
    fun sunHorizontal(
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel = AltitudeModel.GEOMETRIC,
    ): HorizontalCoords

    /** The Sun's apparent visual magnitude at the instant (about −26.7). */
    fun sunMagnitude(instantUtc: Instant): Double

    /**
     * The Moon at one instant: illuminated fraction, phase angle, elongation, named phase, apparent
     * altitude/azimuth and magnitude, from one source with one refraction convention.
     */
    fun moonState(
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel = AltitudeModel.REFRACTED,
    ): MoonState

    /** The Moon's topocentric horizontal direction alone, for the many-sample moonless search. */
    fun moonHorizontal(
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel = AltitudeModel.REFRACTED,
    ): HorizontalCoords

    /** The Moon's sub-Earth libration, so the rendered disc shows the face the sky does. */
    fun moonLibration(instantUtc: Instant): MoonLibration

    /** The Moon's geocentric apparent angular diameter in degrees. */
    fun moonAngularDiameterDeg(instantUtc: Instant): Double

    /** The Moon's geocentric distance in kilometres, which the scene's disc scale needs. */
    fun moonDistanceKm(instantUtc: Instant): Double

    /** A planet's **geocentric** equatorial coordinates, epoch J2000.0 / ICRS, in degrees. */
    fun planetGeocentricEquatorial(body: SolarSystemBody, instantUtc: Instant): EquatorialCoords

    /** A planet's **geocentric** ecliptic direction, in degrees. */
    fun planetGeocentricEcliptic(body: SolarSystemBody, instantUtc: Instant): EclipticDirection

    /** A planet's topocentric horizontal direction for [model]. */
    fun planetHorizontal(
        body: SolarSystemBody,
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel,
    ): HorizontalCoords

    /** A planet's apparent visual magnitude at the instant. */
    fun planetMagnitude(body: SolarSystemBody, instantUtc: Instant): Double

    /**
     * A fixed catalogue direction's topocentric horizontal position.
     *
     * [coords] is J2000.0/ICRS; the adapter precesses it to the equator of date before projecting,
     * because the vendored `horizon()` requires of-date coordinates.
     */
    fun fixedHorizontal(
        point: GeodeticPoint,
        coords: EquatorialCoords,
        instantUtc: Instant,
        model: AltitudeModel = AltitudeModel.REFRACTED,
    ): HorizontalCoords

    /**
     * The sunset and sunrise **of the 90.8333° zenith-distance test** (16′ semidiameter + 34′
     * refraction) inside [session].
     *
     * A `null` is a case, never an error: it is the polar-day / polar-night signal that
     * `DarkWindowClassifier` reads. This is deliberately **not** the −18° dark-window test and **not** a
     * geometric-height crossing — `searchRiseSet` includes the semidiameter, the horizon dip and
     * near-horizon refraction, which is exactly what sunrise and sunset mean.
     */
    fun sunriseSunset(point: GeodeticPoint, session: NightSpan): SunriseSunsetTimes

    /**
     * A coarse sampling grid of the Sun's altitude across [span], one sample per [stepSeconds].
     *
     * The default step is the **10-minute** step every sampled event search in this app uses. This is
     * the "sampled and refined" half of the event-search rule; the crossings themselves come from
     * [sunAltitudeCrossings] or `TwilightSearch`.
     */
    fun sampleSunAltitude(
        point: GeodeticPoint,
        span: NightSpan,
        stepSeconds: Long = DEFAULT_SAMPLE_STEP_SECONDS,
        model: AltitudeModel = AltitudeModel.GEOMETRIC,
    ): List<SunAltitudeSample>

    /**
     * Every crossing of [targetAltitudeDeg] by the Sun's **geometric centre** inside [span], in
     * chronological order.
     *
     * This is the twilight-boundary and sunrise/sunset event search: segmented, stepped and refined, so
     * a short excursion through a threshold between two samples still yields both of its crossings, and
     * an empty list is a case rather than an error. The `−6/−12/−18°` boundaries and the `−0.8333°`
     * sunrise/sunset centre angle are all expressed through this one call.
     */
    fun sunAltitudeCrossings(
        point: GeodeticPoint,
        span: NightSpan,
        targetAltitudeDeg: Double,
        stepSeconds: Long = DEFAULT_SAMPLE_STEP_SECONDS,
        toleranceSeconds: Double = TwilightSearch.DEFAULT_TOLERANCE_SECONDS,
    ): List<Crossing>

    companion object {
        /** The sampled-search step every event search here uses: ten minutes. */
        const val DEFAULT_SAMPLE_STEP_SECONDS: Long = 600L
    }
}

/**
 * The production [SkyEphemeris]: a thin, explicit adapter over the vendored Astronomy Engine
 * (`io.github.cosinekitty.astronomy`, a source file in this module — no dependency to add).
 *
 * **The degrees/hours boundary, stated once.** Every domain type stores right ascension in **degrees**;
 * the engine's `horizon()` takes it in **sidereal hours** and converts internally with
 * `sinra = dsin(ra * 15.0)` (`astronomy.kt:5413`). So the forward conversion is made through the
 * domain's own named accessor [EquatorialCoords.raHours] (`raDeg / 15.0`) and the inverse through
 * [EquatorialCoords.ofHours] (`hours * 15.0`), and `15.0` appears beside an RA nowhere else in this
 * module. A stray hour/degree mix is the single most likely defect in this file, which is why both
 * directions go through a domain type rather than through arithmetic written here.
 *
 * **Observer sign convention, read from the vendored declaration** (`astronomy.kt:1412-1426`):
 * `Observer(latitude, longitude, height)` — latitude **N+ / S−**, longitude **E+ / W−**, height in
 * **metres** — identical to [GeodeticPoint]'s own convention, so [pointToObserver] carries no sign flip.
 * Azimuth comes back clockwise from north (`Topocentric`, `astronomy.kt:1685-1690`) and is normalised
 * into `[0, 360)` before it becomes a [HorizontalCoords].
 */
class AstronomySkyEphemeris : SkyEphemeris {

    override fun localSiderealHours(point: GeodeticPoint, instantUtc: Instant): Double {
        val greenwichHours = siderealTime(toEngineTime(instantUtc))
        // Degrees→hours for the longitude: the engine's sidereal time is Greenwich, and 15° of
        // longitude is one hour of rotation. This is the one place longitude meets hours.
        val localHours = greenwichHours + point.lonDeg / 15.0
        val m = localHours % 24.0
        return if (m < 0.0) m + 24.0 else m
    }

    override fun sunEquatorialJ2000(point: GeodeticPoint, instantUtc: Instant): EquatorialCoords {
        val time = toEngineTime(instantUtc)
        // Topocentric apparent J2000 equatorial direction: `equator(..., J2000, Corrected)` subtracts
        // the observer's geocentric vector and applies aberration, which is why the point matters.
        val equatorial = equator(Body.Sun, time, pointToObserver(point), EquatorEpoch.J2000, Aberration.Corrected)
        return fromEngineHours(equatorial.ra, equatorial.dec)
    }

    override fun sunHorizontal(
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel,
    ): HorizontalCoords = topocentricHorizontal(Body.Sun, point, instantUtc, model)

    override fun sunMagnitude(instantUtc: Instant): Double =
        illumination(Body.Sun, toEngineTime(instantUtc)).mag

    override fun moonState(
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel,
    ): MoonState {
        val time = toEngineTime(instantUtc)
        val light = illumination(Body.Moon, time)
        val horizontal = moonHorizontal(point, instantUtc, model)
        // Elongation is the Moon's geocentric ecliptic longitude minus the Sun's, `[0, 360)`; the
        // vendored `moonPhase()` is exactly that difference (0° new, 180° full) (`astronomy.kt:6173`).
        val elongation = moonPhase(time)
        return MoonState.of(
            illuminatedFraction = light.phaseFraction,
            phaseAngleDeg = light.phaseAngle,
            elongationDeg = elongation,
            altitudeDeg = horizontal.altitudeDeg,
            azimuthDeg = horizontal.azimuthDeg,
            magnitude = light.mag,
        )
    }

    override fun moonHorizontal(
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel,
    ): HorizontalCoords = topocentricHorizontal(Body.Moon, point, instantUtc, model)

    override fun moonLibration(instantUtc: Instant): MoonLibration {
        val lib = libration(toEngineTime(instantUtc))
        return MoonLibration(librationLatDeg = lib.elat, librationLonDeg = lib.elon)
    }

    override fun moonAngularDiameterDeg(instantUtc: Instant): Double =
        libration(toEngineTime(instantUtc)).diamDeg

    override fun moonDistanceKm(instantUtc: Instant): Double =
        libration(toEngineTime(instantUtc)).distanceKm

    override fun planetGeocentricEquatorial(
        body: SolarSystemBody,
        instantUtc: Instant,
    ): EquatorialCoords = geocentricEquatorial(body.toEngine(), instantUtc)

    override fun planetGeocentricEcliptic(
        body: SolarSystemBody,
        instantUtc: Instant,
    ): EclipticDirection {
        val time = toEngineTime(instantUtc)
        val ecliptic = equatorialToEcliptic(geoVector(body.toEngine(), time, Aberration.Corrected))
        return EclipticDirection(latDeg = ecliptic.elat, lonDeg = ecliptic.elon)
    }

    override fun planetHorizontal(
        body: SolarSystemBody,
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel,
    ): HorizontalCoords = topocentricHorizontal(body.toEngine(), point, instantUtc, model)

    override fun planetMagnitude(body: SolarSystemBody, instantUtc: Instant): Double =
        illumination(body.toEngine(), toEngineTime(instantUtc)).mag

    override fun fixedHorizontal(
        point: GeodeticPoint,
        coords: EquatorialCoords,
        instantUtc: Instant,
        model: AltitudeModel,
    ): HorizontalCoords {
        val time = toEngineTime(instantUtc)
        val observer = pointToObserver(point)
        val ofDate = precessToDate(coords, time)
        // `raHours` is the ONE degrees→hours conversion (EquatorialCoords.raDeg / 15.0): `horizon()`
        // takes sidereal hours, the domain stores degrees, and this is the boundary.
        val topo = horizon(time, observer, ofDate.raHours, ofDate.decDeg, refractionOf(model))
        return horizontal(topo.azimuth, topo.altitude)
    }

    override fun sunriseSunset(point: GeodeticPoint, session: NightSpan): SunriseSunsetTimes {
        val observer = pointToObserver(point)
        val start = toEngineTime(session.startUtc)
        // A one-day search forward from local noon covers the whole session locally. `null` is the polar
        // case, never an error, and `takeIf { it in session }` keeps both instants inside the half-open
        // session window this app reasons in. `searchRiseSet` is deliberately the semidiameter + dip +
        // near-horizon-refraction test (`astronomy.kt:6818-6845`), never the −0.8333° centre crossing.
        val sunsetEngine = searchRiseSet(Body.Sun, observer, Direction.Set, start, 1.0)
        val sunset = sunsetEngine?.let { toInstant(it) }?.takeIf { it in session }
        val sunriseFrom = sunsetEngine ?: start
        val sunriseEngine = searchRiseSet(Body.Sun, observer, Direction.Rise, sunriseFrom, 1.0)
        val sunrise = sunriseEngine?.let { toInstant(it) }?.takeIf { it in session }
        return SunriseSunsetTimes(sunsetUtc = sunset, sunriseUtc = sunrise)
    }

    override fun sampleSunAltitude(
        point: GeodeticPoint,
        span: NightSpan,
        stepSeconds: Long,
        model: AltitudeModel,
    ): List<SunAltitudeSample> = TwilightSearch.sample(span, stepSeconds) {
        sunHorizontal(point, it, model).altitudeDeg
    }

    override fun sunAltitudeCrossings(
        point: GeodeticPoint,
        span: NightSpan,
        targetAltitudeDeg: Double,
        stepSeconds: Long,
        toleranceSeconds: Double,
    ): List<Crossing> = TwilightSearch.crossings(span, targetAltitudeDeg, stepSeconds, toleranceSeconds) {
        sunHorizontal(point, it, AltitudeModel.GEOMETRIC).altitudeDeg
    }

    // -------------------------------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------------------------------

    /** A geocentric J2000.0/ICRS direction of an engine body, in degrees. */
    private fun geocentricEquatorial(body: Body, instantUtc: Instant): EquatorialCoords {
        val time = toEngineTime(instantUtc)
        val equatorial = geoVector(body, time, Aberration.Corrected).toEquatorial()
        return fromEngineHours(equatorial.ra, equatorial.dec)
    }

    /**
     * Topocentric horizontal coordinates of an engine body.
     *
     * `equator(..., EquatorEpoch.OfDate, ...)` returns the body's topocentric direction on the true
     * equator of date, which is what `horizon()` consumes, so the pair is used together and the
     * refraction convention is chosen per call by [model].
     */
    private fun topocentricHorizontal(
        body: Body,
        point: GeodeticPoint,
        instantUtc: Instant,
        model: AltitudeModel,
    ): HorizontalCoords {
        val time = toEngineTime(instantUtc)
        val observer = pointToObserver(point)
        val equatorial = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val topo = horizon(time, observer, equatorial.ra, equatorial.dec, refractionOf(model))
        return horizontal(topo.azimuth, topo.altitude)
    }

    /** Precesses a J2000.0/ICRS direction to the true equator of date, which `horizon()` requires. */
    private fun precessToDate(coords: EquatorialCoords, time: Time): EquatorialCoords {
        val raRad = Math.toRadians(coords.raNormalised)
        val decRad = Math.toRadians(coords.decDeg)
        val unit = Vector(
            Math.cos(decRad) * Math.cos(raRad),
            Math.cos(decRad) * Math.sin(raRad),
            Math.sin(decRad),
            time,
        )
        val ofDate = rotationEqjEqd(time).rotate(unit).toEquatorial()
        return fromEngineHours(ofDate.ra, ofDate.dec)
    }

    private fun refractionOf(model: AltitudeModel): Refraction = when (model) {
        AltitudeModel.GEOMETRIC -> Refraction.None
        AltitudeModel.REFRACTED -> Refraction.Normal
    }

    private fun pointToObserver(point: GeodeticPoint): Observer =
        Observer(point.latDeg, point.lonDeg, point.elevationM)

    private fun horizontal(azimuthDeg: Double, altitudeDeg: Double): HorizontalCoords =
        HorizontalCoords(
            altitudeDeg = altitudeDeg,
            azimuthDeg = HorizontalCoords.normaliseAzimuth(azimuthDeg),
        )

    private fun toEngineTime(instantUtc: Instant): Time =
        Time.fromMillisecondsSince1970(instantUtc.toEpochMilli())

    private fun toInstant(time: Time): Instant = Instant.ofEpochMilli(time.toMillisecondsSince1970())

    /**
     * The engine's sidereal hours → the app's degrees, through the domain's own named constructor so
     * the `× 15.0` exists in one place and only there.
     */
    private fun fromEngineHours(raHours: Double, decDeg: Double): EquatorialCoords {
        require(raHours.isFinite() && decDeg.isFinite()) { "engine returned a non-finite position" }
        return EquatorialCoords.ofHours(hours = raNormalisedHours(raHours), decDeg = decDeg)
    }

    /**
     * `EquatorialCoords.ofHours` multiplies by 15.0; a negative or wrapped-about RA from the engine is
     * normalised here so the stored degrees are always in the app's own `[0, 360)` convention before
     * the type sees them.
     */
    private fun raNormalisedHours(raHours: Double): Double {
        val m = raHours % 24.0
        return if (m < 0.0) m + 24.0 else m
    }
}
