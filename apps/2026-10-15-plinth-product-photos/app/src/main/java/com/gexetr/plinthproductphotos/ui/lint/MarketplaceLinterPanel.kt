package com.gexetr.plinthproductphotos.ui.lint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.export.ExportPack
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MarketplaceLinterPanel(
    packs: List<ExportPack>,
    onPacksChanged: (List<ExportPack>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<LinterActionReport?>(null) }
    var busy by remember { mutableStateOf(false) }
    var errorResource by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = {
                busy = true
                errorResource = 0
                report = null
                scope.launch {
                    try {
                        val next = withContext(Dispatchers.IO) {
                            MarketplaceLinterAction.checkAndRepair(context, packs)
                        }
                        report = next
                        onPacksChanged(next.packs)
                    } catch (_: IOException) {
                        errorResource = R.string.linter_check_failed
                    } catch (_: IllegalArgumentException) {
                        errorResource = R.string.linter_check_failed
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = packs.isNotEmpty() && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.linter_check_and_fix))
        }
        if (busy) CircularProgressIndicator()
        if (errorResource != 0) {
            Text(
                stringResource(errorResource),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        report?.let { result ->
            if (result.presetUnavailable) {
                Text(stringResource(R.string.linter_preset_unavailable))
            }
            if (result.imageUnreadable) {
                Text(stringResource(R.string.linter_image_unreadable))
            }
            if (result.repairFailed) {
                Text(stringResource(R.string.linter_repair_failed))
            }
            if (result.foundNoFindings && !result.presetUnavailable) {
                Text(stringResource(R.string.linter_no_findings))
            }
            result.rows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.linter_marketplace_rule, row.marketplaceName, row.ruleId),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.linter_violated_limit, row.measuredLimit),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    when (row.disposition) {
                        LinterActionDisposition.REPAIRED ->
                            Text(stringResource(R.string.linter_repaired), style = MaterialTheme.typography.bodyMedium)
                        LinterActionDisposition.SELLER_CHANGE ->
                            Text(stringResource(R.string.linter_seller_change), style = MaterialTheme.typography.bodyMedium)
                        LinterActionDisposition.CREDIT_COST -> Text(
                            row.credits?.let { stringResource(R.string.linter_cost_before_run, it) }
                                ?: stringResource(R.string.linter_credit_cost_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinterActionDisposition.MANUAL_ACTION ->
                            Text(stringResource(R.string.linter_manual_action), style = MaterialTheme.typography.bodyMedium)
                        LinterActionDisposition.CANNOT_EDIT ->
                            Text(stringResource(R.string.linter_cannot_edit), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
