package com.gexetr.unitwalk.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one-time unlock's brain.
 *
 * It reads the app's **one shared** [BillingController] and nothing else: it connects once with the
 * slot's single-product catalog, mirrors the controller's own `availability` and `entitlements` flows
 * into one [UnlockUiState], reads Play's own localized price for the product, and offers `purchase()`
 * and `restore()`. Every user-visible word lives on the screen, so this class can be unit-tested
 * without a device, a network or Play services.
 *
 * ### The controller is the shell's, not this class's
 *
 * The controller is created in `ui/UnitWalkApp.kt` and handed in, so the entitlement a purchase here
 * grants is the *same* state the portfolio's unit cap and the report's watermark read. This class
 * builds no controller of its own: a second instance would carry its own store and its own Play
 * connection, and a purchase made through it would be invisible to the rest of the app until a
 * restart. For the same reason [onCleared] does **not** shut the controller down — its lifetime
 * belongs to the shell that created it, and shutting it down here would kill billing for the whole app
 * the moment this sheet went away. Leaving the sheet only cancels this class's own scope.
 *
 * ### Why the dispatcher is a parameter
 *
 * The factory's `SettingsViewModel` forwards to `viewModelScope`, which is bound to
 * `Dispatchers.Main` — unavailable in a plain JVM test without `kotlinx-coroutines-test`, which is
 * not in this slot's catalog. Taking the dispatcher as a constructor default keeps production on
 * the main thread (Play Billing's flow must be launched there) while letting a JVM test pass
 * `Dispatchers.Unconfined`, so the three outcomes that matter — unowned, purchased, failed — are
 * provable without a Play connection. The scope is ours, so [onCleared] cancels it explicitly.
 */
class UnlockViewModel(
    private val billing: BillingController,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        // The controller's state is the source of truth; this ViewModel holds no entitlement copy.
        scope.launch {
            billing.availability.collect { availability ->
                _uiState.update { it.copy(availability = availability) }
            }
        }
        scope.launch {
            billing.entitlements.collect { entitlements ->
                _uiState.update { it.copy(isOwned = entitlements.ownsUnitWalkUnlock()) }
            }
        }
        scope.launch { connect() }
    }

    fun onEvent(event: UnlockEvent) {
        when (event) {
            UnlockEvent.PurchaseRequested -> purchase()
            UnlockEvent.RestoreRequested -> restore()
            UnlockEvent.MessageDismissed -> _uiState.update {
                it.copy(message = null, lastFailure = null)
            }
        }
    }

    private suspend fun connect() {
        when (val result = billing.connect(UNIT_WALK_CATALOG)) {
            is PlayKilnResult.Success -> refreshPrice()
            is PlayKilnResult.Failure -> _uiState.update {
                it.copy(message = UnlockMessage.Unavailable, lastFailure = result.reason)
            }
        }
    }

    /**
     * Reads Play's own formatted price once, after the connection is up.
     *
     * The amount Play will charge is localized to the user's Play country and currency, so the literal
     * in `strings_unlock.xml` is only a fallback: a `ru` account is charged rubles, not the dollars the
     * resource states. The read is best-effort — a `null` answer (no connection, product not
     * configured, query timeout) leaves [UnlockUiState.priceLabel] null and the screen keeps its own
     * string. Nothing here fails the sheet: a price is display, not entitlement.
     */
    private suspend fun refreshPrice() {
        val label = billing.formattedPrice(UNIT_WALK_UNLOCK)
        if (label != null) {
            _uiState.update { it.copy(priceLabel = label) }
        }
    }

    private fun purchase() {
        if (_uiState.value.isOwned) {
            _uiState.update { it.copy(message = UnlockMessage.AlreadyOwned) }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isWorking = true, message = null, lastFailure = null) }
            when (val result = billing.purchase(UNIT_WALK_UNLOCK)) {
                // Ownership is read from the entitlements the purchase actually returned, never
                // assumed from a `Success`: a settled purchase that granted nothing must not
                // unlock the app. The entitlements flow mirrors it too, but stating it here makes
                // the ownership decision follow from the result at the one point that set it.
                is PlayKilnResult.Success -> _uiState.update {
                    it.copy(
                        isWorking = false,
                        isOwned = result.value.ownsUnitWalkUnlock(),
                        message = UnlockMessage.PurchaseSucceeded,
                    )
                }
                is PlayKilnResult.Failure -> _uiState.update {
                    it.copy(
                        isWorking = false,
                        message = UnlockMessage.PurchaseFailed,
                        lastFailure = result.reason,
                    )
                }
            }
        }
    }

    private fun restore() {
        scope.launch {
            _uiState.update { it.copy(isWorking = true, message = null, lastFailure = null) }
            when (val result = billing.restore()) {
                is PlayKilnResult.Success -> _uiState.update {
                    it.copy(
                        isWorking = false,
                        isOwned = result.value.ownsUnitWalkUnlock(),
                        message = UnlockMessage.RestoreSucceeded,
                    )
                }
                is PlayKilnResult.Failure -> _uiState.update {
                    it.copy(
                        isWorking = false,
                        message = UnlockMessage.RestoreFailed,
                        lastFailure = result.reason,
                    )
                }
            }
        }
    }

    /**
     * Cancels this class's own scope **and nothing else**.
     *
     * Deliberately public, unlike the `protected` member it overrides, for one reason: `onCleared` is
     * the exact point at which the old code shut the billing controller down, and a test has to be able
     * to reach it to prove the fix holds. `androidx.lifecycle.ViewModelStore` — the framework's real
     * caller — is annotated `@RestrictTo(LIBRARY_GROUP)`, so driving this through a store would mean
     * depending on a library-group API in the slot's test source, and calling the method the framework
     * calls is the same assertion without that dependency.
     *
     * The controller belongs to the shell that created it and outlives this sheet. Calling
     * `shutdown()` here would end billing for the whole app — the portfolio's unit cap and the
     * report's watermark included — the moment this ViewModel was cleared, which is precisely the
     * regression `leavingTheUnlockSheetCannotShutTheSharedControllerDown` exists to catch. Only this
     * class's own scope is ours.
     */
    public override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    companion object {
        /**
         * Builds the ViewModel over the shell's **one shared** [BillingController].
         *
         * The controller is created by `ui/UnitWalkApp.kt`, together with a real `Activity` provider and
         * the durable store, and passed in. The sheet never builds one of its own: a second controller
         * would carry its own store and its own Play connection, so a purchase made here would grant an
         * entitlement the unit cap and the report watermark could not see until the app restarted.
         *
         * Nothing else needs to be handed in. `authority` stays `null` because the catalog declares no
         * server-cost consumable, which the shell already honours when it builds the controller.
         */
        fun factory(billing: BillingController): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    UnlockViewModel(billing) as T
            }
    }
}
