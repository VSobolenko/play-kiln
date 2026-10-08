package com.gexetr.unitwalk.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.gexetr.unitwalk.domain.ReportModel
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.WalkKind
import java.io.ByteArrayOutputStream

/**
 * Renders the finished walk to an A4-ish PDF.
 *
 * ## This class is verified on the device, not by a JVM unit test
 *
 * It is the one file in this worker's scope that imports the Android framework
 * (`android.graphics.pdf.PdfDocument`, `android.graphics.Paint`, `android.graphics.Color`), so it
 * cannot run under plain JUnit — this slot has no Robolectric dependency, and the factory's rule is
 * that a framework-bound renderer is proved by running it on the attached phone, not by a mocked
 * test. The *content* it draws is a [ReportModel] built by the framework-free
 * [com.gexetr.unitwalk.domain.buildReportModel], which **is** unit-tested; this file is only the
 * layout of that value onto pages.
 *
 * The PDF carries the unit, the walk kind, the dates, every room with its note, condition and photo
 * file names, every recorded difference and the entered signatures. Nothing here uploads anything:
 * it produces bytes.
 *
 * ## The watermark
 *
 * When the one-time unlock is not confirmed as owned, every page carries [watermark] — a large,
 * diagonal, translucent stamp in a colour the record never uses, so a reader cannot mistake it for
 * part of the condition record. It is a **parameter** rather than a global read: the decision is
 * [reportWatermark]'s (a pure function with its own JVM test), and this file only draws what it is
 * handed. A `null` watermark is the paid report and draws nothing extra.
 */

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val LINE_HEIGHT = 16f
private const val TITLE_SIZE = 20f
private const val HEADING_SIZE = 14f
private const val BODY_SIZE = 11f
private const val MAX_CHARS = 96

/** Big enough that the stamp is read before the small body text, faint enough to read through. */
private const val WATERMARK_TEXT_SIZE = 44f
private const val WATERMARK_NOTE_SIZE = 13f

/** A red no body line uses, at low alpha: visible over the record, never part of it. */
private const val WATERMARK_INK = 0x46_8A1F1F

/** Lays [model] onto A4-ish 595x842 pt pages and returns the finished PDF bytes. */
fun renderReportPdf(model: ReportModel, watermark: ReportWatermark? = null): ByteArray {
    val document = PdfDocument()
    val titlePaint = textPaint(TITLE_SIZE, Color.BLACK, bold = true)
    val headingPaint = textPaint(HEADING_SIZE, Color.BLACK, bold = true)
    val strongPaint = textPaint(BODY_SIZE, Color.BLACK, bold = false)
    val bodyPaint = textPaint(BODY_SIZE, Color.DKGRAY, bold = false)
    val watermarkTextPaint = textPaint(WATERMARK_TEXT_SIZE, WATERMARK_INK, bold = true)
    val watermarkNotePaint = textPaint(WATERMARK_NOTE_SIZE, WATERMARK_INK, bold = false)

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
    )
    var canvas = page.canvas
    var y = MARGIN

    fun stampWatermark() {
        val stamp = watermark ?: return
        drawWatermark(canvas, stamp, watermarkTextPaint, watermarkNotePaint)
    }

    fun startNewPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
        )
        canvas = page.canvas
        y = MARGIN
        stampWatermark()
    }

    stampWatermark()

    fun drawLine(text: String, paint: Paint, height: Float = LINE_HEIGHT) {
        if (y + height > PAGE_HEIGHT - MARGIN) startNewPage()
        canvas.drawText(text.take(MAX_CHARS), MARGIN, y, paint)
        y += height
    }

    // Header.
    drawLine(model.unitName.ifBlank { "Unit walk" }, titlePaint)
    if (model.unitAddress.isNotBlank()) drawLine(model.unitAddress, bodyPaint)
    drawLine("Walk: ${kindLabel(model.kind)}", headingPaint)
    drawLine("Started: ${model.startedAtIso}", bodyPaint)
    drawLine("Finished: ${model.finishedAtIso ?: "-"}", bodyPaint)
    drawLine("Rooms: ${model.rooms.size}    Photos: ${model.photoCount}", bodyPaint)
    y += LINE_HEIGHT

    // Rooms.
    drawLine("Rooms", headingPaint)
    model.rooms.forEachIndexed { index, room ->
        drawLine("${index + 1}. ${room.name}", strongPaint)
        drawLine("   Condition: ${room.condition?.let(::conditionLabel) ?: "-"}", bodyPaint)
        if (room.note.isNotBlank()) drawLine("   ${room.note}", bodyPaint)
        if (room.photoFileNames.isEmpty()) {
            drawLine("   No photos.", bodyPaint)
        } else {
            drawLine("   Photos: ${room.photoFileNames.joinToString(", ")}", bodyPaint)
        }
    }

    // Differences.
    y += LINE_HEIGHT
    drawLine("Differences", headingPaint)
    if (model.deltas.isEmpty()) {
        drawLine("No differences recorded.", bodyPaint)
    } else {
        model.deltas.forEach { delta ->
            drawLine("- ${conditionLabel(delta.condition)}", bodyPaint)
            if (delta.note.isNotBlank()) drawLine("  ${delta.note}", bodyPaint)
        }
    }

    // Signatures.
    y += LINE_HEIGHT
    drawLine("Signatures", headingPaint)
    if (model.signatures.isEmpty()) {
        drawLine("Not signed.", bodyPaint)
    } else {
        model.signatures.forEach { signature ->
            drawLine("- ${signature.name.ifBlank { "-" }} (${signature.signedAtIso})", bodyPaint)
        }
    }

    document.finishPage(page)
    val out = ByteArrayOutputStream()
    document.writeTo(out)
    document.close()
    return out.toByteArray()
}

private fun textPaint(size: Float, color: Int, bold: Boolean): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        isFakeBoldText = bold
    }

/**
 * Stamps [watermark] across the middle of the page, rotated so it lies diagonally over the record.
 *
 * The canvas is saved and restored around the rotation, so the record's own lines stay horizontal and
 * the page's `y` cursor is untouched. Centring is done by the paint's [Paint.Align.CENTER] rather than
 * by measuring, which keeps the stamp in place whatever its text becomes.
 */
private fun drawWatermark(
    canvas: Canvas,
    watermark: ReportWatermark,
    stampPaint: Paint,
    stampNotePaint: Paint,
) {
    val centreX = PAGE_WIDTH / 2f
    val centreY = PAGE_HEIGHT / 2f
    canvas.save()
    canvas.rotate(-40f, centreX, centreY)
    stampPaint.textAlign = Paint.Align.CENTER
    stampNotePaint.textAlign = Paint.Align.CENTER
    canvas.drawText(watermark.text, centreX, centreY, stampPaint)
    canvas.drawText(watermark.note, centreX, centreY + 26f, stampNotePaint)
    canvas.restore()
}

private fun kindLabel(kind: WalkKind): String = when (kind) {
    WalkKind.MOVE_IN -> "Move-in"
    WalkKind.MOVE_OUT -> "Move-out"
    WalkKind.SINGLE -> "Single"
}

private fun conditionLabel(condition: RoomCondition): String = when (condition) {
    RoomCondition.GOOD -> "Good"
    RoomCondition.WORN -> "Worn"
    RoomCondition.DAMAGED -> "Damaged"
    RoomCondition.MISSING -> "Missing"
}
