package com.gexetr.unitwalk.pdf

/**
 * The mark a free-tier report carries, as plain data.
 *
 * It exists as its own file, with no Android import, so the decision that a report is stamped can be
 * driven by a JVM unit test: [com.gexetr.unitwalk.pdf.renderReportPdf] itself needs
 * `android.graphics.pdf.PdfDocument` and is therefore proved on the device, but *whether* the stamp
 * is there — and what it says — is decided here, where a test can read it.
 *
 * The text is deliberately a claim about the document rather than about the property, and the note
 * names the purchase. A stamped PDF that a tenant reads must not be mistakable for the landlord's own
 * condition record, so the stamp is drawn large, diagonal, translucent and in a colour the record
 * never uses (see `drawWatermark` in `PdfReport.kt`) — never as ordinary body text.
 */
data class ReportWatermark(
    /** The large line, drawn across the middle of every page. */
    val text: String,
    /** The small line beneath it, naming what removes the stamp. */
    val note: String,
) {
    companion object {
        /**
         * The stamp a report carries while the one-time unlock is not confirmed as owned.
         *
         * Fixed text rather than a resource id: this value is rendered into a PDF by a JVM-testable
         * pure function, and it is a property of the *document*, not of a screen — the same stamp must
         * appear whatever the app's locale is, because the PDF leaves the app and is read by whoever
         * receives the file.
         */
        val FREE_TIER: ReportWatermark = ReportWatermark(
            text = "FREE PREVIEW",
            note = "Unlock UnitWalk to remove this stamp",
        )
    }
}

/**
 * The whole paid perk, as a pure decision: an owned unlock removes the watermark, and nothing else
 * does.
 *
 * [owned] must be `true` only for a *confirmed* entitlement — the caller reads it through
 * `UnlockState.isOwned` in `ui/unlock/UnlockProducts.kt`, which treats an unresolved entitlement as
 * not owned. Passing a boolean here rather than an entitlement type is deliberate: it keeps this
 * module free of both Android and the billing capability, so the watermark rule can be tested without
 * either, and `null` has exactly one meaning — draw a clean report.
 */
fun reportWatermark(owned: Boolean): ReportWatermark? = if (owned) null else ReportWatermark.FREE_TIER
