package com.gexetr.lumintralightplanner.data

import com.gexetr.lumintralightplanner.domain.EquipmentProfile
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.Scout
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The export/import codec of `data/SpotTransfer.kt`, exercised on the JVM — no Android framework, no
 * `Robolectric`, no device.
 *
 * Every assertion here is one sentence of the spec: the document's field list and its `schemaVersion`
 * order (`specs/scout-spots` *"The user's spots, horizons and equipment export to a file they own"*),
 * the typed refusal of an unknown version and of a crafted payload, all-or-nothing import, the
 * merge/replace counts shown before the commit, the caps validated before allocation, and the
 * per-record store codec whose failure mode is `null` so [ScoutStore] can quarantine one record without
 * losing the rest.
 *
 * **What these tests cannot say.** They are not evidence that the slot compiles or that a device run
 * succeeds, and they cannot prove the *pixels* of anything: the codec is text in, text out. The exported
 * file's usability on another phone, and the atomicity of DataStore's own file replacement, are outside
 * this file's reach — the latter is the platform's guarantee, cited in [ScoutStore]'s KDoc rather than
 * measured here.
 */
class SpotTransferTest {

    private val now: Instant = Instant.parse("2026-10-09T21:14:03Z")

    // ---------------------------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------------------------

    private fun horizon(
        id: String = "h1",
        elevation: Float = 4.0f,
        name: String = "Ridge",
    ): HorizonProfile = HorizonProfile(
        id = id,
        name = name,
        latitudeDeg = 59.9,
        longitudeDeg = 10.7,
        altitudeDeg = FloatArray(HorizonProfile.SAMPLE_COUNT) { elevation },
    )

    private fun aSpot(
        id: String = "s1",
        name: String = "Home",
        latDeg: Double = 59.9,
        lonDeg: Double = 10.7,
        horizonProfileId: String? = null,
        equipment: EquipmentProfile = EquipmentProfile.DEFAULT,
        approximate: Boolean = false,
    ): Scout = Scout(
        id = id,
        name = name,
        latDeg = latDeg,
        lonDeg = lonDeg,
        elevationM = 12.0,
        tzId = "Europe/Oslo",
        horizonProfileId = horizonProfileId,
        equipment = equipment,
        approximate = approximate,
    )

    private fun aDocument(
        spots: List<Scout>,
        horizons: Map<String, HorizonProfile> = emptyMap(),
        favourites: Set<String> = emptySet(),
    ): String = SpotTransfer.encodeDocument(spots, horizons, favourites, now)

    /**
     * A hand-written document, so a payload the writer would never produce can still be exercised.
     *
     * [equipmentProfiles] and [favouriteTargetIds] are the **inner** array contents — the template
     * wraps them in `[…]`. Defaults are empty strings (not `"[]"`), so an omitted list becomes a
     * legal empty JSON array rather than the nested `[ [] ]` that `kotlinx.serialization` refuses.
     */
    private fun rawDocument(
        spots: String,
        equipmentProfiles: String = "",
        favouriteTargetIds: String = "",
        schemaVersion: String = "1",
        app: String = "lumintra",
    ): String = """
        {
          "schemaVersion": $schemaVersion,
          "app": "$app",
          "exportedAtUtc": "2026-10-09T21:14:03Z",
          "spots": [ $spots ],
          "equipmentProfiles": [ $equipmentProfiles ],
          "favouriteTargetIds": [ $favouriteTargetIds ]
        }
    """.trimIndent()

    private fun rawSpot(
        id: String = "s1",
        name: String = "Home",
        latDeg: String = "59.9",
        lonDeg: String = "10.7",
        tzId: String = "Europe/Oslo",
        note: String = "null",
        equipmentProfileId: String = "null",
        horizonProfile: String = "null",
    ): String = """
        {
          "id": "$id", "name": "$name", "latDeg": $latDeg, "lonDeg": $lonDeg,
          "elevationM": 12.0, "tzId": "$tzId", "note": $note,
          "equipmentProfileId": $equipmentProfileId, "horizonProfile": $horizonProfile
        }
    """.trimIndent()

    private fun rawHorizon(samples: List<Int>, sampleCount: Int = 360): String =
        """{ "sampleCount": $sampleCount, "azimuthStepDeg": 1.0, "elevationUnitDeg": 0.1, """ +
            """"samples": [${samples.joinToString(",")}] }"""

    private fun samplesOf(count: Int, value: Int = 0): List<Int> = List(count) { value }

    private fun refusalOf(document: String): SpotTransfer.SpotTransferError {
        val result = SpotTransfer.decodeDocument(document)
        val refused = result as? SpotTransfer.DecodeResult.Refused
        return requireNotNull(refused) { "expected a typed refusal, got ${result::class.simpleName}" }.error
    }

    private fun decodedOf(document: String): SpotTransfer.ScoutBundle {
        val result = SpotTransfer.decodeDocument(document)
        val ok = result as? SpotTransfer.DecodeResult.Ok
        return requireNotNull(ok) { "expected a decoded document, got $result" }.bundle
    }

    private fun planOf(
        document: String,
        existingSpots: List<Scout> = emptyList(),
        existingFavourites: Set<String> = emptySet(),
        mode: SpotTransfer.ImportMode = SpotTransfer.ImportMode.MERGE,
    ): SpotTransfer.ImportPlan =
        SpotTransfer.plan(document, existingSpots, existingFavourites, mode)

    private fun readyOf(plan: SpotTransfer.ImportPlan): SpotTransfer.ImportPlan.Ready {
        val ready = plan as? SpotTransfer.ImportPlan.Ready
        return requireNotNull(ready) { "expected a ready plan, got $plan" }
    }

    // ---------------------------------------------------------------------------------------------
    // The document's shape
    // ---------------------------------------------------------------------------------------------

    @Test
    fun namesTheExportFileWithTheUtcDate() {
        assertEquals("lumintra-scout-20261009.json", SpotTransfer.exportFileName(now))
        assertEquals("lumintra-scout-20261009.json", SpotTransfer.exportFileName(Instant.parse("2026-10-09T23:59:59Z")))
        assertEquals("lumintra-scout-20261010.json", SpotTransfer.exportFileName(Instant.parse("2026-10-10T00:00:01Z")))
    }

    @Test
    fun writesSchemaVersionFirst() {
        val text = aDocument(listOf(aSpot()))

        val firstQuotedToken = Regex("\"[A-Za-z]+\"").find(text)?.value
        assertEquals("\"schemaVersion\"", firstQuotedToken)
        assertTrue(text.trimStart().startsWith("{"))
        assertTrue(text.indexOf("\"app\"") > text.indexOf("\"schemaVersion\""))
        assertTrue(text.indexOf("\"spots\"") > text.indexOf("\"app\""))
    }

    @Test
    fun writesExactlyTheSpecifiedFieldsAndNoOthers() {
        val text = aDocument(
            spots = listOf(aSpot(horizonProfileId = "h1")),
            horizons = mapOf("h1" to horizon()),
            favourites = setOf("m31", "ngc7000"),
        )
        val root = Json.parseToJsonElement(text).jsonObject

        assertEquals(
            setOf("schemaVersion", "app", "exportedAtUtc", "spots", "equipmentProfiles", "favouriteTargetIds"),
            root.keys,
        )
        assertEquals(1, root["schemaVersion"]!!.jsonPrimitive.content.toInt())
        assertEquals("lumintra", root["app"]!!.jsonPrimitive.content)
        assertEquals("2026-10-09T21:14:03Z", root["exportedAtUtc"]!!.jsonPrimitive.content)

        val spot = root["spots"]!!.jsonArray.single().jsonObject
        assertEquals(
            setOf(
                "id", "name", "latDeg", "lonDeg", "elevationM", "tzId",
                "note", "equipmentProfileId", "horizonProfile",
            ),
            spot.keys,
        )

        val profile = spot["horizonProfile"]!!.jsonObject
        assertEquals(
            setOf("sampleCount", "azimuthStepDeg", "elevationUnitDeg", "samples"),
            profile.keys,
        )
        assertEquals(360, profile["sampleCount"]!!.jsonPrimitive.content.toInt())
        assertEquals(1.0, profile["azimuthStepDeg"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(0.1, profile["elevationUnitDeg"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertEquals(360, profile["samples"]!!.jsonArray.size)

        val equipment = root["equipmentProfiles"]!!.jsonArray.single().jsonObject
        assertEquals(
            setOf("id", "name", "focalLengthMm", "sensorWidthMm", "sensorHeightMm"),
            equipment.keys,
        )

        assertEquals(listOf("m31", "ngc7000"), root["favouriteTargetIds"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun carriesNoSessionDataNoCacheAndNoActiveSelection() {
        val text = aDocument(listOf(aSpot(horizonProfileId = "h1")), mapOf("h1" to horizon()))

        for (forbidden in listOf("active", "session", "forecast", "elementSet", "noradId", "cloud", "cache")) {
            assertFalse(
                "the document must not carry '$forbidden'",
                text.lowercase().contains(forbidden.lowercase()),
            )
        }
    }

    @Test
    fun writesAFlatProfileAsAPresentProfileAndNoProfileAsNull() {
        val flat = HorizonProfile.flat("h1", "Flat", 59.9, 10.7)
        val withProfile = Json.parseToJsonElement(
            aDocument(listOf(aSpot(horizonProfileId = "h1")), mapOf("h1" to flat)),
        ).jsonObject
        val withoutProfile = Json.parseToJsonElement(aDocument(listOf(aSpot()))).jsonObject

        val present = withProfile["spots"]!!.jsonArray.single().jsonObject["horizonProfile"]!!.jsonObject
        assertEquals(360, present["samples"]!!.jsonArray.size)
        assertTrue(present["samples"]!!.jsonArray.all { it.jsonPrimitive.content == "0" })

        // `hasProfile = false` is a stated null, not an absent key: the two states stay distinguishable.
        val absent = withoutProfile["spots"]!!.jsonArray.single().jsonObject
        assertTrue(absent.containsKey("horizonProfile"))
        assertEquals("null", absent["horizonProfile"]!!.jsonPrimitive.content)
    }

    // ---------------------------------------------------------------------------------------------
    // Round trip
    // ---------------------------------------------------------------------------------------------

    @Test
    fun roundTripsASpotItsHorizonAndItsFavourites() {
        val original = aSpot(id = "s1", name = "Fjord", horizonProfileId = "h1")
        val originalHorizon = horizon(id = "h1", elevation = 6.5f, name = "Fjord")

        val bundle = decodedOf(
            aDocument(
                spots = listOf(original),
                horizons = mapOf("h1" to originalHorizon),
                favourites = setOf("ngc7000", "m31"),
            ),
        )

        val spot = bundle.spots.single()
        assertEquals("s1", spot.id)
        assertEquals("Fjord", spot.name)
        assertEquals(59.9, spot.latDeg, 1e-9)
        assertEquals(10.7, spot.lonDeg, 1e-9)
        assertEquals(12.0, spot.elevationM, 1e-9)
        assertEquals("Europe/Oslo", spot.tzId)
        assertEquals(EquipmentProfile.DEFAULT, spot.equipment)
        assertEquals(setOf("m31", "ngc7000"), bundle.favourites)

        // The document's horizon object carries no id, so the profile's id is derived from its spot —
        // deterministic, which is what makes re-importing the same file idempotent.
        val derivedId = requireNotNull(spot.horizonProfileId)
        assertEquals("horizon-s1", derivedId)
        val profile = requireNotNull(bundle.horizons[derivedId])
        assertEquals("Fjord", profile.name)
        assertEquals(59.9, profile.latitudeDeg, 1e-9)
        assertEquals(10.7, profile.longitudeDeg, 1e-9)
        assertArrayEquals(
            "the samples survive the document byte-for-byte",
            originalHorizon.encode(),
            profile.encode(),
        )
    }

    @Test
    fun roundTripsAPoleSpotWhereLatitudeIsExactlyNinety() {
        for (latDeg in listOf(90.0, -90.0)) {
            val bundle = decodedOf(aDocument(listOf(aSpot(id = "pole", name = "Pole", latDeg = latDeg))))
            assertEquals(latDeg, bundle.spots.single().latDeg, 0.0)
        }
    }

    @Test
    fun roundTripsANonDefaultEquipmentProfileAsADocumentProfile() {
        val lens = EquipmentProfile("Custom 50", 50.0, 36.0, 24.0, presetName = "50 mm full frame")
        val bundle = decodedOf(aDocument(listOf(aSpot(equipment = lens))))

        val resolved = bundle.spots.single().equipment
        assertEquals("Custom 50", resolved.name)
        assertEquals(50.0, resolved.focalLengthMm, 1e-9)
        assertEquals(36.0, resolved.sensorWidthMm, 1e-9)
        assertEquals(24.0, resolved.sensorHeightMm, 1e-9)
        // The field list has no key for `presetName`; the numbers and the name are lossless, the label is not.
        assertNull(resolved.presetName)
    }

    @Test
    fun writesOneEquipmentEntryPerDistinctProfileAndReferencesIt() {
        val first = EquipmentProfile.DEFAULT
        val second = EquipmentProfile("135 mm full frame", 135.0, 36.0, 24.0)

        val text = aDocument(
            listOf(
                aSpot(id = "a", name = "A", equipment = first),
                aSpot(id = "b", name = "B", equipment = first),
                aSpot(id = "c", name = "C", equipment = second),
            ),
        )
        val root = Json.parseToJsonElement(text).jsonObject
        assertEquals(2, root["equipmentProfiles"]!!.jsonArray.size)

        val bundle = decodedOf(text)
        val byId = bundle.spots.associateBy { it.id }
        assertEquals(first.focalLengthMm, byId.getValue("a").equipment.focalLengthMm, 1e-9)
        assertEquals(second.focalLengthMm, byId.getValue("c").equipment.focalLengthMm, 1e-9)

        val ids = root["spots"]!!.jsonArray.map { it.jsonObject["equipmentProfileId"]!!.jsonPrimitive.content }
        assertEquals(3, ids.size)
        assertEquals("two distinct ids across three spots", 2, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("eq-") })
    }

    @Test
    fun twoExportsOfOneStateAreByteIdentical() {
        val spots = listOf(aSpot(id = "b", name = "b"), aSpot(id = "a", name = "A"))
        val horizons = mapOf("h1" to horizon())

        val first = aDocument(spots, horizons, setOf("m31", "ngc7000"))
        val second = aDocument(spots.reversed(), horizons, setOf("ngc7000", "m31"))

        assertEquals(first, second)
    }

    // ---------------------------------------------------------------------------------------------
    // Typed refusals — never an exception
    // ---------------------------------------------------------------------------------------------

    @Test
    fun refusesAnEmptyOrBlankDocument() {
        assertEquals(SpotTransfer.SpotTransferErrorCode.EMPTY_DOCUMENT, refusalOf("").code)
        assertEquals(SpotTransfer.SpotTransferErrorCode.EMPTY_DOCUMENT, refusalOf("   ").code)
    }

    @Test
    fun refusesADocumentLargerThanTheCharacterCapBeforeParsingIt() {
        val huge = "x".repeat(SpotTransfer.MAX_DOCUMENT_CHARS + 1)
        val error = refusalOf(huge)

        assertEquals(SpotTransfer.SpotTransferErrorCode.TOO_LARGE, error.code)
        assertEquals("document", error.field)
    }

    @Test
    fun refusesEveryHostilePayloadWithATypedFailureAndNeverThrows() {
        val valid = aDocument(listOf(aSpot(horizonProfileId = "h1")), mapOf("h1" to horizon()))
        val hostile = listOf(
            "", " ", "\n", "null", "42", "true", "\"lumintra\"",
            "{}", "[]", "[1,2,3]", "{\"schemaVersion\":\"1\"}",
            "{ not json at all", "{\"schemaVersion\": 1,", valid.take(valid.length / 2),
            "{\"schemaVersion\":1,\"app\":\"lumintra\",\"spots\":\"not-an-array\"}",
            "{\"schemaVersion\":1,\"app\":3,\"spots\":[]}",
        )

        for (document in hostile) {
            val result = SpotTransfer.decodeDocument(document)
            assertTrue(
                "expected a typed refusal for ${document.take(24)}",
                result is SpotTransfer.DecodeResult.Refused,
            )
        }
    }

    @Test
    fun refusesASchemaVersionNewerThanTheAppAndNamesTheField() {
        val error = refusalOf(rawDocument(spots = "", schemaVersion = "${SpotTransfer.SUPPORTED_SCHEMA_VERSION + 1}"))

        assertEquals(SpotTransfer.SpotTransferErrorCode.UNKNOWN_SCHEMA_VERSION, error.code)
        assertEquals("schemaVersion", error.field)
        assertTrue(error.diagnostic.contains("found="))
    }

    @Test
    fun refusesASchemaVersionOlderThanTheOldestSupportedOne() {
        assertEquals(
            SpotTransfer.SpotTransferErrorCode.NOT_A_SCOUT_FILE,
            refusalOf(rawDocument(spots = "", schemaVersion = "0")).code,
        )
        assertEquals(
            SpotTransfer.SpotTransferErrorCode.NOT_A_SCOUT_FILE,
            refusalOf(rawDocument(spots = "", schemaVersion = "-1")).code,
        )
    }

    @Test
    fun refusesAForbiddenWriterTag() {
        val error = refusalOf(rawDocument(spots = "", app = "some-other-app"))
        assertEquals(SpotTransfer.SpotTransferErrorCode.NOT_A_SCOUT_FILE, error.code)
        assertEquals("app", error.field)
    }

    @Test
    fun refusesADocumentThatIsNotAScoutFileWithoutReadingItAsOne() {
        val error = refusalOf("[{\"schemaVersion\": 1}]")
        assertEquals(SpotTransfer.SpotTransferErrorCode.NOT_A_SCOUT_FILE, error.code)
        // `kotlinx.serialization` reports a character offset; the codec converts it to a 1-based line or
        // leaves it null rather than inventing one.
        assertTrue(error.line == null || error.line >= 1)
    }

    // ---------------------------------------------------------------------------------------------
    // Caps and per-record validation
    // ---------------------------------------------------------------------------------------------

    @Test
    fun refusesMoreSpotsThanTheCap() {
        val tooMany = (1..SpotTransfer.MAX_SPOTS + 1).map {
            aSpot(id = "s$it", name = "Spot $it", latDeg = 50.0 + it * 0.001)
        }
        val error = refusalOf(aDocument(tooMany))

        assertEquals(SpotTransfer.SpotTransferErrorCode.TOO_MANY_SPOTS, error.code)
        assertEquals("spots", error.field)
    }

    @Test
    fun refusesMoreEquipmentProfilesThanTheCap() {
        val entries = (1..SpotTransfer.MAX_EQUIPMENT_PROFILES + 1).joinToString(",") {
            """{"id":"eq-$it","name":"Lens $it","focalLengthMm":$it.0,"sensorWidthMm":36.0,"sensorHeightMm":24.0}"""
        }
        val error = refusalOf(
            rawDocument(
                spots = rawSpot(equipmentProfileId = "\"eq-1\""),
                equipmentProfiles = entries,
            ),
        )

        assertEquals(SpotTransfer.SpotTransferErrorCode.TOO_MANY_EQUIPMENT_PROFILES, error.code)
        assertEquals("equipmentProfiles", error.field)
    }

    @Test
    fun refusesMoreFavouritesThanTheCap() {
        val ids = (1..SpotTransfer.MAX_FAVOURITES + 1).joinToString(",") { "\"t$it\"" }
        val error = refusalOf(rawDocument(spots = "", favouriteTargetIds = ids))

        assertEquals(SpotTransfer.SpotTransferErrorCode.TOO_MANY_FAVOURITES, error.code)
        assertEquals("favouriteTargetIds", error.field)
    }

    @Test
    fun refusesAHorizonArrayThatIsNotExactlyThreeHundredAndSixtyEntries() {
        for (count in listOf(0, 1, 359, 361, 720)) {
            val error = refusalOf(
                rawDocument(rawSpot(horizonProfile = rawHorizon(samplesOf(count)))),
            )
            assertEquals(
                "a $count-entry array must be refused, not padded",
                SpotTransfer.SpotTransferErrorCode.WRONG_HORIZON_SAMPLE_COUNT,
                error.code,
            )
            assertEquals("horizonProfile.samples", error.field)
            assertEquals(0, error.recordIndex)
            assertTrue(error.diagnostic.contains("samples=$count"))
        }
    }

    @Test
    fun refusesADeclaredSampleCountThatDisagreesWithTheArray() {
        val error = refusalOf(
            rawDocument(rawSpot(horizonProfile = rawHorizon(samplesOf(360), sampleCount = 720))),
        )

        assertEquals(SpotTransfer.SpotTransferErrorCode.WRONG_HORIZON_SAMPLE_COUNT, error.code)
        assertEquals("horizonProfile.sampleCount", error.field)
        assertTrue(error.diagnostic.contains("sampleCount=720"))
    }

    @Test
    fun refusesAHorizonSampleOutsideTheStorableRange() {
        val samples = samplesOf(HorizonProfile.SAMPLE_COUNT).toMutableList().apply { this[17] = 901 }
        val error = refusalOf(rawDocument(rawSpot(horizonProfile = rawHorizon(samples))))

        assertEquals(SpotTransfer.SpotTransferErrorCode.HORIZON_SAMPLE_OUT_OF_RANGE, error.code)
        assertEquals("horizonProfile.samples", error.field)
        assertTrue(error.diagnostic.contains("value=901"))

        val tooLow = samplesOf(HorizonProfile.SAMPLE_COUNT).toMutableList().apply { this[17] = -101 }
        assertEquals(
            SpotTransfer.SpotTransferErrorCode.HORIZON_SAMPLE_OUT_OF_RANGE,
            refusalOf(rawDocument(rawSpot(horizonProfile = rawHorizon(tooLow)))).code,
        )
    }

    @Test
    fun refusesAHorizonCodecTheAppDoesNotImplement() {
        val horizon = """{ "sampleCount": 360, "azimuthStepDeg": 0.5, "elevationUnitDeg": 0.1, """ +
            """"samples": [${samplesOf(360).joinToString(",")}] }"""
        val error = refusalOf(rawDocument(rawSpot(horizonProfile = horizon)))

        assertEquals(SpotTransfer.SpotTransferErrorCode.UNSUPPORTED_HORIZON_ENCODING, error.code)
        assertEquals("horizonProfile", error.field)
    }

    @Test
    fun refusesALatitudeOutsideTheClosedRangeAndNamesTheRecordAndField() {
        val error = refusalOf(rawDocument(rawSpot(latDeg = "91.0")))

        assertEquals(SpotTransfer.SpotTransferErrorCode.COORDINATE_OUT_OF_RANGE, error.code)
        assertEquals("latDeg", error.field)
        assertEquals(0, error.recordIndex)
    }

    @Test
    fun refusesALongitudeOutsideTheRangeAndALongitudeOfTheWrongShape() {
        val tooFarWest = refusalOf(rawDocument(rawSpot(lonDeg = "-180.5")))
        assertEquals(SpotTransfer.SpotTransferErrorCode.COORDINATE_OUT_OF_RANGE, tooFarWest.code)
        assertEquals("lonDeg", tooFarWest.field)

        val tooFarEast = refusalOf(rawDocument(rawSpot(latDeg = "0.0", lonDeg = "181.0")))
        assertEquals(SpotTransfer.SpotTransferErrorCode.COORDINATE_OUT_OF_RANGE, tooFarEast.code)
        assertEquals("lonDeg", tooFarEast.field)
    }

    @Test
    fun acceptsTheTwoClosedEdgesOfLatitudeAndLongitude() {
        // Every edge is a legal spot: the ranges are closed on both ends, which is why a pole imports.
        for (latDeg in listOf("90.0", "-90.0", "0.0", "59.9123")) {
            assertEquals(1, decodedOf(rawDocument(rawSpot(latDeg = latDeg))).spots.size)
        }
        for (lonDeg in listOf("180.0", "-180.0", "0.0")) {
            assertEquals(1, decodedOf(rawDocument(rawSpot(lonDeg = lonDeg))).spots.size)
        }
    }

    @Test
    fun refusesABlankNameAnEmptyIdAnOverlongNoteAndAnEmptyZone() {
        assertEquals(
            SpotTransfer.SpotTransferErrorCode.EMPTY_NAME,
            refusalOf(rawDocument(rawSpot(name = "   "))).code,
        )
        assertEquals(
            SpotTransfer.SpotTransferErrorCode.MISSING_REQUIRED_FIELD,
            refusalOf(rawDocument(rawSpot(id = ""))).code,
        )

        val longNote = "n".repeat(SpotTransfer.MAX_NOTE_LENGTH + 1)
        val noteError = refusalOf(rawDocument(rawSpot(note = "\"$longNote\"")))
        assertEquals(SpotTransfer.SpotTransferErrorCode.NOTE_TOO_LONG, noteError.code)
        assertEquals("note", noteError.field)

        val zoneError = refusalOf(rawDocument(rawSpot(tzId = "")))
        assertEquals(SpotTransfer.SpotTransferErrorCode.INVALID_RECORD, zoneError.code)
        assertEquals("tzId", zoneError.field)
    }

    @Test
    fun acceptsANoteThatIsNullAndOneWithinTheCap() {
        assertEquals(1, decodedOf(rawDocument(rawSpot(note = "null"))).spots.size)
        assertEquals(1, decodedOf(rawDocument(rawSpot(note = "\"windy at the ridge\""))).spots.size)
    }

    @Test
    fun refusesADuplicateSpotId() {
        val error = refusalOf(
            rawDocument(spots = rawSpot(id = "s1") + "," + rawSpot(id = "s1", name = "Other")),
        )

        assertEquals(SpotTransfer.SpotTransferErrorCode.DUPLICATE_SPOT_ID, error.code)
        assertEquals("id", error.field)
        assertEquals(1, error.recordIndex)
    }

    @Test
    fun refusesADuplicateEquipmentProfileId() {
        val entries = """{"id":"eq-1","name":"A","focalLengthMm":24.0,"sensorWidthMm":36.0,"sensorHeightMm":24.0},""" +
            """{"id":"eq-1","name":"B","focalLengthMm":50.0,"sensorWidthMm":36.0,"sensorHeightMm":24.0}"""
        val error = refusalOf(rawDocument(spots = "", equipmentProfiles = entries))

        assertEquals(SpotTransfer.SpotTransferErrorCode.DUPLICATE_EQUIPMENT_PROFILE_ID, error.code)
        assertEquals("equipmentProfiles.id", error.field)
    }

    @Test
    fun refusesAnEquipmentProfileWithAnImpossibleLensOrSensor() {
        val entries = """{"id":"eq-1","name":"Bad","focalLengthMm":0.0,"sensorWidthMm":36.0,"sensorHeightMm":24.0}"""
        assertEquals(
            SpotTransfer.SpotTransferErrorCode.INVALID_RECORD,
            refusalOf(rawDocument(spots = "", equipmentProfiles = entries)).code,
        )
    }

    @Test
    fun refusesASpotWhoseEquipmentProfileIsNotInTheDocument() {
        val error = refusalOf(rawDocument(rawSpot(equipmentProfileId = "\"eq-missing\"")))

        assertEquals(SpotTransfer.SpotTransferErrorCode.UNKNOWN_EQUIPMENT_PROFILE, error.code)
        assertEquals("equipmentProfileId", error.field)
        assertEquals(0, error.recordIndex)
    }

    @Test
    fun resolvesAnEquipmentProfileTheDocumentDoesCarry() {
        val entries = """{"id":"eq-1","name":"Wide","focalLengthMm":14.0,"sensorWidthMm":36.0,"sensorHeightMm":24.0}"""
        val bundle = decodedOf(
            rawDocument(spots = rawSpot(equipmentProfileId = "\"eq-1\""), equipmentProfiles = entries),
        )

        assertEquals("Wide", bundle.spots.single().equipment.name)
        assertEquals(14.0, bundle.spots.single().equipment.focalLengthMm, 1e-9)
    }

    @Test
    fun refusesAFavouriteIdThatIsLongerThanTheIdentifierCap() {
        val error = refusalOf(
            rawDocument(spots = "", favouriteTargetIds = "\"${"t".repeat(SpotTransfer.MAX_ID_LENGTH + 1)}\""),
        )

        assertEquals(SpotTransfer.SpotTransferErrorCode.INVALID_RECORD, error.code)
        assertEquals("favouriteTargetIds", error.field)
    }

    // ---------------------------------------------------------------------------------------------
    // All-or-nothing import, and the merge/replace counts
    // ---------------------------------------------------------------------------------------------

    @Test
    fun refusesAWholeDocumentWhenOneRecordIsInvalidAndImportsNothing() {
        val document = rawDocument(
            spots = rawSpot(id = "good", name = "Good") + "," + rawSpot(id = "bad", name = "Bad", latDeg = "91.0"),
        )

        assertEquals(SpotTransfer.SpotTransferErrorCode.COORDINATE_OUT_OF_RANGE, refusalOf(document).code)
        assertEquals(1, refusalOf(document).recordIndex)

        val plan = planOf(document, existingSpots = listOf(aSpot(id = "kept")))
        val refused = requireNotNull(plan as? SpotTransfer.ImportPlan.Refused) {
            "a document with one invalid record must refuse the whole import"
        }
        assertEquals(SpotTransfer.SpotTransferErrorCode.COORDINATE_OUT_OF_RANGE, refused.error.code)
        assertEquals(1, refused.error.recordIndex)
    }

    @Test
    fun mergeSkipsARecordWithTheSameNameAndCoordinatesAndCountsBoth() {
        val existing = listOf(aSpot(id = "old-id", name = "Home", latDeg = 59.9, lonDeg = 10.7))
        val document = aDocument(
            listOf(
                aSpot(id = "new-id", name = "Home", latDeg = 59.9, lonDeg = 10.7),
                aSpot(id = "cabin", name = "Cabin", latDeg = 60.1, lonDeg = 10.9),
            ),
        )

        val ready = readyOf(planOf(document, existingSpots = existing))
        assertEquals(SpotTransfer.ImportMode.MERGE, ready.mode)
        assertEquals(1, ready.added)
        assertEquals(1, ready.skipped)
        assertEquals(listOf("cabin"), ready.spots.map { it.id })
    }

    @Test
    fun mergeDeDuplicatesWithinTheDocumentItselfToo() {
        val document = aDocument(
            listOf(
                aSpot(id = "one", name = "Home", latDeg = 59.9, lonDeg = 10.7),
                aSpot(id = "two", name = "Home", latDeg = 59.9, lonDeg = 10.7),
            ),
        )

        val ready = readyOf(planOf(document))
        assertEquals(1, ready.added)
        assertEquals(1, ready.skipped)
    }

    @Test
    fun replaceAllCountsEveryRecordAndSkipsNone() {
        val existing = listOf(aSpot(id = "old-id", name = "Home", latDeg = 59.9, lonDeg = 10.7))
        val document = aDocument(
            listOf(
                aSpot(id = "new-id", name = "Home", latDeg = 59.9, lonDeg = 10.7),
                aSpot(id = "cabin", name = "Cabin", latDeg = 60.1, lonDeg = 10.9),
            ),
        )

        val ready = readyOf(planOf(document, existingSpots = existing, mode = SpotTransfer.ImportMode.REPLACE_ALL))
        assertEquals(SpotTransfer.ImportMode.REPLACE_ALL, ready.mode)
        assertEquals(2, ready.added)
        assertEquals(0, ready.skipped)
        assertEquals(listOf("cabin", "new-id"), ready.spots.map { it.id })
    }

    @Test
    fun mergesFavouritesAsAUnionAndNeverRemovesOne() {
        val document = aDocument(listOf(aSpot()), favourites = setOf("m31", "ngc7000"))

        val ready = readyOf(planOf(document, existingFavourites = setOf("m31")))
        assertEquals(setOf("m31", "ngc7000"), ready.favourites)
        assertEquals(1, ready.favouritesAdded)

        val noNewOnes = readyOf(planOf(document, existingFavourites = setOf("m31", "ngc7000")))
        assertEquals(setOf("m31", "ngc7000"), noNewOnes.favourites)
        assertEquals(0, noNewOnes.favouritesAdded)
    }

    @Test
    fun carriesTheHorizonsOfTheRecordsItWouldAdd() {
        val document = aDocument(
            spots = listOf(aSpot(id = "s1", name = "Fjord", horizonProfileId = "h1")),
            horizons = mapOf("h1" to horizon(id = "h1", elevation = 8.0f, name = "Fjord")),
        )

        val ready = readyOf(planOf(document))
        val profile = requireNotNull(ready.horizons["horizon-s1"])
        assertEquals(8.0, profile.altitudeDeg[0].toDouble(), 1e-6)
        assertEquals("horizon-s1", ready.spots.single().horizonProfileId)
    }

    @Test
    fun aRefusedPlanIsNotAReadyPlanWithZeroCounts() {
        val plan = planOf("[not json]")
        assertTrue(plan is SpotTransfer.ImportPlan.Refused)
        assertFalse(plan is SpotTransfer.ImportPlan.Ready)
    }

    // ---------------------------------------------------------------------------------------------
    // The per-record store codec ([ScoutStore] stores one JSON string per spot and per horizon)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theStoreRecordKeepsWhatTheDocumentFieldListCannotCarry() {
        val lens = EquipmentProfile("Custom 50", 50.0, 36.0, 24.0, presetName = "50 mm full frame")
        val original = aSpot(
            id = "s1",
            name = "Home",
            horizonProfileId = "h1",
            equipment = lens,
            approximate = true,
        )

        val restored = requireNotNull(SpotTransfer.decodeSpotRecord(SpotTransfer.encodeSpotRecord(original)))
        assertEquals(original, restored)
        assertTrue("the store record keeps the approximate flag", restored.approximate)
        assertEquals("50 mm full frame", restored.equipment.presetName)
        assertEquals("h1", restored.horizonProfileId)
    }

    @Test
    fun aCorruptStoreRecordReadsAsNullSoOneBadRecordCostsOneSpot() {
        for (corrupt in listOf("", "{", "not json", "{}", "{\"id\":\"s1\"}", "null")) {
            assertNull("'$corrupt' must be quarantined, not thrown", SpotTransfer.decodeSpotRecord(corrupt))
        }
    }

    @Test
    fun theHorizonStoreRecordRoundTripsIncludingItsIdentityAndPlace() {
        val original = horizon(id = "h1", elevation = -3.5f, name = "Ridge")

        val restored = requireNotNull(SpotTransfer.decodeHorizonRecord(SpotTransfer.encodeHorizonRecord(original)))
        assertEquals(original, restored)
        assertEquals("h1", restored.id)
        assertEquals("Ridge", restored.name)
        assertEquals(59.9, restored.latitudeDeg, 0.0)
    }

    @Test
    fun aCorruptHorizonStoreRecordReadsAsNull() {
        for (corrupt in listOf("", "{", "not json", "{}", "null")) {
            assertNull(SpotTransfer.decodeHorizonRecord(corrupt))
        }

        // A record whose declared sample count disagrees with its array is corrupt too, not padded.
        val encoded = SpotTransfer.encodeHorizonRecord(horizon())
        assertTrue(encoded.contains("\"sampleCount\": 360"))
        assertNull(
            SpotTransfer.decodeHorizonRecord(encoded.replace("\"sampleCount\": 360", "\"sampleCount\": 359")),
        )
    }

    @Test
    fun aStoreRecordFromANewerBuildIsQuarantinedRatherThanMisread() {
        // The on-device format is versioned for the same reason the document is: a record written by a
        // build that changed a field's meaning must not be decoded under this build's meaning.
        val spotRecord = SpotTransfer.encodeSpotRecord(aSpot())
        val horizonRecord = SpotTransfer.encodeHorizonRecord(horizon())

        assertEquals("the record is versioned", 1, Regex("\"schemaVersion\"").findAll(spotRecord).count())
        assertTrue(
            "the spot record carries its own version first",
            spotRecord.trimStart('{', '\n', ' ').startsWith("\"schemaVersion\": ${SpotTransfer.SUPPORTED_SCHEMA_VERSION}"),
        )
        assertTrue(
            "the horizon record carries its own version first",
            horizonRecord.trimStart('{', '\n', ' ').startsWith("\"schemaVersion\": ${SpotTransfer.SUPPORTED_SCHEMA_VERSION}"),
        )

        // This build's version (and one field removed from the front) still reads.
        assertNotNull(SpotTransfer.decodeSpotRecord(spotRecord))
        assertNotNull(SpotTransfer.decodeHorizonRecord(horizonRecord))

        val newerVersion = "\"schemaVersion\": ${SpotTransfer.SUPPORTED_SCHEMA_VERSION + 1}"
        val spotRefused = spotRecord.replace(
            "\"schemaVersion\": ${SpotTransfer.SUPPORTED_SCHEMA_VERSION}",
            newerVersion,
        )
        val horizonRefused = horizonRecord.replace(
            "\"schemaVersion\": ${SpotTransfer.SUPPORTED_SCHEMA_VERSION}",
            newerVersion,
        )
        assertNull("a newer spot record must be quarantined", SpotTransfer.decodeSpotRecord(spotRefused))
        assertNull("a newer horizon record must be quarantined", SpotTransfer.decodeHorizonRecord(horizonRefused))
    }

    @Test
    fun theFavouriteIdsEntryRoundTripsSortedAndRefusesCorruption() {
        assertEquals(setOf("a", "b"), SpotTransfer.decodeFavouriteIds(SpotTransfer.encodeFavouriteIds(setOf("b", "a"))))
        assertEquals(emptySet<String>(), SpotTransfer.decodeFavouriteIds("not json"))
        assertEquals(emptySet<String>(), SpotTransfer.decodeFavouriteIds("{}"))
        assertEquals(setOf("m31"), SpotTransfer.decodeFavouriteIds("[\"m31\", \"\", \" \"]"))
    }

    @Test
    fun theCapsTheWriterPublishesAreTheOnesTheReaderEnforces() {
        assertEquals(360, HorizonProfile.SAMPLE_COUNT)
        assertEquals(-100, HorizonProfile.MIN_STORED)
        assertEquals(900, HorizonProfile.MAX_STORED)
        assertEquals(4_000_000, SpotTransfer.MAX_DOCUMENT_CHARS)
        assertTrue(SpotTransfer.MAX_SPOTS in 1..10_000)
        assertTrue(SpotTransfer.MAX_DOCUMENT_CHARS > SpotTransfer.MAX_SPOTS * HorizonProfile.ENCODED_BYTES / 2)

        // The generous case must be admitted: a document with a horizon profile on every one of 200 spots
        // is well inside both the spot cap and the character cap, so the cap is not a de-facto size limit.
        val many = (1..200).map { aSpot(id = "s$it", name = "Spot $it", latDeg = 50.0 + it * 0.001, horizonProfileId = "h$it") }
        val horizons = many.associate { it.horizonProfileId!! to horizon(id = it.horizonProfileId!!, elevation = 2.0f) }
        val text = aDocument(many, horizons)

        assertTrue(
            "a 200-spot document must be inside the character cap, was ${text.length}",
            text.length < SpotTransfer.MAX_DOCUMENT_CHARS,
        )
        assertEquals(200, decodedOf(text).spots.size)
    }
}
