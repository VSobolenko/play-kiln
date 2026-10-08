package com.gexetr.unitwalk.domain

/**
 * The finished report, described as plain data.
 *
 * This is deliberately **framework-free**: it is the shape the PDF writer consumes
 * ([com.gexetr.unitwalk.pdf.renderReportPdf]) and the shape the JVM unit tests assert against, so
 * "the report carries every room and every photo" is provable without a device and without drawing
 * anything. The PDF renderer holds no logic of its own beyond laying this value onto pages.
 */
data class ReportModel(
    val unitName: String,
    val unitAddress: String,
    val kind: WalkKind,
    val startedAtIso: String,
    val finishedAtIso: String?,
    val rooms: List<ReportRoom>,
    val deltas: List<RoomDelta>,
    val signatures: List<Signature>,
    val photoCount: Int,
)

/** One room as it appears in the report: its note, its condition and the photos that carry it. */
data class ReportRoom(
    val name: String,
    val note: String,
    val photoFileNames: List<String>,
    val condition: RoomCondition?,
)

/**
 * Builds the report description for the walk identified by [walkId].
 *
 * The unit's name and address are read from the portfolio, each room's condition from the walk's own
 * recorded deltas (keyed by room id), and [ReportModel.photoCount] is the total across all rooms.
 *
 * A [walkId] that is not in [state] returns an empty description rather than throwing: a report is
 * built from a snapshot the caller already holds, and a lookup miss is a caller bug that should
 * surface as an empty report, never as a crash on a file the landlord owns.
 */
fun buildReportModel(state: WalkState, walkId: String): ReportModel {
    val walk = state.walks.firstOrNull { it.id == walkId }
        ?: return ReportModel(
            unitName = "",
            unitAddress = "",
            kind = WalkKind.SINGLE,
            startedAtIso = "",
            finishedAtIso = null,
            rooms = emptyList(),
            deltas = emptyList(),
            signatures = emptyList(),
            photoCount = 0,
        )

    val unit = state.units.firstOrNull { it.id == walk.unitId }
    val conditionByRoomId: Map<String, RoomCondition> =
        walk.differences.associate { it.roomId to it.condition }

    val rooms = walk.rooms.map { room ->
        ReportRoom(
            name = room.name,
            note = room.note,
            photoFileNames = room.photos.map { it.fileName },
            condition = conditionByRoomId[room.id],
        )
    }

    return ReportModel(
        unitName = unit?.name.orEmpty(),
        unitAddress = unit?.address.orEmpty(),
        kind = walk.kind,
        startedAtIso = walk.startedAtIso,
        finishedAtIso = walk.finishedAtIso,
        rooms = rooms,
        deltas = walk.differences,
        signatures = walk.signatures,
        photoCount = walk.rooms.sumOf { it.photos.size },
    )
}
