package com.gexetr.capability.billing

import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settlement rules, tested without Android.
 *
 * Every test here maps to a way the factory could hand out the wrong thing: granting a pending
 * purchase, crediting one payment twice, acknowledging a consumable instead of consuming it, or
 * trusting a client-reported token for credit that costs money.
 */
class PurchaseLedgerTest {

    private val unlock = ProductId("premium")
    private val pack = ProductId("tokens_100")
    private val subscription = ProductId("monthly")

    private val catalog = BillingCatalog(
        inAppProducts = listOf(unlock),
        consumables = listOf(ConsumablePack(id = pack, grants = 100, unit = "tokens")),
        subscriptions = listOf(subscription),
    )

    private fun purchased(
        token: String,
        product: ProductId,
        acknowledged: Boolean = false,
        intentId: String? = null,
    ) =
        PurchaseRecord(
            token = token,
            productIds = listOf(product.value),
            state = PurchaseState.PURCHASED,
            acknowledged = acknowledged,
            intentId = intentId,
        )

    @Test
    fun `a consumable credits the wallet and is consumed rather than acknowledged`() = runTest {
        // The defect this guards: acknowledging a consumable leaves it un-consumed, so Play
        // refuses the second purchase of the same pack and the user can buy it exactly once.
        val ledger = PurchaseLedger(InMemoryBillingStateStore())

        val settlement = ledger.settle(listOf(purchased("t1", pack)), catalog)

        assertEquals(100L, settlement.wallet.balanceOf("tokens"))
        assertEquals(listOf("t1"), settlement.consume)
        assertTrue("a consumable must not also be acknowledged", settlement.acknowledge.isEmpty())
        assertTrue(settlement.entitlements.owned.isEmpty())
    }

    @Test
    fun `an entitlement grants and is acknowledged`() = runTest {
        val ledger = PurchaseLedger(InMemoryBillingStateStore())

        val settlement = ledger.settle(listOf(purchased("t2", unlock)), catalog)

        assertTrue(settlement.entitlements.owns(unlock.key()))
        assertEquals(listOf("t2"), settlement.acknowledge)
        assertTrue(settlement.consume.isEmpty())
    }

    @Test
    fun `an intent-bound entitlement is observed but not granted when authority rejects`() = runTest {
        val authority = IntentAwareAuthority(
            confirmation = PlayKilnResult.Failure(PlayKilnResult.Reason.POLICY_BLOCKED),
        )
        val ledger = PurchaseLedger(InMemoryBillingStateStore(), authority)

        val settlement = ledger.settle(
            listOf(purchased("server-rejected", unlock, intentId = "intent-rejected")),
            catalog,
        )

        assertFalse(settlement.entitlements.owns(unlock.key()))
        assertTrue(settlement.acknowledge.isEmpty())
        assertEquals(
            listOf(PurchaseOutcome.UnconfirmedEntitlement(unlock)),
            settlement.outcomes,
        )
        assertEquals(listOf("intent-rejected"), authority.confirmedIntentIds)
    }

    @Test
    fun `an intent-bound entitlement grants and acknowledges once after authority confirms`() = runTest {
        val authority = IntentAwareAuthority(confirmation = PlayKilnResult.Success(Unit))
        val store = InMemoryBillingStateStore()
        val ledger = PurchaseLedger(store, authority)
        val record = purchased("server-confirmed", unlock, intentId = "intent-confirmed")

        val confirmed = ledger.settle(listOf(record), catalog)
        val replay = ledger.settle(listOf(record), catalog)

        assertTrue(confirmed.entitlements.owns(unlock.key()))
        assertEquals(listOf("server-confirmed"), confirmed.acknowledge)
        assertTrue(replay.acknowledge.isEmpty())
        assertEquals(confirmed.entitlements, replay.entitlements)
        assertEquals(listOf("intent-confirmed"), authority.confirmedIntentIds)
        assertEquals(
            listOf(PurchaseOutcome.AlreadySettled("server-confirmed", unlock)),
            replay.outcomes,
        )
    }

    @Test
    fun `an entitlement without an intent keeps the legacy local path`() = runTest {
        val authority = IntentAwareAuthority(
            confirmation = PlayKilnResult.Failure(PlayKilnResult.Reason.POLICY_BLOCKED),
        )
        val ledger = PurchaseLedger(InMemoryBillingStateStore(), authority)

        val settlement = ledger.settle(listOf(purchased("legacy", unlock)), catalog)

        assertTrue(settlement.entitlements.owns(unlock.key()))
        assertEquals(listOf("legacy"), settlement.acknowledge)
        assertTrue(authority.confirmedIntentIds.isEmpty())
    }

    @Test
    fun `an already acknowledged entitlement is not acknowledged again`() = runTest {
        val ledger = PurchaseLedger(InMemoryBillingStateStore())

        val settlement = ledger.settle(listOf(purchased("t3", unlock, acknowledged = true)), catalog)

        assertTrue(settlement.entitlements.owns(unlock.key()))
        assertTrue(settlement.acknowledge.isEmpty())
    }

    @Test
    fun `a pending purchase grants nothing`() = runTest {
        // Granting here hands over the product before Play has taken the money.
        val ledger = PurchaseLedger(InMemoryBillingStateStore())
        val pending = PurchaseRecord("t4", listOf(pack.value), PurchaseState.PENDING)

        val settlement = ledger.settle(listOf(pending), catalog)

        assertEquals(0L, settlement.wallet.balanceOf("tokens"))
        assertTrue(settlement.consume.isEmpty())
        assertTrue(settlement.acknowledge.isEmpty())
        assertEquals(listOf(PurchaseOutcome.AwaitingPayment("t4")), settlement.outcomes)
    }

    @Test
    fun `the same token credits the wallet only once`() = runTest {
        // Play re-reports an unconsumed purchase on every cold start. Without per-token
        // idempotency one payment becomes several hundred tokens.
        val store = InMemoryBillingStateStore()
        val ledger = PurchaseLedger(store)
        val record = purchased("t5", pack)

        ledger.settle(listOf(record), catalog)
        val second = ledger.settle(listOf(record), catalog)

        assertEquals(100L, second.wallet.balanceOf("tokens"))
        assertEquals(listOf(PurchaseOutcome.AlreadySettled("t5")), second.outcomes)
    }

    @Test
    fun `two separate purchases of the same pack both credit`() = runTest {
        // The behaviour the factory could not express before: a repeatable purchase.
        val ledger = PurchaseLedger(InMemoryBillingStateStore())

        ledger.settle(listOf(purchased("t6", pack)), catalog)
        val second = ledger.settle(listOf(purchased("t7", pack)), catalog)

        assertEquals(200L, second.wallet.balanceOf("tokens"))
        assertEquals(listOf("t7"), second.consume)
    }

    @Test
    fun `a product the catalog does not sell is ignored`() = runTest {
        val ledger = PurchaseLedger(InMemoryBillingStateStore())

        val settlement = ledger.settle(listOf(purchased("t8", ProductId("legacy_item"))), catalog)

        assertTrue(settlement.entitlements.owned.isEmpty())
        assertTrue(settlement.consume.isEmpty())
        assertTrue(settlement.acknowledge.isEmpty())
    }

    @Test
    fun `a subscription purchase becomes an active subscription`() = runTest {
        val ledger = PurchaseLedger(InMemoryBillingStateStore())

        val settlement = ledger.settle(listOf(purchased("t9", subscription)), catalog)

        assertTrue(settlement.entitlements.hasActiveSubscription(subscription.key()))
    }

    @Test
    fun `a server-cost pack without an authority grants nothing`() = runTest {
        // Credit that costs real money to serve must not be granted on the client's word alone.
        val serverCost = BillingCatalog(
            consumables = listOf(
                ConsumablePack(
                    id = pack,
                    grants = 100,
                    unit = "tokens",
                    fulfillment = Fulfillment.SERVER_COST,
                )
            )
        )
        val ledger = PurchaseLedger(InMemoryBillingStateStore(), authority = null)

        val settlement = ledger.settle(listOf(purchased("t10", pack)), serverCost)

        assertEquals(0L, settlement.wallet.balanceOf("tokens"))
        assertTrue("an unconfirmed credit must not be consumed", settlement.consume.isEmpty())
        assertEquals(listOf(PurchaseOutcome.NotAuthorized(pack)), settlement.outcomes)
    }

    @Test
    fun `a server-cost pack is credited only when the authority confirms`() = runTest {
        val serverCost = BillingCatalog(
            consumables = listOf(
                ConsumablePack(
                    id = pack,
                    grants = 100,
                    unit = "tokens",
                    fulfillment = Fulfillment.SERVER_COST,
                )
            )
        )
        val ledger = PurchaseLedger(InMemoryBillingStateStore(), authority = AcceptingAuthority())

        val settlement = ledger.settle(listOf(purchased("t11", pack)), serverCost)

        assertEquals(100L, settlement.wallet.balanceOf("tokens"))
        assertEquals(listOf("t11"), settlement.consume)
    }

    @Test
    fun `a refused authority is retried on the next settle`() = runTest {
        // A failure must leave the purchase unsettled so a transient server error costs the user
        // a retry rather than the pack they paid for.
        val serverCost = BillingCatalog(
            consumables = listOf(
                ConsumablePack(
                    id = pack,
                    grants = 100,
                    unit = "tokens",
                    fulfillment = Fulfillment.SERVER_COST,
                )
            )
        )
        val authority = ScriptedAuthority(listOf(false, true))
        val ledger = PurchaseLedger(InMemoryBillingStateStore(), authority)
        val record = purchased("t12", pack)

        val refused = ledger.settle(listOf(record), serverCost)
        val retried = ledger.settle(listOf(record), serverCost)

        assertEquals(0L, refused.wallet.balanceOf("tokens"))
        assertEquals(100L, retried.wallet.balanceOf("tokens"))
    }
}

private class AcceptingAuthority : PurchaseAuthority {
    override suspend fun confirmPurchase(purchaseToken: String, product: ProductId) =
        PlayKilnResult.Success(Unit)

    override suspend fun confirmSpend(unit: String, amount: Long) = PlayKilnResult.Success(Unit)
}

/** Confirms the first [confirmations.size] purchases according to the scripted flags. */
private class ScriptedAuthority(private val confirmations: List<Boolean>) : PurchaseAuthority {
    private var index = 0

    override suspend fun confirmPurchase(purchaseToken: String, product: ProductId): PlayKilnResult<Unit> {
        val confirmed = confirmations.getOrElse(index) { true }
        index += 1
        return if (confirmed) {
            PlayKilnResult.Success(Unit)
        } else {
            PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)
        }
    }

    override suspend fun confirmSpend(unit: String, amount: Long) = PlayKilnResult.Success(Unit)
}

private class IntentAwareAuthority(
    private val confirmation: PlayKilnResult<Unit>,
) : PurchaseAuthority {
    val confirmedIntentIds = mutableListOf<String>()

    override suspend fun confirmPurchase(purchaseToken: String, product: ProductId) =
        PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

    override suspend fun confirmPurchase(
        purchaseToken: String,
        product: ProductId,
        intentId: String,
    ): PlayKilnResult<Unit> {
        confirmedIntentIds += intentId
        return confirmation
    }

    override suspend fun confirmSpend(unit: String, amount: Long) = PlayKilnResult.Success(Unit)
}
