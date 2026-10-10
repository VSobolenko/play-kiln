package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant

/**
 * Where an element set's age falls. These bands are **product choices, not measured constants**, and
 * the tighter 72-hour refusal is deliberate: the penalty for a wrong minute (a missed pass) is
 * asymmetric, the app advertises offline use, and the same propagator diverges 27,924 km at ~21.3 days.
 */
enum class ElementSetAgeBand {
    /** `age ≤ 24 h`: compute, and show no age badge. */
    FRESH,

    /** `24 h < age ≤ 72 h`: compute, and show the age prominently beside every prediction. */
    AGING,

    /** `age > 72 h`: refuse to compute any pass time and offer a refresh. */
    EXPIRED,
}

/**
 * The age of an element set relative to the session instant, with its band.
 *
 * Surfaced next to every prediction, because a pass time computed from a stale set looks exactly like a
 * correct one.
 */
data class ElementSetAge(
    val age: Duration,
    val band: ElementSetAgeBand,
) {
    val ageHours: Double get() = age.toMinutes() / 60.0

    val ageDays: Double get() = ageHours / 24.0

    /** False for [ElementSetAgeBand.EXPIRED]: no pass time may be computed from this set. */
    val mayCompute: Boolean get() = band != ElementSetAgeBand.EXPIRED

    /** True once the age is worth stating beside the prediction. */
    val showBadge: Boolean get() = band != ElementSetAgeBand.FRESH

    companion object {
        val FRESH_MAX: Duration = Duration.ofHours(24)
        val EXPIRED_AFTER: Duration = Duration.ofHours(72)

        fun from(epochUtc: Instant, atUtc: Instant): ElementSetAge {
            val raw = Duration.between(epochUtc, atUtc)
            val age = if (raw.isNegative) Duration.ZERO else raw
            val band = when {
                age <= FRESH_MAX -> ElementSetAgeBand.FRESH
                age <= EXPIRED_AFTER -> ElementSetAgeBand.AGING
                else -> ElementSetAgeBand.EXPIRED
            }
            return ElementSetAge(age, band)
        }
    }
}

/**
 * Why a geometrically real pass is or is not usable.
 *
 * The two-state output (`occurs` vs `observable`) exists so a pass in the Earth's shadow is **stated**
 * rather than silently dropped from the list.
 */
enum class PassVisibility {
    /** The satellite is sunlit and the observer's sky is dark enough at the pass instant. */
    OBSERVABLE,

    /** The satellite is inside the Earth's umbra: it is not illuminated, so it cannot be seen. */
    IN_EARTH_SHADOW,

    /** The observer's Sun is too high: the sky is too bright for the pass to be seen. */
    TOO_BRIGHT,
}

/** One point of the sub-satellite ground track, for the pass's map-free track drawing. */
data class GroundTrackPoint(
    val instantUtc: Instant,
    val latDeg: Double,
    val lonDeg: Double,
    val altitudeKm: Double,
)

/** How much the app is willing to claim about whether a pass will cross the lens frame. */
enum class FrameCrossingConfidence {
    /** The closest approach is well inside the frame and the pass is near and bright. */
    HIGH,

    /** The geometry is inside the frame but the approach is grazing. */
    MEDIUM,

    /** The pass is far or fast: the frame question is not answerable from an element set. */
    LOW,

    /** No lens is configured, or the pass is too far to say anything. */
    UNKNOWN,
}

/**
 * Whether the pass is predicted to cross the user's lens frame, and how confident that prediction is.
 *
 * The confidence is carried because an element set's cross-track error grows with age, and a confident
 * "it will cross your frame" from a two-day-old set would be a claim the data cannot support.
 */
data class FrameCrossing(
    val crosses: Boolean,
    /** The minimum angular separation from the frame's centre, in degrees, when it is computable. */
    val closestApproachDeg: Double?,
    val confidence: FrameCrossingConfidence,
) {
    companion object {
        /** No lens configured: no claim at all. */
        val UNKNOWN = FrameCrossing(false, null, FrameCrossingConfidence.UNKNOWN)
    }
}

/**
 * One satellite pass, with every number the readout shows.
 *
 * `occurred` and `observable` are two separate booleans on purpose: a pass that happened but was in the
 * Earth's shadow is a fact the user can learn from, and dropping it silently is the defect this type
 * prevents.
 */
data class SatellitePass(
    val noradId: Long,
    val satelliteName: String,
    val objectId: String?,
    val aosUtc: Instant,
    val losUtc: Instant,
    val maxElevationDeg: Double,
    val maxElevationUtc: Instant,
    val subPointTrack: List<GroundTrackPoint>,
    val visibility: PassVisibility,
    /** The observer's Sun geometric altitude at the pass instant, degrees. */
    val observerSunAltitudeDeg: Double,
    val satelliteSunlit: Boolean,
    /** The element set this prediction came from. */
    val elementSetEpochUtc: Instant,
    val elementSetAge: ElementSetAge,
    val frameCrossing: FrameCrossing = FrameCrossing.UNKNOWN,
) {
    init {
        require(!losUtc.isBefore(aosUtc)) { "pass LOS $losUtc is before AOS $aosUtc" }
    }

    val duration: Duration get() = Duration.between(aosUtc, losUtc)

    val span: NightSpan get() = NightSpan(aosUtc, losUtc)

    /** A geometric pass exists: the satellite really did come above the user's elevation mask. */
    val occurred: Boolean get() = true

    /** A pass the user can actually watch: sunlit satellite, dark enough sky. */
    val observable: Boolean get() = visibility == PassVisibility.OBSERVABLE

    /** The pass is worth showing even when unobservable, so its state is stated rather than implied. */
    val hasStatedState: Boolean get() = true

    companion object {
        /** Default elevation mask, adjustable 0–45°. */
        const val DEFAULT_MASK_DEG: Double = 10.0
        const val MIN_MASK_DEG: Double = 0.0
        const val MAX_MASK_DEG: Double = 45.0

        /**
         * The dark-enough threshold: the observer's **geometric** Sun altitude must be at or below civil
         * twilight. The app states which threshold it used; −6.0° is the default.
         */
        const val DEFAULT_SUN_THRESHOLD_DEG: Double = -6.0

        /**
         * Classifies one pass. The order matters: an umbral pass is stated as such even when the sky is
         * also bright, because the shadow is the more specific reason.
         */
        fun visibilityOf(
            satelliteSunlit: Boolean,
            observerSunAltitudeDeg: Double,
            sunThresholdDeg: Double = DEFAULT_SUN_THRESHOLD_DEG,
        ): PassVisibility = when {
            !satelliteSunlit -> PassVisibility.IN_EARTH_SHADOW
            observerSunAltitudeDeg > sunThresholdDeg -> PassVisibility.TOO_BRIGHT
            else -> PassVisibility.OBSERVABLE
        }
    }
}
