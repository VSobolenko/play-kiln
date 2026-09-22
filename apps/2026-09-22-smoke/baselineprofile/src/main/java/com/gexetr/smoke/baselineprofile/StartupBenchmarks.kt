package com.gexetr.smoke.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.ExperimentalStableBaselineProfilesApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule as BenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = benchmarkRule.startup {
        startupTimingMetric()
    }

    // Run this generator separately from the benchmark; do not include both in
    // the same instrumentation module pass.
    @OptIn(ExperimentalStableBaselineProfilesApi::class)
    @Test
    fun generate() = baselineProfileRule.collectStableBaselineProfile(
        packageName = PACKAGE_NAME,
        maxIterations = 15,
        stableIterations = 3,
    ) {
        pressHome()
        startActivityAndWait(mainActivityIntent())
    }

    private fun BenchmarkRule.startup(metricsBlock: MutableList<Metric>.() -> Unit) {
        val metrics = mutableListOf<Metric>().apply(metricsBlock)
        measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = {
                pressHome()
            },
            measureBlock = {
                startActivityAndWait(mainActivityIntent())
            },
        )
    }

    private fun MutableList<Metric>.startupTimingMetric() {
        add(StartupTimingMetric())
    }

    private fun mainActivityIntent() =
        Intent(Intent.ACTION_MAIN).setClassName(PACKAGE_NAME, MAIN_ACTIVITY)

    companion object {
        // scripts/new-slot.mjs rewrites both constants when a slot is created.
        private const val PACKAGE_NAME = "com.gexetr.smoke"
        private const val MAIN_ACTIVITY = "com.gexetr.smoke.MainActivity"
    }
}
