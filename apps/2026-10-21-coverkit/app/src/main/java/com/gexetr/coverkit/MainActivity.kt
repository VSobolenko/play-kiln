package com.gexetr.coverkit

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
import com.gexetr.capability.billing.FakeBillingController
import com.gexetr.capability.billing.PlayBillingController
import com.gexetr.coverkit.data.DataStoreBrandKitStore
import com.gexetr.coverkit.data.FileProjectStore
import com.gexetr.coverkit.ui.PlayKilnApp
import com.gexetr.coverkit.ui.settings.SettingsViewModel
import com.gexetr.coverkit.ui.theme.PlayKilnTheme
import com.gexetr.coverkit.ui.unlock.BillingPurchaseGate
import com.gexetr.coverkit.ui.unlock.PurchaseGate

/**
 * Composition root: settings theme, project/brand stores, and billing unlock gate.
 */
class MainActivity : AppCompatActivity() {

    private var billingController: BillingController? = null
    private var purchaseGate: PurchaseGate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val projectStore = FileProjectStore(applicationContext)
        val brandKitStore = DataStoreBrandKitStore(applicationContext)

        val controller: BillingController = if (BuildConfig.DEBUG) {
            FakeBillingController()
        } else {
            PlayBillingController(
                context = applicationContext,
                activityProvider = { this },
            )
        }
        billingController = controller

        val gate: PurchaseGate = BillingPurchaseGate(
            controller = controller,
            debugOwned = debugUnlockRequested(intent),
        )
        purchaseGate = gate

        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.factory(applicationContext))
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(gate) { gate.connect() }

            PlayKilnTheme(
                darkTheme = settings.themeMode.isDark(systemInDarkTheme),
                dynamicColor = settings.dynamicColor,
            ) {
                PlayKilnApp(
                    projectStore = projectStore,
                    brandKitStore = brandKitStore,
                    purchaseGate = gate,
                    hapticsEnabled = settings.hapticsEnabled,
                )
            }
        }
    }

    override fun onDestroy() {
        purchaseGate?.shutdown()
        purchaseGate = null
        billingController?.shutdown()
        billingController = null
        super.onDestroy()
    }
}

private const val EXTRA_DEBUG_UNLOCK = "com.gexetr.coverkit.DEBUG_UNLOCK"

private fun debugUnlockRequested(intent: Intent): Boolean =
    BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEBUG_UNLOCK, false)
