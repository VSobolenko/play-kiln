package com.gexetr.coverkit.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.coverkit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    gate: PurchaseGate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: UnlockViewModel = viewModel(factory = UnlockViewModel.factory(gate))
    val owned by viewModel.owned.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    LaunchedEffect(gate) {
        viewModel.connect()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.unlock_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.unlock_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.unlock_bullet_watermark), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.unlock_bullet_sizes), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.unlock_bullet_templates), style = MaterialTheme.typography.bodyMedium)
            }
            PricePanel(
                owned = owned,
                price = stringResource(R.string.unlock_price),
                priceNote = stringResource(R.string.unlock_price_note),
            )
            if (!owned) {
                val busy = status == UnlockStatus.BUSY || status == UnlockStatus.CONNECTING
                Button(
                    onClick = { viewModel.purchase() },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.unlock_action))
                }
                TextButton(
                    onClick = { viewModel.restore() },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.unlock_restore))
                }
            }
            if (status == UnlockStatus.BUSY || status == UnlockStatus.CONNECTING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.unlock_busy), style = MaterialTheme.typography.bodyMedium)
                }
            }
            val message = when (status) {
                UnlockStatus.FAILED -> R.string.unlock_failed
                UnlockStatus.UNAVAILABLE -> R.string.unlock_unavailable
                else -> null
            }
            if (message != null) {
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(
                    onClick = { viewModel.dismissMessage() },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_dismiss))
                }
            }
        }
    }
}

@Composable
private fun PricePanel(owned: Boolean, price: String, priceNote: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (owned) {
                Text(
                    text = stringResource(R.string.unlock_unlocked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.unlock_owned_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = stringResource(R.string.unlock_price_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = price, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = priceNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
