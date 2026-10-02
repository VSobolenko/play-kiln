package com.gexetr.lawnroundlog.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.domain.VisitStatus
import com.gexetr.lawnroundlog.ui.ScreenHeader
import com.gexetr.lawnroundlog.ui.components.GlyphDelete
import com.gexetr.lawnroundlog.ui.components.GlyphEdit
import com.gexetr.lawnroundlog.ui.components.GlyphExport
import com.gexetr.lawnroundlog.ui.components.GlyphLock
import com.gexetr.lawnroundlog.ui.components.RoundEmptyState
import com.gexetr.lawnroundlog.ui.components.SectionStrip
import com.gexetr.lawnroundlog.ui.components.VisitStatusPill
import com.gexetr.lawnroundlog.ui.round.RoundViewModel
import com.gexetr.lawnroundlog.ui.round.formatAmount
import com.gexetr.lawnroundlog.ui.round.formatAmountInput
import com.gexetr.lawnroundlog.ui.settings.SettingsUiState

/**
 * The ledger: everything recorded, newest first, correctable and deletable.
 *
 * The same record the round writes, read back as a timeline, with export behind the unlock. Nothing on
 * this screen is a copy — every row is derived from the round as it stands, so correcting a visit here
 * is the same edit as correcting it there.
 */
@Composable
fun HistoryRoute(
    round: RoundViewModel,
    settings: SettingsUiState,
    unlocked: Boolean,
    onBack: () -> Unit,
    onRequestUnlock: () -> Unit,
) {
    val state by round.state.collectAsStateWithLifecycle()
    val history: HistoryViewModel = viewModel()
    val filter by history.filter.collectAsStateWithLifecycle()
    val exportStatus by history.export.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val latestRecord = rememberUpdatedState(state.record)

    // The picker is the system's, so the file lands where the operator chooses and the app never needs
    // a storage permission. The write happens in the ViewModel against the record as it stands when the
    // picker returns, which is why the record is read through `rememberUpdatedState` rather than
    // captured once at the call site.
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { target ->
        if (target != null) {
            history.exportCsv(context, target, latestRecord.value)
        }
    }

    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    val entries = remember(state.record, filter) { buildLedger(state.record, filter) }
    val editingEntry = entries.firstOrNull { it.id == editing }
    val deletingEntry = entries.firstOrNull { it.id == deleting }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .imePadding(),
    ) {
        ScreenHeader(
            title = stringResource(R.string.history_title),
            subtitle = stringResource(R.string.history_count, entries.size),
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        if (unlocked) {
                            createDocument.launch("lawn-round-log.csv")
                        } else {
                            onRequestUnlock()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    // One target, two states: the glyph says whether the tap exports the ledger or
                    // opens the unlock, and the label says the same thing in words.
                    val gateLabel = stringResource(
                        if (unlocked) R.string.export_csv else R.string.unlock_title,
                    )
                    if (unlocked) {
                        GlyphExport(
                            modifier = Modifier.semantics { contentDescription = gateLabel },
                        )
                    } else {
                        GlyphLock(
                            modifier = Modifier.semantics { contentDescription = gateLabel },
                        )
                    }
                }
            },
        )

        SectionStrip(
            items = listOf(
                stringResource(R.string.history_filter_all),
                stringResource(R.string.history_filter_visits),
                stringResource(R.string.history_filter_payments),
            ),
            selectedIndex = filter.ordinal,
            onSelect = { index -> history.setFilter(HistoryFilter.entries[index]) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        ExportBanner(status = exportStatus, onDismiss = history::clearExportStatus)

        if (entries.isEmpty()) {
            RoundEmptyState(
                title = stringResource(R.string.history_empty),
                body = stringResource(R.string.history_empty_body),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = entries, key = { entry -> entry.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { editing = entry.id },
                        // "Ask before a customer, visit or payment is removed" is a preference the
                        // History screen honours too: with it off, the delete happens on the tap and
                        // the dialog is not shown. Leaving it unconditional here would make the
                        // setting true on one screen and false on another.
                        onDelete = {
                            if (settings.confirmBeforeDelete) {
                                deleting = entry.id
                            } else {
                                entry.visit?.let { visit -> round.deleteVisit(visit) }
                                entry.payment?.let { payment -> round.removePayment(payment) }
                            }
                        },
                    )
                }
            }
        }
    }

    if (editingEntry != null) {
        EditEntryDialog(
            entry = editingEntry,
            currencySymbol = settings.currencySymbol,
            onDismiss = { editing = null },
            onSetStatus = { status ->
                editingEntry.visit?.let { round.restateVisit(it, status) }
                editing = null
            },
            onSetAmount = { amount ->
                editingEntry.payment?.let { round.recordPayment(it.customerId, amount) }
                editing = null
            },
        )
    }

    if (deletingEntry != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.history_delete_entry_body,
                        deletingEntry.customerName.ifBlank {
                            stringResource(R.string.history_unknown_customer)
                        },
                        deletingEntry.dateIso,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingEntry.visit?.let { round.deleteVisit(it) }
                        deletingEntry.payment?.let { round.removePayment(it) }
                        deleting = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // The banner is cleared when the screen leaves, so reopening history does not replay a report about
    // a file the operator already has.
    DisposableEffect(Unit) {
        onDispose { history.clearExportStatus() }
    }
}

/** One recorded event: who, when, how much, and the two things that can be done to it. */
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.customerName.ifBlank { stringResource(R.string.history_unknown_customer) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                Text(
                    text = entry.dateIso,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.status != null) {
                    Spacer(Modifier.size(6.dp))
                    VisitStatusPill(status = entry.status)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (entry.payment != null) {
                        formatAmount(currencySymbol, entry.amountUsd)
                    } else {
                        stringResource(R.string.history_visit_amount, formatAmount(currencySymbol, entry.amountUsd))
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp),
                    ) {
                        val editLabel = stringResource(R.string.action_edit_entry, entry.customerName)
                        GlyphEdit(
                            modifier = Modifier.semantics { contentDescription = editLabel },
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp),
                    ) {
                        val deleteLabel = stringResource(R.string.action_delete_entry, entry.customerName)
                        GlyphDelete(
                            modifier = Modifier.semantics { contentDescription = deleteLabel },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The correction dialog. It shows the controls for whichever kind of entry was opened, and nothing for
 * the other — a visit has an outcome and a payment has an amount, and offering both would invite the
 * operator to change the wrong one.
 */
@Composable
private fun EditEntryDialog(
    entry: HistoryEntry,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSetStatus: (VisitStatus) -> Unit,
    onSetAmount: (Double) -> Unit,
) {
    var amountText by rememberSaveable(entry.id) {
        mutableStateOf(if (entry.payment != null) formatAmountInput(entry.amountUsd) else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(
                        R.string.history_edit_subtitle,
                        entry.customerName.ifBlank { stringResource(R.string.history_unknown_customer) },
                        entry.dateIso,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (entry.visit != null) {
                    Text(
                        text = stringResource(R.string.history_edit_status),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onSetStatus(VisitStatus.MOWED) }) {
                            Text(stringResource(R.string.visit_mowed))
                        }
                        TextButton(onClick = { onSetStatus(VisitStatus.SKIPPED) }) {
                            Text(stringResource(R.string.visit_skipped))
                        }
                        TextButton(onClick = { onSetStatus(VisitStatus.RAINED_OFF) }) {
                            Text(stringResource(R.string.visit_rained_off))
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            val normalized = input.replace(',', '.')
                            if (normalized.isEmpty() || normalized.all { it.isDigit() || it == '.' }) {
                                amountText = normalized
                            }
                        },
                        label = { Text(stringResource(R.string.payment_amount)) },
                        prefix = { Text(currencySymbol) },
                        singleLine = false,
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { amountText.toDoubleOrNull()?.let(onSetAmount) },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (entry.payment != null) {
                TextButton(
                    onClick = { amountText.toDoubleOrNull()?.let(onSetAmount) },
                ) { Text(stringResource(R.string.action_save)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * What the last export wrote.
 *
 * It reports the row count the export returned rather than the fact that a file exists, because a file
 * with no rows in it is exactly the outcome the operator needs to be told about.
 */
@Composable
private fun ExportBanner(status: ExportStatus, onDismiss: () -> Unit) {
    val message = when (status) {
        ExportStatus.Idle -> return
        ExportStatus.Failed -> stringResource(R.string.export_failed)
        is ExportStatus.Done -> stringResource(R.string.export_done, status.rows)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    }
}
