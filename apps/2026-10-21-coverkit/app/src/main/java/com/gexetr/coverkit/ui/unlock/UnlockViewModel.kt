package com.gexetr.coverkit.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.core.PlayKilnResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-time non-consumable unlock SKU. */
val UNLOCK_PRODUCT: ProductId = ProductId("coverkit_unlock")

enum class UnlockStatus {
    IDLE,
    CONNECTING,
    BUSY,
    FAILED,
    UNAVAILABLE,
}

interface PurchaseGate {
    val owned: StateFlow<Boolean>
    val status: StateFlow<UnlockStatus>
    suspend fun connect()
    fun purchase()
    fun restore()
    fun clearStatus()
    fun shutdown()
}

class BillingPurchaseGate(
    private val controller: BillingController,
    private val product: ProductId = UNLOCK_PRODUCT,
    private val catalog: BillingCatalog = BillingCatalog(inAppProducts = listOf(product)),
    debugOwned: Boolean = false,
) : PurchaseGate {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val debugOwnedFlow = MutableStateFlow(debugOwned)

    override val owned: StateFlow<Boolean> = if (debugOwned) {
        debugOwnedFlow.asStateFlow()
    } else {
        controller.entitlements
            .map { entitlements -> entitlements.owns(product.key()) }
            .stateIn(scope, SharingStarted.Eagerly, controller.entitlements.value.owns(product.key()))
    }

    private val mutableStatus = MutableStateFlow(UnlockStatus.IDLE)
    override val status: StateFlow<UnlockStatus> = mutableStatus.asStateFlow()

    override suspend fun connect() {
        mutableStatus.value = UnlockStatus.CONNECTING
        mutableStatus.value = when (val result = controller.connect(catalog)) {
            is PlayKilnResult.Success -> UnlockStatus.IDLE
            is PlayKilnResult.Failure -> result.reason.toStatus()
        }
    }

    override fun purchase() {
        if (mutableStatus.value == UnlockStatus.BUSY) return
        mutableStatus.value = UnlockStatus.BUSY
        scope.launch {
            mutableStatus.value = when (val result = controller.purchase(product)) {
                is PlayKilnResult.Success -> UnlockStatus.IDLE
                is PlayKilnResult.Failure -> result.reason.toStatus()
            }
        }
    }

    override fun restore() {
        if (mutableStatus.value == UnlockStatus.BUSY) return
        mutableStatus.value = UnlockStatus.BUSY
        scope.launch {
            mutableStatus.value = when (val result = controller.restore()) {
                is PlayKilnResult.Success -> UnlockStatus.IDLE
                is PlayKilnResult.Failure -> result.reason.toStatus()
            }
        }
    }

    override fun clearStatus() {
        if (mutableStatus.value == UnlockStatus.BUSY || mutableStatus.value == UnlockStatus.CONNECTING) {
            return
        }
        mutableStatus.value = UnlockStatus.IDLE
    }

    override fun shutdown() {
        scope.cancel()
    }
}

private fun PlayKilnResult.Reason.toStatus(): UnlockStatus = when (this) {
    PlayKilnResult.Reason.NOT_AVAILABLE, PlayKilnResult.Reason.NETWORK -> UnlockStatus.UNAVAILABLE
    PlayKilnResult.Reason.NOT_CONFIGURED,
    PlayKilnResult.Reason.NO_CONSENT,
    PlayKilnResult.Reason.POLICY_BLOCKED,
    -> UnlockStatus.FAILED
    PlayKilnResult.Reason.UNKNOWN -> UnlockStatus.IDLE
}

class UnlockViewModel(private val gate: PurchaseGate) : ViewModel() {
    val owned: StateFlow<Boolean> = gate.owned
    val status: StateFlow<UnlockStatus> = gate.status

    suspend fun connect() {
        gate.connect()
    }

    fun purchase() {
        gate.purchase()
    }

    fun restore() {
        gate.restore()
    }

    fun dismissMessage() {
        gate.clearStatus()
    }

    companion object {
        fun factory(gate: PurchaseGate): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    UnlockViewModel(gate) as T
            }
    }
}
