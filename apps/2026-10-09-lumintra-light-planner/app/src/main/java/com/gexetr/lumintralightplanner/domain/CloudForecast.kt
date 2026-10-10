package com.gexetr.lumintralightplanner.domain

import java.time.Duration
import java.time.Instant

/**
 * One hour of the forecast, carrying **total** cloud only.
 *
 * There is deliberately no low/mid/high field: `api.met.no`'s compact forecast exposes them, but the
 * app states one number and the copy says *forecast*, so a three-way split would invite a precision the
 * product does not claim.
 */
data class CloudHour(
    val hourUtc: Instant,
    /** Total cloud area fraction as a percentage, `0…100`. */
    val cloudCoverPercent: Int,
) {
    init {
        require(cloudCoverPercent in 0..100) { "cloudCoverPercent $cloudCoverPercent outside 0..100" }
    }

    /** The fraction form, for an arithmetic that needs `0.0…1.0`. */
    val cloudFraction: Double get() = cloudCoverPercent / 100.0
}

/**
 * How old a fetched value is, with the instant it was fetched.
 *
 * Every external input the brief shows carries one of these, and the wording never turns a forecast
 * into a measurement.
 */
data class ForecastAge(
    val fetchedAtUtc: Instant,
    val age: Duration,
) {
    val ageHours: Double get() = age.toMinutes() / 60.0

    /**
     * Past this age the value is shown as stale rather than current. Six hours is chosen because the
     * source publishes a new run roughly every hour to six hours; beyond it the number is not a
     * today's-forecast any more.
     */
    val isStale: Boolean get() = age > STALE_AFTER

    companion object {
        val STALE_AFTER: Duration = Duration.ofHours(6)

        fun from(fetchedAtUtc: Instant, nowUtc: Instant): ForecastAge {
            val age = Duration.between(fetchedAtUtc, nowUtc)
            return ForecastAge(fetchedAtUtc, if (age.isNegative) Duration.ZERO else age)
        }
    }
}

/**
 * The cloud outlook for one night: hourly totals plus the age of the reading they came from.
 *
 * The source label is a constant of the type because there is exactly one source; a second source would
 * be a different decision, not a variation of this one.
 */
data class CloudReport(
    val hours: List<CloudHour>,
    val age: ForecastAge,
) {
    /** The hours that fall inside [span], in order. */
    fun hoursWithin(span: NightSpan): List<CloudHour> =
        hours.filter { !it.hourUtc.isBefore(span.startUtc) && it.hourUtc.isBefore(span.endUtc) }
            .sortedBy { it.hourUtc }

    /**
     * The mean total cloud across [span], or `null` when no hour falls inside it — a stated absence
     * rather than a zero that would read as a clear sky.
     */
    fun meanCloudPercentWithin(span: NightSpan): Int? {
        val inside = hoursWithin(span)
        if (inside.isEmpty()) return null
        return Math.round(inside.map { it.cloudCoverPercent }.average()).toInt()
    }

    /** The best hour inside [span]: the lowest total cloud, or `null` when the span holds no hour. */
    fun clearestHourWithin(span: NightSpan): CloudHour? =
        hoursWithin(span).minByOrNull { it.cloudCoverPercent }

    companion object {
        /** The source label the UI shows beside every cloud number. */
        const val SOURCE_LABEL: String = "MET Norway"
    }
}
