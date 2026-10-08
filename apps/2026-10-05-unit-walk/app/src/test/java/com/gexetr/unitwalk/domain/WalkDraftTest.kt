package com.gexetr.unitwalk.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure draft mutations: what each one returns, and that it never mutates its input. */
class WalkDraftTest {

    @Test
    fun finishWalkStampsTheFinishTimeAndFlipsStatus() {
        val state = WalkState(units = listOf(Unit(id = "u1", name = "Flat 3B")))
        val started = startWalk(
            state = state,
            unitId = "u1",
            kind = WalkKind.MOVE_OUT,
            nowIso = "2026-10-05T10:00:00Z",
        )

        assertEquals(WalkStatus.DRAFT, started.status)
        assertNull(started.finishedAtIso)
        assertEquals("2026-10-05T10:00:00Z", started.startedAtIso)

        val opened = openRoom(started, "Kitchen")
        val roomId = opened.rooms.single().id
        val withPhoto = addPhoto(
            walk = opened,
            roomId = roomId,
            photo = Photo(id = "p1", fileName = "kitchen.jpg"),
        )
        val noted = setNote(withPhoto, roomId, "Chip on the worktop")
        val roomFinished = finishRoom(noted, roomId)
        val finished = finishWalk(roomFinished, "2026-10-05T11:30:00Z")

        assertEquals(WalkStatus.FINISHED, finished.status)
        assertEquals("2026-10-05T11:30:00Z", finished.finishedAtIso)
        assertEquals("Chip on the worktop", finished.rooms.single().note)
        assertEquals(listOf("kitchen.jpg"), finished.rooms.single().photos.map { it.fileName })
    }

    @Test
    fun everyMutationReturnsANewSnapshotAndLeavesThePreviousOneUntouched() {
        val state = WalkState(units = listOf(Unit(id = "u1", name = "Flat 3B")))
        val draft = startWalk(state, "u1", WalkKind.SINGLE, "2026-10-05T10:00:00Z")
        val opened = openRoom(draft, "Hall")

        assertTrue(draft.rooms.isEmpty())
        assertEquals(1, opened.rooms.size)

        val roomId = opened.rooms.single().id
        val noted = setNote(opened, roomId, "Scuff by the door")

        assertEquals("", opened.rooms.single().note)
        assertEquals("Scuff by the door", noted.rooms.single().note)
    }

    @Test
    fun setDeltaKeepsAtMostOneDifferencePerRoom() {
        val walk = Walk(id = "w1", unitId = "u1")
        val first = setDelta(walk, RoomDelta(roomId = "r1", condition = RoomCondition.WORN, note = "chip"))
        val second = setDelta(first, RoomDelta(roomId = "r1", condition = RoomCondition.DAMAGED, note = "hole"))
        val other = setDelta(second, RoomDelta(roomId = "r2", condition = RoomCondition.GOOD))

        assertEquals(2, other.differences.size)
        assertEquals(RoomCondition.DAMAGED, other.differences.first { it.roomId == "r1" }.condition)
        assertEquals("hole", other.differences.first { it.roomId == "r1" }.note)
    }

    @Test
    fun signaturesAreAppendedInEntryOrder() {
        val walk = Walk(id = "w1", unitId = "u1")
        val signed = addSignature(
            addSignature(walk, Signature(name = "Landlord", signedAtIso = "2026-10-05T11:00:00Z")),
            Signature(name = "Tenant", signedAtIso = "2026-10-05T11:01:00Z"),
        )

        assertEquals(listOf("Landlord", "Tenant"), signed.signatures.map { it.name })
    }
}
