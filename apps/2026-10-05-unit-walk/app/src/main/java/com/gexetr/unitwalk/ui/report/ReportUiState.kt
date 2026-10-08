package com.gexetr.unitwalk.ui.report

import android.net.Uri
import com.gexetr.unitwalk.ui.unlock.UnlockState
import com.gexetr.unitwalk.ui.unlock.isOwned

/**
 * The report surface's state, as typed values only — the screen owns every word.
 *
 * It carries the app's own report of what it wrote ([status], [pdfFileName], [pdfByteCount]) rather
 * than a boolean: "the PDF is ready" is a fact with a size and a name, and a silent success would
 * hide a zero-byte file. [pendingDeletion] exists so the screen can show a confirmation that names
 * the item before anything is removed, instead of deleting on the first tap.
 *
 * [unlock] is the one-time unlock as this screen reads it, and it is the whole free/paid boundary in
 * one field. [isOwned] and [watermarked] are **derived** from it rather than stored beside it, so the
 * upsell and the watermark cannot disagree with the entitlement they claim to describe — a second
 * boolean that drifted from [unlock] is exactly the defect this shape prevents.
 */
data class ReportUiState(
    val status: ReportStatus = ReportStatus.Loading,
    val walkId: String = "",
    val unitName: String = "",
    val roomCount: Int = 0,
    val photoCount: Int = 0,
    val deltaCount: Int = 0,
    val signatureCount: Int = 0,
    val pdfUri: Uri? = null,
    val pdfFileName: String? = null,
    val pdfByteCount: Long = 0,
    val suggestedFileName: String? = null,
    val pendingDeletion: PendingDeletion? = null,
    val message: ReportMessage? = null,
    /**
     * Whether the one-time unlock is confirmed for this account.
     *
     * [UnlockState.Unknown] is deliberately not owned: while billing has not answered, the report is
     * stamped rather than handed out as the paid perk, and it is still written and still shareable —
     * export is never gated on ownership.
     */
    val unlock: UnlockState = UnlockState.Unknown,
) {
    /** The paid perk, confirmed: an owner sees no upsell and gets a report without a watermark. */
    val isOwned: Boolean get() = unlock.isOwned

    /** Whether the PDF written for this state carries the free tier's stamp. */
    val watermarked: Boolean get() = !unlock.isOwned
}


/** Where the report handoff currently stands. */
enum class ReportStatus {
    Loading,
    Ready,
    Empty,
    Failed,
}

/** What just happened, in the app's own vocabulary — never a sentence. */
enum class ReportMessage {
    PdfWritten,
    PdfWriteFailed,
    SignatureSaved,
    SavedToChosenLocation,
    SaveFailed,
    NothingToSave,
}

/** A deletion the user has asked for but not yet confirmed; [displayName] is what it names. */
sealed interface PendingDeletion {
    val displayName: String

    data class WalkDeletion(
        val walkId: String,
        override val displayName: String,
    ) : PendingDeletion

    data class UnitDeletion(
        val unitId: String,
        override val displayName: String,
    ) : PendingDeletion
}

/** The user's intents on the report surface. */
sealed interface ReportEvent {
    data class SignatureEntered(
        val name: String,
        val signedAtIso: String,
    ) : ReportEvent

    data class DeleteWalkRequested(
        val walkId: String,
        val walkName: String,
    ) : ReportEvent

    data class DeleteUnitRequested(
        val unitId: String,
        val unitName: String,
    ) : ReportEvent

    data object DeletionConfirmed : ReportEvent

    data object DeletionCancelled : ReportEvent

    data object MessageDismissed : ReportEvent
}
