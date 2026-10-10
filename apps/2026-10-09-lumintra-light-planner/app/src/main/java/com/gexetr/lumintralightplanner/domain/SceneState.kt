package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant

/** A projected position inside the scene rect, as fractions of its width and height in `[0, 1]`. */
data class ScenePoint(
    val xFraction: Float,
    val yFraction: Float,
)

/** The Moon's libration, so the rendered disc shows the same face the sky does. */
data class MoonLibration(
    val librationLatDeg: Double,
    val librationLonDeg: Double,
)

/**
 * One body the scene draws: a catalogue target, a planet, the Sun or the Moon.
 *
 * [occludedByHorizon] is computed here rather than inside the draw, so the draw is a pure function of
 * the frame and the horizon line's occlusion is a stated fact rather than a rendering side effect.
 */
data class SceneBody(
    val id: String,
    val name: String,
    val type: ObjectType,
    val position: HorizontalCoords,
    val magnitude: Double? = null,
    val angularSizeDeg: Double? = null,
    /** For the Moon and the planets: how much of the disc is lit, `0.0…1.0`. */
    val illuminatedFraction: Double? = null,
    val kind: ObjectKind = ObjectKind.FIXED,
    val occludedByHorizon: Boolean = false,
) {
    val isPointSource: Boolean get() = angularSizeDeg == null || angularSizeDeg <= 0.0
}

/** One sampled point of a pass trail, already in horizontal coordinates. */
data class SceneTrailPoint(
    val instantUtc: Instant,
    val position: HorizontalCoords,
)

/**
 * One satellite trail, split at horizon crossings and carrying its own honesty: a stale element set is
 * named on the trail rather than only in a list far away.
 */
data class ScenePassTrail(
    val noradId: Long,
    val satelliteName: String,
    val visibility: PassVisibility,
    val elementSetAge: ElementSetAge,
    val segments: List<List<SceneTrailPoint>>,
) {
    /** True when the trail must state its element set's age beside the streak. */
    val mustStateAge: Boolean get() = elementSetAge.showBadge
}

/**
 * The scene's camera: which direction the rect is centred on, and how wide it is.
 *
 * [fovH] and [fovV] are the **canvas** fields, which are `2.0 ×` the lens's own — the 50 % context
 * margin the scene capability requires on each side. The lens's own frame is drawn inside as a
 * hairline rectangle.
 */
data class SceneCamera(
    val centerAzimuthDeg: Double,
    val centerAltitudeDeg: Double,
    val fovH: Double,
    val fovV: Double,
    /** False once the user has centred on a target; a named control restores the default. */
    val isDefaultCentring: Boolean = true,
) {
    /** Clamped at the zenith: the window never asks for altitudes above +90°. */
    val topAltitudeDeg: Double get() = minOf(centerAltitudeDeg + fovV / 2.0, 90.0)

    val bottomAltitudeDeg: Double get() = centerAltitudeDeg - fovV / 2.0

    /**
     * Projects a sky position into the scene rect.
     *
     * Returns `null` when the position is **horizontally outside the window** — a body behind the
     * viewer must not project onto the screen, and clamping it to an edge would draw the user's western
     * ridge on the wrong side. Vertically the result is clamped, which is what lets a body below the
     * rect still be drawn as an occluded silhouette by the horizon layer.
     */
    fun project(position: HorizontalCoords): ScenePoint? {
        val dAz = wrap180(position.azimuthDeg - centerAzimuthDeg)
        if (dAz < -fovH / 2.0 || dAz > fovH / 2.0) return null
        val x = (dAz + fovH / 2.0) / fovH
        val span = topAltitudeDeg - bottomAltitudeDeg
        if (span <= 0.0) return null
        val yRaw = (topAltitudeDeg - position.altitudeDeg) / span
        val y = yRaw.coerceIn(0.0, 1.0)
        return ScenePoint(x.toFloat(), y.toFloat())
    }

    companion object {
        /** The context margin: the canvas is twice the lens field on each axis. */
        const val CONTEXT_FACTOR: Double = 2.0

        /** The horizon sits this far above the scene rect's lower edge in the default centring. */
        const val HORIZON_OFFSET_DEG: Double = 4.0

        /** `wrap180()` maps an azimuth difference into `[−180°, +180°)`. */
        fun wrap180(deg: Double): Double {
            var d = deg % 360.0
            if (d >= 180.0) d -= 360.0
            if (d < -180.0) d += 360.0
            return d
        }

        /**
         * The default camera for a spot, and the camera after a target is selected.
         *
         * With **no target**, the centre altitude is `F_v/2 − 4°` (so the horizon falls 4° above the
         * rect's lower edge) and the centre azimuth is **180° north of the equator, 0° south of it** —
         * the antisolar meridian at local midnight. With a target, the centre moves to its direction,
         * clamped so the window stays inside `altitude ∈ [−10°, +90°]`.
         */
        fun of(
            lens: LensPreset,
            latitudeDeg: Double,
            targetPosition: HorizontalCoords? = null,
        ): SceneCamera {
            val lensFov = FieldOfView.of(lens)
            val fovH = lensFov.fovH * CONTEXT_FACTOR
            val fovV = lensFov.fovV * CONTEXT_FACTOR
            if (targetPosition == null) {
                return SceneCamera(
                    centerAzimuthDeg = if (latitudeDeg >= 0.0) 180.0 else 0.0,
                    centerAltitudeDeg = fovV / 2.0 - HORIZON_OFFSET_DEG,
                    fovH = fovH,
                    fovV = fovV,
                    isDefaultCentring = true,
                )
            }
            val desiredCenter = targetPosition.altitudeDeg
            val minCenter = MIN_ALTITUDE_DEG + fovV / 2.0
            val maxCenter = MAX_ALTITUDE_DEG - fovV / 2.0
            val center = if (minCenter <= maxCenter) desiredCenter.coerceIn(minCenter, maxCenter)
            else (MIN_ALTITUDE_DEG + MAX_ALTITUDE_DEG) / 2.0
            return SceneCamera(
                centerAzimuthDeg = HorizontalCoords.normaliseAzimuth(targetPosition.azimuthDeg),
                centerAltitudeDeg = center,
                fovH = fovH,
                fovV = fovV,
                isDefaultCentring = false,
            )
        }

        /** The canvas window stays inside this altitude band. */
        const val MIN_ALTITUDE_DEG: Double = -10.0
        const val MAX_ALTITUDE_DEG: Double = 90.0
    }
}

/**
 * The immutable per-instant payload the scene canvas consumes.
 *
 * Every layer's input is a field here, and nothing else is read inside the draw — no wall clock, no
 * device location, no network state — so two renders of the same frame at the same viewport size
 * produce identical pixels.
 */
data class SceneFrame(
    val instantUtc: Instant,
    val spotId: String,
    val point: GeodeticPoint,
    val tzId: String,
    val camera: SceneCamera,
    /** The lens's own field of view, drawn as the hairline frame inside the canvas. */
    val lensFov: FieldOfView,
    /** Layer 1 and 9 input: the Sun, whose altitude is the LUT axis. */
    val sun: SceneBody,
    /** Layer 6 input. */
    val moon: SceneBody,
    val moonLibration: MoonLibration? = null,
    val planetBodies: List<SceneBody> = emptyList(),
    val selectedTarget: SceneBody? = null,
    val trails: List<ScenePassTrail> = emptyList(),
    /** Layer 10 input; `null` means no profile has been drawn, which is drawn as a flat horizon. */
    val horizon: HorizonProfile? = null,
    /** Layer 3 and 4 input: the sky's rotation for this instant and place. */
    val skyRotationRad: Double,
    val milkyWayRotationRad: Double,
    val milkyWayCorePosition: HorizontalCoords,
    /** Air mass at the frame's centre, for the instrument readout. */
    val airMassAtCenter: Double,
    /** The band the Sun is currently inside, or `null` when the Sun is above the horizon. */
    val twilightBand: TwilightKind?,
    val skyGrade: SkyGrade,
    val redMode: Boolean,
) {
    /**
     * ε: the solar altitude the twilight LUT is indexed by. Named because the scene capability speaks
     * of the sky being "evaluated from ε" and an un-named altitude is easy to substitute.
     */
    val epsilonDeg: Double get() = sun.position.altitudeDeg

    /** True when a target is centred, which is when the "back to the horizon" control is offered. */
    val hasSelectedTarget: Boolean get() = selectedTarget != null

    /** The azimuth the readout states, so the viewing direction is never inferred from layout. */
    val viewingAzimuthDeg: Double get() = camera.centerAzimuthDeg

    /** Trails that must state the age of their element set where the streak is drawn. */
    val trailsNeedingAge: List<ScenePassTrail> get() = trails.filter { it.mustStateAge }
}

/**
 * The scrub position: the value in `0…1` and the absolute instant it resolves to.
 *
 * It is a **read-only** value inside the draw lambda — the draw reads it and cannot write it, which is
 * what keeps a scrub from feeding back into the frame it is computing.
 */
data class SceneScrub(
    val value: Double,
    val instantUtc: Instant,
) {
    init {
        require(value in 0.0..1.0) { "scrub value $value outside [0, 1]" }
    }

    companion object {
        /** The instant a scrub value resolves to inside a session window. */
        fun forValue(value: Double, span: NightSpan): SceneScrub {
            val clamped = value.coerceIn(0.0, 1.0)
            val millis = (span.duration.toMillis() * clamped).toLong()
            return SceneScrub(clamped, span.startUtc.plusMillis(millis))
        }

        /** The scrub value that resolves to an instant inside a session window. */
        fun forInstant(instantUtc: Instant, span: NightSpan): SceneScrub {
            val total = span.duration.toMillis()
            if (total <= 0L) return SceneScrub(0.0, span.startUtc)
            val offset = Duration.between(span.startUtc, instantUtc).toMillis().coerceIn(0L, total)
            return SceneScrub((offset.toDouble() / total).coerceIn(0.0, 1.0), span.startUtc.plusMillis(offset))
        }
    }
}
