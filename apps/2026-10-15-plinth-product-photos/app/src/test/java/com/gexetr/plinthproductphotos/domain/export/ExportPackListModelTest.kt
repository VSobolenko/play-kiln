package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.ui.export.ExportPackListModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportPackListModelTest {
    @Test
    fun packRowsAreFetchedOnlyWhenTheLazyListRequestsThem() {
        val packs = CountingPacks(9)
        val model = ExportPackListModel(packs)

        assertEquals(0, packs.readCount)
        assertEquals(9, model.size)
        assertEquals(0, packs.readCount)
        assertEquals("marketplace-3", model.keyAt(3))
        assertEquals(1, packs.readCount)
        assertEquals("marketplace-7", model.packAt(7).marketplaceId)
        assertEquals(2, packs.readCount)
    }

    private class CountingPacks(override val size: Int) : AbstractList<ExportPack>() {
        var readCount = 0
            private set

        override fun get(index: Int): ExportPack {
            readCount++
            return ExportPack(
                marketplaceId = "marketplace-$index",
                marketplaceName = "Marketplace $index",
                rulesVerifiedAt = "2026-10-16",
                rulesSource = "https://example.com",
                zipFileName = "marketplace-$index.zip",
                entries = emptyList(),
                manifestJson = "{}",
            )
        }
    }
}
