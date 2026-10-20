package com.gexetr.plinthproductphotos.ui.report

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.domain.provenance.ReportApiClient
import com.gexetr.plinthproductphotos.domain.provenance.ReportCategory
import com.gexetr.plinthproductphotos.domain.provenance.ReportResponse
import com.gexetr.plinthproductphotos.domain.provenance.ReportStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReportTarget(
    val jobId: String,
    val operationLabel: String,
    val outputLabel: String,
)

private val defaultReportApi = ReportApiClient()

@Composable
fun ReportOutputAction(
    target: ReportTarget,
    deviceToken: String,
    modifier: Modifier = Modifier,
    reportApi: ReportApiClient = defaultReportApi,
) {
    val scope = rememberCoroutineScope()
    val categories = listOf(
        ReportCategory.OFFENSIVE to stringResource(R.string.report_reason_offensive),
        ReportCategory.WRONG_PRODUCT to stringResource(R.string.report_reason_wrong_product),
        ReportCategory.UNSAFE to stringResource(R.string.report_reason_unsafe),
        ReportCategory.RIGHTS to stringResource(R.string.report_reason_rights),
        ReportCategory.OTHER to stringResource(R.string.report_reason_other),
    )
    val networkFailure = stringResource(R.string.report_network_failure)
    val unknownFailure = stringResource(R.string.report_unknown_failure)
    var dialogOpen by remember(target) { mutableStateOf(false) }
    var selectedCategory by remember(target) { mutableStateOf(ReportCategory.OFFENSIVE) }
    var note by remember(target) { mutableStateOf("") }
    var sending by remember(target) { mutableStateOf(false) }
    var response by remember(target) { mutableStateOf<ReportResponse?>(null) }
    var status by remember(target) { mutableStateOf<ReportStatusResponse?>(null) }
    var failureText by remember(target) { mutableStateOf<String?>(null) }

    Button(
        onClick = { dialogOpen = true },
        enabled = response == null,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(
            stringResource(
                if (response == null) R.string.report_action else R.string.report_recorded_action,
            ),
        )
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!sending) dialogOpen = false },
            title = { Text(stringResource(R.string.report_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(
                            R.string.report_output_context,
                            target.operationLabel,
                            target.outputLabel,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    categories.forEach { (category, label) ->
                        Row(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { if (response == null) selectedCategory = category },
                                enabled = response == null && !sending,
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            if (response == null && it.length <= ReportApiClient.MAX_NOTE_LENGTH) {
                                note = it
                            }
                        },
                        label = { Text(stringResource(R.string.report_note_label)) },
                        supportingText = {
                            Text(
                                stringResource(
                                    R.string.report_note_counter,
                                    note.length,
                                    ReportApiClient.MAX_NOTE_LENGTH,
                                ),
                            )
                        },
                        enabled = response == null && !sending,
                        modifier = Modifier.heightIn(min = 56.dp),
                    )
                    Text(
                        stringResource(R.string.report_output_stays_local),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.provenance_retention_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    response?.let {
                        Text(
                            stringResource(R.string.report_receipt, it.id, status?.caseStatus ?: it.status.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PlinthTheme.accentInk,
                        )
                        if (status == null) {
                            Text(
                                stringResource(R.string.report_readback_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    failureText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (sending) CircularProgressIndicator()
                }
            },
            confirmButton = {
                if (response == null) {
                    Button(
                        enabled = !sending,
                        onClick = {
                            failureText = null
                            sending = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    reportApi.submit(
                                        jobId = target.jobId,
                                        category = selectedCategory,
                                        note = note,
                                        deviceToken = deviceToken,
                                    )
                                }
                                when (result) {
                                    is ApiResult.Success -> {
                                        response = result.value
                                        val currentStatus = withContext(Dispatchers.IO) {
                                            reportApi.getStatus(result.value.id, deviceToken)
                                        }
                                        if (currentStatus is ApiResult.Success) status = currentStatus.value
                                    }
                                    is ApiResult.Failure -> failureText = when (result.failure) {
                                        is ApiFailure.Network -> networkFailure
                                        else -> result.envelope?.message ?: unknownFailure
                                    }
                                }
                                sending = false
                            }
                        },
                    ) {
                        Text(stringResource(R.string.report_submit))
                    }
                } else {
                    TextButton(onClick = { dialogOpen = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.report_done))
                    }
                }
            },
            dismissButton = {
                if (response == null) {
                    TextButton(
                        onClick = { if (!sending) dialogOpen = false },
                        enabled = !sending,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.report_cancel))
                    }
                }
            },
        )
    }
}
