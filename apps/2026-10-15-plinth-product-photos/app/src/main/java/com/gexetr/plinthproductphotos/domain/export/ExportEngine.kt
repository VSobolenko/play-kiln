package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.domain.lint.ImageFacts
import com.gexetr.plinthproductphotos.domain.lint.FindingSeverity
import com.gexetr.plinthproductphotos.domain.lint.MarketplaceLinter
import com.gexetr.plinthproductphotos.domain.lint.PackOrderItem
import com.gexetr.plinthproductphotos.domain.lint.PixelFactsMeasurement
import com.gexetr.plinthproductphotos.domain.lint.PixelFacts
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.PresetStatus
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

enum class ImageProcessing {
    ORIGINAL,
    MANUAL_ADJUSTMENT,
    GENERATED_BACKGROUND_COMPOSITE,
    TRAINED_MODEL_ENHANCEMENT,
    ALGORITHMIC_ENHANCEMENT,
}

data class ExportRuleOverride(
    val ruleId: String,
    val measuredValue: String,
    val marketplaceId: String? = null,
) {
    init {
        require(ruleId.isNotBlank() && measuredValue.isNotBlank())
    }
}

data class ExportInput(
    val itemId: String,
    val sku: String,
    val role: SlotRole,
    val photoOrder: Int,
    val image: PixelBuffer,
    val processing: ImageProcessing,
    val provenance: Provenance,
    val measuredFacts: ImageFacts,
    val operations: List<String> = emptyList(),
    val appliedOverlays: List<String> = emptyList(),
    val overrides: List<ExportRuleOverride> = emptyList(),
    val itemMask: Mask? = null,
) {
    init {
        require(itemId.isNotBlank() && sku.isNotBlank())
        require(photoOrder > 0)
    }
}

data class ExportOptions(
    val format: String? = null,
    val quality: Int = 90,
    val longSide: Int? = null,
    // Watermarked unless the caller proves the server-confirmed unlock; the safe value is the default.
    val watermark: Boolean = true,
) {
    init {
        require(quality in 1..100)
        require(longSide == null || longSide > 0)
    }
}

fun interface RasterEncoder {
    fun encode(image: PixelBuffer, format: String, quality: Int): ByteArray
}

data class ExportArtifact(
    val fileName: String,
    val width: Int,
    val height: Int,
    val format: String,
    val quality: Int?,
    val bytes: ByteArray,
    val provenance: Provenance,
    val processing: ImageProcessing,
    val digitalSourceTypeUri: String?,
    val xmpMetadata: String,
    val hasAlpha: Boolean,
    val measuredFacts: ImageFacts,
)

object ExportEngine {
    private val supportedFormats = setOf("jpg", "jpeg", "png", "webp")

    // Scoped to one source photo; retain encoded bytes/facts, never fitted pixel buffers.
    internal class RenderCache {
        val rasters = mutableMapOf<RasterKey, EncodedRaster>()
    }

    internal class RasterKey(
        val image: PixelBuffer,
        val mask: ByteArray?,
        val width: Int,
        val height: Int,
        val format: String,
        val quality: Int,
        val sourceType: String?,
        val watermark: Boolean,
    ) {
        override fun equals(other: Any?): Boolean =
            other is RasterKey && image === other.image && mask === other.mask &&
                width == other.width && height == other.height && format == other.format &&
                quality == other.quality && sourceType == other.sourceType && watermark == other.watermark

        override fun hashCode(): Int {
            var result = System.identityHashCode(image)
            result = 31 * result + System.identityHashCode(mask)
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + format.hashCode()
            result = 31 * result + quality
            result = 31 * result + (sourceType?.hashCode() ?: 0)
            return 31 * result + watermark.hashCode()
        }
    }

    internal data class EncodedRaster(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
        val hasAlpha: Boolean,
        val pixelFacts: PixelFacts?,
    )

    fun render(
        input: ExportInput,
        preset: MarketplacePreset,
        options: ExportOptions,
        encoder: RasterEncoder,
    ): ExportArtifact = render(input, preset, options, encoder, null)

    internal fun render(
        input: ExportInput,
        preset: MarketplacePreset,
        options: ExportOptions,
        encoder: RasterEncoder,
        cache: RenderCache?,
    ): ExportArtifact {
        require(input.role != SlotRole.REFERENCE) { "Reference photos must never be exported" }
        val format = chooseFormat(preset, options.format)
        var dimensions = targetDimensions(input.image, preset, options)
        var quality = if (format == "jpg" || format == "jpeg") {
            max(options.quality, preset.image.minJpegQuality ?: 1)
        } else {
            options.quality
        }
        require(quality in 1..100) { "Preset JPEG quality must be between 1 and 100" }
        val sourceType = digitalSourceType(input.processing)
        val xmp = XmpWriter.packet(
            creatorTool = CREATOR_TOOL,
            digitalSourceTypeUri = sourceType,
        )
        fun encodeAtSize(): EncodedRaster {
            val key = RasterKey(input.image, input.itemMask?.alpha, dimensions.first, dimensions.second, format, quality, sourceType, options.watermark)
            cache?.rasters?.get(key)?.let { return it }
            val fitted = fitCanvas(input.image, dimensions.first, dimensions.second)
            val outputImage = if (format == "jpg" || format == "jpeg") flattenAlpha(fitted) else fitted
            val encodedImage = if (options.watermark) Watermark.apply(outputImage) else outputImage
            val encoded = encoder.encode(encodedImage, format, quality)
            val raster = EncodedRaster(
                bytes = XmpWriter.embed(encoded, format, xmp),
                width = outputImage.width,
                height = outputImage.height,
                hasAlpha = format != "jpg" && format != "jpeg" && containsAlpha(outputImage),
                pixelFacts = input.itemMask?.let { mask ->
                    PixelFactsMeasurement.measure(
                        outputImage,
                        fitExportMask(mask, input.image.width, input.image.height, outputImage.width, outputImage.height),
                    )
                },
            )
            return raster
        }
        var raster = encodeAtSize()
        val maxBytes = preset.image.maxBytes
        if (maxBytes != null && raster.bytes.size.toLong() > maxBytes) {
            val jpegFloor = preset.image.minJpegQuality ?: 70
            if ((format == "jpg" || format == "jpeg") && quality > jpegFloor) {
                quality = jpegFloor
                raster = encodeAtSize()
            }
            while (raster.bytes.size.toLong() > maxBytes) {
                val nextWidth = (dimensions.first * 0.95).roundToInt().coerceAtLeast(1)
                val nextHeight = (dimensions.second * 0.95).roundToInt().coerceAtLeast(1)
                val minimumWidth = preset.image.minWidth ?: 1
                val minimumHeight = preset.image.minHeight ?: 1
                val minimumLongSide = preset.image.minLongSide ?: 1
                if (nextWidth < minimumWidth || nextHeight < minimumHeight ||
                    max(nextWidth, nextHeight) < minimumLongSide ||
                    nextWidth == dimensions.first && nextHeight == dimensions.second
                ) break
                dimensions = nextWidth to nextHeight
                raster = encodeAtSize()
            }
        }
        val pixelFacts = raster.pixelFacts
        val measuredFacts = if (pixelFacts == null) input.measuredFacts else input.measuredFacts.copy(
            itemFillRatio = pixelFacts.itemFillRatio,
            backgroundMeanChroma = pixelFacts.backgroundMeanChroma,
            borderMinimumChannelOutsideItem = pixelFacts.borderMinimumChannelOutsideItem,
        )
        val facts = measuredFacts.copy(
            width = raster.width,
            height = raster.height,
            ratio = raster.width.toDouble() / raster.height,
            format = format,
            bytes = raster.bytes.size.toLong(),
            role = input.role,
            jpegQualityEstimate = quality.takeIf { format == "jpg" || format == "jpeg" },
            hasAlpha = raster.hasAlpha,
            exifGpsPresent = input.measuredFacts.exifGpsPresent,
            operations = input.operations,
            appliedOverlays = input.appliedOverlays,
        )
        val findings = MarketplaceLinter.evaluate(facts, preset)
        val errors = findings.filter { it.severity == FindingSeverity.ERROR }
        require(input.overrides.all { it.marketplaceId == null || it.marketplaceId == preset.id }) {
            "An override is scoped to a different marketplace"
        }
        val overridePairs = input.overrides.map { it.ruleId to it.measuredValue }
        require(overridePairs.distinct().size == overridePairs.size) { "Duplicate rule overrides are not allowed" }
        val errorPairs = errors.map { it.ruleId to it.measuredValue }.toSet()
        require(overridePairs.all { it in errorPairs }) {
            "An override must match a measured ERROR finding and its measured value"
        }
        val unoverridden = errors.filter { (it.ruleId to it.measuredValue) !in overridePairs }
        if (unoverridden.isNotEmpty()) {
            throw ExportRejectedException(unoverridden.map { it.ruleId to it.measuredValue })
        }
        cache?.rasters?.put(
            RasterKey(input.image, input.itemMask?.alpha, dimensions.first, dimensions.second, format, quality, sourceType, options.watermark),
            raster,
        )
        return ExportArtifact(
            fileName = FileNaming.name(input.sku, input.role, input.photoOrder, format),
            width = raster.width,
            height = raster.height,
            format = format,
            quality = quality.takeIf { format != "png" },
            bytes = raster.bytes,
            provenance = input.provenance,
            processing = input.processing,
            digitalSourceTypeUri = sourceType,
            xmpMetadata = xmp,
            hasAlpha = raster.hasAlpha,
            measuredFacts = facts,
        )
    }

    private fun chooseFormat(preset: MarketplacePreset, preferred: String?): String {
        val allowed = preset.image.formats
            .map { it.lowercase() }
            .filter { it in supportedFormats }
        require(allowed.isNotEmpty()) { "Preset ${preset.id} has no supported raster format" }
        val requested = preferred?.lowercase()?.let { if (it == "jpeg") "jpg" else it }
        return requested?.takeIf { it in allowed }
            ?: listOf("jpg", "png", "webp").firstOrNull { candidate ->
                allowed.any { if (candidate == "jpg") it in setOf("jpg", "jpeg") else it == candidate }
            }?.let { if (it == "jpg" && allowed.contains("jpeg") && !allowed.contains("jpg")) "jpeg" else it }
            ?: allowed.first()
            .let { if (it == "jpeg") "jpg" else it }
    }

    private fun targetDimensions(
        source: PixelBuffer,
        preset: MarketplacePreset,
        options: ExportOptions,
    ): Pair<Int, Int> {
        val image = preset.image
        val aspect = image.aspect?.let(::parseAspect)
        val recommended = image.recommended
        var width: Int
        var height: Int
        if (options.longSide != null) {
            val ratio = aspect ?: recommended?.let { it.width.toDouble() / it.height }
                ?: source.width.toDouble() / source.height
            width = (options.longSide * min(ratio, 1.0)).roundToInt().coerceAtLeast(1)
            height = (options.longSide / max(ratio, 1.0)).roundToInt().coerceAtLeast(1)
        } else if (aspect != null) {
            val longSide = recommended?.let { max(it.width, it.height) }
                ?: image.maxLongSide
                ?: image.minLongSide
                ?: max(source.width, source.height)
            width = (longSide * min(aspect, 1.0)).roundToInt().coerceAtLeast(1)
            height = (longSide / max(aspect, 1.0)).roundToInt().coerceAtLeast(1)
        } else if (recommended != null) {
            width = recommended.width
            height = recommended.height
        } else {
            width = source.width
            height = source.height
        }

        val minScale = maxOf(
            image.minWidth?.toDouble()?.div(width) ?: 0.0,
            image.minHeight?.toDouble()?.div(height) ?: 0.0,
            image.minLongSide?.toDouble()?.div(max(width, height)) ?: 0.0,
            1.0,
        )
        val maxScale = minOf(
            image.maxSide?.toDouble()?.div(max(width, height)) ?: Double.POSITIVE_INFINITY,
            image.maxLongSide?.toDouble()?.div(max(width, height)) ?: Double.POSITIVE_INFINITY,
            image.maxSidesSum?.toDouble()?.div(width + height) ?: Double.POSITIVE_INFINITY,
        )
        require(minScale <= maxScale) { "Preset ${preset.id} has incompatible minimum and maximum dimensions" }
        val scale = if (minScale > 1.0) minScale else min(maxScale, 1.0)
        width = (width * scale).roundToInt().coerceAtLeast(1)
        height = (height * scale).roundToInt().coerceAtLeast(1)
        return width to height
    }

    private fun parseAspect(value: String): Double {
        val parts = value.split(':')
        require(parts.size == 2)
        val width = parts[0].toDoubleOrNull()
        val height = parts[1].toDoubleOrNull()
        require(width != null && height != null && width > 0 && height > 0) {
            "Invalid preset aspect: $value"
        }
        return width / height
    }

    private fun fitCanvas(source: PixelBuffer, width: Int, height: Int): PixelBuffer {
        val scale = min(width.toDouble() / source.width, height.toDouble() / source.height)
        val scaledWidth = (source.width * scale).roundToInt().coerceIn(1, width)
        val scaledHeight = (source.height * scale).roundToInt().coerceIn(1, height)
        if (source.width == width && source.height == height) return source
        val scaled = source.scale(scaledWidth, scaledHeight)
        if (scaledWidth == width && scaledHeight == height) return scaled
        val background = source[0, 0]
        val pixels = IntArray(width * height) { background }
        val offsetX = (width - scaledWidth) / 2
        val offsetY = (height - scaledHeight) / 2
        for (y in 0 until scaledHeight) {
            scaled.pixels.copyInto(
                destination = pixels,
                destinationOffset = (offsetY + y) * width + offsetX,
                startIndex = y * scaledWidth,
                endIndex = (y + 1) * scaledWidth,
            )
        }
        return PixelBuffer(width, height, pixels)
    }

    private fun containsAlpha(image: PixelBuffer): Boolean = image.pixels.any { (it and 0xff) != 255 }

    private fun flattenAlpha(image: PixelBuffer): PixelBuffer {
        if (!containsAlpha(image)) return image
        val pixels = IntArray(image.pixels.size) { index ->
            val pixel = image.pixels[index]
            val alpha = pixel and 0xff
            if (alpha == 255) {
                pixel
            } else {
                val inverseAlpha = 255 - alpha
                val red = (((pixel ushr 24) and 0xff) * alpha + 255 * inverseAlpha + 127) / 255
                val green = (((pixel ushr 16) and 0xff) * alpha + 255 * inverseAlpha + 127) / 255
                val blue = (((pixel ushr 8) and 0xff) * alpha + 255 * inverseAlpha + 127) / 255
                PixelBuffer.rgba(red, green, blue)
            }
        }
        return PixelBuffer(image.width, image.height, pixels)
    }

    private fun digitalSourceType(processing: ImageProcessing): String? = when (processing) {
        ImageProcessing.ORIGINAL, ImageProcessing.MANUAL_ADJUSTMENT -> null
        ImageProcessing.GENERATED_BACKGROUND_COMPOSITE ->
            "http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia"
        ImageProcessing.TRAINED_MODEL_ENHANCEMENT ->
            "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"
        ImageProcessing.ALGORITHMIC_ENHANCEMENT ->
            "http://cv.iptc.org/newscodes/digitalsourcetype/algorithmicallyEnhanced"
    }

    private const val CREATOR_TOOL = "Plinth AI: Product Photos 0.1.0"
}

class ExportRejectedException(val findings: List<Pair<String, String>>) :
    IllegalStateException("Export has unoverridden rule errors: ${findings.joinToString { it.first }}")

data class ManifestFile(
    val path: String,
    val provenance: Provenance,
    val processing: ImageProcessing,
    val overrides: List<ExportRuleOverride>,
    val width: Int = 0,
    val height: Int = 0,
    val format: String = "",
    val quality: Int? = null,
    val digitalSourceTypeUri: String? = null,
)

object ExportManifest {
    fun encode(preset: MarketplacePreset, files: List<ManifestFile>): String {
        files.forEach { file ->
            require(file.overrides.all { it.marketplaceId == null || it.marketplaceId == preset.id }) {
                "A manifest override is scoped to a different marketplace"
            }
            val pairs = file.overrides.map { it.ruleId to it.measuredValue }
            require(pairs.distinct().size == pairs.size) { "Duplicate manifest overrides are not allowed" }
        }
        return Json { prettyPrint = true }.encodeToString(
            buildJsonObject {
                put("schemaVersion", 1)
                put("marketplaceId", preset.id)
                put("marketplaceName", preset.name)
                put("rulesVerifiedAt", preset.verifiedAt)
                put("rulesSource", preset.sourceUrl)
                putJsonArray("files") {
                    files.forEach { file ->
                        addJsonObject {
                            put("path", file.path)
                            put("width", file.width)
                            put("height", file.height)
                            put("format", file.format)
                            file.quality?.let { put("quality", it) }
                            put("provenance", file.provenance.name)
                            put("processing", file.processing.name)
                            file.digitalSourceTypeUri?.let { put("digitalSourceTypeUri", it) }
                            putJsonArray("overrides") {
                                file.overrides.forEach { override ->
                                    addJsonObject {
                                        put("ruleId", override.ruleId)
                                        put("measuredValue", override.measuredValue)
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

data class ExportPackEntry(val path: String, val artifact: ExportArtifact)

data class ExportPack(
    val marketplaceId: String,
    val marketplaceName: String,
    val rulesVerifiedAt: String,
    val rulesSource: String,
    val zipFileName: String,
    val entries: List<ExportPackEntry>,
    val manifestJson: String,
)

internal data class ExportItemKey(val itemId: String, val sku: String, val role: SlotRole)

object MarketplacePackExporter {
    fun build(
        items: List<ExportInput>,
        presets: List<MarketplacePreset>,
        options: ExportOptions,
        encoder: RasterEncoder,
    ): List<ExportPack> {
        val exportable = items.filter { it.role != SlotRole.REFERENCE }
        return buildPrepared(
            items = exportable.map { ExportItemKey(it.itemId, it.sku, it.role) },
            presets = presets,
            options = options,
            encoder = encoder,
            prepare = { index -> { exportable[index] } },
        )
    }

    internal fun buildPrepared(
        items: List<ExportItemKey>,
        presets: List<MarketplacePreset>,
        options: ExportOptions,
        encoder: RasterEncoder,
        prepare: (Int) -> (MarketplacePreset) -> ExportInput,
    ): List<ExportPack> {
        require(presets.isNotEmpty()) { "Select at least one marketplace" }
        require(presets.map { it.id }.distinct().size == presets.size) { "Marketplace presets must be unique" }
        require(presets.all { it.status != PresetStatus.UNREAD }) { "Unread marketplace rules cannot be exported" }
        val packNames = presets.map { FileNaming.packName(it.id).lowercase(Locale.ROOT) }
        require(packNames.distinct().size == packNames.size) {
            "Marketplace IDs produce colliding ZIP names"
        }
        require(items.isNotEmpty()) { "There are no exportable photos" }
        require(items.none { it.role == SlotRole.REFERENCE }) { "Reference photos must never be exported" }
        val skus = FileNaming.uniqueSkus(items.map { it.itemId to it.sku })
        val order = items.map { PackOrderItem(it.role) }
        val packEntries = presets.map { mutableListOf<ExportPackEntry>() }
        val manifests = presets.map { mutableListOf<ManifestFile>() }
        items.forEachIndexed { itemIndex, item ->
            val inputForPreset = prepare(itemIndex)
            val cache = ExportEngine.RenderCache()
            presets.forEachIndexed { presetIndex, preset ->
                val input = inputForPreset(preset)
                require(input.itemId == item.itemId && input.sku == item.sku && input.role == item.role) {
                    "Prepared export input does not match its item"
                }
                val namedInput = input.copy(
                    sku = skus.getValue(input.itemId),
                    overrides = input.overrides.filter {
                        it.marketplaceId == null || it.marketplaceId == preset.id
                    },
                    measuredFacts = input.measuredFacts.copy(
                        packItemCount = items.size,
                        packOrder = order,
                    ),
                )
                val artifact = ExportEngine.render(namedInput, preset, options, encoder, cache)
                val path = "${skus.getValue(input.itemId)}/${artifact.fileName}"
                manifests[presetIndex] += ManifestFile(
                    path = path,
                    provenance = artifact.provenance,
                    processing = artifact.processing,
                    overrides = namedInput.overrides,
                    width = artifact.width,
                    height = artifact.height,
                    format = artifact.format,
                    quality = artifact.quality,
                    digitalSourceTypeUri = artifact.digitalSourceTypeUri,
                )
                packEntries[presetIndex] += ExportPackEntry(path, artifact)
            }
        }
        return presets.mapIndexed { index, preset ->
            val entries = packEntries[index]
            require(entries.map { it.path.lowercase() }.distinct().size == entries.size) {
                "Export names collide within ${preset.id}"
            }
            val manifest = ExportManifest.encode(
                preset = preset,
                files = manifests[index],
            )
            ExportPack(
                marketplaceId = preset.id,
                marketplaceName = preset.name,
                rulesVerifiedAt = preset.verifiedAt,
                rulesSource = preset.sourceUrl,
                zipFileName = FileNaming.packName(preset.id),
                entries = entries,
                manifestJson = manifest,
            )
        }
    }
}
