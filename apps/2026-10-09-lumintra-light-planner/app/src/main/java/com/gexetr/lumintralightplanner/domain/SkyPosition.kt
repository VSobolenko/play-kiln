package com.gexetr.lumintralightplanner.domain

/**
 * A direction on the local sky: altitude above the horizon and azimuth around it.
 *
 * The conventions are fixed here once, because getting either wrong silently mirrors the whole sky:
 *
 * - [altitudeDeg] is degrees above (+) or below (−) the geometric horizon.
 * - [azimuthDeg] is degrees measured **0 = North, 90 = East, clockwise**, in `[0, 360)`.
 */
data class HorizontalCoords(
    val altitudeDeg: Double,
    val azimuthDeg: Double,
) {
    /** Normalised to `[0, 360)` so a value from an engine that returns ±180 compares equal. */
    val azimuthNormalised: Double get() = normaliseAzimuth(azimuthDeg)

    /** The compass-point key the UI maps to a resource: "N", "NE", "E", … (16 points). */
    val compassPoint: String
        get() {
            val names = COMPASS_16
            val index = ((azimuthNormalised / 22.5) + 0.5).toInt() % 16
            return names[index]
        }

    /** Air mass at this altitude: Kasten–Young, clamped where the formula breaks down. */
    val airMass: Double
        get() = airMassAt(altitudeDeg)

    /** Angular separation from [other] in degrees. */
    fun separationTo(other: HorizontalCoords): Double = angularSeparationDeg(
        altitudeDeg, azimuthDeg,
        other.altitudeDeg, other.azimuthDeg,
    )

    /** This position after the horizon has blocked it, if the horizon is above it. */
    fun occludedBy(horizon: HorizonProfile): Boolean =
        horizon.altitudeAt(azimuthDeg) > altitudeDeg

    companion object {
        val COMPASS_16 = arrayOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
        )

        fun normaliseAzimuth(deg: Double): Double {
            val m = deg % 360.0
            return if (m < 0.0) m + 360.0 else m
        }

        /**
         * Kasten & Young (1989) relative optical air mass.
         *
         * Below the horizon there is no meaningful air mass for a target the user can see, so the
         * value is clamped at the horizon and reported as-is rather than returning infinity.
         */
        fun airMassAt(altitudeDeg: Double): Double {
            val h = if (altitudeDeg < 0.0) 0.0 else altitudeDeg
            val denom = h + 5.256 * Math.pow(h + 3.724, -1.0) + 0.00009
            return if (denom <= 0.0) AIR_MASS_AT_HORIZON else 1.0 / denom
        }

        /** The air mass at the horizon (h = 0°), ≈ 37.9. Stated so a clamp is never a surprise. */
        const val AIR_MASS_AT_HORIZON: Double = 37.9195

        /** Great-circle separation in degrees between two alt/az positions. */
        fun angularSeparationDeg(
            alt1Deg: Double,
            az1Deg: Double,
            alt2Deg: Double,
            az2Deg: Double,
        ): Double {
            val a1 = Math.toRadians(alt1Deg)
            val a2 = Math.toRadians(alt2Deg)
            val dAz = Math.toRadians(normaliseAzimuth(az2Deg - az1Deg))
            val cosSep = Math.sin(a1) * Math.sin(a2) + Math.cos(a1) * Math.cos(a2) * Math.cos(dAz)
            return Math.toDegrees(Math.acos(cosSep.coerceIn(-1.0, 1.0)))
        }
    }
}

/**
 * A direction in the catalogue's frame: right ascension and declination, epoch **J2000.0 / ICRS**.
 *
 * [raDeg] is **degrees in `[0, 360)`**, never hours. This is the one place the app's own convention
 * differs from the vendored engine, whose `horizon()` and `defineStar()` take RA in **hours** and
 * convert internally with `sinra = dsin(ra * 15.0)`. The conversion `raDeg / 15.0` happens at exactly
 * one boundary — the `data/`↔engine adapter — and hours are never stored anywhere.
 */
data class EquatorialCoords(
    val raDeg: Double,
    val decDeg: Double,
) {
    /** The engine's RA unit. Named so a call site cannot confuse it with [raDeg]. */
    val raHours: Double get() = raDeg / 15.0

    init {
        require(raDeg.isFinite() && decDeg.isFinite()) { "EquatorialCoords must be finite" }
        require(decDeg >= -90.0 && decDeg <= 90.0) { "decDeg $decDeg outside [-90, 90]" }
    }

    /** Normalised to `[0, 360)`. */
    val raNormalised: Double
        get() {
            val m = raDeg % 360.0
            return if (m < 0.0) m + 360.0 else m
        }

    /** The RA as the conventional `hh mm ss` label, computed from degrees and never stored as hours. */
    fun raLabel(): String {
        val totalHours = raNormalised / 15.0
        val h = totalHours.toInt()
        val minutesTotal = (totalHours - h) * 60.0
        val m = minutesTotal.toInt()
        val s = ((minutesTotal - m) * 60.0).toInt()
        return "%02d %02d %02d".format(h, m, s)
    }

    /** The Dec as the conventional `±dd° mm′` label. */
    fun decLabel(): String {
        val sign = if (decDeg < 0.0) "-" else "+"
        val abs = Math.abs(decDeg)
        val d = abs.toInt()
        val m = ((abs - d) * 60.0).toInt()
        return "%s%02d° %02d′".format(sign, d, m)
    }

    companion object {
        /** The catalogue epoch, declared rather than implied. No precession is applied to stored values. */
        const val EPOCH: String = "J2000.0"

        /** Builds from the conventional hours/minutes/seconds a human reads off a chart. */
        fun ofHours(hours: Double, decDeg: Double): EquatorialCoords =
            EquatorialCoords(hours * 15.0, decDeg)
    }
}

/**
 * A geodetic place on the Earth, with the sign conventions the engine requires.
 *
 * - [latDeg] latitude degrees **North + / South −**
 * - [lonDeg] longitude degrees **East + / West −**
 * - [elevationM] metres above sea level
 *
 * A west-positive longitude silently mirrors the sky, which is why the convention is stated on the
 * type rather than left to a parameter name.
 */
data class GeodeticPoint(
    val latDeg: Double,
    val lonDeg: Double,
    val elevationM: Double = 0.0,
) {
    init {
        require(latDeg in -90.0..90.0) { "latDeg $latDeg outside [-90, 90]" }
        require(lonDeg in -180.0..180.0) { "lonDeg $lonDeg outside [-180, 180]" }
    }

    /** The degrees–minutes label a user can read back, e.g. `59°54′ N, 10°45′ E`. */
    fun label(): String = "${latLabel()}, ${lonLabel()}"

    fun latLabel(): String {
        val hemisphere = if (latDeg < 0.0) "S" else "N"
        val abs = Math.abs(latDeg)
        val d = abs.toInt()
        val m = ((abs - d) * 60.0).toInt()
        return "%d°%02d′ %s".format(d, m, hemisphere)
    }

    fun lonLabel(): String {
        val hemisphere = if (lonDeg < 0.0) "W" else "E"
        val abs = Math.abs(lonDeg)
        val d = abs.toInt()
        val m = ((abs - d) * 60.0).toInt()
        return "%d°%02d′ %s".format(d, m, hemisphere)
    }

    /**
     * The coordinate the network clients are allowed to send: truncated to **4 decimal places**.
     *
     * `api.met.no` returns 403 for 5 or more decimals, so truncation is a protocol requirement here,
     * not a precision preference.
     */
    fun networkRounded(): GeodeticPoint = GeodeticPoint(
        latDeg = truncateTo4(latDeg),
        lonDeg = truncateTo4(lonDeg),
        elevationM = elevationM,
    )

    companion object {
        /** Toward zero, not to nearest: a truncation cannot move the point further than the format allows. */
        fun truncateTo4(value: Double): Double {
            val scaled = value * 10_000.0
            val truncated = if (scaled < 0.0) Math.ceil(scaled) else Math.floor(scaled)
            return truncated / 10_000.0
        }
    }
}
