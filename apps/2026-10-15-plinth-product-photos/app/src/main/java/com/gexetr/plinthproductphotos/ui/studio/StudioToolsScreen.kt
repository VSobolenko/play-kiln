package com.gexetr.plinthproductphotos.ui.studio

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

// Superseded catalogue screen; StudioRoute is the routed live studio.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.series.SeriesSettlement
import com.gexetr.plinthproductphotos.domain.studio.OperationAccess
import com.gexetr.plinthproductphotos.domain.studio.OperationOffer
import com.gexetr.plinthproductphotos.domain.studio.OperationUnavailableReason
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationCatalog
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationConfig
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationId
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationRequest
import com.gexetr.plinthproductphotos.domain.studio.UpscaleResolution
import com.gexetr.plinthproductphotos.ui.components.PlinthStage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioToolsScreen(
    balanceCredits: Int,
    preset: MarketplacePreset?,
    onOpenWallet: () -> Unit,
    onSelectOperation: (OperationOffer) -> Unit,
    modifier: Modifier = Modifier,
    config: StudioOperationConfig = StudioOperationConfig(),
    seriesProgress: SeriesSettlement? = null,
    mainRequiresRealPhotograph: Boolean = false,
) {
    var seriesVariantCount by rememberSaveable { mutableIntStateOf(1) }
    var seriesOperation by rememberSaveable { mutableStateOf(StudioOperationId.RECOLOUR) }
    var selectedRequest by remember { mutableStateOf<StudioOperationRequest?>(null) }
    val requests = remember(seriesVariantCount, seriesOperation) {
        listOf(
            StudioOperationRequest(StudioOperationId.RELIGHT),
            StudioOperationRequest(StudioOperationId.EXPAND_CANVAS),
            StudioOperationRequest(StudioOperationId.ERASE_REGION),
            StudioOperationRequest(StudioOperationId.UPSCALE, upscaleResolution = UpscaleResolution.TWO_K),
            StudioOperationRequest(StudioOperationId.UPSCALE, upscaleResolution = UpscaleResolution.FOUR_K),
            StudioOperationRequest(StudioOperationId.RECOLOUR),
            StudioOperationRequest(StudioOperationId.GHOST_MANNEQUIN),
            StudioOperationRequest(StudioOperationId.VIRTUAL_MODEL),
            StudioOperationRequest(
                operationId = StudioOperationId.SERIES,
                variantCount = seriesVariantCount,
                seriesOperation = seriesOperation,
            ),
        )
    }
    val offers = remember(requests, balanceCredits, preset, config, mainRequiresRealPhotograph) {
        requests.map { request ->
            StudioOperationCatalog.offer(
                request = request,
                balanceCredits = balanceCredits,
                preset = preset,
                config = config,
                isMainSlot = true,
                mainRequiresRealPhotograph = mainRequiresRealPhotograph,
            )
        }
    }
    val hasUnmeasuredEnhancedThresholds = offers.any { offer ->
        (offer.access as? OperationAccess.Unavailable)?.reasons
            ?.contains(OperationUnavailableReason.FIDELITY_THRESHOLDS_UNMEASURED) == true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.studio_tools_title)) })
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.w6_studio_one_balance, balanceCredits),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = onOpenWallet, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.w6_studio_top_up))
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PlinthStage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.studio_tools_heading),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.studio_catalogue_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (hasUnmeasuredEnhancedThresholds) {
                            Text(
                                text = stringResource(R.string.studio_s2_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(stringResource(R.string.w6_studio_credit_explanation))
                    }
                }
            }
            items(
                items = offers.filter { it.request.operationId != StudioOperationId.SERIES },
                key = { offer ->
                    "${offer.operation.id.wireValue}-${offer.request.upscaleResolution}-" +
                        "${offer.request.variantCount}-${offer.request.seriesOperation}"
                },
            ) { offer ->
                StudioOperationRow(
                    offer = offer,
                    onOpenWallet = onOpenWallet,
                    onSelectOperation = { selectedRequest = it.request },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                SeriesOptions(
                    operation = seriesOperation,
                    variantCount = seriesVariantCount,
                    onOperationChange = { seriesOperation = it },
                    onVariantCountChange = { seriesVariantCount = it },
                )
                StudioOperationRow(
                    offer = offers.last(),
                    onOpenWallet = onOpenWallet,
                    onSelectOperation = { selectedRequest = it.request },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                SeriesProgress(progress = seriesProgress)
            }
        }
        offers.firstOrNull { it.request == selectedRequest }?.let { offer ->
            AlertDialog(
                onDismissRequest = { selectedRequest = null },
                title = { Text(stringResource(operationNameResource(offer.request.operationId))) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(studioToolPurposeResource(offer.request)))
                        Text(costLineResource(offer))
                        Text(stringResource(R.string.w6_studio_delivery_charge))
                        (offer.access as? OperationAccess.Unavailable)?.reasons?.forEach {
                            Text(stringResource(reasonResource(it)), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    when (offer.access) {
                        OperationAccess.Available -> Button(onClick = {
                            selectedRequest = null
                            onSelectOperation(offer)
                        }) { Text(stringResource(R.string.w6_studio_confirm)) }
                        is OperationAccess.InsufficientCredits -> TextButton(onClick = {
                            selectedRequest = null
                            onOpenWallet()
                        }) { Text(stringResource(R.string.w6_studio_top_up)) }
                        is OperationAccess.Unavailable -> Unit
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedRequest = null }) {
                        Text(stringResource(R.string.w6_studio_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SeriesOptions(
    operation: StudioOperationId,
    variantCount: Int,
    onOperationChange: (StudioOperationId) -> Unit,
    onVariantCountChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.studio_series_options), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.w6_studio_series_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SeriesOperationChoice(
            label = stringResource(R.string.studio_series_recolour),
            selected = operation == StudioOperationId.RECOLOUR,
            onClick = { onOperationChange(StudioOperationId.RECOLOUR) },
        )
        SeriesOperationChoice(
            label = stringResource(R.string.studio_series_virtual_model),
            selected = operation == StudioOperationId.VIRTUAL_MODEL,
            onClick = { onOperationChange(StudioOperationId.VIRTUAL_MODEL) },
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.studio_series_variant_count, variantCount),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { if (variantCount > 1) onVariantCountChange(variantCount - 1) },
                    enabled = variantCount > 1,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_series_decrease))
                }
                TextButton(
                    onClick = { if (variantCount < Int.MAX_VALUE) onVariantCountChange(variantCount + 1) },
                    enabled = variantCount < Int.MAX_VALUE,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_series_increase))
                }
            }
        }
    }
}

@Composable
private fun SeriesOperationChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        trailingContent = {
            Text(stringResource(if (selected) R.string.studio_series_selected else R.string.studio_series_choose))
        },
    )
}

@Composable
private fun StudioOperationRow(
    offer: OperationOffer,
    onOpenWallet: () -> Unit,
    onSelectOperation: (OperationOffer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(operationNameResource(offer.request.operationId)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(studioToolPurposeResource(offer.request)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(provenanceResource(StudioOperationCatalog.provenanceFor(offer.request))))
                Text(text = costLineResource(offer))
                when (val access = offer.access) {
                    OperationAccess.Available -> Text(
                        stringResource(R.string.studio_operation_available),
                        color = PlinthTheme.accentInk,
                    )
                    is OperationAccess.Unavailable -> access.reasons.forEach { reason ->
                        Text(
                            text = stringResource(reasonResource(reason)),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is OperationAccess.InsufficientCredits -> Text(
                        text = stringResource(
                            R.string.studio_insufficient_credits,
                            access.shortfallCredits,
                            offer.balanceBeforeCredits,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            when (val access = offer.access) {
                OperationAccess.Available -> TextButton(
                    onClick = { onSelectOperation(offer) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_operation_select))
                }
                is OperationAccess.Unavailable -> Text(
                    text = stringResource(R.string.studio_operation_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is OperationAccess.InsufficientCredits -> TextButton(
                    onClick = onOpenWallet,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.studio_open_wallet))
                }
            }
        }
    }
}

internal fun studioToolPurposeResource(request: StudioOperationRequest): Int = when (request.operationId) {
    StudioOperationId.RELIGHT -> R.string.w6_studio_purpose_relight
    StudioOperationId.EXPAND_CANVAS -> R.string.w6_studio_purpose_expand
    StudioOperationId.ERASE_REGION -> R.string.w6_studio_purpose_erase
    StudioOperationId.UPSCALE -> when (request.upscaleResolution) {
        UpscaleResolution.TWO_K -> R.string.w6_studio_purpose_upscale_2k
        UpscaleResolution.FOUR_K -> R.string.w6_studio_purpose_upscale_4k
    }
    StudioOperationId.RECOLOUR -> R.string.w6_studio_purpose_recolour
    StudioOperationId.GHOST_MANNEQUIN -> R.string.w6_studio_purpose_ghost
    StudioOperationId.VIRTUAL_MODEL -> R.string.w6_studio_purpose_model
    StudioOperationId.SERIES -> R.string.w6_studio_purpose_series
    else -> error("Operation ${request.operationId.wireValue} is not listed on S-14")
}

@Composable
private fun SeriesProgress(progress: SeriesSettlement?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.studio_series_progress), style = MaterialTheme.typography.titleMedium)
        if (progress == null) {
            Text(
                text = stringResource(R.string.studio_series_not_started),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(
                    R.string.studio_series_delivered_count,
                    progress.deliveredVariants,
                    progress.variants.size,
                ),
            )
            Text(
                stringResource(
                    R.string.studio_series_ledger_movements,
                    progress.reservedCredits,
                    progress.settledCredits,
                    progress.releasedCredits,
                ),
            )
            Text(
                stringResource(
                    if (progress.isTerminal) R.string.studio_series_complete else R.string.studio_series_running,
                ),
                color = PlinthTheme.accentInk,
            )
        }
    }
}

private fun operationNameResource(id: StudioOperationId): Int = when (id) {
    StudioOperationId.RELIGHT -> R.string.studio_operation_relight
    StudioOperationId.EXPAND_CANVAS -> R.string.studio_operation_expand_canvas
    StudioOperationId.ERASE_REGION -> R.string.studio_operation_erase_region
    StudioOperationId.UPSCALE -> R.string.studio_operation_upscale
    StudioOperationId.RECOLOUR -> R.string.studio_operation_recolour
    StudioOperationId.GHOST_MANNEQUIN -> R.string.studio_operation_ghost_mannequin
    StudioOperationId.VIRTUAL_MODEL -> R.string.studio_operation_virtual_model
    StudioOperationId.SERIES -> R.string.studio_operation_series
    else -> error("Operation ${id.wireValue} is not listed on S-14")
}

@Composable
private fun costLineResource(offer: OperationOffer): String {
    if (offer.request.operationId == StudioOperationId.SERIES) {
        return if (offer.balanceAfterCredits == null) {
            stringResource(
                R.string.studio_series_cost_insufficient,
                offer.cost.credits,
                offer.request.variantCount,
                offer.cost.unitCredits,
                offer.balanceBeforeCredits,
            )
        } else {
            stringResource(
                R.string.studio_series_cost_after,
                offer.cost.credits,
                offer.request.variantCount,
                offer.cost.unitCredits,
                offer.balanceBeforeCredits,
                offer.balanceAfterCredits,
            )
        }
    }
    return if (offer.balanceAfterCredits == null) {
        stringResource(
            R.string.studio_cost_and_balance,
            offer.cost.credits,
            offer.balanceBeforeCredits,
        )
    } else {
        stringResource(
            R.string.studio_cost_balance_after,
            offer.cost.credits,
            offer.balanceBeforeCredits,
            offer.balanceAfterCredits,
        )
    }
}

private fun provenanceResource(provenance: Provenance?): Int = when (provenance) {
    Provenance.ORIGINAL_PIXELS -> R.string.studio_provenance_original
    Provenance.MANUALLY_ADJUSTED -> R.string.studio_provenance_adjusted
    Provenance.COMPOSITE_GENERATED_BACKGROUND -> R.string.studio_provenance_composite
    Provenance.ENHANCED -> R.string.studio_provenance_enhanced
    null -> R.string.studio_provenance_inherited
}

private fun reasonResource(reason: OperationUnavailableReason): Int = when (reason) {
    OperationUnavailableReason.ORIGINAL_ONLY_MARKETPLACE,
    OperationUnavailableReason.ORIGINAL_ONLY_OPERATION -> R.string.studio_reason_original_only
    OperationUnavailableReason.FIDELITY_THRESHOLDS_UNMEASURED -> R.string.studio_reason_s2
    OperationUnavailableReason.OPERATION_DISABLED -> R.string.studio_reason_operation_disabled
    OperationUnavailableReason.SERVER_OPERATION_DISABLED -> R.string.studio_reason_server_disabled
    OperationUnavailableReason.BACKGROUND_POLICY -> R.string.studio_reason_background
    OperationUnavailableReason.WHITE_MAIN_BACKGROUND -> R.string.studio_reason_white_main
    OperationUnavailableReason.REAL_PHOTO_REQUIRED -> R.string.studio_reason_real_photo
    OperationUnavailableReason.SLIDE_OVERLAY_NOT_ALLOWED -> R.string.studio_reason_slide_overlay
    OperationUnavailableReason.GHOST_MANNEQUIN_ROUTE_MISSING -> R.string.studio_reason_ghost_route
    OperationUnavailableReason.VIRTUAL_MODEL_REVIEW_PENDING -> R.string.studio_reason_virtual_model
    OperationUnavailableReason.SERIES_VARIANT_BLOCKED -> R.string.studio_reason_series_variant
    OperationUnavailableReason.PRESET_REQUIRED -> R.string.studio_reason_preset_required
}
