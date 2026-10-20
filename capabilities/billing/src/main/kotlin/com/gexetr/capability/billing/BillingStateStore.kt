package com.gexetr.capability.billing

/**
 * Durable state the billing capability must remember across process death.
 *
 * Two distinct facts live here, and conflating them causes real bugs:
 *
 * - **Entitlements** are recoverable from Play, so losing them is an inconvenience.
 * - **Wallet balances** are **not** recoverable from Play. Once a consumable is consumed, Play
 *   keeps no record of what the user still holds. A wallet lost here is paid-for credit lost,
 *   and the user has a legitimate refund claim.
 * - **Settled tokens** make purchase handling idempotent. Play re-reports an unconsumed purchase
 *   on every cold start; without this record the same payment can be granted repeatedly.
 * - **Pending purchase intent** preserves the server binding across process death until the server
 *   confirms it. A rejected purchase must not become a local entitlement on the next restore.
 *
 * A slot must supply a real implementation backed by `DataStore`/`Room` for any app that sells
 * consumables. The in-memory default exists so the capability is usable and honest in tests, not
 * as a production choice.
 */
interface BillingStateStore {
    fun loadEntitlements(): Entitlements
    fun saveEntitlements(entitlements: Entitlements)

    fun loadWallet(): Wallet
    fun saveWallet(wallet: Wallet)

    /** True when this purchase token has already been granted and must not be granted again. */
    fun isSettled(purchaseToken: String): Boolean
    fun markSettled(purchaseToken: String)

    /**
     * Persist the intent while Play's UI is open, so a process restart cannot turn a rejected
     * server-bound entitlement into a legacy local grant during restore.
     *
     * The defaults preserve existing store implementations but disable intent-bound purchases
     * until the store can durably support them.
     */
    fun savePendingPurchaseIntent(product: ProductId, intent: PurchaseIntent): Boolean = false
    fun loadPendingPurchaseIntent(): PendingPurchaseIntent? = null
    fun clearPendingPurchaseIntent(intentId: String) = Unit
}

data class PendingPurchaseIntent(
    val product: ProductId,
    val intent: PurchaseIntent,
)

/**
 * Process-lifetime [BillingStateStore].
 *
 * Correct for tests, previews, and demo builds. **Not** suitable for a shipped app that sells
 * consumables: the wallet and the settled-token ledger are both lost on process death, so a
 * restored app can neither see the user's balance nor remember what it already granted.
 */
class InMemoryBillingStateStore(
    initialEntitlements: Entitlements = Entitlements.EMPTY,
    initialWallet: Wallet = Wallet.EMPTY,
) : BillingStateStore {

    private var entitlements = initialEntitlements
    private var wallet = initialWallet
    private val settled = mutableSetOf<String>()
    private var pendingPurchaseIntent: PendingPurchaseIntent? = null

    override fun loadEntitlements(): Entitlements = entitlements

    override fun saveEntitlements(entitlements: Entitlements) {
        this.entitlements = entitlements
    }

    override fun loadWallet(): Wallet = wallet

    override fun saveWallet(wallet: Wallet) {
        this.wallet = wallet
    }

    override fun isSettled(purchaseToken: String): Boolean = purchaseToken in settled

    override fun markSettled(purchaseToken: String) {
        settled += purchaseToken
    }

    override fun savePendingPurchaseIntent(product: ProductId, intent: PurchaseIntent): Boolean {
        val pending = pendingPurchaseIntent
        val requested = PendingPurchaseIntent(product, intent)
        if (pending != null && pending != requested) return false
        pendingPurchaseIntent = requested
        return true
    }

    override fun loadPendingPurchaseIntent(): PendingPurchaseIntent? = pendingPurchaseIntent

    override fun clearPendingPurchaseIntent(intentId: String) {
        if (pendingPurchaseIntent?.intent?.intentId == intentId) {
            pendingPurchaseIntent = null
        }
    }
}
