package com.gexetr.capability.ads

import com.gexetr.capability.core.CapabilityAvailability
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.flow.StateFlow

/**
 * An error reported when the consent form could not be shown.
 *
 * Mirrors the SDK's own error surface (code plus message) instead of inventing semantics, so a
 * slot can decide for itself what to do with a given code. Deliberately our own type rather
 * than the SDK's `FormError`, so a slot never compiles against the consent SDK (see
 * `capabilities/AGENTS.md`).
 */
data class ConsentFormError(
    val errorCode: Int,
    val message: String,
)

/**
 * Consent-gated ad surface.
 *
 * The contract every implementation must honour, and the reason this capability exists:
 *
 * 1. **No ad request before consent resolves.** In the EEA/UK, requesting a personalised
 *    ad before the user has decided is a policy violation, so [canRequestAds] is the gate
 *    and it starts `false`.
 * 2. **Order is enforced, not documented.** [requestConsent] before [initialize], or any
 *    show call before consent, fails instead of silently doing nothing.
 * 3. **Never throw for an expected outcome.** "No fill", "no consent", and "offline" are
 *    ordinary results, returned as [PlayKilnResult.Failure].
 */
interface AdsController {
    /** Current lifecycle state; `READY` means ads may be shown. */
    val availability: StateFlow<CapabilityAvailability>

    /** Loads the SDK. Does not request ads and does not resolve consent. */
    fun initialize(config: AdsConfig)

    /**
     * Runs the consent flow and reports the outcome.
     *
     * [onDismissed] receives a non-null error only when the form itself failed to show;
     * a user who simply closed the form reports `null`.
     */
    suspend fun requestConsent(onDismissed: (ConsentFormError?) -> Unit = {}): PlayKilnResult<Unit>

    /** True only when consent has resolved and the SDK is ready to serve. */
    fun canRequestAds(): Boolean

    suspend fun showInterstitial(): PlayKilnResult<Unit>

    /** Returns true only when the user actually earned the reward. */
    suspend fun showRewarded(): Boolean

    fun shutdown()
}
