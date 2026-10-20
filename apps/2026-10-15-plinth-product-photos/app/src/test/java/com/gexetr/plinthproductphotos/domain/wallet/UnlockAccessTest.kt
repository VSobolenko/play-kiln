package com.gexetr.plinthproductphotos.domain.wallet

import com.gexetr.plinthproductphotos.data.db.ItemEntity
import com.gexetr.plinthproductphotos.data.look.looksWithDefault
import com.gexetr.plinthproductphotos.domain.export.RasterEncoder
import com.gexetr.plinthproductphotos.domain.export.Watermark
import com.gexetr.plinthproductphotos.domain.model.ItemState
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import com.gexetr.plinthproductphotos.ui.export.prepareExportPacks
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockAccessTest {
    @Test
    fun selectionLimitsApplyToFreeAndUnlockedImports() {
        assertEquals(5, WalletAccessPolicy.selectionLimit(false))
        assertEquals(50, WalletAccessPolicy.selectionLimit(true))
        val items = (1..7).toList()
        assertEquals((1..5).toList(), WalletAccessPolicy.clampSelection(items, false))
        assertEquals(items, WalletAccessPolicy.clampSelection(items, true))
        assertTrue(WalletAccessPolicy.selectionExceedsLimit(6, false))
        assertFalse(WalletAccessPolicy.selectionExceedsLimit(6, true))
    }

    @Test
    fun freeExportIncludesEveryPresetAndWatermarksOnlyTheWatermarkBand() {
        val free = render(unlocked = false)
        val unlocked = render(unlocked = true)

        assertEquals(presets().map { it.id }, free.packs.map { it.marketplaceId })
        assertEquals(free.packs.map { it.marketplaceId }, unlocked.packs.map { it.marketplaceId })
        assertTrue(free.packs.any { pack -> preset(pack.marketplaceId).editPolicy == EditPolicy.ORIGINAL_ONLY })
        assertEquals(9, free.packs.size)
        free.images.zip(unlocked.images).forEach { (watermarked, plain) ->
            val band = Watermark.bandFor(plain.width, plain.height)
            var changedInside = false
            plain.pixels.indices.forEach { index ->
                val x = index % plain.width
                val y = index / plain.width
                val changed = plain.pixels[index] != watermarked.pixels[index]
                if (x in band.left until band.right && y in band.top until band.bottom) changedInside = changedInside || changed
                else assertFalse("free export changed outside watermark band at ($x, $y)", changed)
            }
            assertTrue("free export must be watermarked", changedInside)
            assertTrue(
                "free export must pass the unwatermarked render through Watermark.apply",
                Watermark.apply(plain).pixels.contentEquals(watermarked.pixels),
            )
        }
    }

    @Test
    fun unlockedExportIncludesEveryPresetWithoutApplyingAWatermark() {
        val free = render(unlocked = false)
        val unlocked = render(unlocked = true)

        assertEquals(presets().map { it.id }, unlocked.packs.map { it.marketplaceId })
        assertEquals(9, unlocked.packs.size)
        free.images.zip(unlocked.images).forEach { (watermarked, plain) ->
            val band = Watermark.bandFor(plain.width, plain.height)
            plain.pixels.indices.forEach { index ->
                val x = index % plain.width
                val y = index / plain.width
                if (x !in band.left until band.right || y !in band.top until band.bottom) {
                    assertEquals("unlock must not affect pixels outside the watermark band", plain.pixels[index], watermarked.pixels[index])
                }
            }
        }
    }

    private fun render(unlocked: Boolean): RenderedPacks {
        val images = mutableListOf<PixelBuffer>()
        val packs = prepareExportPacks(
            items = listOf(importedItem("photo")),
            look = looksWithDefault(emptyList(), "Plinth").single(),
            sceneBackground = null,
            presets = presets(),
            encoder = RasterEncoder { image, _, _ ->
                images += image
                byteArrayOf(-1, -40, -1, -39)
            },
            unlocked = unlocked,
            loadSource = { PixelBuffer.solid(64, 64, PixelBuffer.rgba(40, 80, 120)) },
            loadMask = { _, source -> Mask(ByteArray(source.pixels.size) { -1 }) },
        )
        return RenderedPacks(packs, images)
    }

    private fun presets() = (MarketplacePresetParser.parseDocument(
        File("src/main/assets/marketplaces.json").readText(),
    ) as PresetParseResult.Success).document.marketplaces

    private fun preset(id: String) = presets().single { it.id == id }

    private fun importedItem(id: String) = ItemEntity(
        id = id, projectId = id, sourcePath = "sources/$id.png", sku = null, groupId = null,
        role = SlotRole.MAIN, category = null, categoryConfirmed = false, attributesJson = null,
        scaleHintJson = null, scaleHintConfirmed = false, suggestedSku = null, skuConfirmed = false,
        photoOrder = 0, marketplaceId = null, maskPath = null, maskRevision = 0,
        state = ItemState.IMPORTED, lastError = null,
    )

    private data class RenderedPacks(
        val packs: List<com.gexetr.plinthproductphotos.domain.export.ExportPack>,
        val images: List<PixelBuffer>,
    )
}
