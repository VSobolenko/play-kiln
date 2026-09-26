package com.gexetr.pipesizing.ui.cases

import com.gexetr.pipesizing.monetization.MonetizationConfig
import com.gexetr.pipesizing.ui.home.CaseKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedCaseTest {

    @Test
    fun aCaseSurvivesTheStorageFormat() {
        val original = SavedCase(
            id = "case-1",
            name = "Boiler room, north riser",
            savedAtEpochMillis = 1_700_000_000_000L,
            fields = mapOf(
                CaseKeys.MATERIAL to "steel",
                CaseKeys.LOAD to "100",
                CaseKeys.METHOD to "CAPACITY_COMPARISON",
            ),
        )

        val restored = SavedCaseCodec.decode(SavedCaseCodec.encode(listOf(original)))

        assertEquals(listOf(original), restored)
    }

    @Test
    fun aNameCarryingTheFormatSeparatorsCannotSplitARecord() {
        // Every character the format uses for structure is inside this case on purpose. If the codec
        // did not escape them, this single case would decode as several broken ones.
        val hostile = SavedCase(
            id = "case-2",
            name = "Line\nbreak | pipe ; semi = eq \\ backslash",
            savedAtEpochMillis = 1L,
            fields = mapOf("k|ey;=" to "v\nal|ue;="),
        )

        val encoded = SavedCaseCodec.encode(listOf(hostile))

        // The name and the values are stored escaped, so no raw record separator reaches the text.
        assertEquals(0, encoded.count { it == '\n' })
        assertEquals(listOf(hostile), SavedCaseCodec.decode(encoded))
    }

    @Test
    fun anUnreadableRecordIsDroppedWithoutLosingTheReadableOnes() {
        val first = SavedCase("a", "First", 10L, mapOf("x" to "1"))
        val third = SavedCase("c", "Third", 30L, mapOf("x" to "3"))
        // A truncated record in the middle is what a killed write leaves behind.
        val stored = SavedCaseCodec.encode(listOf(first)) + "\n" + "garbage-without-timestamp" + "\n" +
            SavedCaseCodec.encode(listOf(third))

        val decoded = SavedCaseCodec.decode(stored)

        assertEquals(listOf(first, third), decoded)
    }

    @Test
    fun aTimestampThatIsNotANumberDropsOnlyItsOwnRecord() {
        val stored = "id|name|not-a-number|key=value"

        assertTrue(SavedCaseCodec.decode(stored).isEmpty())
    }

    @Test
    fun blankStorageDecodesToNothing() {
        assertTrue(SavedCaseCodec.decode("").isEmpty())
        assertTrue(SavedCaseCodec.decode("\n\n").isEmpty())
    }

    @Test
    fun theLibraryShowsNewestFirstAndDeleteRemovesExactlyOne() = runBlocking {
        val older = SavedCase("older", "Older", 10L, emptyMap())
        val newer = SavedCase("newer", "Newer", 20L, emptyMap())
        val repository = CasesRepository.InMemory(listOf(older, newer))

        assertEquals(listOf("newer", "older"), repository.cases.first().map { it.id })

        repository.delete("newer")

        // The deleted case is gone and the one beside it is untouched.
        assertEquals(listOf("older"), repository.cases.first().map { it.id })
        assertNull(repository.get("newer"))
        assertEquals("older", repository.get("older")?.id)
    }

    @Test
    fun upsertingTheSameIdReplacesRatherThanDuplicates() = runBlocking {
        val repository = CasesRepository.InMemory()
        val case = SavedCase("same", "Before", 10L, emptyMap())

        repository.upsert(case)
        repository.upsert(case.copy(name = "After", savedAtEpochMillis = 20L))

        assertEquals(listOf("After"), repository.cases.first().map { it.name })
    }

    @Test
    fun theFreeTierStopsAtTheConfiguredLimitAndProDoesNot() {
        val limit = MonetizationConfig.FREE_CASE_LIMIT

        assertTrue("the first cases are free", canAddCase(0, limit, pro = false))
        assertTrue("the last case under the limit is free", canAddCase(limit - 1, limit, pro = false))
        assertFalse("the case that reaches the limit is not free", canAddCase(limit, limit, pro = false))
        assertFalse("a case over the limit is not free", canAddCase(limit + 5, limit, pro = false))
        assertTrue("pro lifts the limit", canAddCase(limit + 5, limit, pro = true))
    }
}
