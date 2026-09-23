package com.gexetr.capability.billing

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic [BillingController] for tests, previews, and screenshots.
 *
 * Part of the public API on purpose: slot tests must not touch Play, and a screenshot or demo
 * build must not be able to charge anyone.
 *
 * It settles through the same [PurchaseLedger] as the real controller, so a test of the fake is
 * a test of the actual credit, consume and idempotency rules rather than of a parallel
 * implementation that can drift.
 *
 * @param purchaseOutcomes scripted results per product, consumed in order and then falling back
 *   to success. This is what makes "network fails, entitlements stay unchanged" testable without
 *   a network.
 * @param seedEntitlements what the user already owns, for testing gated UI.
 * @param seedWallet starting balances, so a paywall can be tested without buying a pack first.
 * @param authority required when the catalog declares a server-cost pack, exactly as in
 *   production.
 */
class FakeBillingController(
    seedEntitlements: Entitlements = Entitlements.EMPTY,
    seedWallet: Wallet = Wallet.EMPTY,
    private val authority: PurchaseAuthority? = null,
    purchaseOutcomes: Map<ProductId, List<PlayKilnResult<Entitlements>>> = emptyMap(),
) : BillingController {

    private val store = InMemoryBillingStateStore(seedEntitlements, seedWallet)
    private val ledger = PurchaseLedger(store, authority)

    private val mutableAvailability = MutableStateFlow(CapabilityAvailability.UNINITIALIZED)
    override val availability: StateFlow<CapabilityAvailability> = mutableAvailability.asStateFlow()

    private val mutableEntitlements = MutableStateFlow(seedEntitlements)
    override val entitlements: StateFlow<Entitlements> = mutableEntitlements.asStateFlow()

    private val mutableWallet = MutableStateFlow(seedWallet)
    override val wallet: StateFlow<Wallet> = mutableWallet.asStateFlow()

    private val recorded = mutableListOf<String>()
    private val remainingOutcomes: MutableMap<ProductId, MutableList<PlayKilnResult<Entitlements>>> =
        purchaseOutcomes.mapValuesTo(mutableMapOf()) { (_, outcomes) -> outcomes.toMutableList() }

    private var connectedCatalog: BillingCatalog = BillingCatalog.EMPTY
    private var purchaseCounter = 0

    /** In order, so a test can assert the exact lifecycle the app performed. */
    fun calls(): List<String> = recorded.toList()

    fun catalog(): BillingCatalog = connectedCatalog

    override suspend fun connect(catalog: BillingCatalog): PlayKilnResult<Unit> {
        recorded += "connect"

        if (catalog.validate().isNotEmpty()) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
        }
        if (catalog.requiresPurchaseAuthority() && authority == null) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
        }

        connectedCatalog = catalog
        mutableAvailability.value = CapabilityAvailability.READY
        return PlayKilnResult.Success(Unit)
    }

    override suspend fun restore(): PlayKilnResult<Entitlements> {
        recorded += "restore"
        // Deliberately does not invent wallet state: Play knows purchases, not balances, so a
        // restore on a fresh install legitimately returns whatever the store still holds.
        syncFromStore()
        return PlayKilnResult.Success(mutableEntitlements.value)
    }

    override suspend fun purchase(product: ProductId): PlayKilnResult<Entitlements> {
        recorded += "purchase"

        val scripted = remainingOutcomes[product]?.removeFirstOrNull()
        if (scripted is PlayKilnResult.Failure) return scripted

        // A synthetic token makes the fake exercise the real token-based idempotency path.
        purchaseCounter += 1
        val record = PurchaseRecord(
            token = "fake-token-$purchaseCounter",
            productIds = listOf(product.value),
            state = PurchaseState.PURCHASED,
        )

        val settlement = ledger.settle(listOf(record), connectedCatalog)
        mutableEntitlements.value = settlement.entitlements
        mutableWallet.value = settlement.wallet

        return PlayKilnResult.Success(mutableEntitlements.value)
    }

    override suspend fun acknowledgePendingPurchases(): PlayKilnResult<Unit> {
        recorded += "acknowledgePendingPurchases"
        return PlayKilnResult.Success(Unit)
    }

    override suspend fun spend(unit: String, amount: Long): PlayKilnResult<Wallet> {
        recorded += "spend"

        val needsAuthority = connectedCatalog.consumables
            .firstOrNull { it.unit == unit }
            ?.fulfillment == Fulfillment.SERVER_COST

        if (needsAuthority) {
            val authority = authority
                ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)
            val confirmed = authority.confirmSpend(unit, amount)
            if (confirmed is PlayKilnResult.Failure) return confirmed
        }

        val debited = mutableWallet.value.debited(unit, amount)
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)

        mutableWallet.value = debited
        store.saveWallet(debited)
        return PlayKilnResult.Success(debited)
    }

    override fun shutdown() {
        recorded += "shutdown"
        mutableAvailability.value = CapabilityAvailability.UNINITIALIZED
    }

    private fun syncFromStore() {
        mutableEntitlements.value = store.loadEntitlements()
        mutableWallet.value = store.loadWallet()
    }
}
