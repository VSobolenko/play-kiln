package com.gexetr.plinthproductphotos.ui.autocard

import com.gexetr.plinthproductphotos.domain.autocard.RecognitionImage
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCardImageWorkTest {
    @Test
    fun imageWorkDoesNotExecuteOnTheCallingThreadAndReturnsTheActualResult() = runBlocking {
        val caller = Thread.currentThread()
        val image = PixelBuffer.solid(2, 2, PixelBuffer.rgba(12, 34, 56))

        val result = AutoCardImageWork.run {
            assertNotSame(caller, Thread.currentThread())
            image
        }

        assertSame(image, result)
        assertSame(caller, Thread.currentThread())
    }

    @Test
    fun imageWorkPropagatesFailuresInsteadOfReturningAReplacement() = runBlocking {
        val failure = IllegalStateException("fixture image failure")
        val result = runCatching {
            AutoCardImageWork.run<PixelBuffer> { throw failure }
        }
        val propagated = result.exceptionOrNull()
        assertEquals("propagated failure type", IllegalStateException::class.java, propagated?.javaClass)
        assertEquals("propagated failure message", "fixture image failure", propagated?.message)
    }

    @Test
    fun cancelledCpuWorkCannotOverlapItsReplacement() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val replacementStarted = CountDownLatch(1)
        val replacementEntered = CountDownLatch(1)
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()
        val first = async(kotlinx.coroutines.Dispatchers.Default) {
            AutoCardImageWork.run {
                maximumActive.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                entered.countDown()
                try {
                    check(release.await(5, TimeUnit.SECONDS))
                } finally {
                    active.decrementAndGet()
                }
            }
        }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            first.cancel()
            val second = async(kotlinx.coroutines.Dispatchers.Default) {
                replacementStarted.countDown()
                AutoCardImageWork.run {
                    replacementEntered.countDown()
                    maximumActive.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                    active.decrementAndGet()
                }
            }
            assertTrue(replacementStarted.await(5, TimeUnit.SECONDS))
            assertFalse(replacementEntered.await(250, TimeUnit.MILLISECONDS))
            release.countDown()
            second.await()
            first.join()
            assertEquals(1, maximumActive.get())
        } finally {
            release.countDown()
            first.cancel()
        }
    }

    @Test
    fun bitmapRowConversionUsesOnlyOneRowAndDoesNotChangeTheSource() {
        val pixels = intArrayOf(0x12345678, 0xabcdef01.toInt(), 0xff0000ff.toInt(), 0x00ff0080)
        val image = PixelBuffer(2, 2, pixels)
        val row = IntArray(2)

        image.copyAutoCardArgbRow(0, row)
        assertEquals(0x78123456, row[0])
        assertEquals(0x01abcdef, row[1])
        image.copyAutoCardArgbRow(1, row)
        assertEquals(0xffff0000.toInt(), row[0])
        assertEquals(0x8000ff00.toInt(), row[1])
        assertEquals(0x12345678, pixels[0])
        assertEquals(0x00ff0080, pixels[3])
    }

    @Test
    fun displayPreviewIsBoundedButDoesNotUpscaleSmallImages() {
        val large = autoCardPreviewSize(2344, 2344)
        assertEquals(1024, large.width)
        assertEquals(1024, large.height)
        assertTrue(large.width.toLong() * large.height * 4 <= 4_194_304)
        assertEquals(RecognitionImage.Size(600, 800), autoCardPreviewSize(600, 800))
        assertEquals(RecognitionImage.Size(1024, 524), autoCardPreviewSize(2344, 1200))
    }
}
