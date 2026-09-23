package com.gexetr.capability.ads

import android.app.Activity
import android.content.Context
import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

private const val AD_LOAD_TIMEOUT_MS = 10_000L
private const val AD_SHOW_TIMEOUT_MS = 60_000L

/**
 * Real [AdsController] over the Google Mobile Ads SDK and the UMP consent SDK.
 *
 * Loads no inventory and shows nothing until the user has resolved the consent form, because
 * a personalised ad request before consent is a policy violation in the EEA/UK. The gate is
 * [canRequestAds], and it starts `false`.
 *
 * @param context any context; the application context is kept, so passing an Activity cannot
 *   leak it.
 * @param activityProvider the current Activity, needed by the consent form and by full-screen
 *   ad formats. Returning `null` (no Activity in the foreground) yields a `NOT_AVAILABLE`
 *   failure instead of a crash.
 */
class AdMobAdsController(
    context: Context,
    private val activityProvider: () -> Activity?,
) : AdsController {

    private val appContext: Context = context.applicationContext ?: context

    private val mutableAvailability = MutableStateFlow(CapabilityAvailability.UNINITIALIZED)
    override val availability: StateFlow<CapabilityAvailability> = mutableAvailability.asStateFlow()

    private val sdkInitialized = AtomicBoolean(false)

    private var config: AdsConfig? = null
    private var consentInformation: ConsentInformation? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    override fun initialize(config: AdsConfig) {
        if (config.validate().isNotEmpty()) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return
        }

        this.config = config
        mutableAvailability.value = CapabilityAvailability.NEEDS_CONSENT

        // Initialising the SDK is not an ad request, so it may start before consent. Loading
        // inventory may not, which is why canRequestAds() gates that instead of this flag.
        if (sdkInitialized.compareAndSet(false, true)) {
            MobileAds.initialize(appContext) { /* readiness is tracked through consent, not here */ }
        }
    }

    override suspend fun requestConsent(
        onDismissed: (ConsentFormError?) -> Unit,
    ): PlayKilnResult<Unit> {
        val activity = activityProvider()
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)

        val information = UserMessagingPlatform.getConsentInformation(appContext)
        consentInformation = information

        val updateError = suspendCancellableCoroutine { continuation ->
            information.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                { if (continuation.isActive) continuation.resume(null) },
                { error -> if (continuation.isActive) continuation.resume(error) },
            )
        }

        if (updateError != null) {
            mutableAvailability.value = CapabilityAvailability.FAILED
            return PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)
        }

        val formError = suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (continuation.isActive) continuation.resume(error)
            }
        }

        onDismissed(
            formError?.let { ConsentFormError(errorCode = it.errorCode, message = it.message) }
        )

        return if (information.canRequestAds()) {
            mutableAvailability.value = CapabilityAvailability.READY
            PlayKilnResult.Success(Unit)
        } else {
            // The user declined, so ads stay off. This is an ordinary outcome, not an error.
            mutableAvailability.value = CapabilityAvailability.FAILED
            PlayKilnResult.Failure(PlayKilnResult.Reason.NO_CONSENT)
        }
    }

    override fun canRequestAds(): Boolean =
        mutableAvailability.value == CapabilityAvailability.READY &&
            consentInformation?.canRequestAds() == true

    override suspend fun showInterstitial(): PlayKilnResult<Unit> {
        if (!canRequestAds()) return PlayKilnResult.Failure(PlayKilnResult.Reason.NO_CONSENT)

        val activity = activityProvider()
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)
        val unitId = config?.interstitialUnitId?.takeIf { it.isNotBlank() }
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

        val ad = interstitialAd ?: loadInterstitial(unitId)
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)

        val dismissed = withTimeoutOrNull(AD_SHOW_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
                ad.show(activity)
            }
        }

        // A shown interstitial is single-use, so never keep it for the next call.
        interstitialAd = null

        return if (dismissed == true) {
            PlayKilnResult.Success(Unit)
        } else {
            PlayKilnResult.Failure(PlayKilnResult.Reason.UNKNOWN)
        }
    }

    override suspend fun showRewarded(): Boolean {
        if (!canRequestAds()) return false

        val activity = activityProvider() ?: return false
        val unitId = config?.rewardedUnitId?.takeIf { it.isNotBlank() } ?: return false

        val ad = rewardedAd ?: loadRewarded(unitId) ?: return false

        val earned = withTimeoutOrNull(AD_SHOW_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Dismissal before the reward callback means no reward was earned.
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
                ad.show(activity) {
                    if (continuation.isActive) continuation.resume(true)
                }
            }
        }

        rewardedAd = null
        return earned == true
    }

    override fun shutdown() {
        interstitialAd = null
        rewardedAd = null
        consentInformation = null
        mutableAvailability.value = CapabilityAvailability.UNINITIALIZED
    }

    private suspend fun loadInterstitial(unitId: String): InterstitialAd? =
        withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                InterstitialAd.load(
                    appContext,
                    unitId,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            if (continuation.isActive) continuation.resume(ad)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            // "No fill" is routine, not an exception.
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }
        }

    private suspend fun loadRewarded(unitId: String): RewardedAd? =
        withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                RewardedAd.load(
                    appContext,
                    unitId,
                    AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            if (continuation.isActive) continuation.resume(ad)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }
        }
}
