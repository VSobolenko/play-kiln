package com.gexetr.unitwalk.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.PlayBillingController
import com.gexetr.unitwalk.R
import com.gexetr.unitwalk.data.DurableBillingStateStore
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.ui.about.AboutScreen
import com.gexetr.unitwalk.ui.compare.CompareScreen
import com.gexetr.unitwalk.ui.nav.AboutDestination
import com.gexetr.unitwalk.ui.nav.CompareDestination
import com.gexetr.unitwalk.ui.nav.ReportDestination
import com.gexetr.unitwalk.ui.nav.SettingsDestination
import com.gexetr.unitwalk.ui.nav.UnitsDestination
import com.gexetr.unitwalk.ui.nav.UnlockDestination
import com.gexetr.unitwalk.ui.nav.WalkDestination
import com.gexetr.unitwalk.ui.report.ReportScreen
import com.gexetr.unitwalk.ui.settings.SettingsRoute
import com.gexetr.unitwalk.ui.units.UnitsScreen
import com.gexetr.unitwalk.ui.unlock.UnlockScreen
import com.gexetr.unitwalk.ui.walk.WalkScreen

/**
 * The slot's destination host: the only place that knows which screen follows which.
 *
 * **The route graph, and why it has this shape.**
 *
 * ```
 * Units ──open a walk──> Walk ──finished──> Compare ──open report──> Report ──unlock──> Unlock
 *   ├──compare a walk─────────────────────────┘
 *   ├──settings──> Settings
 *   └──about─────> About
 * ```
 *
 * Both ways into [WalkDestination] arrive through one callback: the portfolio opens a walk it has
 * already persisted — whether that walk is new or resumed — by calling `onResumeWalk` with its id
 * (`ui/units/UnitsScreen.kt:92-98`), so the shell never has to invent an id or read the store.
 * `onStartWalk` is the tap that *asks* for a new walk and is not a navigation cue; the entry below
 * states why.
 *
 * A walk is finished *through* the comparison: the recorded room deltas live on the walk
 * (`Walk.differences`) and the finished report is written from them, so the step that records a
 * difference is the step between finishing a walk and reading its report. The comparison is also
 * reachable directly from the portfolio (`UnitsScreen.onOpenCompare`), because a landlord compares an
 * older walk without opening it again, and `CompareScreen` states the "no prior report" case honestly
 * rather than refusing.
 *
 * **The two-pane form.** [ListDetailSceneStrategy] is what makes the window size matter: below its
 * breakpoint `UnitsScreen` fills the window and every other destination replaces it, and above it the
 * portfolio stays in the leading pane while the walk it opens appears beside it. Without a strategy the
 * same back stack renders one stretched column on a tablet, which is the defect
 * `.cursor/rules/ui-ux-baseline.mdc` exists for.
 *
 * **Back is owned by the host.** `rememberNavBackStack` keeps the stack across a process kill
 * (`@Serializable` keys), `onBack` drives predictive back, and every screen's own "back" control calls
 * the same [pop] — a screen never rewrites the stack itself.
 *
 * [hapticsEnabled] is a constructor parameter rather than a read of the store: the preference is owned
 * by the settings store and hoisted by `MainActivity`, so the shell stays a function of its arguments
 * and its callers stay testable. It is spent on the one act the shell performs — a screen change — as a
 * long-press tick, which is what makes the toggle in Settings observable instead of decorative.
 *
 * **The one entitlement source.** `billing` is created here, once, and handed to every screen that has
 * to know whether the one-time unlock is owned: the portfolio (its unit cap), the report (its
 * watermark), and the Unlock sheet (the purchase flow). All three ask the same shared question through
 * `UnlockState.isOwned` in `ui/unlock/UnlockProducts.kt`, so the product key and the paid boundary live
 * in one file. It is one instance with one Play connection and one durable store, which is what makes a
 * purchase visible to the cap and the watermark **in the same session**: a second controller would carry
 * its own store, so the sheet would unlock itself and nothing else until the app restarted. It is given
 * a live `Activity` provider because the purchase flow needs one, and its lifetime is this
 * composition's — the `DisposableEffect` below is the only thing that shuts it down.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun UnitWalkApp(hapticsEnabled: Boolean = true) {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(UnitsDestination)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val haptics = LocalHapticFeedback.current

    val context = LocalContext.current
    // The application context is stable across a `LocalContext` change, so the store and the controller
    // are not rebuilt when only the wrapper context differs — rebuilding the store is what would spin up
    // a second DataStore over the same file.
    val appContext = context.applicationContext ?: context
    // Re-read every composition: a configuration change must hand the purchase flow the Activity that is
    // alive now, not the one this composition started with.
    val currentActivity = rememberUpdatedState(context.findActivity())
    val billingStore = remember(appContext) { DurableBillingStateStore(appContext) }
    val billing: BillingController = remember(appContext, billingStore) {
        PlayBillingController(
            context = appContext,
            activityProvider = { currentActivity.value },
            store = billingStore,
        )
    }
    DisposableEffect(billing) {
        onDispose { billing.shutdown() }
    }

    /**
     * One screen change: the tick, then the push. Kept as one lambda so no destination can be reached
     * without the haptic the preference promises, and so [hapticsEnabled] is read in the one place
     * that can honour it.
     */
    val navigate: (NavKey) -> Unit = { destination ->
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        backStack.push(destination)
    }

    // Without this Surface nothing between the window and `NavDisplay` provides a content colour, so
    // `LocalContentColor` keeps Compose's default `Color.Black` and every default-coloured Text/Icon
    // is invisible when the window background is dark. `Surface` sets
    // `contentColor = contentColorFor(containerColor)`, and this `background` yields `onBackground`.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.pop() },
            sceneStrategies = listOf(listDetailStrategy),
            entryProvider = entryProvider {
                entry<UnitsDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = { DetailPlaceholder() },
                    ),
                ) {
                    UnitsScreen(
                        // Deliberately nothing to navigate to yet. The portfolio creates the walk
                        // itself when the same tap dispatches `UnitsEvent.StartWalk`, persists it, and
                        // *then* hands the shell its id through `onResumeWalk` from
                        // `LaunchedEffect(state.openWalk)` in `ui/units/UnitsScreen.kt:92-98` — "so
                        // the id the shell navigates with always names a walk a restart would still
                        // find". An optimistic push here would race that write and land on
                        // `WalkMessage.NothingToResume`, then leave a dead walk screen under the real
                        // one when `onResumeWalk` arrived. The parameters are typed so the callback's
                        // contract is visible at the call site.
                        onStartWalk = { _: String, _: WalkKind -> Unit },
                        onResumeWalk = { walkId -> navigate(WalkDestination(walkId)) },
                        onOpenCompare = { walkId -> navigate(CompareDestination(walkId)) },
                        onOpenUnlock = { navigate(UnlockDestination) },
                        onOpenSettings = { navigate(SettingsDestination) },
                        onOpenAbout = { navigate(AboutDestination) },
                        billing = billing,
                    )
                }
                entry<WalkDestination>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    WalkScreen(
                        walkId = key.walkId,
                        onBack = { backStack.pop() },
                        // Finishing a walk opens the comparison rather than the report: the
                        // differences recorded there are what the report is written from.
                        onFinished = { walkId -> navigate(CompareDestination(walkId)) },
                    )
                }
                entry<CompareDestination>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    CompareScreen(
                        walkId = key.walkId,
                        onBack = { backStack.pop() },
                        onOpenReport = { walkId -> navigate(ReportDestination(walkId)) },
                    )
                }
                entry<ReportDestination>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    ReportScreen(
                        walkId = key.walkId,
                        onBack = { backStack.pop() },
                        onOpenUnlock = { navigate(UnlockDestination) },
                        billing = billing,
                    )
                }
                entry<UnlockDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    UnlockScreen(onBack = { backStack.pop() }, billing = billing)
                }
                entry<SettingsDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsRoute(onBack = { backStack.pop() })
                }
                entry<AboutDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    AboutScreen(onBack = { backStack.pop() })
                }
            },
        )
    }
}

/** One pop, and only when there is something to pop. */
private fun NavBackStack<NavKey>.pop() {
    if (size > 1) removeLastOrNull()
}

/** One push, and never a duplicate of the destination already on top. */
private fun NavBackStack<NavKey>.push(destination: NavKey) {
    if (lastOrNull() != destination) add(destination)
}

/**
 * The purchase flow needs a live `Activity`, and the current context may be a wrapper.
 *
 * Unwrapping the chain rather than casting means a themed or otherwise wrapped context still resolves,
 * so the unlock is never dead because the wrong `Context` type happened to be current at composition.
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The leading pane's own words for "nothing is open yet", shown only on a wide window. */
@Composable
private fun DetailPlaceholder() {
    Text(
        text = stringResource(R.string.nav_detail_placeholder),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
