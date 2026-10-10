package com.gexetr.lumintralightplanner.domain

/**
 * The user's horizon: an obstruction profile as a **fixed 360-sample array**, one sample per whole
 * degree of azimuth.
 *
 * The count is settled at 360 deliberately (1.0° per whole degree of azimuth), over the 720-sample /
 * 0.5° shape an earlier source report proposed. Other azimuths interpolate **linearly**, wrapping at
 * `0/360`. Occlusion is an `O(1)` array lookup, which is what lets the scene mask every drawn object
 * without a spatial index.
 *
 * [altitudeDeg] holds elevations in degrees in `[-10.0, +90.0]`. It is serialised as a **720-byte
 * buffer of 360 signed 16-bit values** in tenths of a degree, little-endian — see [encode] / [decode].
 */
class HorizonProfile(
    val id: String,
    val name: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    altitudeDeg: FloatArray,
) {
    val altitudeDeg: FloatArray = altitudeDeg.copyOf().also {
        require(it.size == SAMPLE_COUNT) {
            "horizon must hold exactly $SAMPLE_COUNT samples, got ${it.size}"
        }
    }

    /** The elevation at a whole-degree azimuth, in degrees. */
    operator fun get(azimuthDegIndex: Int): Float = altitudeDeg[wrapIndex(azimuthDegIndex)]

    /**
     * The elevation in degrees at an arbitrary azimuth, interpolating linearly between the two
     * neighbouring whole degrees and wrapping across 0/360.
     */
    fun altitudeAt(azimuthDeg: Double): Double {
        val normalised = HorizontalCoords.normaliseAzimuth(azimuthDeg)
        val lower = normalised.toInt() % SAMPLE_COUNT
        val upper = (lower + 1) % SAMPLE_COUNT
        val t = normalised - lower
        val a = altitudeDeg[lower].toDouble()
        val b = altitudeDeg[upper].toDouble()
        return a + (b - a) * t
    }

    /** A copy with one whole-degree sample replaced, clamped to the storable range. */
    fun withSample(azimuthDegIndex: Int, elevationDeg: Double): HorizonProfile {
        val next = altitudeDeg.copyOf()
        next[wrapIndex(azimuthDegIndex)] = clampStorage(elevationDeg)
        return HorizonProfile(id, name, latitudeDeg, longitudeDeg, next)
    }

    /** The samples as [HorizonSample] values, for a UI that edits them one at a time. */
    fun samples(): List<HorizonSample> =
        altitudeDeg.indices.map { HorizonSample(it.toDouble(), altitudeDeg[it].toDouble()) }

    val highestDeg: Double get() = altitudeDeg.maxOrNull()?.toDouble() ?: 0.0
    val lowestDeg: Double get() = altitudeDeg.minOrNull()?.toDouble() ?: 0.0

    /**
     * The serialised form: exactly [ENCODED_BYTES] bytes, 360 little-endian `int16` values in tenths
     * of a degree.
     *
     * The widths are stated arithmetic, not a measurement: `2 B × 360 = 720 B`.
     */
    fun encode(): ByteArray {
        val out = ByteArray(ENCODED_BYTES)
        for (i in 0 until SAMPLE_COUNT) {
            val raw = Math.round(altitudeDeg[i] * 10.0f).toInt().coerceIn(MIN_STORED, MAX_STORED)
            out[i * 2] = (raw and 0xFF).toByte()
            out[i * 2 + 1] = ((raw shr 8) and 0xFF).toByte()
        }
        return out
    }

    /** Structurally equal profiles compare by their samples, not by array identity. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HorizonProfile) return false
        return id == other.id && name == other.name &&
            latitudeDeg == other.latitudeDeg && longitudeDeg == other.longitudeDeg &&
            altitudeDeg.contentEquals(other.altitudeDeg)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + latitudeDeg.hashCode()
        result = 31 * result + longitudeDeg.hashCode()
        result = 31 * result + altitudeDeg.contentHashCode()
        return result
    }

    companion object {
        /** One sample per whole degree of azimuth. */
        const val SAMPLE_COUNT: Int = 360

        /** `2 B × 360`, little-endian signed 16-bit. */
        const val ENCODED_BYTES: Int = SAMPLE_COUNT * 2

        /** Elevation stored in tenths of a degree, clamped to `[-10.0°, +90.0°]`. */
        const val MIN_DEG: Double = -10.0
        const val MAX_DEG: Double = 90.0
        const val MIN_STORED: Int = -100
        const val MAX_STORED: Int = 900

        /** A flat horizon at the given elevation, the shape a new spot starts from. */
        fun flat(
            id: String,
            name: String,
            latitudeDeg: Double,
            longitudeDeg: Double,
            elevationDeg: Double = 0.0,
        ): HorizonProfile = HorizonProfile(
            id = id,
            name = name,
            latitudeDeg = latitudeDeg,
            longitudeDeg = longitudeDeg,
            altitudeDeg = FloatArray(SAMPLE_COUNT) { clampStorage(elevationDeg) },
        )

        /**
         * Decodes the 720-byte form. A buffer of the wrong length, or one holding a value outside the
         * storable range, is a **typed failure** (`null`) rather than an exception — the exported file
         * is untrusted input and a malformed one must cost the file, never the app.
         */
        fun decode(
            id: String,
            name: String,
            latitudeDeg: Double,
            longitudeDeg: Double,
            bytes: ByteArray,
            offset: Int = 0,
        ): HorizonProfile? {
            if (bytes.size - offset < ENCODED_BYTES) return null
            val samples = FloatArray(SAMPLE_COUNT)
            for (i in 0 until SAMPLE_COUNT) {
                val lo = bytes[offset + i * 2].toInt() and 0xFF
                val hi = bytes[offset + i * 2 + 1].toInt() and 0xFF
                val raw = (hi shl 8) or lo
                val signed = if (raw and 0x8000 != 0) raw - 0x10000 else raw
                if (signed < MIN_STORED || signed > MAX_STORED) return null
                samples[i] = signed / 10.0f
            }
            return HorizonProfile(id, name, latitudeDeg, longitudeDeg, samples)
        }

        private fun clampStorage(elevationDeg: Double): Float =
            elevationDeg.coerceIn(MIN_DEG, MAX_DEG).toFloat()

        private fun wrapIndex(index: Int): Int {
            val m = index % SAMPLE_COUNT
            return if (m < 0) m + SAMPLE_COUNT else m
        }
    }
}

/** One sample of a [HorizonProfile]: the obstruction's elevation at a whole-degree azimuth. */
data class HorizonSample(
    val azimuthDeg: Double,
    val altitudeDeg: Double,
)
