package com.gexetr.laneset.ui.unlock

import androidx.compose.runtime.Immutable
import com.gexetr.laneset.data.BillingAvailability

/**
 * Everything the unlock screen renders, as one immutable value.
 *
 * The two facts the screen must not get wrong are hoisted in from `BillingManager` rather than
 * mirrored here — [isUnlocked] and [availability] are the manager's own `StateFlow`s, combined in the
 * ViewModel — because a UI copy of an entitlement flag is how an app tells a paying swimmer they own
 * nothing. The two local fields describe the *attempt* in flight, not the entitlement: a purchase
 * that is still open on the Play sheet, and a restore that is still talking to Play.
 *
 * [notice] is a one-line note under the action, and it is `null` when there is nothing honest to say.
 */
@Immutable
data class UnlockUiState(
    /** Whether the one-time unlock is owned, made authoritative by Play, never by a local flag. */
    val isUnlocked: Boolean = false,

    /** Whether the Play Billing service can be reached at all. */
    val availability: BillingAvailability = BillingAvailability.UNINITIALIZED,

    /** The purchase sheet is open and the outcome has not arrived yet. */
    val isPurchasing: Boolean = false,

    /** A restore is reading the account's purchases back from Play. */
    val isRestoring: Boolean = false,

    /** A closing note about the last attempt, or `null`. */
    val notice: UnlockNotice? = null,
) {
    /**
     * Whether the buy action may be offered.
     *
     * Only when Play is reachable, nothing is owned yet and no attempt is already running: a button
     * that cannot work is worse than a sentence saying why, which is what the screen shows instead.
     */
    val canBuy: Boolean
        get() = availability == BillingAvailability.READY && !isUnlocked && !isPurchasing && !isRestoring
}

/**
 * The note the screen shows after an attempt.
 *
 * Four states, and **the first is not an error**: a cancellation is the swimmer closing Play's own
 * sheet, nothing was charged and nothing is owned, so it is reported in the ordinary surface ink
 * rather than in the error colour. Collapsing it into the failure case would tell a swimmer who
 * changed their mind that something went wrong — the defect `PurchaseOutcome` itself exists to
 * prevent, and it must not be re-introduced one layer up.
 */
enum class UnlockNotice {
    /** Play's sheet was closed; nothing was charged. */
    CANCELLED,

    /** The purchase did not complete; the buy action is offered again as the retry. */
    FAILED,

    /** The purchase could not be attempted at all — the service or the product was not available. */
    UNAVAILABLE,

    /** A restore was requested and Play holds no unlock on this account. */
    NOT_RESTORED,
}
