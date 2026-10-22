package com.gexetr.coverkit.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun initialStateIsReadyForTheStarterAction() {
        val viewModel = HomeViewModel()

        assertEquals(HomeUiState(), viewModel.uiState.value)
    }

    @Test
    fun primaryActionMarksTheStarterCompleteAndRequestsConfirmation() {
        val viewModel = HomeViewModel()

        viewModel.onEvent(HomeEvent.PrimaryActionClicked)

        assertEquals(
            HomeUiState(
                isStarterComplete = true,
                snackbarMessage = HomeSnackbarMessage.ActionCompleted,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun dismissingTheSnackbarKeepsCompletedWorkAndClearsOnlyTheMessage() {
        val viewModel = HomeViewModel()
        viewModel.onEvent(HomeEvent.PrimaryActionClicked)

        viewModel.onEvent(HomeEvent.SnackbarDismissed)

        assertEquals(
            HomeUiState(isStarterComplete = true),
            viewModel.uiState.value,
        )
    }

    @Test
    fun repeatedPrimaryActionDoesNotLoseCompletionState() {
        val viewModel = HomeViewModel()
        viewModel.onEvent(HomeEvent.PrimaryActionClicked)

        viewModel.onEvent(HomeEvent.PrimaryActionClicked)

        assertEquals(
            HomeUiState(
                isStarterComplete = true,
                snackbarMessage = HomeSnackbarMessage.AlreadyCompleted,
            ),
            viewModel.uiState.value,
        )
    }
}
