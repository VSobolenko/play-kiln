package com.gexetr.plinthproductphotos.ui

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationDataLoadTest {
    @Test
    fun blockingLoadRunsOffTheCallerThreadAndReturnsTheOriginalValue() = runBlocking {
        val caller = Thread.currentThread()
        val expected = Any()

        val actual = loadDestinationData {
            assertNotSame(caller, Thread.currentThread())
            expected
        }

        assertSame(expected, actual)
        assertSame(caller, Thread.currentThread())
    }

    @Test
    fun callerCanObservePendingLoadBeforeBlockingWorkFinishes() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val expected = Any()
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            loadDestinationData {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS)) { "Caller did not release the pending read" }
                expected
            }
        }

        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertFalse(pending.isCompleted)
            release.countDown()
            assertSame(expected, pending.await())
        } finally {
            release.countDown()
            pending.cancel()
        }
    }

    @Test
    fun failedLoadPropagatesTheOriginalFailureInsteadOfAnEmptyDefault() = runBlocking {
        val failure = IOException("fixture asset read failure")

        val result = runCatching {
            loadDestinationData<Any> { throw failure }
        }

        val propagated = result.exceptionOrNull()
        assertEquals("propagated failure type", IOException::class.java, propagated?.javaClass)
        assertEquals("propagated failure message", "fixture asset read failure", propagated?.message)
    }
}
