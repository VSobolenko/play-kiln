package com.gexetr.plinthproductphotos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.data.session.SessionConnectionState
import com.gexetr.plinthproductphotos.data.session.SessionFailure

@Composable
internal fun SessionConnectionScreen(state: SessionConnectionState, onRetry: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val policyUrl = stringResource(R.string.privacy_policy_url)
    var linkFailed by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.session_title), style = MaterialTheme.typography.headlineSmall)
            if (state is SessionConnectionState.Connecting) {
                CircularProgressIndicator()
                Text(stringResource(R.string.session_connecting))
            } else if (state is SessionConnectionState.Failed) {
                val message = when (state.reason) {
                    SessionFailure.CONFIGURATION -> R.string.session_configuration
                    SessionFailure.INTEGRITY -> R.string.session_integrity
                    SessionFailure.NETWORK -> R.string.session_network
                    SessionFailure.SERVER -> R.string.session_server
                    SessionFailure.STORAGE -> R.string.session_storage
                }
                Text(stringResource(message))
                Button(onClick = onRetry) { Text(stringResource(R.string.session_retry)) }
            }
            TextButton(
                onClick = {
                    try {
                        uriHandler.openUri(policyUrl)
                        linkFailed = false
                    } catch (_: IllegalArgumentException) {
                        linkFailed = true
                    }
                },
            ) { Text(stringResource(R.string.settings_product_privacy_title)) }
            if (linkFailed) Text(stringResource(R.string.session_link_failed))
        }
    }
}
