package com.gexetr.pipesizing.monetization

import com.gexetr.capability.ads.AdsController
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The slot's monetization façade: one place that owns consent order, entitlement state, and the
 * frequency cap on interruptions.
 *
 * All of it sits on the two capabilities, never on AdMob or Billing types directly, so a slot test
 * can drive the whole flow with `FakeAdsController` and `FakeBillingController` and never touch the
 * network.
 *
 * The order is deliberate and non-negotiable: ads are initialised, then consent is resolved, and
 * only then can an ad be requested. That is a policy requirement in the EEA/UK, and it is enforced
 * by [AdsController] rather than by this class trusting itself.
 */
class MonetizationController(
    private val ads: AdsController,
    private val billing: BillingController,
    private val scope: CoroutineScope,
    private val config: MonetizationConfigContext = MonetizationConfigContext(MonetizationConfig),
) {
    private val mutableState = MutableStateFlow(MonetizationState())
    val state: StateFlow<MonetizationState> = mutableState.asStateFlow()

    /** Completed evaluations this session, used only for the interstitial frequency cap. */
    private var evaluationsSinceAdsBecameVisible = 0

    /**
     * Starts both capabilities. Returns immediately: the app must not wait on a network round trip
     * to draw its first screen, and every failure here is an ordinary state the UI already handles.
     */
    fun start() {
        ads.initialize(config.ads)

        scope.launch {
            val consent = ads.requestConsent()
            mutableState.update {
                it.copy(
                    consentResolved = true,
                    adsAvailable = ads.canRequestAds(),
                    message = if (consent is PlayKilnResult.Success) {
                        MonetizationMessage.CONSENT_RESOLVED
                    } else {
                        MonetizationMessage.CONSENT_UNAVAILABLE
                    },
                )
            }
        }

        scope.launch {
            // Connecting also restores what the user owns and settles any interrupted purchase,
            // because the capability does that inside connect() rather than leaving it to the app.
            billing.connect(config.catalog)
            applyEntitlements(billing.entitlements.value)
        }

        scope.launch {
            billing.entitlements.collect { applyEntitlements(it) }
        }
    }

    fun openPaywall() = mutableState.update { it.copy(paywallVisible = true) }

    fun dismissPaywall() = mutableState.update { it.copy(paywallVisible = false) }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    /**
     * Re-runs the consent flow. On the first run this is what shows the form; after a transient
     * failure it is the user's retry. It cannot change choices that are already recorded, because
     * the ads capability exposes no privacy-options entry point.
     */
    suspend fun reviewAdChoices(): Boolean {
        val consent = ads.requestConsent()
        val available = ads.canRequestAds()
        mutableState.update {
            it.copy(
                consentResolved = true,
                adsAvailable = available,
                message = if (consent is PlayKilnResult.Success) {
                    MonetizationMessage.CONSENT_RESOLVED
                } else {
                    MonetizationMessage.CONSENT_UNAVAILABLE
                },
            )
        }
        return available
    }

    /**
     * Buys the one-time unlock. A failure never mutates entitlements: only a purchase Play confirms
     * reaches [applyEntitlements], so a cancelled or failed flow cannot hand out Pro.
     */
    suspend fun purchasePro(): PurchaseOutcome {
        if (mutableState.value.pro) return PurchaseOutcome.SUCCESS

        mutableState.update { it.copy(purchaseInFlight = true) }
        val result = billing.purchase(config.proUnlock)
        mutableState.update { it.copy(purchaseInFlight = false) }

        return when (result) {
            is PlayKilnResult.Success -> {
                applyEntitlements(result.value)
                // Play can report a purchase while it is still pending, and a pending purchase is
                // not paid: the entitlement is what decides, never the call's own success.
                if (mutableState.value.pro) PurchaseOutcome.SUCCESS else PurchaseOutcome.PENDING
            }

            is PlayKilnResult.Failure -> when (result.reason) {
                PlayKilnResult.Reason.NOT_AVAILABLE,
                PlayKilnResult.Reason.NOT_CONFIGURED,
                -> PurchaseOutcome.UNAVAILABLE

                // The capability reports a user-cancelled flow as UNKNOWN; both it and a genuine
                // unknown outcome leave entitlement state untouched, so the user can simply retry.
                PlayKilnResult.Reason.UNKNOWN -> PurchaseOutcome.CANCELLED

                else -> PurchaseOutcome.FAILED
            }
        }
    }

    /** Re-reads purchases from Play. The recovery path after a reinstall or a new device. */
    suspend fun restore(): Boolean {
        val result = billing.restore()
        if (result is PlayKilnResult.Success) applyEntitlements(result.value)
        val restored = mutableState.value.pro
        mutableState.update {
            it.copy(
                message = if (restored) {
                    MonetizationMessage.RESTORE_SUCCESS
                } else {
                    MonetizationMessage.RESTORE_NONE
                },
            )
        }
        return restored
    }

    /**
     * Offers an interstitial at the one moment the app is allowed to: after a completed
     * evaluation, every [MonetizationConfig.INTERSTITIAL_EVERY_N_EVALUATIONS]th time, and never for
     * a Pro user. No fill, no consent, or no network are ordinary outcomes and simply mean no ad.
     */
    suspend fun onEvaluationCompleted(): Boolean {
        if (!mutableState.value.adsVisible) return false

        evaluationsSinceAdsBecameVisible += 1
        if (evaluationsSinceAdsBecameVisible % config.interstitialEveryNEvaluations != 0) return false

        return ads.showInterstitial() is PlayKilnResult.Success
    }

    /** Opt-in rewarded video. The reward is whatever the caller unlocks; this only reports it. */
    suspend fun watchRewarded(): Boolean {
        if (!mutableState.value.adsAvailable) return false
        return ads.showRewarded()
    }

    fun shutdown() {
        ads.shutdown()
        billing.shutdown()
    }

    private fun applyEntitlements(entitlements: Entitlements) {
        val pro = entitlements.owns(config.proUnlock.key())
        mutableState.update { it.copy(pro = pro, paywallVisible = it.paywallVisible && !pro) }
    }
}

/**
 * The values [MonetizationController] reads, as an injectable seam.
 *
 * Reading `MonetizationConfig` through a value makes the free limits and the frequency cap
 * assertable in a JVM test instead of being constants a test cannot vary.
 */
data class MonetizationConfigContext(
    val ads: com.gexetr.capability.ads.AdsConfig,
    val catalog: com.gexetr.capability.billing.BillingCatalog,
    val proUnlock: com.gexetr.capability.billing.ProductId,
    val interstitialEveryNEvaluations: Int,
) {
    constructor(config: MonetizationConfig) : this(
        ads = config.adsConfig,
        catalog = config.catalog,
        proUnlock = config.PRO_UNLOCK,
        interstitialEveryNEvaluations = config.INTERSTITIAL_EVERY_N_EVALUATIONS,
    )
}
