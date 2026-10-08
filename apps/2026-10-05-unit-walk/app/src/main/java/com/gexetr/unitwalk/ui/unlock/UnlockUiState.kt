package com.gexetr.unitwalk.ui.unlock

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult

/**
 * The unlock surface's state, as typed values only.
 *
 * There is deliberately **no app-authored copy here**: the screen owns every word, and this file owns
 * the facts. [message] names *what happened* as an enum and [lastFailure] carries the capability's own
 * typed reason, so a screen can render either a localized string or a specific explanation without the
 * ViewModel knowing any copy. The one string that does appear — [priceLabel] — is a value Play returns
 * for the user's own currency; the app neither writes nor translates it, and the screen keeps its own
 * resource as the fallback for when Play cannot answer.
 */
data class UnlockUiState(
    val availability: CapabilityAvailability = CapabilityAvailability.UNINITIALIZED,
    val isOwned: Boolean = false,
    val isWorking: Boolean = false,
    val message: UnlockMessage? = null,
    val lastFailure: PlayKilnResult.Reason? = null,
    /**
     * Play's own formatted price for the unlock — e.g. `"$9.99"` or `"9,99 ₽"` — or `null` while Play
     * has not answered.
     *
     * `null` is the honest default: it means **unknown**, never "free". The screen then falls back to
     * its own string resource, so a preview or a test that never connects still shows a price rather
     * than a blank, and a user whose account Play prices in another currency sees the amount they will
     * actually be charged instead of the app's literal.
     */
    val priceLabel: String? = null,
)

/** What just happened, in the app's own vocabulary — never a sentence. */
enum class UnlockMessage {
    PurchaseSucceeded,
    PurchaseFailed,
    AlreadyOwned,
    RestoreSucceeded,
    RestoreFailed,
    Unavailable,
}

/** The user's intents on the unlock surface. */
sealed interface UnlockEvent {
    data object PurchaseRequested : UnlockEvent

    data object RestoreRequested : UnlockEvent

    data object MessageDismissed : UnlockEvent
}
