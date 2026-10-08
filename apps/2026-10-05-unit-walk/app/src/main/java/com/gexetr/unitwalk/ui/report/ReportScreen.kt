package com.gexetr.unitwalk.ui.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.capability.billing.BillingController
import com.gexetr.unitwalk.R
import com.gexetr.unitwalk.ui.components.HeroPanel
import com.gexetr.unitwalk.ui.components.PillKind
import com.gexetr.unitwalk.ui.components.StatusPill
import com.gexetr.unitwalk.ui.theme.PlayKilnTheme
import java.time.Instant

/**
 * The finished report and its two real handoffs — the surface behind the `signed-pdf-share`
 * promise.
 *
 * The split follows `ui/settings/SettingsScreen.kt`: [ReportScreen] / [ReportRoute] build the ViewModel and
 * collect its state; `ReportContent` is a pure function of [ReportUiState] that emits
 * [ReportEvent]s. The PDF itself, the `content://` URI and the two intents belong to
 * [ReportViewModel] — this file never rebuilds a URI, and it never writes a file by hand.
 *
 * The handoffs are the point of the screen:
 *
 *  - **Share** wraps the ViewModel's own [ReportViewModel.shareIntent] (`ACTION_SEND` over the
 *    `FileProvider` URI, with `FLAG_GRANT_READ_URI_PERMISSION`) in `Intent.createChooser`.
 *  - **Save** launches `ActivityResultContracts.CreateDocument` so the user picks a destination,
 *    then hands the returned URI to [ReportViewModel.writeReportTo].
 *
 * Neither handoff is ever gated on ownership: a free user can always share and save. The one-time
 * unlock changes only what the bytes carry — a clean report instead of a watermarked one — so the
 * upsell below is shown while the unlock is not confirmed owned, and never to an owner.
 *
 * What the app wrote is reported, never assumed: the file's name and byte count are printed from
 * the state, and `report_status_failed` / `report_file_missing` say so out loud. Deleting a walk
 * goes through a dialog whose message names the item.
 */
@Composable
fun ReportScreen(
    walkId: String,
    onBack: () -> Unit,
    onOpenUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    billing: BillingController? = null,
) {
    val viewModel: ReportViewModel = viewModel(
        key = walkId,
        factory = ReportViewModel.factory(LocalContext.current, walkId, billing),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ReportContent(
        state = state,
        onEvent = viewModel::onEvent,
        onShare = { shareReport(context, viewModel.shareIntent()) },
        onSave = { viewModel.writeReportTo(it) },
        onBack = onBack,
        onOpenUnlock = onOpenUnlock,
        modifier = modifier,
    )
}

/** The route form, mirroring `SettingsRoute`: same ViewModel, same state, one call site either way. */
@Composable
fun ReportRoute(
    walkId: String,
    onBack: () -> Unit,
    onOpenUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    billing: BillingController? = null,
) {
    ReportScreen(
        walkId = walkId,
        onBack = onBack,
        onOpenUnlock = onOpenUnlock,
        modifier = modifier,
        billing = billing,
    )
}

/**
 * Fires the share sheet with the ViewModel's intent. A `null` intent means no report exists yet, so
 * there is nothing to share and nothing is launched — the button is already disabled in that case.
 */
private fun shareReport(context: Context, intent: Intent?) {
    if (intent == null) return
    val chooser = Intent.createChooser(
        intent,
        context.getString(R.string.report_share_chooser_title),
    )
    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(chooser) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportContent(
    state: ReportUiState,
    onEvent: (ReportEvent) -> Unit,
    onShare: () -> Unit,
    onSave: (Uri) -> Unit,
    onBack: () -> Unit,
    onOpenUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        stringResource(
            when (message) {
                ReportMessage.PdfWritten -> R.string.report_message_pdf_written
                ReportMessage.PdfWriteFailed -> R.string.report_message_pdf_failed
                ReportMessage.SignatureSaved -> R.string.report_message_signature_saved
                ReportMessage.SavedToChosenLocation -> R.string.report_message_saved_to_location
                ReportMessage.SaveFailed -> R.string.report_message_save_failed
                ReportMessage.NothingToSave -> R.string.report_message_nothing_to_save
            },
        )
    }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onEvent(ReportEvent.MessageDismissed)
        }
    }

    // The system save picker. The suggested name comes from the state, never from a literal.
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(PDF_MIME_TYPE),
    ) { target: Uri? ->
        if (target != null) onSave(target)
    }

    val isReady = state.status == ReportStatus.Ready && state.pdfUri != null
    val defaultFileName = stringResource(R.string.report_default_file_name)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.report_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroPanel(
                title = state.unitName.ifBlank { stringResource(R.string.report_title) },
                subtitle = stringResource(R.string.report_hero_subtitle),
                stat = state.roomCount.toString(),
                statLabel = stringResource(R.string.report_stat_rooms),
            )

            StatusPill(
                label = stringResource(statusResource(state.status)),
                kind = when (state.status) {
                    ReportStatus.Ready -> PillKind.OK
                    ReportStatus.Loading -> PillKind.NEUTRAL
                    ReportStatus.Empty -> PillKind.WARN
                    ReportStatus.Failed -> PillKind.DANGER
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    value = state.photoCount.toString(),
                    label = stringResource(R.string.report_stat_photos),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = state.deltaCount.toString(),
                    label = stringResource(R.string.report_stat_differences),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = state.signatureCount.toString(),
                    label = stringResource(R.string.report_stat_signatures),
                    modifier = Modifier.weight(1f),
                )
            }

            // What the app actually wrote — the file's name and size, or plainly nothing yet.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.report_file_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val fileName = state.pdfFileName
                    if (fileName == null) {
                        Text(
                            text = stringResource(R.string.report_file_missing),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.report_bytes_format,
                                state.pdfByteCount.toInt(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.status == ReportStatus.Empty) {
                Text(
                    text = stringResource(R.string.report_status_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = onShare,
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.report_share_action))
            }

            OutlinedButton(
                onClick = {
                    createDocument.launch(
                        state.suggestedFileName
                            ?: defaultFileName,
                    )
                },
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.report_save_action))
            }

            SignatureSection(
                signatureCount = state.signatureCount,
                onSign = { name -> onEvent(ReportEvent.SignatureEntered(name, Instant.now().toString())) },
            )

            // The paid perk is a cleaner report, never the ability to export one. Shown only while
            // the unlock is not confirmed owned — an owner is never told to buy something he has.
            if (!state.isOwned) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.report_locked_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Button(
                            onClick = onOpenUnlock,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.report_locked_action))
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    onEvent(
                        ReportEvent.DeleteWalkRequested(
                            walkId = state.walkId,
                            walkName = state.unitName,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.report_delete_walk_action))
            }
        }
    }

    val pending = state.pendingDeletion
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { onEvent(ReportEvent.DeletionCancelled) },
            title = { Text(stringResource(R.string.report_delete_walk_title)) },
            text = {
                Text(
                    stringResource(R.string.report_delete_walk_message, pending.displayName),
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(ReportEvent.DeletionConfirmed) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.report_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ReportEvent.DeletionCancelled) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.report_delete_cancel))
                }
            },
        )
    }
}

/** Landlord and tenant names, each saved on its own so a half-filled form is never a silent no-op. */
@Composable
private fun SignatureSection(
    signatureCount: Int,
    onSign: (String) -> Unit,
) {
    var landlordName by rememberSaveable { mutableStateOf("") }
    var tenantName by rememberSaveable { mutableStateOf("") }
    var warningVisible by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.report_section_signatures),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.report_signature_count_format, signatureCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = landlordName,
                onValueChange = { landlordName = it },
                label = { Text(stringResource(R.string.report_signature_landlord_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (landlordName.isBlank()) {
                        warningVisible = true
                    } else {
                        warningVisible = false
                        onSign(landlordName.trim())
                        landlordName = ""
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.report_signature_landlord_save))
            }

            OutlinedTextField(
                value = tenantName,
                onValueChange = { tenantName = it },
                label = { Text(stringResource(R.string.report_signature_tenant_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (tenantName.isBlank()) {
                        warningVisible = true
                    } else {
                        warningVisible = false
                        onSign(tenantName.trim())
                        tenantName = ""
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.report_signature_tenant_save))
            }

            if (warningVisible) {
                Text(
                    text = stringResource(R.string.report_signature_name_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** A label-and-number tile; the number and the word are both text, so nothing is colour-only. */
@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun statusResource(status: ReportStatus): Int = when (status) {
    ReportStatus.Loading -> R.string.report_status_loading
    ReportStatus.Ready -> R.string.report_status_ready
    ReportStatus.Empty -> R.string.report_status_empty
    ReportStatus.Failed -> R.string.report_status_failed
}

private const val PDF_MIME_TYPE = "application/pdf"

@Preview(name = "Report ready", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ReportContentReadyPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        ReportContent(
            state = ReportUiState(
                status = ReportStatus.Ready,
                walkId = "walk-1",
                unitName = "Unit A",
                roomCount = 4,
                photoCount = 9,
                deltaCount = 2,
                signatureCount = 1,
                pdfUri = Uri.parse("content://com.gexetr.unitwalk.fileprovider/reports/report-walk-1.pdf"),
                pdfFileName = "report-walk-1.pdf",
                pdfByteCount = 24_576,
                suggestedFileName = "Unit_A.pdf",
            ),
            onEvent = {},
            onShare = {},
            onSave = {},
            onBack = {},
            onOpenUnlock = {},
        )
    }
}

@Preview(name = "Report empty", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ReportContentEmptyPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        ReportContent(
            state = ReportUiState(
                status = ReportStatus.Empty,
                walkId = "walk-2",
                unitName = "Unit B",
                suggestedFileName = "unit-walk-report.pdf",
            ),
            onEvent = {},
            onShare = {},
            onSave = {},
            onBack = {},
            onOpenUnlock = {},
        )
    }
}
