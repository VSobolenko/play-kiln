package com.gexetr.smoke.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeEvent) {
        _uiState.update { state ->
            when (event) {
                HomeEvent.PrimaryActionClicked -> state.copy(
                    isStarterComplete = true,
                    snackbarMessage = if (state.isStarterComplete) {
                        HomeSnackbarMessage.AlreadyCompleted
                    } else {
                        HomeSnackbarMessage.ActionCompleted
                    },
                )
                HomeEvent.SnackbarDismissed -> state.copy(snackbarMessage = null)
            }
        }
    }
}
