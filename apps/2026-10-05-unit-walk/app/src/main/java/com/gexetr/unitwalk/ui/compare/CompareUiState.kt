package com.gexetr.unitwalk.ui.compare

import com.gexetr.unitwalk.domain.CompareOutcome
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.WalkKind

/**
 * One room's line in the move-in versus move-out comparison.
 *
 * [roomId] is carried even though [com.gexetr.unitwalk.domain.RoomComparison] does not: recording a
 * difference needs the room's identity, and the domain row is deliberately a *reading* rather than a
 * key. [priorCondition] is `null` when there is no prior report — never a fabricated `GOOD`.
 */
data class CompareRow(
    val roomId: String,
    val roomName: String,
    val priorCondition: RoomCondition?,
    val currentCondition: RoomCondition?,
    val note: String,
)

/** The comparison surface's state, as typed values only — the screen owns every word. */
data class CompareUiState(
    val isLoaded: Boolean = false,
    val unitName: String = "",
    val walkId: String = "",
    val walkKind: WalkKind = WalkKind.SINGLE,
    val outcome: CompareOutcome = CompareOutcome.HasPrior,
    val priorWalkId: String? = null,
    val rows: List<CompareRow> = emptyList(),
    val message: CompareMessage? = null,
)

/** What just happened, in the app's own vocabulary — never a sentence. */
enum class CompareMessage {
    DeltaSaved,
    SaveFailed,
    WalkNotFound,
}

/** The user's intents on the comparison surface. */
sealed interface CompareEvent {
    data class RecordDelta(
        val roomId: String,
        val condition: RoomCondition,
        val note: String,
    ) : CompareEvent

    data object MessageDismissed : CompareEvent
}
