package com.gexetr.plinthproductphotos.domain.studio

import com.gexetr.plinthproductphotos.domain.series.SeriesSettlement
import com.gexetr.plinthproductphotos.domain.series.SeriesVariantStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesSettlementTest {
    @Test
    fun partialSeriesSettlesDeliveredVariantsAndReleasesTheRemainder() {
        val final = series()
            .delivered("v1")
            .delivered("v2")
            .delivered("v3")
            .finish()

        assertEquals(6, final.settledCredits)
        assertEquals(2, final.releasedCredits)
        assertEquals(0, final.reservedRemainderCredits)
    }

    @Test
    fun fullyDeliveredSeriesReleasesNothing() {
        val final = series()
            .delivered("v1")
            .delivered("v2")
            .delivered("v3")
            .delivered("v4")
            .finish()

        assertEquals(8, final.settledCredits)
        assertEquals(0, final.releasedCredits)
        assertEquals(0, final.reservedRemainderCredits)
    }

    @Test
    fun failedVariantIsReleasedButNeverSettled() {
        val afterFailure = series().failed("v1")

        assertEquals(SeriesVariantStatus.FAILED, afterFailure.variants.first().status)
        assertEquals(0, afterFailure.settledCredits)
        assertEquals(2, afterFailure.releasedCredits)
        assertEquals(6, afterFailure.reservedRemainderCredits)
    }

    private fun series() = SeriesSettlement.fromReservation(
        seriesId = "series-1",
        reservedCredits = 8,
        perVariantCredits = 2,
        variantIds = listOf("v1", "v2", "v3", "v4"),
    )
}
