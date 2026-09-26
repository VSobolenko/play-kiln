package com.gexetr.pipesizing.monetization

import com.gexetr.capability.ads.AdsConfig
import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.ProductId
import com.gexetr.pipesizing.BuildConfig

/**
 * The one place the slot states what it sells, how often it interrupts, and which ad inventory it
 * uses. Kept as a single object so placement and pricing can be tuned without searching the UI.
 *
 * Every monetization decision here is the plan in `economics.json` made concrete:
 * the archetype is ads-led with a one-time unlock, and the unlock is exactly the free-tier limits
 * being lifted rather than a hidden capability the free app pretends to have.
 */
object MonetizationConfig {

    /** Saved cases a user may keep without buying anything. */
    const val FREE_CASE_LIMIT = 3

    /**
     * A completed evaluation is the only place an interstitial is allowed. It is never shown on
     * launch and never over an input, because an ad covering the field a fitter is typing into is
     * the complaint that this niche's incumbents are criticised for.
     */
    const val INTERSTITIAL_EVERY_N_EVALUATIONS = 3

    /** The single non-consumable this app sells. */
    val PRO_UNLOCK: ProductId = ProductId("pro_unlock")

    val catalog: BillingCatalog = BillingCatalog.utility(removeAds = PRO_UNLOCK)

    /**
     * Ad inventory. The banner unit is declared but unused: the ads capability's contract requires
     * a banner unit even though its public API exposes only interstitial and rewarded formats, and
     * this slot deliberately shows no banner at all.
     */
    val adsConfig: AdsConfig = AdsConfig(
        applicationId = BuildConfig.ADMOB_APP_ID,
        bannerUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID,
        interstitialUnitId = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
        rewardedUnitId = BuildConfig.ADMOB_REWARDED_UNIT_ID,
    )
}
