package com.gexetr.capability.billing

import com.gexetr.capability.core.PlayKilnResult

/**
 * A purchase as the billing library reports it, decoupled from the Android type so the
 * settlement rules can be unit-tested without a device.
 */
data class PurchaseRecord(
    val token: String,
    val productIds: List<String>,
    val state: PurchaseState,
    val acknowledged: Boolean = false,
    val intentId: String? = null,
)

/** Mirrors `Purchase.PurchaseState` without importing it. */
enum class PurchaseState {
    /** Paid for. The only state that may grant anything. */
    PURCHASED,

    /** Play is still taking payment. Granting now would hand over the product for free. */
    PENDING,

    UNSPECIFIED,
}

/** What settlement did with one product of one purchase, so callers can log or assert it. */
sealed interface PurchaseOutcome {
    /** A consumable pack was credited to the wallet and the purchase is being consumed. */
    data class CreditedPack(val product: ProductId, val unit: String, val amount: Long) : PurchaseOutcome

    /** An entitlement or subscription was granted. */
    data class Granted(val product: ProductId, val isSubscription: Boolean) : PurchaseOutcome

    /** An intent-bound entitlement was seen, but the authority did not confirm it. */
    data class UnconfirmedEntitlement(val product: ProductId) : PurchaseOutcome

    /** A purchase token already settled; applying it again would duplicate its grant. */
    data class AlreadySettled(
        val token: String,
        val product: ProductId? = null,
    ) : PurchaseOutcome

    /** Reported by Play but not paid for yet, so nothing was granted. */
    data class AwaitingPayment(val token: String) : PurchaseOutcome

    /** The purchase is for a product this catalog does not sell. Ignored, never granted. */
    data class UnknownProduct(val token: String, val product: ProductId) : PurchaseOutcome

    /** A server-cost pack whose purchase the authority would not confirm. No credit granted. */
    data class NotAuthorized(val product: ProductId) : PurchaseOutcome
}

/**
 * The result of applying a set of purchases: the new state, plus the I/O still owed to Play.
 *
 * Returned as data rather than performed here so [PurchaseLedger] stays pure and testable — the
 * acknowledge and consume calls are the caller's to make.
 */
data class Settlement(
    val entitlements: Entitlements,
    val wallet: Wallet,
    /** Tokens to acknowledge. Empty for consumables, which are consumed instead. */
    val acknowledge: List<String>,
    /** Tokens to consume, which is what makes a consumable purchasable a second time. */
    val consume: List<String>,
    val outcomes: List<PurchaseOutcome>,
)

/**
 * Turns reported purchases into state changes, with the rules that are easy to get wrong in one
 * place:
 *
 * 1. Only `PURCHASED` grants. A pending purchase must never be granted.
 * 2. A consumable is **consumed**, not acknowledged — consume implies acknowledgement, and
 *    consuming is what makes the product buyable again. Skipping it means the user can buy the
 *    pack exactly once, forever.
 * 3. Settlement is **idempotent per token**. Play re-reports an unconsumed purchase on every
 *    cold start, so crediting on each report would multiply the user's balance for one payment.
 * 4. A server-cost pack is credited only when the authority confirms the token, and an
 *    intent-bound entitlement is granted only when the authority confirms that intent.
 * 5. A failure grants nothing: no credit, no entitlement, and no consume, so it retries later.
 */
class PurchaseLedger(
    private val store: BillingStateStore,
    private val authority: PurchaseAuthority? = null,
) {

    suspend fun settle(records: List<PurchaseRecord>, catalog: BillingCatalog): Settlement {
        var entitlements = store.loadEntitlements()
        var wallet = store.loadWallet()

        val acknowledge = mutableListOf<String>()
        val consume = mutableListOf<String>()
        val outcomes = mutableListOf<PurchaseOutcome>()

        records.forEach recordLoop@ { record ->
            if (record.state != PurchaseState.PURCHASED) {
                outcomes += PurchaseOutcome.AwaitingPayment(record.token)
                return@recordLoop
            }

            record.productIds.forEach productLoop@ { raw ->
                val product = ProductId(raw)

                if (product !in catalog.all()) {
                    outcomes += PurchaseOutcome.UnknownProduct(record.token, product)
                    return@productLoop
                }

                // Consumables are the only product kind that can be granted twice for one
                // payment, because Play keeps re-reporting them until they are consumed.
                val pack = catalog.packFor(product)

                if ((pack != null || record.intentId != null) && store.isSettled(record.token)) {
                    outcomes += PurchaseOutcome.AlreadySettled(
                        record.token,
                        product = product.takeIf { record.intentId != null },
                    )
                    return@productLoop
                }

                if (pack != null) {
                    if (!authorizePurchase(pack, product, record.token, record.intentId)) {
                        outcomes += PurchaseOutcome.NotAuthorized(product)
                        return@productLoop
                    }
                    wallet = wallet.credited(pack.unit, pack.grants)
                    store.markSettled(record.token)
                    consume += record.token
                    outcomes += PurchaseOutcome.CreditedPack(product, pack.unit, pack.grants)
                    return@productLoop
                }

                val isSubscription = catalog.isSubscription(product)
                if (record.intentId != null && !authorizePurchase(product, record.token, record.intentId)) {
                    outcomes += PurchaseOutcome.UnconfirmedEntitlement(product)
                    return@productLoop
                }
                entitlements = entitlements.granting(product.key(), isSubscription)
                if (record.intentId != null) store.markSettled(record.token)
                if (!record.acknowledged) acknowledge += record.token
                outcomes += PurchaseOutcome.Granted(product, isSubscription)
            }
        }

        store.saveEntitlements(entitlements)
        store.saveWallet(wallet)

        return Settlement(
            entitlements = entitlements,
            wallet = wallet,
            acknowledge = acknowledge.distinct(),
            consume = consume.distinct(),
            outcomes = outcomes,
        )
    }

    /** A local pack needs no authority; a server-cost pack is refused rather than trusted. */
    private suspend fun authorizePurchase(
        pack: ConsumablePack,
        product: ProductId,
        token: String,
        intentId: String?,
    ): Boolean {
        if (intentId == null && pack.fulfillment != Fulfillment.SERVER_COST) return true
        val authority = authority ?: return false
        return when (confirm(authority, token, product, intentId)) {
            is PlayKilnResult.Success<*> -> true
            is PlayKilnResult.Failure -> false
        }
    }

    private suspend fun authorizePurchase(
        product: ProductId,
        token: String,
        intentId: String,
    ): Boolean {
        val authority = authority ?: return false
        return authority.confirmPurchase(token, product, intentId) is PlayKilnResult.Success<*>
    }

    private suspend fun confirm(
        authority: PurchaseAuthority,
        token: String,
        product: ProductId,
        intentId: String?,
    ): PlayKilnResult<Unit> =
        if (intentId == null) {
            authority.confirmPurchase(token, product)
        } else {
            authority.confirmPurchase(token, product, intentId)
        }
}

/** State exposed by the intent-bound controller entry point. */
enum class PurchaseResolutionState {
    ENTITLEMENT_GRANTED,
    CONSUMABLE_CREDITED,
    PURCHASE_OBSERVED,
    AWAITING_PAYMENT,
}

/**
 * What the capability actually settled for an intent-bound flow.
 *
 * `PURCHASE_OBSERVED` is deliberately distinct from `ENTITLEMENT_GRANTED`: seeing a Play
 * purchase is not proof that the authority accepted it.
 */
data class PurchaseResolution(
    val product: ProductId,
    val state: PurchaseResolutionState,
    val entitlements: Entitlements,
    val wallet: Wallet,
)

internal fun Settlement.resolutionFor(product: ProductId): PurchaseResolution {
    val productOutcome = outcomes.firstOrNull { outcome ->
        when (outcome) {
            is PurchaseOutcome.CreditedPack -> outcome.product == product
            is PurchaseOutcome.Granted -> outcome.product == product
            is PurchaseOutcome.UnconfirmedEntitlement -> outcome.product == product
            is PurchaseOutcome.NotAuthorized -> outcome.product == product
            is PurchaseOutcome.UnknownProduct -> outcome.product == product
            is PurchaseOutcome.AlreadySettled -> outcome.product == product
            else -> false
        }
    }
    val state = when {
        productOutcome is PurchaseOutcome.Granted -> PurchaseResolutionState.ENTITLEMENT_GRANTED
        productOutcome is PurchaseOutcome.CreditedPack -> PurchaseResolutionState.CONSUMABLE_CREDITED
        productOutcome is PurchaseOutcome.AlreadySettled && entitlements.owns(product.key()) ->
            PurchaseResolutionState.ENTITLEMENT_GRANTED
        outcomes.any { it is PurchaseOutcome.AwaitingPayment } ->
            PurchaseResolutionState.AWAITING_PAYMENT
        else -> PurchaseResolutionState.PURCHASE_OBSERVED
    }
    return PurchaseResolution(product, state, entitlements, wallet)
}
