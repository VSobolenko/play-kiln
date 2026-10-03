package com.gexetr.laneset.ui.importer

import com.gexetr.laneset.domain.Session

/**
 * What the import screen is showing, as one closed set.
 *
 * The screen is a function of this value, so every branch is reachable in a preview and a test:
 * `Idle` before the swimmer asks for a read, `Reviewed` when the parser produced a session the
 * swimmer can inspect, and the remaining members each state one honest refusal. Nothing here is a
 * boolean that can be true and false at once.
 */
enum class CoachImportStatus {
    /** Nothing has been read yet, or the text changed since the last read. */
    Idle,

    /** The parser produced a session; the review and the accept action are shown. */
    Reviewed,

    /** The text was empty when the swimmer asked for a read. */
    EmptyInput,

    /** There is nothing usable on the clipboard to paste in. */
    ClipboardEmpty,

    /** The parser found no set at all in the text. */
    NothingRecognised,

    /** There is no threshold pace yet, so no target can be put on an interval. */
    NeedsPace,

    /** The accepted session was written to the store. */
    Saved,
}

/**
 * The import screen's state.
 *
 * [parsed] is the parser's own [Session], held whole rather than mirrored as a second model, so the
 * review the swimmer approves and the value that reaches the store are the same object. [text] is the
 * raw paste; it is the only input the parser is given, and it is never rewritten to fit the parse.
 *
 * [unreadableLineNumbers] is 1-based, matching what a text editor shows. Whether the swimmer has a
 * threshold pace is deliberately not a field here: the absence of a pace is reported as the
 * `NeedsPace` status at the moment it blocks the parse, so nothing has to be kept in sync.
 */
data class CoachImportUiState(
    val text: String = "",
    val parsed: Session? = null,
    val unreadableLineNumbers: List<Int> = emptyList(),
    val status: CoachImportStatus = CoachImportStatus.Idle,
)
