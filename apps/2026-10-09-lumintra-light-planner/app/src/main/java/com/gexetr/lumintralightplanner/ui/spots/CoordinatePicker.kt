package com.gexetr.lumintralightplanner.ui.spots

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.PlayKilnTheme
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// =================================================================================================
// Coordinate text: one codec, one home
// =================================================================================================
//
// Latitude is degrees North + / South −, longitude is degrees East + / West −
// (`domain/SkyPosition.kt` states the convention on `GeodeticPoint`). Every surface that reads or
// writes a coordinate string goes through this object, so the sign convention, the DMS rounding and
// the paste validation cannot drift between the editor and the picker.
//
// The degree/minute/second marks (`°`, `′`, `″`) are unit symbols and are written here, exactly as
// `domain/GeodeticPoint.label()` already writes them. The hemisphere LETTERS are localised, so every
// function that renders a hemisphere takes the letter from a caller that read it from a resource —
// Latin "N/S/E/W" is not the same word as Cyrillic "С/Ю/В/З".

/** The two ways a coordinate may be typed. The stored value is identical in both. */
enum class CoordinateEntryFormat { DECIMAL, DMS }

/** The hemisphere controls the editor requires, because a dropped `-` is the classic entry error. */
enum class LatitudeHemisphere {
    NORTH,
    SOUTH;

    val sign: Double get() = if (this == NORTH) 1.0 else -1.0
}

enum class LongitudeHemisphere {
    EAST,
    WEST;

    val sign: Double get() = if (this == EAST) 1.0 else -1.0
}

/** Why a pasted pair was refused. Each reason is a distinct sentence, never one generic failure. */
enum class PairFailure {
    EMPTY,
    NEEDS_TWO_VALUES,
    AMBIGUOUS,
    UNREADABLE,
    HEMISPHERE_MISMATCH,
    LATITUDE_OUT_OF_RANGE,
    LONGITUDE_OUT_OF_RANGE,
}

sealed interface PairParse {
    data class Ok(val latitudeDeg: Double, val longitudeDeg: Double) : PairParse
    data class Failed(val reason: PairFailure) : PairParse
}

/** The parsed pieces of one coordinate, for the degrees/minutes/seconds fields. */
data class DmsParts(val degrees: Int, val minutes: Int, val seconds: Double)

/**
 * The one codec for a coordinate string.
 *
 * Everything here is `Locale.ROOT` on the way *in* and out of the canonical form, because the
 * canonical form is machine text: a comma decimal mark is accepted from a human, never produced.
 */
object CoordinateText {

    /** Signed decimal degrees with no trailing zeros: `59.913`, `-33.9`, `0`. */
    fun formatDecimal(degrees: Double): String {
        if (!degrees.isFinite()) return ""
        var text = String.format(Locale.ROOT, "%.6f", degrees)
        if (text.contains('.')) text = text.trimEnd('0').trimEnd('.')
        return if (text == "-0" || text.isEmpty()) "0" else text
    }

    /** `59.9130, 10.7520` — the pair, for a readout that has no hemisphere letters to localise. */
    fun formatDecimalPair(latitudeDeg: Double, longitudeDeg: Double): String =
        "${formatDecimal(latitudeDeg)}, ${formatDecimal(longitudeDeg)}"

    /** Splits a value into whole degrees, whole minutes and seconds (< 60), on the absolute value. */
    fun dmsParts(degrees: Double): DmsParts {
        val abs = abs(degrees)
        val wholeDegrees = abs.toInt()
        val minutesTotal = (abs - wholeDegrees) * 60.0
        var minutes = minutesTotal.toInt()
        var seconds = (minutesTotal - minutes) * 60.0
        // Rounding seconds to two decimals can carry into the minute; 59.999" becomes 60.00".
        seconds = (seconds * 100.0).roundToInt() / 100.0
        if (seconds >= 60.0) {
            seconds = 0.0
            minutes += 1
        }
        return DmsParts(wholeDegrees, minutes, seconds)
    }

    /** `59°54′47.2″ N` — [hemisphereLetter] is the caller's localised letter. */
    fun formatDms(degrees: Double, hemisphereLetter: String): String {
        if (!degrees.isFinite()) return ""
        val parts = dmsParts(degrees)
        return "${parts.degrees}°${parts.minutes}′${formatSeconds(parts.seconds)}″ $hemisphereLetter"
    }

    /** `59°54′47.2″ N, 10°45′7.2″ E` in the caller's localised letters. */
    fun formatDmsPair(point: GeodeticPoint, north: String, south: String, east: String, west: String): String {
        val latLetter = if (point.latDeg < 0.0) south else north
        val lonLetter = if (point.lonDeg < 0.0) west else east
        return "${formatDms(point.latDeg, latLetter)}, ${formatDms(point.lonDeg, lonLetter)}"
    }

    private fun formatSeconds(seconds: Double): String {
        val text = String.format(Locale.ROOT, "%.2f", seconds)
        return text.trimEnd('0').trimEnd('.')
    }

    /**
     * Composes the degrees/minutes/seconds fields into the canonical decimal text.
     *
     * Returns `null` when the three fields are not a legal coordinate: a non-numeric field, a minute
     * or second of 60 or more, or a magnitude outside [maxMagnitude]. The sign comes from the
     * hemisphere control and never from a typed `-`, which is the whole point of the control.
     */
    fun composeDms(
        degreesText: String,
        minutesText: String,
        secondsText: String,
        negative: Boolean,
        maxMagnitude: Double,
    ): String? {
        val degrees = parseUnsigned(degreesText) ?: return null
        val minutes = if (minutesText.isBlank()) 0.0 else parseUnsigned(minutesText) ?: return null
        val seconds = if (secondsText.isBlank()) 0.0 else parseUnsigned(secondsText) ?: return null
        if (minutes >= 60.0 || seconds >= 60.0) return null
        val magnitude = degrees + minutes / 60.0 + seconds / 3600.0
        if (magnitude > maxMagnitude) return null
        return formatDecimal(if (negative) -magnitude else magnitude)
    }

    /** A bare non-negative number from a field; the hemisphere control supplies the sign. */
    private fun parseUnsigned(text: String): Double? {
        val normalised = text.trim().replace(',', '.').trimEnd('°', '′', '″', '\'', '"').trim()
        if (normalised.isEmpty()) return null
        if (!NUMBER.matches(normalised)) return null
        val value = normalised.toDoubleOrNull() ?: return null
        return if (value < 0.0) abs(value) else value
    }

    /** A signed decimal number, accepting a comma decimal mark and a trailing `°`. */
    fun parseDecimal(text: String): Double? {
        val normalised = text.trim().replace(',', '.').trimEnd('°').trim()
        if (normalised.isEmpty() || !NUMBER.matches(normalised)) return null
        return normalised.toDoubleOrNull()
    }

    /**
     * Parses a pasted pair, validating each half against its own range.
     *
     * The rule that matters: **latitude and longitude are never silently interchanged.** A first
     * value carrying an E/W letter, or a second carrying N/S, is refused as
     * [PairFailure.HEMISPHERE_MISMATCH] rather than swapped; a first value outside ±90 is refused
     * rather than reinterpreted as a longitude.
     *
     * Splitting rule, stated so it can be audited: halves are separated by `;`, or by a single `,`,
     * or by hemisphere letters in a whitespace-separated paste. A whitespace paste that carries no
     * hemisphere letter and is not exactly two numbers is refused as ambiguous rather than guessed —
     * `59 54 47 10 45 8` could be either axis order, so it is not accepted.
     */
    fun parsePair(text: String): PairParse {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return PairParse.Failed(PairFailure.EMPTY)

        val halves: List<String> = when {
            trimmed.contains(';') -> trimmed.split(';')
            trimmed.count { it == ',' } == 1 -> trimmed.split(',')
            else -> splitOnHemispheres(trimmed)
        }
        val meaningful = halves.map { it.trim() }.filter { it.isNotEmpty() }
        if (meaningful.size != 2) {
            return PairParse.Failed(
                if (meaningful.size < 2) PairFailure.NEEDS_TWO_VALUES else PairFailure.AMBIGUOUS,
            )
        }

        val first = parseHalf(meaningful[0])
        val second = parseHalf(meaningful[1])
        if (first is Half.Failed) return PairParse.Failed(first.reason)
        if (second is Half.Failed) return PairParse.Failed(second.reason)
        first as Half.Ok
        second as Half.Ok

        // An explicit hemisphere letter is authoritative; without one, the axis decides the range.
        // Written as explicit `==` comparisons rather than `in "EW"`: a `Char?` has no `contains`
        // overload on `String`, and the nullable comparison is the shape that would not compile.
        if (first.hemisphere == 'E' || first.hemisphere == 'W') {
            return PairParse.Failed(PairFailure.HEMISPHERE_MISMATCH)
        }
        if (second.hemisphere == 'N' || second.hemisphere == 'S') {
            return PairParse.Failed(PairFailure.HEMISPHERE_MISMATCH)
        }

        val latitude = first.degrees
        val longitude = second.degrees
        if (latitude !in -90.0..90.0) return PairParse.Failed(PairFailure.LATITUDE_OUT_OF_RANGE)
        if (longitude !in -180.0..180.0) return PairParse.Failed(PairFailure.LONGITUDE_OUT_OF_RANGE)
        return PairParse.Ok(latitude, longitude)
    }

    private sealed interface Half {
        data class Ok(val degrees: Double, val hemisphere: Char?) : Half
        data class Failed(val reason: PairFailure) : Half
    }

    /**
     * Splits a whitespace-separated paste on hemisphere letters, so `59°54′47″N 10°45′8″E` becomes
     * two halves. A paste with no letters splits only when it is exactly two numbers.
     */
    private fun splitOnHemispheres(text: String): List<String> {
        val words = text.split(WHITESPACE).filter { it.isNotBlank() }
        val hasLetter = words.any { word -> word.any { it.uppercaseChar() in "NSEW" } }
        if (!hasLetter) return if (words.size == 2) words else emptyList()

        val halves = ArrayList<String>()
        var current = StringBuilder()
        for (word in words) {
            current.append(' ').append(word)
            if (word.any { it.uppercaseChar() in "NSEW" }) {
                halves.add(current.toString())
                current = StringBuilder()
            }
        }
        if (current.isNotBlank()) halves.add(current.toString())
        return halves
    }

    private fun parseHalf(text: String): Half {
        val words = text.trim().split(WHITESPACE).filter { it.isNotBlank() }
        if (words.isEmpty()) return Half.Failed(PairFailure.NEEDS_TWO_VALUES)

        var hemisphere: Char? = null
        val numbers = ArrayList<Double>()
        for (word in words) {
            var token = word.trim()
            // A hemisphere letter may lead or trail its number ("N59.913", "59.913N", "47″N").
            val leading = token.firstOrNull()?.uppercaseChar()
            if (leading != null && leading in "NSEW") {
                if (hemisphere != null) return Half.Failed(PairFailure.AMBIGUOUS)
                hemisphere = leading
                token = token.drop(1)
            }
            val trailing = token.lastOrNull()?.uppercaseChar()
            if (trailing != null && trailing in "NSEW") {
                if (hemisphere != null) return Half.Failed(PairFailure.AMBIGUOUS)
                hemisphere = trailing
                token = token.dropLast(1)
            }
            token = token.replace('°', ' ').replace('′', ' ').replace('″', ' ')
                .replace('\'', ' ').replace('"', ' ')
            for (piece in token.split(WHITESPACE)) {
                if (piece.isBlank()) continue
                val normalised = piece.replace(',', '.')
                if (!NUMBER.matches(normalised)) return Half.Failed(PairFailure.UNREADABLE)
                val value = normalised.toDoubleOrNull() ?: return Half.Failed(PairFailure.UNREADABLE)
                numbers.add(value)
            }
        }
        if (numbers.isEmpty() || numbers.size > 3) return Half.Failed(PairFailure.UNREADABLE)
        if (numbers.size >= 2 && numbers[1] >= 60.0) return Half.Failed(PairFailure.UNREADABLE)
        if (numbers.size == 3 && numbers[2] >= 60.0) return Half.Failed(PairFailure.UNREADABLE)

        val magnitude = when (numbers.size) {
            1 -> numbers[0]
            2 -> numbers[0] + numbers[1] / 60.0
            else -> numbers[0] + numbers[1] / 60.0 + numbers[2] / 3600.0
        }
        val signed = when (hemisphere) {
            null -> magnitude
            'S', 'W' -> -abs(magnitude)
            else -> abs(magnitude)
        }
        return Half.Ok(signed, hemisphere)
    }

    private val NUMBER = Regex("^[+-]?\\d+([.,]\\d*)?$")
    private val WHITESPACE = Regex("[\\s\\u00A0]+")
}

// =================================================================================================
// The keyless platform geocoder
// =================================================================================================
//
// `android.location.Geocoder` is the only geocoding call this app is allowed to make: it is keyless,
// it carries no credential and no permission beyond the `INTERNET` the app already declares, and a
// third-party geocoding API would need a client key, which `client-secrets.mdc` forbids inside
// `app/src/main/`.
//
// The branch is forced by the slot's `minSdk = 26` against the asynchronous `GeocodeListener`
// overload's API 33: `GeocodeListener` on 33+, and the deprecated synchronous overload executed on
// `Dispatchers.IO` on 26–32 — the fallback the platform reference itself permits. `Geocoder.isPresent()`
// is checked before either branch, because with no backend implementation every call errors.
//
// A missing implementation, an offline device, a `null` result and an empty list are all NORMAL
// states: the caller keeps its coordinates and the user's own label, labels the spot `approximate`
// and gates nothing.

/** One place-name result: the label the user reads and the coordinate it resolved to. */
data class GeocodeHit(val label: String, val latitudeDeg: Double, val longitudeDeg: Double) {
    val point: GeodeticPoint get() = GeodeticPoint(latitudeDeg, longitudeDeg)
}

object PlatformGeocoder {

    /** API 9. False means no geocoding backend exists here, so no call is attempted. */
    fun isPresent(): Boolean = Geocoder.isPresent()

    /** The place name for a coordinate, or `null` when the lookup found nothing. */
    suspend fun reverseLookup(context: Context, point: GeodeticPoint, locale: Locale): String? {
        if (!isPresent()) return null
        val geocoder = Geocoder(context, locale)
        val found = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fromLocationAsync(geocoder, point.latDeg, point.lonDeg)
        } else {
            fromLocationBlocking(geocoder, point.latDeg, point.lonDeg)
        }
        return found.firstOrNull()?.describe()
    }

    /** The places a typed name resolves to. An empty list is a normal answer, not an error. */
    suspend fun forwardLookup(context: Context, query: String, locale: Locale): List<GeocodeHit> {
        if (!isPresent() || query.isBlank()) return emptyList()
        val geocoder = Geocoder(context, locale)
        val found = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fromNameAsync(geocoder, query)
        } else {
            fromNameBlocking(geocoder, query)
        }
        return found.mapNotNull { address ->
            val latitude = address.latitude
            val longitude = address.longitude
            val label = address.describe()
            if (label == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) null
            else GeocodeHit(label, latitude, longitude)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun fromLocationAsync(geocoder: Geocoder, latitude: Double, longitude: Double): List<Address> =
        suspendCancellableCoroutine { continuation ->
            val listener = Geocoder.GeocodeListener { addresses ->
                if (continuation.isActive) continuation.resume(addresses.orEmpty())
            }
            try {
                geocoder.getFromLocation(latitude, longitude, MAX_RESULTS, listener)
            } catch (failure: Exception) {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun fromNameAsync(geocoder: Geocoder, query: String): List<Address> =
        suspendCancellableCoroutine { continuation ->
            val listener = Geocoder.GeocodeListener { addresses ->
                if (continuation.isActive) continuation.resume(addresses.orEmpty())
            }
            try {
                geocoder.getFromLocationName(query, MAX_RESULTS, listener)
            } catch (failure: Exception) {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }

    /** API 26–32: the deprecated overload, always off the main thread — never on it. */
    @Suppress("DEPRECATION")
    private suspend fun fromLocationBlocking(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): List<Address> = withContext(Dispatchers.IO) {
        try {
            geocoder.getFromLocation(latitude, longitude, MAX_RESULTS).orEmpty()
        } catch (failure: IOException) {
            emptyList()
        } catch (failure: IllegalArgumentException) {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun fromNameBlocking(geocoder: Geocoder, query: String): List<Address> =
        withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocationName(query, MAX_RESULTS).orEmpty()
            } catch (failure: IOException) {
                emptyList()
            } catch (failure: IllegalArgumentException) {
                emptyList()
            }
        }

    private const val MAX_RESULTS = 8

    /** A place name from the parts a human reads, falling back to the first address line. */
    private fun Address.describe(): String? {
        val parts = listOfNotNull(
            locality?.takeIf { it.isNotBlank() },
            subAdminArea?.takeIf { it.isNotBlank() },
            adminArea?.takeIf { it.isNotBlank() },
            countryName?.takeIf { it.isNotBlank() },
        ).distinct()
        if (parts.isNotEmpty()) return parts.joinToString(", ")
        return getAddressLine(0)?.takeIf { it.isNotBlank() }
    }
}

// =================================================================================================
// The picker surface
// =================================================================================================

/** The lowest zoom shows the whole world; the highest is roughly 60 degrees across the width. */
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 60f

/** Compact enough that an open map leaves room to scroll while Save stays pinned below. */
private val PICKER_HEIGHT = 168.dp
private const val GRATICULE_STEP_DEG = 15.0

/** The visible extent for one zoom level at one canvas aspect. */
private data class ViewSpans(val lonDeg: Double, val latDeg: Double)

private fun spansFor(zoom: Float, size: Size): ViewSpans {
    val width = size.width.coerceAtLeast(1f)
    val height = size.height.coerceAtLeast(1f)
    val lon = 360.0 / zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    val lat = (lon / width * height).coerceAtMost(180.0)
    return ViewSpans(lon, lat)
}

private fun clampCentreLat(centreDeg: Double, latSpan: Double): Double {
    val half = latSpan / 2.0
    return if (half >= 90.0) 0.0 else centreDeg.coerceIn(-90.0 + half, 90.0 - half)
}

private fun clampCentreLon(centreDeg: Double, lonSpan: Double): Double {
    val half = lonSpan / 2.0
    return if (half >= 180.0) 0.0 else centreDeg.coerceIn(-180.0 + half, 180.0 - half)
}

/**
 * The slot's own coordinate surface: a pannable, zoomable plate-carrée canvas with a fixed centre
 * crosshair and a draggable-into-place pin.
 *
 * **No map SDK, no tile provider, no API key and no permission.** The surface is a graticule drawn
 * by this file, which is what makes the offline path true by construction: there is nothing to fetch,
 * so there is nothing that can fail when the device has no network.
 *
 * The pin is placed by an **explicit action** — the crosshair button, a tap on the canvas, or the
 * editor's own fields. Nothing here ever invents a coordinate: with no pin, the readout says so and
 * the spot keeps whatever the fields hold. A **view centre is not a spot coordinate**; it only says
 * which part of the world the canvas is showing, and [centreHint] is where it opens (the editor
 * passes the spot's own region, or the region of its time zone).
 */
@Composable
fun CoordinatePicker(
    point: GeodeticPoint?,
    onMove: (GeodeticPoint) -> Unit,
    modifier: Modifier = Modifier,
    centreHint: GeodeticPoint? = null,
) {
    val lumintra = LumintraTheme.colors
    val seed = point ?: centreHint ?: GeodeticPoint(0.0, 0.0)

    var centreLat by rememberSaveable { mutableStateOf(seed.latDeg) }
    var centreLon by rememberSaveable { mutableStateOf(seed.lonDeg) }
    var zoom by rememberSaveable { mutableStateOf(2f) }
    // The canvas extent is measured, not authored: it is re-read on the first layout after any
    // recreation, so it is deliberately NOT saveable (a `Parcelable` round-trip would buy nothing).
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val canvasDescription = stringResource(R.string.spot_edit_picker_canvas_description)
    val pin = point

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.spot_edit_picker_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            softWrap = true,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(PICKER_HEIGHT)
                .background(lumintra.skyZenith, MaterialTheme.shapes.medium)
                .semantics { contentDescription = canvasDescription }
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val layout = Size(size.width.toFloat(), size.height.toFloat())
                        val spans = spansFor(zoom, layout)
                        val lat = clampCentreLat(centreLat, spans.latDeg)
                        val lon = clampCentreLon(centreLon, spans.lonDeg)
                        val tappedLat = lat - (offset.y / layout.height - 0.5f) * spans.latDeg
                        val tappedLon = lon + (offset.x / layout.width - 0.5f) * spans.lonDeg
                        onMove(
                            GeodeticPoint(
                                latDeg = tappedLat.coerceIn(-90.0, 90.0),
                                lonDeg = tappedLon.coerceIn(-180.0, 180.0),
                            ),
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val layout = Size(size.width.toFloat(), size.height.toFloat())
                        val spans = spansFor(zoom, layout)
                        if (layout.width <= 0f || layout.height <= 0f) return@detectDragGestures
                        // Dragging the map east moves the view west; dragging down moves it north.
                        centreLon = clampCentreLon(
                            centreLon - dragAmount.x / layout.width * spans.lonDeg,
                            spans.lonDeg,
                        )
                        centreLat = clampCentreLat(
                            centreLat + dragAmount.y / layout.height * spans.latDeg,
                            spans.latDeg,
                        )
                    }
                },
        ) {
            val spans = spansFor(zoom, size)
            val viewLat = clampCentreLat(centreLat, spans.latDeg)
            val viewLon = clampCentreLon(centreLon, spans.lonDeg)
            fun xFor(lonDeg: Double): Float =
                ((lonDeg - viewLon) / spans.lonDeg + 0.5).toFloat() * size.width
            fun yFor(latDeg: Double): Float =
                (0.5 - (latDeg - viewLat) / spans.latDeg).toFloat() * size.height

            val left = viewLon - spans.lonDeg / 2.0
            val right = viewLon + spans.lonDeg / 2.0
            val bottom = viewLat - spans.latDeg / 2.0
            val top = viewLat + spans.latDeg / 2.0

            var meridian = Math.floor(left / GRATICULE_STEP_DEG) * GRATICULE_STEP_DEG
            while (meridian <= right) {
                val x = xFor(meridian)
                val strong = abs(meridian) < 1e-9
                drawLine(
                    color = if (strong) lumintra.onSkyMuted else lumintra.hairline,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (strong) 2f else 1f,
                )
                meridian += GRATICULE_STEP_DEG
            }
            var parallel = Math.floor(bottom / GRATICULE_STEP_DEG) * GRATICULE_STEP_DEG
            while (parallel <= top) {
                val y = yFor(parallel)
                val strong = abs(parallel) < 1e-9
                drawLine(
                    color = if (strong) lumintra.onSkyMuted else lumintra.hairline,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (strong) 2f else 1f,
                )
                parallel += GRATICULE_STEP_DEG
            }

            // The centre crosshair: the point the "place the pin here" action uses, and the only
            // position a screen reader can drive, because a canvas tap cannot be read by TalkBack.
            val centre = Offset(size.width / 2f, size.height / 2f)
            drawLine(
                color = lumintra.firstLight,
                start = Offset(centre.x, centre.y - 14f),
                end = Offset(centre.x, centre.y + 14f),
                strokeWidth = 2f,
            )
            drawLine(
                color = lumintra.firstLight,
                start = Offset(centre.x - 14f, centre.y),
                end = Offset(centre.x + 14f, centre.y),
                strokeWidth = 2f,
            )
            drawCircle(color = lumintra.firstLight, radius = 3f, center = centre)

            if (pin != null && pin.latDeg in bottom..top && pin.lonDeg in left..right) {
                val pinCentre = Offset(xFor(pin.lonDeg), yFor(pin.latDeg))
                drawCircle(color = lumintra.firstLight, radius = 7f, center = pinCentre)
                drawCircle(
                    color = lumintra.onSky,
                    radius = 7f,
                    center = pinCentre,
                    style = Stroke(width = 2f),
                )
            }
        }

        Text(
            text = stringResource(
                R.string.spot_edit_picker_range,
                CoordinateText.formatDecimal(
                    spansFor(
                        zoom,
                        Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                    ).lonDeg,
                ),
                CoordinateText.formatDecimal(
                    spansFor(
                        zoom,
                        Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                    ).latDeg,
                ),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            softWrap = true,
        )

        Text(
            text = if (pin == null) {
                stringResource(R.string.spot_edit_picker_no_pin)
            } else {
                stringResource(R.string.spot_edit_picker_pin_at, formatPickerPoint(pin))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (pin == null) lumintra.caution else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            softWrap = true,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { zoom = (zoom * 2f).coerceAtMost(MAX_ZOOM) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_plus),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.spot_edit_picker_zoom_in),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            FilledTonalButton(
                onClick = { zoom = (zoom / 2f).coerceAtLeast(MIN_ZOOM) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.spot_edit_picker_zoom_out))
            }
        }

        FilledTonalButton(
            onClick = {
                val spans = spansFor(
                    zoom,
                    Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                )
                onMove(
                    GeodeticPoint(
                        latDeg = clampCentreLat(centreLat, spans.latDeg),
                        lonDeg = clampCentreLon(centreLon, spans.lonDeg),
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .heightIn(min = 48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_map_pin),
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.spot_edit_picker_drop_pin),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (pin != null) {
            FilledTonalButton(
                onClick = {
                    centreLat = pin.latDeg
                    centreLon = pin.lonDeg
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_locate_fixed),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.spot_edit_picker_centre_on_pin),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** The picker's own readout, in the locale's hemisphere letters. */
@Composable
private fun formatPickerPoint(point: GeodeticPoint): String = CoordinateText.formatDmsPair(
    point = point,
    north = stringResource(R.string.coord_hemisphere_n),
    south = stringResource(R.string.coord_hemisphere_s),
    east = stringResource(R.string.coord_hemisphere_e),
    west = stringResource(R.string.coord_hemisphere_w),
)

@Preview(name = "Coordinate picker", showBackground = true, widthDp = 412)
@Composable
private fun CoordinatePickerPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        CoordinatePicker(
            point = GeodeticPoint(59.9139, 10.7522),
            onMove = {},
            centreHint = GeodeticPoint(59.9, 10.7),
            modifier = Modifier.padding(16.dp),
        )
    }
}
