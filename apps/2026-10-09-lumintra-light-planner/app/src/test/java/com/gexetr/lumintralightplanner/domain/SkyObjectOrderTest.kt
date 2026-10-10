package com.gexetr.lumintralightplanner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalogue's one order and its search predicate.
 *
 * `specs/targets/spec.md`: `CATALOGUE_ORDER` is **magnitude ascending, magnitude-unknown rows last,
 * then by `id`** — the list's declared order, never a per-night ranking. The magnitude used is V when
 * known, else B, else "unknown"; a row with no magnitude is never given a fabricated one.
 */
class SkyObjectOrderTest {

    private fun obj(
        id: String,
        magV: Double? = null,
        magB: Double? = null,
        name: String = id,
        messier: String? = null,
        ngc: String? = null,
        ic: String? = null,
        nameRu: String? = null,
        aliases: List<String> = emptyList(),
        commonNames: List<String> = emptyList(),
        sizeMajArcmin: Double? = null,
        raDeg: Double = 10.0,
        decDeg: Double = 40.0,
        type: ObjectType = ObjectType.GALAXY,
    ) = SkyObject(
        id = id,
        messier = messier,
        ngc = ngc,
        ic = ic,
        name = name,
        nameRu = nameRu,
        type = type,
        raDeg = raDeg,
        decDeg = decDeg,
        magV = magV,
        magB = magB,
        sizeMajArcmin = sizeMajArcmin,
        aliases = aliases,
        commonNames = commonNames,
    )

    // ---------------------------------------------------------------- the magnitude

    @Test
    fun displayMagnitudePrefersVThenBFallingBackToNull() {
        assertEquals(3.4, obj("a", magV = 3.4).displayMagnitude!!, 1e-12)
        assertEquals(4.4, obj("b", magB = 4.4).displayMagnitude!!, 1e-12)
        assertEquals(3.4, obj("c", magV = 3.4, magB = 4.4).displayMagnitude!!, 1e-12)
        assertNull(obj("d").displayMagnitude)
        assertTrue(obj("d").hasUnknownMagnitude)
        assertFalse(obj("a", magV = 3.4).hasUnknownMagnitude)
    }

    // ------------------------------------------------------------------- the order

    @Test
    fun theCatalogueOrderIsMagnitudeAscending() {
        val objects = listOf(
            obj("dim", magV = 8.2),
            obj("bright", magV = 1.4),
            obj("middle", magV = 5.0),
        )
        assertEquals(
            listOf("bright", "middle", "dim"),
            objects.sortedWith(SkyObject.CATALOGUE_ORDER).map { it.id },
        )
    }

    @Test
    fun magnitudeUnknownRowsSortLastEvenBehindTheDimmestKnownOne() {
        val objects = listOf(
            obj("unknown"),
            obj("dim", magV = 12.9),
            obj("bright", magV = 0.03),
        )
        assertEquals(
            listOf("bright", "dim", "unknown"),
            objects.sortedWith(SkyObject.CATALOGUE_ORDER).map { it.id },
        )
    }

    @Test
    fun aBOnlyRowSortsOnItsBMagnitudeRatherThanAsUnknown() {
        val objects = listOf(
            obj("unknown"),
            obj("bOnly", magB = 2.0),
            obj("vRow", magV = 6.0),
        )
        assertEquals(
            listOf("bOnly", "vRow", "unknown"),
            objects.sortedWith(SkyObject.CATALOGUE_ORDER).map { it.id },
        )
    }

    @Test
    fun anEqualMagnitudeIsBrokenByIdAscending() {
        val objects = listOf(
            obj("m33", magV = 5.72),
            obj("m31", magV = 5.72),
            obj("m110", magV = 5.72),
        )
        assertEquals(
            listOf("m110", "m31", "m33"),
            objects.sortedWith(SkyObject.CATALOGUE_ORDER).map { it.id },
        )
    }

    @Test
    fun twoUnknownMagnitudesAreAlsoBrokenById() {
        val objects = listOf(obj("zeta"), obj("alpha"))
        assertEquals(
            listOf("alpha", "zeta"),
            objects.sortedWith(SkyObject.CATALOGUE_ORDER).map { it.id },
        )
    }

    // ------------------------------------------------------------------- the search

    @Test
    fun anEmptyOrBlankQueryMatchesEverything() {
        val o = obj("m31", messier = "31", name = "Andromeda Galaxy")
        assertTrue(SkyObject.matchesQuery(o, ""))
        assertTrue(SkyObject.matchesQuery(o, "   "))
    }

    @Test
    fun theQueryIsCaseInsensitiveAndSubstringBased() {
        val o = obj("m31", messier = "31", name = "Andromeda Galaxy", ngc = "224")
        assertTrue(SkyObject.matchesQuery(o, "andromeda"))
        assertTrue(SkyObject.matchesQuery(o, "ANDROMEDA"))
        assertTrue(SkyObject.matchesQuery(o, "dromed"))
        assertTrue(SkyObject.matchesQuery(o, "m31"))
        assertTrue(SkyObject.matchesQuery(o, "messier 31"))
        assertTrue(SkyObject.matchesQuery(o, "ngc224"))
        assertTrue(SkyObject.matchesQuery(o, "NGC 224"))
        assertTrue(SkyObject.matchesQuery(o, "  m31  "))
        assertFalse(SkyObject.matchesQuery(o, "orion"))
    }

    @Test
    fun theSearchAlsoReachesTheRussianNameAliasesAndCommonNames() {
        val o = obj(
            id = "m42",
            name = "Orion Nebula",
            nameRu = "Туманность Ориона",
            aliases = listOf("NGC 1976"),
            commonNames = listOf("Great Nebula"),
        )
        assertTrue(SkyObject.matchesQuery(o, "туманность"))
        assertTrue(SkyObject.matchesQuery(o, "ориона"))
        assertTrue(SkyObject.matchesQuery(o, "ngc 1976"))
        assertTrue(SkyObject.matchesQuery(o, "great nebula"))
        assertFalse(SkyObject.matchesQuery(o, "andromeda"))
    }

    @Test
    fun searchableTermsCarriesEveryStringTheQueryMayMatch() {
        val o = obj(
            id = "m31",
            messier = "31",
            ngc = "224",
            ic = "10",
            name = "Andromeda Galaxy",
            nameRu = "Галактика Андромеды",
            aliases = listOf("UGC 454"),
            commonNames = listOf("Andromeda"),
        )
        val terms = o.searchableTerms
        assertTrue("m31" in terms)
        assertTrue("Andromeda Galaxy" in terms)
        assertTrue("Галактика Андромеды" in terms)
        assertTrue("m31" in terms)
        assertTrue("messier 31" in terms)
        assertTrue("ngc224" in terms)
        assertTrue("ngc 224" in terms)
        assertTrue("ic10" in terms)
        assertTrue("ic 10" in terms)
        assertTrue("UGC 454" in terms)
        assertTrue("Andromeda" in terms)
        // The bare id is always searchable, even when every other designation is absent.
        assertTrue("idOnly" in obj("idOnly").searchableTerms)
    }

    // ------------------------------------------------------------- other derivations

    @Test
    fun designationPrefersMessierThenNgcThenIcThenTheId() {
        assertEquals("M31", obj("m31", messier = "31").designation)
        assertEquals("NGC 7000", obj("ngc7000", ngc = "7000").designation)
        assertEquals("IC 1396", obj("ic1396", ic = "1396").designation)
        assertEquals("sh2-155", obj("sh2-155").designation)
    }

    @Test
    fun angularSizeAndPointSourceFollowTheStatedSize() {
        assertEquals(0.25, obj("a", sizeMajArcmin = 15.0).angularSizeMajDeg!!, 1e-12)
        assertNull(obj("b").angularSizeMajDeg)
        assertTrue(obj("b").isPointSource)
        assertTrue(obj("c", sizeMajArcmin = 0.0).isPointSource)
        assertFalse(obj("d", sizeMajArcmin = 15.0).isPointSource)
    }

    @Test
    fun raHoursIsTheEnginesUnitAndRaDegIsTheStoredOne() {
        val o = obj("a", raDeg = 10.5)
        assertEquals(10.5, o.raDeg, 1e-12)
        assertEquals(0.7, o.raHours, 1e-12)
        assertEquals(10.5, o.equatorial.raDeg, 1e-12)
    }

    @Test
    fun objectTypeWireNamesAndTheComputedOnlySplit() {
        assertEquals("globular_cluster", ObjectType.GLOBULAR_CLUSTER.wireName)
        assertEquals(ObjectType.PLANETARY_NEBULA, ObjectType.fromWire("planetary_nebula"))
        assertNull(ObjectType.fromWire("not_a_type"))
        assertTrue(ObjectType.PLANET.isComputedOnly)
        assertTrue(ObjectType.MOON.isComputedOnly)
        assertTrue(ObjectType.SUN.isComputedOnly)
        assertFalse(ObjectType.GALAXY.isComputedOnly)
        assertFalse(ObjectType.AUTHORED_TYPES.any { it.isComputedOnly })
        assertTrue(ObjectType.AUTHORED_TYPES.size == ObjectType.entries.size - 3)
    }

    @Test
    fun sourceIdsAndVerificationDefaultToAbsent() {
        val ids = SourceIds()
        assertNull(ids.wikidata)
        assertNull(ids.heasarc)
        assertNull(ids.simbad)
        assertNull(obj("a").verification)
        assertTrue(obj("a").aliases.isEmpty())
        assertTrue(obj("a").commonNames.isEmpty())
    }
}
