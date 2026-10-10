package com.gexetr.lumintralightplanner.ui.format

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Lumintra's formatting contract: the six functions every screen formats with, so a time, a night
 * label, a duration or an angle reads the same in the brief, the scene, the pass list and the share
 * card.
 *
 * These are pure functions — no `Context`, no Compose, no resource lookup — for two reasons. A
 * `ViewModel` can format a value it read from `domain/` without threading an `Activity` through
 * itself, and a JVM unit test can drive every branch without an instrumented device.
 *
 * Three decisions are deliberate and are stated here rather than discovered later:
 *
 *  * **Every function that produces words takes the locale it must speak.** Nothing reads the
 *    device default: a formatter that did would print an English night label inside a Russian
 *    screen, and the defect appears only after localisation.
 *  * **Numbers are formatted with [Locale.ROOT].** An almanac writes a measurement as `+12.4°`, and
 *    the separator is part of the notation rather than of the sentence, so it does not move with the
 *    interface language. `INTERFACES.md`'s examples are written the same way.
 *  * **The duration's unit words are a small table in this module, not ICU.**
 *    `android.icu.text.MeasureFormat` would localise them, but `android.icu` is a stub on the JVM
 *    and would throw `RuntimeException: Method not mocked` under `run-slot-tests.mjs`; two unit words
 *    are a smaller price than a formatter no unit test can call. [UnitWords] is the extension point
 *    for a language added later.
 *
 * Three glyphs below are notation rather than copy: the degree sign, an em dash for a value that is
 * absent or not finite, and the arrow between the two ends of a night. They are written as escapes
 * so that no source-code literal can drift, and they are deliberately identical in every locale.
 */
private const val DEGREE = "\u00B0"
private const val ABSENT = "\u2014"
private const val NIGHT_ARROW = "\u2192"

/** The three unit words a short duration is built from, in one locale's language. */
private class UnitWords(val hour: String, val minute: String, val second: String) {
    companion object {
        /**
         * The unit words for [locale].
         *
         * `ru` is spelled out because it is the slot's declared second language; every other locale
         * falls back to the SI-style abbreviations, which are legible in most of them. Adding a
         * language means adding one row here, not a second formatter.
         */
        fun of(locale: Locale): UnitWords = when (locale.language.lowercase(Locale.ROOT)) {
            "ru" -> UnitWords(hour = "\u0447", minute = "\u043C\u0438\u043D", second = "\u0441")
            else -> UnitWords(hour = "h", minute = "min", second = "s")
        }
    }
}

/**
 * A clock reading in [zoneId], written in [locale].
 *
 * [use24Hour] is passed in rather than read from the device: the brief has a 12/24-hour switch, and a
 * screen that shows both a 24-hour table and a 12-hour clock is a real state the setting exists for.
 */
fun formatTime(instant: Instant, zoneId: ZoneId, locale: Locale, use24Hour: Boolean): String {
    val pattern = if (use24Hour) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId).format(instant)
}

/**
 * The night's label, spanning midnight without repeating the month: `Tue 8 → Wed 9 Sep`.
 *
 * The month is written once when both ends are in the same month (the common case), and on both ends
 * when the night crosses a month boundary — a label that named only one month there would be wrong
 * rather than merely terse. Weekday and month names come from the JDK's own locale data, so a Russian
 * night reads `Вт 8 → Ср 9 сен`.
 */
fun formatNightLabel(sessionStart: Instant, sessionEnd: Instant, zoneId: ZoneId, locale: Locale): String {
    val start = ZonedDateTime.ofInstant(sessionStart, zoneId)
    val end = ZonedDateTime.ofInstant(sessionEnd, zoneId)
    val startWeekday = start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val endWeekday = end.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val startMonth = start.month.getDisplayName(TextStyle.SHORT, locale)
    val endMonth = end.month.getDisplayName(TextStyle.SHORT, locale)
    return when {
        start.toLocalDate() == end.toLocalDate() ->
            "$startWeekday ${start.dayOfMonth} $startMonth"

        start.year == end.year && start.month == end.month ->
            "$startWeekday ${start.dayOfMonth} $NIGHT_ARROW $endWeekday ${end.dayOfMonth} $startMonth"

        else ->
            "$startWeekday ${start.dayOfMonth} $startMonth $NIGHT_ARROW " +
                "$endWeekday ${end.dayOfMonth} $endMonth"
    }
}

/**
 * A whole-minute length such as `4 h 12 min`, in [locale]'s words.
 *
 * A duration shorter than a minute reports seconds rather than `0 min`, because a zero is
 * indistinguishable from a failure to compute. A negative duration is clamped to zero: the callers
 * pass an end minus a start, and an inversion there is a bug to see as `0 s`, not as `-3 min`.
 */
fun formatDurationShort(duration: Duration, locale: Locale): String {
    val safe = if (duration.isNegative) Duration.ZERO else duration
    val totalMinutes = safe.seconds / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    val words = UnitWords.of(locale)
    return when {
        hours > 0L && minutes > 0L -> "$hours ${words.hour} $minutes ${words.minute}"
        hours > 0L -> "$hours ${words.hour}"
        totalMinutes > 0L -> "$totalMinutes ${words.minute}"
        else -> "${safe.seconds} ${words.second}"
    }
}

/** Signed degrees with one decimal, the sign always present: `+12.4°`, `-3.0°`. */
fun formatSignedDegrees(degrees: Double): String {
    if (!degrees.isFinite()) return ABSENT
    val rounded = (degrees * 10.0).roundToLong() / 10.0
    // `-0.4` rounds to zero; the sign must not survive as `-0.0°`.
    val normalised = if (rounded == 0.0) 0.0 else rounded
    return String.format(Locale.ROOT, "%+.1f", normalised) + DEGREE
}

/** Degrees with one decimal and no sign: `12.4°`. */
fun formatDegrees(degrees: Double): String {
    if (!degrees.isFinite()) return ABSENT
    val rounded = (degrees * 10.0).roundToLong() / 10.0
    val normalised = if (rounded == 0.0) 0.0 else rounded
    return String.format(Locale.ROOT, "%.1f", normalised) + DEGREE
}

/**
 * A percentage from a `0..1` fraction, rounded to the whole percent: `62%`.
 *
 * The input is clamped rather than allowed to print `104%`: the callers pass an illuminated fraction
 * or a cloud-cover fraction, whose sources are already bounded, and a clamped reading is a smaller
 * lie than an impossible one.
 */
fun formatPercent(fraction: Double): String {
    if (!fraction.isFinite()) return ABSENT
    val clamped = fraction.coerceIn(0.0, 1.0)
    return "${(clamped * 100.0).roundToLong()}%"
}
