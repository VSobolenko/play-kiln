package com.gexetr.unitwalk.ui.units

import androidx.compose.runtime.Immutable
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.ui.unlock.UnlockState

/**
 * The portfolio screen's state: one row per named unit, as an immutable value.
 *
 * The state carries **typed** messages and never a resource id and never a user-facing sentence:
 * the copy for a message belongs to the screen's resources, so the brain stays unit-testable without
 * Android and a translated string never has to be plumbed through a ViewModel.
 *
 * Every fact here is derived from the durable portfolio ([com.gexetr.unitwalk.domain.WalkState]) —
 * except [UnitRow.locked], which the entitlement decides — and nothing is invented. A unit with no
 * walks reports `walkCount = 0`, no resumable draft and no prior report, and the screen renders an
 * honest empty state instead of a fabricated row; likewise a unit whose prior move-in walk is absent
 * or still a draft reports `hasPriorMoveIn = false`.
 */
@Immutable
data class UnitsUiState(
    val isLoading: Boolean = true,
    val units: List<UnitRow> = emptyList(),
    /** A deletion that has been requested but not yet confirmed, or `null`. */
    val pendingDelete: PendingUnitDelete? = null,
    /** A walk the screen should open now (started or resumed), or `null` once it has been opened. */
    val openWalk: WalkRef? = null,
    /**
     * The one-time unlock, as the free-plan unit cap reads it: it decides which rows are usable, not
     * only whether a second unit may be created. While it is not [UnlockState.Owned] — including
     * [UnlockState.Unknown], before Play has answered — the free unit is the portfolio's first unit
     * and every other unit is [UnitRow.locked].
     */
    val unlock: UnlockState = UnlockState.Unknown,
    /**
     * A refusal — a create past the free unit, or a walk on a locked one — that wants the Unlock
     * action on its message. It is a one-shot that **arms** that action rather than a navigation
     * instruction: the destination opens only when the user taps the action, and the screen clears
     * it with [UnitsEvent.UnlockOpened] as the message is shown.
     */
    val openUnlock: Boolean = false,
    val message: UnitsMessage? = null,
    /**
     * A marker for message **publication**, so the screen's snackbar effect restarts on every message
     * even when it is an equal value to the one already showing: [UnitsMessage] cases are
     * `data object`s, so a second refusal is an equal key and an effect keyed on `message` alone
     * would show nothing. It is bumped wherever a message is published and never identifies which
     * message it was.
     */
    val messageId: Long = 0,
)

/**
 * One unit as the portfolio lists it.
 *
 * [resumableDraft] is the unit's most recent `DRAFT` walk — the "continue" affordance — and is
 * `null` when the unit has no walk in progress. [walkCount] is the number of walks that belong to
 * this unit, so a unit with none is visibly empty rather than rounded up to one.
 *
 * [locked] is the free-plan cap made visible: while the unlock is not owned, every unit except the
 * first is locked. A locked unit keeps every fact — its name, its address and its [walkCount] are
 * unchanged — and only its way in is closed, so the portfolio tells the truth instead of refusing an
 * action its own row still offers.
 */
@Immutable
data class UnitRow(
    val id: String,
    val name: String,
    val address: String,
    val walkCount: Int,
    val resumableDraft: WalkRef?,
    val hasPriorMoveIn: Boolean,
    val locked: Boolean,
)

/** A reference to one walk: enough for the screen to open it without re-reading the document. */
@Immutable
data class WalkRef(
    val walkId: String,
    val unitId: String,
    val kind: WalkKind,
)

/**
 * A deletion awaiting the landlord's confirmation.
 *
 * Deleting is a two-step act on purpose: [UnitsEvent.RequestDeleteUnit] only stages this value, and
 * [UnitsEvent.ConfirmDeleteUnit] is what removes the unit and its walks. The name and the walk count
 * travel with the prompt so the confirmation can name exactly what is about to be removed.
 */
@Immutable
data class PendingUnitDelete(
    val unitId: String,
    val unitName: String,
    val walkCount: Int,
)

/** The typed result of an action. The screen turns each case into copy from its own resources. */
sealed interface UnitsMessage {
    data object UnitCreated : UnitsMessage
    data object UnitRenamed : UnitsMessage
    data object UnitDeleted : UnitsMessage
    data object NameRequired : UnitsMessage
    data object UnitNotFound : UnitsMessage
    data object WalkStarted : UnitsMessage

    /**
     * The free plan already holds its one unit, or the unit the user tried to open lies behind the
     * unlock, so the action needs the unlock. The copy says so and carries the unlock as the
     * message's own action; it is not the only way to the Unlock destination — the portfolio's top
     * bar opens it too — and nothing the user already has is removed.
     */
    data object UnitLimitReached : UnitsMessage

    /** The write to disk failed; the change is **not** reported as saved. */
    data object NotSaved : UnitsMessage
}

/** Everything the portfolio screen can ask the brain to do. */
sealed interface UnitsEvent {
    /** The screen appeared; load (or reload) the portfolio from disk. */
    data object ScreenShown : UnitsEvent

    data class CreateUnit(val name: String) : UnitsEvent

    data class RenameUnit(val unitId: String, val name: String) : UnitsEvent

    /** Stage a deletion. Removes nothing until [ConfirmDeleteUnit] arrives. */
    data class RequestDeleteUnit(val unitId: String) : UnitsEvent

    data object ConfirmDeleteUnit : UnitsEvent

    data object CancelDeleteUnit : UnitsEvent

    /** Start a fresh draft walk from the named unit. */
    data class StartWalk(val unitId: String, val kind: WalkKind) : UnitsEvent

    /** Open a walk that already exists (the "continue" affordance). */
    data class ResumeWalk(val walkId: String) : UnitsEvent

    /** The screen has navigated into [UnitsUiState.openWalk]; clear the one-shot. */
    data object WalkOpened : UnitsEvent

    /** The screen has consumed [UnitsUiState.openUnlock] (the refusal's message is shown); clear it. */
    data object UnlockOpened : UnitsEvent

    /** The screen has shown [UnitsUiState.message]; clear it. */
    data object MessageShown : UnitsEvent
}
