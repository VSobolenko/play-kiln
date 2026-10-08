package com.gexetr.unitwalk.domain

/**
 * How a move-out walk's comparison against its unit's prior move-in resolved.
 *
 * The absence of a prior report is a **result**, not an empty report. [CompareOutcome.NoPriorReport]
 * exists so the UI can say "there is no prior report for this unit" instead of drawing a baseline
 * that was never recorded — the factory's rule that a comparison must never fabricate its prior.
 */
sealed interface CompareOutcome {
    /** The unit has a finished prior move-in walk; the rows carry its per-room conditions. */
    data object HasPrior : CompareOutcome

    /** The unit has no finished prior move-in walk; every row carries `priorCondition = null`. */
    data object NoPriorReport : CompareOutcome
}

/**
 * One room's row in a move-out comparison.
 *
 * [priorCondition] is the condition recorded on the **prior** move-in walk for the same room name,
 * or `null` when there is no prior report (or the prior report did not record that room). [note] is
 * the note the landlord recorded on the **current** walk's delta for that room.
 */
data class RoomComparison(
    val roomName: String,
    val priorCondition: RoomCondition?,
    val note: String,
    val outcome: CompareOutcome = CompareOutcome.HasPrior,
)

/**
 * The unit's most recent **finished** move-in walk, other than the walk being compared.
 *
 * Only a finished [WalkKind.MOVE_IN] counts as a baseline: a draft move-in is still being written,
 * and using it would compare against a report that does not exist yet. `null` means the unit has no
 * prior report — [compareRooms] then returns rows that say so rather than inventing a baseline.
 */
fun priorMoveInFor(state: WalkState, unitId: String, currentWalkId: String): Walk? =
    state.walks
        .filter { walk ->
            walk.unitId == unitId &&
                walk.id != currentWalkId &&
                walk.kind == WalkKind.MOVE_IN &&
                walk.status == WalkStatus.FINISHED
        }
        .maxByOrNull { it.startedAtIso }

/**
 * Builds one comparison row per room of [current].
 *
 * When [prior] is `null` — or a prior walk exists but records no condition for a room — the row's
 * [RoomComparison.priorCondition] is `null` and its [RoomComparison.outcome] is
 * [CompareOutcome.NoPriorReport]; a missing prior is never rendered as an empty `GOOD`. When
 * [prior] is present, a room is matched by **name**, and its condition is read from the prior
 * walk's own recorded delta for that room.
 */
fun compareRooms(prior: Walk?, current: Walk): List<RoomComparison> {
    val priorByRoomName: Map<String, Room> = prior?.rooms?.associateBy { it.name } ?: emptyMap()
    val priorConditionByRoomId: Map<String, RoomCondition> =
        prior?.differences?.associate { it.roomId to it.condition } ?: emptyMap()
    val currentDeltaByRoomId: Map<String, RoomDelta> =
        current.differences.associateBy { it.roomId }

    return current.rooms.map { room ->
        if (prior == null) {
            RoomComparison(
                roomName = room.name,
                priorCondition = null,
                note = currentDeltaByRoomId[room.id]?.note.orEmpty(),
                outcome = CompareOutcome.NoPriorReport,
            )
        } else {
            val priorRoom = priorByRoomName[room.name]
            RoomComparison(
                roomName = room.name,
                priorCondition = priorRoom?.let { priorConditionByRoomId[it.id] },
                note = currentDeltaByRoomId[room.id]?.note.orEmpty(),
                outcome = CompareOutcome.HasPrior,
            )
        }
    }
}
