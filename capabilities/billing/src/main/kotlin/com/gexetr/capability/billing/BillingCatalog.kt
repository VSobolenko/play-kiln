package com.gexetr.capability.billing

/**
 * The products one app sells: permanent unlocks, spendable packs, and subscriptions.
 *
 * Declared once per slot and validated before a release, so a duplicate or malformed ID
 * fails locally rather than at Play upload.
 *
 * The three lists are not interchangeable. An entitlement is owned forever, a subscription
 * has a lifecycle, and a consumable is spent — each maps to a different Play Billing call and
 * a different question in the UI. Modelling them as one list of IDs is what produces the
 * classic bug where a token pack is granted permanently and can be bought only once.
 */
data class BillingCatalog(
    val inAppProducts: List<ProductId> = emptyList(),
    val consumables: List<ConsumablePack> = emptyList(),
    val subscriptions: List<ProductId> = emptyList(),
) {
    /** Every declared product ID, subscriptions first, then unlocks, then packs. */
    fun all(): List<ProductId> = subscriptions + inAppProducts + consumables.map { it.id }

    fun isSubscription(product: ProductId): Boolean = subscriptions.contains(product)

    fun isConsumable(product: ProductId): Boolean = consumables.any { it.id == product }

    /** The pack declaration for [product], or `null` when it is not a consumable. */
    fun packFor(product: ProductId): ConsumablePack? = consumables.firstOrNull { it.id == product }

    /** Every unit this catalog can credit, for seeding balance UI before anything is bought. */
    fun consumableUnits(): Set<String> = consumables.mapTo(mutableSetOf()) { it.unit }

    /**
     * True when at least one pack costs money to serve, and a [PurchaseAuthority] is therefore
     * mandatory. Kept as a catalog question so the requirement is derived from the declared
     * products rather than from an app remembering to pass one.
     */
    fun requiresPurchaseAuthority(): Boolean =
        consumables.any { it.fulfillment == Fulfillment.SERVER_COST }

    /** One message per problem: invalid IDs, bad packs, and duplicates. Empty means valid. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()

        // IDs first, then pack-specific rules, so a single bad consumable is reported once
        // rather than twice (the pack validates its own ID too).
        (subscriptions + inAppProducts).forEach { product ->
            product.validate()?.let(problems::add)
        }
        consumables.forEach { pack ->
            problems += pack.validate()
        }

        all()
            .groupingBy { it }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
            .forEach { duplicate ->
                problems += "product ID '${duplicate.value}' is declared more than once"
            }

        return problems
    }

    companion object {
        val EMPTY = BillingCatalog()

        /** A catalog with no entitlement for ads: the shape a utility slot usually starts from. */
        fun utility(removeAds: ProductId, pro: ProductId? = null): BillingCatalog =
            BillingCatalog(inAppProducts = listOfNotNull(removeAds, pro))
    }
}
