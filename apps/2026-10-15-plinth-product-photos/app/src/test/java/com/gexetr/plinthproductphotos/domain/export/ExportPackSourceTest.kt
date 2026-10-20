package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.data.db.ItemEntity
import com.gexetr.plinthproductphotos.data.look.looksWithDefault
import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.ItemState
import com.gexetr.plinthproductphotos.domain.model.Look
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import com.gexetr.plinthproductphotos.ui.export.ExportPackEmptyReason
import com.gexetr.plinthproductphotos.ui.export.assembleExportInput
import com.gexetr.plinthproductphotos.ui.export.emptyReason
import com.gexetr.plinthproductphotos.ui.export.prepareExportPacks
import com.gexetr.plinthproductphotos.ui.export.resizeMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExportPackSourceTest {
    @Test
    fun largeUnmaskedImportWithDefaultLookGetsAnExportablePhotoOrder() {
        val source = PixelBuffer.solid(2344, 2344, PixelBuffer.rgba(40, 80, 120))
        val input = assembleExportInput(
            item = importedItem("large"),
            source = source,
            mask = Mask(ByteArray(source.pixels.size) { -1 }),
            look = looksWithDefault(emptyList(), "Plinth").single(),
            sceneBackground = null,
        )

        assertEquals(1, input.photoOrder)
        assertEquals(1200, input.image.width)
        assertEquals(1200, input.image.height)
        assertEquals(0.64, requireNotNull(input.measuredFacts.itemFillRatio), 0.000001)
        assertEquals(PixelBuffer.rgba(40, 80, 120), input.image[600, 600])
    }

    @Test
    fun freshUnmaskedImportsBuildEveryBundledMarketplacePackWithoutBypassingEditingPolicy() {
        val items = listOf(importedItem("jpeg"), importedItem("large"))
        val presets = (MarketplacePresetParser.parseDocument(
            File("src/main/assets/marketplaces.json").readText(),
        ) as PresetParseResult.Success).document.marketplaces
        val decoded = mutableListOf<String>()
        val encodedSizes = mutableListOf<Pair<Int, Int>>()
        val targets = listOf(
            900 to 1200, 900 to 1200, 1920 to 1440, 900 to 1200,
            1600 to 1600, 2000 to 2000, 1080 to 1080, 2000 to 2000, 2048 to 2048,
        )
        val packs = prepareExportPacks(
            items = items,
            look = looksWithDefault(emptyList(), "Plinth").single(),
            sceneBackground = null,
            presets = presets,
            encoder = RasterEncoder { image, format, _ ->
                assertEquals("jpg", format)
                assertTrue((image.width to image.height) in targets)
                encodedSizes += image.width to image.height
                byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte())
            },
            loadSource = { item ->
                if (decoded.isNotEmpty()) {
                    assertTrue("Finish encoding a source before decoding the next photo", encodedSizes.isNotEmpty())
                }
                decoded += item.id
                val size = if (item.id == "large") 2344 else 64
                PixelBuffer.solid(size, size, PixelBuffer.rgba(40, 80, 120))
            },
            loadMask = { item, source ->
                assertEquals(null, item.maskPath)
                Mask(ByteArray(source.pixels.size) { -1 })
            },
        )

        assertEquals(listOf("jpeg", "large"), decoded)
        assertEquals(
            List(2) {
                listOf(
                    900 to 1200, 1920 to 1440, 900 to 1200, 1600 to 1600,
                    2000 to 2000, 1080 to 1080, 2048 to 2048,
                )
            }.flatten(),
            encodedSizes,
        )
        assertEquals(presets.map { it.id }, packs.map { it.marketplaceId })
        assertEquals(9, packs.size)
        packs.forEach { pack ->
            assertEquals(2, pack.entries.size)
            assertEquals(2, pack.entries.map { it.path }.distinct().size)
            val originalOnly = presets.single { it.id == pack.marketplaceId }.editPolicy == EditPolicy.ORIGINAL_ONLY
            val target = targets[presets.indexOfFirst { it.id == pack.marketplaceId }]
            pack.entries.forEach { entry ->
                assertEquals(target, entry.artifact.width to entry.artifact.height)
                assertTrue(entry.path.endsWith("_main_1.jpg"))
                assertTrue(entry.artifact.bytes.size > 4)
                assertEquals(
                    if (originalOnly) ImageProcessing.ORIGINAL else ImageProcessing.GENERATED_BACKGROUND_COMPOSITE,
                    entry.artifact.processing,
                )
                if (originalOnly) {
                    assertEquals(Provenance.ORIGINAL_PIXELS, entry.artifact.provenance)
                    assertTrue(entry.artifact.measuredFacts.operations.isEmpty())
                    assertEquals(null, entry.artifact.digitalSourceTypeUri)
                } else {
                    assertEquals(listOf("background-replace"), entry.artifact.measuredFacts.operations)
                }
                assertFalse(entry.artifact.measuredFacts.width == 0)
            }
        }
    }

    @Test
    fun originalOnlyPackUsesUncompositedPixelsEvenWhenLookChangesTheBackground() {
        val presets = (MarketplacePresetParser.parseDocument(
            File("src/main/assets/marketplaces.json").readText(),
        ) as PresetParseResult.Success).document.marketplaces
            .filter { it.id == "wb" || it.id == "vinted" }
        val original = PixelBuffer.rgba(40, 80, 120)
        val encoded = mutableListOf<PixelBuffer>()
        prepareExportPacks(
            items = listOf(importedItem("photo")),
            look = looksWithDefault(emptyList(), "Plinth").single(),
            sceneBackground = null,
            presets = presets,
            encoder = RasterEncoder { image, _, _ ->
                encoded += image
                byteArrayOf(-1, -40, -1, -39)
            },
            loadSource = { PixelBuffer.solid(32, 24, original) },
            loadMask = { _, source -> Mask(ByteArray(source.pixels.size) { -1 }) },
        )

        assertEquals(2, encoded.size)
        assertEquals(PixelBuffer.rgba(255, 255, 255), encoded[0][0, 0])
        assertEquals(original, encoded[1][0, 0])
        assertEquals(original, encoded[1][450, 600])
    }

    @Test
    fun emptyReasonDistinguishesAnEmptyLibraryFromMissingExportInputs() {
        assertEquals(ExportPackEmptyReason.NO_LIBRARY_ITEMS, emptyReason(0, 0, 0, 0))
        assertEquals(ExportPackEmptyReason.NO_EXPORTABLE_ITEMS, emptyReason(4, 0, 1, 3))
        assertEquals(ExportPackEmptyReason.NO_SAVED_LOOKS, emptyReason(4, 3, 0, 3))
        assertEquals(ExportPackEmptyReason.NO_MARKETPLACE_PRESETS, emptyReason(4, 3, 1, 0))
        assertEquals(null, emptyReason(4, 3, 1, 3))
    }

    @Test
    fun resizesSavedMaskToSourceDimensions() {
        val resized = resizeMask(
            source = byteArrayOf(1, 2, 3, 4),
            sourceWidth = 2,
            sourceHeight = 2,
            targetWidth = 4,
            targetHeight = 4,
        )

        assertEquals(16, resized.size)
        assertEquals(1.toByte(), resized[0])
        assertEquals(2.toByte(), resized[3])
        assertEquals(3.toByte(), resized[12])
        assertEquals(4.toByte(), resized[15])
    }

    @Test
    fun assemblesExportInputFromLibraryItemSourceMaskAndSavedLook() {
        val item = ItemEntity(
            id = "photo-7",
            projectId = "project-1",
            sourcePath = "sources/photo.png",
            sku = null,
            groupId = null,
            role = SlotRole.EXTRA,
            category = null,
            categoryConfirmed = false,
            attributesJson = null,
            scaleHintJson = null,
            scaleHintConfirmed = false,
            suggestedSku = null,
            skuConfirmed = false,
            photoOrder = 3,
            marketplaceId = null,
            maskPath = "masks/photo-7-1.png",
            maskRevision = 1,
            state = ItemState.CUT,
            lastError = null,
        )
        val background = PixelBuffer.fromArgb(0xffcc3311.toInt())
        val input = assembleExportInput(
            item = item,
            source = PixelBuffer.solid(2, 2, PixelBuffer.fromArgb(0xff22aa44.toInt())),
            mask = Mask(ByteArray(4) { 0xff.toByte() }),
            look = Look(
                id = "look-1",
                name = "Warm",
                background = BackgroundSpec(
                    kind = BackgroundSpec.Kind.SOLID,
                    colorArgb = 0xffcc3311.toInt(),
                    color2Argb = null,
                    angleDeg = null,
                    sceneId = null,
                    sceneAssetPath = null,
                    prompt = null,
                    seed = null,
                ),
                shadow = ShadowSpec(kind = ShadowSpec.Kind.NONE),
                marginPct = 0.25f,
                fillTarget = 0.5f,
                createdAt = 10L,
            ),
            sceneBackground = null,
            canvasSize = 16,
        )

        assertEquals(item.id, input.itemId)
        assertEquals(item.id, input.sku)
        assertEquals(item.role, input.role)
        assertEquals(item.photoOrder, input.photoOrder)
        assertEquals(16, input.image.width)
        assertEquals(16, input.image.height)
        assertEquals(background, input.image[0, 0])
        assertEquals(Provenance.COMPOSITE_GENERATED_BACKGROUND, input.provenance)
        assertEquals(ImageProcessing.GENERATED_BACKGROUND_COMPOSITE, input.processing)
        assertEquals(0.25, requireNotNull(input.measuredFacts.itemFillRatio), 0.000001)
        assertEquals(17, input.measuredFacts.borderMinimumChannelOutsideItem)
        assertTrue(requireNotNull(input.measuredFacts.backgroundMeanChroma) > 12.0)
        assertEquals(listOf("background-replace"), input.operations)
        assertEquals(input.operations, input.measuredFacts.operations)
    }

    private fun importedItem(id: String) = ItemEntity(
        id = id,
        projectId = id,
        sourcePath = "sources/$id.png",
        sku = null,
        groupId = null,
        role = SlotRole.MAIN,
        category = null,
        categoryConfirmed = false,
        attributesJson = null,
        scaleHintJson = null,
        scaleHintConfirmed = false,
        suggestedSku = null,
        skuConfirmed = false,
        photoOrder = 0,
        marketplaceId = null,
        maskPath = null,
        maskRevision = 0,
        state = ItemState.IMPORTED,
        lastError = null,
    )
}
