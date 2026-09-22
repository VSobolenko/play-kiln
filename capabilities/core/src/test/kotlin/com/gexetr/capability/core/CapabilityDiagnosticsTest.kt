package com.gexetr.capability.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CapabilityDiagnosticsTest {
    @Test
    fun `put replaces an id without changing insertion order and collectors observe it`() = runTest {
        val diagnostics: CapabilityDiagnostics = MutableCapabilityDiagnostics()
        val mutableDiagnostics = diagnostics as MutableCapabilityDiagnostics
        val observed = mutableListOf<List<CapabilityReport>>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            diagnostics.reports.take(4).toList(observed)
        }
        val adsInitial = CapabilityReport(
            id = CapabilityId.ADS,
            availability = CapabilityAvailability.UNINITIALIZED,
            detail = null,
        )
        val billing = CapabilityReport(
            id = CapabilityId.BILLING,
            availability = CapabilityAvailability.READY,
            detail = "connected",
        )
        val adsUpdated = CapabilityReport(
            id = CapabilityId.ADS,
            availability = CapabilityAvailability.FAILED,
            detail = "initialization failed",
        )

        mutableDiagnostics.put(adsInitial)
        mutableDiagnostics.put(billing)
        mutableDiagnostics.put(adsUpdated)
        collector.join()

        assertEquals(
            listOf(
                emptyList(),
                listOf(adsInitial),
                listOf(adsInitial, billing),
                listOf(adsUpdated, billing),
            ),
            observed,
        )
        assertEquals(listOf(adsUpdated, billing), diagnostics.reports.value)
    }
}
