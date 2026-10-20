package com.gexetr.plinthproductphotos.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun initialStateInvitesTheFirstImport() {
        val viewModel = HomeViewModel()

        assertEquals(HomeUiState(), viewModel.uiState.value)
    }
}
