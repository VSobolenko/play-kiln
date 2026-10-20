package com.gexetr.plinthproductphotos.domain.coach

import kotlin.math.abs

enum class CoachCheck {
    SHARPNESS,
    EXPOSURE,
    FILL,
    ASPECT,
}

enum class CheckVerdict {
    PASS,
    NEEDS_ATTENTION,
    UNVERIFIED,
}

enum class CoachRemedy {
    STEADY_AND_FOCUS,
    ADJUST_LIGHT_AND_EXPOSURE,
    MOVE_CLOSER_WITHOUT_CROPPING,
    REPOSITION_TO_MATCH_FRAME,
}

enum class MeasurementState {
    MEASURED,
    NO_MEASUREMENT,
    NO_THRESHOLD,
    NO_MEASUREMENT_OR_THRESHOLD,
    NO_SUBJECT_BOUNDS,
    NO_PRESET_ASPECT,
    INVALID_PRESET_ASPECT,
}

data class CoachQualityInput(
    val width: Int,
    val height: Int,
    val presetAspect: String?,
    val sharpnessScore: Double? = null,
    val minimumSharpnessScore: Double? = null,
    val clippedExposureFraction: Double? = null,
    val maximumClippedExposureFraction: Double? = null,
    val itemFillRatio: Double? = null,
    val minimumFillRatio: Double? = null,
)

data class CoachQualityResult(
    val check: CoachCheck,
    val verdict: CheckVerdict,
    val measurementState: MeasurementState,
    val remedy: CoachRemedy,
)

object CoachQuality {
    private const val ASPECT_TOLERANCE = 0.01

    fun evaluate(input: CoachQualityInput): List<CoachQualityResult> {
        require(input.width > 0 && input.height > 0) { "Frame dimensions must be positive" }
        return listOf(
            thresholdCheck(
                check = CoachCheck.SHARPNESS,
                measurement = input.sharpnessScore,
                threshold = input.minimumSharpnessScore,
                passes = { score, minimum -> score >= minimum },
                remedy = CoachRemedy.STEADY_AND_FOCUS,
            ),
            thresholdCheck(
                check = CoachCheck.EXPOSURE,
                measurement = input.clippedExposureFraction,
                threshold = input.maximumClippedExposureFraction,
                passes = { clipped, maximum -> clipped <= maximum },
                remedy = CoachRemedy.ADJUST_LIGHT_AND_EXPOSURE,
            ),
            fillCheck(input.itemFillRatio, input.minimumFillRatio),
            aspectCheck(input.width, input.height, input.presetAspect),
        )
    }

    private fun thresholdCheck(
        check: CoachCheck,
        measurement: Double?,
        threshold: Double?,
        passes: (Double, Double) -> Boolean,
        remedy: CoachRemedy,
    ): CoachQualityResult {
        if (measurement == null || threshold == null) {
            val missing = when {
                measurement == null && threshold == null -> MeasurementState.NO_MEASUREMENT_OR_THRESHOLD
                measurement == null -> MeasurementState.NO_MEASUREMENT
                else -> MeasurementState.NO_THRESHOLD
            }
            return CoachQualityResult(check, CheckVerdict.UNVERIFIED, missing, remedy)
        }
        require(measurement.isFinite() && threshold.isFinite()) { "Quality measurements must be finite" }
        return CoachQualityResult(
            check = check,
            verdict = if (passes(measurement, threshold)) CheckVerdict.PASS else CheckVerdict.NEEDS_ATTENTION,
            measurementState = MeasurementState.MEASURED,
            remedy = remedy,
        )
    }

    private fun fillCheck(fill: Double?, minimum: Double?): CoachQualityResult {
        if (minimum == null) {
            return CoachQualityResult(
                CoachCheck.FILL,
                CheckVerdict.UNVERIFIED,
                MeasurementState.NO_THRESHOLD,
                CoachRemedy.MOVE_CLOSER_WITHOUT_CROPPING,
            )
        }
        require(minimum.isFinite() && minimum in 0.0..1.0) { "Minimum fill ratio must be between 0 and 1" }
        if (fill == null) {
            return CoachQualityResult(
                CoachCheck.FILL,
                CheckVerdict.UNVERIFIED,
                MeasurementState.NO_SUBJECT_BOUNDS,
                CoachRemedy.MOVE_CLOSER_WITHOUT_CROPPING,
            )
        }
        require(fill.isFinite() && fill in 0.0..1.0) { "Item fill ratio must be between 0 and 1" }
        return CoachQualityResult(
            CoachCheck.FILL,
            if (fill >= minimum) CheckVerdict.PASS else CheckVerdict.NEEDS_ATTENTION,
            MeasurementState.MEASURED,
            CoachRemedy.MOVE_CLOSER_WITHOUT_CROPPING,
        )
    }

    private fun aspectCheck(width: Int, height: Int, aspect: String?): CoachQualityResult {
        if (aspect == null) {
            return CoachQualityResult(
                CoachCheck.ASPECT,
                CheckVerdict.PASS,
                MeasurementState.NO_PRESET_ASPECT,
                CoachRemedy.REPOSITION_TO_MATCH_FRAME,
            )
        }
        val parts = aspect.split(':')
        val numerator = parts.getOrNull(0)?.toDoubleOrNull()
        val denominator = parts.getOrNull(1)?.toDoubleOrNull()
        if (parts.size != 2 || numerator == null || denominator == null ||
            numerator <= 0.0 || denominator <= 0.0
        ) {
            return CoachQualityResult(
                CoachCheck.ASPECT,
                CheckVerdict.UNVERIFIED,
                MeasurementState.INVALID_PRESET_ASPECT,
                CoachRemedy.REPOSITION_TO_MATCH_FRAME,
            )
        }
        val expected = numerator / denominator
        val actual = width.toDouble() / height
        return CoachQualityResult(
            CoachCheck.ASPECT,
            if (abs(actual - expected) <= ASPECT_TOLERANCE) CheckVerdict.PASS else CheckVerdict.NEEDS_ATTENTION,
            MeasurementState.MEASURED,
            CoachRemedy.REPOSITION_TO_MATCH_FRAME,
        )
    }
}

object LevelGuide {
    const val DEAD_ZONE_DEGREES = 2.0

    fun isLevel(degreesFromLevel: Double, deadZoneDegrees: Double = DEAD_ZONE_DEGREES): Boolean {
        require(degreesFromLevel.isFinite() && deadZoneDegrees.isFinite() && deadZoneDegrees >= 0.0)
        return abs(degreesFromLevel) <= deadZoneDegrees
    }
}
