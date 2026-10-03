package com.gexetr.laneset.ui.home

import com.gexetr.laneset.data.SessionPosition
import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.SectionKind
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SessionConstraints
import com.gexetr.laneset.domain.SessionSection
import com.gexetr.laneset.domain.SetBlock
import com.gexetr.laneset.domain.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session walk the runner drives, and the durability rule the stored position rests on.
 *
 * These are the pure functions beside the Home state, exercised with no Android type on the
 * classpath. That is the same choice `PersistedSession` recording makes for its codec, and for the
 * same reason: the promise is that the swimmer's place survives process death, so the arithmetic
 * that decides where the swimmer is has to be answerable without a device.
 */
class SessionProgressTest {

    // --- fixtures -----------------------------------------------------------------------------

    private fun block(repetitions: Int): SetBlock = SetBlock(
        distanceMeters = 100,
        repetitions = repetitions,
        stroke = Stroke.FREESTYLE,
        targetTimeSeconds = 110,
        restSeconds = 20,
    )

    private fun session(vararg sections: SessionSection): Session = Session(
        constraints = SessionConstraints(
            poolLengthMeters = 25,
            minutesAvailable = 60,
            focus = Focus.ENDURANCE,
            strokesToAvoid = emptySet(),
            gearOnDeck = emptySet(),
        ),
        pacePer100Seconds = 110.0,
        sections = sections.toList(),
        estimatedTotalSeconds = sections.sumOf { it.totalSeconds },
        totalDistanceMeters = sections.sumOf { it.totalDistanceMeters },
    )

    /** Warm-up 3 reps, main set 2 reps then 4 reps, cool-down 2 reps. */
    private val full = session(
        SessionSection(SectionKind.WARM_UP, listOf(block(3))),
        SessionSection(SectionKind.MAIN_SET, listOf(block(2), block(4))),
        SessionSection(SectionKind.COOL_DOWN, listOf(block(2))),
    )

    // --- advancing ----------------------------------------------------------------------------

    @Test
    fun advanceStepsOneRepetitionAtATime() {
        assertEquals(SessionPosition(0, 0, 1), full.advance(SessionPosition.Start))
    }

    @Test
    fun advanceCarriesAcrossTheSectionBoundary() {
        // The last repetition of the warm-up is followed by the first of the main set, not by the
        // end of the session: a set list is one continuous thing to the swimmer.
        assertEquals(SessionPosition(1, 0, 0), full.advance(SessionPosition(0, 0, 2)))
    }

    @Test
    fun advanceStepsOverAnEmptySection() {
        val gappy = session(
            SessionSection(SectionKind.WARM_UP, listOf(block(3))),
            SessionSection(SectionKind.MAIN_SET, emptyList()),
            SessionSection(SectionKind.COOL_DOWN, listOf(block(2))),
        )

        assertEquals(SessionPosition(2, 0, 0), gappy.advance(SessionPosition(0, 0, 2)))
    }

    @Test
    fun theLastRepetitionAdvancesNowhereAndIsTheEnd() {
        val end = SessionPosition(2, 0, 1)

        assertEquals(end, full.advance(end))
        assertTrue(full.isAtEnd(end))
        assertFalse(full.isAtEnd(SessionPosition(2, 0, 0)))
    }

    // --- retreating ---------------------------------------------------------------------------

    @Test
    fun retreatLandsOnTheLastRepetitionOfThePreviousBlock() {
        // Back from the first line of the second main block lands on the previous block's last
        // repetition (block(2) has repetitions 0 and 1), not on its first.
        assertEquals(SessionPosition(1, 0, 1), full.retreat(SessionPosition(1, 1, 0)))
    }

    @Test
    fun retreatFromTheTopOfTheSessionStaysAtTheTop() {
        assertEquals(SessionPosition.Start, full.retreat(SessionPosition.Start))
    }

    // --- the stored position --------------------------------------------------------------------

    @Test
    fun aStoredPositionNamingAMissingSectionClampsToTheStart() {
        assertEquals(SessionPosition.Start, SessionPosition(9, 0, 2).clampedTo(full))
    }

    @Test
    fun aStoredRepetitionBeyondItsBlockClampsToThatBlocksLastRepetition() {
        assertEquals(SessionPosition(0, 0, 2), SessionPosition(0, 0, 99).clampedTo(full))
    }

    @Test
    fun blockAtAnswersForARestoredPosition() {
        assertEquals(block(4), full.blockAt(SessionPosition(1, 1, 3)))
    }

    // --- the pace clock -------------------------------------------------------------------------

    @Test
    fun formatClockPrintsTheNotationPoolSetsAreWrittenIn() {
        assertEquals("1:50", formatClock(110))
        assertEquals("0:00", formatClock(0))
        assertEquals("1:01:01", formatClock(3661))
        assertEquals("0:00", formatClock(-5))
    }
}
