package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.data.OmmElementSet
import com.gexetr.lumintralightplanner.data.OmmUnits
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests over the **pure** element-record parser — [OmmKeyword] — and the unit conversions in
 * `OmmUnits`.
 *
 * The parser is deliberately separated from the engine so this file needs **no Orekit initialisation
 * and no Android**: it reads and writes keyword text, so it compiles and runs as a plain JUnit 4 test
 * on the JVM.
 *
 * ### What this file covers
 *
 * The `EPOCH` read (CelesTrak's own `2026-10-09T04:28:31.895104` form, no `Z`, read as UTC), the
 * catalogue number as a **number** rather than as a column (six-digit and nine-digit `NORAD_CAT_ID`),
 * the record-grouping rules (a blank line, and the repeated `CCSDS_OMM_VERS` that starts a new record
 * without one), and every refusal path as a **typed failure** rather than a throw: a missing required
 * key, an unreadable number, a physically impossible eccentricity, a line that is not a
 * `KEY = VALUE` pair, and a JSON body (which belongs to `ElementsProtocol`, not here). The negative
 * controls matter as much as the positive ones: a body that parses into a record when it should be
 * refused is the defect that puts a fabricated orbit behind a pass time.
 *
 * ### What this file does **not** cover, and cannot
 *
 * The propagation, the 3600 s segmented event search, the umbra test, the drawn-horizon mask and the
 * lens-frame crossings all need Orekit data loaded and a real propagator, so they are not testable in
 * a unit test and are **not** claimed here. The engine's entry point refuses with a typed
 * `ENGINE_NOT_READY` state rather than initialising on the caller's thread, which is what makes the
 * un-initialised path safe to leave untested.
 */
class TleParsingTest {

    // -------------------------------------------------------------------------------------------
    // One record, read in full
    // -------------------------------------------------------------------------------------------

    @Test
    fun `parses a full keyword record into a typed element set`() {
        val set = single(record())

        assertEquals(25_544L, set.noradId)
        assertEquals("ISS (ZARYA)", set.objectName)
        assertEquals("1998-067A", set.objectId)
        assertEquals("U", set.classificationType)
        assertEquals(Instant.parse("2026-10-09T04:28:31.895104Z"), set.epochUtc)
        assertEquals(15.48769982, set.meanMotionRevPerDay, 1e-12)
        assertEquals(0.0005314, set.eccentricity, 1e-15)
        assertEquals(51.6415, set.inclinationDeg, 1e-12)
        assertEquals(244.3907, set.raOfAscNodeDeg, 1e-12)
        assertEquals(131.2999, set.argOfPericenterDeg, 1e-12)
        assertEquals(228.6909, set.meanAnomalyDeg, 1e-12)
        assertEquals(0.000140559, set.bstar, 1e-15)
        assertEquals(999, set.elementSetNo)
        assertEquals(45_123L, set.revAtEpoch)
        assertTrue("EPHEMERIS_TYPE = 0 is the SGP4 model the engine propagates", set.isSgp4)
        // The record's own bytes travel with it, so a downstream parser reads what the source sent.
        assertTrue(set.payload.contains("NORAD_CAT_ID = 25544"))
    }

    // -------------------------------------------------------------------------------------------
    // Catalogue numbers are numbers, not five-digit columns
    // -------------------------------------------------------------------------------------------

    @Test
    fun `carries a six-digit catalogue number as a number`() {
        val set = single(record(noradId = "100123"))
        assertEquals(100_123L, set.noradId)
    }

    @Test
    fun `carries a nine-digit catalogue number as a number`() {
        val set = single(record(noradId = "100000001"))
        assertEquals(100_000_001L, set.noradId)
    }

    @Test
    fun `refuses a catalogue number that is not a positive integer`() {
        assertTrue(OmmKeyword.parse(record(noradId = "0")) is OmmKeywordParse.Malformed)
        assertTrue(OmmKeyword.parse(record(noradId = "-25544")) is OmmKeywordParse.Malformed)
        assertTrue(OmmKeyword.parse(record(noradId = "ISS")) is OmmKeywordParse.Malformed)
    }

    // -------------------------------------------------------------------------------------------
    // The epoch
    // -------------------------------------------------------------------------------------------

    @Test
    fun `reads the CelesTrak epoch without a Z as UTC`() {
        // CelesTrak's OMM sends `EPOCH` with no zone designator; it is UTC because TIME_SYSTEM says so.
        val set = single(record(epoch = "2026-10-10T11:03:12.345678"))
        assertEquals(Instant.parse("2026-10-10T11:03:12.345678Z"), set.epochUtc)
    }

    @Test
    fun `reads a Z-suffixed epoch as the same instant as the bare form`() {
        val bare = single(record(epoch = "2026-10-10T11:03:12.345678"))
        val suffixed = single(record(epoch = "2026-10-10T11:03:12.345678Z"))
        assertEquals(bare.epochUtc, suffixed.epochUtc)
    }

    @Test
    fun `refuses a record whose epoch cannot be read as an instant`() {
        assertTrue(OmmKeyword.parse(record(epoch = "the ninth of October")) is OmmKeywordParse.Malformed)
    }

    // -------------------------------------------------------------------------------------------
    // Record grouping
    // -------------------------------------------------------------------------------------------

    @Test
    fun `separates two records that a blank line divides`() {
        val body = record() + "\n\n" + record(noradId = "43013", overrides = mapOf("OBJECT_NAME" to "NOAA 20 (JPSS-1)"))
        val sets = records(body)

        assertEquals(2, sets.size)
        assertEquals(25_544L, sets[0].noradId)
        assertEquals("ISS (ZARYA)", sets[0].objectName)
        assertEquals(43_013L, sets[1].noradId)
        assertEquals("NOAA 20 (JPSS-1)", sets[1].objectName)
    }

    @Test
    fun `starts a new record at a repeated CCSDS_OMM_VERS with no blank line`() {
        val body = record() + "\n" + record(noradId = "43013", overrides = mapOf("OBJECT_NAME" to "NOAA 20 (JPSS-1)"))
        val sets = records(body)

        assertEquals(2, sets.size)
        assertEquals(25_544L, sets[0].noradId)
        assertEquals(43_013L, sets[1].noradId)
    }

    @Test
    fun `returns Empty for a blank body`() {
        assertEmpty("")
        assertEmpty("   ")
        assertEmpty("\n\n   \n\t\n")
    }

    @Test
    fun `returns Empty for a body that carries only comments`() {
        assertEmpty("COMMENT = this body carries no element record")
        assertEmpty("COMMENT = one\nCOMMENT = and another\n")
    }

    // -------------------------------------------------------------------------------------------
    // Refusals are typed outcomes, never throws
    // -------------------------------------------------------------------------------------------

    @Test
    fun `refuses a record that is missing a key the propagator needs`() {
        val reason = malformedReason(record(drop = setOf("MEAN_ANOMALY")))
        assertTrue("the refusal names the missing field: $reason", reason.contains("missing a field"))
    }

    @Test
    fun `refuses a record whose numeric value cannot be read`() {
        assertTrue(
            OmmKeyword.parse(record(overrides = mapOf("MEAN_MOTION" to "not-a-number")))
                is OmmKeywordParse.Malformed,
        )
        assertTrue(
            OmmKeyword.parse(record(overrides = mapOf("BSTAR" to ""))) is OmmKeywordParse.Malformed,
        )
    }

    @Test
    fun `refuses a physically impossible orbit`() {
        // An eccentricity of 1.0 is a parabola, not an orbit; a bound check is not a style choice.
        assertTrue(OmmKeyword.parse(record(overrides = mapOf("ECCENTRICITY" to "1.0"))) is OmmKeywordParse.Malformed)
        assertTrue(OmmKeyword.parse(record(overrides = mapOf("ECCENTRICITY" to "-0.1"))) is OmmKeywordParse.Malformed)
        assertTrue(OmmKeyword.parse(record(overrides = mapOf("MEAN_MOTION" to "0"))) is OmmKeywordParse.Malformed)
        assertTrue(OmmKeyword.parse(record(overrides = mapOf("INCLINATION" to "181.0"))) is OmmKeywordParse.Malformed)
    }

    @Test
    fun `refuses a line that is not a key value pair`() {
        val reason = malformedReason("1 25544U 98067A   26282.18648090  .00010731  00000+0  14055-3 0  9995")
        assertTrue("the refusal names the shape it wanted: $reason", reason.contains("KEY = VALUE"))
    }

    @Test
    fun `refuses a JSON body by name rather than half-reading it`() {
        val reason = malformedReason("""[{"OBJECT_NAME":"ISS (ZARYA)","NORAD_CAT_ID":25544}]""")
        assertTrue("JSON is ElementsProtocol's job and the refusal says so: $reason", reason.contains("JSON"))
    }

    @Test
    fun `refuses a record whose ephemeris type is unreadable`() {
        assertTrue(
            OmmKeyword.parse(record(overrides = mapOf("EPHEMERIS_TYPE" to "SGP4")))
                is OmmKeywordParse.Malformed,
        )
    }

    // -------------------------------------------------------------------------------------------
    // The raw-map reader, and the required-key contract
    // -------------------------------------------------------------------------------------------

    @Test
    fun `fromKeywords refuses a map missing a required key`() {
        val fields = baseFields()
        fields.remove("NORAD_CAT_ID")
        assertNull(OmmKeyword.fromKeywords(fields))
    }

    @Test
    fun `the required key set holds every field the propagator needs`() {
        val required = OmmKeyword.REQUIRED_KEYS
        for (key in listOf(
            "OBJECT_NAME",
            "EPOCH",
            "NORAD_CAT_ID",
            "MEAN_MOTION",
            "ECCENTRICITY",
            "INCLINATION",
            "RA_OF_ASC_NODE",
            "ARG_OF_PERICENTER",
            "MEAN_ANOMALY",
            "EPHEMERIS_TYPE",
            "BSTAR",
            "MEAN_MOTION_DOT",
            "MEAN_MOTION_DDOT",
        )) {
            assertTrue("$key must be required, not defaulted", required.contains(key))
        }
    }

    // -------------------------------------------------------------------------------------------
    // The writer, and the round trip through it
    // -------------------------------------------------------------------------------------------

    @Test
    fun `round-trips an element set through the keyword writer`() {
        val original = single(record())
        val rewritten = OmmKeyword.toKvn(original)

        // The header keys CelesTrak's own OMM does not carry are supplied, because Orekit's parser
        // requires them. Their values describe the data as the source sends it.
        assertTrue(rewritten.contains("CCSDS_OMM_VERS = 2.0"))
        assertTrue(rewritten.contains("CENTER_NAME = EARTH"))
        assertTrue(rewritten.contains("REF_FRAME = TEME"))
        assertTrue(rewritten.contains("TIME_SYSTEM = UTC"))
        assertTrue(rewritten.contains("MEAN_ELEMENT_THEORY = SGP/SGP4"))

        val reparsed = single(rewritten)
        assertEquals(original.noradId, reparsed.noradId)
        assertEquals(original.objectName, reparsed.objectName)
        assertEquals(original.objectId, reparsed.objectId)
        assertEquals(original.epochUtc, reparsed.epochUtc)
        assertEquals(original.meanMotionRevPerDay, reparsed.meanMotionRevPerDay, 1e-12)
        assertEquals(original.eccentricity, reparsed.eccentricity, 1e-15)
        assertEquals(original.inclinationDeg, reparsed.inclinationDeg, 1e-12)
        assertEquals(original.bstar, reparsed.bstar, 1e-15)
    }

    @Test
    fun `classifies the SGP4 ephemeris type`() {
        assertTrue(single(record(overrides = mapOf("EPHEMERIS_TYPE" to "0"))).isSgp4)
        assertFalse(single(record(overrides = mapOf("EPHEMERIS_TYPE" to "2"))).isSgp4)
    }

    // -------------------------------------------------------------------------------------------
    // Unit conversions — rad/s, not rev/day
    // -------------------------------------------------------------------------------------------

    @Test
    fun `converts mean motion from revolutions per day to radians per second`() {
        val revPerDay = 15.48769982
        val radiansPerSecond = OmmUnits.meanMotionToRadPerSecond(revPerDay)
        val expected = revPerDay * 2.0 * Math.PI / OmmUnits.SECONDS_PER_DAY

        assertEquals(expected, radiansPerSecond, 1e-18)
        // The defect this pins: passing rev/day straight to the propagator makes the satellite fall
        // out of the sky. A value three orders of magnitude too large is that bug, named.
        assertTrue("mean motion must be rad/s: $radiansPerSecond", radiansPerSecond < 0.01)
        assertTrue(
            "15.48769982 rev/day is 1.1e-3 rad/s: $radiansPerSecond",
            radiansPerSecond > 1.0e-3 && radiansPerSecond < 1.5e-3,
        )
    }

    @Test
    fun `converts the dot terms with the squared and cubed day denominators`() {
        val dot = 1.0731e-4
        val dotDot = 0.0

        val seconds2 = OmmUnits.meanMotionDotToRadPerSecond2(dot)
        assertEquals(dot * 2.0 * Math.PI / (OmmUnits.SECONDS_PER_DAY * OmmUnits.SECONDS_PER_DAY), seconds2, 1e-25)
        assertTrue("the second derivative is tiny: $seconds2", seconds2 in 1.0e-15..1.0e-12)

        val seconds3 = OmmUnits.meanMotionDotDotToRadPerSecond3(dotDot)
        assertEquals(
            dotDot * 2.0 * Math.PI / (OmmUnits.SECONDS_PER_DAY * OmmUnits.SECONDS_PER_DAY * OmmUnits.SECONDS_PER_DAY),
            seconds3,
            1e-30,
        )
    }

    @Test
    fun `wraps angles into the two-pi interval instead of clamping them`() {
        assertEquals(Math.toRadians(330.0), OmmUnits.wrapToTwoPi(OmmUnits.toRadians(-30.0)), 1e-15)
        assertEquals(Math.toRadians(10.0), OmmUnits.wrapToTwoPi(OmmUnits.toRadians(370.0)), 1e-15)
        assertEquals(0.0, OmmUnits.wrapToTwoPi(0.0), 0.0)

        // Clamping a negative anomaly to zero would move the satellite rather than normalise it, so
        // the propagator parameters carry the wrapped value and the inclination is left unwrapped.
        val set = single(
            record(overrides = mapOf("MEAN_ANOMALY" to "-30.0", "RA_OF_ASC_NODE" to "-10.0")),
        )
        val params = set.toPropagatorParams()

        assertEquals(Math.toRadians(330.0), params.meanAnomalyRad, 1e-15)
        assertEquals(Math.toRadians(350.0), params.raOfAscNodeRad, 1e-15)
        assertFalse("a wrapped anomaly is not zero", params.meanAnomalyRad == 0.0)
        assertEquals(OmmUnits.toRadians(51.6415), params.inclinationRad, 1e-15)
        assertEquals(set.epochUtc, params.epochUtc)
        assertEquals(set.noradId, params.noradId)
    }

    // -------------------------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------------------------

    /** The base record's fields, in the order a keyword body lists them. */
    private fun baseFields(
        noradId: String = "25544",
        epoch: String = "2026-10-09T04:28:31.895104",
    ): LinkedHashMap<String, String> = linkedMapOf(
        "CCSDS_OMM_VERS" to "2.0",
        "COMMENT" to "GENERATED VIA SPACE-TRACK.ORG API",
        "CREATION_DATE" to "2026-10-09T04:28:31",
        "ORIGINATOR" to "18 SPCS",
        "OBJECT_NAME" to "ISS (ZARYA)",
        "OBJECT_ID" to "1998-067A",
        "CENTER_NAME" to "EARTH",
        "REF_FRAME" to "TEME",
        "TIME_SYSTEM" to "UTC",
        "MEAN_ELEMENT_THEORY" to "SGP4",
        "EPOCH" to epoch,
        "MEAN_MOTION" to "15.48769982",
        "ECCENTRICITY" to "0.0005314",
        "INCLINATION" to "51.6415",
        "RA_OF_ASC_NODE" to "244.3907",
        "ARG_OF_PERICENTER" to "131.2999",
        "MEAN_ANOMALY" to "228.6909",
        "EPHEMERIS_TYPE" to "0",
        "CLASSIFICATION_TYPE" to "U",
        "NORAD_CAT_ID" to noradId,
        "ELEMENT_SET_NO" to "999",
        "REV_AT_EPOCH" to "45123",
        "BSTAR" to "0.00014055900000",
        "MEAN_MOTION_DOT" to "0.00010731",
        "MEAN_MOTION_DDOT" to "0",
    )

    /** One keyword record, with fields overridden or removed so a refusal can be driven by name. */
    private fun record(
        noradId: String = "25544",
        epoch: String = "2026-10-09T04:28:31.895104",
        overrides: Map<String, String> = emptyMap(),
        drop: Set<String> = emptySet(),
    ): String {
        val fields = baseFields(noradId, epoch)
        fields.putAll(overrides)
        for (key in drop) fields.remove(key)
        return fields.entries.joinToString(separator = "\n") { (key, value) -> "$key = $value" }
    }

    private fun records(body: String): List<OmmElementSet> =
        when (val parsed = OmmKeyword.parse(body)) {
            is OmmKeywordParse.Records -> parsed.sets
            is OmmKeywordParse.Malformed ->
                throw AssertionError("expected a record, the body was refused: ${parsed.reason}")

            is OmmKeywordParse.Empty -> throw AssertionError("expected a record, the body read as empty")
        }

    private fun single(body: String): OmmElementSet {
        val sets = records(body)
        assertEquals("expected exactly one record", 1, sets.size)
        return sets.single()
    }

    private fun malformedReason(body: String): String =
        when (val parsed = OmmKeyword.parse(body)) {
            is OmmKeywordParse.Malformed -> parsed.reason
            is OmmKeywordParse.Records ->
                throw AssertionError("expected a refusal, the body parsed into ${parsed.sets.size} record(s)")

            is OmmKeywordParse.Empty -> throw AssertionError("expected a refusal, the body read as empty")
        }

    private fun assertEmpty(body: String) {
        val parsed = OmmKeyword.parse(body)
        assertTrue("expected Empty, got $parsed", parsed is OmmKeywordParse.Empty)
    }
}
