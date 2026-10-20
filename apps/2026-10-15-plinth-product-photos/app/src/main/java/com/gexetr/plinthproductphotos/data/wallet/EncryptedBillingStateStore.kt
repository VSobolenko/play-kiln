package com.gexetr.plinthproductphotos.data.wallet

import android.content.Context
import com.gexetr.capability.billing.BillingStateStore
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.PendingPurchaseIntent
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.billing.ProductKey
import com.gexetr.capability.billing.PurchaseIntent
import com.gexetr.capability.billing.Wallet
import org.json.JSONArray
import org.json.JSONObject

class EncryptedBillingStateStore(context: Context) : BillingStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val vault = AndroidKeystoreVault(KEY_ALIAS)

    @Synchronized
    override fun loadEntitlements(): Entitlements = read().entitlements

    @Synchronized
    override fun saveEntitlements(entitlements: Entitlements) {
        val current = read()
        val intentProduct = current.pendingPurchaseIntent?.product?.key()
        val trustedOwned = entitlements.owned.filterTo(current.entitlements.owned.toMutableSet()) { key ->
            key in current.entitlements.owned || key == intentProduct
        }
        write(current.copy(entitlements = Entitlements(owned = trustedOwned)))
    }

    @Synchronized
    override fun loadWallet(): Wallet = read().wallet

    @Synchronized
    override fun saveWallet(wallet: Wallet) {
        write(read().copy(wallet = wallet))
    }

    @Synchronized
    override fun isSettled(purchaseToken: String): Boolean =
        sha256Hex(purchaseToken) in read().settledTokenHashes

    @Synchronized
    override fun markSettled(purchaseToken: String) {
        val current = read()
        write(current.copy(settledTokenHashes = current.settledTokenHashes + sha256Hex(purchaseToken)))
    }

    @Synchronized
    override fun savePendingPurchaseIntent(product: ProductId, intent: PurchaseIntent): Boolean {
        val current = read()
        val requested = PendingPurchaseIntent(product, intent)
        if (current.pendingPurchaseIntent != null && current.pendingPurchaseIntent != requested) {
            return false
        }
        write(current.copy(pendingPurchaseIntent = requested))
        return true
    }

    @Synchronized
    override fun loadPendingPurchaseIntent(): PendingPurchaseIntent? = read().pendingPurchaseIntent

    @Synchronized
    override fun clearPendingPurchaseIntent(intentId: String) {
        val current = read()
        if (current.pendingPurchaseIntent?.intent?.intentId == intentId) {
            write(current.copy(pendingPurchaseIntent = null))
        }
    }

    private fun read(): BillingStateSnapshot {
        val ciphertext = preferences.getString(STATE_KEY, null) ?: return BillingStateSnapshot()
        val json = JSONObject(vault.decryptBase64(ciphertext))
        val owned = json.getJSONArray(FIELD_OWNED).strings().map(::ProductKey).toSet()
        val balancesJson = json.getJSONObject(FIELD_BALANCES)
        val balances = balancesJson.keys().asSequence().associate { key ->
            key to balancesJson.getString(key).toLong()
        }
        val pendingJson = json.optJSONObject(FIELD_PENDING)
        val pending = pendingJson?.let {
            PendingPurchaseIntent(
                product = ProductId(it.getString(FIELD_PRODUCT)),
                intent = PurchaseIntent(
                    intentId = it.getString(FIELD_INTENT_ID),
                    obfuscatedAccountId = it.getString(FIELD_ACCOUNT_ID),
                    obfuscatedProfileId = it.getString(FIELD_PROFILE_ID),
                ),
            )
        }
        return BillingStateSnapshot(
            entitlements = Entitlements(owned = owned),
            wallet = Wallet(balances),
            settledTokenHashes = json.getJSONArray(FIELD_SETTLED).strings().toSet(),
            pendingPurchaseIntent = pending,
        )
    }

    private fun write(snapshot: BillingStateSnapshot) {
        val balances = JSONObject().apply {
            snapshot.wallet.balances.forEach { (unit, amount) -> put(unit, amount.toString()) }
        }
        val json = JSONObject()
            .put(FIELD_OWNED, JSONArray(snapshot.entitlements.owned.map(ProductKey::value)))
            .put(FIELD_BALANCES, balances)
            .put(FIELD_SETTLED, JSONArray(snapshot.settledTokenHashes))

        snapshot.pendingPurchaseIntent?.let { pending ->
            json.put(
                FIELD_PENDING,
                JSONObject()
                    .put(FIELD_PRODUCT, pending.product.value)
                    .put(FIELD_INTENT_ID, pending.intent.intentId)
                    .put(FIELD_ACCOUNT_ID, pending.intent.obfuscatedAccountId)
                    .put(FIELD_PROFILE_ID, pending.intent.obfuscatedProfileId),
            )
        }

        val encrypted = vault.encryptToBase64(json.toString())
        check(preferences.edit().putString(STATE_KEY, encrypted).commit()) {
            "Could not persist encrypted billing state"
        }
    }

    private fun JSONArray.strings(): List<String> =
        List(length()) { index -> getString(index) }

    private data class BillingStateSnapshot(
        val entitlements: Entitlements = Entitlements.EMPTY,
        val wallet: Wallet = Wallet.EMPTY,
        val settledTokenHashes: Set<String> = emptySet(),
        val pendingPurchaseIntent: PendingPurchaseIntent? = null,
    )

    private companion object {
        const val PREFERENCES_NAME = "wallet_billing_state"
        const val KEY_ALIAS = "plinth_wallet_billing_state_v1"
        const val STATE_KEY = "encrypted_state"
        const val FIELD_OWNED = "owned"
        const val FIELD_BALANCES = "balances"
        const val FIELD_SETTLED = "settled"
        const val FIELD_PENDING = "pending"
        const val FIELD_PRODUCT = "product"
        const val FIELD_INTENT_ID = "intent_id"
        const val FIELD_ACCOUNT_ID = "account_id"
        const val FIELD_PROFILE_ID = "profile_id"
    }
}
