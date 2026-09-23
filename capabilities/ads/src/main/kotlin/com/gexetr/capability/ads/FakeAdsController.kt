package com.gexetr.capability.ads

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic [AdsController] for tests, previews, and screenshots.
 *
 * Part of the public API on purpose: a slot's tests must never touch the network, and a
 * store screenshot run must never show a live ad.
 */
class FakeAdsController(
    private val consentOutcome: PlayKilnResult<Unit> = PlayKilnResult.Success(Unit),
    private val rewardEarned: Boolean = false,
) : AdsController {

    private val mutableAvailability = MutableStateFlow(CapabilityAvailability.UNINITIALIZED)
    override val availability: StateFlow<CapabilityAvailability> = mutableAvailability.asStateFlow()

    private val recorded = mutableListOf<String>()
    private var latestConfig: AdsConfig? = null
    private var consentResolved = false

    /** In order, so a test can assert the exact lifecycle the app performed. */
    fun calls(): List<String> = recorded.toList()

    fun config(): AdsConfig? = latestConfig

    override fun initialize(config: AdsConfig) {
        recorded += "initialize"
        latestConfig = config
        mutableAvailability.value = CapabilityAvailability.NEEDS_CONSENT
    }

    override suspend fun requestConsent(
        onDismissed: (ConsentFormError?) -> Unit,
    ): PlayKilnResult<Unit> {
        recorded += "requestConsent"
        return when (consentOutcome) {
            is PlayKilnResult.Success -> {
                consentResolved = true
                mutableAvailability.value = CapabilityAvailability.READY
                PlayKilnResult.Success(Unit)
            }

            is PlayKilnResult.Failure -> {
                consentResolved = false
                mutableAvailability.value = CapabilityAvailability.FAILED
                consentOutcome
            }
        }
    }

    override fun canRequestAds(): Boolean = consentResolved

    override suspend fun showInterstitial(): PlayKilnResult<Unit> {
        recorded += "showInterstitial"
        return if (canRequestAds()) {
            PlayKilnResult.Success(Unit)
        } else {
            PlayKilnResult.Failure(PlayKilnResult.Reason.NO_CONSENT)
        }
    }

    override suspend fun showRewarded(): Boolean {
        recorded += "showRewarded"
        return canRequestAds() && rewardEarned
    }

    override fun shutdown() {
        recorded += "shutdown"
        consentResolved = false
        mutableAvailability.value = CapabilityAvailability.UNINITIALIZED
    }
}
