package com.gexetr.laneset.ui.unlock

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.laneset.data.AppGraph
import com.gexetr.laneset.data.BillingManager
import com.gexetr.laneset.data.ConnectOutcome
import com.gexetr.laneset.data.PurchaseOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The unlock screen's state holder.
 *
 * It mirrors nothing: [UnlockUiState.isUnlocked] and [UnlockUiState.availability] are the
 * [BillingManager]'s own `StateFlow`s, combined with the local attempt state, so there is exactly one
 * source of truth for the entitlement and the screen cannot drift from Play. The local part is the
 * transient attempt — the sheet being open, a restore running, the last note — which is UI state and
 * has no business in the data layer.
 *
 * It does **not** connect on construction. The foreground lifecycle is owned by the host
 * (`MainActivity`), which calls `connect()` when the app becomes visible and `disconnect()` when it
 * leaves; a second `connect()` from here would race that one. When the connection has not settled the
 * screen says so and offers the restore action, which is the same reconnect path.
 *
 * `WhileSubscribed(5_000)` keeps the combined flow alive across a configuration change without
 * keeping it alive while the destination is off the back stack.
 */
class UnlockViewModel(private val billing: BillingManager) : ViewModel() {

    /** The transient part of the screen: the attempt in flight and the last note. */
    private val local = MutableStateFlow(UnlockUiState())

    val state: StateFlow<UnlockUiState> = combine(
        billing.isUnlocked,
        billing.availability,
        local,
    ) { isUnlocked, availability, current ->
        // The manager's two facts always win; the local fields only describe the attempt.
        current.copy(isUnlocked = isUnlocked, availability = availability)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = UnlockUiState(),
    )

    /**
     * Opens Play's purchase sheet for the one-time unlock.
     *
     * Every [PurchaseOutcome] is handled and none is folded into another: a purchase that completes,
     * a sheet the swimmer closed and a purchase that failed are three different things to say, and
     * `Purchased` also flips the entitlement, so the screen moves to its owned state on its own.
     *
     * [activity] is required by the Play sheet and is never stored — the launch happens synchronously
     * inside the call, and the coroutine holds it only for that moment.
     */
    fun buy(activity: Activity) {
        if (local.value.isPurchasing) return
        viewModelScope.launch {
            local.update { it.copy(isPurchasing = true, notice = null) }
            val notice = when (billing.launchPurchase(activity)) {
                PurchaseOutcome.Purchased -> null
                PurchaseOutcome.Cancelled -> UnlockNotice.CANCELLED
                is PurchaseOutcome.Failed -> UnlockNotice.FAILED
                is PurchaseOutcome.Unavailable -> UnlockNotice.UNAVAILABLE
            }
            local.update { it.copy(isPurchasing = false, notice = notice) }
        }
    }

    /**
     * Reads the account's purchases back from Play.
     *
     * This is the restore path and the reconnect path at once, and it invents nothing: `connect()`
     * already calls `restoreOwnedPurchases()`, so a reinstall or a new device recovers the unlock by
     * the same code the first launch uses. The note distinguishes "Play was reached and owns nothing
     * here" from "Play could not be reached", because those are different sentences to a swimmer.
     */
    fun restore() {
        if (local.value.isRestoring) return
        viewModelScope.launch {
            local.update { it.copy(isRestoring = true, notice = null) }
            val notice = when (billing.connect()) {
                ConnectOutcome.Connected ->
                    if (billing.isUnlocked.value) null else UnlockNotice.NOT_RESTORED
                is ConnectOutcome.Failed -> UnlockNotice.UNAVAILABLE
            }
            local.update { it.copy(isRestoring = false, notice = notice) }
        }
    }

    companion object {
        /**
         * Builds the ViewModel over the app's **one** billing manager.
         *
         * The manager is read from [AppGraph], never constructed here: a second `PlayBillingManager`
         * would be a second `BillingClient` with a second connection, and the host's foreground
         * lifecycle would be driving only one of them. `AppGraph.init` is idempotent, so calling it
         * here as well removes the ordering hazard if the factory is ever reached before the host's
         * `onCreate` has run — without rebuilding an already-built graph.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    AppGraph.init(applicationContext)
                    return UnlockViewModel(AppGraph.billing) as T
                }
            }
        }
    }
}
