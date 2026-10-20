package com.gexetr.plinthproductphotos.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.data.project.RecentPhotoStore
import org.json.JSONException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val projects: ProjectRepository? = null,
    private val applicationContext: Context? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(isLoading = projects != null || applicationContext != null),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        if (projects != null || applicationContext != null) {
            viewModelScope.launch(Dispatchers.IO) {
                // Constructing the repository also opens preferences; keep that off the UI thread.
                val repository = projects ?: ProjectRepository(requireNotNull(applicationContext))
                val preferences = applicationContext?.getSharedPreferences(
                    RecentPhotoStore.PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                )
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }
                preferences?.registerOnSharedPreferenceChangeListener(listener)
                refresh()
                try {
                    for (request in refreshRequests) {
                        loadPhotos(repository)
                    }
                } finally {
                    preferences?.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }
        }
    }

    fun refresh() {
        refreshRequests.trySend(Unit)
    }

    private fun loadPhotos(repository: ProjectRepository) {
        _uiState.update { it.copy(loadError = false) }
        try {
            val photos = repository.recentPhotos()
            _uiState.update {
                it.copy(
                    recentPhotos = photos.take(8),
                    photoCount = photos.size,
                    isLoading = false,
                )
            }
        } catch (failure: JSONException) {
            Log.e("PlinthHome", "Could not load recent photo metadata", failure)
            _uiState.update { it.copy(isLoading = false, loadError = true) }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(applicationContext = applicationContext) as T
            }
        }
    }
}
