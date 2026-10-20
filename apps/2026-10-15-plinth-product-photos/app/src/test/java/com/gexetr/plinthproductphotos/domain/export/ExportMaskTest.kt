package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.domain.lint.PixelFactsMeasurement
import com.gexetr.plinthproductphotos.domain.model.Mask
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ExportMaskTest {
    @Test
    fun repeatedMaskRowsAndColumnsKeepTheSameFeatheredCoverage() {
        val mask = Mask(ByteArray(16 * 12) { index ->
            val x = index % 16
            val y = index / 16
            when {
                x !in 4..11 || y !in 3..8 -> 0
                x == 4 -> 10
                x == 11 -> 100
                else -> -1
            }.toByte()
        })
        val original = mask.alpha.copyOf()
        listOf(27 to 37, 8 to 4, 16 to 12).forEach { (width, height) ->
            assertArrayEquals(
                PixelFactsMeasurement.fitMask(mask, 16, 12, width, height).alpha,
                fitExportMask(mask, 16, 12, width, height).alpha,
            )
        }
        assertArrayEquals(original, mask.alpha)
    }

    @Test
    fun singleChannelExportMaskMatchesThePixelResamplerIncludingFeatheredEdgesAndPadding() {
        val mask = Mask(byteArrayOf(0, 10, 90, -1, 127, 0, -1, 50, -1, 60, 0, 100))
        listOf(8 to 10, 2 to 2, 4 to 3, 4 to 9, 12 to 3).forEach { (width, height) ->
            assertArrayEquals(
                PixelFactsMeasurement.fitMask(mask, 4, 3, width, height).alpha,
                fitExportMask(mask, 4, 3, width, height).alpha,
            )
        }
    }
}
