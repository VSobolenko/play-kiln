package com.gexetr.pipesizing.monetization

import android.app.Activity
import android.content.Context
import com.gexetr.capability.ads.AdMobAdsController
import com.gexetr.capability.ads.AdsController
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.PlayBillingController
import com.gexetr.pipesizing.ui.cases.CasesRepository
import com.gexetr.pipesizing.ui.cases.DataStoreCasesRepository
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.atomic.AtomicReference

/**
 * The slot's manual dependency graph.
 *
 * It lives on the Application because the ads and billing clients are process-scoped: rebuilding
 * them on every configuration change would reconnect to Play and re-initialise the Ads SDK each
 * time the device rotates, which is slow and duplicates listeners.
 *
 * The current Activity is a reference rather than a constructor argument because the consent form
 * and the full-screen ad formats need one, while the Application outlives every Activity. A null
 * value is handled by the capabilities as "nothing can be shown right now", never as a crash.
 */
class PlayKilnContainer private constructor(
    private val billingStore: PersistentBillingStateStore,
    private val activityReference: AtomicReference<Activity?>,
    val ads: AdsController,
    val billing: BillingController,
    val cases: CasesRepository,
    val monetization: MonetizationController,
) {
    private var monetizationStarted = false

    /** Called by the Activity on resume and on pause; null means no Activity can host a form. */
    fun setActivity(activity: Activity?) {
        activityReference.set(activity)
    }

    /** Loads the persisted entitlement before billing connects, so Pro survives going offline. */
    suspend fun prepare() = billingStore.hydrate()

    fun startMonetization() {
        if (monetizationStarted) return
        monetizationStarted = true
        monetization.start()
    }

    fun close() {
        activityReference.set(null)
        monetization.shutdown()
    }

    companion object {
        fun create(context: Context, scope: CoroutineScope): PlayKilnContainer {
            val billingStore = PersistentBillingStateStore(context, scope)
            val activityReference = AtomicReference<Activity?>(null)
            val activityProvider = { activityReference.get() }

            val ads = AdMobAdsController(context, activityProvider)
            val billing = PlayBillingController(
                context = context,
                activityProvider = activityProvider,
                authority = null, // no consumable is sold, so no server confirmation path is needed
                store = billingStore,
            )

            return PlayKilnContainer(
                billingStore = billingStore,
                activityReference = activityReference,
                ads = ads,
                billing = billing,
                cases = DataStoreCasesRepository(context),
                monetization = MonetizationController(ads = ads, billing = billing, scope = scope),
            )
        }
    }
}
