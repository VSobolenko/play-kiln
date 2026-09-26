package com.gexetr.teachergradebook.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The window-level shell: one snackbar host for the whole app, and no insets applied here.
 *
 * Inset handling is left to each screen's own `Scaffold`, because a nested scaffold that applied
 * the status-bar inset twice would push every screen's title down by the height of the status bar
 * and its bottom content up by the height of the navigation bar. The snackbar host opts back in
 * with `navigationBarsPadding`, so a message is never hidden behind the gesture bar.
 */
@Composable
fun PlayKilnSurface(
    snackbarHost: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}
