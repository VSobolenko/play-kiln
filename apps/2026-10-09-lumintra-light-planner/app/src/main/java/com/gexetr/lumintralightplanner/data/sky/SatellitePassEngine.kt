package com.gexetr.lumintralightplanner.data.sky

import com.gexetr.lumintralightplanner.data.CloudProtocol
import com.gexetr.lumintralightplanner.data.OmmElementSet
import com.gexetr.lumintralightplanner.data.OmmUnits
import com.gexetr.lumintralightplanner.domain.ElementSetAge
import com.gexetr.lumintralightplanner.domain.ElementSetAgeBand
import com.gexetr.lumintralightplanner.domain.FieldOfView
import com.gexetr.lumintralightplanner.domain.FrameCrossing
import com.gexetr.lumintralightplanner.domain.FrameCrossingConfidence
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.GroundTrackPoint
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.HorizontalCoords
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.PassVisibility
import com.gexetr.lumintralightplanner.domain.SatellitePass
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.orekit.data.DataSource
import org.orekit.errors.OrekitException
import org.orekit.files.ccsds.ndm.ParserBuilder
import org.orekit.files.ccsds.ndm.odm.omm.Omm
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.SpacecraftState
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.propagation.events.ElevationDetector
import org.orekit.propagation.events.EventsLogger
import org.orekit.time.AbsoluteDate
import org.orekit.utils.Constants
import org.orekit.utils.ElevationMask
import org.orekit.utils.OccultationEngine

// ---------------------------------------------------------------------------------------------
// Public contract
// ---------------------------------------------------------------------------------------------

/**
 * Why the engine produced no pass time for an element set, or for the whole request.
 *
 * Nothing here is user-facing text: the screens map each value to its own resource, exactly as the
 * element fetch layer maps [com.gexetr.lumintralightplanner.data.ElementFailure].
 */
enum class PassRefusalReason {
    /** [OrekitRuntime.warmUp] has not completed: no frame and no time scale exist yet. */
    ENGINE_NOT_READY,

    /** The runtime's bundled data could not be loaded. */
    ENGINE_INIT_FAILED,

    /** The dark window is empty or ends before it starts. */
    NO_DARK_WINDOW,

    /** The request carried no element set at all. */
    NO_ELEMENT_SETS,

    /** `age > 72 h`: no pass time of any kind may be computed from this set. */
    ELEMENT_SET_EXPIRED,

    /** `EPOCH` is later than the session instant beyond the tolerance: a wrong device clock. */
    ELEMENT_SET_EPOCH_IN_FUTURE,

    /** `EPHEMERIS_TYPE` is not SGP4: a model this app does not propagate. */
    UNSUPPORTED_EPHEMERIS_TYPE,

    /** The record could not be turned into a propagatable orbit by either path. */
    ORBIT_NOT_CONSTRUCTED,

    /** A 200 body the parser cannot read — the spec's `MALFORMED_RESPONSE`. */
    MALFORMED_RESPONSE,

    /** A segment's propagation threw: the search stopped rather than reporting a partial answer. */
    PROPAGATION_FAILED,
}

/** The state of one element set in one request: what it is, its age, and what was done with it. */
data class ElementSetStatus(
    val noradId: Long,
    val satelliteName: String,
    val objectId: String?,
    /** The set's own `EPOCH`, the instant the age is measured from. */
    val epochUtc: Instant,
    val age: ElementSetAge,
    /** True when the set was propagated and its passes are in the outcome. */
    val computed: Boolean,
    /** Why it was not computed, when [computed] is false. */
    val refusal: PassRefusalReason?,
) {
    /** True when the set's age is worth stating beside every prediction. */
    val showAgeBadge: Boolean get() = age.showBadge
}

/** A failure of the **whole** request, as opposed to one element set. */
data class EngineFailure(
    val reason: PassRefusalReason,
    /** A technical diagnostic for logs; the screens own the sentence the user reads. */
    val detail: String,
)

/**
 * Whether the pass is above the user's own drawn horizon, above the user's constant mask but behind
 * the drawn profile, or below the mask altogether.
 *
 * It exists because `PassVisibility` (the frozen domain enum) carries only the *optical* reason —
 * shadow, brightness — and the spec's edge table needs the **geometric** reason as its own sentence
 * ("Rises behind your horizon", `E42`). A pass below the constant mask is not reported at all.
 */
enum class ElevationVerdict {
    /** Above both the constant mask and the drawn profile at its peak azimuth. */
    ABOVE_HORIZON,

    /** Above the constant mask but below the drawn horizon line: it rises behind the user's ridge. */
    BEHIND_DRAWN_HORIZON,

    /** Below the constant mask: not reported as a pass at all. */
    BELOW_MASK,
}

/**
 * One computed pass, with the readings the frozen domain type cannot carry.
 *
 * Everything the user sees is in [pass]. This type adds the numbers the spec requires and the
 * domain has no field for: the **observable interval** (the intersection of above-the-mask, sunlit
 * and observer-dark, whose 30 s floor decides whether a pass may be offered as visible), the
 * geometric verdict against the drawn horizon, and the azimuths the readout names.
 */
data class PassComputation(
    val pass: SatellitePass,
    val elevationVerdict: ElevationVerdict,
    /** The intersection of the three conditions, in seconds; the spec's floor is 30 s. */
    val observableIntervalSeconds: Double,
    val observableInterval: Duration,
    /** True when the interval clears the 30 s floor, or when no exposure/lens question is asked. */
    val clearsIntervalFloor: Boolean,
    val maxRangeKm: Double,
    val aosAzimuthDeg: Double,
    val peakAzimuthDeg: Double,
    val losAzimuthDeg: Double,
) {
    /** A pass the user can actually watch: the domain's optical verdict **and** the 30 s floor. */
    val offeredAsVisible: Boolean
        get() = pass.observable && clearsIntervalFloor && elevationVerdict == ElevationVerdict.ABOVE_HORIZON
}

/** One crossing of the user's lens frame, with its entry and exit instants. */
data class FrameCrossingSpan(
    val noradId: Long,
    val satelliteName: String,
    val entryUtc: Instant,
    val exitUtc: Instant,
    val samplesInside: Int,
    val crossing: FrameCrossing,
) {
    val duration: Duration get() = Duration.between(entryUtc, exitUtc)
}

/**
 * Everything one search produced: the passes, the frame crossings, and the state of every element
 * set that was considered — including the ones that were refused.
 *
 * It is a **typed result, never an empty list standing in for a failure**: [failure] is the
 * whole-request failure, each refused set names its own reason in [elementSets], and a screen can
 * therefore always tell "no passes tonight" from "we could not compute".
 */
data class PassSearchOutcome(
    val passes: List<SatellitePass>,
    val computations: List<PassComputation>,
    val crossings: List<FrameCrossingSpan>,
    val elementSets: List<ElementSetStatus>,
    val segmentsSearched: Int,
    val eventsLogged: Int,
    val failure: EngineFailure?,
) {
    /** True when the request was answered and the answer is genuinely "nothing in this window". */
    val isGenuinelyEmpty: Boolean get() = failure == null && passes.isEmpty()

    /** True when the window was searched for at least one set. */
    val anyComputed: Boolean get() = elementSets.any { it.computed }

    companion object {
        fun failed(
            reason: PassRefusalReason,
            detail: String,
            sets: List<ElementSetStatus> = emptyList(),
        ): PassSearchOutcome =
            PassSearchOutcome(emptyList(), emptyList(), emptyList(), sets, 0, 0, EngineFailure(reason, detail))
    }
}

/**
 * The observer's own horizon, as an elevation per azimuth.
 *
 * A `fun interface` rather than the domain type so the engine can be driven by a two-line fixture in
 * a JVM test, and so the one adapter from [HorizonProfile] lives in one place — [of] below.
 */
fun interface HorizonMask {

    /** The obstruction's elevation at [azimuthDeg] (`0° = North, 90° = East`), in degrees. */
    fun elevationDegAt(azimuthDeg: Double): Double

    companion object {
        /** A flat horizon, for a spot with nothing drawn. */
        fun flat(elevationDeg: Double): HorizonMask = HorizonMask { elevationDeg }

        /** The user's drawn profile: its own linear interpolation, wrapping across `0/360`. */
        fun of(profile: HorizonProfile): HorizonMask = HorizonMask { azimuth -> profile.altitudeAt(azimuth) }
    }
}

/**
 * The observer's **geometric** Sun altitude at an instant, in degrees.
 *
 * Injected rather than imported: the app's Sun comes from `SkyEphemeris` (`AstronomySkyEphemeris`),
 * and `design.md` fixes the two engines against each other — the Astronomy Engine is the authority
 * for the Sun and the Moon, Orekit only for the satellite. [of] is the one adapter.
 */
fun interface SunAltitudeSource {

    fun geometricAltitudeDeg(instantUtc: Instant): Double

    companion object {
        /**
         * The Sun's **geometric** altitude from the Astronomy Engine — the `AltitudeModel.GEOMETRIC`
         * reading, which is what the twilight and dark-window rules use, never the refracted one.
         */
        fun of(ephemeris: SkyEphemeris, point: GeodeticPoint): SunAltitudeSource =
            SunAltitudeSource { instant ->
                ephemeris.sunHorizontal(point, instant, AltitudeModel.GEOMETRIC).altitudeDeg
            }
    }
}

/**
 * The user's lens frame and the canvas it is drawn on, in the numbers the projection needs.
 *
 * `pxPerDeg = canvasWidthPx / sceneFovHorizontalDeg` is the spec's equidistant projection: because
 * the scale is one constant, a rectangle in **degrees** and a rectangle in **pixels** are the same
 * test, and the engine therefore works in degrees and needs no canvas to decide a crossing.
 */
data class LensFrame(
    val fieldOfView: FieldOfView,
    val aimAzimuthDeg: Double,
    val aimAltitudeDeg: Double,
    /** Rotation of the frame about the aim point, in degrees, counter-clockwise. */
    val rollDeg: Double,
    val canvasWidthPx: Int,
    val sceneFovHorizontalDeg: Double,
) {
    init {
        require(fieldOfView.fovH > 0.0 && fieldOfView.fovV > 0.0) { "a frame needs a positive extent" }
        require(canvasWidthPx > 0) { "a canvas needs a positive width" }
        require(sceneFovHorizontalDeg > 0.0) { "the scene needs a positive horizontal field" }
    }

    /** Pixels per degree of the scene's equidistant projection. */
    val pxPerDeg: Double get() = canvasWidthPx.toDouble() / sceneFovHorizontalDeg
}

/**
 * One search: who is looking, from where, over which window, for which objects.
 *
 * [elementSets] are the sets the fetch layer resolved (`ElementsResult.Available.sets`), or the sets
 * a KVN body parsed into — the engine re-applies the age bands itself, because it is the last place
 * a pass time can be produced and the bands must hold there whatever the caller did.
 */
data class PassSearchRequest(
    val elementSets: List<OmmElementSet>,
    val observer: GeodeticPoint,
    /** The session's dark window; capped at 24 h by the engine. */
    val window: NightSpan,
    /** The session's "now", captured once by the UI and never re-read from the clock. */
    val sessionInstantUtc: Instant,
    val sunAltitude: SunAltitudeSource,
    val horizon: HorizonMask? = null,
    /** The constant elevation mask, `0.0…45.0°`, defaulting to the domain's own `DEFAULT_MASK_DEG`. */
    val maskDeg: Double = SatellitePass.DEFAULT_MASK_DEG,
    /** The observer-dark threshold: `-6.0°` (civil) or `-18.0°` (fully dark). */
    val sunThresholdDeg: Double = SatellitePass.DEFAULT_SUN_THRESHOLD_DEG,
    /** The lens frame and exposure, when the crossing question is being asked. */
    val lens: LensFrame? = null,
    val exposureSeconds: Double = 0.0,
) {
    init {
        require(maskDeg in SatellitePass.MIN_MASK_DEG..SatellitePass.MAX_MASK_DEG) {
            "mask ${maskDeg}° is outside ${SatellitePass.MIN_MASK_DEG}…${SatellitePass.MAX_MASK_DEG}°"
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The pure OMM keyword parser — no Orekit, no Android, JVM-testable
// ---------------------------------------------------------------------------------------------

/** The outcome of parsing an OMM **keyword** body: the sets, or the reason the body was refused. */
sealed interface OmmKeywordParse {

    data class Records(val sets: List<OmmElementSet>) : OmmKeywordParse

    /** A body the parser cannot read. Never a partial parse: one bad record refuses the whole body. */
    data class Malformed(val reason: String) : OmmKeywordParse

    /** The body carried no element record at all (blank, or comments only). */
    data object Empty : OmmKeywordParse
}

/**
 * The OMM keyword (`KEY = VALUE`) reader and writer, factored out of the engine so it can be tested
 * on the JVM with no Orekit initialisation at all.
 *
 * **Why keyword text and not the JSON CelesTrak sends.** The fetch layer already parses the JSON
 * into [OmmElementSet] (`ElementsProtocol.parseRecords`); what Orekit's own `OmmParser` wants is the
 * CCSDS keyword form, so the engine *writes* the keyword body from the set and lets Orekit parse it
 * — which is the spec's requirement ("obtain the orbit from Orekit's own OMM support") — and this
 * reader is the **documented fallback contract** for a record that parser refuses. Both routes end
 * in the same `org.orekit.propagation.analytical.tle.TLE`, and this reader is exercised by the unit
 * tests without a jar on the classpath.
 *
 * **The required-key list is the fetch layer's, exactly.** `ElementsProtocol.parseRecords`' own KDoc
 * names which keys are required and which four are descriptive; this reader uses the same sets, so a
 * body one parser accepts is not refused by the other. `EPOCH` without a `Z`
 * (`"2026-10-09T04:28:31.895104"`) is read as UTC, because that is how CelesTrak states it.
 *
 * **Catalogue numbers are read as numbers, not as columns.** There is no five-digit column here, so
 * a `NORAD_CAT_ID` of `100123` (six digits, the space CelesTrak moved into on 2026-07-11) or of
 * `100000001` (nine) is carried as the `Long` it says rather than truncated or skipped.
 */
object OmmKeyword {

    /** Keys the propagator needs: a record missing any of these is refused, never defaulted. */
    val REQUIRED_KEYS: Set<String> = setOf(
        "OBJECT_NAME",
        "EPOCH",
        "NORAD_CAT_ID",
        "MEAN_MOTION",
        "ECCENTRICITY",
        "INCLINATION",
        "RA_OF_ASC_NODE",
        "ARG_OF_PERICENTER",
        "MEAN_ANOMALY",
        "EPHEMERIS_TYPE",
        "BSTAR",
        "MEAN_MOTION_DOT",
        "MEAN_MOTION_DDOT",
    )

    /**
     * Parses an OMM keyword body into element sets.
     *
     * A JSON body is refused by name rather than half-read: JSON is `ElementsProtocol`'s job, and two
     * JSON readers would be two answers about the same bytes.
     */
    fun parse(body: String): OmmKeywordParse {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return OmmKeywordParse.Empty
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return OmmKeywordParse.Malformed("body is JSON; OMM JSON is parsed by ElementsProtocol")
        }

        val blocks = ArrayList<MutableMap<String, String>>()
        var current = LinkedHashMap<String, String>()
        for (rawLine in body.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                if (current.isNotEmpty()) {
                    blocks.add(current)
                    current = LinkedHashMap()
                }
                continue
            }
            if (line.startsWith("COMMENT")) continue
            val separator = line.indexOf('=')
            if (separator <= 0) {
                return OmmKeywordParse.Malformed("line is not a KEY = VALUE pair: ${line.take(48)}")
            }
            val key = line.substring(0, separator).trim().uppercase()
            var value = line.substring(separator + 1).trim()
            if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length - 1)
            }
            // A second `CCSDS_OMM_VERS` — or a repeated `OBJECT_NAME`/`NORAD_CAT_ID` — starts the next
            // record even when the file separates records with no blank line.
            if (key in current && (key == "CCSDS_OMM_VERS" || key == "OBJECT_NAME" || key == "NORAD_CAT_ID")) {
                blocks.add(current)
                current = LinkedHashMap()
            }
            current[key] = value
        }
        if (current.isNotEmpty()) blocks.add(current)
        if (blocks.isEmpty()) return OmmKeywordParse.Empty

        val sets = ArrayList<OmmElementSet>(blocks.size)
        for (block in blocks) {
            val set = fromKeywords(block)
                ?: return OmmKeywordParse.Malformed(
                    "an OMM record is missing a field the propagator needs or holds an unreadable value",
                )
            sets.add(set)
        }
        if (sets.isEmpty()) return OmmKeywordParse.Empty
        return OmmKeywordParse.Records(sets)
    }

    /** Parses one record. `null` when a required key is absent or a value cannot be read. */
    fun fromKeywords(keywords: Map<String, String>): OmmElementSet? {
        if (!keywords.keys.containsAll(REQUIRED_KEYS)) return null

        val objectName = keywords["OBJECT_NAME"]?.takeIf { it.isNotBlank() } ?: return null
        val epoch = CloudProtocol.parseIso8601Utc(keywords["EPOCH"]!!) ?: return null
        val noradId = keywords["NORAD_CAT_ID"]!!.trim().toLongOrNull() ?: return null
        if (noradId <= 0L) return null
        val meanMotion = keywords["MEAN_MOTION"]!!.trim().toDoubleOrNull() ?: return null
        val eccentricity = keywords["ECCENTRICITY"]!!.trim().toDoubleOrNull() ?: return null
        val inclination = keywords["INCLINATION"]!!.trim().toDoubleOrNull() ?: return null
        val raan = keywords["RA_OF_ASC_NODE"]!!.trim().toDoubleOrNull() ?: return null
        val argPericenter = keywords["ARG_OF_PERICENTER"]!!.trim().toDoubleOrNull() ?: return null
        val meanAnomaly = keywords["MEAN_ANOMALY"]!!.trim().toDoubleOrNull() ?: return null
        val ephemerisType = keywords["EPHEMERIS_TYPE"]!!.trim().toIntOrNull() ?: return null
        val bstar = keywords["BSTAR"]!!.trim().toDoubleOrNull() ?: return null
        val meanMotionDot = keywords["MEAN_MOTION_DOT"]!!.trim().toDoubleOrNull() ?: return null
        val meanMotionDotDot = keywords["MEAN_MOTION_DDOT"]!!.trim().toDoubleOrNull() ?: return null

        // The same three physical plausibility checks `ElementsProtocol.parseRecord` applies, so the
        // two readers cannot disagree about what a usable orbit is.
        if (!eccentricity.isFinite() || eccentricity < 0.0 || eccentricity >= 1.0) return null
        if (!meanMotion.isFinite() || meanMotion <= 0.0) return null
        if (!inclination.isFinite() || Math.abs(inclination) > 180.0) return null

        return OmmElementSet(
            noradId = noradId,
            objectName = objectName,
            objectId = keywords["OBJECT_ID"],
            classificationType = keywords["CLASSIFICATION_TYPE"],
            epochUtc = epoch,
            elementSetNo = keywords["ELEMENT_SET_NO"]?.trim()?.toIntOrNull(),
            revAtEpoch = keywords["REV_AT_EPOCH"]?.trim()?.toLongOrNull(),
            ephemerisType = ephemerisType,
            meanMotionRevPerDay = meanMotion,
            eccentricity = eccentricity,
            inclinationDeg = inclination,
            raOfAscNodeDeg = raan,
            argOfPericenterDeg = argPericenter,
            meanAnomalyDeg = meanAnomaly,
            bstar = bstar,
            meanMotionDotRevPerDay2 = meanMotionDot,
            meanMotionDotDotRevPerDay3 = meanMotionDotDot,
            payload = toKvn(keywords),
        )
    }

    /**
     * Writes one element set as a **complete** OMM keyword record.
     *
     * The header and metadata keys CelesTrak's OMM payload does not carry — `CCSDS_OMM_VERS`,
     * `CREATION_DATE`, `ORIGINATOR`, `CENTER_NAME`, `REF_FRAME`, `TIME_SYSTEM`,
     * `MEAN_ELEMENT_THEORY` — are supplied here, because Orekit's parser requires them and the
     * source omits them. The values are the CCSDS ones that describe the data as the source sends
     * it: TEME, UTC, and the SGP/SGP4 theory CelesTrak states. They are constants, not guesses about
     * the orbit; every number that describes the orbit comes from the record itself.
     */
    fun toKvn(set: OmmElementSet): String = buildString {
        appendLine("CCSDS_OMM_VERS = 2.0")
        appendLine("CREATION_DATE = ${set.epochUtc}")
        appendLine("ORIGINATOR = CELESTRAK")
        appendLine("OBJECT_NAME = ${set.objectName}")
        set.objectId?.let { appendLine("OBJECT_ID = $it") }
        appendLine("CENTER_NAME = EARTH")
        appendLine("REF_FRAME = TEME")
        appendLine("TIME_SYSTEM = UTC")
        appendLine("MEAN_ELEMENT_THEORY = SGP/SGP4")
        appendLine("EPOCH = ${set.epochUtc}")
        appendLine("MEAN_MOTION = ${set.meanMotionRevPerDay}")
        appendLine("ECCENTRICITY = ${set.eccentricity}")
        appendLine("INCLINATION = ${set.inclinationDeg}")
        appendLine("RA_OF_ASC_NODE = ${set.raOfAscNodeDeg}")
        appendLine("ARG_OF_PERICENTER = ${set.argOfPericenterDeg}")
        appendLine("MEAN_ANOMALY = ${set.meanAnomalyDeg}")
        appendLine("EPHEMERIS_TYPE = ${set.ephemerisType}")
        appendLine("CLASSIFICATION_TYPE = ${(set.classificationType ?: "U").first()}")
        appendLine("NORAD_CAT_ID = ${set.noradId}")
        appendLine("ELEMENT_SET_NO = ${set.elementSetNo ?: 0}")
        appendLine("REV_AT_EPOCH = ${set.revAtEpoch ?: 0L}")
        appendLine("BSTAR = ${set.bstar}")
        appendLine("MEAN_MOTION_DOT = ${set.meanMotionDotRevPerDay2}")
        appendLine("MEAN_MOTION_DDOT = ${set.meanMotionDotDotRevPerDay3}")
    }

    /** The writer over a raw keyword map, used where the block is the record. */
    private fun toKvn(keywords: Map<String, String>): String =
        keywords.entries.joinToString(separator = "\n") { (key, value) -> "$key = $value" }
}

// ---------------------------------------------------------------------------------------------
// The engine
// ---------------------------------------------------------------------------------------------

/**
 * The satellite-pass engine: the **only** place in Lumintra that propagates an orbit.
 *
 * ### What it does, in the order the spec fixes
 *
 * 1. **Refuses before it computes.** Every element set is checked against the age bands
 *    (`age > 72 h` is refused outright, never propagated "just in case"), against a future `EPOCH`,
 *    and against an `EPHEMERIS_TYPE` that is not SGP4. Each refusal is a typed [ElementSetStatus]; a
 *    set is never silently dropped and no pass time is ever produced from a set the app does not
 *    trust.
 * 2. **Segments the search.** The window is the session's dark window, capped at **24 h**, walked in
 *    **3600 s** segments. Each segment gets a **fresh** `ElevationDetector` (`maxCheck = 60.0 s`,
 *    threshold `1e-6`) on a **fresh** propagator, and the propagation is
 *    `propagate(segmentStart, segmentEnd)`. This is not a style choice: a single propagation over a
 *    long window with one detector finds **1** acquisition where a brute-force scan finds **15**
 *    (`spec.md`, measured three times; edge case `E43`). Re-arming per segment is what makes the
 *    event search correct, and the events two adjacent segments both report are de-duplicated.
 * 3. **Accounts for the user's own horizon.** The detector's mask is an [ElevationMask] built on the
 *    same one-sample-per-degree grid the user's drawn [HorizonProfile] uses, holding
 *    `min(constantMask, drawnHorizon(azimuth))` — the lower of the two is what *detects*, so a pass
 *    behind a ridge is still found and can be reported as "rises behind your horizon" rather than
 *    vanishing; the drawn profile then decides visibility, and a pass whose peak is under the
 *    constant mask is not reported at all. A flat 0° horizon is never assumed: when the spot has a
 *    drawn profile, that profile *is* the mask at every azimuth.
 * 4. **Classifies in the spec's order.** `SatellitePass.visibilityOf` is the single classifier, so a
 *    pass in the Earth's shadow is `IN_EARTH_SHADOW` even when the sky is also too bright — the
 *    shadow is the more specific reason. The engine never reorders, averages or overrides it.
 * 5. **Samples once, reads everything.** The track is sampled at 1 s (capped at
 *    [MAX_TRACK_SAMPLES] per pass): the maximum elevation, the observable interval (above the mask
 *    **and** sunlit **and** observer-dark, with the spec's 30 s floor), the ground track and the
 *    lens-frame crossings all come out of that one walk.
 *
 * ### Threading and cancellation
 *
 * [findPasses] is `suspend`, runs on [OrekitRuntime.dispatcher] (`Dispatchers.Default`, never the
 * main thread) and checks for cancellation **between segments** — a 24-segment search over several
 * objects is seconds of work, so a cancelled search stops at the next segment boundary rather than
 * running to completion. It never blocks on I/O: everything it reads comes from the sets it was
 * handed, and it never touches a network — the only network client in this capability is
 * `ElementsRepository`, which runs before the engine is called.
 *
 * The per-request state (`topocentricFrame`, the drawn horizon, the constant mask, the detector
 * mask) is bound by [findPasses] for the duration of one call and unbound in a `finally`, so a
 * later caller cannot inherit a stale observer. One engine instance serves one `findPasses` call at
 * a time; the app uses one engine from one ViewModel.
 *
 * ### What it cannot claim
 *
 * It cannot claim the app is on the sky: the propagation is the one Orekit performs, and the
 * accuracy claim the product is allowed to make is propagator fidelity, never a pass time in seconds
 * (`spec.md` § "The only accuracy claim is propagator fidelity"). It also cannot make a prediction
 * correct — an element set's cross-track error grows with its age, which is exactly why the bands
 * are enforced here rather than in a screen.
 */
class SatellitePassEngine(private val runtime: OrekitRuntime) {

    /**
     * The engine over the app's single runtime, built from the bundled leap-second table.
     *
     * @param openLeapSeconds called by Orekit when it needs the leap-second table; it is the caller
     *        who owns the asset (see [OrekitRuntime.LEAP_SECOND_ASSET_PATH]).
     */
    constructor(openLeapSeconds: () -> java.io.InputStream) : this(OrekitRuntime.create(openLeapSeconds))

    /**
     * Computes every pass the request's sets produce inside the request's window.
     *
     * @return a [PassSearchOutcome] with the passes, the frame crossings, one [ElementSetStatus] per
     *         input set (including the refused ones) and, when the request as a whole could not be
     *         answered, a single [EngineFailure]. It never throws for a refused set and never returns
     *         an empty list in place of a failure.
     */
    suspend fun findPasses(request: PassSearchRequest): PassSearchOutcome {
        if (!runtime.isReady) {
            return PassSearchOutcome.failed(
                PassRefusalReason.ENGINE_NOT_READY,
                "OrekitRuntime.warmUp() has not completed",
            )
        }
        if (request.elementSets.isEmpty()) {
            return PassSearchOutcome.failed(PassRefusalReason.NO_ELEMENT_SETS, "the request carried no element set")
        }
        val window = clampWindow(request.window)
            ?: return PassSearchOutcome.failed(
                PassRefusalReason.NO_DARK_WINDOW,
                "the dark window ${request.window.startUtc}…${request.window.endUtc} is empty",
            )

        bind(request)
        return try {
            // The contract is that this never runs on the caller's thread: a 24-segment, multi-object
            // search is seconds of CPU-bound propagation, and the caller is a ViewModel's
            // viewModelScope on the main dispatcher. Dispatchers.Default is where it belongs.
            withContext(runtime.dispatcher) { search(request, window) }
        } finally {
            unbind()
        }
    }

    /** The search itself, with the per-request state already bound. */
    private suspend fun search(request: PassSearchRequest, window: NightSpan): PassSearchOutcome {
        val statuses = ArrayList<ElementSetStatus>(request.elementSets.size)
        val computations = ArrayList<PassComputation>()
        val crossings = ArrayList<FrameCrossingSpan>()
        var segments = 0
        var events = 0

        for (set in request.elementSets) {
            coroutineContext.ensureActive()
            val age = set.ageAt(request.sessionInstantUtc)
            val refusal = refusalFor(set, age, request.sessionInstantUtc)
            if (refusal != null) {
                statuses.add(status(set, age, computed = false, refusal = refusal))
                continue
            }

            val build = orbitFor(set)
            when (build) {
                is OrbitBuild.Refused -> {
                    statuses.add(status(set, age, computed = false, refusal = build.reason))
                    continue
                }

                is OrbitBuild.Ok -> {
                    val segmentResult = try {
                        searchSegments(build.tle, window)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (e: OrekitException) {
                        // Abort rather than continue: a partially searched window would be reported
                        // as fewer passes, which reads exactly like "nothing happened tonight".
                        return PassSearchOutcome.failed(
                            PassRefusalReason.PROPAGATION_FAILED,
                            "propagation failed for ${set.objectName} (${e.javaClass.simpleName}): ${e.message}",
                            statuses,
                        )
                    }
                    segments += segmentResult.segments
                    events += segmentResult.events.size
                    statuses.add(status(set, age, computed = true, refusal = null))

                    val paired = pairEvents(segmentResult.events)
                    for ((aos, los) in paired) {
                        coroutineContext.ensureActive()
                        val computation = evaluate(
                            set = set,
                            tle = build.tle,
                            aos = aos,
                            los = los,
                            request = request,
                        ) ?: continue
                        computations.add(computation)
                        crossings.addAll(crossingSpans(computation, request))
                    }
                }
            }
        }

        computations.sortBy { it.pass.aosUtc }
        crossings.sortBy { it.entryUtc }
        return PassSearchOutcome(
            passes = computations.map { it.pass },
            computations = computations,
            crossings = crossings,
            elementSets = statuses,
            segmentsSearched = segments,
            eventsLogged = events,
            failure = null,
        )
    }

    // -----------------------------------------------------------------------------------------
    // Refusal rules
    // -----------------------------------------------------------------------------------------

    private fun refusalFor(
        set: OmmElementSet,
        age: ElementSetAge,
        sessionInstantUtc: Instant,
    ): PassRefusalReason? = when {
        set.isEpochInFuture(sessionInstantUtc) -> PassRefusalReason.ELEMENT_SET_EPOCH_IN_FUTURE
        age.band == ElementSetAgeBand.EXPIRED -> PassRefusalReason.ELEMENT_SET_EXPIRED
        !set.isSgp4 -> PassRefusalReason.UNSUPPORTED_EPHEMERIS_TYPE
        else -> null
    }

    private fun status(
        set: OmmElementSet,
        age: ElementSetAge,
        computed: Boolean,
        refusal: PassRefusalReason?,
    ): ElementSetStatus = ElementSetStatus(
        noradId = set.noradId,
        satelliteName = set.objectName,
        objectId = set.objectId,
        epochUtc = set.epochUtc,
        age = age,
        computed = computed,
        refusal = refusal,
    )

    /** The window, capped at [MAX_WINDOW_HOURS]; `null` when it is empty or inverted. */
    private fun clampWindow(window: NightSpan): NightSpan? {
        val start = window.startUtc
        val capped = window.endUtc.let { end ->
            val limit = start.plus(Duration.ofHours(MAX_WINDOW_HOURS))
            if (end.isAfter(limit)) limit else end
        }
        return if (capped.isAfter(start)) NightSpan(start, capped) else null
    }

    // -----------------------------------------------------------------------------------------
    // The segmented event search
    // -----------------------------------------------------------------------------------------

    private class SegmentResult(val segments: Int, val events: List<TimedEvent>)

    private class TimedEvent(val instantUtc: Instant, val ascending: Boolean)

    /**
     * Walks `[start, end]` in [SEGMENT_SECONDS] slices, re-arming a fresh detector on a fresh
     * propagator for each one, and returns the de-duplicated events.
     *
     * The two-argument `propagate(segmentStart, segmentEnd)` is what bounds the event search to the
     * slice; a detector that has already fired inside one call is not re-armed, which is the
     * measured trap this method exists to avoid.
     *
     * Exactly **one** detector is monitored per propagator — this engine's own elevation detector —
     * so every event the logger reports belongs to it and no detector-identity filter is needed or
     * possible to get wrong.
     */
    private suspend fun searchSegments(tle: TLE, window: NightSpan): SegmentResult {
        val events = ArrayList<TimedEvent>()
        var segments = 0
        var cursor = window.startUtc
        while (cursor.isBefore(window.endUtc)) {
            coroutineContext.ensureActive()
            val next = minOf(cursor.plusSeconds(SEGMENT_SECONDS), window.endUtc)
            val start = runtime.absoluteDate(cursor)
            val end = runtime.absoluteDate(next)

            val propagator = TLEPropagator.selectExtrapolator(tle, runtime.teme)
            val logger = EventsLogger()
            propagator.addEventDetector(logger.monitorDetector(freshDetector()))
            propagator.propagate(start, end)
            segments++

            for (logged in logger.getLoggedEvents()) {
                events.add(TimedEvent(runtime.instantOf(logged.getDate()), logged.isIncreasing()))
            }
            cursor = next
        }
        events.sortBy { it.instantUtc }
        return SegmentResult(segments, dedupe(events))
    }

    /**
     * A **fresh** elevation detector for one segment, with the mask this search must clear.
     *
     * The mask is `min(constantMask, drawnHorizon(azimuth))` at each of the 360 whole-degree
     * azimuths — see the class KDoc: the lower of the two is what *detects*, and the drawn profile is
     * what decides *visibility*. The detector is rebuilt for every segment because a detector that
     * has already fired is not re-armed inside one `propagate` call.
     */
    private fun freshDetector(): ElevationDetector {
        val topocentric = requireNotNull(topocentricFrame) { "the search did not build its topocentric frame" }
        val mask = requireNotNull(detectionMask) { "the search did not build its elevation mask" }
        return ElevationDetector(topocentric)
            .withElevationMask(mask)
            .withMaxCheck(MAX_CHECK_SECONDS)
            .withThreshold(THRESHOLD)
    }

    /**
     * Drops a duplicate of the event a neighbouring segment already reported.
     *
     * Two segments can both log the same crossing because the pair shares a boundary instant: the
     * event at exactly `segmentEnd` is logged by the segment that ends there and found again by the
     * one that starts there. Only an event of the **same direction** inside
     * [BOUNDARY_DEDUP_SECONDS] is dropped, so a genuinely short pass (up and down inside two
     * seconds) is not erased.
     */
    private fun dedupe(events: List<TimedEvent>): List<TimedEvent> {
        val kept = ArrayList<TimedEvent>(events.size)
        for (event in events) {
            val previous = kept.lastOrNull()
            if (previous != null &&
                previous.ascending == event.ascending &&
                Duration.between(previous.instantUtc, event.instantUtc).abs().seconds <= BOUNDARY_DEDUP_SECONDS
            ) {
                continue
            }
            kept.add(event)
        }
        return kept
    }

    /** Pairs an acquisition with the loss that ends it, in time order. */
    private fun pairEvents(events: List<TimedEvent>): List<Pair<Instant, Instant>> {
        val pairs = ArrayList<Pair<Instant, Instant>>()
        var pendingAcquisition: TimedEvent? = null
        for (event in events) {
            if (event.ascending) {
                if (pendingAcquisition == null) pendingAcquisition = event
            } else {
                val acquisition = pendingAcquisition
                if (acquisition != null && !event.instantUtc.isBefore(acquisition.instantUtc)) {
                    pairs.add(acquisition.instantUtc to event.instantUtc)
                    pendingAcquisition = null
                }
            }
        }
        return pairs
    }

    // -----------------------------------------------------------------------------------------
    // One pass, read once
    // -----------------------------------------------------------------------------------------

    private data class Sample(
        val instantUtc: Instant,
        val elevationDeg: Double,
        val azimuthDeg: Double,
        val rangeKm: Double,
        val sunlit: Boolean,
        val sunAltitudeDeg: Double,
        val latitudeDeg: Double,
        val longitudeDeg: Double,
        val altitudeKm: Double,
        /** Above the constant mask **and** above the drawn horizon at this sample's azimuth. */
        val aboveMask: Boolean,
    )

    private fun evaluate(
        set: OmmElementSet,
        tle: TLE,
        aos: Instant,
        los: Instant,
        request: PassSearchRequest,
    ): PassComputation? {
        val propagator = TLEPropagator.selectExtrapolator(tle, runtime.teme)
        val topocentric = requireNotNull(topocentricFrame) { "the search did not build its topocentric frame" }
        val seconds = Duration.between(aos, los).seconds.toDouble()
        if (seconds <= 0.0) return null
        val step = maxOf(SAMPLE_STEP_SECONDS, seconds / MAX_TRACK_SAMPLES)

        val samples = ArrayList<Sample>(MIN_TRACK_SAMPLES)
        var cursor = aos
        while (!cursor.isAfter(los)) {
            val date = runtime.absoluteDate(cursor)
            val state = propagator.propagate(date)
            samples.add(sampleAt(state, date, cursor, topocentric, request))
            cursor = cursor.plusMillis((step * 1000.0).toLong())
        }
        if (samples.isEmpty()) return null

        val peak = samples.maxByOrNull { it.elevationDeg } ?: return null

        // Step 4 of the normative search: a pass is kept only if it clears the constant mask. The
        // drawn horizon is a visibility reason (below), not a drop.
        if (peak.elevationDeg < request.maskDeg) return null

        val elevationVerdict = if (isBehindDrawnHorizon(peak)) {
            ElevationVerdict.BEHIND_DRAWN_HORIZON
        } else {
            ElevationVerdict.ABOVE_HORIZON
        }

        val observable = samples.filter { it.aboveMask && it.sunlit && it.sunAltitudeDeg <= request.sunThresholdDeg }
        val observableSeconds = if (observable.isEmpty()) {
            0.0
        } else {
            Duration.between(observable.first().instantUtc, observable.last().instantUtc).seconds.toDouble() + step
        }

        val aosSample = samples.first()
        val losSample = samples.last()
        val farthest = samples.maxByOrNull { it.rangeKm } ?: peak
        val visibility = SatellitePass.visibilityOf(
            satelliteSunlit = peak.sunlit,
            observerSunAltitudeDeg = peak.sunAltitudeDeg,
            sunThresholdDeg = request.sunThresholdDeg,
        )
        val crossing = frameCrossing(samples, request, observableSeconds, step)

        val pass = SatellitePass(
            noradId = set.noradId,
            satelliteName = set.objectName,
            objectId = set.objectId,
            aosUtc = aos,
            losUtc = los,
            maxElevationDeg = peak.elevationDeg,
            maxElevationUtc = peak.instantUtc,
            subPointTrack = groundTrack(samples),
            visibility = visibility,
            observerSunAltitudeDeg = peak.sunAltitudeDeg,
            satelliteSunlit = peak.sunlit,
            elementSetEpochUtc = set.epochUtc,
            elementSetAge = set.ageAt(request.sessionInstantUtc),
            frameCrossing = crossing,
        )

        return PassComputation(
            pass = pass,
            elevationVerdict = elevationVerdict,
            observableIntervalSeconds = observableSeconds,
            observableInterval = Duration.ofMillis((observableSeconds * 1000.0).toLong()),
            clearsIntervalFloor = observableSeconds >= MIN_OBSERVABLE_SECONDS,
            maxRangeKm = farthest.rangeKm,
            aosAzimuthDeg = aosSample.azimuthDeg,
            peakAzimuthDeg = peak.azimuthDeg,
            losAzimuthDeg = losSample.azimuthDeg,
        )
    }

    private fun sampleAt(
        state: SpacecraftState,
        date: AbsoluteDate,
        instantUtc: Instant,
        topocentric: TopocentricFrame,
        request: PassSearchRequest,
    ): Sample {
        val position = state.getPVCoordinates().getPosition()
        val frame = state.getFrame()
        val elevation = Math.toDegrees(topocentric.getElevation(position, frame, date))
        val azimuth = Math.toDegrees(topocentric.getAzimuth(position, frame, date))
        val rangeKm = topocentric.getRange(position, frame, date) / 1000.0
        val subPoint = runtime.earth.transform(position, frame, date)
        return Sample(
            instantUtc = instantUtc,
            elevationDeg = elevation,
            azimuthDeg = azimuth,
            rangeKm = rangeKm,
            sunlit = isSunlit(state),
            sunAltitudeDeg = request.sunAltitude.geometricAltitudeDeg(instantUtc),
            latitudeDeg = Math.toDegrees(subPoint.getLatitude()),
            longitudeDeg = Math.toDegrees(subPoint.getLongitude()),
            altitudeKm = subPoint.getAltitude() / 1000.0,
            aboveMask = elevation >= visibleMaskDeg(azimuth),
        )
    }

    /** True when the drawn profile, at the peak's own azimuth, is above the pass's maximum. */
    private fun isBehindDrawnHorizon(peak: Sample): Boolean {
        val horizon = requestScopedHorizon ?: return false
        return peak.elevationDeg < horizon.elevationDegAt(peak.azimuthDeg)
    }

    /**
     * The mask a sample must clear to count as "above the horizon" for the observable interval: the
     * constant mask, and the user's drawn horizon when there is one. A pass behind the ridge has an
     * observable interval of zero, so it cannot clear the 30 s floor — which is exactly the spec's
     * "the drawn horizon is the mask" rule, and never a flat 0° assumption.
     */
    private fun visibleMaskDeg(azimuthDeg: Double): Double {
        val horizon = requestScopedHorizon ?: return requestScopedMaskDeg
        val azimuth = HorizontalCoords.normaliseAzimuth(azimuthDeg)
        return maxOf(requestScopedMaskDeg, horizon.elevationDegAt(azimuth))
    }

    private fun groundTrack(samples: List<Sample>): List<GroundTrackPoint> {
        if (samples.isEmpty()) return emptyList()
        val stride = maxOf(1, samples.size / GROUND_TRACK_TARGET_POINTS)
        return samples.filterIndexed { index, _ -> index % stride == 0 }.map { sample ->
            GroundTrackPoint(
                instantUtc = sample.instantUtc,
                latDeg = sample.latitudeDeg,
                lonDeg = sample.longitudeDeg,
                altitudeKm = sample.altitudeKm,
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // Sunlit: the umbra test
    // -----------------------------------------------------------------------------------------

    /**
     * True when the satellite is **not** inside the Earth's umbra, tested against the **WGS84
     * ellipsoid** by Orekit's own occultation geometry.
     *
     * The test is `org.orekit.utils.OccultationEngine(sun, SUN_RADIUS, earth).angles(state)`: for the
     * satellite as the observer, it returns three angles — the **separation** between the centres of
     * the Sun and the Earth, the **limb radius** of the Earth (computed with
     * `OneAxisEllipsoid.pointOnLimb`, so the flattening is in the number) and the **occulted apparent
     * radius** of the Sun. The Sun's disc is entirely covered exactly when
     * `separation + occultedApparentRadius ≤ limbRadius`, which is the umbra.
     *
     * That formula is the one part of this method not measured from bytecode. `javap
     * -classpath .kiln/scratch/android-orekit-smoke/app/libs/orekit-13.1.5.jar` (the smoke
     * project's jar; this slot pins 13.1.9) measured the **surface**: the constructor
     * `OccultationEngine(ExtendedPositionProvider occulted, double occultedRadius,
     * OneAxisEllipsoid occulting)`, `angles(SpacecraftState)` returning
     * `OccultationEngine$OccultationAngles`, and its three accessors `getSeparation()`,
     * `getLimbRadius()` and `getOccultedApparentRadius()`. The **direction** of the comparison is
     * read from what those three numbers mean — a body is hidden when its separation plus its
     * apparent radius does not clear the occulting limb's radius — and has not been checked against
     * an Orekit run. Using Orekit's geometry rather than a hand-rolled cone test is what makes
     * "against the WGS84 ellipsoid" true rather than asserted, and it is the **one** shadow model:
     * [com.gexetr.lumintralightplanner.domain.PassVisibility] is decided from this boolean and
     * nothing else re-derives a shadow.
     *
     * **The penumbra is deliberately visible.** A satellite the Earth only partly blocks is *not* in
     * the umbra, so it is reported as sunlit — which is the spec's "the penumbra means
     * visible-but-fading", stated as one rule instead of two.
     *
     * The state handed to `angles` is the propagator's own, unchanged, and the occulting body is
     * [OrekitRuntime.earth] — the same WGS84 ellipsoid the ground track and the sub-satellite point
     * are computed against — so the geometry is Orekit's and the shape is the one the app already
     * uses. Which frame Orekit converts into internally is Orekit's business and is not asserted
     * here.
     */
    private fun isSunlit(state: SpacecraftState): Boolean {
        val angles = occultation().angles(state)
        return angles.separation + angles.occultedApparentRadius > angles.limbRadius
    }

    /**
     * The occultation engine, built once per bound request. It holds no per-sample state, so one
     * instance serves the whole search; it is dropped by [unbind] with the rest of the request state.
     */
    private fun occultation(): OccultationEngine {
        val existing = occultationEngine
        if (existing != null) return existing
        val created = OccultationEngine(runtime.sun, Constants.SUN_RADIUS, runtime.earth)
        occultationEngine = created
        return created
    }

    // -----------------------------------------------------------------------------------------
    // The lens frame
    // -----------------------------------------------------------------------------------------

    /**
     * The maximal run of samples whose projection lies inside the frame, when there is one.
     *
     * The projection is the spec's equidistant one: `pxPerDeg = canvasWidthPx / sceneFovHorizontalDeg`
     * is one constant, so "inside the pixel rectangle" and "inside the angular rectangle" are the same
     * question and the engine can answer it in degrees. A sample's offset from the aim point is
     * `Δaltitude` vertically and `Δazimuth · cos(altitude)` horizontally — the angular distance the
     * azimuth difference actually spans on the sky — rotated by the frame's roll.
     *
     * The exposure rule is the spec's: the maximal inside run must last at least the session's
     * exposure, except where the exposure is at least as long as the observable interval, in which
     * case the whole observable interval is one exposure and any inside run is a crossing. A crossing
     * is **predicted**, and the confidence says how much of it the element data can support — an
     * element set's cross-track error grows with its age, so a grazing, distant or fast pass never
     * earns `HIGH`.
     */
    private fun frameCrossing(
        samples: List<Sample>,
        request: PassSearchRequest,
        observableSeconds: Double,
        stepSeconds: Double,
    ): FrameCrossing {
        val lens = request.lens ?: return FrameCrossing.UNKNOWN
        if (samples.isEmpty()) return FrameCrossing.UNKNOWN

        var runStart = -1
        var bestStart = -1
        var bestCount = 0
        var closest = Double.MAX_VALUE
        for ((index, sample) in samples.withIndex()) {
            val offset = offsetDeg(sample, lens)
            val separation = Math.hypot(offset.first, offset.second)
            if (separation < closest) closest = separation
            val inside = Math.abs(offset.first) <= lens.fieldOfView.fovH / 2.0 &&
                Math.abs(offset.second) <= lens.fieldOfView.fovV / 2.0
            if (inside) {
                if (runStart < 0) runStart = index
                val count = index - runStart + 1
                if (count > bestCount) {
                    bestCount = count
                    bestStart = runStart
                }
            } else {
                runStart = -1
            }
        }

        val closestDeg = closest.takeIf { it.isFinite() }
        if (bestStart < 0 || bestCount <= 0) {
            return FrameCrossing(
                crosses = false,
                closestApproachDeg = closestDeg,
                confidence = confidenceFor(samples, inside = false),
            )
        }

        val runSeconds = bestCount * stepSeconds
        val exposure = request.exposureSeconds
        val covered = when {
            exposure <= 0.0 -> true
            observableSeconds > 0.0 && exposure >= observableSeconds -> true
            else -> runSeconds >= exposure
        }
        return FrameCrossing(
            crosses = covered,
            closestApproachDeg = closestDeg,
            confidence = confidenceFor(samples, inside = true),
        )
    }

    /** The angular offset of a sample from the frame's aim point, in degrees, in the frame's own axes. */
    private fun offsetDeg(sample: Sample, lens: LensFrame): Pair<Double, Double> {
        val azimuthDelta = wrapSignedDegrees(sample.azimuthDeg - lens.aimAzimuthDeg)
        val horizontal = azimuthDelta * Math.cos(Math.toRadians(sample.elevationDeg.coerceIn(-89.0, 89.0)))
        val vertical = sample.elevationDeg - lens.aimAltitudeDeg
        val roll = Math.toRadians(lens.rollDeg)
        val x = horizontal * Math.cos(roll) + vertical * Math.sin(roll)
        val y = -horizontal * Math.sin(roll) + vertical * Math.cos(roll)
        return x to y
    }

    /**
     * How much the element data can support. A pass close enough to fill the frame, at low range and
     * with a slow track, is `HIGH`; a grazing approach is `MEDIUM`; a distant or fast one is `LOW`.
     * These thresholds are this app's own — the spec names the four levels and not their boundaries.
     */
    private fun confidenceFor(samples: List<Sample>, inside: Boolean): FrameCrossingConfidence {
        if (samples.isEmpty()) return FrameCrossingConfidence.UNKNOWN
        val closestRange = samples.minOf { it.rangeKm }
        val farthestRange = samples.maxOf { it.rangeKm }
        return when {
            closestRange > FAR_RANGE_KM || farthestRange > VERY_FAR_RANGE_KM -> FrameCrossingConfidence.LOW
            inside && closestRange <= NEAR_RANGE_KM -> FrameCrossingConfidence.HIGH
            inside -> FrameCrossingConfidence.MEDIUM
            else -> FrameCrossingConfidence.LOW
        }
    }

    /** The crossing's entry and exit instants, from the sample run the crossing was found in. */
    private fun crossingSpans(computation: PassComputation, request: PassSearchRequest): List<FrameCrossingSpan> {
        val lens = request.lens ?: return emptyList()
        if (!computation.pass.frameCrossing.crosses) return emptyList()
        val topocentric = topocentricFrame ?: return emptyList()
        val propagator = try {
            TLEPropagator.selectExtrapolator(orbitOf(computation.pass.noradId, request), runtime.teme)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            return emptyList()
        }

        var entry: Instant? = null
        var exit: Instant? = null
        var inside = 0
        var cursor = computation.pass.aosUtc
        while (!cursor.isAfter(computation.pass.losUtc)) {
            val date = runtime.absoluteDate(cursor)
            val state = propagator.propagate(date)
            val sample = sampleAt(state, date, cursor, topocentric, request)
            val offset = offsetDeg(sample, lens)
            val isInside = Math.abs(offset.first) <= lens.fieldOfView.fovH / 2.0 &&
                Math.abs(offset.second) <= lens.fieldOfView.fovV / 2.0
            if (isInside) {
                if (entry == null) entry = cursor
                exit = cursor
                inside++
            }
            cursor = cursor.plusMillis((SAMPLE_STEP_SECONDS * 1000.0).toLong())
        }
        val start = entry ?: return emptyList()
        val end = exit ?: return emptyList()
        return listOf(
            FrameCrossingSpan(
                noradId = computation.pass.noradId,
                satelliteName = computation.pass.satelliteName,
                entryUtc = start,
                exitUtc = end,
                samplesInside = inside,
                crossing = computation.pass.frameCrossing,
            ),
        )
    }

    private fun orbitOf(noradId: Long, request: PassSearchRequest): TLE {
        val set = request.elementSets.first { it.noradId == noradId }
        val build = orbitFor(set)
        require(build is OrbitBuild.Ok) { "element set ${set.noradId} was not propagatable" }
        return build.tle
    }

    // -----------------------------------------------------------------------------------------
    // Building the orbit
    // -----------------------------------------------------------------------------------------

    private sealed interface OrbitBuild {
        data class Ok(val tle: TLE) : OrbitBuild

        data class Refused(val reason: PassRefusalReason, val detail: String) : OrbitBuild
    }

    /**
     * Turns one element set into a `TLE`, by Orekit's own OMM support first and by the documented
     * field mapping second.
     *
     * The first path is the spec's requirement: write the record as OMM keyword text and let
     * `org.orekit.files.ccsds.ndm.odm.omm.OmmParser` build it, then ask `Omm.generateTLE()`. The
     * second is the spec's fallback contract — the same fields through the propagator's own
     * constructor, with every unit converted by [OmmUnits] — and it exists because the primary path
     * depends on a parser accepting a record whose header this app synthesises, which is exactly the
     * kind of assumption that must not be able to make an object **disappear**. A set that neither
     * path can build is refused with a typed reason, never dropped silently.
     *
     * The fallback carries the propagator's `int` catalogue-number field, so a number wider than
     * `Int.MAX_VALUE` cannot be constructed that way; it is refused by name rather than truncated,
     * and the primary OMM path is the one that can carry such an identity. Nothing here ever
     * substitutes or shortens a catalogue number.
     */
    private fun orbitFor(set: OmmElementSet): OrbitBuild {
        val utc = runtime.utc
        val keywordText = OmmKeyword.toKvn(set)

        val fromOmm = try {
            val parser = ParserBuilder(runtime.dataContext).withSimpleEOP(true).buildOmmParser()
            val source = DataSource(
                OMM_SOURCE_NAME,
                DataSource.StreamOpener { keywordText.byteInputStream(Charsets.UTF_8) },
            )
            val omm: Omm = parser.parseMessage(source)
            omm.generateTLE()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            null
        }

        if (fromOmm != null) return OrbitBuild.Ok(fromOmm)

        val norad = set.noradId
        if (norad > Int.MAX_VALUE) {
            return OrbitBuild.Refused(
                PassRefusalReason.ORBIT_NOT_CONSTRUCTED,
                "catalogue number $norad is wider than the propagator's field",
            )
        }
        val launchYear = launchYearOf(set.objectId)
        val launchPiece = launchPieceOf(set.objectId)
        val launchNumber = launchNumberOf(set.objectId)
        return try {
            val tle = TLE(
                norad.toInt(),
                (set.classificationType ?: "U").first(),
                launchYear,
                launchNumber,
                launchPiece,
                set.ephemerisType,
                set.elementSetNo ?: 0,
                runtime.absoluteDate(set.epochUtc),
                OmmUnits.meanMotionToRadPerSecond(set.meanMotionRevPerDay),
                OmmUnits.meanMotionDotToRadPerSecond2(set.meanMotionDotRevPerDay2),
                OmmUnits.meanMotionDotDotToRadPerSecond3(set.meanMotionDotDotRevPerDay3),
                set.eccentricity,
                OmmUnits.toRadians(set.inclinationDeg),
                OmmUnits.wrapToTwoPi(OmmUnits.toRadians(set.argOfPericenterDeg)),
                OmmUnits.wrapToTwoPi(OmmUnits.toRadians(set.raOfAscNodeDeg)),
                OmmUnits.wrapToTwoPi(OmmUnits.toRadians(set.meanAnomalyDeg)),
                (set.revAtEpoch ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                set.bstar,
                utc,
            )
            OrbitBuild.Ok(tle)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            OrbitBuild.Refused(
                PassRefusalReason.ORBIT_NOT_CONSTRUCTED,
                "neither Orekit's OMM parser nor the field mapping built ${set.objectName}: ${e.message}",
            )
        }
    }

    private fun launchYearOf(objectId: String?): Int =
        objectId?.substringBefore('-')?.trim()?.toIntOrNull()?.coerceIn(0, 9999) ?: 0

    private fun launchNumberOf(objectId: String?): Int {
        val middle = objectId?.split('-')?.getOrNull(1) ?: return 0
        return middle.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    }

    private fun launchPieceOf(objectId: String?): String =
        objectId?.split('-')?.getOrNull(2)?.trim()?.take(3) ?: ""

    // -----------------------------------------------------------------------------------------
    // Search state scoped to one request
    // -----------------------------------------------------------------------------------------

    private var requestScopedHorizon: HorizonMask? = null
    private var requestScopedMaskDeg: Double = SatellitePass.DEFAULT_MASK_DEG
    private var topocentricFrame: TopocentricFrame? = null
    private var detectionMask: ElevationMask? = null

    /** The Sun–Earth occultation geometry, built once per bound request (see [occultation]). */
    private var occultationEngine: OccultationEngine? = null

    /**
     * Binds the per-request state the segment loop needs — the topocentric frame of the observer, the
     * drawn horizon, the constant mask and the detector's [ElevationMask]. It is set once per search
     * and read by the detector and by [evaluate]; the engine is not documented as safe for two
     * concurrent [findPasses] calls on one instance, and the app uses one engine from one ViewModel.
     */
    private fun bind(request: PassSearchRequest) {
        topocentricFrame = runtime.topocentricFrame(request.observer)
        requestScopedHorizon = request.horizon
        requestScopedMaskDeg = request.maskDeg
        detectionMask = buildDetectionMask(request.horizon, request.maskDeg)
    }

    /** Unbinds the per-request state, so a later caller cannot inherit a stale observer. */
    private fun unbind() {
        topocentricFrame = null
        requestScopedHorizon = null
        requestScopedMaskDeg = SatellitePass.DEFAULT_MASK_DEG
        detectionMask = null
        occultationEngine = null
    }

    /**
     * The detector's mask, on the same `1°`-per-azimuth grid the drawn [HorizonProfile] uses — the
     * only mask form Orekit's `ElevationDetector` accepts as an azimuth-dependent obstruction.
     *
     * `Orekit`'s `ElevationMask` takes `{azimuth, elevation}` rows **in radians** and builds its own
     * wrap-around point, and it rejects two rows sharing one normalised azimuth — which is why the
     * grid is `0°…359°` and never repeats `0°` as `360°`.
     */
    private fun buildDetectionMask(horizon: HorizonMask?, constantDeg: Double): ElevationMask {
        val points = Array(MASK_SAMPLES) { index ->
            val azimuthDeg = index.toDouble()
            val horizonDeg = horizon?.elevationDegAt(azimuthDeg) ?: constantDeg
            doubleArrayOf(
                OmmUnits.toRadians(azimuthDeg),
                OmmUnits.toRadians(minOf(constantDeg, horizonDeg)),
            )
        }
        return ElevationMask(points)
    }

    /** The signed difference of two azimuths, in `(-180°, +180°]`. */
    private fun wrapSignedDegrees(degrees: Double): Double {
        val wrapped = HorizontalCoords.normaliseAzimuth(degrees)
        return if (wrapped > 180.0) wrapped - 360.0 else wrapped
    }

    companion object {
        /** The single segment length: `3600 s`, the spec's measured-correct one. */
        const val SEGMENT_SECONDS: Long = 3_600L

        /** `maxCheck` for every segment's detector. */
        const val MAX_CHECK_SECONDS: Double = 60.0

        /** The detectors' default threshold, stated rather than inherited silently. */
        const val THRESHOLD: Double = 1.0e-6

        /** The window cap: a longer dark window is searched one hour at a time to at most this. */
        const val MAX_WINDOW_HOURS: Long = 24L

        /** Two events of the same direction closer than this are one event. */
        const val BOUNDARY_DEDUP_SECONDS: Long = 2L

        /** The spec's floor for the intersection of above-the-mask, sunlit and observer-dark. */
        const val MIN_OBSERVABLE_SECONDS: Double = 30.0

        /** The sampling step of the track read: the spec's 1 s. */
        const val SAMPLE_STEP_SECONDS: Double = 1.0

        /** The cap on samples per pass, so a long pass cannot cost an unbounded walk. */
        const val MAX_TRACK_SAMPLES: Double = 1_800.0

        private const val MIN_TRACK_SAMPLES: Int = 32

        /** Ground-track points reported per pass. */
        const val GROUND_TRACK_TARGET_POINTS: Int = 60

        /** One detector-mask sample per whole degree of azimuth — the drawn profile's own grid. */
        const val MASK_SAMPLES: Int = 360

        /** The range below which a frame crossing can be `HIGH`. */
        const val NEAR_RANGE_KM: Double = 1_500.0

        /** The range above which the frame question is not answerable from an element set. */
        const val FAR_RANGE_KM: Double = 3_000.0

        private const val VERY_FAR_RANGE_KM: Double = 6_000.0

        /** The name Orekit's parser sees the synthesised record under. */
        const val OMM_SOURCE_NAME: String = "lumintra-omm.kvn"
    }
}
