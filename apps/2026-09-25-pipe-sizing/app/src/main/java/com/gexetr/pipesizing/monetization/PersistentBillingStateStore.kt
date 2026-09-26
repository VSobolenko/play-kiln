package com.gexetr.pipesizing.monetization

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gexetr.capability.billing.BillingStateStore
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.InMemoryBillingStateStore
import com.gexetr.capability.billing.ProductKey
import com.gexetr.capability.billing.SubscriptionStatus
import com.gexetr.capability.billing.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.billingStateStore by preferencesDataStore(name = "billing_state")

/**
 * The billing capability's durable state, backed by DataStore.
 *
 * The capability ships an in-memory default so it is honest in tests, and a shipped app must not
 * use it: an entitlement lost on process death would drop a paying user to the free tier while
 * offline, which in a basement is exactly when a gas fitter is using the app. Entitlements are
 * recoverable from Play, but only when the network is there.
 *
 * Reads are served from memory because the capability's contract is synchronous, and the memory
 * copy is hydrated once from the store before billing connects. Writes are applied to memory
 * first and persisted in the background, so a slow disk never blocks a purchase.
 *
 * The settled-token ledger is kept here rather than in the capability's memory store because
 * `BillingStateStore` exposes no way to read it back, and a ledger that is written but never
 * persisted would re-credit a consumable on every cold start. This slot sells no consumable today;
 * the path is implemented so a later one cannot silently regress.
 */
class PersistentBillingStateStore(
    private val context: Context,
    private val scope: CoroutineScope,
) : BillingStateStore {

    private val memory = InMemoryBillingStateStore()
    private val settledTokens = mutableSetOf<String>()

    /** Loads the persisted snapshot into memory. Must run before the billing capability connects. */
    suspend fun hydrate() {
        val stored = context.billingStateStore.data.first()
        val owned = stored[KEY_OWNED].orEmpty().map(::ProductKey).toSet()
        val subscriptions = stored[KEY_SUBSCRIPTIONS].orEmpty()
            .mapNotNull(::decodeSubscription)
            .toMap()
        memory.saveEntitlements(Entitlements(owned = owned, subscriptions = subscriptions))
        memory.saveWallet(
            Wallet(stored[KEY_WALLET].orEmpty().mapNotNull(::decodeBalance).toMap()),
        )
        settledTokens.clear()
        settledTokens += stored[KEY_SETTLED].orEmpty()
    }

    override fun loadEntitlements(): Entitlements = memory.loadEntitlements()

    override fun saveEntitlements(entitlements: Entitlements) {
        memory.saveEntitlements(entitlements)
        persist()
    }

    override fun loadWallet(): Wallet = memory.loadWallet()

    override fun saveWallet(wallet: Wallet) {
        memory.saveWallet(wallet)
        persist()
    }

    override fun isSettled(purchaseToken: String): Boolean = purchaseToken in settledTokens

    override fun markSettled(purchaseToken: String) {
        settledTokens += purchaseToken
        memory.markSettled(purchaseToken)
        persist()
    }

    private fun persist() {
        val entitlements = memory.loadEntitlements()
        val wallet = memory.loadWallet()
        val settled = settledTokens.toSet()
        scope.launch {
            context.billingStateStore.edit { stored ->
                stored[KEY_OWNED] = entitlements.owned.mapTo(mutableSetOf()) { it.value }
                stored[KEY_SUBSCRIPTIONS] = entitlements.subscriptions
                    .mapTo(mutableSetOf()) { (key, status) -> "${key.value}$SEPARATOR${status.name}" }
                stored[KEY_WALLET] = wallet.balances
                    .mapTo(mutableSetOf()) { (unit, amount) -> "$unit$SEPARATOR$amount" }
                stored[KEY_SETTLED] = settled
            }
        }
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

        val KEY_OWNED = stringSetPreferencesKey("owned")
        val KEY_SUBSCRIPTIONS = stringSetPreferencesKey("subscriptions")
        val KEY_WALLET = stringSetPreferencesKey("wallet")
        val KEY_SETTLED = stringSetPreferencesKey("settled_tokens")
    }
}
