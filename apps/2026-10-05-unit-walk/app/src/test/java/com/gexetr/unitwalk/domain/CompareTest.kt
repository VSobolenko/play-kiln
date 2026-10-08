package com.gexetr.unitwalk.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Linking a move-out walk to its prior move-in, and refusing to invent a baseline. */
class CompareTest {

    @Test
    fun aMoveOutComparesRoomByRoomAgainstTheLinkedMoveIn() {
        val priorMoveIn = Walk(
            id = "move-in",
            unitId = "u1",
            kind = WalkKind.MOVE_IN,
            startedAtIso = "2026-01-01T09:00:00Z",
            finishedAtIso = "2026-01-01T10:00:00Z",
            status = WalkStatus.FINISHED,
            rooms = listOf(
                Room(id = "m1r1", name = "Kitchen"),
                Room(id = "m1r2", name = "Hall"),
            ),
            differences = listOf(
                RoomDelta(roomId = "m1r1", condition = RoomCondition.GOOD),
                RoomDelta(roomId = "m1r2", condition = RoomCondition.GOOD),
            ),
        )
        val moveOut = Walk(
            id = "move-out",
            unitId = "u1",
            kind = WalkKind.MOVE_OUT,
            startedAtIso = "2026-06-01T09:00:00Z",
            rooms = listOf(
                Room(id = "m2r1", name = "Kitchen"),
                Room(id = "m2r2", name = "Hall"),
            ),
            differences = listOf(
                RoomDelta(roomId = "m2r1", condition = RoomCondition.WORN, note = "Worktop chipped"),
                RoomDelta(roomId = "m2r2", condition = RoomCondition.DAMAGED, note = "Hole in the wall"),
            ),
        )
        val state = WalkState(
            units = listOf(Unit(id = "u1", name = "Flat 3B")),
            walks = listOf(priorMoveIn, moveOut),
        )

        val prior = priorMoveInFor(state, "u1", moveOut.id)
        assertEquals(priorMoveIn, prior)

        val rows = compareRooms(prior, moveOut)

        assertEquals(2, rows.size)
        assertEquals(listOf("Kitchen", "Hall"), rows.map { it.roomName })
        assertEquals(
            listOf(RoomCondition.GOOD, RoomCondition.GOOD),
            rows.map { it.priorCondition },
        )
        assertEquals("Worktop chipped", rows[0].note)
        assertEquals("Hole in the wall", rows[1].note)
        assertEquals(CompareOutcome.HasPrior, rows[0].outcome)
        assertEquals(CompareOutcome.HasPrior, rows[1].outcome)
    }

    @Test
    fun aUnitWithoutAPriorMoveInReportsNoPriorReportRatherThanFabricatingOne() {
        val moveOut = Walk(
            id = "move-out",
            unitId = "u9",
            kind = WalkKind.MOVE_OUT,
            startedAtIso = "2026-06-01T09:00:00Z",
            rooms = listOf(Room(id = "r1", name = "Kitchen")),
            differences = listOf(RoomDelta(roomId = "r1", condition = RoomCondition.WORN)),
        )
        val state = WalkState(
            units = listOf(Unit(id = "u9", name = "Empty unit")),
            walks = listOf(moveOut),
        )

        val prior = priorMoveInFor(state, "u9", moveOut.id)
        assertNull(prior)

        val rows = compareRooms(prior, moveOut)

        assertEquals(1, rows.size)
        assertNull(rows.single().priorCondition)
        assertEquals(CompareOutcome.NoPriorReport, rows.single().outcome)
    }

    @Test
    fun aDraftMoveInIsNotABaselineAndAWalkFromAnotherUnitIsNotEither() {
        val draftMoveIn = Walk(
            id = "draft-move-in",
            unitId = "u1",
            kind = WalkKind.MOVE_IN,
            startedAtIso = "2026-01-01T09:00:00Z",
            status = WalkStatus.DRAFT,
        )
        val otherUnitMoveIn = Walk(
            id = "other-unit",
            unitId = "u2",
            kind = WalkKind.MOVE_IN,
            startedAtIso = "2026-01-01T09:00:00Z",
            status = WalkStatus.FINISHED,
        )
        val current = Walk(
            id = "current",
            unitId = "u1",
            kind = WalkKind.MOVE_OUT,
            startedAtIso = "2026-06-01T09:00:00Z",
        )
        val state = WalkState(walks = listOf(draftMoveIn, otherUnitMoveIn, current))

        assertNull(priorMoveInFor(state, "u1", current.id))
    }

    @Test
    fun theMostRecentFinishedMoveInWins() {
        val older = Walk(
            id = "older",
            unitId = "u1",
            kind = WalkKind.MOVE_IN,
            startedAtIso = "2025-01-01T09:00:00Z",
            status = WalkStatus.FINISHED,
        )
        val newer = Walk(
            id = "newer",
            unitId = "u1",
            kind = WalkKind.MOVE_IN,
            startedAtIso = "2026-01-01T09:00:00Z",
            status = WalkStatus.FINISHED,
        )
        val current = Walk(id = "current", unitId = "u1", kind = WalkKind.MOVE_OUT)
        val state = WalkState(walks = listOf(older, newer, current))

        assertEquals(newer, priorMoveInFor(state, "u1", current.id))
    }
}
