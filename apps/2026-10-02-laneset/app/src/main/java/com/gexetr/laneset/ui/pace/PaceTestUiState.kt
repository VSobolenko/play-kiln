package com.gexetr.laneset.ui.pace

import androidx.compose.runtime.Immutable
import com.gexetr.laneset.data.PaceProfile
import kotlin.math.roundToLong

/**
 * The guided pace test's state, as the screen reads it.
 *
 * The screen is a pure function of one of these: it never guesses where in the flow it is and it
 * never computes a pace. [profile] is the persisted truth (the two trials and the pace the store
 * derived from them) and [savedTrials] is the pair the swimmer has just finished, carried here so
 * the result renders on the same frame the save returns rather than waiting for the store's flow to
 * re-emit. The two are read through [resultTrials], which prefers the freshly saved pair and falls
 * back to what is on disk.
 *
 * [error] is a typed cause, never a sentence: every message the swimmer reads is a resource the
 * screen resolves, so the error copy is translated with the rest of the app
 * (`.cursor/rules/localization.mdc`).
 */
@Immutable
data class PaceTestUiState(
    /** The trials and the derived pace currently stored on the device. */
    val profile: PaceProfile = PaceProfile.EMPTY,
    /** The pair just saved in this session, or null when the result comes from disk. */
    val savedTrials: PaceProfile? = null,
    /** Which of the three states of the guided test is on screen. */
    val step: PaceTestStep = PaceTestStep.FOUR_HUNDRED,
    /** What the swimmer has typed into the 400 m field. */
    val time400Input: String = "",
    /** What the swimmer has typed into the 200 m field. */
    val time200Input: String = "",
    /** Something the swimmer must fix before the pace can be saved. */
    val error: PaceTestError? = null,
) {
    /**
     * The trials the result screen presents: the pair just saved, or the pair on disk.
     *
     * Null only when neither exists, which is the defensive branch the result screen reports rather
     * than rendering a hero panel full of holes.
     */
    val resultTrials: PaceProfile?
        get() = savedTrials?.takeIf { it.hasTrials } ?: profile.takeIf { it.hasTrials }
}

/** The three states of the guided test, in the order the swimmer walks them. */
enum class PaceTestStep {
    /** Enter the 400 m time — the slower of the two swims. */
    FOUR_HUNDRED,

    /** Enter the 200 m time — the faster one; the gap between the two sets the pace. */
    TWO_HUNDRED,

    /** The threshold pace, with the two trials that produced it. */
    RESULT,
}

/**
 * Why the entered pair cannot become a pace.
 *
 * A sealed type rather than a message, so the ViewModel can report the domain's refusal without
 * owning any user-facing copy, and so a new cause cannot be added without the screen's `when`
 * failing to compile.
 */
sealed interface PaceTestError {
    /** The entry could not be read as `minutes:seconds` or as a number of seconds. */
    data object Unreadable : PaceTestError

    /** The entry read as a time, but not a positive one. */
    data object NotPositive : PaceTestError

    /**
     * Both times read, but the pair cannot yield a positive pace — the 400 m is not slower than the
     * 200 m. Carries the two parsed times so the message can name them.
     */
    data class NotSlower(val time400Seconds: Double, val time200Seconds: Double) : PaceTestError
}

/** Everything the swimmer can do on the pace screen. */
sealed interface PaceTestEvent {
    /** The 400 m field changed. */
    data class Time400Changed(val value: String) : PaceTestEvent

    /** The 200 m field changed. */
    data class Time200Changed(val value: String) : PaceTestEvent

    /** The 400 m time is done; move to the 200 m step when it reads. */
    data object SubmitTime400 : PaceTestEvent

    /** The 200 m time is done; compute and save the pace when both times read. */
    data object SubmitTime200 : PaceTestEvent

    /** Go back to the 400 m field without leaving the test. */
    data object BackToTime400 : PaceTestEvent

    /** Re-run the flow, pre-filled with the trials already on disk. */
    data object Retry : PaceTestEvent

    /** Forget the trials and the pace derived from them. */
    data object Reset : PaceTestEvent
}

/**
 * Reads a swimmer's time as seconds, leniently but not silently.
 *
 * Swimmers read times as `minutes:seconds` — `6:40` — so that is the primary form; `6:40.5` keeps a
 * tenth, which is how a pace clock is judged; and a bare number is read as seconds, so `400` is the
 * same swim as `6:40`. Anything else returns null rather than throwing: the caller turns null into
 * the "that does not read as a time" message instead of a `NumberFormatException`.
 *
 * A `seconds` part of 60 or more is refused (`6:99` is not a time), and a three-part entry
 * (`6:40:10`) is refused rather than guessed at.
 */
internal fun parsePaceTime(input: String): Double? {
    val text = input.trim()
    if (text.isEmpty()) return null
    val parts = text.split(':')
    return when (parts.size) {
        1 -> text.toDoubleOrNull()
        2 -> {
            val minutes = parts[0].trim().toIntOrNull() ?: return null
            val seconds = parts[1].trim().toDoubleOrNull() ?: return null
            if (minutes < 0 || seconds < 0.0 || seconds >= 60.0) return null
            minutes * 60.0 + seconds
        }
        else -> null
    }
}

/**
 * Renders seconds back as the swimmer's own notation: `6:40`, and `6:40.5` when a tenth is present.
 *
 * The digits, the colon and the tenth are formatting, not copy: there is no translatable word in the
 * result, which is why this is not a resource. It is the exact inverse of [parsePaceTime] for every
 * value the two need to round-trip.
 */
internal fun formatPaceTime(seconds: Double): String {
    val tenths = (seconds * 10.0).roundToLong().coerceAtLeast(0L)
    val whole = tenths / 10L
    val fraction = tenths % 10L
    val minutes = whole / 60L
    val remainder = whole % 60L
    val base = "$minutes:${remainder.toString().padStart(2, '0')}"
    return if (fraction == 0L) base else "$base.$fraction"
}
