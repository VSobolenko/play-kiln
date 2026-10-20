package com.gexetr.plinthproductphotos.ui.export

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.paging.PagingSource
import com.gexetr.plinthproductphotos.data.db.ItemEntity
import com.gexetr.plinthproductphotos.data.db.PlinthDatabase
import com.gexetr.plinthproductphotos.data.db.ProjectSummary
import com.gexetr.plinthproductphotos.data.export.AndroidRasterEncoder
import com.gexetr.plinthproductphotos.data.look.LookRepository
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels
import com.gexetr.plinthproductphotos.data.repository.LibraryRepository
import com.gexetr.plinthproductphotos.domain.export.ExportInput
import com.gexetr.plinthproductphotos.domain.export.ExportItemKey
import com.gexetr.plinthproductphotos.domain.export.ExportOptions
import com.gexetr.plinthproductphotos.domain.export.ExportPack
import com.gexetr.plinthproductphotos.domain.export.ImageProcessing
import com.gexetr.plinthproductphotos.domain.export.MarketplacePackExporter
import com.gexetr.plinthproductphotos.domain.export.RasterEncoder
import com.gexetr.plinthproductphotos.domain.lint.ImageFacts
import com.gexetr.plinthproductphotos.domain.lint.PixelFactsMeasurement
import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pipeline.Compositor
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import com.gexetr.plinthproductphotos.domain.wallet.WalletAccessPolicy
import com.gexetr.plinthproductphotos.ui.wallet.ServerUnlock
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ExportPackSourceState {
    data object Loading : ExportPackSourceState

    data class Empty(val reason: ExportPackEmptyReason) : ExportPackSourceState

    data class Ready(val packs: List<ExportPack>) : ExportPackSourceState

    data class Failed(val cause: Exception) : ExportPackSourceState
}

enum class ExportPackEmptyReason {
    NO_LIBRARY_ITEMS,
    NO_EXPORTABLE_ITEMS,
    NO_SAVED_LOOKS,
    NO_MARKETPLACE_PRESETS,
}

@Composable
fun rememberExportPackSource(refreshKey: Any? = Unit): State<ExportPackSourceState> {
    val context = LocalContext.current.applicationContext
    return produceState<ExportPackSourceState>(
        initialValue = ExportPackSourceState.Loading,
        context,
        refreshKey,
    ) {
        value = try {
            withContext(Dispatchers.IO) { loadExportPacks(context) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            ExportPackSourceState.Failed(failure)
        }
    }
}

private suspend fun loadExportPacks(context: Context): ExportPackSourceState {
    var step = "open database"
    try {
        val database = PlinthDatabase.get(context)
        step = "load saved Looks"
        val looks = LookRepository(context).all()
        step = "read marketplace presets"
        val presets = readMarketplacePresets(context)
        step = "read server unlock"
        val unlocked = ServerUnlock.fetch(context)
        step = "read library"
        val libraryItems = allLibraryItems(context, database)
        val items = libraryItems.filterNot { it.role == SlotRole.REFERENCE }

        emptyReason(libraryItems.size, items.size, looks.size, presets.size)?.let {
            return ExportPackSourceState.Empty(it)
        }

        step = "read Look background"
        val look = requireNotNull(looks.maxByOrNull(Look::createdAt))
        val sceneBackground = readSceneBackground(context, look)
        val packs = withContext(Dispatchers.Default) {
            prepareExportPacks(
                items = items,
                look = look,
                sceneBackground = sceneBackground,
                presets = presets,
                unlocked = unlocked,
                encoder = AndroidRasterEncoder(),
                loadSource = { item ->
                    val sourceFile = ownedFile(context, item.sourcePath)
                    require(sourceFile.isFile) { "Source photo is missing for item ${item.id}" }
                    PhotoPixels.load(sourceFile)
                },
                loadMask = { item, source -> readMask(context, item, source) },
                onStep = { step = it },
            )
        }
        return if (packs.isEmpty()) {
            ExportPackSourceState.Empty(ExportPackEmptyReason.NO_MARKETPLACE_PRESETS)
        } else {
            ExportPackSourceState.Ready(packs)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        Log.e(EXPORT_LOG_TAG, "Marketplace pack preparation failed: $step", failure)
        throw failure
    }
}

internal fun prepareExportPacks(
    items: List<ItemEntity>,
    look: Look,
    sceneBackground: PixelBuffer?,
    presets: List<MarketplacePreset>,
    encoder: RasterEncoder,
    unlocked: Boolean = false,
    loadSource: (ItemEntity) -> PixelBuffer,
    loadMask: (ItemEntity, PixelBuffer) -> Mask,
    onStep: (String) -> Unit = {},
): List<ExportPack> {
    if (presets.isEmpty()) return emptyList()
    val watermark = !WalletAccessPolicy.isUnlocked(unlocked)
    val exportable = items.filterNot { it.role == SlotRole.REFERENCE }
    val needsComposites = presets.any { it.editPolicy == EditPolicy.FREE }
    val needsOriginals = presets.any { it.editPolicy != EditPolicy.FREE }
    return MarketplacePackExporter.buildPrepared(
        items = exportable.map { item ->
            ExportItemKey(item.id, item.sku?.takeIf(String::isNotBlank) ?: item.id, item.role)
        },
        presets = presets,
        options = ExportOptions(quality = DEFAULT_QUALITY, watermark = watermark),
        encoder = encoder,
        prepare = { index ->
            val item = exportable[index]
            onStep("decode source item=${item.id}")
            val source = loadSource(item)
            onStep("read mask item=${item.id}")
            val mask = loadMask(item, source)
            val composite = if (needsComposites) {
                onStep("compose item=${item.id}")
                assembleExportInput(item, source, mask, look, sceneBackground)
            } else null
            val original = if (needsOriginals) {
                onStep("prepare original item=${item.id}")
                assembleExportInput(item, source, mask, look, sceneBackground, applyLook = false)
            } else null
            val inputForPreset: (MarketplacePreset) -> ExportInput = { preset ->
                onStep("render marketplace=${preset.id} item=${item.id}")
                requireNotNull(if (preset.editPolicy == EditPolicy.FREE) composite else original)
            }
            inputForPreset
        },
    )
}

private suspend fun allLibraryItems(context: Context, database: PlinthDatabase): List<ItemEntity> {
    val projectIds = allProjectSummaries(database).asSequence()
        .filter { it.itemCount > 0 }
        .map { it.project.id }
        .toList()
    val repository = LibraryRepository(context, database)
    val items = mutableListOf<ItemEntity>()
    for (projectId in projectIds) items += repository.itemsForProject(projectId)
    return items
}

private suspend fun allProjectSummaries(database: PlinthDatabase): List<ProjectSummary> {
    val source = database.projectDao().pageLibrary(
        search = null,
        category = null,
        marketplaceId = null,
        updatedAfter = null,
        updatedBefore = null,
    )
    val summaries = mutableListOf<ProjectSummary>()
    var firstPage = true
    var nextKey: Int? = null
    do {
        val params: PagingSource.LoadParams<Int> = if (firstPage) {
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = LIBRARY_PAGE_SIZE,
                placeholdersEnabled = false,
            )
        } else {
            PagingSource.LoadParams.Append(
                key = requireNotNull(nextKey),
                loadSize = LIBRARY_PAGE_SIZE,
                placeholdersEnabled = false,
            )
        }
        when (val result = source.load(params)) {
            is PagingSource.LoadResult.Page -> {
                summaries += result.data
                firstPage = false
                val previousKey = nextKey
                nextKey = result.nextKey
                check(nextKey == null || nextKey != previousKey) {
                    "Library paging did not advance to the next page"
                }
            }
            is PagingSource.LoadResult.Error -> throw result.throwable
            is PagingSource.LoadResult.Invalid -> throw IOException("Library changed while loading export items")
        }
    } while (nextKey != null)
    return summaries
}

private fun readMarketplacePresets(context: Context) = when (
    val parsed = MarketplacePresetParser.parseDocument(
        context.assets.open(MARKETPLACE_ASSET).bufferedReader().use { it.readText() },
    )
) {
    is PresetParseResult.Success -> parsed.document.marketplaces
    is PresetParseResult.Invalid -> throw IllegalStateException(
        "Bundled marketplace rules are invalid at ${parsed.error.path}: ${parsed.error.message}",
    )
}

private fun readMask(context: Context, item: ItemEntity, source: PixelBuffer): Mask {
    val path = item.maskPath ?: return Mask(ByteArray(source.pixels.size) { 0xff.toByte() })
    val file = ownedFile(context, path)
    require(file.isFile) { "Saved cutout mask is missing for item ${item.id}" }
    val bitmap = BitmapFactory.decodeFile(file.path)
        ?: throw IOException("Saved cutout mask could not be decoded for item ${item.id}")
    try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val alpha = ByteArray(pixels.size) { (pixels[it] ushr 24).toByte() }
        return Mask(
            if (bitmap.width == source.width && bitmap.height == source.height) {
                alpha
            } else {
                resizeMask(alpha, bitmap.width, bitmap.height, source.width, source.height)
            },
        )
    } finally {
        bitmap.recycle()
    }
}

internal fun resizeMask(
    source: ByteArray,
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
): ByteArray = ByteArray(targetWidth * targetHeight) { index ->
    val x = index % targetWidth
    val y = index / targetWidth
    val sourceX = ((x + 0.5) * sourceWidth / targetWidth).toInt().coerceIn(0, sourceWidth - 1)
    val sourceY = ((y + 0.5) * sourceHeight / targetHeight).toInt().coerceIn(0, sourceHeight - 1)
    source[sourceY * sourceWidth + sourceX]
}

internal fun emptyReason(
    libraryItemCount: Int,
    exportableItemCount: Int,
    savedLookCount: Int,
    marketplacePresetCount: Int,
): ExportPackEmptyReason? = when {
    libraryItemCount == 0 -> ExportPackEmptyReason.NO_LIBRARY_ITEMS
    exportableItemCount == 0 -> ExportPackEmptyReason.NO_EXPORTABLE_ITEMS
    savedLookCount == 0 -> ExportPackEmptyReason.NO_SAVED_LOOKS
    marketplacePresetCount == 0 -> ExportPackEmptyReason.NO_MARKETPLACE_PRESETS
    else -> null
}

private fun readSceneBackground(context: Context, look: Look): PixelBuffer? {
    if (look.background.kind == BackgroundSpec.Kind.SOLID ||
        look.background.kind == BackgroundSpec.Kind.GRADIENT
    ) return null
    val relativePath = requireNotNull(look.background.sceneAssetPath) {
        "Saved Look scene image is not available"
    }.removePrefix("files/")
    val scenesDirectory = File(context.filesDir, "scenes").canonicalFile
    val sceneFile = ownedFile(context, relativePath)
    require(sceneFile.toPath().startsWith(scenesDirectory.toPath())) {
        "Saved Look scene path is outside private scene storage"
    }
    require(sceneFile.isFile) { "Saved Look scene image is missing" }
    return PhotoPixels.load(sceneFile)
}

private fun ownedFile(context: Context, path: String): File {
    val filesRoot = context.filesDir.canonicalFile
    val candidate = File(path).let { if (it.isAbsolute) it else File(filesRoot, path) }.canonicalFile
    require(candidate.toPath().startsWith(filesRoot.toPath())) {
        "Export input path is outside private app storage"
    }
    return candidate
}

internal fun assembleExportInput(
    item: ItemEntity,
    source: PixelBuffer,
    mask: Mask,
    look: Look,
    sceneBackground: PixelBuffer?,
    canvasSize: Int = EXPORT_CANVAS_SIZE,
    applyLook: Boolean = true,
): ExportInput {
    val composition = if (applyLook) Compositor.compose(
        source = source,
        mask = mask,
        canvas = Canvas(canvasSize, canvasSize),
        look = look,
        sceneBackground = sceneBackground,
    ) else null
    val image = composition?.image ?: source
    val alignedMask = composition?.let {
        PixelFactsMeasurement.alignMask(source.width, source.height, mask, image, it.itemBounds)
    }
    val pixelFacts = alignedMask?.let { PixelFactsMeasurement.measure(image, it) }
    // Imported, ungrouped photos use zero; exported file names are one-based.
    require(item.photoOrder >= 0) { "Photo order must be non-negative for item ${item.id}" }
    val operations = if (applyLook) listOf("background-replace") else emptyList()
    return ExportInput(
        itemId = item.id,
        sku = item.sku?.takeIf(String::isNotBlank) ?: item.id,
        role = item.role,
        photoOrder = item.photoOrder.coerceAtLeast(1),
        image = image,
        itemMask = alignedMask,
        processing = if (applyLook) ImageProcessing.GENERATED_BACKGROUND_COMPOSITE else ImageProcessing.ORIGINAL,
        provenance = if (applyLook) Provenance.COMPOSITE_GENERATED_BACKGROUND else Provenance.ORIGINAL_PIXELS,
        operations = operations,
        measuredFacts = ImageFacts(
            width = image.width,
            height = image.height,
            ratio = image.width.toDouble() / image.height,
            format = "jpg",
            bytes = 0,
            role = item.role,
            itemFillRatio = pixelFacts?.itemFillRatio,
            backgroundMeanChroma = pixelFacts?.backgroundMeanChroma,
            borderMinimumChannelOutsideItem = pixelFacts?.borderMinimumChannelOutsideItem,
            operations = operations,
        ),
    )
}

private const val MARKETPLACE_ASSET = "marketplaces.json"
private const val EXPORT_LOG_TAG = "PlinthExport"
private const val DEFAULT_QUALITY = 90
private const val LIBRARY_PAGE_SIZE = 100
private const val EXPORT_CANVAS_SIZE = 1200
