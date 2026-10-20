package com.gexetr.plinthproductphotos.ui.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.db.PlinthDatabase
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore
import com.gexetr.plinthproductphotos.ui.components.projectDisplayTitle
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProfileBalanceStatus {
    LOADING,
    CURRENT,
    CACHED,
    UNAVAILABLE,
    ERROR,
}

enum class ProfileHistoryStatus {
    LOADING,
    READY,
    ERROR,
}

internal data class ProfileBalancePresentation(
    val credits: Int?,
    val status: ProfileBalanceStatus,
)

data class ProfileResult(
    val title: String,
    val imagePath: String,
    val photoId: String,
    val createdAt: Long,
    val outputId: String = "",
)

data class ProfileUiState(
    val credits: Int? = null,
    val balanceStatus: ProfileBalanceStatus = ProfileBalanceStatus.LOADING,
    val history: List<ProfileResult> = emptyList(),
    val historyStatus: ProfileHistoryStatus = ProfileHistoryStatus.LOADING,
    val projectCount: Int = 0,
    val savedResultCount: Int = 0,
)

internal fun profileBalancePresentation(
    serverConfirmedBalance: Int?,
    cachedBalance: Int?,
    refreshFailed: Boolean = false,
): ProfileBalancePresentation = when {
    serverConfirmedBalance != null ->
        ProfileBalancePresentation(serverConfirmedBalance, ProfileBalanceStatus.CURRENT)
    cachedBalance != null ->
        ProfileBalancePresentation(cachedBalance, ProfileBalanceStatus.CACHED)
    refreshFailed -> ProfileBalancePresentation(null, ProfileBalanceStatus.ERROR)
    else -> ProfileBalancePresentation(null, ProfileBalanceStatus.UNAVAILABLE)
}

internal data class ProfileHistoryCandidate(
    val title: String,
    val imagePath: String,
    val createdAt: Long,
    val photoId: String = "",
    val isReference: Boolean = false,
    val outputId: String = "",
)

internal fun recentProfileResults(
    candidates: List<ProfileHistoryCandidate>,
    limit: Int = PROFILE_HISTORY_LIMIT,
): List<ProfileResult> {
    require(limit > 0) { "Profile history limit must be positive" }
    return candidates.asSequence()
        .filter {
            !it.isReference &&
                it.imagePath.isNotBlank() &&
                File(it.imagePath).let { image -> image.isFile && image.canRead() }
        }
        .sortedByDescending(ProfileHistoryCandidate::createdAt)
        .take(limit)
        .map { ProfileResult(projectDisplayTitle(it.title, it.createdAt), it.imagePath, it.photoId, it.createdAt, it.outputId) }
        .toList()
}

class ProfileViewModel internal constructor(
    private val sessionStore: WalletSessionStore,
    private val api: ApiClient,
    private val database: PlinthDatabase,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { refreshBalance() }
        viewModelScope.launch { refreshHistory() }
    }

    private suspend fun refreshBalance() {
        val session = try {
            withContext(Dispatchers.IO) { sessionStore.current() }
        } catch (failure: Exception) {
            Log.e(TAG, "Could not read the saved wallet session", failure)
            val balance = profileBalancePresentation(null, null, refreshFailed = true)
            _state.update { it.copy(credits = balance.credits, balanceStatus = balance.status) }
            return
        }

        if (session == null) {
            val balance = profileBalancePresentation(null, null)
            _state.update { it.copy(credits = balance.credits, balanceStatus = balance.status) }
            return
        }

        val response = try {
            withContext(Dispatchers.IO) { api.getWallet(session.deviceToken) }
        } catch (failure: Exception) {
            Log.e(TAG, "Could not refresh the server wallet balance", failure)
            showCachedBalance(session.balanceCache)
            return
        }
        when (response) {
            is ApiResult.Success -> {
                try {
                    withContext(Dispatchers.IO) { sessionStore.updateServerBalance(response.value.balance) }
                } catch (failure: Exception) {
                    Log.e(TAG, "Could not cache the server-confirmed wallet balance", failure)
                }
                val balance = profileBalancePresentation(
                    serverConfirmedBalance = response.value.balance,
                    cachedBalance = session.balanceCache,
                )
                _state.update {
                    it.copy(credits = balance.credits, balanceStatus = balance.status)
                }
            }
            is ApiResult.Failure -> showCachedBalance(session.balanceCache)
        }
    }

    private fun showCachedBalance(balance: Int?) {
        val presentation = profileBalancePresentation(
            serverConfirmedBalance = null,
            cachedBalance = balance,
            refreshFailed = true,
        )
        _state.update {
            it.copy(
                credits = presentation.credits,
                balanceStatus = presentation.status,
            )
        }
    }

    private suspend fun refreshHistory() {
        _state.update { it.copy(historyStatus = ProfileHistoryStatus.LOADING) }
        try {
            val snapshot = withContext(Dispatchers.IO) { loadLocalResults() }
            _state.update {
                it.copy(
                    history = recentProfileResults(snapshot.candidates),
                    historyStatus = ProfileHistoryStatus.READY,
                    projectCount = snapshot.projectCount,
                    savedResultCount = recentProfileResults(snapshot.candidates, Int.MAX_VALUE).size,
                )
            }
        } catch (failure: Exception) {
            Log.e(TAG, "Could not load saved results for the profile", failure)
            _state.update { it.copy(historyStatus = ProfileHistoryStatus.ERROR) }
        }
    }

    private suspend fun loadLocalResults(): ProfileLibrarySnapshot {
        val source = database.projectDao().pageLibrary(
            search = null,
            category = null,
            marketplaceId = null,
            updatedAfter = null,
            updatedBefore = null,
        )
        val page = when (
            val loaded = source.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = PROFILE_PROJECT_SCAN_LIMIT,
                    placeholdersEnabled = false,
                ),
            )
        ) {
            is PagingSource.LoadResult.Page -> loaded.data
            is PagingSource.LoadResult.Error -> throw loaded.throwable
            is PagingSource.LoadResult.Invalid -> error("Local project history changed while loading")
        }

        val candidates = buildList {
            for (summary in page) {
                for (output in database.outputDao().exportableOutputs(summary.project.id)) {
                    add(
                        ProfileHistoryCandidate(
                            title = summary.project.name,
                            imagePath = output.path,
                            createdAt = output.createdAt,
                            photoId = output.itemId,
                            outputId = output.id,
                        ),
                    )
                }
            }
        }
        return ProfileLibrarySnapshot(candidates = candidates, projectCount = page.size)
    }

    companion object {
        private const val TAG = "ProfileViewModel"

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
                    return ProfileViewModel(
                        sessionStore = WalletSessionStore(appContext),
                        api = ApiClient(),
                        database = PlinthDatabase.get(appContext),
                    ) as T
                }
            }
        }
    }
}

private data class ProfileLibrarySnapshot(
    val candidates: List<ProfileHistoryCandidate>,
    val projectCount: Int,
)

private const val PROFILE_HISTORY_LIMIT = 5
private const val PROFILE_PROJECT_SCAN_LIMIT = 30
