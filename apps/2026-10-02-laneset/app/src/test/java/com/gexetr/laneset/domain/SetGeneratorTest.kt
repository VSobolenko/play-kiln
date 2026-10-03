package com.gexetr.laneset.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetGeneratorTest {

    @Test
    fun identicalConstraintsProduceIdenticalSessions() {
        val first = SetGenerator.generateSession(constraintsOf(), PACE)
        val second = SetGenerator.generateSession(constraintsOf(), PACE)

        assertEquals(first, second)
        assertEquals(first.mainSetBlocks, second.mainSetBlocks)
    }

    @Test
    fun differentPoolLengthsProduceDifferentMainSets() {
        val shortPool = SetGenerator.generateSession(constraintsOf(poolLengthMeters = 25), PACE)
        val longPool = SetGenerator.generateSession(constraintsOf(poolLengthMeters = 50), PACE)

        assertNotEquals(shortPool.mainSetBlocks, longPool.mainSetBlocks)
    }

    @Test
    fun differentMinuteBudgetsProduceDifferentMainSets() {
        val shortSession = SetGenerator.generateSession(constraintsOf(minutesAvailable = 40), PACE)
        val longSession = SetGenerator.generateSession(constraintsOf(minutesAvailable = 75), PACE)

        assertNotEquals(shortSession.mainSetBlocks, longSession.mainSetBlocks)
    }

    @Test
    fun differentGearOnDeckProducesDifferentMainSets() {
        val bodyOnly = SetGenerator.generateSession(constraintsOf(gearOnDeck = emptySet()), PACE)
        val withGear = SetGenerator.generateSession(
            constraintsOf(gearOnDeck = setOf(Gear.FINS, Gear.KICKBOARD)),
            PACE,
        )

        assertNotEquals(bodyOnly.mainSetBlocks, withGear.mainSetBlocks)
    }

    @Test
    fun differentAvoidedStrokesProduceDifferentMainSets() {
        val everything = SetGenerator.generateSession(constraintsOf(), PACE)
        val noButterfly = SetGenerator.generateSession(
            constraintsOf(strokesToAvoid = setOf(Stroke.BUTTERFLY)),
            PACE,
        )

        assertNotEquals(everything.mainSetBlocks, noButterfly.mainSetBlocks)
    }

    @Test
    fun everyGeneratedIntervalCarriesATargetTime() {
        val session = SetGenerator.generateSession(constraintsOf(), PACE)

        assertTrue(session.blocks.isNotEmpty())
        assertTrue(session.blocks.all { it.targetTimeSeconds > 0 })
    }

    @Test
    fun strokesToAvoidAreNeverEmitted() {
        val avoided = setOf(Stroke.BUTTERFLY, Stroke.BREASTSTROKE)
        val session = SetGenerator.generateSession(constraintsOf(strokesToAvoid = avoided), PACE)

        assertTrue(session.blocks.none { it.stroke in avoided })
    }

    @Test
    fun onlyDeclaredGearIsUsed() {
        val session = SetGenerator.generateSession(constraintsOf(gearOnDeck = setOf(Gear.PULL_BUOY)), PACE)

        assertTrue(session.blocks.any { Gear.PULL_BUOY in it.gear })
        assertTrue(session.blocks.all { block -> block.gear.all { it == Gear.PULL_BUOY } })
    }

    @Test
    fun theSessionFitsInsideTheMinuteBudget() {
        val constraints = constraintsOf(minutesAvailable = 45)
        val session = SetGenerator.generateSession(constraints, PACE)

        assertTrue(session.estimatedTotalSeconds > 0)
        assertTrue(session.estimatedTotalSeconds <= constraints.minutesAvailable * 60)
        assertTrue(session.totalDistanceMeters > 0)
    }

    @Test
    fun theSessionHasAWarmUpAMainSetAndACoolDown() {
        val session = SetGenerator.generateSession(constraintsOf(), PACE)

        assertEquals(SectionKind.WARM_UP, session.sections.first().kind)
        assertTrue(session.section(SectionKind.MAIN_SET)?.blocks?.isNotEmpty() == true)
        assertEquals(SectionKind.COOL_DOWN, session.sections.last().kind)
    }

    private fun constraintsOf(
        poolLengthMeters: Int = 25,
        minutesAvailable: Int = 50,
        focus: Focus = Focus.THRESHOLD,
        strokesToAvoid: Set<Stroke> = emptySet(),
        gearOnDeck: Set<Gear> = emptySet(),
    ) = SessionConstraints(
        poolLengthMeters = poolLengthMeters,
        minutesAvailable = minutesAvailable,
        focus = focus,
        strokesToAvoid = strokesToAvoid,
        gearOnDeck = gearOnDeck,
    )

    private companion object {
        const val PACE = 85.0
    }
}
