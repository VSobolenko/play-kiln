package com.gexetr.capability.billing

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Entitlement and balance surface over Google Play Billing.
 *
 * The contract every implementation must honour, and the parts apps get wrong:
 *
 * 1. **[connect] is idempotent and retry-safe.** The billing service dies and reconnects
 *    during normal use; a UI must not have to care.
 * 2. **[restore] is the single source of truth for [entitlements].** What the user owns comes
 *    from Play, never from a local guess, so a reinstall or a new device recovers purchases.
 * 3. **Purchases are acknowledged.** An unacknowledged purchase is refunded by Play after
 *    three days, which looks like the app losing a sale.
 * 4. **A failed purchase never mutates [entitlements] or [wallet].** Only a confirmed
 *    purchase grants.
 * 5. **A consumable is consumed, not owned.** Buying one credits [wallet] and then consumes
 *    the purchase, or the product can never be bought a second time. This is the single most
 *    common consumable bug.
 *
 * ### What [restore] cannot do
 *
 * Play tracks *purchases*, not *balances*. Once a consumable is consumed, the only record of
 * what the user still has is ours. A wallet rebuilt from Play on a fresh install is therefore
 * always empty, which is why [PurchaseAuthority] and a server-held balance are required for any
 * consumable that costs money to fulfil.
 */
interface BillingController {
    /** Current lifecycle state; `READY` means purchasing may be attempted. */
    val availability: StateFlow<CapabilityAvailability>

    /** What the user owns right now: permanent unlocks and subscription states. */
    val entitlements: StateFlow<Entitlements>

    /** Spendable balances produced by consumable purchases. Empty until one is bought. */
    val wallet: StateFlow<Wallet>

    suspend fun connect(catalog: BillingCatalog): PlayKilnResult<Unit>

    /**
     * Re-reads purchases from Play; the recovery path after reinstall or device change.
     *
     * Recovers entitlements and active subscriptions. It **cannot** recover spent consumables
     * — see the note on this interface.
     */
    suspend fun restore(): PlayKilnResult<Entitlements>

    /**
     * Play's own formatted price for [product] — e.g. `"$9.99"` or `"9,99 ₽"` — or `null` when it
     * cannot be answered.
     *
     * The amount a user is charged is localized by Play to the account's country and currency, so an
     * app that prints its own literal shows dollars to a user who will be charged rubles. This is the
     * accessor a slot reads to display the real amount, and it is declared here with a default of
     * `null` so every implementor — the real controller, the fake, a preview, a demo build — answers
     * without being forced to invent a price it does not know.
     *
     * `null` means **unknown**: never "free", never a guessed string. The product may not be
     * configured yet, the billing service may be disconnected, or the query may time out, and a
     * caller's correct response to all three is the same — keep its own fallback rather than state an
     * amount Play will not charge. It is not an error.
     */
    suspend fun formattedPrice(product: ProductId): String? = null

    /** Launches the purchase flow for [product] and grants or credits on success. */
    suspend fun purchase(product: ProductId): PlayKilnResult<Entitlements>

    /**
     * Launches an intent-bound purchase and reports whether Play merely observed the purchase or
     * an entitlement was actually granted. Implementations that do not support server intents fail
     * closed rather than dropping the binding and launching the legacy flow.
     */
    suspend fun purchase(
        product: ProductId,
        intent: PurchaseIntent,
    ): PlayKilnResult<PurchaseResolution> =
        PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

    /** Acknowledges purchases that Play reported as pending; call on cold start. */
    suspend fun acknowledgePendingPurchases(): PlayKilnResult<Unit>

    /**
     * Spends [amount] of [unit] from [wallet].
     *
     * Fails with [PlayKilnResult.Reason.NOT_AVAILABLE] when the balance is insufficient rather
     * than clamping or going negative: a spend that cannot be covered is a bug in the caller,
     * and hiding it either gives away paid credit or silently drops the user's action.
     *
     * For a capability that costs real money per use, the authoritative debit happens
     * server-side; this is the local view and must not be the only guard.
     */
    suspend fun spend(unit: String, amount: Long): PlayKilnResult<Wallet>

    fun shutdown()
}
