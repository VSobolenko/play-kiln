package com.gexetr.lumintralightplanner.ui.share

import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.CloudReport
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.FieldOfView
import com.gexetr.lumintralightplanner.domain.KeyTimeBand
import com.gexetr.lumintralightplanner.domain.LensPreset
import com.gexetr.lumintralightplanner.domain.MoonPhaseName
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.PassVisibility
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.SessionBrief
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.TargetInterval
import com.gexetr.lumintralightplanner.domain.TargetWindow
import com.gexetr.lumintralightplanner.ui.format.formatDurationShort
import com.gexetr.lumintralightplanner.ui.format.formatNightLabel
import com.gexetr.lumintralightplanner.ui.format.formatSignedDegrees
import com.gexetr.lumintralightplanner.ui.format.formatTime
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * The text brief that leaves the device.
 *
 * It is a **pure** composer: it takes the brief and the locale's words as arguments, reads no asset,
 * no Context and no clock of its own, and can therefore be asserted on the JVM with fixed numbers —
 * which is the only way the "carries the location, the windows, the Moon, the cloud outlook with its
 * age and the target windows" requirement can be tested rather than eyeballed.
 *
 * Two rules it must never break, both from `specs/share-brief/spec.md`:
 *
 * 1. **A forecast is never called a measurement.** Every cloud line carries the source and the age of
 *    the reading it came from, and the block header says "forecast, not a measurement".
 * 2. **Nothing is stated that was not computed.** A night with no dark window says so; a target with
 *    no stated size states that; a target that never rises says that instead of an empty window.
 *
 * ### What it does when the payload is too long
 *
 * The cap is [MAX_CHARS]. The composer does not truncate mid-sentence: it walks a **reduction ladder**
 * that shortens the three lists (cloud hours, target windows, satellite passes) and states how many
 * entries were left out with `share_text_and_more`. The first assembly that fits is the payload, so a
 * long night loses list entries — never a fact, and never the credit line.
 */
object BriefShareText {

    /** The payload cap. Google's own limits are far higher; this one is ours and it is stated. */
    const val MAX_CHARS: Int = 8_000

    /** The list caps of the first assembly: twelve target windows and six passes (spec). */
    const val MAX_TARGETS: Int = 12
    const val MAX_PASSES: Int = 6

    /** The cloud hours named individually before the rest are summarised as a mean. */
    const val MAX_CLOUD_HOURS: Int = 6

    /**
     * The ladder, longest first. Each step is `cloudHours to (targets to passes)`; the last step names
     * no list entries at all, so a pathological night still produces a payload under the cap.
     */
    private val LADDER: List<Triple<Int, Int, Int>> = listOf(
        Triple(MAX_CLOUD_HOURS, MAX_TARGETS, MAX_PASSES),
        Triple(3, 9, 4),
        Triple(0, 6, 2),
        Triple(0, 3, 1),
        Triple(0, 1, 0),
        Triple(0, 0, 0),
    )

    /**
     * Builds the payload.
     *
     * @param strings resolves a string resource id with its arguments — the caller supplies
     *   `context.getString(id, *args)`, so this file needs no Context and every word it emits comes
     *   from `values/` or `values-ru/` rather than from a literal.
     */
    fun build(
        brief: SessionBrief,
        objects: Map<String, SkyObject>,
        lens: LensPreset?,
        zoneId: ZoneId,
        locale: Locale,
        use24Hour: Boolean,
        strings: (Int, List<Any>) -> String,
    ): String {
        val composer = Composer(brief, objects, lens, zoneId, locale, use24Hour, strings)
        var shortest = ""
        for (step in LADDER) {
            val text = composer.assemble(step.first, step.second, step.third)
            shortest = text
            if (text.length <= MAX_CHARS) return text
        }
        return shortest
    }

    /** The character count the Share screen shows beside the preview. */
    fun characterCount(text: String): Int = text.length

    /**
     * The brief as blocks, at the payload's own list caps. The postcard composes from these, so a card
     * and a message shared side by side cannot disagree about a reading.
     */
    fun blocks(
        brief: SessionBrief,
        objects: Map<String, SkyObject>,
        lens: LensPreset?,
        zoneId: ZoneId,
        locale: Locale,
        use24Hour: Boolean,
        strings: (Int, List<Any>) -> String,
    ): ShareBlocks = Composer(brief, objects, lens, zoneId, locale, use24Hour, strings)
        .blocks(MAX_CLOUD_HOURS, MAX_TARGETS, MAX_PASSES)
}

/**
 * The payload's parts, in the order it prints them.
 *
 * Every block is a `List<String>` of finished sentences: nothing here needs a Context, a formatter or a
 * second decision, which is what lets the postcard and the message body share them.
 */
data class ShareBlocks(
    val header: List<String>,
    val dark: List<String>,
    val twilight: List<String>,
    val moon: List<String>,
    val targets: List<String>,
    val passes: List<String>,
    val cloud: List<String>,
    val lens: List<String>,
    val credit: List<String>,
) {
    /** Every line, in order — the payload [BriefShareText.build] joins with newlines. */
    fun flatten(): List<String> = header + dark + twilight + moon + targets + passes + cloud + lens + credit
}

/**
 * One assembly pass. Held as a class rather than a long parameter list so the per-list steps read as
 * what they are: the same payload with a shorter list.
 */
private class Composer(
    private val brief: SessionBrief,
    private val objects: Map<String, SkyObject>,
    private val lens: LensPreset?,
    private val zoneId: ZoneId,
    private val locale: Locale,
    private val use24Hour: Boolean,
    private val strings: (Int, List<Any>) -> String,
) {

    private var omittedTargets: Int = 0
    private var omittedPasses: Int = 0
    private var omittedCloudHours: Int = 0

    private fun s(id: Int, vararg args: Any): String = strings(id, args.toList())

    private fun time(instant: Instant): String = formatTime(instant, zoneId, locale, use24Hour)

    /** "22:41 – 22:47": two clock readings, never a duration the reader has to convert. */
    private fun clockSpan(span: NightSpan): String = time(span.startUtc) + " – " + time(span.endUtc)

    private fun num(value: Double, digits: Int): String = String.format(locale, "%.${digits}f", value)

    fun assemble(cloudHours: Int, targetLimit: Int, passLimit: Int): String =
        blocks(cloudHours, targetLimit, passLimit).flatten().joinToString("\n")

    /**
     * The same sentences as [assemble], kept as **named blocks** rather than one string.
     *
     * The postcard lays the brief out on a canvas instead of in a message body, so it needs the parts
     * rather than the whole: a card cannot wrap a 6 000-character message, and a second composer would
     * be a second chance for the card and the text to state different readings. Blocks are also the
     * only shape in which "the postcard's overlay agrees with the words" can be checked.
     */
    fun blocks(cloudHours: Int, targetLimit: Int, passLimit: Int): ShareBlocks {
        omittedTargets = 0
        omittedPasses = 0
        omittedCloudHours = 0
        return ShareBlocks(
            header = headerBlock(),
            dark = darkBlock(),
            twilight = twilightBlock(),
            moon = moonBlock(),
            targets = targetBlock(targetLimit),
            passes = passBlock(passLimit),
            cloud = cloudBlock(cloudHours),
            lens = lensBlock(),
            credit = listOf(s(R.string.share_text_credit)),
        )
    }

    // ---- header -----------------------------------------------------------------------------------

    private fun headerBlock(): List<String> = buildList {
        add(s(R.string.share_text_header, brief.spotName))
        add("")
        add(s(R.string.share_text_spot, brief.spotName))
        add(
            s(
                R.string.share_text_position,
                formatSignedDegrees(brief.latitudeDeg),
                formatSignedDegrees(brief.longitudeDeg),
            ),
        )
        add(s(R.string.share_text_zone, brief.tzId))
        add(s(R.string.share_text_night, nightLabel()))
    }

    private fun nightLabel(): String =
        formatNightLabel(brief.session.startUtc, brief.session.endUtc, zoneId, locale)

    // ---- dark and light windows -------------------------------------------------------------------

    private fun darkBlock(): List<String> = buildList {
        val report = brief.darkWindow
        add(s(R.string.share_text_dark_kind, darkKindWord(report.kind)))
        when (report.kind) {
            DarkWindowKind.DARK -> {
                report.darkWindows.forEachIndexed { index, window ->
                    val line = if (index == 0) {
                        s(R.string.share_text_dark_window, time(window.startUtc), time(window.endUtc))
                    } else {
                        s(R.string.share_text_dark_window_second, time(window.startUtc), time(window.endUtc))
                    }
                    add(line)
                }
            }

            DarkWindowKind.POLAR_NIGHT -> {
                val window = report.bestWindow ?: brief.session
                add(s(R.string.share_text_polar_night, time(window.startUtc), time(window.endUtc)))
            }

            DarkWindowKind.POLAR_DAY -> add(s(R.string.share_text_polar_day))

            DarkWindowKind.NO_ASTRO_DARK -> add(
                s(
                    R.string.share_text_no_astro_dark,
                    deepestTwilightWord(),
                    num(report.sunAltMinDeg, 1),
                    time(report.sunAltMinInstant),
                ),
            )

            DarkWindowKind.NO_DARK -> add(
                s(R.string.share_text_no_dark, num(report.sunAltMinDeg, 1), time(report.sunAltMinInstant)),
            )
        }
    }

    /** The light windows: sunset, sunrise, then the three twilight bands as the brief's own rows. */
    private fun twilightBlock(): List<String> = buildList {
        brief.sunriseSunset.sunsetUtc?.let { add(s(R.string.share_text_sunset, time(it))) }
        brief.sunriseSunset.sunriseUtc?.let { add(s(R.string.share_text_sunrise, time(it))) }
        for (band in TWILIGHT_BANDS) {
            val row = brief.keyTimeRow(band.first) ?: continue
            for (span in row.intervals) {
                add(s(band.second, time(span.startUtc), time(span.endUtc)))
            }
        }
    }

    /** The deepest twilight actually reached, chosen by which band has a row — never by guessing. */
    private fun deepestTwilightWord(): String = when {
        brief.keyTimeRow(KeyTimeBand.ASTRONOMICAL)?.occurs == true ->
            s(R.string.share_text_twilight_astronomical_short)

        brief.keyTimeRow(KeyTimeBand.NAUTICAL)?.occurs == true -> s(R.string.share_text_twilight_nautical_short)
        else -> s(R.string.share_text_twilight_civil_short)
    }

    // ---- the Moon ---------------------------------------------------------------------------------

    private fun moonBlock(): List<String> {
        val moon = brief.moon
        val percent = Math.round(moon.illuminatedFraction * 100.0)
        val place = s(R.string.share_text_moon_alt_az, num(moon.altitudeDeg, 1), num(moon.azimuthDeg, 1))
        return listOf(s(R.string.share_text_moon, percent, phaseWord(moon.phaseName), place))
    }

    // ---- targets ----------------------------------------------------------------------------------

    private fun targetBlock(limit: Int): List<String> = buildList {
        add(s(R.string.share_text_targets_header))
        val ordered = brief.targetWindows.keys.sortedWith(targetOrder())
        if (ordered.isEmpty()) {
            add(s(R.string.share_text_no_target))
            return@buildList
        }
        ordered.take(limit).forEach { id -> add(targetLine(id)) }
        val omitted = ordered.size - minOf(limit, ordered.size)
        omittedTargets = omitted
        if (omitted > 0) add(s(R.string.share_text_and_more, omitted))
    }

    /**
     * The catalogue's own order, which is the only order this list has: magnitude ascending,
     * magnitude-unknown rows last, then by id. A window is never re-ordered by how long it lasts.
     */
    private fun targetOrder(): Comparator<String> = Comparator { left, right ->
        val a = objects[left]
        val b = objects[right]
        when {
            a != null && b != null -> SkyObject.CATALOGUE_ORDER.compare(a, b)
            a != null -> -1
            b != null -> 1
            else -> left.compareTo(right)
        }
    }

    private fun targetLine(id: String): String {
        val obj = objects[id]
        val designation = obj?.designation ?: id
        val name = obj?.let { localisedName(it) } ?: id
        return when (val window = brief.targetWindows[id]) {
            null -> s(R.string.share_text_target_never_rises, name, designation)

            is TargetWindow.NeverRises -> s(R.string.share_text_target_never_rises, name, designation)

            is TargetWindow.Circumpolar -> s(
                R.string.share_text_target_circumpolar,
                name,
                designation,
                num(window.minimumAltitudeDeg, 1),
                time(window.atInstant),
            )

            is TargetWindow.Visible -> {
                val interval = window.bestInterval
                if (interval == null) {
                    s(R.string.share_text_target_never_rises, name, designation)
                } else {
                    intervalLine(names = name, designation = designation, interval = interval)
                }
            }
        }
    }

    private fun intervalLine(names: String, designation: String, interval: TargetInterval): String =
        if (interval.isPartial) {
            s(
                R.string.share_text_target_partial,
                names,
                designation,
                time(interval.startUtc),
                time(interval.endUtc),
                num(interval.peakAltitudeDeg, 1),
            )
        } else {
            s(
                R.string.share_text_target_window,
                names,
                designation,
                time(interval.startUtc),
                time(interval.endUtc),
                num(interval.peakAltitudeDeg, 1),
            )
        }

    /** `nameRu` is preferred exactly when the payload is composed for a Russian reader. */
    private fun localisedName(obj: SkyObject): String =
        if (locale.language == "ru" && !obj.nameRu.isNullOrBlank()) obj.nameRu else obj.name

    // ---- satellite passes -------------------------------------------------------------------------

    private fun passBlock(limit: Int): List<String> = buildList {
        add(s(R.string.share_text_passes_header))
        val passes = brief.visiblePasses
        if (passes.isEmpty()) {
            add(s(R.string.share_text_no_pass))
            return@buildList
        }
        passes.take(limit).forEach { add(passLine(it)) }
        val omitted = passes.size - minOf(limit, passes.size)
        omittedPasses = omitted
        if (omitted > 0) add(s(R.string.share_text_and_more, omitted))
    }

    private fun passLine(pass: SatellitePass): String = s(
        R.string.share_text_pass,
        pass.satelliteName,
        clockSpan(pass.span),
        num(pass.maxElevationDeg, 1),
        passVisibilityWord(pass),
        formatDurationShort(pass.elementSetAge.age, locale),
    )

    private fun passVisibilityWord(pass: SatellitePass): String = when (pass.visibility) {
        PassVisibility.OBSERVABLE -> s(R.string.share_text_pass_visible)
        PassVisibility.IN_EARTH_SHADOW -> s(R.string.share_text_pass_shadow)
        PassVisibility.TOO_BRIGHT -> s(R.string.share_text_pass_bright)
    }

    // ---- cloud ------------------------------------------------------------------------------------

    private fun cloudBlock(hours: Int): List<String> = buildList {
        add(s(R.string.share_text_cloud_header))
        val report = brief.cloud
        if (report == null) {
            add(s(R.string.share_text_no_forecast))
            return@buildList
        }
        add(
            s(
                R.string.share_text_cloud_source,
                CloudReport.SOURCE_LABEL,
                time(report.age.fetchedAtUtc),
                formatDurationShort(report.age.age, locale),
            ),
        )
        val within = report.hoursWithin(brief.session)
        within.take(hours).forEach { hour ->
            add(s(R.string.share_text_cloud_hour, time(hour.hourUtc), hour.cloudCoverPercent))
        }
        val omitted = within.size - minOf(hours, within.size)
        omittedCloudHours = omitted
        val meanSpan = brief.darkWindow.bestWindow
        val mean = report.meanCloudPercentWithin(meanSpan ?: brief.session)
        if (mean != null) {
            add(
                if (meanSpan != null) {
                    s(R.string.share_text_cloud_mean, mean)
                } else {
                    s(R.string.share_text_cloud_mean_session, mean)
                },
            )
        }
        if (omitted > 0) add(s(R.string.share_text_and_more, omitted))
    }

    // ---- lens -------------------------------------------------------------------------------------

    private fun lensBlock(): List<String> {
        val lens = this.lens ?: return emptyList()
        val fov = FieldOfView.of(lens)
        return listOf(
            s(R.string.share_text_lens_header),
            s(
                R.string.share_text_lens,
                num(lens.focalLengthMm, 0),
                num(lens.sensorWidthMm, 1),
                num(lens.sensorHeightMm, 1),
                num(fov.fovH, 1),
                num(fov.fovV, 1),
            ),
        )
    }

    // ---- words ------------------------------------------------------------------------------------

    private fun darkKindWord(kind: DarkWindowKind): String = when (kind) {
        DarkWindowKind.DARK -> s(R.string.share_dark_kind_dark)
        DarkWindowKind.POLAR_DAY -> s(R.string.share_dark_kind_polar_day)
        DarkWindowKind.POLAR_NIGHT -> s(R.string.share_dark_kind_polar_night)
        DarkWindowKind.NO_ASTRO_DARK -> s(R.string.share_dark_kind_no_astro)
        DarkWindowKind.NO_DARK -> s(R.string.share_dark_kind_no_dark)
    }

    private fun phaseWord(phase: MoonPhaseName): String = when (phase) {
        MoonPhaseName.NEW_MOON -> s(R.string.share_phase_new_moon)
        MoonPhaseName.WAXING_CRESCENT -> s(R.string.share_phase_waxing_crescent)
        MoonPhaseName.FIRST_QUARTER -> s(R.string.share_phase_first_quarter)
        MoonPhaseName.WAXING_GIBBOUS -> s(R.string.share_phase_waxing_gibbous)
        MoonPhaseName.FULL_MOON -> s(R.string.share_phase_full_moon)
        MoonPhaseName.WANING_GIBBOUS -> s(R.string.share_phase_waning_gibbous)
        MoonPhaseName.LAST_QUARTER -> s(R.string.share_phase_last_quarter)
        MoonPhaseName.WANING_CRESCENT -> s(R.string.share_phase_waning_crescent)
    }

    private companion object {
        /** The three twilight bands, in the order the payload lists them, with their own key. */
        val TWILIGHT_BANDS: List<Pair<KeyTimeBand, Int>> = listOf(
            KeyTimeBand.CIVIL to R.string.share_text_civil,
            KeyTimeBand.NAUTICAL to R.string.share_text_nautical,
            KeyTimeBand.ASTRONOMICAL to R.string.share_text_astronomical,
        )
    }
}
