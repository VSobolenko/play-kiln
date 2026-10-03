package com.gexetr.laneset.ui.home

import androidx.compose.runtime.Immutable
import com.gexetr.laneset.data.SessionPosition
import com.gexetr.laneset.data.SettingsStore
import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.Gear
import com.gexetr.laneset.domain.SectionKind
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SessionConstraints
import com.gexetr.laneset.domain.SetBlock
import com.gexetr.laneset.domain.Stroke

/**
 * The pace the free tier computes with, per 100 m, in seconds.
 *
 * **This is a product number, not a measured one, and it is the one number in the app nobody swam.**
 * The app's whole promise is that the targets come from the swimmer's *own* two time trials, and the
 * two trials are behind the one-time unlock. Until they are swum there is no threshold pace to
 * compute from, so the free Endurance session has to be built on *some* pace or it cannot exist at
 * all — and the owner's decision is that Endurance is the free tier and therefore has to produce a
 * real, printable set list before anything is bought.
 *
 * 110 s/100 m (1:50) is a typical trained amateur's threshold pace: fast enough that the free
 * Endurance set is a genuine session rather than a strawman, slow enough that it is not a target a
 * lane swimmer is quietly set up to fail. Every interval target derived from it is printed with the
 * app saying the pace is the app's default (`home_pace_value_default`), never as the swimmer's own,
 * and the pace panel above the numbers says what swimming the two trials would change. The owner
 * should revisit this figure with a real distribution of trial times if one is ever collected; it is
 * declared here, once, so that is a one-line change.
 *
 * Declared as a top-level constant rather than beside the class because it is a *product* decision
 * that the screen, the ViewModel and any future test all read from the same place.
 */
const val DEFAULT_FREE_PACE_PER_100_SECONDS: Double = 110.0

/**
 * The pool lengths a swimmer can pick.
 *
 * The app's own world is metres, and these two are what a public pool is built as in every declared
 * locale's market. `new-slot.mjs`-style "one source of truth" applies here too: the list is a
 * program input, so it lives in Kotlin rather than being re-typed in the screen.
 */
val POOL_LENGTH_OPTIONS: List<Int> = listOf(25, 50)

/**
 * The minute budgets a swimmer can pick.
 *
 * A closed list rather than a free number field on purpose: a pool visit is planned in whole
 * quarters of an hour, and a stepper with a range is a control the swimmer has to operate instead of
 * a choice they make. Five minutes below the shortest option is a session the generator cannot fill
 * honestly, which is why the list starts at twenty.
 */
val MINUTES_OPTIONS: List<Int> = listOf(20, 30, 45, 60, 75, 90)

/** The budget the builder pre-fills: the length of an ordinary swim. */
const val DEFAULT_MINUTES_AVAILABLE: Int = 45

/**
 * The one focus the free tier computes.
 *
 * Endurance is the focus a swimmer can program from a default pace and still get a usable session
 * out of: its pace factor is the mildest of the five, so a slightly wrong pace produces a set that is
 * a little too hard or a little too easy rather than one nobody could swim. Speed and Threshold are
 * the two focuses where a wrong pace is actively harmful, and they are exactly the two the trials
 * exist to fix — which is why they are behind the unlock and this one is not.
 */
val FREE_FOCUS: Focus = Focus.ENDURANCE

/**
 * Everything the home surface renders, as one immutable value.
 *
 * The state is deliberately *the whole screen*: the constraints the swimmer has entered, the session
 * that was generated from them, where they are inside it, and what they are allowed to do with it.
 * Nothing the screen shows is derived inside the composable, so a change in any input moves through
 * one `HomeUiState` and the screen stays a function of its argument.
 *
 * Two pairs of fields look redundant and are not. `thresholdPacePer100Seconds` is the swimmer's own
 * measured pace (null until the trials are swum) while [activePacePer100Seconds] is the number the
 * generator is actually called with — the measured one when it exists, [DEFAULT_FREE_PACE_PER_100_SECONDS]
 * when it does not — so the screen can say which of the two produced the numbers on screen without
 * re-deciding it. `session` is the session *as generated*, and [isSessionCurrent] is whether the
 * constraints in the form still describe it, which is what makes "your constraints changed, rebuild"
 * a statement about data rather than a guess about whether the swimmer meant to change something.
 */
@Immutable
data class HomeUiState(
    // --- the constraints, exactly as the swimmer entered them -------------------------------
    /** The chosen pool length in metres; starts at the value the settings store publishes. */
    val poolLengthMeters: Int = SettingsStore.DEFAULT_POOL_LENGTH_METERS,
    /** The wall-clock budget for the whole session, in minutes. */
    val minutesAvailable: Int = DEFAULT_MINUTES_AVAILABLE,
    /** The day's focus. Anything but [FREE_FOCUS] is behind the unlock. */
    val focus: Focus = FREE_FOCUS,
    /** Strokes the swimmer does not want programmed today. Never all five: see [HomeNotice]. */
    val strokesToAvoid: Set<Stroke> = emptySet(),
    /** The equipment actually on the deck; the generator may program nothing else. */
    val gearOnDeck: Set<Gear> = emptySet(),

    // --- the entitlement and the pace it unlocks --------------------------------------------
    /** Whether the one-time unlock is owned, straight from `BillingManager.isUnlocked`. */
    val isUnlocked: Boolean = false,
    /** Whether the swimmer has swum both time trials, so a measured pace exists. */
    val hasTrials: Boolean = false,
    /** The measured threshold pace per 100 m, or `null` before the trials. */
    val thresholdPacePer100Seconds: Double? = null,

    // --- the session on screen --------------------------------------------------------------
    /** The generated (or restored) session, or `null` before the first build. */
    val session: Session? = null,
    /** Where the swimmer is inside [session]; always clamped against it before it is stored. */
    val position: SessionPosition = SessionPosition.Start,
    /** Whether the export/print sheet is open. The sheet itself is `ui/export/ExportSheet.kt`. */
    val isExportSheetVisible: Boolean = false,

    // --- things the screen has to say or do once -------------------------------------------
    /** A refused or failed action, shown until the swimmer changes an input. */
    val notice: HomeNotice? = null,
    /** A destination the ViewModel asked for, consumed and cleared by the route. */
    val navigationRequest: HomeNavigation? = null,
) {
    /**
     * The constraints the form currently describes.
     *
     * Built here rather than in the ViewModel so the "did the form change?" question and the
     * generator call read the same value. `SessionConstraints` is the domain's own type and the
     * generator's own parameter, so there is no second shape for the same five fields.
     */
    val constraints: SessionConstraints
        get() = SessionConstraints(
            poolLengthMeters = poolLengthMeters,
            minutesAvailable = minutesAvailable,
            focus = focus,
            strokesToAvoid = strokesToAvoid,
            gearOnDeck = gearOnDeck,
        )

    /**
     * The pace every target on screen was computed from.
     *
     * The measured threshold when the trials have been swum, and [DEFAULT_FREE_PACE_PER_100_SECONDS]
     * before that. It is an `Int`-free `Double` so it can be handed straight to
     * `SetGenerator.generateSession` and `PaceModel.intervalTargetSeconds` without a conversion that
     * could round twice.
     */
    val activePacePer100Seconds: Double
        get() = thresholdPacePer100Seconds ?: DEFAULT_FREE_PACE_PER_100_SECONDS

    /** Whether the targets on screen are the app's default pace rather than the swimmer's own. */
    val usesDefaultPace: Boolean
        get() = !hasTrials

    /**
     * Whether the session on screen was built from the constraints still in the form.
     *
     * Compared as values — the constraints and the pace — because those are the generator's whole
     * input. The session is deterministic, so an equal input means the same set list: the session is
     * current and nothing needs rebuilding.
     */
    val isSessionCurrent: Boolean
        get() = session?.let { current ->
            current.constraints == constraints && current.pacePer100Seconds == activePacePer100Seconds
        } == true

    /** The block the swimmer is on, or `null` when there is no session. */
    val currentBlock: SetBlock?
        get() = session?.blockAt(position)

    /** The section the swimmer is in, or `null` when there is no session. */
    val currentSectionKind: SectionKind?
        get() = session?.sectionKindAt(position)

    /** Whether the last repetition of the session has been reached. */
    val isSessionFinished: Boolean
        get() = session?.isAtEnd(position) ?: false
}

/**
 * A destination the home surface needs, which the route — not the screen — performs.
 *
 * The gating decision lives in the ViewModel, because whether a focus or a handoff is available is a
 * question about entitlement and the screen must not be able to answer it differently. The route
 * reads this, calls the matching navigation lambda and then clears it, so a state restoration cannot
 * replay a navigation that already happened.
 */
enum class HomeNavigation {
    /** The two time trials. Locked, because they are what the paid focuses are computed from. */
    PaceTest,

    /** Paste a workout a coach wrote. Locked: it is the paid half of the app's input. */
    CoachImport,

    /** The one-time unlock screen. */
    Unlock,
}

/**
 * Something the screen has to say about an action that did not happen.
 *
 * Deliberately not a snackbar: both of these are *refusals the swimmer can act on*, and the input
 * they have to change is on the same screen — a transient message would be gone before the swimmer
 * read the control they need to touch.
 */
enum class HomeNotice {
    /** The swimmer tried to avoid the last stroke still allowed; every session needs one. */
    KeepOneStroke,

    /** The generator refused the constraints. Reachable only through a state the form guards against. */
    GenerationFailed,
}

/**
 * One thing the swimmer did.
 *
 * Every control on the screen sends one of these and nothing else, so the ViewModel is the only place
 * that decides what a tap means — which is what keeps the free/paid boundary in one file instead of
 * spread across the layout.
 */
sealed interface HomeEvent {
    /** The swimmer chose a pool length, in metres. */
    data class PoolLengthSelected(val meters: Int) : HomeEvent

    /** The swimmer chose a budget, in minutes. */
    data class MinutesSelected(val minutes: Int) : HomeEvent

    /** The swimmer chose a focus. A locked focus becomes a request for the unlock. */
    data class FocusSelected(val focus: Focus) : HomeEvent

    /** The swimmer switched one stroke out of (or back into) the session. */
    data class StrokeAvoidToggled(val stroke: Stroke) : HomeEvent

    /** The swimmer switched one piece of equipment onto (or off) the deck. */
    data class GearToggled(val gear: Gear) : HomeEvent

    /** Build the session from the constraints as they stand. */
    data object GenerateRequested : HomeEvent

    /** One repetition further into the session. */
    data object AdvanceRequested : HomeEvent

    /** One repetition back. */
    data object RetreatRequested : HomeEvent

    /** Throw the session away and start from the constraints. */
    data object SessionDiscarded : HomeEvent

    /** The swimmer asked to send or print the set list. Locked. */
    data object ExportRequested : HomeEvent

    /** The sheet was dismissed by the swimmer, the gesture or after a completed share. */
    data object ExportDismissed : HomeEvent

    /** The swimmer asked to open a coach's workout. Locked. */
    data object ImportRequested : HomeEvent

    /**
     * The swimmer asked for the guided pace trials.
     *
     * Locked like the other paid half: the trials are what replaces the free tier's default pace, so
     * the decision whether this is the trials or the unlock screen stays here rather than in the
     * layout's `isUnlocked` branch.
     */
    data object PaceTestRequested : HomeEvent

    /** The swimmer asked for the unlock screen directly. */
    data object UnlockRequested : HomeEvent

    /** The route performed [HomeUiState.navigationRequest]; it can be forgotten. */
    data object NavigationHandled : HomeEvent

    /** The swimmer acknowledged the notice. */
    data object NoticeDismissed : HomeEvent
}

// ---------------------------------------------------------------------------------------------
// Advancing through a session: pure, on the domain types, and testable without Android
// ---------------------------------------------------------------------------------------------

/**
 * The block a position names, or `null` when the session has no such block.
 *
 * The position is clamped first, so this answers for a restored position as well as a fresh one: a
 * position stored against a session that has since changed cannot point at a line this session does
 * not have.
 */
internal fun Session.blockAt(position: SessionPosition): SetBlock? {
    val clamped = position.clampedTo(this)
    return sections.getOrNull(clamped.sectionIndex)?.blocks?.getOrNull(clamped.blockIndex)
}

/** The kind of section a position is in, or `null` when the session has no such section. */
internal fun Session.sectionKindAt(position: SessionPosition): SectionKind? =
    sections.getOrNull(position.clampedTo(this).sectionIndex)?.kind

/**
 * One repetition further, or the same position when there is nowhere further to go.
 *
 * The walk crosses blocks and sections rather than only counting repetitions, because a set list is
 * one continuous thing to the swimmer even though the model groups it: the last repetition of the
 * warm-up is followed by the first repetition of the main set, not by the end of the session.
 * Empty sections are stepped over rather than landed in — a session whose main set came out empty
 * would otherwise have a position that `clampedTo` refuses, which would silently send the swimmer
 * back to the start.
 */
internal fun Session.advance(position: SessionPosition): SessionPosition {
    val current = position.clampedTo(this)
    val section = sections.getOrNull(current.sectionIndex) ?: return current
    val block = section.blocks.getOrNull(current.blockIndex) ?: return current

    if (current.repetitionIndex + 1 < block.repetitions) {
        return current.copy(repetitionIndex = current.repetitionIndex + 1)
    }
    if (current.blockIndex + 1 < section.blocks.size) {
        return SessionPosition(current.sectionIndex, current.blockIndex + 1, 0)
    }
    for (index in (current.sectionIndex + 1) until sections.size) {
        if (sections[index].blocks.isNotEmpty()) return SessionPosition(index, 0, 0)
    }
    return current
}

/**
 * One repetition back, or the same position at the top of the session.
 *
 * Going back lands on the **last** repetition of the previous block rather than on its first,
 * because that is the line the swimmer would have swum next had they not moved on.
 */
internal fun Session.retreat(position: SessionPosition): SessionPosition {
    val current = position.clampedTo(this)
    val section = sections.getOrNull(current.sectionIndex) ?: return SessionPosition.Start

    if (current.repetitionIndex > 0) return current.copy(repetitionIndex = current.repetitionIndex - 1)
    if (current.blockIndex > 0) {
        val previous = section.blocks[current.blockIndex - 1]
        return SessionPosition(current.sectionIndex, current.blockIndex - 1, lastRepetitionOf(previous))
    }
    for (index in (current.sectionIndex - 1) downTo 0) {
        val candidate = sections[index]
        if (candidate.blocks.isEmpty()) continue
        val lastIndex = candidate.blocks.lastIndex
        return SessionPosition(index, lastIndex, lastRepetitionOf(candidate.blocks[lastIndex]))
    }
    return SessionPosition.Start
}

/**
 * Whether [position] is the session's last repetition.
 *
 * Asked by comparing a step forward with where the swimmer already is, so there is no second copy of
 * the "is this the end?" rule that could disagree with [advance].
 */
internal fun Session.isAtEnd(position: SessionPosition): Boolean =
    advance(position) == position.clampedTo(this)

/**
 * How many repetitions the swimmer has left, counting the one they are on.
 *
 * This is the number a swimmer actually wants mid-session; it is derived by walking the session
 * forward one repetition at a time rather than by storing a written-down remainder, so it cannot
 * drift from the session it belongs to. Sessions are tens of repetitions, so the walk is free.
 */
internal fun Session.remainingRepetitions(position: SessionPosition): Int {
    var remaining = 1
    var cursor = position.clampedTo(this)
    while (true) {
        val next = advance(cursor)
        if (next == cursor) return remaining
        remaining += 1
        cursor = next
    }
}

/** The ordinal (0-based) of the block a position names, across the whole session. */
internal fun Session.blockOrdinalAt(position: SessionPosition): Int {
    val clamped = position.clampedTo(this)
    var ordinal = 0
    for (index in 0 until clamped.sectionIndex.coerceAtLeast(0)) {
        ordinal += sections.getOrNull(index)?.blocks?.size ?: 0
    }
    return ordinal + clamped.blockIndex
}

/** The index of a block's last repetition, never negative even for a degenerate block. */
private fun lastRepetitionOf(block: SetBlock): Int = (block.repetitions - 1).coerceAtLeast(0)

/**
 * A duration as a swimmer reads it off a pace clock: `m:ss`, or `h:mm:ss` once there is an hour.
 *
 * Built by hand rather than through `String.format` on purpose. `String.format` without an explicit
 * locale formats in the device's locale, and a target time is a datum a swimmer compares against a
 * clock rather than prose: in a locale with non-ASCII decimal digits the interval target would render
 * in different glyphs than the one printed in the store description. The digits here are the ones the
 * pace clock shows, in every locale, and the separators are chosen rather than translated.
 *
 * Seconds are always two digits, minutes are not padded when there is no hour (`5:30`, not `05:30`),
 * which is the notation pool sets are written in.
 */
internal fun formatClock(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val hours = clamped / 3600
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    val secondsPart = seconds.toString().padStart(2, '0')
    val minutesPart = if (hours > 0) minutes.toString().padStart(2, '0') else minutes.toString()
    return if (hours > 0) "$hours:$minutesPart:$secondsPart" else "$minutesPart:$secondsPart"
}
