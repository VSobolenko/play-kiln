package com.gexetr.teachergradebook.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.ui.components.Mark
import com.gexetr.teachergradebook.ui.components.Panel
import com.gexetr.teachergradebook.ui.components.SectionMark
import com.gexetr.teachergradebook.ui.components.Tone

/** The one place the app asks for the permanent, non-consumable unlock. */
@Composable
fun UnlockDialog(
    reason: String?,
    purchasing: Boolean,
    billingReady: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    localizedPrice: String? = null,
) {
    AlertDialog(
        onDismissRequest = { if (!purchasing) onDismiss() },
        title = { Text(stringResource(R.string.unlock_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                reason?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.unlock_benefit_heading),
                    style = MaterialTheme.typography.titleSmall,
                )
                UnlockBenefit(SectionMark.Roster, R.string.unlock_benefit_classes)
                UnlockBenefit(SectionMark.Roster, R.string.unlock_benefit_students)
                UnlockBenefit(SectionMark.Grades, R.string.unlock_benefit_categories)
                UnlockBenefit(SectionMark.Reports, R.string.unlock_benefit_csv)
                UnlockBenefit(SectionMark.Reports, R.string.unlock_benefit_pdf)
                UnlockBenefit(SectionMark.Setup, R.string.unlock_benefit_backup)

                Panel(tone = Tone.Brand) {
                    Text(
                        text = localizedPrice ?: stringResource(R.string.unlock_price_loading),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.unlock_one_time_purchase),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = stringResource(R.string.unlock_google_play_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!billingReady) {
                    Panel(tone = Tone.Danger) {
                        Text(
                            text = stringResource(R.string.unlock_billing_unavailable_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(R.string.unlock_billing_unavailable_body),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPurchase,
                enabled = !purchasing && billingReady,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                if (purchasing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.unlock_purchase_button))
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onRestore,
                    enabled = !purchasing && billingReady,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.unlock_restore_button)) }
                TextButton(
                    onClick = onDismiss,
                    enabled = !purchasing,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.unlock_dismiss_button)) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UnlockBenefit(mark: SectionMark, textRes: Int) {
    Panel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Mark(mark = mark, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
            Text(text = stringResource(textRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
