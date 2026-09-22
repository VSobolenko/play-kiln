package com.gexetr.smoke.ui.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val isStarterComplete: Boolean = false,
    val snackbarMessage: HomeSnackbarMessage? = null,
)

enum class HomeSnackbarMessage {
    ActionCompleted,
    AlreadyCompleted,
}

sealed interface HomeEvent {
    data object PrimaryActionClicked : HomeEvent
    data object SnackbarDismissed : HomeEvent
}
