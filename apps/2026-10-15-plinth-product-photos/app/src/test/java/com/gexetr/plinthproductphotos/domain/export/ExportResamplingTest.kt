package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportResamplingTest {
    @Test
    fun cachedLanczosAndMitchellWeightsKeepThePreviousOpaqueAndAlphaPixels() {
        val opaque = source(opaque = true)
        val alpha = source(opaque = false)
        assertArrayEquals(
            intArrayOf(0x2e6a33ff, 0x727c5cff, 0xa37045ff.toInt(), 0x3a826eff),
            opaque.scale(2, 2).pixels,
        )
        assertArrayEquals(
            intArrayOf(0x51ad4458, 0x62624771, 0x8689488f.toInt(), 0x326f60a7),
            alpha.scale(2, 2).pixels,
        )
        val positions = listOf(0, 5, 16, 24, 29)
        assertArrayEquals(
            intArrayOf(0x000000ff, 0xa0208aff.toInt(), 0x5a724fff, 0xa70517ff.toInt(), 0x4628a2ff),
            opaque.scale(6, 5).pixels.let { pixels -> positions.map { pixels[it] }.toIntArray() },
        )
        assertArrayEquals(
            intArrayOf(0xd6830b00.toInt(), 0x9f228a7c.toInt(), 0x484d218c, 0xa5031747.toInt(), 0x472aa2c5),
            alpha.scale(6, 5).pixels.let { pixels -> positions.map { pixels[it] }.toIntArray() },
        )
    }

    @Test
    fun scalingOnlyTheHeightDoesNotBlurUnchangedColumns() {
        val colors = intArrayOf(PixelBuffer.rgba(255, 0, 0), PixelBuffer.rgba(0, 255, 0), PixelBuffer.rgba(0, 0, 255))
        val image = PixelBuffer(3, 2, colors + colors).scale(3, 9)
        for (y in 0 until image.height) {
            colors.forEachIndexed { x, color -> assertEquals(color, image[x, y]) }
        }
    }

    @Test(timeout = 10_000)
    fun twoLargeImportsCanBeResampledForNinePacksWithoutPerPixelFilterWork() {
        val color = PixelBuffer.rgba(40, 80, 120)
        repeat(2) {
            val source = PixelBuffer.solid(2344, 2344, color)
            val composite = source.scale(960, 960)
            listOf(
                900 to 900, 900 to 900, 1440 to 1440,
                900 to 900, 1600 to 1600, 2000 to 2000,
                1000 to 1000, 1600 to 1600, 2048 to 2048,
            ).forEach { (width, height) ->
                val image = composite.scale(width, height)
                assertEquals(width * height, image.pixels.size)
                assertEquals(color, image[width / 2, height / 2])
            }

        }
    }

    @Test
    fun largeResamplingSplitsRowsIntoExecutorTasks() {
        val submittedTasks = AtomicInteger()
        val executor = Executor { task ->
            submittedTasks.incrementAndGet()
            task.run()
        }
        val source = PixelBuffer(512, 512, IntArray(512 * 512) { index ->
            PixelBuffer.rgba(index and 0xff, index / 512 and 0xff, index * 31 and 0xff)
        })
        val sequential = source.scale(600, 600, executor)
        val parallelExecutor = Executors.newFixedThreadPool(2)
        val parallel = try {
            source.scale(600, 600, parallelExecutor)
        } finally {
            parallelExecutor.shutdown()
        }

        assertTrue("Large axes should be split into independently processed row chunks", submittedTasks.get() > 2)
        assertArrayEquals(sequential.pixels, parallel.pixels)
    }

    private fun source(opaque: Boolean) = PixelBuffer(4, 3, IntArray(12) { index ->
        PixelBuffer.rgba(
            index * 53 and 255,
            index * 97 and 255,
            index * 131 and 255,
            if (opaque) 255 else index * 41 and 255,
        )
    })
}
