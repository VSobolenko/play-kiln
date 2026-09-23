package com.gexetr.capability.billing

/**
 * Spendable balance produced by consumable purchases.
 *
 * A consumable is deliberately **not** an entitlement. Play requires a consumable to be
 * consumed before the same product can be bought again, and the user does not "own" it
 * afterwards — they own a quantity. So the app tracks balances, keyed by unit, and a
 * purchase of `tokens_100` becomes `+100 tokens` rather than `owned += tokens_100`.
 *
 * Balances are modelled as an immutable map so they can be persisted, diffed in tests, and
 * restored from a server without touching the billing SDK.
 *
 * ### Why this type cannot be the only source of truth
 *
 * A balance held only on the device is editable by the user, and it resets on reinstall.
 * For anything that costs real money to fulfil — an AI inference, a generated asset — the
 * authoritative balance belongs on a server that has verified the purchase. This type is
 * the *displayed and cached* view; see [PurchaseAuthority] for the enforcement seam.
 */
data class Wallet(val balances: Map<String, Long> = emptyMap()) {

    /** Zero for a unit that has never been credited. */
    fun balanceOf(unit: String): Long = balances[unit] ?: 0L

    fun units(): Set<String> = balances.keys

    fun isEmpty(): Boolean = balances.values.all { it == 0L }

    /** Adds [amount] of [unit]. Rejects non-positive amounts: a zero-credit is always a bug. */
    fun credited(unit: String, amount: Long): Wallet {
        require(unit.isNotBlank()) { "unit must not be blank" }
        require(amount > 0) { "credit amount must be positive, was $amount" }
        return copy(balances = balances + (unit to balanceOf(unit) + amount))
    }

    /**
     * Removes [amount] of [unit], or returns `null` when the balance is insufficient.
     *
     * Returning `null` instead of throwing or clamping is the point: a spend that would go
     * negative is a bug in the calling app, and silently granting it either gives away paid
     * credit or hides the defect. The caller must handle the refusal.
     */
    fun debited(unit: String, amount: Long): Wallet? {
        require(unit.isNotBlank()) { "unit must not be blank" }
        require(amount > 0) { "debit amount must be positive, was $amount" }
        val current = balanceOf(unit)
        if (current < amount) return null
        return copy(balances = balances + (unit to current - amount))
    }

    /** True when [unit] holds at least [amount]. UI gates read this before offering an action. */
    fun canAfford(unit: String, amount: Long): Boolean = balanceOf(unit) >= amount

    companion object {
        val EMPTY = Wallet()
    }
}
