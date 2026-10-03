package com.gexetr.lawnroundlog.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The free-tier limit itself is `com.gexetr.lawnroundlog.ui.round.FREE_TIER_CUSTOMER_LIMIT`, declared
// once beside the code that enforces it and imported here for the one sentence that quotes it. A
// second `8` in this file is a number that could drift from the rule it describes.

/**
 * The one product this app sells: a permanent, non-consumable unlock of the whole round.
 *
 * A `ProductId` rather than a string so `BillingCatalog.validate()` can check the shape before
 * anything reaches Play — a malformed ID found at upload costs a release cycle.
 */
val UNLOCK_PRODUCT: ProductId = ProductId("round_log_unlock")

/** What the unlock screen has to say about the purchase, as a state rather than as a message log. */
enum class UnlockStatus {
    /** Nothing to report; the screen shows the price and the button. */
    IDLE,

    /** The billing service is being reached for the first time. */
    CONNECTING,

    /** The Play flow is on screen, or a restore is running. */
    BUSY,

    /** The last attempt failed; the message is [com.gexetr.lawnroundlog.R.string.unlock_failed]. */
    FAILED,

    /** Play is not usable on this device; the message is `R.string.unlock_unavailable`. */
    UNAVAILABLE,
}

/**
 * The purchase, as the UI needs it: whether the unlock is owned, and what to say about the last
 * attempt.
 *
 * It exists as an interface for two reasons, both practical. The screen does not depend on the
 * billing SDK, so its states are reachable in a preview and in a test without Play; and the
 * capability this app is built against exposes a suspend `purchase()` that must run on a scope the
 * *screen* does not own, which is a decision a gate can make once instead of every call site.
 */
interface PurchaseGate {
    /** Whether the permanent unlock is owned. The single question every gated feature asks. */
    val owned: StateFlow<Boolean>

    /** The last thing that happened, for the one line of feedback the screen shows. */
    val status: StateFlow<UnlockStatus>

    /** Connects to Play and settles what the user already owns. Idempotent. */
    suspend fun connect()

    /** Starts the purchase flow. Returns immediately; the result arrives through [status]. */
    fun purchase()

    /** Re-reads purchases from Play, which is the recovery path after a reinstall. */
    fun restore()

    /** Clears a message the operator has read, so returning to the screen is quiet again. */
    fun clearStatus()

    /** Releases the scope and the Play connection. Called by the composition root, not by a screen. */
    fun shutdown()
}

/**
 * [PurchaseGate] over the real [BillingController].
 *
 * It adds no billing logic of its own — entitlement comes from the controller's own `entitlements`
 * flow, which is derived from Play and never from a local guess, so a reinstall recovers the unlock.
 * What this class owns is the two things that must not be a screen's business: the coroutine scope a
 * suspend purchase needs, and the mapping from Play's failure reasons onto something a person can
 * read.
 *
 * The product is one non-consumable, so the catalog it connects with declares exactly one in-app
 * product and no consumables and no subscriptions — which is also why the app never needs a
 * `PurchaseAuthority`: nothing here costs money to serve.
 */
class BillingPurchaseGate(
    private val controller: BillingController,
    private val product: ProductId = UNLOCK_PRODUCT,
    private val catalog: BillingCatalog = BillingCatalog(inAppProducts = listOf(product)),
    /**
     * Test-only: reports the unlock as owned without asking Play.
     *
     * The export handoffs sit behind this gate, and the gate is a Play purchase that cannot complete
     * for an app that is not on Play yet — so without this a debug build could not reach the paid
     * surface on a device, and the handoffs would ship untested. `MainActivity` passes true only when
     * a debug build was launched with the documented extra, and a release build's `BuildConfig.DEBUG`
     * is a compile-time false, so the branch and this parameter are folded away by R8 and the shipped
     * gate is exactly what it was.
     */
    debugOwned: Boolean = false,
) : PurchaseGate {

    /**
     * The scope for a purchase that outlives a recomposition.
     *
     * `Dispatchers.Main.immediate` because the billing SDK's purchase flow must be launched from the
     * main thread and its callbacks land there; the controller already moves its own work off it.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val debugOwnedFlow = MutableStateFlow(debugOwned)

    override val owned: StateFlow<Boolean> = if (debugOwned) {
        debugOwnedFlow.asStateFlow()
    } else {
        controller.entitlements
            .map { entitlements -> entitlements.owns(product.key()) }
            .stateIn(scope, SharingStarted.Eagerly, controller.entitlements.value.owns(product.key()))
    }

    private val mutableStatus = MutableStateFlow(UnlockStatus.IDLE)
    override val status: StateFlow<UnlockStatus> = mutableStatus.asStateFlow()

    override suspend fun connect() {
        mutableStatus.value = UnlockStatus.CONNECTING
        // A catalog that cannot be served correctly never becomes READY, so this is also the check
        // that keeps a typo in the product ID from looking like a network problem.
        mutableStatus.value = when (val result = controller.connect(catalog)) {
            is PlayKilnResult.Success -> UnlockStatus.IDLE
            is PlayKilnResult.Failure -> result.reason.toStatus()
        }
    }

    override fun purchase() {
        if (mutableStatus.value == UnlockStatus.BUSY) return
        mutableStatus.value = UnlockStatus.BUSY
        scope.launch {
            mutableStatus.value = when (val result = controller.purchase(product)) {
                is PlayKilnResult.Success -> UnlockStatus.IDLE
                is PlayKilnResult.Failure -> result.reason.toStatus()
            }
        }
    }

    override fun restore() {
        if (mutableStatus.value == UnlockStatus.BUSY) return
        mutableStatus.value = UnlockStatus.BUSY
        scope.launch {
            mutableStatus.value = when (val result = controller.restore()) {
                is PlayKilnResult.Success -> UnlockStatus.IDLE
                is PlayKilnResult.Failure -> result.reason.toStatus()
            }
        }
    }

    override fun clearStatus() {
        if (mutableStatus.value == UnlockStatus.BUSY || mutableStatus.value == UnlockStatus.CONNECTING) return
        mutableStatus.value = UnlockStatus.IDLE
    }

    override fun shutdown() {
        scope.cancel()
    }
}

/**
 * Play's failure, as something the unlock screen can say.
 *
 * `UNKNOWN` maps to [UnlockStatus.IDLE] and that is deliberate rather than lazy: the controller
 * reports a deliberate `USER_CANCELED` as `UNKNOWN`, and showing "the purchase did not complete" to
 * someone who just pressed back is a false alarm on the most common path. The cost is that a genuine
 * unknown error is also silent; the operator sees that nothing was charged and can try again, which
 * is a better failure than an error message that is wrong most of the time it appears.
 */
private fun PlayKilnResult.Reason.toStatus(): UnlockStatus = when (this) {
    PlayKilnResult.Reason.NOT_AVAILABLE, PlayKilnResult.Reason.NETWORK -> UnlockStatus.UNAVAILABLE
    PlayKilnResult.Reason.NOT_CONFIGURED,
    PlayKilnResult.Reason.NO_CONSENT,
    PlayKilnResult.Reason.POLICY_BLOCKED,
    -> UnlockStatus.FAILED
    PlayKilnResult.Reason.UNKNOWN -> UnlockStatus.IDLE
}

/**
 * The unlock screen's state, and the one thing the screen itself owns: dismissing a message.
 *
 * Ownership of the gate stays with the composition root — `MainActivity` creates it and shuts it
 * down — because the purchase outlives any one screen and a screen that shut the Play connection
 * down on the way out would leave the entitlement flow cold until it was rebuilt.
 */
class UnlockViewModel(private val gate: PurchaseGate) : ViewModel() {

    val owned: StateFlow<Boolean> = gate.owned

    val status: StateFlow<UnlockStatus> = gate.status

    suspend fun connect() {
        gate.connect()
    }

    fun purchase() {
        gate.purchase()
    }

    fun restore() {
        gate.restore()
    }

    fun dismissMessage() {
        gate.clearStatus()
    }

    companion object {
        fun factory(gate: PurchaseGate): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    UnlockViewModel(gate) as T
            }
    }
}
