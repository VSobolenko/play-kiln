package com.gexetr.teachergradebook.monetization

import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the user must be told after an unlock action. */
enum class UnlockMessage {
    PURCHASED,
    RESTORED,
    NOTHING_TO_RESTORE,

    /** Play reported a success but has not released the purchase yet; not yet paid, not unlocked. */
    PENDING,

    /** Play Billing itself cannot sell here: no service, or the product is not configured. */
    UNAVAILABLE,

    /** No purchase completed. True for a cancellation and for a declined payment alike. */
    NOT_COMPLETED,

    /** Play could not be reached, so nothing was attempted. */
    OFFLINE,
}

data class UnlockState(
    val unlocked: Boolean = false,
    val purchasing: Boolean = false,
    val paywallVisible: Boolean = false,
    /** The reason the paywall opened, so the sheet can say what the teacher was trying to do. */
    val paywallReason: String? = null,
    val message: UnlockMessage? = null,
    val billingReady: Boolean = false,
)

/**
 * The unlock, as one state machine.
 *
 * Three rules are enforced here rather than hoped for:
 *
 * 1. **Only a confirmed purchase unlocks.** A cancelled, pending or failed flow cannot flip the
 *    entitlement, because the state comes from the capability's entitlement set and never from the
 *    result of the call itself.
 * 2. **A closed or unavailable paywall is not an error.** Being offline with the unlock already
 *    bought is normal, and the local entitlement survives it.
 * 3. **The app never states a price of its own.** The billing capability exposes entitlements, not
 *    product details, so no localized price is available in-process. Rather than hard-code "$6.99"
 *    — a number that is simply wrong for a teacher in another currency — the sheet says the unlock
 *    is a one-time purchase and that Google Play shows the exact price on the confirmation sheet
 *    it opens. The listing carries the price too, and that is where the capability's own contract
 *    puts the disclosure. Recorded as a deviation in the change's `design.md`.
 */
class EntitlementController(
    private val billing: BillingController,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(UnlockState())
    val state: StateFlow<UnlockState> = mutableState.asStateFlow()

    /** Launches the process-scoped connection, applies the stored entitlement, and listens. */
    fun start() {
        scope.launch {
            applyEntitlements(billing.entitlements.value)
            val result = billing.connect(MonetizationConfig.catalog)
            mutableState.update {
                it.copy(billingReady = result is PlayKilnResult.Success)
            }
        }
        scope.launch {
            billing.entitlements.collect(::applyEntitlements)
        }
    }

    fun openPaywall(reason: String) = mutableState.update {
        it.copy(paywallVisible = true, paywallReason = reason)
    }

    fun dismissPaywall() = mutableState.update { it.copy(paywallVisible = false) }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    suspend fun purchase(): UnlockMessage {
        if (mutableState.value.unlocked) return UnlockMessage.PURCHASED
        mutableState.update { it.copy(purchasing = true) }
        val result = billing.purchase(MonetizationConfig.PRO_UNLOCK)
        mutableState.update { it.copy(purchasing = false) }

        val message = when (result) {
            is PlayKilnResult.Success ->
                // A success whose entitlement did not move means Play is holding the purchase for
                // approval. Pending is not paid, so it is reported as pending rather than as bought.
                if (mutableState.value.unlocked) UnlockMessage.PURCHASED else UnlockMessage.PENDING

            is PlayKilnResult.Failure -> when (result.reason) {
                PlayKilnResult.Reason.NOT_AVAILABLE,
                PlayKilnResult.Reason.NOT_CONFIGURED,
                PlayKilnResult.Reason.POLICY_BLOCKED,
                PlayKilnResult.Reason.NO_CONSENT,
                -> UnlockMessage.UNAVAILABLE

                PlayKilnResult.Reason.NETWORK -> UnlockMessage.OFFLINE

                // A cancellation arrives here with no dedicated reason, so the wording reports only
                // what is certain — that nothing was completed and nothing was charged — instead of
                // telling a teacher with a declined card that they cancelled.
                PlayKilnResult.Reason.UNKNOWN -> UnlockMessage.NOT_COMPLETED
            }
        }
        mutableState.update { it.copy(message = message) }
        return message
    }

    /** The recovery path after a reinstall or a new device, which reviewers will exercise. */
    suspend fun restore(): UnlockMessage {
        val result = billing.restore()
        if (result is PlayKilnResult.Success) applyEntitlements(result.value)
        val message = if (mutableState.value.unlocked) {
            UnlockMessage.RESTORED
        } else {
            UnlockMessage.NOTHING_TO_RESTORE
        }
        mutableState.update { it.copy(message = message) }
        return message
    }

    fun shutdown() = billing.shutdown()

    private fun applyEntitlements(entitlements: Entitlements) {
        val unlocked = entitlements.owns(MonetizationConfig.PRO_UNLOCK.key())
        mutableState.update {
            it.copy(unlocked = unlocked, paywallVisible = it.paywallVisible && !unlocked)
        }
    }
}
