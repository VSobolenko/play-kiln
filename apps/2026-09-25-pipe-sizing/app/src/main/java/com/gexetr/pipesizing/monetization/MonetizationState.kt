package com.gexetr.pipesizing.monetization

/** What a purchase attempt did, as the UI needs to describe it. */
enum class PurchaseOutcome {
    SUCCESS,
    PENDING,
    CANCELLED,
    UNAVAILABLE,
    FAILED,
}

/** A one-shot message for the UI to surface; cleared once shown. */
enum class MonetizationMessage {
    CONSENT_RESOLVED,
    CONSENT_UNAVAILABLE,
    PURCHASE_SUCCESS,
    PURCHASE_PENDING,
    PURCHASE_FAILED,
    PURCHASE_UNAVAILABLE,
    RESTORE_SUCCESS,
    RESTORE_NONE,
    REWARD_EARNED,
    REWARD_FAILED,
}

/**
 * Everything the UI needs to know about entitlement and ad readiness.
 *
 * `pro` is derived from what Play reports, never from a local flag, so a reinstall or a new device
 * recovers the purchase instead of charging again. `adsAvailable` is false until the user's ad
 * choices have resolved, which is what keeps a personalised ad request from happening before
 * consent.
 */
data class MonetizationState(
    val pro: Boolean = false,
    val consentResolved: Boolean = false,
    val adsAvailable: Boolean = false,
    val purchaseInFlight: Boolean = false,
    val paywallVisible: Boolean = false,
    val message: MonetizationMessage? = null,
) {
    /** Interstitial and rewarded inventory may be offered. Pro never sees either. */
    val adsVisible: Boolean get() = adsAvailable && !pro
}
