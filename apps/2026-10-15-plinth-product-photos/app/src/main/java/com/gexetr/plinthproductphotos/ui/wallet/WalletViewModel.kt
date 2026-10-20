package com.gexetr.plinthproductphotos.ui.wallet

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.capability.billing.PlayBillingController
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.core.PlayKilnResult
import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.JobStatus
import com.gexetr.plinthproductphotos.data.api.WalletResponse
import com.gexetr.plinthproductphotos.data.session.PendingCutoutStore
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore
import com.gexetr.plinthproductphotos.data.wallet.EncryptedBillingStateStore
import com.gexetr.plinthproductphotos.data.wallet.HttpWalletPurchaseApi
import com.gexetr.plinthproductphotos.data.wallet.WalletProductIds
import com.gexetr.plinthproductphotos.data.wallet.WalletPurchaseAuthority
import com.gexetr.plinthproductphotos.data.wallet.WalletPurchaseCoordinator
import com.gexetr.plinthproductphotos.data.wallet.WalletPurchaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class WalletViewModel private constructor(
    private val sessionStore: WalletSessionStore,
    private val api: ApiClient,
    private val pendingCutoutStore: PendingCutoutStore,
    private val billing: PlayBillingController,
    private val purchases: WalletPurchaseCoordinator,
    private val productIds: WalletProductIds,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletScreenStateMapper.offline())
    val state: StateFlow<WalletScreenState> = _state.asStateFlow()
    val chipBalance = sessionStore.balance.catch { failure ->
        if (failure !is IOException) throw failure
        Log.e("WalletViewModel", "Could not observe the saved wallet balance", failure)
        emit(null)
    }
    private var billingReady = false

    init {
        connectBilling()
        refreshWallet()
    }

    override fun onCleared() {
        billing.shutdown()
        super.onCleared()
    }

    fun refreshWallet() {
        viewModelScope.launch {
            val session = sessionStore.current()
            if (session == null) {
                _state.value = WalletScreenStateMapper.offline().copy(
                    localizedPrices = _state.value.localizedPrices,
                )
                return@launch
            }

            val response = withContext(Dispatchers.IO) {
                api.getWallet(session.deviceToken)
            }
            when (response) {
                is ApiResult.Success -> applyServerWallet(response.value, session.deviceToken)
                is ApiResult.Failure -> {
                    val previous = _state.value
                    _state.value = WalletScreenStateMapper.offline().copy(
                        localizedPrices = previous.localizedPrices,
                        purchaseStatus = previous.purchaseStatus,
                        restoreStatus = previous.restoreStatus,
                        recoveryStatus = previous.recoveryStatus,
                    )
                }
            }
        }
    }

    fun purchase(product: ProductId) {
        if (!_state.value.canPurchase(product) || !productIds.contains(product)) return
        _state.update { it.copy(purchaseStatus = WalletPurchaseUiStatus.CONFIRMING) }
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { purchases.purchase(product) }) {
                WalletPurchaseResult.Confirming -> {
                    _state.update { it.copy(purchaseStatus = WalletPurchaseUiStatus.CONFIRMING) }
                }
                WalletPurchaseResult.Pending -> {
                    _state.update { it.copy(purchaseStatus = WalletPurchaseUiStatus.PENDING) }
                }
                WalletPurchaseResult.Purchased -> {
                    _state.update {
                        it.copy(
                            purchaseStatus = WalletPurchaseUiStatus.CONFIRMED,
                        )
                    }
                    refreshWallet()
                }
                is WalletPurchaseResult.Failed -> {
                    _state.update {
                        it.copy(
                            purchaseStatus = when (result.reason) {
                                ApiFailure.PurchaseInvalid -> WalletPurchaseUiStatus.REJECTED_NOT_CHARGED
                                ApiFailure.PurchasePending -> WalletPurchaseUiStatus.PENDING
                                else -> WalletPurchaseUiStatus.UNCONFIRMED
                            },
                        )
                    }
                }
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            when (withContext(Dispatchers.IO) { billing.restore() }) {
                is PlayKilnResult.Success -> {
                    _state.update { it.copy(restoreStatus = WalletRestoreUiStatus.SUCCEEDED) }
                }
                is PlayKilnResult.Failure -> {
                    _state.update { it.copy(restoreStatus = WalletRestoreUiStatus.FAILED) }
                }
            }
            refreshWallet()
        }
    }

    fun recoverWallet(code: String) {
        val submittedCode = code.trim().uppercase()
        if (submittedCode.isBlank()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { purchases.recoverWallet(submittedCode) }
            when (result) {
                is ApiResult.Success -> {
                    _state.update { it.copy(recoveryStatus = WalletRecoveryUiStatus.SUCCEEDED) }
                    refreshWallet()
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(recoveryStatus = WalletRecoveryUiStatus.FAILED) }
                }
            }
        }
    }

    private fun connectBilling() {
        viewModelScope.launch {
            when (withContext(Dispatchers.IO) { purchases.connect() }) {
                is PlayKilnResult.Success -> {
                    billingReady = true
                    val prices = withContext(Dispatchers.IO) { purchases.localizedPrices() }
                    _state.update {
                        it.copy(
                            localizedPrices = prices,
                            purchaseAvailable = !it.offline,
                        )
                    }
                }
                is PlayKilnResult.Failure -> {
                    billingReady = false
                    _state.update { it.copy(purchaseAvailable = false) }
                }
            }
        }
    }

    private suspend fun recentJobStatuses(deviceToken: String): Pair<List<WalletJobStatus>, Boolean> {
        val jobId = pendingCutoutStore.current()?.jobId ?: return emptyList<WalletJobStatus>() to false
        return when (val response = withContext(Dispatchers.IO) { api.pollJob(jobId, deviceToken) }) {
            is ApiResult.Success -> listOf(WalletScreenStateMapper.mapJob(jobId, response.value)) to false
            is ApiResult.Failure -> emptyList<WalletJobStatus>() to true
        }
    }

    private suspend fun applyServerWallet(response: WalletResponse, deviceToken: String) {
        sessionStore.updateServerBalance(response.balance)
        val (jobs, jobsUnavailable) = recentJobStatuses(deviceToken)
        val previous = _state.value
        _state.value = WalletScreenStateMapper.fromServer(
            response,
            jobs = jobs,
        ).copy(
            localizedPrices = previous.localizedPrices,
            purchaseStatus = previous.purchaseStatus,
            purchaseAvailable = billingReady,
            restoreStatus = previous.restoreStatus,
            recoveryStatus = previous.recoveryStatus,
            jobStatusUnavailable = jobsUnavailable,
        )
    }

    companion object {
        fun factory(
            context: Context,
            activityProvider: () -> Activity?,
        ): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(WalletViewModel::class.java))
                    val sessionStore = WalletSessionStore(appContext)
                    val api = ApiClient()
                    val purchaseApi = HttpWalletPurchaseApi()
                    val billing = PlayBillingController(
                        context = appContext,
                        activityProvider = activityProvider,
                        authority = WalletPurchaseAuthority(
                            context = appContext,
                            sessionStore = sessionStore,
                            api = purchaseApi,
                        ),
                        store = EncryptedBillingStateStore(appContext),
                    )
                    return WalletViewModel(
                        sessionStore = sessionStore,
                        api = api,
                        pendingCutoutStore = PendingCutoutStore(appContext),
                        billing = billing,
                        purchases = WalletPurchaseCoordinator(
                            controller = billing,
                            sessionStore = sessionStore,
                            products = WalletScreenStateMapper.products,
                            purchaseApi = purchaseApi,
                            apiClient = api,
                        ),
                        productIds = WalletScreenStateMapper.products,
                    ) as T
                }
            }
        }
    }
}

/** The unlock is whatever the server says; an unreachable server is never read as unlocked. */
internal object ServerUnlock {
    suspend fun fetch(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = WalletSessionStore(context.applicationContext).current() ?: return@withContext false
            when (val response = ApiClient().getWallet(session.deviceToken)) {
                is ApiResult.Success -> response.value.unlocked
                is ApiResult.Failure -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}

internal object WalletScreenStateMapper {
    val products = WalletProductIds(
        starter = ProductId("plinth_starter_90"),
        studio = ProductId("plinth_studio_230"),
        pro = ProductId("plinth_pro_600"),
        unlock = ProductId("plinth_unlock"),
    )

    fun fromServer(
        response: WalletResponse,
        jobs: List<WalletJobStatus> = emptyList(),
    ): WalletScreenState = WalletScreenState(
        balance = response.balance,
        productIds = products,
        reserved = response.reserved,
        refundDebt = response.refundDebt,
        serverConfirmedUnlock = response.unlocked,
        purchaseAvailable = true,
        history = response.entries.map { entry ->
            WalletHistoryRow(label = entry.kind, delta = entry.delta)
        },
        jobs = jobs,
    )

    fun offline(): WalletScreenState = WalletScreenState(
        balance = 0,
        productIds = products,
        offline = true,
        purchaseAvailable = false,
    )

    fun mapJob(jobId: String, status: JobStatus): WalletJobStatus =
        WalletJobStatus(jobId = jobId, status = status)
}
