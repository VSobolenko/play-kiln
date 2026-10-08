package com.gexetr.unitwalk.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The report description: it carries every room, the photo count and a non-empty body. */
class ReportTest {

    @Test
    fun theReportCarriesEveryRoomAndThePhotoCount() {
        val state = WalkState(
            units = listOf(Unit(id = "u1", name = "Flat 3B", address = "12 Oak Street")),
            walks = listOf(
                Walk(
                    id = "w1",
                    unitId = "u1",
                    kind = WalkKind.MOVE_IN,
                    startedAtIso = "2026-10-05T10:00:00Z",
                    finishedAtIso = "2026-10-05T11:00:00Z",
                    status = WalkStatus.FINISHED,
                    rooms = listOf(
                        Room(
                            id = "r1",
                            name = "Kitchen",
                            note = "Chip on the worktop",
                            photos = listOf(
                                Photo(id = "p1", fileName = "kitchen-1.jpg"),
                                Photo(id = "p2", fileName = "kitchen-2.jpg"),
                            ),
                        ),
                        Room(
                            id = "r2",
                            name = "Hall",
                            note = "Scuff by the door",
                            photos = listOf(Photo(id = "p3", fileName = "hall-1.jpg")),
                        ),
                    ),
                    differences = listOf(
                        RoomDelta(roomId = "r1", condition = RoomCondition.WORN),
                        RoomDelta(roomId = "r2", condition = RoomCondition.GOOD),
                    ),
                    signatures = listOf(
                        Signature(name = "A. Landlord", signedAtIso = "2026-10-05T11:05:00Z"),
                    ),
                ),
            ),
        )

        val model = buildReportModel(state, "w1")

        assertEquals("Flat 3B", model.unitName)
        assertEquals("12 Oak Street", model.unitAddress)
        assertEquals(WalkKind.MOVE_IN, model.kind)
        assertEquals("2026-10-05T10:00:00Z", model.startedAtIso)
        assertEquals("2026-10-05T11:00:00Z", model.finishedAtIso)

        assertEquals(2, model.rooms.size)
        assertEquals(listOf("Kitchen", "Hall"), model.rooms.map { it.name })
        assertEquals(listOf("kitchen-1.jpg", "kitchen-2.jpg"), model.rooms[0].photoFileNames)
        assertEquals(listOf("hall-1.jpg"), model.rooms[1].photoFileNames)
        assertEquals(RoomCondition.WORN, model.rooms[0].condition)
        assertEquals(RoomCondition.GOOD, model.rooms[1].condition)
        assertEquals(3, model.photoCount)
        assertEquals(2, model.deltas.size)
        assertEquals(1, model.signatures.size)

        // The description is non-empty: it has rooms, names and a start date to draw.
        assertTrue(model.rooms.isNotEmpty())
        assertTrue(model.rooms.all { it.name.isNotBlank() })
        assertTrue(model.startedAtIso.isNotBlank())
    }

    @Test
    fun anUnknownWalkIdBuildsAnEmptyDescriptionRatherThanThrowing() {
        val model = buildReportModel(WalkState(), "missing")

        assertTrue(model.rooms.isEmpty())
        assertTrue(model.deltas.isEmpty())
        assertTrue(model.signatures.isEmpty())
        assertEquals(0, model.photoCount)
    }
}
