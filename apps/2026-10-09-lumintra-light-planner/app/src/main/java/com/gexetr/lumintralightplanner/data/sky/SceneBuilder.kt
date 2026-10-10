package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.domain.EquatorialCoords
import com.gexetr.lumintralightplanner.domain.HorizontalCoords
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.LensPreset
import com.gexetr.lumintralightplanner.domain.ObjectKind
import com.gexetr.lumintralightplanner.domain.ObjectType
import com.gexetr.lumintralightplanner.domain.SceneBody
import com.gexetr.lumintralightplanner.domain.SceneCamera
import com.gexetr.lumintralightplanner.domain.SceneFrame
import com.gexetr.lumintralightplanner.domain.ScenePassTrail
import com.gexetr.lumintralightplanner.domain.ScenePoint
import com.gexetr.lumintralightplanner.domain.SceneScrub
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.TwilightKind
import com.gexetr.lumintralightplanner.domain.FieldOfView
import java.time.Instant

/**
 * The words a frame needs for the bodies the catalogue does not name.
 *
 * The Sun, the Moon and the seven planets have no row in `objects.json`, so their labels cannot come
 * from a [SkyObject]. They are **supplied by the caller** rather than written here: this module is in
 * `data/`, which has no `Context` and therefore no `stringResource`, and a literal in this file would
 * be a hardcoded user-facing string in the one layer that cannot be localised. The screens pass the
 * resource-backed strings in.
 */
data class SceneNames(
    /** The Sun's label, from `R.string.scene_body_sun`. */
    val sun: String,
    /** The Moon's label, from `R.string.scene_body_moon`. */
    val moon: String,
    /** One label per planet; a missing entry falls back to the engine enum's own name. */
    val planets: Map<SolarSystemBody, String> = emptyMap(),
)

/** One catalogue object projected into a scene rect: the projection's result, never a re-derivation. */
data class SceneRow(
    val object0: SkyObject,
    /** The object's apparent horizontal direction at the frame's instant. */
    val position: HorizontalCoords,
    /** The projected position, from [SceneCamera.project]; always inside the horizontal window. */
    val point: ScenePoint,
    /** True when the drawn horizon stands above the object — the same test the frame's bodies use. */
    val occluded: Boolean,
    val isSelected: Boolean,
)

/**
 * Builds the Night screen's one immutable [SceneFrame] from the night's [NightComputation].
 *
 * **Projection is not re-derived here.** The frame's camera comes from `SceneCamera.of(...)` and every
 * screen position comes from `SceneCamera.project(...)`, both in `domain/SceneState.kt`. This object
 * computes *directions* — where each body is — and hands them to the domain's own geometry; it owns no
 * `wrap180`, no field-of-view arithmetic and no clamp of its own. Re-implementing the projection is the
 * defect the domain camera exists to prevent, because the spec's own formulas
 * (`x = W·(wrap180(az − A_c) + F_h/2)/F_h`, `y = H·(A_top − alt)/(A_top − A_bottom)`) then exist twice.
 *
 * **The frame is a pure function of its arguments.** [SceneScrub.instantUtc] is the only time input,
 * the ephemeris is passed in, and nothing reads a wall clock, a device location or the network. Two
 * calls with the same `(computation, scrub, lens, selectedTargetId, redMode)` therefore produce equal
 * frames, which is what the scrub's "a scrub frame does not recompute the night's ephemeris" rule and
 * the screen's memoisation both rest on: a scrub re-projects facts already in the computation and does
 * not re-run a single twilight, moon or target search.
 *
 * **Refraction is per use, as the temporal-geometry section requires.** Every *drawn* position here is
 * [AltitudeModel.REFRACTED] (the Moon, the Sun, the planets, a target, the galactic centre); the only
 * geometric call is the Sun's altitude that selects [SceneFrame.twilightBand], because the twilight
 * bands are defined on the Sun's geometric centre at −6/−12/−18° and a refracted value would put the
 * frame in the wrong band near a boundary.
 */
object SceneBuilder {

    /**
     * The Sun's apparent angular **diameter** in degrees: 32′ = 0.5333°.
     *
     * The same 16′ semidiameter the 90.8333° sunrise test uses (design.md "Time and geometry"), stated
     * as a diameter because [SceneBody.angularSizeDeg] is a diameter. It is a rendered size, not a
     * measurement of this instant — the Sun's true diameter varies ±1.7 % over the year and nothing on
     * the screen resolves that.
     */
    const val SUN_ANGULAR_SIZE_DEG: Double = 32.0 / 60.0

    /**
     * Sgr A*, the galactic centre, as a fixed J2000.0/ICRS direction in **degrees**.
     *
     * The Milky Way band's position is the ESO panorama's own baked transform; this one direction is
     * what [SceneFrame.milkyWayCorePosition] carries so the canvas can place the band's core and state
     * the scene credit beside it. It is a catalogue-shaped constant because `objects.json` carries no
     * row for it — if one ever lands there, that row should replace this constant.
     */
    const val GALACTIC_CENTRE_RA_DEG: Double = 266.4168
    const val GALACTIC_CENTRE_DEC_DEG: Double = -29.0078

    /**
     * The frame for one scrub position.
     *
     * @param computation the night, already computed; its `request` carries the spot, the horizon, the
     *   equipment and the catalogue rows, and its `skyGrade` is the darkness the frame reports.
     * @param scrub the position to draw, from `SceneScrub.forValue` / `SceneScrub.forInstant`.
     * @param lens the lens the frame's field of view is drawn from. The **canvas** field is twice the
     *   lens's own (`SceneCamera.CONTEXT_FACTOR = 2.0`), which `SceneCamera.of` applies.
     * @param ephemeris the engine; a parameter rather than a field so the frame is JVM-testable.
     * @param names the localized labels for the bodies the catalogue does not name.
     * @param selectedTargetId the centred target's catalogue id, or `null` for the horizon default.
     *   An id that matches no row is treated as no selection rather than as an error — a deleted
     *   target must not blank the scene.
     * @param trails the satellite trails already built from the pass history. They are **passed in**
     *   rather than derived here: a trail is the satellite's topocentric history, which is Orekit's
     *   propagation (`data/sky/SatellitePassEngine.kt`), and this builder has no propagator.
     * @param redMode whether the night-vision palette is active; the frame carries it so the draw
     *   reads it from the frame rather than from a screen-level flag.
     */
    fun build(
        computation: NightComputation,
        scrub: SceneScrub,
        lens: LensPreset,
        ephemeris: SkyEphemeris,
        names: SceneNames,
        selectedTargetId: String? = null,
        trails: List<ScenePassTrail> = emptyList(),
        redMode: Boolean = false,
    ): SceneFrame {
        val instantUtc: Instant = scrub.instantUtc
        val request = computation.request
        val point = request.point
        val horizon = computation.horizon

        val sunHorizontal = ephemeris.sunHorizontal(point, instantUtc, AltitudeModel.REFRACTED)
        val sunBody = SceneBody(
            id = SUN_ID,
            name = names.sun,
            type = ObjectType.SUN,
            position = sunHorizontal,
            magnitude = ephemeris.sunMagnitude(instantUtc),
            angularSizeDeg = SUN_ANGULAR_SIZE_DEG,
            kind = ObjectKind.COMPUTED,
            occludedByHorizon = occludedBy(horizon, sunHorizontal),
        )

        val moonState = ephemeris.moonState(point, instantUtc, AltitudeModel.REFRACTED)
        val moonBody = SceneBody(
            id = MOON_ID,
            name = names.moon,
            type = ObjectType.MOON,
            position = moonState.position,
            magnitude = moonState.magnitude,
            angularSizeDeg = ephemeris.moonAngularDiameterDeg(instantUtc),
            illuminatedFraction = moonState.illuminatedFraction,
            kind = ObjectKind.COMPUTED,
            occludedByHorizon = occludedBy(horizon, moonState.position),
        )

        // A planet is a magnitude-scaled disc, never a texture: no angular size is invented for it.
        val planetBodies = SolarSystemBody.entries.map { body ->
            val horizontal = ephemeris.planetHorizontal(body, point, instantUtc, AltitudeModel.REFRACTED)
            SceneBody(
                id = body.name,
                name = names.planets[body] ?: body.name,
                type = ObjectType.PLANET,
                position = horizontal,
                magnitude = ephemeris.planetMagnitude(body, instantUtc),
                angularSizeDeg = null,
                kind = ObjectKind.COMPUTED,
                occludedByHorizon = occludedBy(horizon, horizontal),
            )
        }

        val selectedTarget = selectedTargetId?.let { id -> targetBody(id, computation, instantUtc, ephemeris, horizon) }

        // The camera is the DOMAIN's: `of()` applies the 2× context margin, the F_v/2 − 4° horizon
        // centring and the [−10°, +90°] clamp; this builder passes a direction and nothing more.
        val camera = SceneCamera.of(lens, request.spot.latDeg, selectedTarget?.position)

        val milkyWayCore = ephemeris.fixedHorizontal(
            point,
            EquatorialCoords(GALACTIC_CENTRE_RA_DEG, GALACTIC_CENTRE_DEC_DEG),
            instantUtc,
            AltitudeModel.REFRACTED,
        )

        // One rotation for the stars and the band: the Milky Way follows the same rotation as the star
        // field (night-scene "The band follows the same rotation as the stars"), so the two values are
        // deliberately the same number and neither the canvas nor this builder may drift them apart.
        val skyRotationRad = Math.toRadians(ephemeris.localSiderealHours(point, instantUtc) * 15.0)

        return SceneFrame(
            instantUtc = instantUtc,
            spotId = request.spot.id,
            point = point,
            tzId = request.spot.tzId,
            camera = camera,
            lensFov = FieldOfView.of(lens),
            sun = sunBody,
            moon = moonBody,
            moonLibration = ephemeris.moonLibration(instantUtc),
            planetBodies = planetBodies,
            selectedTarget = selectedTarget,
            trails = trails,
            horizon = horizon,
            skyRotationRad = skyRotationRad,
            milkyWayRotationRad = skyRotationRad,
            milkyWayCorePosition = milkyWayCore,
            airMassAtCenter = HorizontalCoords.airMassAt(camera.centerAltitudeDeg),
            // Geometric on purpose: the band is defined on the Sun's geometric centre at −6/−12/−18°.
            twilightBand = twilightBandOf(
                ephemeris.sunHorizontal(point, instantUtc, AltitudeModel.GEOMETRIC).altitudeDeg,
            ),
            skyGrade = computation.skyGrade,
            redMode = redMode,
        )
    }

    /** The frame for a scrub **value** inside the computation's own session window. */
    fun buildAt(
        computation: NightComputation,
        scrubValue: Double,
        lens: LensPreset,
        ephemeris: SkyEphemeris,
        names: SceneNames,
        selectedTargetId: String? = null,
        trails: List<ScenePassTrail> = emptyList(),
        redMode: Boolean = false,
    ): SceneFrame = build(
        computation = computation,
        scrub = SceneScrub.forValue(scrubValue, computation.session),
        lens = lens,
        ephemeris = ephemeris,
        names = names,
        selectedTargetId = selectedTargetId,
        trails = trails,
        redMode = redMode,
    )

    /**
     * Every catalogue row the frame's window contains, projected by [SceneCamera.project].
     *
     * `project` returns `null` for a body **outside the horizontal window** — a body behind the viewer
     * must not be clamped to an edge, because that would put the user's western ridge on the wrong side
     * — so this list is exactly the rows the scene draws, and a row that is merely below the rect is
     * still included because `project` clamps it vertically (which is what lets the horizon layer draw
     * a set Moon as a cut silhouette rather than removing it).
     */
    fun rows(
        frame: SceneFrame,
        objects: List<SkyObject>,
        ephemeris: SkyEphemeris,
    ): List<SceneRow> {
        val out = ArrayList<SceneRow>(objects.size)
        for (object0 in objects) {
            val horizontal = ephemeris.fixedHorizontal(
                frame.point,
                object0.equatorial,
                frame.instantUtc,
                AltitudeModel.REFRACTED,
            )
            val point = frame.camera.project(horizontal) ?: continue
            out.add(
                SceneRow(
                    object0 = object0,
                    position = horizontal,
                    point = point,
                    occluded = occludedBy(frame.horizon, horizontal),
                    isSelected = frame.selectedTarget?.id == object0.id,
                ),
            )
        }
        return out
    }

    /**
     * The band the Sun is inside: the **deepest** threshold it is at or below, or `null` in daylight.
     *
     * The rule is the twilight test's own — geometric centre, no refraction, no semidiameter — so a
     * frame taken at a boundary is in the same band the brief's twilight table calls it.
     */
    fun twilightBandOf(sunGeometricAltitudeDeg: Double): TwilightKind? = when {
        sunGeometricAltitudeDeg <= TwilightKind.ASTRONOMICAL.angleDeg -> TwilightKind.ASTRONOMICAL
        sunGeometricAltitudeDeg <= TwilightKind.NAUTICAL.angleDeg -> TwilightKind.NAUTICAL
        sunGeometricAltitudeDeg <= TwilightKind.CIVIL.angleDeg -> TwilightKind.CIVIL
        else -> null
    }

    /**
     * True when the drawn horizon stands above a body.
     *
     * An absent or flat profile occludes nothing above 0°: the spec's flat-horizon case is a horizon,
     * not an absence, and it must not hide the sky. The test is the domain's own
     * (`HorizonProfile.altitudeAt` interpolates and wraps), spelled in one place so the frame's bodies
     * and the projected rows cannot disagree about what is hidden.
     */
    fun occludedBy(horizon: HorizonProfile?, position: HorizontalCoords): Boolean {
        val ridgeDeg = horizon?.altitudeAt(position.azimuthDeg) ?: 0.0
        return position.altitudeDeg < ridgeDeg
    }

    private const val SUN_ID: String = "sun"
    private const val MOON_ID: String = "moon"

    /** One catalogue row as a [SceneBody], or `null` when the id matches nothing. */
    private fun targetBody(
        id: String,
        computation: NightComputation,
        instantUtc: Instant,
        ephemeris: SkyEphemeris,
        horizon: HorizonProfile?,
    ): SceneBody? {
        val target = computation.objects.firstOrNull { it.id == id } ?: return null
        val horizontal = ephemeris.fixedHorizontal(
            computation.request.point,
            target.equatorial,
            instantUtc,
            AltitudeModel.REFRACTED,
        )
        return SceneBody(
            id = target.id,
            name = target.name,
            type = target.type,
            position = horizontal,
            magnitude = target.displayMagnitude,
            angularSizeDeg = target.angularSizeMajDeg,
            kind = target.kind,
            occludedByHorizon = occludedBy(horizon, horizontal),
        )
    }
}
