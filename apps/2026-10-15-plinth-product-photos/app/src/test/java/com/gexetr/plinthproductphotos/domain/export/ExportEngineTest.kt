package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.domain.lint.ImageFacts
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.ImageRules
import com.gexetr.plinthproductphotos.domain.presets.ListingRules
import com.gexetr.plinthproductphotos.domain.presets.MainRules
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.PresetStatus
import com.gexetr.plinthproductphotos.domain.presets.TextRules
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ExportEngineTest {
    @Test
    fun reusingAnEncodingStillChecksTheNextMarketplacesEditingPolicy() {
        val cache = ExportEngine.RenderCache()
        var encodes = 0
        val encoder = RasterEncoder { _, _, _ ->
            encodes++
            byteArrayOf(-1, -40, -1, -39)
        }
        val photo = input("photo", "SKU", ImageProcessing.GENERATED_BACKGROUND_COMPOSITE)
            .copy(operations = listOf("background-replace"))
        ExportEngine.render(photo, preset("free"), ExportOptions(), encoder, cache)
        val rejected = assertThrows(ExportRejectedException::class.java) {
            ExportEngine.render(
                photo,
                preset("original").copy(editPolicy = EditPolicy.ORIGINAL_ONLY),
                ExportOptions(),
                encoder,
                cache,
            )
        }

        assertEquals(1, encodes)
        assertTrue(rejected.findings.any { it.first == "L15" })
    }

    @Test
    fun cachedFactsAreNotReusedForADifferentMaskOnTheSameImage() {
        val cache = ExportEngine.RenderCache()
        val photo = input("photo", "SKU")
        val full = photo.copy(itemMask = Mask(ByteArray(photo.image.pixels.size) { -1 }))
        val empty = photo.copy(itemMask = Mask(ByteArray(photo.image.pixels.size)))
        val first = ExportEngine.render(full, preset("one"), ExportOptions(), FakeEncoder(), cache)
        val second = ExportEngine.render(empty, preset("two"), ExportOptions(), FakeEncoder(), cache)

        assertEquals(1.0, requireNotNull(first.measuredFacts.itemFillRatio), 0.0)
        assertEquals(0.0, requireNotNull(second.measuredFacts.itemFillRatio), 0.0)
    }

    @Test
    fun cachedRasterStillHonorsAStricterMarketplacesByteLimit() {
        val cache = ExportEngine.RenderCache()
        val photo = input("photo", "SKU")
        val loose = ExportEngine.render(photo, preset("loose"), ExportOptions(), SizeEncoder(), cache)
        val strict = ExportEngine.render(
            photo, preset("strict", maxBytes = 2_000), ExportOptions(), SizeEncoder(), cache,
        )

        assertTrue(strict.bytes.size <= 2_000)
        assertTrue(strict.width < loose.width)
        assertEquals(2, cache.rasters.size)
    }

    @Test
    fun artifactCarriesTheFactsUsedByTheLinterWithoutChangingEncodedOutput() {
        val original = input("item-1", "SKU").copy(role = SlotRole.EXTRA)
        val measured = original.copy(measuredFacts = original.measuredFacts.copy(
            itemFillRatio = 0.25,
            backgroundMeanChroma = 2.0,
            borderMinimumChannelOutsideItem = 255,
        ))
        val options = ExportOptions(format = "jpg")
        val baseline = ExportEngine.render(original, preset("market"), options, FakeEncoder())
        val result = ExportEngine.render(measured, preset("market"), options, FakeEncoder())

        assertArrayEquals(baseline.bytes, result.bytes)
        assertEquals(baseline.fileName, result.fileName)
        assertEquals(SlotRole.EXTRA, result.measuredFacts.role)
        assertEquals(0.25, requireNotNull(result.measuredFacts.itemFillRatio), 0.0)
        assertEquals(2.0, requireNotNull(result.measuredFacts.backgroundMeanChroma), 0.0)
        assertEquals(255, result.measuredFacts.borderMinimumChannelOutsideItem)
        assertEquals(result.bytes.size.toLong(), result.measuredFacts.bytes)
        assertEquals(result.width, result.measuredFacts.width)
    }

    @Test
    fun factsAreMeasuredAgainAfterTheEncoderCanvasAddsPadding() {
        val image = PixelBuffer.solid(2, 2, PixelBuffer.rgba(255, 255, 255))
        val original = input("item-1", "SKU").copy(image = image)
        val measured = original.copy(itemMask = Mask(ByteArray(4) { -1 }))
        val preset = preset("market", recommended = 2 to 4)
        val baselineEncoder = FakeEncoder()
        val measuredEncoder = FakeEncoder()
        val baseline = ExportEngine.render(original, preset, ExportOptions(), baselineEncoder)
        val result = ExportEngine.render(measured, preset, ExportOptions(), measuredEncoder)

        assertArrayEquals(baseline.bytes, result.bytes)
        assertArrayEquals(requireNotNull(baselineEncoder.image).pixels, requireNotNull(measuredEncoder.image).pixels)
        assertEquals(baselineEncoder.quality, measuredEncoder.quality)
        assertEquals(baseline.fileName, result.fileName)
        assertEquals(baseline.format, result.format)
        assertEquals(0.5, requireNotNull(result.measuredFacts.itemFillRatio), 0.000001)
        assertEquals(255, result.measuredFacts.borderMinimumChannelOutsideItem)
        assertEquals(0.0, requireNotNull(result.measuredFacts.backgroundMeanChroma), 0.001)
    }

    @Test
    fun namesFilesFromSkuPatternAndSanitizesIllegalCharacters() {
        assertEquals("A-014_main_1.jpg", FileNaming.name("A-014", SlotRole.MAIN, 1, "jpg"))
        assertEquals("A-014_extra_2.png", FileNaming.name("A:014? ", SlotRole.EXTRA, 2, "png"))
    }

    @Test
    fun duplicateItemSkusGetDistinctNamesAcrossMarketplacePacks() {
        val items = listOf(
            input("item-1", "SKU/1"),
            input("item-2", "SKU/1"),
            input("reference", "RULER").copy(role = SlotRole.REFERENCE),
        )
        val packs = MarketplacePackExporter.build(
            items = items,
            presets = listOf(preset("wb"), preset("ebay")),
            options = ExportOptions(format = "jpg", quality = 90),
            encoder = FakeEncoder(),
        )

        assertEquals(listOf("wb", "ebay"), packs.map { it.marketplaceId })
        assertEquals(2, packs.size)
        packs.forEach { pack ->
            assertEquals(2, pack.entries.map { it.path }.distinct().size)
            assertTrue(pack.entries.any { "SKU-1" in it.path })
            assertTrue(pack.entries.any { "SKU-1-2" in it.path })
        }
    }

    @Test
    fun overridesAreIncludedOnlyForTheirMarketplaceAndOnlyWithTheirMeasuredValue() {
        val facts = ImageFacts(
            width = 80,
            height = 60,
            ratio = 80.0 / 60.0,
            format = "jpg",
            bytes = 0,
            detectedTextBlocks = listOf(
                com.gexetr.plinthproductphotos.domain.lint.DetectedTextBlock("SALE", 4, 0.1, true),
            ),
        )
        val photo = input("item-1", "A-014").copy(
            measuredFacts = facts,
            overrides = listOf(ExportRuleOverride("L10", "4 characters; heightShare=0.1", "ebay")),
        )
        val packs = MarketplacePackExporter.build(
            items = listOf(photo),
            presets = listOf(
                preset("ebay", textOnMain = "forbidden"),
                preset("etsy"),
            ),
            options = ExportOptions(format = "jpg"),
            encoder = FakeEncoder(),
        )

        val ebayFile = kotlinx.serialization.json.Json.parseToJsonElement(packs[0].manifestJson)
            .jsonObject["files"]!!.jsonArray.single().jsonObject
        val etsyFile = kotlinx.serialization.json.Json.parseToJsonElement(packs[1].manifestJson)
            .jsonObject["files"]!!.jsonArray.single().jsonObject
        assertEquals(1, ebayFile["overrides"]!!.jsonArray.size)
        assertEquals(0, etsyFile["overrides"]!!.jsonArray.size)
        assertEquals(
            "4 characters; heightShare=0.1",
            ebayFile["overrides"]!!.jsonArray.single().jsonObject["measuredValue"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun presetDimensionsFormatAndJpegQualityFloorAreApplied() {
        val encoder = FakeEncoder()
        val result = ExportEngine.render(
            input = input("item-1", "A-014"),
            preset = preset(
                id = "wb",
                formats = setOf("jpg", "png"),
                minJpegQuality = 82,
                recommended = 120 to 120,
            ),
            options = ExportOptions(format = "jpg", quality = 70),
            encoder = encoder,
        )

        assertEquals(120, result.width)
        assertEquals(120, result.height)
        assertEquals("jpg", result.format)
        assertEquals(82, result.quality)
        assertEquals(82, encoder.quality)
    }

    @Test
    fun unsupportedRequestedFormatFallsBackToAllowedFormatAndEmbedsOneXmpPacket() {
        val result = ExportEngine.render(
            input = input("item-1", "A-014"),
            preset = preset("etsy", formats = setOf("png")),
            options = ExportOptions(format = "jpg", quality = 88),
            encoder = FakeEncoder(),
        )
        val encoded = String(result.bytes, Charsets.ISO_8859_1)

        assertEquals("png", result.format)
        assertNull(result.quality)
        assertEquals(1, encoded.split("XML:com" + ".adobe.xmp").size - 1)
    }

    @Test
    fun outputIsDownscaledUntilItsEncodedBytesFitThePresetLimit() {
        val result = ExportEngine.render(
            input = input("item-1", "A-014"),
            preset = preset("ebay", maxBytes = 2_000),
            options = ExportOptions(format = "jpg", quality = 90),
            encoder = SizeEncoder(),
        )

        assertTrue(result.width < 120)
        assertTrue(result.height < 90)
        assertTrue(result.bytes.size <= 2_000)
    }

    @Test
    fun digitalSourceTypeIsSelectedByTheActualProcessingKind() {
        val expected = mapOf(
            ImageProcessing.ORIGINAL to null,
            ImageProcessing.MANUAL_ADJUSTMENT to null,
            ImageProcessing.GENERATED_BACKGROUND_COMPOSITE to
                "http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia",
            ImageProcessing.TRAINED_MODEL_ENHANCEMENT to
                "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia",
            ImageProcessing.ALGORITHMIC_ENHANCEMENT to
                "http://cv.iptc.org/newscodes/digitalsourcetype/algorithmicallyEnhanced",
        )

        expected.forEach { (processing, uri) ->
            val result = ExportEngine.render(
                input("item-1", "A-014", processing = processing),
                preset("ebay"),
                ExportOptions(format = "jpg", quality = 90),
                FakeEncoder(),
            )
            assertEquals(uri, result.digitalSourceTypeUri)
            if (uri == null) {
                assertFalse(result.xmpMetadata.contains("DigitalSourceType"))
            } else {
                assertTrue(result.xmpMetadata.contains(uri))
            }
            assertFalse(result.xmpMetadata.contains("C2PA", ignoreCase = true))
        }
    }

    @Test
    fun manifestContainsOnlyExplicitRuleOverridesWithMeasuredValues() {
        val manifest = ExportManifest.encode(
            preset = preset("ebay"),
            files = listOf(
                ManifestFile(
                    path = "A-014_main_1.jpg",
                    provenance = Provenance.ENHANCED,
                    processing = ImageProcessing.TRAINED_MODEL_ENHANCEMENT,
                    overrides = listOf(ExportRuleOverride("L05", "ratio=0.82")),
                ),
            ),
        )
        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(manifest)
            .jsonObject["files"]!!.jsonArray.single().jsonObject

        assertEquals(
            "L05",
            parsed["overrides"]!!.jsonArray.single().jsonObject["ruleId"]!!.jsonPrimitive.content,
        )
        assertEquals(
            "ratio=0.82",
            parsed["overrides"]!!.jsonArray.single().jsonObject["measuredValue"]!!.jsonPrimitive.content,
        )
        assertEquals(1, parsed["overrides"]!!.jsonArray.size)
        assertFalse(parsed.containsKey("quality"))
    }

    private fun input(
        itemId: String,
        sku: String,
        processing: ImageProcessing = ImageProcessing.ORIGINAL,
    ) = ExportInput(
        itemId = itemId,
        sku = sku,
        role = SlotRole.MAIN,
        photoOrder = 1,
        image = PixelBuffer.solid(80, 60, PixelBuffer.rgba(20, 30, 40)),
        processing = processing,
        provenance = if (processing == ImageProcessing.ORIGINAL) Provenance.ORIGINAL_PIXELS else Provenance.ENHANCED,
        measuredFacts = ImageFacts(width = 80, height = 60, ratio = 80.0 / 60.0, format = "jpg", bytes = 0),
    )

    private fun preset(
        id: String,
        formats: Set<String> = setOf("jpg", "png"),
        minJpegQuality: Int? = null,
        recommended: Pair<Int, Int> = 120 to 90,
        textOnMain: String? = null,
        maxBytes: Long? = null,
    ) = MarketplacePreset(
        id = id,
        name = id,
        regions = listOf("US"),
        defaultLocale = "en-US",
        queue = 1,
        editPolicy = EditPolicy.FREE,
        allowedOverlays = null,
        status = PresetStatus.FIRST_PARTY,
        verifiedAt = "2026-10-14",
        sourceUrl = "https://example.org/rules",
        sources = emptyList(),
        image = ImageRules(
            formats = formats,
            maxBytes = maxBytes,
            minJpegQuality = minJpegQuality,
            recommended = com.gexetr.plinthproductphotos.domain.presets.RecommendedDimensions(
                width = recommended.first,
                height = recommended.second,
                basis = "test",
            ),
            raw = JsonObject(emptyMap()),
        ),
        main = MainRules(
            backgroundPolicy = "any",
            minFillRatio = null,
            textOnMain = textOnMain,
            originalOnlyAllowed = emptySet(),
            raw = JsonObject(emptyMap()),
        ),
        text = TextRules(emptySet(), null, emptyList(), JsonObject(emptyMap())),
        listing = ListingRules(null, null, JsonObject(emptyMap())),
        notes = emptyList(),
        raw = JsonObject(emptyMap()),
    )

    private class FakeEncoder : RasterEncoder {
        var quality: Int? = null
        var image: PixelBuffer? = null

        override fun encode(image: PixelBuffer, format: String, quality: Int): ByteArray {
            this.quality = quality
            this.image = image
            return when (format) {
                "jpg" -> byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte())
                "png" -> png(image.width, image.height)
                else -> byteArrayOf(1, 2, 3, 4)
            }
        }

        private fun png(width: Int, height: Int): ByteArray {
            val signature = byteArrayOf(
                0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
            )
            val header = byteArrayOf(
                (width ushr 24).toByte(), (width ushr 16).toByte(), (width ushr 8).toByte(), width.toByte(),
                (height ushr 24).toByte(), (height ushr 16).toByte(), (height ushr 8).toByte(), height.toByte(),
                8, 6, 0, 0, 0,
            )
            val data = byteArrayOf(0, 0, 0, 0)
            return signature + chunk("IHDR", header) + chunk("IDAT", data) + chunk("IEND", byteArrayOf())
        }

        private fun chunk(type: String, data: ByteArray): ByteArray {
            val typeBytes = type.toByteArray(Charsets.US_ASCII)
            val crc = java.util.zip.CRC32().apply {
                update(typeBytes)
                update(data)
            }.value.toInt()
            val length = data.size
            return byteArrayOf(
                (length ushr 24).toByte(), (length ushr 16).toByte(),
                (length ushr 8).toByte(), length.toByte(),
            ) + typeBytes + data + byteArrayOf(
                (crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte(),
            )
        }
    }

    private class SizeEncoder : RasterEncoder {
        override fun encode(image: PixelBuffer, format: String, quality: Int): ByteArray {
            val payloadSize = (image.width * image.height / 2).coerceIn(1, 60_000)
            val length = payloadSize + 2
            return byteArrayOf(
                0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xef.toByte(),
                (length ushr 8).toByte(), length.toByte(),
            ) + ByteArray(payloadSize) + byteArrayOf(0xff.toByte(), 0xd9.toByte())
        }
    }
}
