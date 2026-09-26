package com.gexetr.teachergradebook.monetization

import android.content.Context
import android.content.SharedPreferences
import com.gexetr.capability.billing.BillingStateStore
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.ProductKey
import com.gexetr.capability.billing.SubscriptionStatus
import com.gexetr.capability.billing.Wallet

/**
 * The billing capability's durable state.
 *
 * **SharedPreferences, not DataStore, and that is a deliberate deviation.** The capability
 * snapshots the store inside `PlayBillingController`'s constructor
 * (`MutableStateFlow(store.loadEntitlements())`), which means the durable read has to complete
 * *before* the controller is built. DataStore offers no synchronous read, so a DataStore-backed
 * store forces either a blocking read on the main thread during `Application.onCreate` or an
 * asynchronous dance that has to construct the controller twice — and the failure it buys is the
 * one the spec forbids outright: an unlocked app, reopened offline, showing the free tier.
 *
 * SharedPreferences is synchronous, durable, and the right size for this data: a handful of
 * product keys and tokens. The capability's guidance to use DataStore/Room is written for an app
 * that sells consumables and needs transactional wallet state; this app sells one permanent
 * unlock, and correctness at construction outweighs the library preference.
 *
 * Writes use `commit()` rather than `apply()` so a granted entitlement is on disk before the
 * purchase call returns. The file is a few hundred bytes, and the write happens off the main
 * thread because the capability's ledger runs on a background dispatcher.
 */
class PersistentBillingStateStore(context: Context) : BillingStateStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun loadEntitlements(): Entitlements = Entitlements(
        owned = prefs.getStringSet(KEY_OWNED, emptySet()).orEmpty().mapTo(mutableSetOf(), ::ProductKey),
        subscriptions = prefs.getStringSet(KEY_SUBSCRIPTIONS, emptySet()).orEmpty()
            .mapNotNull(::decodeSubscription)
            .toMap(),
    )

    override fun saveEntitlements(entitlements: Entitlements) {
        prefs.edit()
            .putStringSet(KEY_OWNED, entitlements.owned.mapTo(mutableSetOf()) { it.value })
            .putStringSet(
                KEY_SUBSCRIPTIONS,
                entitlements.subscriptions
                    .mapTo(mutableSetOf()) { (key, status) -> "${key.value}$SEPARATOR${status.name}" },
            )
            .commit()
    }

    override fun loadWallet(): Wallet = Wallet(
        prefs.getStringSet(KEY_WALLET, emptySet()).orEmpty()
            .mapNotNull(::decodeBalance)
            .toMap(),
    )

    override fun saveWallet(wallet: Wallet) {
        prefs.edit()
            .putStringSet(
                KEY_WALLET,
                wallet.balances.mapTo(mutableSetOf()) { (unit, amount) -> "$unit$SEPARATOR$amount" },
            )
            .commit()
    }

    /** The ledger that makes granting idempotent when Play re-reports an unconsumed purchase. */
    override fun isSettled(purchaseToken: String): Boolean =
        prefs.getStringSet(KEY_SETTLED, emptySet()).orEmpty().contains(purchaseToken)

    override fun markSettled(purchaseToken: String) {
        val current = prefs.getStringSet(KEY_SETTLED, emptySet()).orEmpty()
        prefs.edit().putStringSet(KEY_SETTLED, current + purchaseToken).commit()
    }

    private fun decodeSubscription(entry: String): Pair<ProductKey, SubscriptionStatus>? {
        val parts = entry.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val status = SubscriptionStatus.entries.firstOrNull { it.name == parts[1] } ?: return null
        return ProductKey(parts[0]) to status
    }

    private fun decodeBalance(entry: String): Pair<String, Long>? {
        val parts = entry.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val amount = parts[1].toLongOrNull() ?: return null
        return parts[0] to amount
    }

    private companion object {
        /** `=` cannot appear in a product key or a wallet unit, so the split is unambiguous. */
        const val SEPARATOR = "="
        const val FILE_NAME = "billing_state"

        const val KEY_OWNED = "owned"
        const val KEY_SUBSCRIPTIONS = "subscriptions"
        const val KEY_WALLET = "wallet"
        const val KEY_SETTLED = "settled_tokens"
    }
}
