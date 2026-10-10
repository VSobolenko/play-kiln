package com.gexetr.lumintralightplanner.ui.night

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.domain.CloudReport
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.ui.theme.LumintraColors
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.min

/**
 * One stop of the 96-stop strip.
 *
 * The strip's colour is the **sky colour at the stop**, not a legend swatch: it is
 * `sRGB( tonemap( E(ε_i) · LUT_A(ε_i, 12°) ) )` — the antisolar table evaluated at the solar altitude
 * of that instant and at the strip's fixed view elevation. Two facts follow and both matter: the strip
 * is a *rendering of the same LUT the scene draws*, so the two can never disagree, and the strip is
 * therefore a true sky at every stop — a dark-window stop is dark because the Sun is below −18°, never
 * because the strip is painted black by a boolean.
 */
@Immutable
data class TimelineStop(
    /** `0..95`. */
    val index: Int,
    /** The scrub value this stop resolves to, `i / (STOP_COUNT − 1)`. */
    val scrub: Double,
    val instantUtc: Instant,
    /** The Sun's **geometric** altitude, the LUT's ε axis. */
    val sunAltitudeDeg: Double,
    /** The evaluated sky colour at this stop. */
    val skyColor: Color,
    /** The instant lies inside a window the Sun spends at or below −18.0°. */
    val inDarkWindow: Boolean,
    /** The instant lies inside a window with no Moon to interfere (down, or ≤ 10 % lit). */
    val inMoonlessWindow: Boolean,
    /** The Moon's centre is above the geometric horizon at this stop. */
    val moonUp: Boolean,
    /** The forecast's total cloud cover at this hour, or `null` when no hour covers it. */
    val cloudPercent: Int?,
)

/**
 * The sampled strip, plus the window geometry the strip draws over it.
 *
 * Every window is kept as the **domain span it came from** rather than as an `x` range, so the strip's
 * geometry is derived at draw time from the span's own fraction of the session and cannot drift from
 * the answer the brief states.
 */
@Immutable
class TimelineStripModel(
    val session: NightSpan,
    val stops: List<TimelineStop>,
    val darkWindows: List<NightSpan>,
    val moonlessWindows: List<NightSpan>,
    val moonRiseUtc: Instant?,
    val moonSetUtc: Instant?,
) {

    init {
        require(stops.size == STOP_COUNT) {
            "the strip is $STOP_COUNT stops by definition, got ${stops.size}"
        }
        require(stops.first().instantUtc == session.startUtc) {
            "stop 0 must be the session start"
        }
        require(stops.last().instantUtc == session.endUtc) {
            "stop ${STOP_COUNT - 1} must be the session end"
        }
    }

    /** True when the night has no dark window at all, so the band must not be drawn. */
    val hasDarkWindow: Boolean get() = darkWindows.any { !it.isEmpty }

    val hasMoonlessWindow: Boolean get() = moonlessWindows.any { !it.isEmpty }

    /** The stop nearest a scrub value; used by the readout, never to move the scrub. */
    fun nearestStop(scrub: Double): TimelineStop =
        stops[(scrub.coerceIn(0.0, 1.0) * (STOP_COUNT - 1)).toInt().coerceIn(0, STOP_COUNT - 1)]

    /** The horizontal fraction `[0, 1]` an instant sits at inside the session window. */
    fun fractionOf(instantUtc: Instant): Float = session.fractionOf(instantUtc).toFloat().coerceIn(0f, 1f)

    /**
     * The horizontal range a span covers, or `null` when it lies outside the session.
     *
     * A span's own start is **inside** it and its own end is **outside** it: every window here is
     * half-open `[start, end)`, which is the one convention this app attributes instants by — an
     * instant exactly on a boundary belongs to the interval that opens there, so two adjacent windows
     * neither overlap by a pixel nor leave a gap between them.
     */
    fun rangeOf(span: NightSpan): Pair<Float, Float>? {
        val a = fractionOf(span.startUtc)
        val b = fractionOf(span.endUtc)
        if (b <= 0f || a >= 1f) return null
        return a.coerceIn(0f, 1f) to b.coerceIn(0f, 1f)
    }

    companion object {
        /** The strip's stop count. Fixed by the night-scene capability. */
        const val STOP_COUNT: Int = 96

        /**
         * The strip's fixed view elevation, in degrees.
         *
         * The strip is one sample row of the sky, and the row it samples is the **antisolar meridian at
         * 12°** — low enough to be inside the twilight glow's own band, high enough not to be the
         * silhouette. It never changes with the camera: a strip whose elevation followed the camera
         * would redraw for a reason that is not a change in the night.
         */
        const val VIEW_ELEVATION_DEG: Double = 12.0

        /** The instant a half-open span is tested against. */
        fun covers(span: NightSpan, instantUtc: Instant): Boolean =
            !instantUtc.isBefore(span.startUtc) && instantUtc.isBefore(span.endUtc)

        /** A window list's membership for one instant, with the closing instant attributed to the last stop. */
        fun coversAny(windows: List<NightSpan>, instantUtc: Instant, isLastStop: Boolean): Boolean {
            if (windows.isEmpty()) return false
            if (windows.any { covers(it, instantUtc) }) return true
            return isLastStop && windows.any { it.endUtc == instantUtc }
        }
    }
}

/**
 * The sampler that turns a night into its 96 stops.
 *
 * It is a **pure function of the session, the solar altitude and the LUT** — no clock, no location, no
 * network — so the strip can be sampled in a unit test from fixed numbers, and a JVM test can assert
 * the darkest stop without a device.
 *
 * [sunAltitudeDeg] is passed in rather than computed here because the Sun's geometric altitude at an
 * arbitrary instant is the ephemeris's job. It is called once per stop, so the whole strip costs 96
 * evaluations and never a search.
 */
object TimelineStripSampler {

    /** The one threshold this sampler knows, because it is the one the strip marks. */
    const val DARK_THRESHOLD_DEG: Double = -18.0

    fun sample(
        session: NightSpan,
        lut: TwilightLut,
        sunAltitudeDeg: (Instant) -> Double,
        darkWindows: List<NightSpan>,
        moonlessWindows: List<NightSpan>,
        moonHorizontal: (Instant) -> Double,
        cloud: CloudReport?,
        viewElevationDeg: Double = TimelineStripModel.VIEW_ELEVATION_DEG,
    ): TimelineStripModel {
        val n = TimelineStripModel.STOP_COUNT
        val stops = ArrayList<TimelineStop>(n)
        val span = session.duration.toMillis().coerceAtLeast(1L)
        for (i in 0 until n) {
            val scrub = i.toDouble() / (n - 1).toDouble()
            val instant = session.startUtc.plusMillis((span * scrub).toLong())
            val sunAlt = sunAltitudeDeg(instant)
            val isLast = i == n - 1
            // The LUT is evaluated at (ε, view altitude) and nowhere else: the strip's colour is the
            // same pure function the scene's layer 1 is, so "the strip is a true sky" is a consequence
            // of the call, not a claim about it.
            val colour = lut.sampleDisplay(sunAlt, viewElevationDeg)
            stops.add(
                TimelineStop(
                    index = i,
                    scrub = scrub,
                    instantUtc = instant,
                    sunAltitudeDeg = sunAlt,
                    skyColor = colour,
                    inDarkWindow = TimelineStripModel.coversAny(darkWindows, instant, isLast),
                    inMoonlessWindow = TimelineStripModel.coversAny(moonlessWindows, instant, isLast),
                    moonUp = moonHorizontal(instant) > 0.0,
                    cloudPercent = cloudPercentAt(cloud, instant),
                ),
            )
        }
        return TimelineStripModel(
            session = session,
            stops = stops,
            darkWindows = darkWindows,
            moonlessWindows = moonlessWindows,
            moonRiseUtc = crossingsOf(moonHorizontal, session, rising = true),
            moonSetUtc = crossingsOf(moonHorizontal, session, rising = false),
        )
    }

    /** The forecast's own hour for an instant: the latest hour at or before it. */
    fun cloudPercentAt(cloud: CloudReport?, instantUtc: Instant): Int? {
        val hours = cloud?.hours ?: return null
        var best: Int? = null
        var bestInstant: Instant? = null
        for (hour in hours) {
            if (hour.hourUtc.isAfter(instantUtc)) continue
            val at = bestInstant
            if (at == null || hour.hourUtc.isAfter(at)) {
                best = hour.cloudCoverPercent
                bestInstant = hour.hourUtc
            }
        }
        return best
    }

    /** The Moon's first rise or set inside the session, from the sampled altitude. */
    private fun crossingsOf(
        moonAltitudeDeg: (Instant) -> Double,
        session: NightSpan,
        rising: Boolean,
    ): Instant? {
        val step = Duration.ofMinutes(5)
        var previous = moonAltitudeDeg(session.startUtc) > 0.0
        var t = session.startUtc.plus(step)
        while (t.isBefore(session.endUtc)) {
            val current = moonAltitudeDeg(t) > 0.0
            if (current != previous && current == rising) return t
            previous = current
            t = t.plus(step)
        }
        return null
    }
}

/**
 * The 96-stop timeline strip: the night's own sky as a row, with the dark window banded over it, the
 * moonless window bracketed under it, the Moon's rise and set ticked, and the cloud forecast as its own
 * thin row.
 *
 * ## The scrub
 *
 * Dragging anywhere on the strip moves the instant and the drag is **never** eased; a tap reports its
 * target for the screen to ease to; lifting the finger reports the release so the screen can run its
 * bounded settle. The strip **reports** a scrub value and never owns one: `onScrub` is the only way
 * the value moves, and [scrubValue] is the only way the strip learns where it is. That is what keeps
 * the gesture from feeding back into the frame it is computing, and it is why no easing, no
 * `Animatable` and no interpolation lives in this file.
 *
 * ## Accessibility
 *
 * The strip is a slider to TalkBack, not an image: it carries
 * [androidx.compose.ui.semantics.progressBarRangeInfo] and a
 * [androidx.compose.ui.semantics.setProgress] action, so the gesture has a named equivalent. The whole
 * control is at least 48 dp tall — the 96-stop sky row, the cloud row and the marker lane together
 * exceed it — so the drag target is never a hairline.
 */
@Composable
fun TimelineStrip(
    model: TimelineStripModel,
    scrubValue: Double,
    onScrub: (Double) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String,
    /**
     * The finger lifted: the screen runs its **bounded settle** and the scene stops drawing when it
     * ends. The strip itself never settles - it reports the release and nothing else.
     */
    onScrubRelease: () -> Unit = {},
    /**
     * A tap-to-advance: the screen eases to this value over at most 200 ms with
     * `emphasized-decelerate`. It is separate from [onScrub] on purpose - a drag must put the
     * pointer's own value into the drawn frame with no interpolation between them, and a tap is the
     * one gesture the capability does ease.
     */
    onScrubEased: (Double) -> Unit = onScrub,
) {
    val palette = LumintraTheme.colors
    // The gesture detectors live inside `pointerInput(model.session)`, which is **not** restarted by a
    // recomposition that produces a new lambda. Reading the callbacks through `rememberUpdatedState` is
    // therefore what keeps the strip from holding a stale one - and keying `pointerInput` on the
    // callbacks instead would restart the detector in the middle of a drag, which would drop it.
    val currentScrub by rememberUpdatedState(onScrub)
    val currentEased by rememberUpdatedState(onScrubEased)
    val currentRelease by rememberUpdatedState(onScrubRelease)
    val scrubAction = remember {
        { value: Float ->
            currentEased(value.toDouble())
            true
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = scrubValue.toFloat().coerceIn(0f, 1f),
                    range = 0f..1f,
                    steps = TimelineStripModel.STOP_COUNT - 2,
                )
                setProgress { value ->
                    scrubAction(value)
                }
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(model.session) {
                    // A tap is a jump, and the jump is eased by the screen: the strip writes no value
                    // of its own, it reports the target and the screen animates to it.
                    detectTapGestures { offset ->
                        if (size.width > 0f) currentEased((offset.x / size.width).toDouble())
                    }
                }
                .pointerInput(model.session) {
                    detectDragGestures(
                        onDragEnd = { currentRelease() },
                        onDragCancel = { currentRelease() },
                    ) { change, _ ->
                        // Uneased, by requirement: the value written here is the value drawn in this
                        // same frame, so no `animateTo` sits between the pointer delta and the pixels.
                        if (size.width > 0f) {
                            currentScrub((change.position.x / size.width).toDouble())
                        }
                    }
                },
        ) {
            drawTimelineStrip(model, scrubValue.toFloat().coerceIn(0f, 1f), palette)
        }
        // The end labels, the dark-window times and the cloud reading are rendered by the screen as
        // text beside the strip, never painted into the canvas: a string drawn with a native Paint is a
        // literal no resource table can translate, and `values-ru/` could not reach it.
    }
}

/**
 * The strip's pixels.
 *
 * Rows, top to bottom: the sky row (24 dp), the dark-window band inside it, the marker lane (16 dp) and
 * the cloud row (16 dp). Everything is drawn from [TimelineStripModel] — there is no colour literal and
 * no instant literal in here, and a night with no dark window draws no band at all.
 */
private fun DrawScope.drawTimelineStrip(
    model: TimelineStripModel,
    scrubValue: Float,
    palette: LumintraColors,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val skyBottom = h * (24f / 56f)
    val laneBottom = h * (40f / 56f)
    val cloudTop = laneBottom
    val cloudHeight = h - cloudTop
    val stops = model.stops
    val moonPath = Path()

    // --- backdrop frame ---------------------------------------------------------------------
    drawRect(
        color = palette.hairline.copy(alpha = 0.22f),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
        style = Stroke(width = 1f),
    )

    // --- the night's own sky, stop by stop ----------------------------------------------------
    for (i in 0 until stops.size - 1) {
        val a = stops[i]
        val b = stops[i + 1]
        val left = a.scrub.toFloat() * w
        val right = b.scrub.toFloat() * w
        val width = (right - left).coerceAtLeast(0.5f)
        val horizonTone = lerp(a.skyColor, palette.skyHorizon, 0.42f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    lerp(a.skyColor, Color.White, 0.06f),
                    a.skyColor,
                    horizonTone,
                ),
                startY = 0f,
                endY = skyBottom,
            ),
            topLeft = Offset(left, 0f),
            size = Size(width, skyBottom),
        )
    }

    drawHourTicks(model, w, skyBottom, laneBottom, cloudTop, h, palette)

    // --- the dark-window band ----------------------------------------------------------------
    if (model.hasDarkWindow) {
        for (span in model.darkWindows) {
            val range = model.rangeOf(span) ?: continue
            val left = range.first * w
            val right = range.second * w
            val width = (right - left).coerceAtLeast(1f)
            val fade = min(width * 0.12f, 10f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to palette.darkWindow.copy(alpha = 0f),
                        0.08f to palette.darkWindow.copy(alpha = 0.55f),
                        0.5f to palette.darkWindow.copy(alpha = 0.92f),
                        0.92f to palette.darkWindow.copy(alpha = 0.55f),
                        1f to palette.darkWindow.copy(alpha = 0f),
                    ),
                    startX = left - fade,
                    endX = right + fade,
                ),
                topLeft = Offset(left - fade, 0f),
                size = Size(width + fade * 2f, skyBottom * 0.28f),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.darkWindow.copy(alpha = 0.55f),
                        palette.darkWindow.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    startY = skyBottom * 0.28f,
                    endY = skyBottom,
                ),
                topLeft = Offset(left, skyBottom * 0.28f),
                size = Size(width, skyBottom * 0.72f),
            )
        }
    }

    // --- marker lane wash --------------------------------------------------------------------
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.skyHorizon.copy(alpha = 0.35f),
                palette.hairline.copy(alpha = 0.12f),
            ),
            startY = skyBottom,
            endY = laneBottom,
        ),
        topLeft = Offset(0f, skyBottom),
        size = Size(w, laneBottom - skyBottom),
    )
    drawLine(
        color = palette.hairline.copy(alpha = 0.45f),
        start = Offset(0f, skyBottom),
        end = Offset(w, skyBottom),
        strokeWidth = 1f,
    )

    // --- the moonless window, bracketed under the sky row ------------------------------------
    if (model.hasMoonlessWindow) {
        for (span in model.moonlessWindows) {
            val range = model.rangeOf(span) ?: continue
            val left = range.first * w
            val right = (range.second * w).coerceAtLeast(left + 1f)
            val bandTop = skyBottom + 1f
            val bandBottom = laneBottom - 1f
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to palette.moonlight.copy(alpha = 0f),
                        0.06f to palette.moonlight.copy(alpha = 0.22f),
                        0.5f to palette.moonlight.copy(alpha = 0.32f),
                        0.94f to palette.moonlight.copy(alpha = 0.22f),
                        1f to palette.moonlight.copy(alpha = 0f),
                    ),
                    startX = left,
                    endX = right,
                ),
                topLeft = Offset(left, bandTop),
                size = Size(right - left, bandBottom - bandTop),
            )
            val y = skyBottom + (laneBottom - skyBottom) * 0.42f
            drawLine(
                color = palette.moonlight.copy(alpha = 0.85f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.5f,
            )
            drawMoonlessCap(left, y, palette.moonlight)
            drawMoonlessCap(right, y, palette.moonlight)
        }
    }

    // --- the Moon's rise and set, as ticks on the lane ---------------------------------------
    val tickY0 = skyBottom + (laneBottom - skyBottom) * 0.62f
    val tickY1 = laneBottom - 2f
    model.moonRiseUtc?.let { instant ->
        val x = model.fractionOf(instant) * w
        drawLine(
            palette.moonlight.copy(alpha = 0.9f),
            Offset(x, tickY0),
            Offset(x, tickY1),
            strokeWidth = 1.5f,
        )
        drawRiseTriangle(moonPath, x, tickY1, up = true, palette.moonlight)
    }
    model.moonSetUtc?.let { instant ->
        val x = model.fractionOf(instant) * w
        drawLine(
            palette.moonlight.copy(alpha = 0.9f),
            Offset(x, tickY0),
            Offset(x, tickY1),
            strokeWidth = 1.5f,
        )
        drawRiseTriangle(moonPath, x, tickY1, up = false, palette.moonlight)
    }

    // --- the cloud forecast row --------------------------------------------------------------
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.cloud.copy(alpha = 0.08f),
                palette.cloud.copy(alpha = 0.22f),
            ),
            startY = cloudTop,
            endY = h,
        ),
        topLeft = Offset(0f, cloudTop),
        size = Size(w, cloudHeight),
    )
    drawLine(
        color = palette.hairline.copy(alpha = 0.35f),
        start = Offset(0f, cloudTop),
        end = Offset(w, cloudTop),
        strokeWidth = 1f,
    )
    for (i in 0 until stops.size - 1) {
        val percent = stops[i].cloudPercent ?: continue
        val left = stops[i].scrub.toFloat() * w
        val right = stops[i + 1].scrub.toFloat() * w
        val width = (right - left).coerceAtLeast(0.5f)
        val filled = (cloudHeight * (percent / 100f)).coerceIn(0f, cloudHeight)
        if (filled <= 0.5f) continue
        val top = h - filled
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.cloud.copy(alpha = 0.35f),
                    palette.cloud.copy(alpha = 0.82f),
                ),
                startY = top,
                endY = h,
            ),
            topLeft = Offset(left, top),
            size = Size(width, filled),
        )
    }

    // --- the scrub handle -------------------------------------------------------------------
    drawScrubHandle(scrubValue, w, h, palette)
}

private fun DrawScope.drawHourTicks(
    model: TimelineStripModel,
    w: Float,
    skyBottom: Float,
    laneBottom: Float,
    cloudTop: Float,
    h: Float,
    palette: LumintraColors,
) {
    val session = model.session
    if (session.duration.toMinutes() < 45) return
    var tick = session.startUtc.truncatedTo(ChronoUnit.HOURS)
    if (tick.isBefore(session.startUtc)) tick = tick.plus(1, ChronoUnit.HOURS)
    val tickColour = palette.hairline.copy(alpha = 0.28f)
    while (tick.isBefore(session.endUtc)) {
        val x = model.fractionOf(tick) * w
        if (x > 0f && x < w) {
            drawLine(tickColour, Offset(x, 0f), Offset(x, skyBottom), strokeWidth = 1f)
            drawLine(
                tickColour.copy(alpha = 0.18f),
                Offset(x, laneBottom),
                Offset(x, cloudTop),
                strokeWidth = 1f,
            )
        }
        tick = tick.plus(1, ChronoUnit.HOURS)
    }
}

private fun DrawScope.drawMoonlessCap(x: Float, y: Float, colour: Color) {
    val cap = 4f
    drawLine(colour.copy(alpha = 0.9f), Offset(x, y - cap), Offset(x, y + cap), strokeWidth = 2f)
}

private fun DrawScope.drawScrubHandle(scrubValue: Float, w: Float, h: Float, palette: LumintraColors) {
    val x = scrubValue.coerceIn(0f, 1f) * w
    val accent = palette.firstLight
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = 0.55f),
                palette.markInk.copy(alpha = 0.95f),
                accent.copy(alpha = 0.45f),
            ),
            startY = 0f,
            endY = h,
        ),
        start = Offset(x, 0f),
        end = Offset(x, h),
        strokeWidth = 2.5f,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.38f), Color.Transparent),
            center = Offset(x, h * 0.5f),
            radius = 14f,
        ),
        radius = 14f,
        center = Offset(x, h * 0.5f),
    )
    val thumbY = 2f
    val thumbR = 5f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent, lerp(accent, palette.markInk, 0.35f)),
            center = Offset(x, thumbY + thumbR),
            radius = thumbR * 1.4f,
        ),
        radius = thumbR,
        center = Offset(x, thumbY + thumbR),
    )
    drawCircle(
        color = palette.markInk.copy(alpha = 0.85f),
        radius = thumbR,
        center = Offset(x, thumbY + thumbR),
        style = Stroke(width = 1f),
    )
}

/** A tiny rise/set caret. A triangle is drawn rather than a glyph because the strip is a canvas. */
private fun DrawScope.drawRiseTriangle(path: Path, x: Float, y: Float, up: Boolean, colour: Color) {
    path.rewind()
    val dy = if (up) -7f else 7f
    path.moveTo(x, y + dy)
    path.lineTo(x - 4f, y)
    path.lineTo(x + 4f, y)
    path.close()
    drawPath(path, colour.copy(alpha = 0.92f))
}

/** The strip's legend colours, so the screen's legend row and the strip cannot disagree. */
@Composable
fun rememberTimelineLegendColors(): TimelineLegendColors {
    val palette = LumintraTheme.colors
    val legendBar = MaterialTheme.colorScheme.outlineVariant
    return remember(palette, legendBar) {
        TimelineLegendColors(
            darkWindow = palette.darkWindow,
            moonless = palette.moonlight,
            cloud = palette.cloud,
            bar = legendBar,
        )
    }
}

/** The four inks the strip's legend names. */
@Immutable
data class TimelineLegendColors(
    val darkWindow: Color,
    val moonless: Color,
    val cloud: Color,
    val bar: Color,
)
