package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.api.ServiceState
import kotlinx.coroutines.delay

/**
 * The non-blocking panel for a paused service or a reached daily cap.
 *
 * It never hides the wallet or the results: `GET /v1/wallet`, job status and result collection stay
 * open server-side in every state, so the only thing this panel removes is the ability to start new
 * paid work. The retry is held for [RETRY_BACKOFF_SECONDS] because `/config` is cached for 60 seconds
 * and a faster loop can only produce a stale answer.
 */
@Composable
fun ServiceStateBanner(
    state: ServiceState,
    dismissible: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paused = state == ServiceState.MAINTENANCE
    var secondsLeft by remember(state) { mutableIntStateOf(RETRY_BACKOFF_SECONDS) }
    LaunchedEffect(state) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
    }
    PlinthStage(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(if (paused) R.string.service_maintenance else R.string.service_daily_cap),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    if (paused) R.string.service_maintenance_detail else R.string.service_daily_cap_detail,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRetry,
                enabled = secondsLeft == 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (secondsLeft == 0) stringResource(R.string.service_retry)
                    else stringResource(R.string.service_retry_countdown, secondsLeft),
                )
            }
            if (dismissible) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.service_dismiss))
                }
            }
        }
    }
}

/**
 * A closed service is not an error to retry: it does not come back by itself, and nothing it refuses
 * will succeed on a second attempt. The panel is read-only on purpose and keeps the paid results and
 * the wallet reachable.
 */
@Composable
fun ServiceClosedPanel(
    onOpenResults: () -> Unit,
    onOpenWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlinthStage(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.service_closed),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.service_closed_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenResults, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.service_open_results))
            }
            TextButton(onClick = onOpenWallet, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.service_open_wallet))
            }
        }
    }
}

/**
 * The one blocking state. Reaching it means the token was re-attested with the installed version and
 * the server still refused, so the installed build really is below the minimum and only an update can
 * unblock new work. It has no dismiss action for the same reason.
 */
@Composable
fun UpdateRequiredDialog(
    minAppVersion: String,
    onOpenPlay: () -> Unit,
) {
    Dialog(onDismissRequest = {}) {
        PlinthStage(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.service_update_required),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.service_update_detail, minAppVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenPlay, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.service_open_play))
                }
            }
        }
    }
}

private const val RETRY_BACKOFF_SECONDS = 30
