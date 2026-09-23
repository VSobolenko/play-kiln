package com.gexetr.capability.billing

import com.gexetr.capability.core.PlayKilnResult

/**
 * Where the truth about a credit or a spend lives.
 *
 * Google Play reports a purchase, and the client is free to report anything else it likes. A
 * patched build on a rooted device can invoke the purchase callback with a fabricated token, so
 * an app that credits on the callback's word hands out credit that may cost real money to
 * fulfil. Play's own guidance is to confirm the token against the Google Play Developer API
 * before granting.
 *
 * The same asymmetry applies to spending. A balance kept only on the device can be edited, so a
 * credit that costs money to serve is not safe to enforce locally.
 *
 * ### When this is required
 *
 * Only when the catalog declares a [Fulfillment.SERVER_COST] pack. A pack whose value is purely
 * internal — an extra theme, a cosmetic, a local feature — can be granted and spent locally,
 * because the worst outcome of cheating is lost revenue to us, not money paid out. Requiring a
 * server for those would block apps the factory has no reason to block.
 *
 * The factory deliberately ships **no default implementation**: a permissive default looks safe
 * and is not, which is worse than an absent one. [PlayBillingController] refuses to serve a
 * catalog that needs an authority without one, loudly, at connect time.
 */
interface PurchaseAuthority {
    /**
     * Confirms with a trusted server that [purchaseToken] is a real paid purchase of [product]
     * and has not already been granted.
     */
    suspend fun confirmPurchase(purchaseToken: String, product: ProductId): PlayKilnResult<Unit>

    /**
     * Confirms that [amount] of [unit] may be spent, and debits the authoritative balance.
     *
     * Called before any local debit, so a client-side balance edit cannot create spendable
     * credit.
     */
    suspend fun confirmSpend(unit: String, amount: Long): PlayKilnResult<Unit>
}
