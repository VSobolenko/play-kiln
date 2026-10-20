package com.gexetr.plinthproductphotos.ui.lint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.gexetr.plinthproductphotos.domain.export.ExportArtifact
import com.gexetr.plinthproductphotos.domain.export.ExportPack
import com.gexetr.plinthproductphotos.domain.export.ExportPackEntry
import com.gexetr.plinthproductphotos.domain.export.ImageProcessing
import com.gexetr.plinthproductphotos.domain.export.XmpWriter
import com.gexetr.plinthproductphotos.domain.lint.ImageFacts
import com.gexetr.plinthproductphotos.domain.lint.LinterFix
import com.gexetr.plinthproductphotos.domain.lint.LinterRepairResult
import com.gexetr.plinthproductphotos.domain.lint.MarketplaceLinter
import com.gexetr.plinthproductphotos.domain.lint.MarketplaceLinterRepair
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import java.io.ByteArrayInputStream
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class LinterActionDisposition {
    REPAIRED,
    SELLER_CHANGE,
    CREDIT_COST,
    MANUAL_ACTION,
    CANNOT_EDIT,
}

data class LinterActionRow(
    val marketplaceName: String,
    val ruleId: String,
    val measuredLimit: String,
    val disposition: LinterActionDisposition,
    val credits: Int? = null,
)

data class LinterActionReport(
    val packs: List<ExportPack>,
    val rows: List<LinterActionRow>,
    val foundNoFindings: Boolean,
    val presetUnavailable: Boolean,
    val imageUnreadable: Boolean = false,
    val repairFailed: Boolean = false,
)

interface MarketplaceLinterImageCodec {
    fun decode(bytes: ByteArray): PixelBuffer
    fun encode(source: ExportArtifact, image: PixelBuffer, format: String, quality: Int): ExportArtifact
    fun exifGpsPresent(bytes: ByteArray): Boolean
}

object MarketplaceLinterAction {
    fun checkAndRepair(context: Context, packs: List<ExportPack>): LinterActionReport {
        val document = context.assets.open("marketplaces.json").bufferedReader().use { it.readText() }
        val presets = when (val parsed = MarketplacePresetParser.parseDocument(document)) {
            is PresetParseResult.Success -> parsed.document.marketplaces.associateBy(MarketplacePreset::id)
            is PresetParseResult.Invalid -> return LinterActionReport(
                packs = packs,
                rows = emptyList(),
                foundNoFindings = false,
                presetUnavailable = true,
            )
        }
        return checkAndRepair(packs, presets, AndroidMarketplaceLinterImageCodec)
    }

    fun checkAndRepair(
        packs: List<ExportPack>,
        presets: Map<String, MarketplacePreset>,
        codec: MarketplaceLinterImageCodec,
    ): LinterActionReport {
        var presetUnavailable = false
        var anyFinding = false
        var imageUnreadable = false
        var repairFailed = false
        val rows = mutableListOf<LinterActionRow>()
        val updatedPacks = packs.map { pack ->
            val preset = presets[pack.marketplaceId]
            if (preset == null) {
                presetUnavailable = true
                return@map pack
            }
            val updatedEntries = pack.entries.mapIndexed { index, entry ->
                var artifact = entry.artifact
                var path = entry.path
                var image = try {
                    codec.decode(artifact.bytes)
                } catch (_: IllegalArgumentException) {
                    imageUnreadable = true
                    return@mapIndexed entry
                }
                val packItemCount = pack.entries.size.takeIf { index == 0 }
                var facts = facts(artifact, codec, packItemCount)
                val findings = MarketplaceLinter.evaluate(facts, preset)
                if (findings.isNotEmpty()) anyFinding = true
                findings.forEach { originalFinding ->
                    val finding = MarketplaceLinter.evaluate(facts, preset)
                        .firstOrNull { it.ruleId == originalFinding.ruleId }
                        ?: return@forEach
                    val recompressedCandidate = if (finding.ruleId == "L02") {
                        val recompress = LinterFix.forFinding(finding, preset) as? LinterFix.Recompress
                        recompress?.let { fix ->
                            try {
                                codec.encode(artifact, image, facts.format, fix.qualityFloor)
                            } catch (_: IllegalArgumentException) {
                                repairFailed = true
                                rows += row(
                                    pack,
                                    finding.ruleId,
                                    finding.violatedLimit,
                                    LinterActionDisposition.MANUAL_ACTION,
                                )
                                return@forEach
                            }
                        }
                    } else {
                        null
                    }
                    when (val repair = MarketplaceLinterRepair.apply(
                        finding = finding,
                        facts = facts,
                        image = image,
                        preset = preset,
                        recompressedByteCount = recompressedCandidate?.bytes?.size?.toLong(),
                    )) {
                        is LinterRepairResult.Applied -> {
                            val candidate = try {
                                (recompressedCandidate ?: codec.encode(
                                    source = artifact,
                                    image = repair.image,
                                    format = repair.format,
                                    quality = repair.quality ?: artifact.quality ?: 90,
                                )).let { encoded ->
                                    encoded.copy(fileName = replaceExtension(encoded.fileName, encoded.format))
                                }
                            } catch (_: IllegalArgumentException) {
                                repairFailed = true
                                rows += row(
                                    pack,
                                    finding.ruleId,
                                    finding.violatedLimit,
                                    LinterActionDisposition.MANUAL_ACTION,
                                )
                                return@forEach
                            }
                            val candidateFacts = facts(candidate, codec, packItemCount)
                            val stillPresent = MarketplaceLinter.evaluate(candidateFacts, preset)
                                .any { it.ruleId == finding.ruleId }
                            if (stillPresent) {
                                repairFailed = true
                                rows += row(pack, finding.ruleId, finding.violatedLimit, LinterActionDisposition.MANUAL_ACTION)
                            } else {
                                artifact = candidate.copy(measuredFacts = candidateFacts)
                                path = replaceExtension(path, candidate.format)
                                image = repair.image
                                facts = candidateFacts
                                rows += row(pack, finding.ruleId, finding.violatedLimit, LinterActionDisposition.REPAIRED)
                            }
                        }
                        is LinterRepairResult.RequiresSellerAction -> {
                            val disposition = disposition(finding.ruleId, repair)
                            rows += row(
                                pack,
                                finding.ruleId,
                                finding.violatedLimit,
                                disposition,
                                repair.creditsBeforeRun,
                            )
                        }
                    }
                }
                ExportPackEntry(path, artifact)
            }
            pack.copy(
                entries = updatedEntries,
                manifestJson = updateManifest(pack, updatedEntries),
            )
        }
        return LinterActionReport(
            packs = updatedPacks,
            rows = rows,
            foundNoFindings = !anyFinding && !presetUnavailable && !imageUnreadable,
            presetUnavailable = presetUnavailable,
            imageUnreadable = imageUnreadable,
            repairFailed = repairFailed,
        )
    }

    private fun disposition(ruleId: String, repair: LinterRepairResult.RequiresSellerAction) = when {
        repair.creditsBeforeRun != null -> LinterActionDisposition.CREDIT_COST
        repair.fix is LinterFix.CannotBeSatisfiedByEditing &&
            ruleId in setOf("L06", "L10", "L11", "L14", "L15", "L17", "L19") ->
            LinterActionDisposition.SELLER_CHANGE
        repair.fix is LinterFix.CannotBeSatisfiedByEditing -> LinterActionDisposition.CANNOT_EDIT
        repair.fix is LinterFix.EditSlideText ||
            repair.fix is LinterFix.RemoveForbiddenOverlay ||
            repair.fix is LinterFix.ReorderAccessorySecond ||
            repair.fix is LinterFix.TrimListingText -> LinterActionDisposition.SELLER_CHANGE
        repair.fix is LinterFix.Recompress -> LinterActionDisposition.MANUAL_ACTION
        else -> LinterActionDisposition.MANUAL_ACTION
    }

    private fun row(
        pack: ExportPack,
        ruleId: String,
        limit: String,
        disposition: LinterActionDisposition,
        credits: Int? = null,
    ) = LinterActionRow(pack.marketplaceName, ruleId, limit, disposition, credits)

    private fun updateManifest(pack: ExportPack, updatedEntries: List<ExportPackEntry>): String {
        val root = Json.parseToJsonElement(pack.manifestJson).jsonObject
        val entriesByOriginalPath = pack.entries.zip(updatedEntries).associate { (original, updated) ->
            original.path to updated
        }
        val files = root["files"]?.jsonArray
            ?: throw IllegalArgumentException("Export manifest is missing its files list")
        val updatedFiles = files.map { file ->
            val originalPath = file.jsonObject["path"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Export manifest file is missing its path")
            val updated = entriesByOriginalPath[originalPath]
                ?: throw IllegalArgumentException("Export manifest path does not match an exported image")
            val artifact = updated.artifact
            JsonObject(file.jsonObject.toMutableMap().apply {
                put("path", JsonPrimitive(updated.path))
                put("width", JsonPrimitive(artifact.width))
                put("height", JsonPrimitive(artifact.height))
                put("format", JsonPrimitive(artifact.format))
                if (artifact.quality == null) remove("quality")
                else put("quality", JsonPrimitive(artifact.quality))
            })
        }
        return JsonObject(root + ("files" to JsonArray(updatedFiles))).toString()
    }

    private fun replaceExtension(path: String, format: String): String {
        val extensionStart = path.lastIndexOf('.')
        val nameStart = path.lastIndexOf('/') + 1
        val base = if (extensionStart >= nameStart) path.substring(0, extensionStart) else path
        val extension = if (format.equals("jpeg", ignoreCase = true)) "jpg" else format.lowercase(Locale.ROOT)
        return "$base.$extension"
    }

    private fun facts(
        artifact: ExportArtifact,
        codec: MarketplaceLinterImageCodec,
        packItemCount: Int? = null,
    ) = artifact.measuredFacts.copy(
        width = artifact.width,
        height = artifact.height,
        ratio = artifact.width.toDouble() / artifact.height,
        format = artifact.format,
        bytes = artifact.bytes.size.toLong(),
        jpegQualityEstimate = artifact.quality,
        hasAlpha = artifact.hasAlpha,
        exifGpsPresent = codec.exifGpsPresent(artifact.bytes),
        packItemCount = packItemCount,
    )

    private object AndroidMarketplaceLinterImageCodec : MarketplaceLinterImageCodec {
        override fun decode(bytes: ByteArray): PixelBuffer {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalArgumentException("Exported image bytes could not be decoded")
            val width = bitmap.width
            val height = bitmap.height
            val argb = IntArray(width * height)
            bitmap.getPixels(argb, 0, width, 0, 0, width, height)
            bitmap.recycle()
            return PixelBuffer(width, height, IntArray(argb.size) { PixelBuffer.fromArgb(argb[it]) })
        }

        override fun encode(
            source: ExportArtifact,
            image: PixelBuffer,
            format: String,
            quality: Int,
        ): ExportArtifact {
            val argb = IntArray(image.pixels.size) { index ->
                val rgba = image.pixels[index]
                ((rgba and 0xff) shl 24) or
                    ((rgba ushr 24 and 0xff) shl 16) or
                    ((rgba ushr 16 and 0xff) shl 8) or
                    (rgba ushr 8)
            }
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(argb, 0, image.width, 0, 0, image.width, image.height)
            val output = java.io.ByteArrayOutputStream()
            val compressed = bitmap.compress(compressionFormat(format), quality.coerceIn(1, 100), output)
            bitmap.recycle()
            require(compressed) { "Android could not encode the repaired image" }
            val encoded = XmpWriter.embed(output.toByteArray(), format, source.xmpMetadata)
            val hasAlpha = format !in setOf("jpg", "jpeg") &&
                image.pixels.any { (it and 0xff) < 255 }
            return source.copy(
                width = image.width,
                height = image.height,
                format = format,
                quality = quality.takeIf { format !in setOf("png") },
                bytes = encoded,
                hasAlpha = hasAlpha,
            )
        }

        override fun exifGpsPresent(bytes: ByteArray): Boolean {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            return GPS_TAGS.any { exif.getAttribute(it) != null }
        }

        private fun compressionFormat(format: String): Bitmap.CompressFormat = when (format.lowercase()) {
            "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> throw IllegalArgumentException("Unsupported marketplace format: $format")
        }

        private val GPS_TAGS = listOf(
            "GPSVersionID",
            "GPSLatitudeRef",
            "GPSLatitude",
            "GPSLongitudeRef",
            "GPSLongitude",
            "GPSAltitudeRef",
            "GPSAltitude",
            "GPSTimeStamp",
            "GPSSatellites",
            "GPSStatus",
            "GPSMeasureMode",
            "GPSDOP",
            "GPSSpeedRef",
            "GPSSpeed",
            "GPSTrackRef",
            "GPSTrack",
            "GPSImgDirectionRef",
            "GPSImgDirection",
            "GPSMapDatum",
            "GPSDestLatitudeRef",
            "GPSDestLatitude",
            "GPSDestLongitudeRef",
            "GPSDestLongitude",
            "GPSDestBearingRef",
            "GPSDestBearing",
            "GPSDestDistanceRef",
            "GPSDestDistance",
            "GPSProcessingMethod",
            "GPSAreaInformation",
            "GPSDateStamp",
            "GPSDifferential",
        )
    }
}
