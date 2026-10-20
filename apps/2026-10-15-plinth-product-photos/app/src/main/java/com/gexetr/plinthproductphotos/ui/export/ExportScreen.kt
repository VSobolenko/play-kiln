package com.gexetr.plinthproductphotos.ui.export

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.export.ExportDelivery
import com.gexetr.plinthproductphotos.domain.export.ExportArtifact
import com.gexetr.plinthproductphotos.domain.export.ExportPack
import com.gexetr.plinthproductphotos.domain.export.FileNaming
import com.gexetr.plinthproductphotos.domain.export.ImageProcessing
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.lint.MarketplaceLinterPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    packs: List<ExportPack>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exportSavedMessage = stringResource(R.string.export_saved)
    val exportDeliveryFailedMessage = stringResource(R.string.export_delivery_failed)
    val exportShareTitle = stringResource(R.string.export_share_title)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var exportPacks by remember { mutableStateOf(packs) }
    var pendingImage by remember { mutableStateOf<ExportArtifact?>(null) }
    var pendingPack by remember { mutableStateOf<ExportPack?>(null) }
    var pendingBundle by remember { mutableStateOf<List<ExportPack>?>(null) }
    var busy by remember { mutableStateOf(false) }
    val packList = remember(exportPacks) { ExportPackListModel(exportPacks) }

    fun onImageDocumentCreated(uri: Uri?) {
        val artifact = pendingImage
        pendingImage = null
        if (uri != null && artifact != null) {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { ExportDelivery.writeImageTo(context, uri, artifact) }
                    snackbar.showSnackbar(exportSavedMessage)
                } catch (_: Exception) {
                    snackbar.showSnackbar(exportDeliveryFailedMessage)
                } finally {
                    busy = false
                }
            }
        }
    }

    val imageDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
        ::onImageDocumentCreated,
    )
    val pngDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
        ::onImageDocumentCreated,
    )
    val webpDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/webp"),
        ::onImageDocumentCreated,
    )
    val zipDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val pack = pendingPack
        pendingPack = null
        if (uri != null && pack != null) {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { ExportDelivery.writePackTo(context, uri, pack) }
                    snackbar.showSnackbar(exportSavedMessage)
                } catch (_: Exception) {
                    snackbar.showSnackbar(exportDeliveryFailedMessage)
                } finally {
                    busy = false
                }
            }
        }
    }
    val bundleDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val selectedPacks = pendingBundle
        pendingBundle = null
        if (uri != null && selectedPacks != null) {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) {
                        ExportDelivery.writeBundleTo(context, uri, selectedPacks)
                    }
                    snackbar.showSnackbar(exportSavedMessage)
                } catch (_: Exception) {
                    snackbar.showSnackbar(exportDeliveryFailedMessage)
                } finally {
                    busy = false
                }
            }
        }
    }

    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.export_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (exportPacks.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.export_empty),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                stringResource(R.string.export_intro),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    ExportReadyCard(packs = exportPacks)
                }
            }
            item {
                MarketplaceLinterPanel(
                    packs = exportPacks,
                    onPacksChanged = { exportPacks = it },
                )
            }
            if (exportPacks.size > 1) {
                item {
                    Button(
                        onClick = {
                            pendingBundle = exportPacks
                            bundleDocument.launch(FileNaming.bundleName())
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.export_save_all_zips))
                    }
                }
            }
            if (exportPacks.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.export_destinations_header),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
            items(
                count = packList.size,
                key = packList::keyAt,
            ) { index ->
                val pack = packList.packAt(index)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(pack.marketplaceName, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    stringResource(R.string.export_pack_summary, pack.entries.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            ReadyPill()
                        }
                        Text(
                            stringResource(R.string.export_rules_source, pack.rulesVerifiedAt, pack.rulesSource),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    stringResource(R.string.export_manifest_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    stringResource(R.string.export_manifest_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        pack.entries.forEach { entry ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(entry.path, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(
                                            R.string.export_file_summary,
                                            entry.artifact.width,
                                            entry.artifact.height,
                                            entry.artifact.format.uppercase(),
                                            entry.artifact.quality?.let {
                                                stringResource(R.string.export_quality_percent, it)
                                            } ?: stringResource(R.string.export_quality_lossless),
                                            entry.artifact.bytes.size,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Text(
                                            stringResource(entry.artifact.processing.labelResource()),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        TextButton(
                                            enabled = !busy,
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            onClick = {
                                                scope.launch {
                                                    busy = true
                                                    try {
                                                        val uri = withContext(Dispatchers.IO) {
                                                            ExportDelivery.prepareShareUri(context, entry.artifact)
                                                        }
                                                        ExportDelivery.shareImage(
                                                            context,
                                                            entry.artifact,
                                                            uri,
                                                            exportShareTitle,
                                                        )
                                                    } catch (_: Exception) {
                                                        snackbar.showSnackbar(exportDeliveryFailedMessage)
                                                    } finally {
                                                        busy = false
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(stringResource(R.string.export_share))
                                        }
                                        TextButton(
                                            enabled = !busy,
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            onClick = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    scope.launch {
                                                        busy = true
                                                        try {
                                                            withContext(Dispatchers.IO) {
                                                                ExportDelivery.saveImageToGallery(context, entry.artifact)
                                                            }
                                                            snackbar.showSnackbar(exportSavedMessage)
                                                        } catch (_: Exception) {
                                                            snackbar.showSnackbar(
                                                                exportDeliveryFailedMessage,
                                                            )
                                                        } finally {
                                                            busy = false
                                                        }
                                                    }
                                                } else {
                                                    pendingImage = entry.artifact
                                                    when (entry.artifact.format) {
                                                        "jpg", "jpeg" -> imageDocument.launch(entry.artifact.fileName)
                                                        "png" -> pngDocument.launch(entry.artifact.fileName)
                                                        "webp" -> webpDocument.launch(entry.artifact.fileName)
                                                    }
                                                }
                                            },
                                        ) {
                                            Text(
                                                stringResource(
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        R.string.export_save_gallery
                                                    } else {
                                                        R.string.export_save_file
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                pendingPack = pack
                                zipDocument.launch(pack.zipFileName)
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.export_save_zip))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportReadyCard(packs: List<ExportPack>) {
    val imageCount = packs.sumOf { it.entries.size }
    val formats = packs.flatMap { pack -> pack.entries.map { it.artifact.format.uppercase() } }
        .distinct()
        .sorted()
        .joinToString(" · ")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReadyPill()
            Text(
                stringResource(R.string.export_ready_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.export_ready_summary, packs.size, imageCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.export_ready_formats, formats),
                style = MaterialTheme.typography.labelLarge,
                color = PlinthTheme.accentInk,
            )
        }
    }
}

@Composable
private fun ReadyPill() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50.dp),
    ) {
        Text(
            stringResource(R.string.export_ready_label),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun ImageProcessing.labelResource(): Int = when (this) {
    ImageProcessing.ORIGINAL -> R.string.export_provenance_original
    ImageProcessing.MANUAL_ADJUSTMENT -> R.string.export_provenance_manual
    ImageProcessing.GENERATED_BACKGROUND_COMPOSITE -> R.string.export_provenance_composite
    ImageProcessing.TRAINED_MODEL_ENHANCEMENT -> R.string.export_provenance_trained
    ImageProcessing.ALGORITHMIC_ENHANCEMENT -> R.string.export_provenance_algorithmic
}

internal class ExportPackListModel(private val packs: List<ExportPack>) {
    val size: Int
        get() = packs.size

    fun keyAt(index: Int): String = packs[index].marketplaceId

    fun packAt(index: Int): ExportPack = packs[index]
}
