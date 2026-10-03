package com.gexetr.laneset.ui.importer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.laneset.data.AppGraph
import com.gexetr.laneset.data.PaceStore
import com.gexetr.laneset.data.SessionPosition
import com.gexetr.laneset.data.SessionStore
import com.gexetr.laneset.data.SettingsStore
import com.gexetr.laneset.domain.CoachSetParser
import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.SessionConstraints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The import screen's state holder.
 *
 * It owns the paste and the parsed preview, and it writes the accepted session through the same
 * [SessionStore] Home reads — the store is the bus between the two screens, so there is no event
 * channel here and no second copy of the session to keep in sync.
 *
 * **The parser is called, never re-implemented.** [CoachSetParser.parse] is the only reader of a
 * written practice in the app; this class supplies the pace and the constraints and holds the result.
 * The one helper that inspects lines ([unreadableLineNumbers]) also calls the same parser rather than
 * matching patterns of its own, so the "what it could not read" report cannot disagree with the parse.
 *
 * The pace comes from [PaceStore], the same threshold the generator uses. When the swimmer has not
 * swum the trials there is no pace, and the honest answer is [CoachImportStatus.NeedsPace] rather than
 * an invented number.
 */
class CoachImportViewModel(
    private val sessionStore: SessionStore,
    private val paceStore: PaceStore,
    private val poolLengthMeters: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(CoachImportUiState())
    val state: StateFlow<CoachImportUiState> = _state.asStateFlow()

    /** The threshold pace once it has been read from the store; `null` until then and when unset. */
    private var pacePer100Seconds: Double? = null

    /** A new paste clears the previous review, so an approved set is never a set of older text. */
    fun onTextChange(text: String) {
        _state.update {
            it.copy(
                text = text,
                parsed = null,
                unreadableLineNumbers = emptyList(),
                status = CoachImportStatus.Idle,
            )
        }
    }

    /**
     * Parses the paste with the swimmer's own pace and shows what was recognised.
     *
     * The pace is read here rather than only once at construction, because the store's first emission
     * is asynchronous: a swimmer who taps Read the instant the screen opens must not be told they have
     * no pace merely because the read had not landed yet. The value is kept after the first read, so
     * the common case is one in-memory lookup.
     */
    fun onRead() {
        val text = _state.value.text
        if (text.isBlank()) {
            _state.update { it.copy(status = CoachImportStatus.EmptyInput) }
            return
        }
        viewModelScope.launch {
            val pace = pacePer100Seconds ?: paceStore.profile.first().thresholdPacePer100Seconds
            // A non-positive stored pace is a refusal, not a number: `CoachSetParser.parse` `require`s a
            // positive pace and would throw, and a crash is not what "no usable pace" should look like.
            // It is also not cached, so a repaired profile is read again on the next attempt.
            if (pace == null || pace <= 0.0) {
                _state.update { it.copy(status = CoachImportStatus.NeedsPace) }
                return@launch
            }
            pacePer100Seconds = pace

            val constraints = SessionConstraints(
                poolLengthMeters = poolLengthMeters,
                minutesAvailable = 1,
                focus = Focus.ENDURANCE,
                strokesToAvoid = emptySet(),
                gearOnDeck = emptySet(),
            )
            // The parser recomputes the minute total from the sets it read, and takes the focus from
            // the text when the practice names one; both of those are its decision, not this caller's.
            val session = CoachSetParser.parse(text, pace, constraints)
            val unreadable = unreadableLineNumbers(text, pace, constraints)
            val recognised = session.blocks.isNotEmpty()

            _state.update {
                it.copy(
                    parsed = session.takeIf { recognised },
                    unreadableLineNumbers = unreadable,
                    status = if (recognised) {
                        CoachImportStatus.Reviewed
                    } else {
                        CoachImportStatus.NothingRecognised
                    },
                )
            }
        }
    }

    /**
     * Takes the clipboard as the source when the swimmer taps the paste action.
     *
     * The read itself is the screen's job — it needs a `Context` this class does not hold — and this
     * method is where the *decision* about what an empty clipboard means lives: a refusal the swimmer
     * can see, not a silent no-op. A successful paste is treated exactly like typing: the previous
     * review is cleared, so an approved set can never belong to older text.
     */
    fun onPasteFromClipboard(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) {
            _state.update { it.copy(status = CoachImportStatus.ClipboardEmpty) }
            return
        }
        _state.update {
            it.copy(
                text = clipboardText,
                parsed = null,
                unreadableLineNumbers = emptyList(),
                status = CoachImportStatus.Idle,
            )
        }
    }

    /** Writes the reviewed session to the store; Home picks it up from there. */
    fun onAccept() {
        val session = _state.value.parsed ?: return
        viewModelScope.launch {
            sessionStore.save(session, SessionPosition.Start)
            _state.update { it.copy(status = CoachImportStatus.Saved) }
        }
    }

    companion object {
        /**
         * Builds the ViewModel against the app graph.
         *
         * `AppGraph.init` is idempotent, so calling it here is harmless when the host has already
         * built the graph and is the reason the screen works when it has not.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    AppGraph.init(applicationContext)
                    return CoachImportViewModel(
                        sessionStore = AppGraph.session,
                        paceStore = AppGraph.pace,
                        poolLengthMeters = SettingsStore.DEFAULT_POOL_LENGTH_METERS,
                    ) as T
                }
            }
        }
    }
}

/**
 * The 1-based numbers of the lines the parser could not turn into a set.
 *
 * The parser deliberately skips such a line rather than guessing at it, and it does not report which
 * ones — so the report is built by handing each line to the *same* parser ([CoachSetParser.parse])
 * and keeping the numbered ones that produced no block. The digit test is the one thing this function
 * adds, and its sole job is to leave a bare section header alone: `Warm up` carries no digits and no
 * set, while `8 x 1oo free` carries digits the parser could not read and is worth telling the swimmer
 * about. It does not restate the parser's distance pattern, so the two cannot drift.
 */
private fun unreadableLineNumbers(
    text: String,
    pacePer100Seconds: Double,
    constraints: SessionConstraints,
): List<Int> {
    val numbers = mutableListOf<Int>()
    text.lines().forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEachIndexed
        val parsed = CoachSetParser.parse(line, pacePer100Seconds, constraints)
        if (parsed.blocks.isEmpty() && line.any { it.isDigit() }) numbers += index + 1
    }
    return numbers
}
