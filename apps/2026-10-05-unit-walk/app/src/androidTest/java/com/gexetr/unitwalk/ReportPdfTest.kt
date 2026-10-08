package com.gexetr.unitwalk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gexetr.unitwalk.domain.Photo
import com.gexetr.unitwalk.domain.PhotoSource
import com.gexetr.unitwalk.domain.Room
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.RoomDelta
import com.gexetr.unitwalk.domain.Signature
import com.gexetr.unitwalk.domain.Unit
import com.gexetr.unitwalk.domain.Walk
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.domain.WalkState
import com.gexetr.unitwalk.domain.WalkStatus
import com.gexetr.unitwalk.domain.buildReportModel
import com.gexetr.unitwalk.pdf.renderReportPdf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The report renderer, proved on the device.
 *
 * `renderReportPdf` draws through `android.graphics.pdf.PdfDocument`, so it cannot run under plain
 * JUnit and this slot has no Robolectric dependency — an instrumented test is the correct home. The
 * orchestrator's device run executes it; a JVM-only run leaves it unrun, which is an honest
 * `UNVERIFIED` rather than a pass.
 *
 * The test builds the report through the framework-free `buildReportModel`, then asserts the model
 * is non-degenerate *and* the bytes are non-empty, so an empty PDF cannot satisfy it by accident.
 */
@RunWith(AndroidJUnit4::class)
class ReportPdfTest {

    @Test
    fun theRenderedReportIsANonEmptyPdfForAWalkWithRoomsNotesAndDeltas() {
        val model = buildReportModel(sampleState(), WALK_ID)

        assertEquals(2, model.rooms.size)
        assertTrue(model.rooms.any { it.note.isNotBlank() })
        assertTrue(model.deltas.isNotEmpty())
        assertTrue(model.photoCount > 0)
        assertTrue(model.signatures.isNotEmpty())

        val bytes = renderReportPdf(model)

        assertTrue(bytes.isNotEmpty())
    }

    private fun sampleState(): WalkState = WalkState(
        units = listOf(Unit(id = UNIT_ID, name = "Flat 3B", address = "12 Oak Street")),
        walks = listOf(
            Walk(
                id = WALK_ID,
                unitId = UNIT_ID,
                kind = WalkKind.MOVE_OUT,
                startedAtIso = "2026-10-05T10:00:00Z",
                finishedAtIso = "2026-10-05T10:40:00Z",
                status = WalkStatus.FINISHED,
                rooms = listOf(
                    Room(
                        id = "room-kitchen",
                        name = "Kitchen",
                        note = "Chip on the worktop",
                        photos = listOf(
                            Photo(
                                id = "photo-kitchen",
                                fileName = "room-kitchen-1.jpg",
                                capturedAtIso = "2026-10-05T10:05:00Z",
                                source = PhotoSource.CAMERA,
                            ),
                        ),
                    ),
                    Room(
                        id = "room-hall",
                        name = "Hall",
                        note = "Scuff by the door",
                        photos = listOf(
                            Photo(
                                id = "photo-hall",
                                fileName = "room-hall-1.jpg",
                                capturedAtIso = "2026-10-05T10:08:00Z",
                                source = PhotoSource.PICKER,
                            ),
                        ),
                    ),
                ),
                differences = listOf(
                    RoomDelta(
                        roomId = "room-kitchen",
                        condition = RoomCondition.WORN,
                        note = "Worktop chipped",
                    ),
                    RoomDelta(
                        roomId = "room-hall",
                        condition = RoomCondition.DAMAGED,
                        note = "Hole in the wall",
                    ),
                ),
                signatures = listOf(
                    Signature(name = "Landlord", signedAtIso = "2026-10-05T10:39:00Z"),
                    Signature(name = "Tenant", signedAtIso = "2026-10-05T10:39:30Z"),
                ),
            ),
        ),
    )

    private companion object {
        const val UNIT_ID = "unit-1"
        const val WALK_ID = "walk-1"
    }
}
