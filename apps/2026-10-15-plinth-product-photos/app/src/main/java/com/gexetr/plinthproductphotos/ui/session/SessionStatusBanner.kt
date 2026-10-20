package com.gexetr.plinthproductphotos.ui.session

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.BuildConfig
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.session.SessionConnectionState
import com.gexetr.plinthproductphotos.data.session.SessionFailure

@StringRes
internal fun sessionFailureMessage(reason: SessionFailure, debug: Boolean): Int = when (reason) {
    SessionFailure.CONFIGURATION -> R.string.session_configuration
    SessionFailure.INTEGRITY -> R.string.session_integrity
    SessionFailure.NETWORK -> if (debug) R.string.session_network_debug else R.string.session_network
    SessionFailure.SERVER -> R.string.session_server
    SessionFailure.STORAGE -> R.string.session_storage
}

@Composable
internal fun SessionStatusBanner(state: SessionConnectionState, onRetry: () -> Unit) {
    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (state is SessionConnectionState.Ready || dismissed) return

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            when (state) {
                SessionConnectionState.Connecting -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.session_connecting), style = MaterialTheme.typography.bodySmall)
                }
                is SessionConnectionState.Failed -> Text(
                    stringResource(sessionFailureMessage(state.reason, BuildConfig.DEBUG)),
                    style = MaterialTheme.typography.bodySmall,
                )
                is SessionConnectionState.Ready -> Unit
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state is SessionConnectionState.Failed) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.session_retry)) }
                }
                TextButton(onClick = { dismissed = true }) {
                    Text(stringResource(R.string.session_browse_without_wallet))
                }
            }
        }
    }
}
