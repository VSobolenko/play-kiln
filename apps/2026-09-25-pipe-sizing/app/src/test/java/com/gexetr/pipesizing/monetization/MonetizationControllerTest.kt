package com.gexetr.pipesizing.monetization

import com.gexetr.capability.ads.AdsConfig
import com.gexetr.capability.ads.FakeAdsController
import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.FakeBillingController
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The whole monetization flow, driven by the capabilities' fakes, so nothing here touches Play or
 * AdMob. Every assertion is about a rule the plan in `economics.json` claims: consent before ads,
 * a purchase that Play has not confirmed cannot grant Pro, and the interstitial is capped.
 */
class MonetizationControllerTest {

    @Test
    fun startInitialisesAdsResolvesConsentAndConnectsBilling() {
        val ads = FakeAdsController()
        val billing = FakeBillingController()
        val controller = newController(ads, billing)

        controller.start()

        // Order matters: the ads SDK is initialised, then consent is resolved, then billing connects.
        assertEquals(listOf("initialize", "requestConsent"), ads.calls())
        assertEquals(listOf("connect"), billing.calls())
        assertTrue(controller.state.value.consentResolved)
        assertTrue(controller.state.value.adsAvailable)
        assertFalse(controller.state.value.pro)
    }

    @Test
    fun aRefusedConsentLeavesAdsUnavailableAndReported() = runBlocking {
        val ads = FakeAdsController(consentOutcome = PlayKilnResult.Failure(PlayKilnResult.Reason.NO_CONSENT))
        val controller = newController(ads, FakeBillingController())

        controller.start()

        assertTrue(controller.state.value.consentResolved)
        assertFalse("no ad may be requested before consent resolves", controller.state.value.adsAvailable)
        assertEquals(MonetizationMessage.CONSENT_UNAVAILABLE, controller.state.value.message)
        // And an evaluation cannot produce an interstitial without consent.
        assertFalse(controller.onEvaluationCompleted())
        assertFalse(ads.calls().contains("showInterstitial"))
    }

    @Test
    fun aConfirmedPurchaseGrantsProAndClosesThePaywall() = runBlocking {
        val billing = FakeBillingController()
        val controller = newController(FakeAdsController(), billing)
        controller.start()
        controller.openPaywall()

        val outcome = controller.purchasePro()

        assertEquals(PurchaseOutcome.SUCCESS, outcome)
        assertTrue(controller.state.value.pro)
        assertFalse("Pro must not be shown the paywall again", controller.state.value.paywallVisible)
        assertFalse("Pro must not see ads", controller.state.value.adsVisible)
        assertFalse(controller.state.value.purchaseInFlight)
    }

    @Test
    fun aPurchasePlayDoesNotConfirmStaysPendingAndGrantsNothing() = runBlocking {
        // An empty catalog is exactly the state where Play reports the purchase but nothing in the
        // ledger matches it, which is what a pending purchase looks like from here.
        val controller = newControllerWithCatalog(config(catalog = BillingCatalog.EMPTY))
        controller.start()

        val outcome = controller.purchasePro()

        assertEquals(PurchaseOutcome.PENDING, outcome)
        assertFalse("an unconfirmed purchase must never hand out Pro", controller.state.value.pro)
    }

    @Test
    fun aFailedPurchaseLeavesEntitlementsUntouchedAndThePaywallOpen() = runBlocking {
        val billing = FakeBillingController(
            purchaseOutcomes = mapOf(PRO to listOf(PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK))),
        )
        val controller = newController(FakeAdsController(), billing)
        controller.start()
        controller.openPaywall()

        val outcome = controller.purchasePro()

        assertEquals(PurchaseOutcome.FAILED, outcome)
        assertFalse(controller.state.value.pro)
        assertTrue("the paywall stays up so the user can retry", controller.state.value.paywallVisible)
        assertFalse(controller.state.value.purchaseInFlight)
    }

    @Test
    fun aCancelledPurchaseIsReportedAsCancelled() = runBlocking {
        val billing = FakeBillingController(
            purchaseOutcomes = mapOf(PRO to listOf(PlayKilnResult.Failure(PlayKilnResult.Reason.UNKNOWN))),
        )
        val controller = newController(FakeAdsController(), billing)
        controller.start()

        assertEquals(PurchaseOutcome.CANCELLED, controller.purchasePro())
        assertFalse(controller.state.value.pro)
    }

    @Test
    fun buyingAnUnavailableProductIsReportedAsUnavailableRatherThanFailed() = runBlocking {
        val billing = FakeBillingController(
            purchaseOutcomes = mapOf(PRO to listOf(PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE))),
        )
        val controller = newController(FakeAdsController(), billing)
        controller.start()

        assertEquals(PurchaseOutcome.UNAVAILABLE, controller.purchasePro())
    }

    @Test
    fun theInterstitialIsOfferedOnlyEveryConfiguredNumberOfEvaluations() = runBlocking {
        val ads = FakeAdsController()
        val controller = newController(ads, FakeBillingController(), every = 3)
        controller.start()

        val shown = (1..6).map { controller.onEvaluationCompleted() }

        assertEquals(listOf(false, false, true, false, false, true), shown)
        assertEquals(2, ads.calls().count { it == "showInterstitial" })
    }

    @Test
    fun anOwnerIsNeverInterrupted() = runBlocking {
        val ads = FakeAdsController()
        val ownsPro = FakeBillingController(seedEntitlements = Entitlements.EMPTY.granting(PRO.key(), asSubscription = false))
        val controller = newController(ads, ownsPro)
        controller.start()

        assertTrue(controller.state.value.pro)
        assertFalse(controller.state.value.adsVisible)
        // Even on the interval that would otherwise fire.
        repeat(9) { assertFalse(controller.onEvaluationCompleted()) }
        assertFalse(ads.calls().contains("showInterstitial"))
    }

    @Test
    fun restoreReportsWhenNothingIsOwned() = runBlocking {
        val controller = newController(FakeAdsController(), FakeBillingController())
        controller.start()

        assertFalse(controller.restore())
        assertFalse(controller.state.value.pro)
        assertEquals(MonetizationMessage.RESTORE_NONE, controller.state.value.message)
    }

    @Test
    fun restoreRecoversOwnershipRecordedByPlayFromAFreshInstall() = runBlocking {
        val ownsPro = FakeBillingController(seedEntitlements = Entitlements.EMPTY.granting(PRO.key(), asSubscription = false))
        val controller = newController(FakeAdsController(), ownsPro)
        controller.start()

        assertTrue(controller.restore())
        assertTrue(controller.state.value.pro)
        assertEquals(MonetizationMessage.RESTORE_SUCCESS, controller.state.value.message)
    }

    @Test
    fun aFailedAdChoicesReviewLeavesAdsUnavailable() = runBlocking {
        val ads = FakeAdsController(consentOutcome = PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK))
        val controller = newController(ads, FakeBillingController())
        controller.start()
        assertFalse(controller.state.value.adsAvailable)

        // A retry that also fails must not silently unlock inventory: the failure stays visible and
        // no ad may be requested.
        controller.reviewAdChoices()

        assertFalse(controller.state.value.adsAvailable)
        assertEquals(MonetizationMessage.CONSENT_UNAVAILABLE, controller.state.value.message)
        assertFalse(ads.calls().contains("showInterstitial"))
    }

    @Test
    fun aOneShotMessageIsClearedOnceConsumed() {
        val ads = FakeAdsController(consentOutcome = PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK))
        val controller = newController(ads, FakeBillingController())
        controller.start()
        assertEquals(MonetizationMessage.CONSENT_UNAVAILABLE, controller.state.value.message)

        controller.consumeMessage()

        assertNull(controller.state.value.message)
    }

    @Test
    fun shutdownClosesBothCapabilities() {
        val ads = FakeAdsController()
        val billing = FakeBillingController()
        val controller = newController(ads, billing)
        controller.start()

        controller.shutdown()

        assertEquals("shutdown", ads.calls().last())
        assertEquals("shutdown", billing.calls().last())
    }

    private fun newController(
        ads: FakeAdsController,
        billing: FakeBillingController,
        every: Int = MonetizationConfig.INTERSTITIAL_EVERY_N_EVALUATIONS,
    ) = MonetizationController(ads, billing, CoroutineScope(Dispatchers.Unconfined), config(every = every))

    private fun newControllerWithCatalog(context: MonetizationConfigContext) =
        MonetizationController(FakeAdsController(), FakeBillingController(), CoroutineScope(Dispatchers.Unconfined), context)

    private fun config(
        catalog: BillingCatalog = BillingCatalog.utility(removeAds = PRO),
        every: Int = MonetizationConfig.INTERSTITIAL_EVERY_N_EVALUATIONS,
    ) = MonetizationConfigContext(
        // Literal test inventory, so this test never depends on a build-time manifest placeholder.
        ads = AdsConfig(
            applicationId = "ca-app-pub-0000000000000000~0000000000",
            bannerUnitId = "ca-app-pub-0000000000000000/0000000000",
            interstitialUnitId = "ca-app-pub-0000000000000000/1111111111",
            rewardedUnitId = "ca-app-pub-0000000000000000/2222222222",
        ),
        catalog = catalog,
        proUnlock = PRO,
        interstitialEveryNEvaluations = every,
    )

    private companion object {
        val PRO = ProductId("pro_unlock")
    }
}
