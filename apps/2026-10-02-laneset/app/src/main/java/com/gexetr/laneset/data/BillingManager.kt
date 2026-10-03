package com.gexetr.laneset.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How the connection to the Play Billing service currently stands.
 *
 * Three states rather than a boolean, because "we have not asked yet", "we asked and Play is there"
 * and "we asked and Play is not" are three different things to a UI: an unasked store must not be
 * rendered as a locked one, or the app tells a paying swimmer they own nothing while it is still
 * connecting.
 */
enum class BillingAvailability {
    UNINITIALIZED,
    READY,
    UNAVAILABLE,
}

/** The outcome of trying to connect to Play Billing. */
sealed interface ConnectOutcome {
    data object Connected : ConnectOutcome

    /** Play refused or never answered. The library's own code and words are passed through. */
    data class Failed(val responseCode: Int, val debugMessage: String?) : ConnectOutcome
}

/**
 * The outcome of asking Play to sell the unlock.
 *
 * A sealed type rather than a `Boolean` on purpose. "Purchased", "the swimmer closed the sheet" and
 * "the purchase did not complete" are different outcomes with different UI, and flattening them into
 * one boolean is how an app reports a purchase as successful when it did not complete — the defect
 * this type exists to make unrepresentable. [Failed] carries the library's own response code and
 * debug message rather than a generic message, so a caller can tell an unreachable Play from a
 * payment method the account does not have.
 */
sealed interface PurchaseOutcome {
    /** Play confirmed the purchase, and [BillingManager.isUnlocked] is already `true`. */
    data object Purchased : PurchaseOutcome

    /** The swimmer closed the Play sheet. Nothing was charged and nothing is owned. */
    data object Cancelled : PurchaseOutcome

    /** The purchase did not complete. */
    data class Failed(val responseCode: Int, val debugMessage: String?) : PurchaseOutcome

    /**
     * The purchase could not be attempted at all: the service is not connected, or the product is
     * not configured for this build. This is a configuration fault, not a refusal by the swimmer.
     */
    data class Unavailable(val responseCode: Int, val debugMessage: String?) : PurchaseOutcome
}

/**
 * The one thing the app sells: a single non-consumable unlock.
 *
 * The archetype is `purchase_led_single_unlock` — no subscription, no trial, no expiry, no ads and
 * no account — so there is exactly one product, it is bought once, and owning it is the whole of the
 * entitlement. It is a **non-consumable** (`ProductType.INAPP`), which means it is
 * **acknowledged but never consumed**: consuming a non-consumable makes Play treat it as spendable
 * again, which is how an app turns one sale into a refund and a support ticket.
 *
 * The entitlement is exposed as a [StateFlow] so a ViewModel can hoist it and the UI stays a function
 * of state (UDF); the store never talks to a composable.
 */
interface BillingManager {

    /**
     * Whether the unlock is owned.
     *
     * Seeded `false` and made authoritative by talking to Play — a purchase is recovered on a
     * reinstall or a new device by asking Play, never by trusting a local flag, because a local flag
     * is what a reinstall loses.
     */
    val isUnlocked: StateFlow<Boolean>

    /** Whether the billing service can be reached at all. */
    val availability: StateFlow<BillingAvailability>

    /** Connects, then settles what the swimmer already owns. Safe to call again. */
    suspend fun connect(): ConnectOutcome

    /**
     * Opens the Play purchase sheet for the unlock.
     *
     * @param activity the current `Activity`; the Play sheet is started from one, so a null or
     *   finished activity cannot be used and this returns [PurchaseOutcome.Unavailable] rather than
     *   throwing.
     */
    suspend fun launchPurchase(activity: Activity): PurchaseOutcome

    /**
     * Drops the connection. Called when the app goes to the background.
     *
     * [isUnlocked] is deliberately **not** reset: a disconnected service is not a lost purchase, and
     * the next [connect] re-establishes the truth from Play rather than from this flag.
     */
    fun disconnect()

    companion object {
        /**
         * The product id, as it must be typed in Play Console.
         *
         * It is a single value used by the purchase flow, the restore path and the store listing, so
         * it lives here once and is referenced by name rather than pasted (`.cursor/rules/
         * verification-discipline.mdc`, "one source of truth, imported — never restated"). A typo here
         * is not a compile error and not a test failure — it is `ITEM_UNAVAILABLE` in front of a
         * paying user — so it is stated as a constant a later reader can grep for and check against
         * the console.
         */
        const val UNLOCK_PRODUCT_ID: String = "laneset_unlock"
    }
}

private const val CONNECT_TIMEOUT_MS = 20_000L
private const val QUERY_TIMEOUT_MS = 15_000L
private const val ACK_TIMEOUT_MS = 15_000L

/** How long the app waits on the Play sheet before it stops waiting and says so. */
private const val PURCHASE_TIMEOUT_MS = 5 * 60_000L

/**
 * The real [BillingManager], over `com.android.billingclient:billing-ktx`.
 *
 * What this owns, and why each part is here rather than left to the caller:
 *
 * - **Acknowledgement.** Play refunds any purchase left unacknowledged for three days, and a refund
 *   looks exactly like a lost sale. Every purchase this class observes is acknowledged before it is
 *   reported as owned, so "the app says I own it" and "Play says I paid" cannot drift apart.
 * - **Recovery by asking Play.** On connect the owned purchases are read back from Play, which is
 *   what makes the unlock survive a reinstall, a new device and a cleared data directory.
 * - **Pending purchases.** A purchase that Play is still holding completes later, sometimes while the
 *   app is closed; reading purchases back on connect is where such a purchase is finally settled.
 *   A pending purchase is **not** treated as owned, because it is not paid for yet.
 *
 * The library's own response codes are carried out rather than collapsed, so a caller can tell a
 * refusal from a fault.
 */
class PlayBillingManager(
    context: Context,
    private val productId: String = BillingManager.UNLOCK_PRODUCT_ID,
) : BillingManager {

    private val appContext: Context = context.applicationContext ?: context

    /**
     * Carries the I/O that a non-suspend Play callback cannot await, so the acknowledgement and the
     * entitlement update finish before the caller is told the purchase succeeded.
     *
     * `Dispatchers.Default` rather than `Main`: the awaiting coroutine resumes on its own dispatcher
     * (a ViewModel's), so nothing here needs the main thread, and a local JVM test that never connects
     * is not exposed to `Dispatchers.Main`'s test-time initialisation trap.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val mutableIsUnlocked = MutableStateFlow(false)
    override val isUnlocked: StateFlow<Boolean> = mutableIsUnlocked.asStateFlow()

    private val mutableAvailability = MutableStateFlow(BillingAvailability.UNINITIALIZED)
    override val availability: StateFlow<BillingAvailability> = mutableAvailability.asStateFlow()

    /** Play's own record for the product, resolved before the first purchase flow. */
    @Volatile
    private var productDetails: ProductDetails? = null

    /** The purchase awaiting Play's answer; completed by [purchasesUpdatedListener]. */
    @Volatile
    private var pendingPurchase: CompletableDeferred<PurchaseOutcome>? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        val deferred = pendingPurchase
        pendingPurchase = null

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val confirmed = purchases.orEmpty()
                    .filter { purchase -> purchase.purchaseState == Purchase.PurchaseState.PURCHASED }
                scope.launch {
                    // Acknowledge first, then answer: an unacknowledged purchase is one Play takes
                    // back after three days, and a caller that sees `Purchased` must already be able
                    // to observe the entitlement.
                    confirmed.forEach { acknowledge(it) }
                    if (confirmed.any { ownsProduct(it) }) {
                        mutableIsUnlocked.value = true
                        deferred?.complete(PurchaseOutcome.Purchased)
                    } else {
                        // Play answered OK and returned nothing that reads as an owned unlock — a
                        // purchase that is still pending, or a product that is not this one.
                        // Reporting success here is exactly the lie this type exists to prevent.
                        deferred?.complete(
                            PurchaseOutcome.Failed(
                                result.responseCode,
                                "Play answered OK without a purchased unlock in the response",
                            ),
                        )
                    }
                }
            }

            // The swimmer closed the sheet: not a failure, and not a purchase.
            BillingClient.BillingResponseCode.USER_CANCELED ->
                deferred?.complete(PurchaseOutcome.Cancelled)

            // A non-consumable the account already owns. Play refuses the second sale, which is the
            // correct behaviour and means the unlock is held — but it is only reported as owned when
            // Play's own purchase list confirms it, because "already owned" with no recoverable
            // purchase is a state the caller must not be told is a success.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch {
                restoreOwnedPurchases()
                deferred?.complete(
                    if (mutableIsUnlocked.value) {
                        PurchaseOutcome.Purchased
                    } else {
                        PurchaseOutcome.Failed(result.responseCode, result.debugMessage)
                    },
                )
            }

            else -> deferred?.complete(PurchaseOutcome.Failed(result.responseCode, result.debugMessage))
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        // Required by the library before a purchase can be started. One-time products only: the app
        // sells nothing consumable and nothing recurring.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        // A dropped connection is normal on a phone; this keeps a later call from failing for a
        // reason the user cannot act on.
        .enableAutoServiceReconnection()
        .build()

    override suspend fun connect(): ConnectOutcome {
        if (!client.isReady) {
            val result = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    client.startConnection(object : BillingClientStateListener {
                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            if (continuation.isActive) continuation.resume(billingResult)
                        }

                        override fun onBillingServiceDisconnected() {
                            // Automatic reconnection is enabled above, so a later call retries. The
                            // entitlement is not cleared here: a lost service is not a lost purchase.
                        }
                    })
                }
            }

            if (result == null) {
                mutableAvailability.value = BillingAvailability.UNAVAILABLE
                return ConnectOutcome.Failed(
                    BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                    "the billing service did not answer within ${CONNECT_TIMEOUT_MS}ms",
                )
            }
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                mutableAvailability.value = BillingAvailability.UNAVAILABLE
                return ConnectOutcome.Failed(result.responseCode, result.debugMessage)
            }
        }

        mutableAvailability.value = BillingAvailability.READY

        // Settle what is already owned before the UI can offer to sell anything a second time.
        restoreOwnedPurchases()
        // Resolved eagerly so the first tap opens the sheet instead of a network round trip.
        productDetails = queryProductDetails()

        return ConnectOutcome.Connected
    }

    override suspend fun launchPurchase(activity: Activity): PurchaseOutcome {
        if (!client.isReady) {
            return PurchaseOutcome.Unavailable(
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                "billing is not connected; connect() must succeed before a purchase is started",
            )
        }

        // Play rejects a flow built from a product it does not know, so the details are resolved
        // first and a missing product is reported as the configuration fault it is.
        val details = productDetails ?: queryProductDetails()
        if (details == null) {
            return PurchaseOutcome.Unavailable(
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                "product '$productId' is not configured for this build",
            )
        }
        productDetails = details

        val deferred = CompletableDeferred<PurchaseOutcome>()
        pendingPurchase = deferred

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()

        val launch = client.launchBillingFlow(activity, params)
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return PurchaseOutcome.Failed(launch.responseCode, launch.debugMessage)
        }

        // The outcome arrives through the update listener. The timeout stops the app waiting on a
        // sheet the swimmer walked away from; it is reported as a failure to complete, never as a
        // purchase.
        return withTimeoutOrNull(PURCHASE_TIMEOUT_MS) { deferred.await() }
            ?: PurchaseOutcome.Failed(
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                "no answer from the Play purchase sheet within ${PURCHASE_TIMEOUT_MS}ms",
            )
    }

    override fun disconnect() {
        pendingPurchase = null
        productDetails = null
        client.endConnection()
        mutableAvailability.value = BillingAvailability.UNINITIALIZED
        // `isUnlocked` is intentionally left as it is; the next connect() re-reads Play.
    }

    /**
     * Reads the account's purchases back from Play and makes [isUnlocked] authoritative.
     *
     * A failed query leaves the current value alone: a network error must not lock a paying swimmer
     * out of what they bought.
     */
    private suspend fun restoreOwnedPurchases() {
        val purchases = queryPurchases() ?: return
        val owned = purchases.filter { purchase -> ownsProduct(purchase) }
        owned.forEach { acknowledge(it) }
        mutableIsUnlocked.value = owned.isNotEmpty()
    }

    /** One product, one entitlement: the unlock is owned when Play lists it as purchased. */
    private fun ownsProduct(purchase: Purchase): Boolean =
        productId in purchase.products && purchase.purchaseState == Purchase.PurchaseState.PURCHASED

    /**
     * Acknowledges a purchase so Play does not refund it.
     *
     * Never consumes: the unlock is a non-consumable, and consuming it would make it purchasable
     * again, which is the opposite of a one-time unlock.
     */
    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        withTimeoutOrNull(ACK_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                client.acknowledgePurchase(params) { _ ->
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    private suspend fun queryPurchases(): List<Purchase>? = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            ) { billingResult, purchases ->
                if (continuation.isActive) {
                    continuation.resume(
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            purchases
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    private suspend fun queryProductDetails(): ProductDetails? = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        // A one-time product, not a subscription: the archetype forbids recurrence.
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()

        suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (continuation.isActive) {
                    continuation.resume(
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            queryResult.productDetailsList.firstOrNull { it.productId == productId }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
