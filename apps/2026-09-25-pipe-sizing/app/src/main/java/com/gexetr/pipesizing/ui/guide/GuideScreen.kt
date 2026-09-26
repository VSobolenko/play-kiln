package com.gexetr.pipesizing.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gexetr.pipesizing.R

/**
 * The Guide tab: what the tool does, why it refuses, and the two things a user must be able to
 * find — how their ad choices are handled and how a purchase is restored.
 */
@Composable
fun GuideScreen(
    versionName: String,
    isPro: Boolean,
    adsAvailable: Boolean,
    consentResolved: Boolean,
    onReviewAdChoices: () -> Unit,
    onRestorePurchase: () -> Unit,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.guide_intro), style = MaterialTheme.typography.headlineSmall)

        GuideCard(R.string.guide_purpose_title, R.string.guide_purpose_body)
        GuideCard(R.string.guide_correctness_title, R.string.guide_correctness_body)
        GuideCard(R.string.guide_privacy_title, R.string.guide_privacy_body)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.guide_purchase_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.guide_purchase_body), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (isPro) {
                        stringResource(R.string.pro_active_body)
                    } else {
                        stringResource(R.string.paywall_body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isPro) {
                    FilledTonalButton(onClick = onOpenPaywall, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.paywall_title))
                    }
                }
                TextButton(onClick = onRestorePurchase, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.paywall_restore))
                }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.consent_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.consent_body), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = when {
                        adsAvailable -> stringResource(R.string.consent_resolved)
                        consentResolved -> stringResource(R.string.consent_unavailable)
                        else -> stringResource(R.string.consent_body)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onReviewAdChoices, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.consent_action))
                }
            }
        }

        Text(
            text = stringResource(R.string.guide_version_label, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GuideCard(titleRes: Int, bodyRes: Int) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
