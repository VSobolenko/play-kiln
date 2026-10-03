package com.gexetr.laneset.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachSetParserTest {

    @Test
    fun parsesAMultiLinePracticeIntoSectionsAndIntervals() {
        val text = """
            Warm up 300 free
            8x100 free on 1:45
            4x50 kick @1:10
            Cool down 200 free
        """.trimIndent()

        val session = CoachSetParser.parse(text, PACE)

        val warmUp = session.section(SectionKind.WARM_UP)?.blocks.orEmpty()
        assertEquals(1, warmUp.size)
        assertEquals(300, warmUp[0].distanceMeters)
        assertEquals(1, warmUp[0].repetitions)
        assertEquals(Stroke.FREESTYLE, warmUp[0].stroke)
        assertEquals(255, warmUp[0].targetTimeSeconds)

        val main = session.section(SectionKind.MAIN_SET)?.blocks.orEmpty()
        assertEquals(2, main.size)

        assertEquals(8, main[0].repetitions)
        assertEquals(100, main[0].distanceMeters)
        assertEquals(Stroke.FREESTYLE, main[0].stroke)
        assertEquals(85, main[0].targetTimeSeconds)
        // Send-off 1:45 = 105 s, target 85 s, so 20 s rest.
        assertEquals(20, main[0].restSeconds)

        assertEquals(4, main[1].repetitions)
        assertEquals(50, main[1].distanceMeters)
        // "kick" is not a stroke word, so the default stroke stands.
        assertEquals(Stroke.FREESTYLE, main[1].stroke)
        // Send-off @1:10 = 70 s, target 42.5 s -> 43 s, so 27 s rest.
        assertEquals(43, main[1].targetTimeSeconds)
        assertEquals(27, main[1].restSeconds)

        val coolDown = session.section(SectionKind.COOL_DOWN)?.blocks.orEmpty()
        assertEquals(1, coolDown.size)
        assertEquals(200, coolDown[0].distanceMeters)
    }

    @Test
    fun recognisesStrokeNamesAfterTheDistance() {
        val session = CoachSetParser.parse("8x50 back\n6x100 breast", PACE)

        val main = session.section(SectionKind.MAIN_SET)?.blocks.orEmpty()
        assertEquals(2, main.size)
        assertEquals(Stroke.BACKSTROKE, main[0].stroke)
        assertEquals(Stroke.BREASTSTROKE, main[1].stroke)
    }

    @Test
    fun aPracticeWithNoHeadersIsAllMainSet() {
        val session = CoachSetParser.parse("6x200 free on 3:00", PACE)

        assertEquals(1, session.sections.size)
        assertEquals(SectionKind.MAIN_SET, session.sections.first().kind)
    }

    @Test
    fun everyParsedIntervalCarriesATargetTime() {
        val session = CoachSetParser.parse("Warm up 300 free\n8x100 free on 1:45", PACE)

        assertTrue(session.blocks.isNotEmpty())
        assertTrue(session.blocks.all { it.targetTimeSeconds > 0 })
    }

    @Test
    fun theParsedSessionStatesItsOwnMinuteTotal() {
        val session = CoachSetParser.parse("8x100 free on 1:45", PACE)

        // 8 repetitions of 100 m at 85 s with 20 s rest = 8 * 105 s = 840 s = 14 minutes.
        assertEquals(840, session.estimatedTotalSeconds)
        assertEquals(14, session.constraints.minutesAvailable)
    }

    @Test
    fun aBareHeaderClaimsTheFollowingLinesUntilTheNextHeader() {
        val text = """
            Warm up
            300 free
            Main set
            8x100 free on 1:45
            Cool down
            200 free
        """.trimIndent()

        val session = CoachSetParser.parse(text, PACE)

        assertEquals(
            listOf(SectionKind.WARM_UP, SectionKind.MAIN_SET, SectionKind.COOL_DOWN),
            session.sections.map { it.kind },
        )
        assertEquals(1, session.section(SectionKind.WARM_UP)?.blocks?.size ?: -1)
        assertEquals(1, session.section(SectionKind.MAIN_SET)?.blocks?.size ?: -1)
        assertEquals(1, session.section(SectionKind.COOL_DOWN)?.blocks?.size ?: -1)
    }

    private companion object {
        const val PACE = 85.0
    }
}
