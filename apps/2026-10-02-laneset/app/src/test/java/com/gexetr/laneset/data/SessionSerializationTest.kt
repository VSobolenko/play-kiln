package com.gexetr.laneset.data

import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.Gear
import com.gexetr.laneset.domain.PaceModel
import com.gexetr.laneset.domain.SectionKind
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SessionConstraints
import com.gexetr.laneset.domain.SessionSection
import com.gexetr.laneset.domain.SetBlock
import com.gexetr.laneset.domain.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The durability contract, exercised without a device.
 *
 * This is the JVM half of the promise `session-survives-interruption`: a generated session is
 * written to the store and read back with the same set, the same interval targets and the same
 * position in it. The serializer is deliberately free of Android and DataStore types so this test can
 * be a plain unit test — the `DataStoreSessionStore` around it needs a real `Context`, and a store
 * that can only be tested on a phone is a store whose format is only tested on a phone.
 *
 * The negative half matters as much as the positive one. A payload that cannot be read must cost the
 * swimmer the saved session and never the app, so every malformed case below asserts `null` and, by
 * reaching the assertion at all, asserts that nothing was thrown.
 */
class SessionSerializationTest {

    /** 320 s and 150 s trials give an 85 s threshold pace per 100 m. */
    private val pacePer100Seconds = PaceModel.thresholdPacePer100(320.0, 150.0)

    private val constraints = SessionConstraints(
        poolLengthMeters = 25,
        minutesAvailable = 60,
        focus = Focus.THRESHOLD,
        strokesToAvoid = setOf(Stroke.BUTTERFLY, Stroke.BREASTSTROKE),
        gearOnDeck = setOf(Gear.PULL_BUOY, Gear.FINS),
    )

    /**
     * A block whose target time is *derived*, never written as a literal.
     *
     * That is what makes the round-trip assertions meaningful: a target that came back as the right
     * number for the wrong reason still fails, because the expected value is recomputed from the
     * restored pace rather than compared with a constant this test also chose.
     */
    private fun block(
        distanceMeters: Int,
        repetitions: Int,
        stroke: Stroke,
        restSeconds: Int,
        gear: Set<Gear> = emptySet(),
    ) = SetBlock(
        distanceMeters = distanceMeters,
        repetitions = repetitions,
        stroke = stroke,
        targetTimeSeconds = PaceModel.intervalTargetSeconds(pacePer100Seconds, distanceMeters),
        restSeconds = restSeconds,
        gear = gear,
    )

    private val session = Session(
        constraints = constraints,
        pacePer100Seconds = pacePer100Seconds,
        sections = listOf(
            SessionSection(
                kind = SectionKind.WARM_UP,
                blocks = listOf(block(100, 4, Stroke.FREESTYLE, 15)),
            ),
            SessionSection(
                kind = SectionKind.MAIN_SET,
                blocks = listOf(
                    block(100, 8, Stroke.FREESTYLE, 20, gear = setOf(Gear.PULL_BUOY)),
                    block(200, 3, Stroke.INDIVIDUAL_MEDLEY, 30, gear = setOf(Gear.PULL_BUOY, Gear.PADDLES)),
                ),
            ),
            SessionSection(
                kind = SectionKind.COOL_DOWN,
                blocks = listOf(block(50, 4, Stroke.BACKSTROKE, 10)),
            ),
        ),
        estimatedTotalSeconds = 3_000,
        totalDistanceMeters = 3_200,
    )

    /** The target the pace model derives for a 100 m interval, used to build payload anchors. */
    private val hundredMetreTargetSeconds = PaceModel.intervalTargetSeconds(pacePer100Seconds, 100)

    /** The text `encode` writes for the fixture's avoided strokes: names, sorted, comma-separated. */
    private val avoidedStrokesField = constraints.strokesToAvoid
        .map { it.name }
        .sorted()
        .joinToString(",")

    private fun roundTrip(position: SessionPosition): PersistedSession {
        val encoded = SessionSerializer.encode(PersistedSession(session, position))
        val decoded = SessionSerializer.decode(encoded)
        assertNotNull("a session the app wrote itself must read back, payload was:\n$encoded", decoded)
        return requireNotNull(decoded)
    }

    @Test
    fun aSessionRoundTripsWithItsConstraintsAndSet() {
        val restored = roundTrip(SessionPosition.Start).session

        assertEquals(constraints, restored.constraints)
        assertEquals(pacePer100Seconds, restored.pacePer100Seconds, 0.0)
        assertEquals(session.estimatedTotalSeconds, restored.estimatedTotalSeconds)
        assertEquals(session.totalDistanceMeters, restored.totalDistanceMeters)

        // The set itself: every section in order, every block with every field — distance,
        // repetitions, stroke, target, rest and gear. A block dropped, reordered or given a default
        // rest would fail here.
        assertEquals(session.sections.map { it.kind }, restored.sections.map { it.kind })
        assertEquals(session.blocks, restored.blocks)
    }

    @Test
    fun everyIntervalTargetComesBackAsTheTimeThePaceModelDerives() {
        val restored = roundTrip(SessionPosition.Start).session

        restored.blocks.forEach { restoredBlock ->
            assertEquals(
                "the target for ${restoredBlock.distanceMeters}m must be the one the pace implies",
                PaceModel.intervalTargetSeconds(restored.pacePer100Seconds, restoredBlock.distanceMeters),
                restoredBlock.targetTimeSeconds,
            )
        }
    }

    @Test
    fun aBlockWithNoGearAndABlockWithSeveralBothSurvive() {
        val restored = roundTrip(SessionPosition.Start).session

        assertEquals(
            emptySet<Gear>(),
            restored.sections[0].blocks[0].gear,
        )
        assertEquals(
            setOf(Gear.PULL_BUOY, Gear.PADDLES),
            restored.sections[1].blocks[1].gear,
        )
    }

    @Test
    fun thePositionInsideTheSetSurvives() {
        val position = SessionPosition(sectionIndex = 1, blockIndex = 1, repetitionIndex = 2)

        assertEquals(position, roundTrip(position).position)
    }

    @Test
    fun encodingWhatWasDecodedReproducesTheSamePayload() {
        val first = SessionSerializer.encode(PersistedSession(session, SessionPosition(1, 1, 2)))
        val decoded = requireNotNull(SessionSerializer.decode(first))

        assertEquals(first, SessionSerializer.encode(decoded))
    }

    @Test
    fun thePayloadStatesItsSchemaVersionAndAnUnknownOneIsRefused() {
        val encoded = SessionSerializer.encode(PersistedSession(session))

        assertTrue(
            "the payload must declare its schema version, was:\n$encoded",
            encoded.startsWith("v=${SessionSerializer.SCHEMA_VERSION}"),
        )

        val fromTheFuture = encoded.replaceFirst(
            "v=${SessionSerializer.SCHEMA_VERSION}",
            "v=${SessionSerializer.SCHEMA_VERSION + 1}",
        )
        assertNull(
            "a payload from another schema version must not be guessed at",
            SessionSerializer.decode(fromTheFuture),
        )
    }

    @Test
    fun aPositionNamingALineTheSessionDoesNotHaveFallsBackToTheStart() {
        val beyondTheLastSection = SessionPosition(sectionIndex = 9, blockIndex = 0, repetitionIndex = 0)
        val beyondTheLastBlock = SessionPosition(sectionIndex = 1, blockIndex = 9, repetitionIndex = 0)

        assertEquals(SessionPosition.Start, roundTrip(beyondTheLastSection).position)
        assertEquals(SessionPosition.Start, roundTrip(beyondTheLastBlock).position)
    }

    @Test
    fun aRepetitionBeyondItsOwnBlockClampsToThatBlocksLastRepetition() {
        val lastRepetitionOfMainSetBlockTwo = session.sections[1].blocks[1].repetitions - 1

        val restored = roundTrip(
            SessionPosition(sectionIndex = 1, blockIndex = 1, repetitionIndex = 99),
        ).position

        assertEquals(SessionPosition(1, 1, lastRepetitionOfMainSetBlockTwo), restored)
    }

    @Test
    fun aMalformedPayloadDegradesToNothingInsteadOfThrowing() {
        val valid = SessionSerializer.encode(PersistedSession(session, SessionPosition(1, 1, 2)))

        val withoutSections = valid.lineSequence()
            .filterNot { line -> line.startsWith("s=") || line.startsWith("b=") }
            .joinToString("\n")

        val malformed: List<String?> = listOf(
            // Absent, blank or not a record at all.
            null,
            "",
            "   ",
            "\n\n",
            "this is not a session",
            // A line with no key/value separator.
            valid.replace("pool=25", "pool 25"),
            // Truncated before a field the format requires.
            valid.substring(0, valid.indexOf("totalMeters")),
            // A schema version this build does not know.
            valid.replace("v=${SessionSerializer.SCHEMA_VERSION}", "v=99"),
            // Values that are not numbers, or not positive.
            valid.replace("pool=25", "pool=not-a-number"),
            valid.replace("pace=$pacePer100Seconds", "pace="),
            valid.replace("pace=$pacePer100Seconds", "pace=-85.0"),
            // Enum names outside the closed sets the domain declares.
            valid.replace("focus=THRESHOLD", "focus=BACKSTROKE"),
            valid.replace("avoid=$avoidedStrokesField", "avoid=DOLPHIN"),
            valid.replace("FREESTYLE", "DOLPHIN_KICK"),
            // Structure the format cannot accept.
            valid.replace("s=MAIN_SET", "s="),
            valid.replace("pos=1|1|2", "pos=1"),
            valid.replace(
                "b=100|8|FREESTYLE|$hundredMetreTargetSeconds|20|PULL_BUOY",
                "b=100|8|FREESTYLE|PULL_BUOY",
            ),
            "v=1\nb=100|4|FREESTYLE|85|20|-\n",
            // A session with no sections is not a session.
            withoutSections,
            // A field this build does not know is refused rather than ignored.
            "$valid\nunknownKey=1",
        )

        malformed.forEach { payload ->
            val decoded = SessionSerializer.decode(payload)
            assertNull(
                "must not decode to a session: ${payload?.take(60)?.replace("\n", "\\n")}",
                decoded,
            )
        }
    }
}
