package com.gexetr.plinthproductphotos.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileRoute(
    onOpenWallet: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSavedResult: (photoId: String, outputId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val factory = remember(context) { ProfileViewModel.factory(context) }
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)
    val state by profileViewModel.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, profileViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) profileViewModel.refresh()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    ProfileScreen(
        state = state,
        onOpenWallet = onOpenWallet,
        onOpenLibrary = onOpenLibrary,
        onOpenSavedResult = onOpenSavedResult,
        onRefresh = profileViewModel::refresh,
        modifier = modifier,
    )
}
