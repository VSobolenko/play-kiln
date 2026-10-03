package com.gexetr.laneset.domain

import kotlin.math.roundToInt

/**
 * The threshold pace arithmetic, in one place.
 *
 * A swimmer's two time trials are converted to **one** threshold pace per 100 m, and every interval
 * target in the app is derived from that single number. The formula is the Critical Swim Speed (CSS)
 * relationship (Ginn, 1993), which is the standard field test a coach uses when there is no lactate
 * data:
 *
 * ```
 * CSS (metres per second)      = (400 - 200) / (t400 - t200)
 * threshold pace per 100 m (s) = 100 / CSS
 *                              = (t400 - t200) / 2
 * ```
 *
 * The two distances slope the swimmer's speed-versus-duration line, so the *difference* of the two
 * times carries the threshold and the 400 m time alone does not. The result is the pace the swimmer
 * can hold for a long main set rather than a sprint or an easy swim; the generator applies explicit
 * per-focus factors (0.90–1.25) on top of it, and those factors are its decision, not this one's.
 *
 * Every function here is pure and free of Android types, so it is exercised by the JVM unit tests
 * without a device.
 */
object PaceModel {

    /**
     * Threshold pace per 100 m, in seconds, from a 400 m and a 200 m time trial.
     *
     * Both times are seconds for the whole trial ([timeTrial400Seconds] for 400 m,
     * [timeTrial200Seconds] for 200 m). The 400 m must be the slower swim, which is true for any
     * swimmer who paced the trials sensibly; an impossible pair throws rather than returning a
     * negative pace, because a silently wrong target would print on every interval.
     */
    fun thresholdPacePer100(timeTrial400Seconds: Double, timeTrial200Seconds: Double): Double {
        require(timeTrial200Seconds > 0.0) {
            "the 200 m time trial must be positive, was $timeTrial200Seconds s"
        }
        require(timeTrial400Seconds > timeTrial200Seconds) {
            "the 400 m time trial must be slower than the 200 m one, was " +
                "$timeTrial400Seconds s against $timeTrial200Seconds s"
        }
        return (timeTrial400Seconds - timeTrial200Seconds) / 2.0
    }

    /**
     * The time to swim one interval of [distanceMeters] at [pacePer100Seconds] per 100 m.
     *
     * The result is whole seconds on purpose: a pool pace clock is read in whole seconds, and a
     * target a swimmer cannot see on the clock is not a target. A fractional second rounds to the
     * nearest whole second, and the value is never zero even for a token distance.
     */
    fun intervalTargetSeconds(pacePer100Seconds: Double, distanceMeters: Int): Int {
        require(pacePer100Seconds > 0.0) {
            "pacePer100Seconds must be positive, was $pacePer100Seconds s"
        }
        require(distanceMeters > 0) {
            "distanceMeters must be positive, was $distanceMeters"
        }
        return (pacePer100Seconds * distanceMeters / 100.0).roundToInt().coerceAtLeast(1)
    }
}
