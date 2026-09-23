package com.gexetr.capability.ads

/**
 * Identifiers for one app's ad inventory.
 *
 * Ad unit IDs are not secrets, but they are app-specific and easy to mistype, so
 * [validate] exists to turn a silent "no ads ever fill" into a clear failure. It is
 * meant to run in slot setup or in a debug-only check, not in a hot path.
 */
data class AdsConfig(
    val applicationId: String,
    val bannerUnitId: String,
    val interstitialUnitId: String = "",
    val rewardedUnitId: String = "",
    val rewardedInterstitialUnitId: String = "",
) {
    /**
     * Problems that make this configuration unusable. Empty means valid.
     *
     * Interstitial and rewarded units are optional: an app that only shows banners is
     * legitimate. A unit that is present but malformed is always a problem, because it
     * looks configured while never filling.
     */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()

        if (applicationId.isBlank()) {
            problems += "ads application ID is required"
        } else if (!applicationId.startsWith(ADMOB_APP_ID_PREFIX)) {
            problems += "ads application ID must start with $ADMOB_APP_ID_PREFIX"
        }

        if (bannerUnitId.isBlank()) {
            problems += "ads banner unit ID is required"
        }

        problems += adUnitProblems("banner", bannerUnitId)
        problems += adUnitProblems("interstitial", interstitialUnitId)
        problems += adUnitProblems("rewarded", rewardedUnitId)
        problems += adUnitProblems("rewarded interstitial", rewardedInterstitialUnitId)

        return problems
    }

    fun hasRewarded(): Boolean = rewardedUnitId.isNotBlank()

    private fun adUnitProblems(kind: String, value: String): List<String> =
        if (value.isBlank() || value.startsWith(ADMOB_UNIT_ID_PREFIX)) {
            emptyList()
        } else {
            listOf("$kind unit ID must start with $ADMOB_UNIT_ID_PREFIX")
        }

    companion object {
        const val ADMOB_APP_ID_PREFIX = "ca-app-pub-"
        const val ADMOB_UNIT_ID_PREFIX = "ca-app-pub-"
    }
}
