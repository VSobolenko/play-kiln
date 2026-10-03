package com.gexetr.lawnroundlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.PlayBillingController
import com.gexetr.lawnroundlog.data.AppGraph
import com.gexetr.lawnroundlog.ui.PlayKilnApp
import com.gexetr.lawnroundlog.ui.round.RoundViewModel
import com.gexetr.lawnroundlog.ui.settings.SettingsViewModel
import com.gexetr.lawnroundlog.ui.theme.LawnRoundLogTheme
import com.gexetr.lawnroundlog.ui.unlock.BillingPurchaseGate
import com.gexetr.lawnroundlog.ui.unlock.PurchaseGate

/**
 * The single activity, and the composition root of the whole app.
 *
 * Three things live here and nowhere else, because each needs something only an `Activity` can give
 * or has to be resolved before any screen composes:
 *
 * - `AppGraph.init` builds the two data-layer stores, which are the only writers of the record and of
 *   the preferences;
 * - the appearance preference is turned into `darkTheme = …` *outside* the navigation host, because a
 *   theme cannot read a destination's state and a settings row that did not reach the theme would be
 *   a switch that changes nothing;
 * - the billing gate is created from an `Activity`-backed controller, because Google Play's purchase
 *   flow is launched from one, and it is shut down in `onDestroy` so a purchase cannot outlive the
 *   screen that started it.
 *
 * The activity extends [AppCompatActivity] rather than a bare `ComponentActivity` for exactly one
 * reason: the in-app language picker calls `AppCompatDelegate.setApplicationLocales`, and the
 * published caution is that the call only takes effect when its host is an `AppCompatActivity`.
 * `AppCompatActivity` is a `ComponentActivity`, so `enableEdgeToEdge()` and `setContent` are
 * unaffected.
 *
 * There is no `Application` subclass on purpose: the two stores are built from a `Context` that
 * `AppGraph` owns, and an application class would be a second place for the same wiring to drift.
 */
class MainActivity : AppCompatActivity() {

    private var billingController: BillingController? = null

    private var purchaseGate: PurchaseGate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Drawn to the edges at targetSdk 36: the insets are consumed by the screens that own them,
        // and `android:windowSoftInputMode="adjustResize"` in the manifest is the other half of the
        // keyboard placement.
        enableEdgeToEdge()
        AppGraph.init(applicationContext)

        val roundStore = AppGraph.round
        val settingsStore = AppGraph.settings

        val controller = PlayBillingController(
            context = applicationContext,
            activityProvider = { this },
        )
        billingController = controller

        // Built here rather than inside the composition so the one owner of the Play connection is a
        // field this activity also shuts down — a gate created inside `setContent` would be a value
        // `onDestroy` could not reach without a `remember` that composition may never have run.
        val gate: PurchaseGate = BillingPurchaseGate(
            controller = controller,
            // A debug build can be launched into the owned state, so the paid surface — including the
            // CSV handoffs — can be driven on a device before the app is on Play. A release build
            // never reaches this branch: `BuildConfig.DEBUG` is a compile-time false and R8 removes it.
            debugOwned = debugUnlockRequested(intent),
        )
        purchaseGate = gate

        setContent {
            // Activity-scoped, so the theme host below and every screen above share one instance of
            // each — one stored record, one set of preferences, one source of truth per value.
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.factory(settingsStore))
            val roundViewModel: RoundViewModel =
                viewModel(factory = RoundViewModel.factory(roundStore))

            // Connecting is idempotent, so this settles what the operator already owns on cold start
            // and does not start a second billing client when a screen asks again.
            LaunchedEffect(gate) { gate.connect() }

            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            LawnRoundLogTheme(darkTheme = settings.appearance.isDark(systemInDarkTheme)) {
                PlayKilnApp(
                    round = roundViewModel,
                    settings = settingsViewModel,
                    purchaseGate = gate,
                )
            }
        }
    }

    override fun onDestroy() {
        // The gate first: it cancels the scope a purchase may still be running on, and only then is
        // the Play connection ended under it.
        purchaseGate?.shutdown()
        purchaseGate = null
        billingController?.shutdown()
        billingController = null
        super.onDestroy()
    }
}

/**
 * The launch extra a debug build honours to report the unlock as owned.
 *
 * It is a test hook, not a product feature: the paid surface is otherwise unreachable until the app
 * is on Play, so the CSV handoffs could not be exercised on the device at all. Launch with
 * `adb shell am start -n <package>/com.gexetr.lawnroundlog.MainActivity --ez com.gexetr.lawnroundlog.DEBUG_UNLOCK true`
 * to drive it, and launch normally to see the product as a buyer does.
 */
private const val EXTRA_DEBUG_UNLOCK = "com.gexetr.lawnroundlog.DEBUG_UNLOCK"

/**
 * Whether this launch asked for the owned state, in a build where that is allowed.
 *
 * `BuildConfig.DEBUG` is the whole guard, and it is deliberate that the flag is read inside it rather
 * than beside it: in a release build the condition is constant-folded and the extra is never read, so
 * a shipped APK cannot be unlocked by launching it with an argument.
 */
private fun debugUnlockRequested(intent: Intent): Boolean =
    BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEBUG_UNLOCK, false)
