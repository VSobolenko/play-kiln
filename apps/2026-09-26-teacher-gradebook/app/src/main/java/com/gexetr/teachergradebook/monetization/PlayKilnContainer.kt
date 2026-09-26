package com.gexetr.teachergradebook.monetization

import android.app.Activity
import android.app.Application
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.PlayBillingController
import com.gexetr.teachergradebook.data.DocumentStore
import com.gexetr.teachergradebook.data.FileRecordBookRepository
import com.gexetr.teachergradebook.data.RecordBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference

/**
 * The app's dependencies, built once for the process.
 *
 * Hand-wired rather than injected by a library: there are five objects, each with a single
 * implementation and an obvious owner, so a dependency-injection framework would add a build step
 * and an indirection without removing a decision. The interfaces are still there — the repository
 * and the billing store are both replaceable — so the seams that matter for tests stay open.
 *
 * The activity reference is weak and cleared when the activity is destroyed, because Play's
 * purchase flow needs a live Activity and holding one strongly across a rotation is how a billing
 * library ends up launching a dialog on a dead window.
 */
class PlayKilnContainer(private val application: Application) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val documentStore = DocumentStore(application)

    val recordBookRepository: RecordBookRepository =
        FileRecordBookRepository(DocumentStore.recordFile(application))

    private var activityReference = WeakReference<Activity>(null)

    private val billingStateStore = PersistentBillingStateStore(application)

    val billing: BillingController = PlayBillingController(
        context = application,
        activityProvider = { activityReference.get() },
        store = billingStateStore,
    )

    val entitlements = EntitlementController(billing, scope)

    /** Called from the activity's `onCreate`; the purchase flow needs a live window. */
    fun attach(activity: Activity) {
        activityReference.clear()
        activityReference = WeakReference(activity)
    }

    /** Called from `onDestroy`, so a finished activity is never handed to Play. */
    fun detach(activity: Activity) {
        if (activityReference.get() === activity) activityReference.clear()
    }

    fun start() = entitlements.start()

    /** Releases the billing connection. The records stay on disk; nothing is deleted here. */
    fun stop() = entitlements.shutdown()
}
