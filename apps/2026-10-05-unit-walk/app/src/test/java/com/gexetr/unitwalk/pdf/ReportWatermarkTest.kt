package com.gexetr.unitwalk.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The free/paid boundary as a pure decision, driven on the JVM.
 *
 * `renderReportPdf` itself imports `android.graphics.pdf.PdfDocument` and is therefore proved on the
 * device (see `app/src/androidTest/.../ReportPdfTest.kt`); the *decision* it is handed is pure, and
 * that is the half that can be wrong while every pixel of the renderer stays exactly as it was.
 *
 * **The observation that fails against the code before this fix.** `reportWatermark` did not exist and
 * `renderReportPdf` took no watermark parameter, so a report was written for every user with no stamp
 * anywhere: the paid perk gated nothing. These assertions cannot compile against that revision, which
 * is the honest form of "a test of the perk fails when there is no perk" — and, once the function
 * exists, `anUnownedUnlockStampsTheReport` is the row that a `return null` implementation fails.
 */
class ReportWatermarkTest {

    @Test
    fun anOwnedUnlockProducesACleanReport() {
        assertNull(reportWatermark(owned = true))
    }

    @Test
    fun anUnownedUnlockStampsTheReport() {
        assertEquals(ReportWatermark.FREE_TIER, reportWatermark(owned = false))
    }

    @Test
    fun theStampIsVisibleAndNamesWhatRemovesIt() {
        val stamp = reportWatermark(owned = false) ?: error("an unowned report must carry a stamp")

        // Both lines are drawn, so both must say something: an empty line is an invisible stamp.
        assertTrue(stamp.text.isNotBlank())
        assertTrue(stamp.note.isNotBlank())

        // The large line says what the document is, and the small line names the purchase that removes
        // it — a stamp that says neither is a decoration, not a boundary.
        assertTrue(stamp.text.contains("FREE", ignoreCase = true))
        assertTrue(stamp.note.contains("Unlock", ignoreCase = true))
    }

    /**
     * The stamp must not be mistakable for part of the condition record. The record's own vocabulary
     * (`conditionLabel` in `PdfReport.kt`) is Good / Worn / Damaged / Missing, and a stamp that read as
     * one of those words would be read as a recorded finding rather than as a preview mark.
     */
    @Test
    fun theStampIsNotMistakableForAConditionWord() {
        val stamp = reportWatermark(owned = false) ?: error("an unowned report must carry a stamp")
        val recordVocabulary = listOf("Good", "Worn", "Damaged", "Missing")

        assertTrue(
            "the stamp '$stamp' reads as a condition the record itself uses",
            recordVocabulary.none { word -> stamp.text.equals(word, ignoreCase = true) },
        )
    }
}
