package com.gexetr.capability.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val CONNECT_TIMEOUT_MS = 20_000L
private const val QUERY_TIMEOUT_MS = 15_000L
private const val PURCHASE_TIMEOUT_MS = 5 * 60_000L
private const val ACK_TIMEOUT_MS = 15_000L
private const val MAX_CONNECT_ATTEMPTS = 3
private const val RETRY_DELAY_MS = 2_000L

/**
 * Real [BillingController] over the Google Play Billing Library.
 *
 * Owns the parts that are easy to get wrong and expensive to get wrong:
 *
 * - connection retry, because the billing service disconnects during normal use;
 * - entitlement state derived from Play, so a reinstall or a new device recovers purchases;
 * - **acknowledgement**, because Play refunds any purchase left unacknowledged for three days
 *   and a refunded purchase looks like a lost sale;
 * - pending purchases resumed on cold start, which is when an interrupted purchase actually
 *   completes.
 *
 * @param context any context; the application context is kept.
 * @param activityProvider the current Activity, required by the purchase flow. Returning
 *   `null` yields a `NOT_AVAILABLE` failure instead of a crash.
 */
class PlayBillingController(
    context: Context,
    private val activityProvider: () -> Activity?,
    private val authority: PurchaseAuthority? = null,
    private val store: BillingStateStore = InMemoryBillingStateStore(),
) : BillingController {

    private val appContext: Context = context.applicationContext ?: context

    private val ledger = PurchaseLedger(store, authority)

    private val mutableAvailability = MutableStateFlow(CapabilityAvailability.UNINITIALIZED)
    override val availability: StateFlow<CapabilityAvailability> = mutableAvailability.asStateFlow()

    // Seeded from the store rather than from an empty value: a wallet lost on process death is
    // paid-for credit lost, and the store is the only place it can survive.
    private val mutableEntitlements = MutableStateFlow(store.loadEntitlements())
    override val entitlements: StateFlow<Entitlements> = mutableEntitlements.asStateFlow()

    private val mutableWallet = MutableStateFlow(store.loadWallet())
    override val wallet: StateFlow<Wallet> = mutableWallet.asStateFlow()

    private var catalog: BillingCatalog = BillingCatalog.EMPTY

    /** Runs work that must resume after a non-suspend Play callback. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The purchase currently waiting on the Play UI; completed by the update listener. */
    private var awaitingPurchase: CompletableDeferred<PlayKilnResult<Entitlements>>? = null

    private val purchaseUpdateListener = PurchasesUpdatedListener { result, purchases ->
        val deferred = awaitingPurchase
        awaitingPurchase = null

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // Acknowledgement suspends, and this callback is not a coroutine. Applying the
                // purchases before completing means a caller that sees Success also sees the new
                // entitlements, instead of racing them.
                val confirmed = purchases.orEmpty()
                scope.launch {
                    applyPurchases(confirmed)
                    deferred?.complete(PlayKilnResult.Success(mutableEntitlements.value))
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                deferred?.complete(PlayKilnResult.Failure(PlayKilnResult.Reason.UNKNOWN))

            else ->
                deferred?.complete(PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK))
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchaseUpdateListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    override suspend fun connect(catalog: BillingCatalog): PlayKilnResult<Unit> {
        // Refuse before touching the network. A catalog that cannot be served correctly must not
        // reach a state where the UI believes purchasing is available.
        if (catalog.validate().isNotEmpty()) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
        }
        if (catalog.requiresPurchaseAuthority() && authority == null) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
        }

        this.catalog = catalog

        val result = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connectWithRetry() }
        if (result == null || result.responseCode != BillingClient.BillingResponseCode.OK) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return PlayKilnResult.Failure(
                if (result == null) PlayKilnResult.Reason.NETWORK else PlayKilnResult.Reason.NOT_AVAILABLE
            )
        }

        mutableAvailability.value = CapabilityAvailability.READY

        // Cold-start recovery: what the user owns, and any purchase Play is still holding
        // unacknowledged, must be settled before the UI offers to sell anything.
        restore()
        acknowledgePendingPurchases()

        return PlayKilnResult.Success(Unit)
    }

    override suspend fun restore(): PlayKilnResult<Entitlements> {
        if (!client.isReady) {
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)
        }

        val inApp = queryPurchases(BillingClient.ProductType.INAPP)
        val subscriptions = queryPurchases(BillingClient.ProductType.SUBS)
        if (inApp == null || subscriptions == null) {
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)
        }

        applyPurchases(inApp + subscriptions)
        return PlayKilnResult.Success(mutableEntitlements.value)
    }

    override suspend fun purchase(product: ProductId): PlayKilnResult<Entitlements> {
        if (!client.isReady) {
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)
        }

        val activity = activityProvider()
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)

        // Play rejects a flow built from an unknown product, so resolve it first.
        val details = queryProductDetails(product)
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

        val deferred = CompletableDeferred<PlayKilnResult<Entitlements>>()
        awaitingPurchase = deferred

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        val launch = client.launchBillingFlow(activity, params)
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            awaitingPurchase = null
            return PlayKilnResult.Failure(PlayKilnResult.Reason.UNKNOWN)
        }

        // The outcome arrives through the update listener, which is also why entitlements never
        // change on a failure: only a confirmed purchase reaches applyPurchases().
        return withTimeoutOrNull(PURCHASE_TIMEOUT_MS) { deferred.await() }
            ?: PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)
    }

    override suspend fun acknowledgePendingPurchases(): PlayKilnResult<Unit> {
        if (!client.isReady) {
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)
        }

        val inApp = queryPurchases(BillingClient.ProductType.INAPP)
        val subscriptions = queryPurchases(BillingClient.ProductType.SUBS)
        if (inApp == null || subscriptions == null) {
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)
        }

        applyPurchases(inApp + subscriptions)
        return PlayKilnResult.Success(Unit)
    }

    /**
     * Spends from the local wallet, after asking the authority first when the unit costs money
     * to serve.
     *
     * The order matters: a client-side balance is editable, so a server-cost unit must be debited
     * by the server *before* the local copy moves. If the server refuses, nothing local changes.
     */
    override suspend fun spend(unit: String, amount: Long): PlayKilnResult<Wallet> {
        val serverCost = catalog.consumables
            .firstOrNull { it.unit == unit }
            ?.fulfillment == Fulfillment.SERVER_COST

        if (serverCost) {
            val authority = authority
                ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
            val confirmed = authority.confirmSpend(unit, amount)
            if (confirmed is PlayKilnResult.Failure) return confirmed
        }

        val debited = mutableWallet.value.debited(unit, amount)
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)

        mutableWallet.value = debited
        store.saveWallet(debited)
        return PlayKilnResult.Success(debited)
    }

    override fun shutdown() {
        awaitingPurchase = null
        scope.cancel()
        client.endConnection()
        mutableAvailability.value = CapabilityAvailability.UNINITIALIZED
    }

    private suspend fun connectWithRetry(): BillingResult {
        var attempt = 0
        var result = disconnectResult()

        while (attempt < MAX_CONNECT_ATTEMPTS) {
            result = suspendCancellableCoroutine { continuation ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (continuation.isActive) continuation.resume(billingResult)
                    }

                    override fun onBillingServiceDisconnected() {
                        // Reconnection is enabled on the client, so a later call retries.
                    }
                })
            }

            if (result.responseCode == BillingClient.BillingResponseCode.OK) return result
            attempt++
            if (attempt < MAX_CONNECT_ATTEMPTS) delay(RETRY_DELAY_MS)
        }

        return result
    }

    private fun disconnectResult(): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            .setDebugMessage("connection was never established")
            .build()

    private suspend fun queryPurchases(productType: String): List<Purchase>? =
        withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                client.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(productType).build(),
                ) { billingResult, purchases ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                purchases
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }

    private suspend fun queryProductDetails(product: ProductId): ProductDetails? =
        withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            val productType = if (catalog.isSubscription(product)) {
                BillingClient.ProductType.SUBS
            } else {
                BillingClient.ProductType.INAPP
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(product.value)
                            .setProductType(productType)
                            .build()
                    )
                )
                .build()

            suspendCancellableCoroutine { continuation ->
                client.queryProductDetailsAsync(params) { billingResult, queryResult ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                queryResult.productDetailsList.firstOrNull()
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }

    /**
     * Applies confirmed purchases through the shared [PurchaseLedger], then performs the I/O the
     * ledger asked for: acknowledging entitlements and **consuming** consumables.
     *
     * The ledger decides and this method acts, so the rules that are easy to get wrong — never
     * granting a pending purchase, never crediting one payment twice, never acknowledging a
     * consumable instead of consuming it — live in testable code.
     */
    private suspend fun applyPurchases(purchases: List<Purchase>) {
        val records = purchases.map { purchase ->
            PurchaseRecord(
                token = purchase.purchaseToken,
                productIds = purchase.products,
                state = purchase.purchaseState.toPurchaseState(),
                acknowledged = purchase.isAcknowledged,
            )
        }

        val settlement = ledger.settle(records, catalog)
        mutableEntitlements.value = settlement.entitlements
        mutableWallet.value = settlement.wallet

        settlement.acknowledge.forEach { token -> acknowledge(token) }
        settlement.consume.forEach { token -> consume(token) }
    }

    /** Consuming is what makes a consumable purchasable again; it also implies acknowledgement. */
    private suspend fun consume(purchaseToken: String) {
        withTimeoutOrNull(ACK_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val params = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()
                client.consumeAsync(params) { _, _ ->
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    private suspend fun acknowledge(purchaseToken: String) {
        withTimeoutOrNull(ACK_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()
                client.acknowledgePurchase(params) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }
}

/**
 * Maps the library's int state onto our own enum, so the settlement rules can be tested without
 * the Android type. An unrecognised value is treated as `UNSPECIFIED`, which grants nothing —
 * the safe direction for an unknown state.
 */
private fun Int.toPurchaseState(): PurchaseState = when (this) {
    Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
    Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
    else -> PurchaseState.UNSPECIFIED
}
