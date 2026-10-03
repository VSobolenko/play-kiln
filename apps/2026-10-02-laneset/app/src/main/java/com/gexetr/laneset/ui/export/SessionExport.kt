package com.gexetr.laneset.ui.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.gexetr.laneset.domain.Gear
import com.gexetr.laneset.domain.SectionKind
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SetBlock
import com.gexetr.laneset.domain.Stroke
import java.io.OutputStream
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The set list leaving the phone: the pure text renderer, the share intent and the printable card.
 *
 * This object is the single source of truth for the handoff (`differentiators.json` claim
 * `takes-it-to-the-pool`, `data_out`). The share body and the PDF are both written from
 * [buildSetListText], so a change to the format cannot reach one path and miss the other — the two
 * would otherwise drift the first time either is edited.
 *
 * **The renderer is pure on purpose.** [buildSetListText] takes a [Session] and nothing else, so it
 * is directly JVM-testable without a device or a `Context` — which is the property the contract's
 * "a test asserts the shared payload carries the whole set list with its interval targets" rests on.
 * The consequence is that the words it writes are the canonical pool notation a swimmer and a coach
 * both read (`WARM-UP`, `free`, `rest`), not `stringResource` values: a `Context`-free function
 * cannot reach the resource table. The screen's own copy around the exported text *is* localized
 * (`res/values/` and `res/values-ru/`). This limit is stated in the handoff rather than hidden.
 *
 * **No permission, no provider.** A text [Intent.ACTION_SEND] and an
 * [Intent.ACTION_CREATE_DOCUMENT] need no storage permission and no `FileProvider`, which is why
 * `AndroidManifest.xml` is unchanged by this file: the slot's own manifest declares only the Billing
 * permission. `INTERNET` and `ACCESS_NETWORK_STATE` do reach the packaged manifest, merged in by the
 * Google Play Billing library, and neither share path here uses them (`offline-by-construction`).
 */
object SessionExport {

    /**
     * The whole set list as text a swimmer can actually swim from.
     *
     * The header carries what the swimmer checks first — the pool length, the total distance, the
     * total time and the pace every target derives from — and then every section and every interval
     * follows, one per line, in the form `8 x 50 m freestyle @ 1:05, rest 0:20`. Times are written
     * the way a pace clock shows them, never as a raw second count.
     */
    fun buildSetListText(session: Session): String {
        val constraints = session.constraints
        return buildString {
            appendLine("Laneset - Swim Set Builder")
            appendLine(
                "${constraints.poolLengthMeters} m pool | " +
                    "${session.totalDistanceMeters} m | " +
                    "${session.estimatedTotalMinutes} min",
            )
            appendLine("Pace ${formatSeconds(session.pacePer100Seconds.roundToInt())} /100 m")
            session.sections.forEach { section ->
                appendLine()
                appendLine(sectionTitle(section.kind))
                section.blocks.forEach { block -> appendLine("  " + blockLine(block)) }
            }
        }
    }

    /** One interval as a line: reps, distance, stroke (and gear), target and rest. */
    fun blockLine(block: SetBlock): String {
        val gear = block.gear
            .sortedBy { it.name }
            .joinToString(" + ") { gearWord(it) }
        val stroke = if (gear.isEmpty()) strokeWord(block.stroke) else "${strokeWord(block.stroke)} $gear"
        return "${block.repetitions} x ${block.distanceMeters} m $stroke @ " +
            "${formatSeconds(block.targetTimeSeconds)}, rest ${formatSeconds(block.restSeconds)}"
    }

    /**
     * Seconds as a swimmer reads them: `m:ss` under an hour, `h:mm:ss` past it.
     *
     * A pool pace clock is read in minutes and seconds, so `105.0 seconds` is never printed — it is
     * `1:45`. Fixed to [Locale.US] so the digits do not change with the device's numbering system and
     * the same session always produces the same payload.
     */
    fun formatSeconds(totalSeconds: Int): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainder = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, remainder)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, remainder)
        }
    }

    /**
     * The share intent: the whole set list as plain text, launched through a chooser so the swimmer
     * picks the app. [chooserTitle] is the caller's localized chooser caption.
     */
    fun shareIntent(session: Session, chooserTitle: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, buildSetListText(session))
            putExtra(Intent.EXTRA_SUBJECT, "Laneset - ${session.totalDistanceMeters} m")
        }
        return Intent.createChooser(send, chooserTitle)
    }

    /**
     * Shares, or reports that nothing on the device can receive the set list.
     *
     * Play's own guidance is to expect a device with no handler for a send intent; the chooser itself
     * is a system component, so the observable failure is a chooser with no targets. A throwing
     * `startActivity` is caught and reported as `false` rather than crashing the screen.
     */
    fun launchShare(context: Context, session: Session, chooserTitle: String): Boolean = try {
        context.startActivity(shareIntent(session, chooserTitle))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /**
     * The file-picker intent for the printable card: the Storage Access Framework's create-document
     * action, which hands back a `content://` URI the app can write without any storage permission.
     */
    fun createDocumentIntent(suggestedFileName: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, suggestedFileName)
        }

    /**
     * Writes [session] as a one-page (or, for a long set list, a few-page) monospaced card.
     *
     * The card is the same text [buildSetListText] produces, set in a monospace face and paginated
     * when a long set list cannot fit one sheet — a card that silently clipped its last intervals
     * would be a worse handoff than one extra page. The stream is the caller's; this function neither
     * opens nor closes it.
     */
    fun writePdfCard(session: Session, output: OutputStream) {
        val document = PdfDocument()
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val lineHeight = 16f
        val lines = buildSetListText(session).trimEnd().lines().ifEmpty { listOf("Laneset") }
        var index = 0
        var pageNumber = 1
        while (index < lines.size) {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
            )
            val canvas = page.canvas
            var y = margin + lineHeight
            while (index < lines.size && y <= pageHeight - margin) {
                canvas.drawText(lines[index], margin, y, body)
                index += 1
                y += lineHeight
            }
            document.finishPage(page)
            pageNumber += 1
        }
        try {
            document.writeTo(output)
        } finally {
            // A PdfDocument holds a native handle: a failed write must not leak it.
            document.close()
        }
    }

    /** The canonical pool-notation name of a section header. */
    fun sectionTitle(kind: SectionKind): String = when (kind) {
        SectionKind.WARM_UP -> "Warm up"
        SectionKind.MAIN_SET -> "Main set"
        SectionKind.COOL_DOWN -> "Cool down"
    }

    /** The canonical pool-notation name of a stroke. */
    fun strokeWord(stroke: Stroke): String = when (stroke) {
        Stroke.FREESTYLE -> "freestyle"
        Stroke.BACKSTROKE -> "backstroke"
        Stroke.BREASTSTROKE -> "breaststroke"
        Stroke.BUTTERFLY -> "butterfly"
        Stroke.INDIVIDUAL_MEDLEY -> "IM"
    }

    /** The canonical pool-notation name of a piece of gear. */
    fun gearWord(gear: Gear): String = when (gear) {
        Gear.KICKBOARD -> "kickboard"
        Gear.PULL_BUOY -> "pull buoy"
        Gear.FINS -> "fins"
        Gear.PADDLES -> "paddles"
        Gear.SNORKEL -> "snorkel"
    }
}
