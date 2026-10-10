package com.gexetr.lumintralightplanner.domain

/**
 * A lens and sensor, in **millimetres everywhere**.
 *
 * Focal length and sensor dimensions are never converted between unit systems — not for an imperial
 * display setting, not for a label. A converted focal length would silently change every field-of-view
 * number the app states, which is why the rule is written on the type.
 */
data class LensPreset(
    val name: String,
    val sensorWidthMm: Double,
    val sensorHeightMm: Double,
    val focalLengthMm: Double,
    /** Optional pixel pitch in micrometres, used only for the "pixels on sensor" figure. */
    val pixelPitchUm: Double? = null,
) {
    init {
        require(sensorWidthMm > 0.0 && sensorHeightMm > 0.0) { "sensor extents must be positive" }
        require(focalLengthMm > 0.0) { "focal length must be positive" }
    }

    /** The sensor's shorter side: what a target's major axis is compared against. */
    val sensorShortSideMm: Double get() = minOf(sensorWidthMm, sensorHeightMm)

    val sensorDiagonalMm: Double
        get() = Math.hypot(sensorWidthMm, sensorHeightMm)

    /**
     * The lens's crop factor against 36×24 mm, the one ratio photographers expect to see.
     */
    val cropFactor: Double get() = Math.hypot(36.0, 24.0) / sensorDiagonalMm
}

/** A frame's angular extent in degrees, on each axis and across the diagonal. */
data class FieldOfView(
    val fovH: Double,
    val fovV: Double,
    val fovDiag: Double,
) {
    /** The shorter side's angular extent: what the fit statement compares a target against. */
    val shortSideDeg: Double get() = minOf(fovH, fovV)

    val longSideDeg: Double get() = maxOf(fovH, fovV)

    companion object {
        /**
         * `FOV_axis = 2·atan(d_axis / (2·f))`, with `d` the sensor extent and `f` the focal length, both
         * in millimetres. This is the whole computation — no unit conversion, no fudge factor.
         */
        fun of(lens: LensPreset): FieldOfView = FieldOfView(
            fovH = axisFovDeg(lens.sensorWidthMm, lens.focalLengthMm),
            fovV = axisFovDeg(lens.sensorHeightMm, lens.focalLengthMm),
            fovDiag = axisFovDeg(lens.sensorDiagonalMm, lens.focalLengthMm),
        )

        fun axisFovDeg(sensorExtentMm: Double, focalLengthMm: Double): Double =
            2.0 * Math.toDegrees(Math.atan(sensorExtentMm / (2.0 * focalLengthMm)))
    }
}

/** The three-way verdict the fit statement carries, each a distinct sentence. */
enum class FitVerdict {
    /** Major axis at most 0.8 of the frame's short side. */
    FITS,

    /** Major axis more than 0.8 and at most 1.0 of the frame's short side. */
    MARGINAL,

    /** Major axis more than 1.0 of the frame's short side. */
    TOO_LARGE,

    /** No stated apparent size, so no fit statement is produced and none is invented. */
    UNKNOWN_SIZE,
}

/**
 * The fit statement's numbers, all of them shown.
 *
 * A magnitude never produces a fit or a "you will see it" claim, and a star is never drawn as a disc
 * sized from its magnitude.
 */
data class TargetFit(
    val objectAngularSizeDeg: Double?,
    val frameFovShortSideDeg: Double,
    /** Major axis over the frame's short side. `null` when the size is unknown. */
    val frameFrac: Double?,
    val verdict: FitVerdict,
    val pixelsOnSensor: Double?,
) {
    companion object {
        /** At most this fraction of the short side is a comfortable fit. */
        const val FITS_MAX: Double = 0.8

        /** Above this fraction the target does not fit at all. */
        const val MARGINAL_MAX: Double = 1.0

        /**
         * Builds the statement from the object's major axis and the lens.
         *
         * `frameFrac = (sizeMajArcmin / 60) / frameFovShortSideDeg`; the verdict is the three-way split
         * at 0.8 and 1.0. A target with no apparent size yields [FitVerdict.UNKNOWN_SIZE] and no
         * numbers, rather than a fabricated one.
         */
        fun of(sizeMajArcmin: Double?, lens: LensPreset): TargetFit {
            val fov = FieldOfView.of(lens)
            val shortSide = fov.shortSideDeg
            if (sizeMajArcmin == null || sizeMajArcmin <= 0.0) {
                return TargetFit(
                    objectAngularSizeDeg = null,
                    frameFovShortSideDeg = shortSide,
                    frameFrac = null,
                    verdict = FitVerdict.UNKNOWN_SIZE,
                    pixelsOnSensor = null,
                )
            }
            val objectDeg = sizeMajArcmin / 60.0
            val frac = objectDeg / shortSide
            val verdict = when {
                frac <= FITS_MAX -> FitVerdict.FITS
                frac <= MARGINAL_MAX -> FitVerdict.MARGINAL
                else -> FitVerdict.TOO_LARGE
            }
            val pixels = lens.pixelPitchUm?.let { pitchUm ->
                val shortSidePixels = lens.sensorShortSideMm * 1000.0 / pitchUm
                frac * shortSidePixels
            }
            return TargetFit(
                objectAngularSizeDeg = objectDeg,
                frameFovShortSideDeg = shortSide,
                frameFrac = frac,
                verdict = verdict,
                pixelsOnSensor = pixels,
            )
        }
    }
}

// EquipmentProfile lives in Scout.kt: it describes a spot's equipment, and the spec's domain layout
// puts it beside the spot rather than beside the lens arithmetic.
