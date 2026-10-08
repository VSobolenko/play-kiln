package com.gexetr.unitwalk.ui.walk

import androidx.compose.runtime.Immutable
import com.gexetr.unitwalk.domain.PhotoSource
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.domain.WalkStatus

/**
 * The room-by-room walk screen's state, as an immutable value.
 *
 * It mirrors the durable [com.gexetr.unitwalk.domain.Walk]: [rooms] are the walk's rooms in order,
 * each with its own note, photo file names and recorded [RoomRow.condition], and [currentRoomId] is
 * the room the landlord is filling (or `null` when none is open). No user-facing copy lives here —
 * the screen renders [WalkMessage] from its own resources — so this file stays free of `R.string`
 * ids and of prose.
 *
 * A room whose condition has not been recorded carries `condition = null`; it is never rounded up to
 * [RoomCondition.GOOD], because "not recorded" and "recorded as good" are different facts.
 */
@Immutable
data class WalkUiState(
    val isLoading: Boolean = true,
    val walkId: String = "",
    val unitId: String = "",
    val kind: WalkKind = WalkKind.SINGLE,
    val status: WalkStatus = WalkStatus.DRAFT,
    val rooms: List<RoomRow> = emptyList(),
    val currentRoomId: String? = null,
    val photoCount: Int = 0,
    val message: WalkMessage? = null,
) {
    /** The room the landlord is filling, or `null` when no room is open yet. */
    val currentRoom: RoomRow? get() = rooms.firstOrNull { room -> room.id == currentRoomId }
}

/** One room as the walk lists it: its note, its photos and the condition recorded for it. */
@Immutable
data class RoomRow(
    val id: String,
    val name: String,
    val note: String,
    val photoFileNames: List<String>,
    val condition: RoomCondition?,
)

/** The typed result of an action. The screen turns each case into copy from its own resources. */
sealed interface WalkMessage {
    data object RoomAdded : WalkMessage
    data object NoteSaved : WalkMessage
    data object PhotoStored : WalkMessage
    data object ConditionSaved : WalkMessage
    data object RoomFinished : WalkMessage
    data object WalkFinished : WalkMessage
    data object RoomNameRequired : WalkMessage
    data object RoomNotFound : WalkMessage

    /** There is no draft walk to resume; the screen shows an honest empty state. */
    data object NothingToResume : WalkMessage

    /** The write to disk failed; the change is **not** reported as saved. */
    data object NotSaved : WalkMessage
}

/** Everything the walk screen can ask the brain to do. */
sealed interface WalkEvent {
    /** The screen appeared; restore the walk from disk (and from the saved state, if any). */
    data object ScreenShown : WalkEvent

    /** Open the walk with this id (the value the portfolio screen navigated with). */
    data class RequestWalk(val walkId: String) : WalkEvent

    data class AddRoom(val name: String) : WalkEvent

    data class OpenRoom(val roomId: String) : WalkEvent

    data class NoteChanged(val roomId: String, val note: String) : WalkEvent

    data class ConditionChanged(val roomId: String, val condition: RoomCondition) : WalkEvent

    /**
     * Attach a photo to a room.
     *
     * [fileName] is chosen by the caller with [PhotoUris.newPhotoFileName] so the camera's
     * destination file and the committed row share one name; [bytes] are already read from the
     * camera or the gallery and are written to the store *before* the row that names them.
     */
    class AttachPhoto(
        val roomId: String,
        val fileName: String,
        val bytes: ByteArray,
        val source: PhotoSource = PhotoSource.CAMERA,
    ) : WalkEvent

    data class FinishRoom(val roomId: String) : WalkEvent

    data object FinishWalk : WalkEvent

    /** The screen has shown [WalkUiState.message]; clear it. */
    data object MessageShown : WalkEvent
}
