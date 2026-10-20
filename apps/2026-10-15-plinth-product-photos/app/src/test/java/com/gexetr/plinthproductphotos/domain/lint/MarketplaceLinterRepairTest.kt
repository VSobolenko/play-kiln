package com.gexetr.plinthproductphotos.domain.lint

import com.gexetr.plinthproductphotos.domain.export.ExportArtifact
import com.gexetr.plinthproductphotos.domain.export.ExportPack
import com.gexetr.plinthproductphotos.domain.export.ExportPackEntry
import com.gexetr.plinthproductphotos.domain.export.ImageProcessing
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.ContrastThresholds
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.ImageRules
import com.gexetr.plinthproductphotos.domain.presets.ListingRules
import com.gexetr.plinthproductphotos.domain.presets.MainRules
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.domain.presets.PresetStatus
import com.gexetr.plinthproductphotos.domain.presets.TextRules
import com.gexetr.plinthproductphotos.ui.lint.LinterActionDisposition
import com.gexetr.plinthproductphotos.ui.lint.MarketplaceLinterAction
import com.gexetr.plinthproductphotos.ui.lint.MarketplaceLinterImageCodec
import java.nio.ByteBuffer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceLinterRepairTest {
    @Test
    fun l07IsReportedByTheActionOnlyWhenTheMeasuredBorderSurvivesTheArtifact() {
        val preset = preset().let { it.copy(main = it.main.copy(backgroundPolicy = "white-255")) }
        assertPixelRuleReachesAction("L07", preset)
    }

    @Test
    fun l08IsReportedByTheActionOnlyWhenTheMeasuredCoverageSurvivesTheArtifact() {
        val preset = preset().let { it.copy(main = it.main.copy(minFillRatio = 0.85)) }
        assertPixelRuleReachesAction("L08", preset)
    }

    @Test
    fun l09IsReportedByTheActionOnlyWhenTheMeasuredChromaSurvivesTheArtifact() {
        val preset = preset().let { it.copy(main = it.main.copy(backgroundPolicy = "neutral-only")) }
        assertPixelRuleReachesAction("L09", preset)
    }

    @Test
    fun extraRoleIsNotFabricatedAsMainAndCarriedOperationsAreStillChecked() {
        val codec = TestImageCodec()
        val original = artifact(codec, PixelBuffer.solid(3, 3, PixelBuffer.rgba(255, 0, 0)))
        val measured = original.copy(measuredFacts = original.measuredFacts.copy(
            role = SlotRole.EXTRA,
            borderMinimumChannelOutsideItem = 0,
            itemFillRatio = 0.1,
            operations = listOf("background-replace"),
        ))
        val preset = preset(
            editPolicy = EditPolicy.ORIGINAL_ONLY,
            originalOnlyAllowed = setOf("crop"),
        ).let { it.copy(main = it.main.copy(backgroundPolicy = "white-255", minFillRatio = 0.85)) }
        fun check(artifact: ExportArtifact) = MarketplaceLinterAction.checkAndRepair(
            listOf(pack(artifact)), mapOf(preset.id to preset), codec,
        )

        assertTrue(check(measured.copy(measuredFacts = measured.measuredFacts.copy(operations = emptyList()))).rows.isEmpty())
        val report = check(measured)
        assertEquals(listOf("L15"), report.rows.map { it.ruleId })
        assertEquals(SlotRole.EXTRA, report.packs.single().entries.single().artifact.measuredFacts.role)
        assertEquals(0, codec.encodeCalls)
        assertArrayEquals(original.bytes, report.packs.single().entries.single().artifact.bytes)
    }

    @Test
    fun byteFactsAreRefreshedWithoutLosingCarriedPackOrderAndRole() {
        val codec = TestImageCodec()
        val original = artifact(codec, PixelBuffer.solid(3, 3, PixelBuffer.rgba(255, 255, 255)))
        val order = listOf(PackOrderItem(SlotRole.MAIN), PackOrderItem(SlotRole.EXTRA, accessoryTagged = true))
        val measured = original.copy(measuredFacts = original.measuredFacts.copy(
            width = 1, height = 1, ratio = 1.0, bytes = Long.MAX_VALUE, format = "heic",
            role = SlotRole.EXTRA,
            packOrder = order,
        ))
        val preset = preset(minWidth = 3, maxBytes = 1_000)
        val report = MarketplaceLinterAction.checkAndRepair(
            listOf(pack(measured)), mapOf(preset.id to preset), codec,
        )
        assertTrue(report.foundNoFindings)
        assertTrue(report.rows.isEmpty())
        assertEquals(order, report.packs.single().entries.single().artifact.measuredFacts.packOrder)
        assertEquals(0, codec.encodeCalls)
    }

    private fun assertPixelRuleReachesAction(ruleId: String, preset: MarketplacePreset) {
        val codec = TestImageCodec()
        val image = PixelBuffer.solid(3, 3, PixelBuffer.rgba(255, 0, 0))
        val original = artifact(codec, image)
        val pixels = PixelFactsMeasurement.measure(image, Mask(ByteArray(9).also { it[4] = -1 }))
        val measured = original.copy(measuredFacts = original.measuredFacts.copy(
            borderMinimumChannelOutsideItem = pixels.borderMinimumChannelOutsideItem,
            itemFillRatio = pixels.itemFillRatio,
            backgroundMeanChroma = pixels.backgroundMeanChroma,
        ))
        fun check(artifact: ExportArtifact) = MarketplaceLinterAction.checkAndRepair(
            listOf(pack(artifact)), mapOf(preset.id to preset), codec,
        )

        assertFalse(check(original).rows.any { it.ruleId == ruleId })
        val report = check(measured)
        assertTrue(report.rows.any { it.ruleId == ruleId })
        assertFalse(report.foundNoFindings)
        assertArrayEquals(original.bytes, report.packs.single().entries.single().artifact.bytes)
        assertEquals(0, codec.encodeCalls)
    }

    @Test
    fun downscaleFixRepairsImageAndRelintsWithoutTheFinding() {
        val preset = preset(maxSide = 800)
        val image = PixelBuffer.solid(1000, 500, PixelBuffer.rgba(40, 80, 120))
        val facts = facts(width = 1000, height = 500)
        val finding = MarketplaceLinter.evaluate(facts, preset).single { it.ruleId == "L04" }

        val result = MarketplaceLinterRepair.apply(finding, facts, image, preset)

        assertTrue(result is LinterRepairResult.Applied)
        val applied = result as LinterRepairResult.Applied
        assertEquals(800, applied.image.width)
        assertEquals(400, applied.image.height)
        assertFalse(MarketplaceLinter.evaluate(applied.facts, preset).any { it.ruleId == "L04" })
    }

    @Test
    fun findingThatCannotBeEditedIsNeverReportedAsFixed() {
        val preset = preset(
            id = "vinted",
            editPolicy = EditPolicy.ORIGINAL_ONLY,
            originalOnlyAllowed = setOf("crop", "rotate", "exposure", "white-balance"),
        )
        val facts = facts(operations = listOf("background-replace"))
        val finding = MarketplaceLinter.evaluate(facts, preset).single { it.ruleId == "L15" }

        val result = MarketplaceLinterRepair.apply(
            finding = finding,
            facts = facts,
            image = PixelBuffer.solid(1000, 1000, PixelBuffer.rgba(20, 30, 40)),
            preset = preset,
        )

        assertTrue(result is LinterRepairResult.RequiresSellerAction)
        assertEquals(finding, (result as LinterRepairResult.RequiresSellerAction).finding)
    }

    @Test
    fun checkAndRepairEncodesARepairedArtifactThatPassesTheRule() {
        val codec = TestImageCodec()
        val preset = preset(maxSide = 800)
        val artifact = artifact(codec, PixelBuffer.solid(1000, 500, PixelBuffer.rgba(40, 80, 120)))

        val report = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(artifact)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        val repaired = report.packs.single().entries.single().artifact
        assertEquals(LinterActionDisposition.REPAIRED, report.rows.single().disposition)
        assertEquals("maxSide <= 800", report.rows.single().measuredLimit)
        assertEquals(800, repaired.width)
        assertTrue(report.packs.single().manifestJson.contains("\"width\":800"))
        val repairedBytes = codec.decode(repaired.bytes)
        assertEquals(repaired.width, repairedBytes.width)
        assertEquals(repaired.height, repairedBytes.height)
        assertFalse(
            MarketplaceLinter.evaluate(
                facts(repaired.width, repaired.height, repaired.format, repaired.bytes.size.toLong()),
                preset,
            ).any { it.ruleId == "L04" },
        )
        assertTrue(codec.encodeCalls > 0)
    }

    @Test
    fun l02RecompressEncodesAndRechecksTheCandidateByteCount() {
        val codec = SizeSensitiveImageCodec()
        val preset = preset(maxBytes = 550)
        val image = PixelBuffer.solid(10, 10, PixelBuffer.rgba(40, 80, 120))
        val original = artifactWithBytes(codec, image, quality = 90)
        assertTrue(original.bytes.size > 550)
        codec.encodeCalls = 0

        val report = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(original)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        val repaired = report.packs.single().entries.single().artifact
        assertEquals(LinterActionDisposition.REPAIRED, report.rows.single().disposition)
        assertTrue(codec.encodeCalls > 0)
        assertTrue(repaired.bytes.size <= 550)
        assertFalse(
            MarketplaceLinter.evaluate(
                facts(repaired.width, repaired.height, repaired.format, repaired.bytes.size.toLong()),
                preset,
            ).any { it.ruleId == "L02" },
        )
    }

    @Test
    fun l02DoesNotReportACompliantEncodedImage() {
        val codec = SizeSensitiveImageCodec()
        val preset = preset(maxBytes = 550)
        val artifact = artifactWithBytes(
            codec,
            PixelBuffer.solid(10, 10, PixelBuffer.rgba(40, 80, 120)),
            quality = 70,
        )
        assertTrue(artifact.bytes.size <= 550)

        val report = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(artifact)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        assertTrue(report.rows.isEmpty())
        assertTrue(report.foundNoFindings)
    }

    @Test
    fun sellerChangeShowsPackLimitAndPreservesOriginalArtifactBytes() {
        val codec = TestImageCodec()
        val preset = preset(maxCount = 1)
        val first = artifact(codec, PixelBuffer.solid(20, 20, PixelBuffer.rgba(40, 80, 120)))
        val second = artifact(codec, PixelBuffer.solid(20, 20, PixelBuffer.rgba(60, 90, 130)))
        val originals = listOf(first.bytes.copyOf(), second.bytes.copyOf())

        val report = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(first, second)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        assertEquals(LinterActionDisposition.SELLER_CHANGE, report.rows.single().disposition)
        assertEquals("items <= 1", report.rows.single().measuredLimit)
        report.packs.single().entries.forEachIndexed { index, entry ->
            assertArrayEquals(originals[index], entry.artifact.bytes)
        }
        assertEquals(0, codec.encodeCalls)
    }

    @Test
    fun creditRepairShowsItsCostWithoutRunningOrEncodingAPaidOperation() {
        val codec = TestImageCodec()
        val preset = preset(minWidth = 3000)
        val original = artifact(codec, PixelBuffer.solid(1000, 1000, PixelBuffer.rgba(40, 80, 120)))
        val bytes = original.bytes.copyOf()

        val report = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(original)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        assertEquals(LinterActionDisposition.CREDIT_COST, report.rows.single().disposition)
        assertEquals(2, report.rows.single().credits)
        assertArrayEquals(bytes, report.packs.single().entries.single().artifact.bytes)
        assertEquals(0, codec.encodeCalls)
    }

    @Test
    fun forbiddenEditIsReportedAndNeverSentToEncoder() {
        val codec = TestImageCodec()
        val preset = preset(
            id = "vinted",
            maxSide = 800,
            editPolicy = EditPolicy.ORIGINAL_ONLY,
            originalOnlyAllowed = setOf("crop", "rotate", "exposure", "white-balance"),
        )
        val original = artifact(codec, PixelBuffer.solid(1000, 500, PixelBuffer.rgba(20, 30, 40)))
        val bytes = original.bytes.copyOf()

        val report = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(original, marketplaceId = preset.id)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        assertEquals(LinterActionDisposition.CANNOT_EDIT, report.rows.single().disposition)
        assertEquals("maxSide <= 800", report.rows.single().measuredLimit)
        assertArrayEquals(bytes, report.packs.single().entries.single().artifact.bytes)
        assertEquals(0, codec.encodeCalls)
    }

    @Test
    fun checkingAnAlreadyRepairedPackIsIdempotentAndReportsClean() {
        val codec = TestImageCodec()
        val preset = preset(maxSide = 800)
        val original = artifact(codec, PixelBuffer.solid(1000, 500, PixelBuffer.rgba(40, 80, 120)))
        val first = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(original)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        val second = MarketplaceLinterAction.checkAndRepair(
            packs = first.packs,
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        assertTrue(second.rows.isEmpty())
        assertTrue(second.foundNoFindings)
    }

    @Test
    fun originalOnlyFormatRepairLeavesItemBytesByteIdentical() {
        val codec = TestImageCodec()
        val preset = preset(
            id = "vinted",
            formats = setOf("png"),
            editPolicy = EditPolicy.ORIGINAL_ONLY,
            originalOnlyAllowed = setOf("crop", "rotate", "exposure", "white-balance"),
        )
        val original = artifact(codec, PixelBuffer.solid(4, 4, PixelBuffer.rgba(20, 30, 40)))
        val bytes = original.bytes.copyOf()

        val report = MarketplaceLinterAction.checkAndRepair(
            packs = listOf(pack(original, marketplaceId = preset.id)),
            presets = mapOf(preset.id to preset),
            codec = codec,
        )

        assertArrayEquals(bytes, report.packs.single().entries.single().artifact.bytes)
        assertEquals(0, codec.encodeCalls)
    }

    private fun facts(
        width: Int = 1000,
        height: Int = 1000,
        format: String = "jpg",
        bytes: Long = 100_000,
        operations: List<String> = emptyList(),
    ) = ImageFacts(
        width = width,
        height = height,
        ratio = width.toDouble() / height,
        format = format,
        bytes = bytes,
        role = SlotRole.MAIN,
        operations = operations,
    )

    private fun preset(
        id: String = "market",
        maxSide: Int? = null,
        maxCount: Int? = null,
        minWidth: Int? = null,
        maxBytes: Long? = null,
        formats: Set<String> = setOf("jpg"),
        editPolicy: EditPolicy = EditPolicy.FREE,
        originalOnlyAllowed: Set<String> = emptySet(),
    ) = MarketplacePreset(
        id = id,
        name = id,
        regions = listOf("RU"),
        defaultLocale = "ru-RU",
        queue = 1,
        editPolicy = editPolicy,
        allowedOverlays = null,
        status = PresetStatus.FIRST_PARTY,
        verifiedAt = "2026-10-12",
        sourceUrl = "https://seller.example/rules",
        sources = emptyList(),
        image = ImageRules(
            formats = formats,
            maxBytes = maxBytes,
            minWidth = minWidth,
            maxSide = maxSide,
            maxCount = maxCount,
            raw = JsonObject(emptyMap()),
        ),
        main = MainRules(
            backgroundPolicy = null,
            minFillRatio = null,
            textOnMain = null,
            originalOnlyAllowed = originalOnlyAllowed,
            raw = JsonObject(emptyMap()),
        ),
        text = TextRules(
            prohibited = emptySet(),
            contrastMin = ContrastThresholds(4.5, 3.0),
            avoidColourPairs = emptyList(),
            raw = JsonObject(emptyMap()),
        ),
        listing = ListingRules(null, null, JsonObject(emptyMap())),
        notes = emptyList(),
        raw = JsonObject(emptyMap()),
    )

    private fun artifact(codec: TestImageCodec, image: PixelBuffer) = ExportArtifact(
        fileName = "item.jpg",
        width = image.width,
        height = image.height,
        format = "jpg",
        quality = 90,
        bytes = codec.encodePixels(image),
        provenance = Provenance.ORIGINAL_PIXELS,
        processing = ImageProcessing.ORIGINAL,
        digitalSourceTypeUri = null,
        xmpMetadata = "",
        hasAlpha = false,
        measuredFacts = facts(image.width, image.height),
    )

    private fun pack(
        vararg artifacts: ExportArtifact,
        marketplaceId: String = "market",
    ) = ExportPack(
        marketplaceId = marketplaceId,
        marketplaceName = marketplaceId,
        rulesVerifiedAt = "2026-10-12",
        rulesSource = "https://seller.example/rules",
        zipFileName = "$marketplaceId.zip",
        entries = artifacts.mapIndexed { index, artifact ->
            ExportPackEntry("item-$index.jpg", artifact)
        },
        manifestJson = JsonObject(
            mapOf(
                "files" to JsonArray(artifacts.mapIndexed { index, artifact ->
                    JsonObject(
                        mapOf(
                            "path" to JsonPrimitive("item-$index.jpg"),
                            "width" to JsonPrimitive(artifact.width),
                            "height" to JsonPrimitive(artifact.height),
                            "format" to JsonPrimitive(artifact.format),
                            "quality" to JsonPrimitive(requireNotNull(artifact.quality)),
                        ),
                    )
                }),
            ),
        ).toString(),
    )

    private class TestImageCodec : MarketplaceLinterImageCodec {
        var encodeCalls = 0

        override fun decode(bytes: ByteArray): PixelBuffer {
            val input = ByteBuffer.wrap(bytes)
            val width = input.int
            val height = input.int
            return PixelBuffer(width, height, IntArray(width * height) { input.int })
        }

        override fun exifGpsPresent(bytes: ByteArray) = false

        override fun encode(
            source: ExportArtifact,
            image: PixelBuffer,
            format: String,
            quality: Int,
        ): ExportArtifact {
            encodeCalls += 1
            return source.copy(
                width = image.width,
                height = image.height,
                format = format,
                quality = quality,
                bytes = encodePixels(image),
            )
        }

        fun encodePixels(image: PixelBuffer): ByteArray {
            val output = ByteBuffer.allocate(8 + image.pixels.size * Int.SIZE_BYTES)
            output.putInt(image.width)
            output.putInt(image.height)
            image.pixels.forEach { output.putInt(it) }
            return output.array()
        }
    }

    private fun artifactWithBytes(
        codec: SizeSensitiveImageCodec,
        image: PixelBuffer,
        quality: Int,
    ) = ExportArtifact(
        fileName = "item.jpg",
        width = image.width,
        height = image.height,
        format = "jpg",
        quality = quality,
        bytes = codec.encodePixels(image, quality),
        provenance = Provenance.ORIGINAL_PIXELS,
        processing = ImageProcessing.ORIGINAL,
        digitalSourceTypeUri = null,
        xmpMetadata = "",
        hasAlpha = false,
        measuredFacts = facts(image.width, image.height),
    )

    private class SizeSensitiveImageCodec : MarketplaceLinterImageCodec {
        var encodeCalls = 0

        override fun decode(bytes: ByteArray): PixelBuffer {
            val input = ByteBuffer.wrap(bytes)
            val width = input.int
            val height = input.int
            return PixelBuffer(width, height, IntArray(width * height) { input.int })
        }

        override fun exifGpsPresent(bytes: ByteArray) = false

        override fun encode(
            source: ExportArtifact,
            image: PixelBuffer,
            format: String,
            quality: Int,
        ): ExportArtifact {
            encodeCalls += 1
            return source.copy(
                width = image.width,
                height = image.height,
                format = format,
                quality = quality,
                bytes = encodePixels(image, quality),
            )
        }

        fun encodePixels(image: PixelBuffer, quality: Int): ByteArray {
            val output = ByteBuffer.allocate(8 + image.pixels.size * Int.SIZE_BYTES + quality * 2)
            output.putInt(image.width)
            output.putInt(image.height)
            image.pixels.forEach { output.putInt(it) }
            return output.array()
        }
    }
}
