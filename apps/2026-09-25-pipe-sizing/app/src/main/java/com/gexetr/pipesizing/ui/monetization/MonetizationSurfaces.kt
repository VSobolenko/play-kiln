package com.gexetr.pipesizing.ui.monetization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gexetr.pipesizing.R

/**
 * The one screen where money is asked for.
 *
 * It states the three things the free tier actually limits, because a paywall that sells a vague
 * "Pro" is the pattern this factory's own rules call out: the user must be able to read exactly
 * what changes. The price itself is not printed here — Google Play shows the exact price and
 * currency in the purchase sheet, and a hardcoded number would be wrong in every non-US store.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    isPro: Boolean,
    purchaseInFlight: Boolean,
    freeCaseLimit: Int,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.paywall_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.paywall_body), style = MaterialTheme.typography.bodyMedium)

            Benefit(stringResource(R.string.paywall_benefit_cases, freeCaseLimit))
            Benefit(stringResource(R.string.paywall_benefit_ads))
            Benefit(stringResource(R.string.paywall_benefit_report))

            Text(
                text = stringResource(R.string.paywall_price_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isPro) {
                Text(
                    text = stringResource(R.string.pro_active_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(
                    onClick = onBuy,
                    enabled = !purchaseInFlight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (purchaseInFlight) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        }
                        Text(stringResource(R.string.paywall_buy))
                    }
                }
            }

            TextButton(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.paywall_restore))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.paywall_later))
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
    )
}

/**
 * The free tier's report gate: the user chooses between watching an opt-in rewarded video and
 * buying Pro. Nothing is shown before either choice, so the app never blocks its own function with
 * an ad.
 */
@Composable
fun ReportUnlockDialog(
    onWatchRewarded: () -> Unit,
    onOpenPaywall: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_action)) },
        text = { Text(stringResource(R.string.report_rewarded_hint)) },
        confirmButton = {
            TextButton(onClick = onWatchRewarded, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.rewarded_action))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpenPaywall, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.paywall_title))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.paywall_later))
                }
            }
        },
    )
}
