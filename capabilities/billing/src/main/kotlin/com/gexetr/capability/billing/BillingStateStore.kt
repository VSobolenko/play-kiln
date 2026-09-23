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
 * - **Settled tokens** are what make crediting idempotent. Play re-reports an unconsumed purchase
 *   on every cold start; without this record the same payment credits the wallet repeatedly.
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
}

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
}
