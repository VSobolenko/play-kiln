package com.gexetr.capability.billing

/**
 * How the value of a [ConsumablePack] is served once it is spent.
 *
 * This is a property of the app, not a preference: it decides whether the factory may keep the
 * balance on the device.
 */
enum class Fulfillment {
    /**
     * Spending the credit costs the app nothing beyond lost revenue — an extra theme, a filter,
     * a local feature. A local balance is sufficient, and cheating only costs us a sale.
     */
    LOCAL,

    /**
     * Each spend incurs a real cost to serve — an AI inference, a generated asset, a paid API
     * call. A device-side balance is editable and resets on reinstall, so a
     * [PurchaseAuthority] is required and [PlayBillingController] refuses to run without one.
     */
    SERVER_COST,
}

/**
 * A pack the user buys and then spends: credits, tokens, generations, coins.
 *
 * Modelled as a pack rather than a bare [ProductId] because the grant amount is a promise shown
 * on the paywall ("100 tokens") and a pricing decision. Keeping it in the catalog means the
 * store UI, the grant accounting, and the validation all read one declaration instead of three
 * copies of a number.
 *
 * Unlike an entitlement, a pack is **not** owned after purchase: Play requires it to be consumed
 * before it can be bought again, and what the user holds afterwards is a quantity, held in a
 * [Wallet].
 *
 * @param grants how much [unit] this pack adds to the [Wallet]. Must be positive.
 * @param unit the balance this pack credits. Required and explicit: an app may sell several
 *   kinds at once, and inferring the unit from the product name is how two packs silently
 *   collide.
 */
data class ConsumablePack(
    val id: ProductId,
    val grants: Long,
    val unit: String,
    val fulfillment: Fulfillment = Fulfillment.LOCAL,
) {
    /** Non-empty list of problems; empty means valid. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()

        id.validate()?.let(problems::add)

        if (grants <= 0) {
            problems += "consumable '${id.value}' must grant a positive amount, was $grants"
        }

        if (unit.isBlank()) {
            problems += "consumable '${id.value}' must declare a non-blank unit"
        }

        return problems
    }
}
