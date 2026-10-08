package com.gexetr.unitwalk.data

import android.content.Context
import android.content.SharedPreferences
import com.gexetr.capability.billing.BillingStateStore
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.InMemoryBillingStateStore
import com.gexetr.capability.billing.ProductKey
import com.gexetr.capability.billing.SubscriptionStatus
import com.gexetr.capability.billing.Wallet

/**
 * The billing capability's durable state, backed by [SharedPreferences].
 *
 * The capability ships an in-memory default so it is honest in tests, and a shipped app must not use
 * it: `PlayBillingController` snapshots the store **inside its constructor**
 * (`MutableStateFlow(store.loadEntitlements())`,
 * `capabilities/billing/src/main/kotlin/com/gexetr/capability/billing/PlayBillingController.kt:71-72`),
 * so a store that forgets on process death drops a paying user to the free tier — one unit and a
 * watermarked report — on every cold start without Play connectivity. The listing promises "It works
 * offline" (`store/listing.md:38`) and the sheet promises "One payment, yours for good"
 * (`res/strings_unlock.xml:13`); this class is what makes both true, because the entitlement is on
 * disk before the controller is built.
 *
 * ### Why SharedPreferences, and not DataStore
 *
 * [BillingStateStore] is synchronous and the controller reads it **once, in its constructor**, while
 * `DataStore` offers no synchronous read. Both can hold only if the durable read has already
 * completed before the controller exists, which leaves two options and one of them is wrong:
 *
 * - a `runBlocking { … .first() }` on the main thread during the controller's construction, which is
 *   what this store did while it was DataStore-backed — it stalls the first frame on binder and disk
 *   I/O; or
 * - an asynchronous read that constructs the controller once with `Entitlements.EMPTY` and replaces
 *   it when the read lands. That is the async dance this class exists to avoid: the controller seeds
 *   itself from the empty value and answers the free tier until a `restore()` succeeds, which is the
 *   one thing an offline start cannot do.
 *
 * `SharedPreferences` is synchronous by design, durable, and the right size for this data: a handful
 * of product keys and tokens. The capability's guidance to back the store with `DataStore`/`Room`
 * (`BillingStateStore.kt`) is written for an app that sells consumables and needs a transactional
 * wallet; this slot sells one permanent unlock, and correctness at construction outweighs the library
 * preference. The factory already resolved this exact interface the same way in
 * `apps/2026-09-26-teacher-gradebook/app/src/main/java/com/gexetr/teachergradebook/monetization/PersistentBillingStateStore.kt`,
 * whose KDoc states the same reasoning; this class follows that shape.
 *
 * ### What is persisted, and what deliberately is not
 *
 * Only **entitlements** reach the disk. This slot sells one non-consumable unlock and its catalog
 * declares no `ConsumablePack` (`ui/unlock/UnlockProducts.kt`: `UNIT_WALK_CATALOG`, one
 * `inAppProducts` entry), so:
 *
 * - [loadWallet] can only ever be empty — `PurchaseLedger` credits a wallet only in its pack branch
 *   (`PurchaseLedger.kt`, `if (pack != null)`), and no pack exists;
 * - [isSettled] is consulted only for a pack (the same branch), so [markSettled] is never reached and
 *   no token is ever recorded.
 *
 * Persisting either would write bytes that no code path can read back to any effect. Both are
 * therefore delegated to an [InMemoryBillingStateStore], which is the honest implementation for an app
 * that sells no consumable — a reasoned choice, not an oversight. **A future consumable in this slot
 * must revisit this class**: the in-memory default loses paid-for credit on process death, which the
 * capability's own KDoc states as the reason the interface exists.
 *
 * ### Write semantics
 *
 * [saveEntitlements] uses `commit()` rather than `apply()`: a granted entitlement must be on disk
 * before the purchase call returns, and `apply()` writes asynchronously — a process kill in that
 * window loses the local copy. `commit()` returns only once the edit is durable, which is the property
 * that matters here. It is synchronous, so it writes on whichever thread the capability settles on
 * (`PlayBillingController`'s own `Dispatchers.Default` scope when the purchase update listener fires,
 * the caller's dispatcher on the `restore()` / `acknowledgePendingPurchases()` path), but the file is
 * a few hundred bytes, so the write is short. The wallet and the settled-token ledger have no disk
 * write at all — they live in the in-memory mirror below, which is complete for an app with no
 * consumable.
 */
class DurableBillingStateStore(context: Context) : BillingStateStore {

    /**
     * The application context, never the caller's: this store is process-lifetime and must not keep
     * an `Activity` alive. `getSharedPreferences` is synchronous and returns the file handle
     * immediately; nothing is read from disk here.
     */
    private val prefs: SharedPreferences =
        (context.applicationContext ?: context).getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Entitlements round-trip through [prefs]; the wallet and settled tokens live here, in memory. */
    private val memory = InMemoryBillingStateStore()

    /**
     * The durable read, and it is synchronous by construction.
     *
     * `PlayBillingController` calls this in its constructor, so the stored entitlement must be the
     * answer on the first call — no hydration flag, no waiting, no coroutine. A cold start with Play
     * unreachable reads exactly what the last `saveEntitlements` committed.
     */
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

    override fun loadWallet(): Wallet = memory.loadWallet()

    override fun saveWallet(wallet: Wallet) {
        memory.saveWallet(wallet)
    }

    override fun isSettled(purchaseToken: String): Boolean = memory.isSettled(purchaseToken)

    override fun markSettled(purchaseToken: String) {
        memory.markSettled(purchaseToken)
    }

    private fun decodeSubscription(entry: String): Pair<ProductKey, SubscriptionStatus>? {
        val parts = entry.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val status = SubscriptionStatus.entries.firstOrNull { it.name == parts[1] } ?: return null
        return ProductKey(parts[0]) to status
    }

    private companion object {
        /** `=` cannot appear in a product key, so the split is unambiguous. */
        const val SEPARATOR = "="

        /**
         * The preferences file this store owns. It is named after the capability's state rather than
         * the slot, matching the teacher-gradebook precedent. `data/SettingsStore.kt` (`app_settings`)
         * and `data/ReviewPromptStore.kt` (`review_prompts`) keep their own files and their own
         * DataStore backends — only billing moved off DataStore, and only this file names
         * `billing_state`.
         */
        const val FILE_NAME = "billing_state"

        /**
         * Kept identical to the keys the store wrote when it was DataStore-backed, so the stored
         * vocabulary does not change with the backend. Both are string sets: a [ProductKey] is its
         * `.value`, and a subscription is `"<key>$SEPARATOR<status.name>"`.
         */
        const val KEY_OWNED = "owned"
        const val KEY_SUBSCRIPTIONS = "subscriptions"
    }
}
