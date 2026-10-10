package com.gexetr.lumintralightplanner.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The 720-byte horizon codec of `domain/HorizonProfile.kt`, exercised on the JVM.
 *
 * The numbers here are the spec's, not the test's: exactly 360 samples, one per whole degree of azimuth
 * 0..359 (north, clockwise), elevation in tenths of a degree as a signed integer clamped to
 * `[−100, +900]`, little-endian, `2 B × 360 = 720 B` (`design.md` § Persistence; `specs/scout-spots`
 * *"The horizon line is drawn in `SpotEdit` and stored as a fixed 360-sample array"*).
 *
 * The property that matters most is the last one: a wrong-length or out-of-range buffer decodes to
 * `null` rather than throwing, because the bytes come from an untrusted export file.
 */
class HorizonProfileCodecTest {

    private fun profile(vararg samples: Pair<Int, Float>): HorizonProfile {
        val values = FloatArray(HorizonProfile.SAMPLE_COUNT)
        for ((index, value) in samples) values[index] = value
        return HorizonProfile("h1", "Ridge", 59.9, 10.7, values)
    }

    /** The stored tenths-of-a-degree integers, read back the way the codec writes them. */
    private fun storedSamples(bytes: ByteArray): IntArray {
        assertEquals("the buffer is always the full profile", HorizonProfile.ENCODED_BYTES, bytes.size)
        val out = IntArray(HorizonProfile.SAMPLE_COUNT)
        for (i in 0 until HorizonProfile.SAMPLE_COUNT) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt() and 0xFF
            val raw = (hi shl 8) or lo
            out[i] = if (raw and 0x8000 != 0) raw - 0x10000 else raw
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------
    // Width and byte layout
    // ---------------------------------------------------------------------------------------------

    @Test
    fun encodesExactlySevenHundredAndTwentyBytes() {
        assertEquals(720, HorizonProfile.ENCODED_BYTES)
        assertEquals(HorizonProfile.SAMPLE_COUNT * 2, HorizonProfile.ENCODED_BYTES)
        assertEquals(360, HorizonProfile.SAMPLE_COUNT)

        assertEquals(720, profile().encode().size)
        assertEquals(720, profile(0 to 12.3f, 359 to -4.5f).encode().size)
    }

    @Test
    fun encodesTenthsOfADegreeAsSignedLittleEndianSixteenBitValues() {
        val positive = profile(0 to 12.3f).encode()
        assertEquals(123, storedSamples(positive)[0])
        assertEquals(123, positive[0].toInt() and 0xFF)
        assertEquals(0, positive[1].toInt() and 0xFF)

        // Sample 1 lives at byte offsets 2 and 3 −45 = 0xFFD3 little-endian is 0xD3 then 0xFF.
        val negative = profile(1 to -4.5f).encode()
        assertEquals(-45, storedSamples(negative)[1])
        assertEquals(0xD3, negative[2].toInt() and 0xFF)
        assertEquals(0xFF, negative[3].toInt() and 0xFF)
        assertEquals(0, negative[0].toInt() and 0xFF)
        assertEquals(0, negative[1].toInt() and 0xFF)
    }

    // ---------------------------------------------------------------------------------------------
    // Round trip and clamp
    // ---------------------------------------------------------------------------------------------

    @Test
    fun roundTripPreservesEverySampleWithinTheTenthDegreeQuantum() {
        val original = HorizonProfile(
            "h1",
            "Ridge",
            59.9,
            10.7,
            FloatArray(HorizonProfile.SAMPLE_COUNT) { (it % 100) - 10f },
        )

        val decoded = requireNotNull(HorizonProfile.decode("h1", "Ridge", 59.9, 10.7, original.encode()))
        assertArrayEquals(original.altitudeDeg, decoded.altitudeDeg, 1e-3f)
        assertEquals(original.id, decoded.id)
        assertEquals(original.name, decoded.name)
        assertEquals(original.latitudeDeg, decoded.latitudeDeg, 0.0)
        assertEquals(original.longitudeDeg, decoded.longitudeDeg, 0.0)
        assertEquals(original, decoded)
    }

    @Test
    fun clampsToTheStorableRangeAtTheEndsAndLeavesTheInteriorAlone() {
        val extremes = profile(0 to 120f, 1 to -50f, 2 to 45.0f, 3 to -9.95f)

        val stored = storedSamples(extremes.encode())
        assertEquals(900, stored[0])
        assertEquals(-100, stored[1])

        val decoded = requireNotNull(HorizonProfile.decode("h1", "Ridge", 1.0, 2.0, extremes.encode()))
        assertEquals(HorizonProfile.MAX_DEG, decoded.altitudeDeg[0].toDouble(), 1e-6)
        assertEquals(HorizonProfile.MIN_DEG, decoded.altitudeDeg[1].toDouble(), 1e-6)
        assertEquals(45.0, decoded.altitudeDeg[2].toDouble(), 1e-6)
        assertEquals(-9.9, decoded.altitudeDeg[3].toDouble(), 1e-3)
    }

    @Test
    fun clampsExactlyAtTheTwoStorableEnds() {
        val ends = profile(0 to 90f, 1 to -10f)
        val stored = storedSamples(ends.encode())
        assertEquals(HorizonProfile.MAX_STORED, stored[0])
        assertEquals(HorizonProfile.MIN_STORED, stored[1])
    }

    // ---------------------------------------------------------------------------------------------
    // Interpolation, including the wrap at 0/360
    // ---------------------------------------------------------------------------------------------

    @Test
    fun interpolatesLinearlyBetweenWholeDegrees() {
        val subject = profile(0 to 0f, 1 to 20f)

        assertEquals(0.0, subject.altitudeAt(0.0), 1e-6)
        assertEquals(10.0, subject.altitudeAt(0.5), 1e-6)
        assertEquals(20.0, subject.altitudeAt(1.0), 1e-6)
        // Past 1° the next whole-degree sample is the untouched default, so the interval falls back to 0.
        assertEquals(10.0, subject.altitudeAt(1.5), 1e-6)
        assertEquals(0.0, subject.altitudeAt(2.0), 1e-6)
    }

    @Test
    fun wrapsTheInterpolationAcrossZeroAndThreeSixty() {
        val subject = profile(359 to 10f, 0 to 0f, 1 to 20f)

        // The 359°→0° edge is an interval like any other, and it must not be read as an edge case.
        assertEquals(10.0, subject.altitudeAt(359.0), 1e-6)
        assertEquals(5.0, subject.altitudeAt(359.5), 1e-6)
        assertEquals(0.0, subject.altitudeAt(0.0), 1e-6)
        assertEquals(0.0, subject.altitudeAt(360.0), 1e-6)

        // A negative azimuth normalises into [0, 360) and lands in the same wrapped interval.
        assertEquals(5.0, subject.altitudeAt(-0.5), 1e-6)
        assertEquals(5.0, subject.altitudeAt(719.5), 1e-6)
    }

    // ---------------------------------------------------------------------------------------------
    // Untrusted input: null, never an exception
    // ---------------------------------------------------------------------------------------------

    @Test
    fun refusesABufferShorterThanTheProfileWithNullRatherThanThrowing() {
        assertNull(HorizonProfile.decode("h1", "Ridge", 1.0, 2.0, ByteArray(0)))
        assertNull(HorizonProfile.decode("h1", "Ridge", 1.0, 2.0, ByteArray(1)))
        assertNull(HorizonProfile.decode("h1", "Ridge", 1.0, 2.0, ByteArray(HorizonProfile.ENCODED_BYTES - 1)))

        // An offset that leaves fewer than ENCODED_BYTES behind is the same refusal.
        val full = profile().encode()
        assertNull(
            HorizonProfile.decode(
                id = "h1",
                name = "Ridge",
                latitudeDeg = 1.0,
                longitudeDeg = 2.0,
                bytes = full,
                offset = 1,
            ),
        )
    }

    @Test
    fun acceptsABufferLongerThanTheProfileBecauseTheLengthIsAMinimum() {
        // The domain's own rule: only "fewer bytes than a profile" is refused, so a reader that owns a
        // larger buffer can still decode a profile from the front of it. This test states that behaviour
        // rather than leaving it implied.
        val padded = profile(0 to 7.5f).encode() + ByteArray(8)
        val decoded = requireNotNull(HorizonProfile.decode("h1", "Ridge", 1.0, 2.0, padded))
        assertEquals(7.5, decoded.altitudeDeg[0].toDouble(), 1e-6)
    }

    @Test
    fun refusesASampleOutsideTheStorableRangeWithNull() {
        val bytes = ByteArray(HorizonProfile.ENCODED_BYTES)
        // 901 little-endian: 0x0385 → low 0x85, high 0x03.
        bytes[0] = 0x85.toByte()
        bytes[1] = 0x03.toByte()
        assertNull(HorizonProfile.decode("h1", "Ridge", 1.0, 2.0, bytes))

        val tooLow = ByteArray(HorizonProfile.ENCODED_BYTES)
        // −101 little-endian: 0xFF9B → low 0x9B, high 0xFF.
        tooLow[0] = 0x9B.toByte()
        tooLow[1] = 0xFF.toByte()
        assertNull(HorizonProfile.decode("h1", "Ridge", 1.0, 2.0, tooLow))
    }

    // ---------------------------------------------------------------------------------------------
    // Construction, indexing and immutability
    // ---------------------------------------------------------------------------------------------

    @Test
    fun refusesAnArrayThatIsNotExactlyThreeHundredAndSixtySamples() {
        for (count in intArrayOf(0, 1, 359, 361, 720)) {
            try {
                HorizonProfile("h1", "Ridge", 1.0, 2.0, FloatArray(count))
                fail("a profile of $count samples must not be constructible")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message!!.contains("360"))
            }
        }
    }

    @Test
    fun indexesSamplesByWholeDegreeAzimuthAndWrapsTheIndex() {
        val subject = profile(0 to 3f, 359 to 4f)

        assertEquals(3.0, subject[0].toDouble(), 0.0)
        assertEquals(4.0, subject[359].toDouble(), 0.0)
        assertEquals(3.0, subject[360].toDouble(), 0.0)
        assertEquals(4.0, subject[-1].toDouble(), 0.0)

        val samples = subject.samples()
        assertEquals(360, samples.size)
        assertEquals(5.0, samples[5].azimuthDeg, 0.0)
        assertEquals(0.0, samples[5].altitudeDeg, 0.0)
    }

    @Test
    fun withSampleClampsWrapsAndNeverMutatesTheReceiver() {
        val original = profile()

        assertEquals(5.0, original.withSample(360, 5.0)[0].toDouble(), 0.0)
        assertEquals(7.0, original.withSample(-1, 7.0)[359].toDouble(), 0.0)
        assertEquals(HorizonProfile.MAX_DEG, original.withSample(0, 1_000.0)[0].toDouble(), 0.0)
        assertEquals(HorizonProfile.MIN_DEG, original.withSample(0, -1_000.0)[0].toDouble(), 0.0)

        assertEquals("the receiver is untouched", 0.0, original[0].toDouble(), 0.0)
    }

    @Test
    fun aFlatProfileIsAPresentProfileAndNotAnAbsence() {
        // `specs/scout-spots` "Degenerate horizons are legal": 360 zeros is a horizon at 0°, stored and
        // drawn, and it is a different state from hasProfile = false (which is a null record, not this).
        val flat = HorizonProfile.flat("h1", "Flat", 59.9, 10.7)

        assertEquals(360, HorizonProfile.decode("h1", "Flat", 59.9, 10.7, flat.encode())!!.altitudeDeg.size)
        assertEquals(0.0, flat.highestDeg, 0.0)
        assertEquals(0.0, flat.lowestDeg, 0.0)
        assertEquals(0.0, flat.altitudeAt(123.4), 1e-6)
    }

    @Test
    fun aSkyBlockingProfileIsLegalAndReadsBackAtNinetyDegrees() {
        val blocking = HorizonProfile.flat("h1", "Blocked", 59.9, 10.7, HorizonProfile.MAX_DEG)
        val decoded = requireNotNull(HorizonProfile.decode("h1", "Blocked", 59.9, 10.7, blocking.encode()))

        assertArrayEquals(
            FloatArray(HorizonProfile.SAMPLE_COUNT) { HorizonProfile.MAX_DEG.toFloat() },
            decoded.altitudeDeg,
            0f,
        )
        assertEquals(HorizonProfile.MAX_DEG, decoded.highestDeg, 0.0)
    }
}
