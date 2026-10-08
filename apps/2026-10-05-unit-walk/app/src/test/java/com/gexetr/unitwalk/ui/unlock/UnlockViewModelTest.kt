package com.gexetr.unitwalk.ui.unlock

import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.FakeBillingController
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The unlock's outcomes, driven against the capability's own public
 * [FakeBillingController] — no network, no device and no Play services.
 *
 * The ViewModel takes its dispatcher as a parameter, so `Dispatchers.Unconfined` runs each launched
 * coroutine to completion inline and every assertion below reads a settled state. No temporary
 * directory is created, so nothing here writes outside the repository.
 *
 * [FakeBillingController] does **not** override `BillingController.formattedPrice`, which is itself
 * the proof that the interface change was additive: the fake compiles, and answers `null` (unknown)
 * through the default implementation, exactly as a preview or a demo build does. [PricedBillingController]
 * adds the one override a real Play connection would supply, so the display path is covered too.
 */
class UnlockViewModelTest {

    @Test
    fun anUnownedUnlockReportsNotOwnedAndStillConnects() {
        runBlocking {
            val billing = FakeBillingController()
            val viewModel = UnlockViewModel(billing, Dispatchers.Unconfined)

            assertEquals(CapabilityAvailability.READY, viewModel.uiState.value.availability)
            assertFalse(viewModel.uiState.value.isOwned)
            assertTrue(billing.calls().contains("connect"))
        }
    }

    @Test
    fun aSuccessfulPurchaseOutcomeFlipsTheUnlockToOwned() {
        runBlocking {
            val billing = FakeBillingController()
            val viewModel = UnlockViewModel(billing, Dispatchers.Unconfined)

            viewModel.onEvent(UnlockEvent.PurchaseRequested)

            assertTrue(billing.calls().contains("purchase"))
            assertTrue(viewModel.uiState.value.isOwned)
            assertEquals(UnlockMessage.PurchaseSucceeded, viewModel.uiState.value.message)
        }
    }

    @Test
    fun aFailedPurchaseSurfacesATypedErrorAndLeavesTheUnlockUnowned() {
        runBlocking {
            val failure: PlayKilnResult<Entitlements> =
                PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)
            val billing = FakeBillingController(
                purchaseOutcomes = mapOf(UNIT_WALK_UNLOCK to listOf(failure)),
            )
            val viewModel = UnlockViewModel(billing, Dispatchers.Unconfined)

            viewModel.onEvent(UnlockEvent.PurchaseRequested)

            assertFalse(viewModel.uiState.value.isOwned)
            assertEquals(UnlockMessage.PurchaseFailed, viewModel.uiState.value.message)
            assertEquals(PlayKilnResult.Reason.NETWORK, viewModel.uiState.value.lastFailure)
        }
    }

    @Test
    fun aSeededEntitlementReportsOwnedWithoutBuyingAnything() {
        runBlocking {
            val billing = FakeBillingController(
                seedEntitlements = Entitlements(owned = setOf(UNIT_WALK_UNLOCK.key())),
            )
            val viewModel = UnlockViewModel(billing, Dispatchers.Unconfined)

            assertTrue(viewModel.uiState.value.isOwned)
            assertFalse(billing.calls().contains("purchase"))
        }
    }

    @Test
    fun playsOwnPriceReachesTheStateForTheSheetToShow() {
        runBlocking {
            val billing = PricedBillingController(price = "9,99 ₽")
            val viewModel = UnlockViewModel(billing, Dispatchers.Unconfined)

            assertEquals("9,99 ₽", viewModel.uiState.value.priceLabel)
        }
    }

    @Test
    fun anUnansweredPriceStaysNullSoTheSheetKeepsItsFallback() {
        runBlocking {
            // `FakeBillingController` does not override `formattedPrice` at all, so this is the
            // interface's own default answering, exactly as a disconnected or preview build does.
            val billing = FakeBillingController()
            val viewModel = UnlockViewModel(billing, Dispatchers.Unconfined)

            assertNull(viewModel.uiState.value.priceLabel)
        }
    }

    @Test
    fun leavingTheUnlockSheetCannotShutTheSharedControllerDown() {
        runBlocking {
            val billing = FakeBillingController()
            val viewModel = UnlockViewModel(billing, Dispatchers.Unconfined)

            // `onCleared` is what the framework calls when this sheet leaves the back stack — a
            // `ViewModelStore` clearing, or Navigation 3 discarding the destination. The controller
            // belongs to the shell and backs the unit cap and the report watermark as well, so a
            // `shutdown` here would be the entire app losing billing for the rest of the session: the
            // user would have bought the unlock and watched it do nothing until a restart.
            viewModel.onCleared()

            assertFalse(billing.calls().contains("shutdown"))
        }
    }
}

/**
 * A [BillingController] that answers a price and delegates everything else.
 *
 * Interface delegation keeps this to the single override under test, and having a second implementor
 * of the interface is itself the evidence that `formattedPrice` was added **additively**: nothing
 * else had to change for this class to exist, and [FakeBillingController] did not have to change at
 * all.
 */
private class PricedBillingController(
    private val price: String?,
) : BillingController by FakeBillingController() {

    override suspend fun formattedPrice(product: ProductId): String? = price
}
