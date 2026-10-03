package com.gexetr.lawnroundlog.ui.history

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.domain.Csv
import com.gexetr.lawnroundlog.domain.Payment
import com.gexetr.lawnroundlog.domain.RoundState
import com.gexetr.lawnroundlog.domain.Visit
import com.gexetr.lawnroundlog.domain.VisitStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Which half of the ledger the operator is looking at. */
enum class HistoryFilter { ALL, VISITS, PAYMENTS }

/**
 * One row of the ledger.
 *
 * A visit and a payment are flattened into the same shape so the list is one timeline rather than two
 * that have to be read side by side. Only one of [visit] and [payment] is ever non-null, and that is
 * what the row's edit and delete actions are built from.
 */
data class HistoryEntry(
    val id: String,
    val dateIso: String,
    val customerId: String,
    val customerName: String,
    val amountUsd: Double,
    val status: VisitStatus?,
    val visit: Visit?,
    val payment: Payment?,
)

/** What the last export did, so the screen can report rows rather than claim success. */
sealed interface ExportStatus {
    data object Idle : ExportStatus
    data class Done(val rows: Int) : ExportStatus
    data object Failed : ExportStatus
}

/**
 * The name the operator sees for the exported document, shared by both handoffs.
 *
 * A top-level constant rather than two literals, because the Save picker and the Share write must
 * name the same file: two names would mean the saved copy and the shared copy are two documents.
 */
internal const val EXPORT_FILE_NAME = "lawn-round-log.csv"

/** What the file is, and where the Share half stages it before handing its Uri out. */
private const val EXPORT_MIME_TYPE = "text/csv"
private const val EXPORT_CACHE_DIR = "exports"

/**
 * The authority the manifest registers the [FileProvider] under.
 *
 * It is derived from the running package's own name rather than written out, because this app is
 * installed under two applicationIds: the release one and the `.debug` variant the debug build
 * carries. A literal authority makes the debug package claim the release package's provider, which
 * the installer refuses outright (`INSTALL_FAILED_CONFLICTING_PROVIDER`) - so the debug build could
 * not be installed beside the release build and a device test of the debug artifact would be
 * impossible. The manifest spells the same expression as `${applicationId}.fileprovider`, so the
 * two cannot drift.
 */
internal fun fileProviderAuthority(context: Context): String = "${context.packageName}.fileprovider"

/**
 * The history screen's state holder: which half is shown, and what the last export wrote.
 *
 * The ledger itself is derived by [buildLedger] from the round as it currently stands, so the list can
 * never disagree with the round screen: there is one record and one transformation of it, not a second
 * copy kept in step by hand.
 */
class HistoryViewModel : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _export = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val export: StateFlow<ExportStatus> = _export.asStateFlow()

    fun setFilter(value: HistoryFilter) {
        _filter.value = value
    }

    fun clearExportStatus() {
        _export.value = ExportStatus.Idle
    }

    /**
     * Writes the whole record to the document the operator chose.
     *
     * The content resolver is handed in per call rather than held: a `Context` inside a ViewModel
     * outlives the screen that created it, and this only needs it for the duration of one write.
     * The write is on `Dispatchers.IO` because it is the filesystem, and the outcome is reported as a
     * row count from [Csv.Export] rather than as a bare "done".
     */
    fun exportCsv(context: Context, target: Uri, state: RoundState) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val export = Csv.export(state)
                    val stream = context.contentResolver.openOutputStream(target)
                        ?: return@runCatching null
                    stream.use { output ->
                        output.write(export.text.toByteArray(Charsets.UTF_8))
                    }
                    export
                }.getOrNull()
            }
            _export.value = result?.let {
                ExportStatus.Done(it.customerRows + it.visitRows + it.paymentRows)
            } ?: ExportStatus.Failed
        }
    }

    /**
     * Hands the whole record to whatever app the operator picks, as one CSV file.
     *
     * The file is written into the app's own cache and exposed through the [FileProvider] the
     * manifest declares, so the receiving app is granted a read Uri for this one document and the
     * app still needs no storage permission. The write runs on `Dispatchers.IO` because it is the
     * filesystem and the chooser is started from the main dispatcher because it is UI. A chooser
     * that no app accepts reports [ExportStatus.Failed] rather than a success the operator never
     * saw, and the same row count as the Save half is reported on the way out.
     */
    fun shareCsv(context: Context, state: RoundState) {
        viewModelScope.launch {
            val prepared = withContext(Dispatchers.IO) {
                runCatching {
                    val export = Csv.export(state)
                    val directory = File(context.cacheDir, EXPORT_CACHE_DIR).apply { mkdirs() }
                    val file = File(directory, EXPORT_FILE_NAME)
                    file.writeText(export.text, Charsets.UTF_8)
                    export to FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
                }.getOrNull()
            }
            if (prepared == null) {
                _export.value = ExportStatus.Failed
                return@launch
            }

            val (export, uri) = prepared
            val send = Intent(Intent.ACTION_SEND).apply {
                type = EXPORT_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // The clip data is what carries the read grant to receivers that read a Uri from
                // the intent's clip data rather than from `EXTRA_STREAM`.
                clipData = ClipData.newUri(context.contentResolver, EXPORT_FILE_NAME, uri)
            }
            val chooser = Intent.createChooser(send, context.getString(R.string.export_share_chooser))
            val started = runCatching { context.startActivity(chooser) }.isSuccess
            _export.value = if (started) {
                ExportStatus.Done(export.customerRows + export.visitRows + export.paymentRows)
            } else {
                ExportStatus.Failed
            }
        }
    }
}

/**
 * Turns the record into the timeline the history screen draws.
 *
 * Newest first, and ties broken by id so the order is stable across recompositions and across two
 * entries recorded on the same day.
 */
internal fun buildLedger(state: RoundState, filter: HistoryFilter): List<HistoryEntry> {
    val names = state.customers.associate { it.id to it.name }

    val visits = if (filter == HistoryFilter.PAYMENTS) {
        emptyList()
    } else {
        state.visits.map { visit ->
            HistoryEntry(
                id = visit.id,
                dateIso = visit.dateIso,
                customerId = visit.customerId,
                customerName = names[visit.customerId].orEmpty(),
                amountUsd = visit.amountUsd,
                status = visit.status,
                visit = visit,
                payment = null,
            )
        }
    }

    val payments = if (filter == HistoryFilter.VISITS) {
        emptyList()
    } else {
        state.payments.map { payment ->
            HistoryEntry(
                id = payment.id,
                dateIso = payment.dateIso,
                customerId = payment.customerId,
                customerName = names[payment.customerId].orEmpty(),
                amountUsd = payment.amountUsd,
                status = null,
                visit = null,
                payment = payment,
            )
        }
    }

    return (visits + payments).sortedWith(
        compareByDescending<HistoryEntry> { entry -> entry.dateIso }.thenByDescending { entry -> entry.id },
    )
}
