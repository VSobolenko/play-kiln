package com.gexetr.lumintralightplanner.data

import android.content.Context
import android.util.Log
import com.gexetr.lumintralightplanner.BuildConfig
import com.gexetr.lumintralightplanner.domain.ElementSetAge
import com.gexetr.lumintralightplanner.domain.ElementSetAgeBand
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Which CelesTrak element group is being tracked.
 *
 * The set is deliberately small. `STATIONS` and `VISUAL` (the 100 brightest) are the two groups the
 * app tracks unasked, and a user-named object is fetched one catalogue number at a time; pulling the
 * whole `ACTIVE` list is the traffic the source's policy explicitly asks clients to avoid
 * (`design.md` § "Satellites").
 */
sealed interface SatelliteGroup {

    /** The literal value of the request's `GROUP` parameter. */
    val wireValue: String

    /** A filesystem-safe, locale-independent name for this group's cache files. */
    val cacheSlug: String

    /** True when the group names one object rather than a list of them. */
    val isSingleObject: Boolean get() = this is Catnr

    /** The space stations. */
    data object Stations : SatelliteGroup {
        override val wireValue: String get() = "STATIONS"

        override val cacheSlug: String get() = "stations"
    }

    /** The 100 brightest objects. */
    data object Visual : SatelliteGroup {
        override val wireValue: String get() = "VISUAL"

        override val cacheSlug: String get() = "visual"
    }

    /** One user-named object, addressed by its catalogue number. */
    data class Catnr(val noradId: Long) : SatelliteGroup {
        init {
            require(noradId > 0L) { "noradId $noradId is not a catalogue number" }
        }

        override val wireValue: String get() = "CATNR=$noradId"

        override val cacheSlug: String get() = "catnr-$noradId"
    }

    companion object {
        /** The groups tracked when the user has named nothing. */
        val DEFAULT_TRACKED: List<SatelliteGroup> = listOf(Stations, Visual)

        /**
         * The inverse of [wireValue], for a UI that stores the tracked set. A value the app does not
         * understand is `null` rather than a fabricated group.
         */
        fun fromWire(wire: String): SatelliteGroup? = when {
            wire == Stations.wireValue -> Stations
            wire == Visual.wireValue -> Visual
            wire.startsWith("CATNR=") -> wire.removePrefix("CATNR=").toLongOrNull()?.takeIf { it > 0L }?.let(::Catnr)
            else -> null
        }
    }
}

/**
 * The endpoint, the request and the OMM/JSON parse — **all of it pure**, so the pieces the spec pins
 * down (the URL, the `User-Agent`, the field mapping and the unit conversion) are unit-testable
 * without a device, a network or a coroutine.
 *
 * Four facts are load-bearing:
 *
 * 1. **The response is OMM/JSON, never fixed-width TLE.** CelesTrak exhausted the 5-digit catalogue
 *    space on 2026-07-11 and does not publish TLE text for newly catalogued objects, so the parser
 *    reads OMM keys and accepts a 6-digit `NORAD_CAT_ID` (`100000+`) as a first-class value.
 * 2. **The client is keyless.** There is no key, no token and no account anywhere in this file or in
 *    the request it builds.
 * 3. **The request is identified** by `User-Agent: <app>/<versionName> (<contact>)`. The identity is
 *    defined **once**, in [CloudProtocol], and imported here rather than restated, so the satellite
 *    and the weather clients cannot drift apart in the string both services read.
 * 4. **Units are converted before anything else sees them.** OMM gives angles in degrees and mean
 *    motion in revolutions per day; the propagator takes radians and radians per second. Passing
 *    `15.48769982` through unconverted is the most likely silent defect in this path.
 */
object ElementsProtocol {

    const val SCHEME: String = "https"
    const val HOST: String = "celestrak.org"

    /** The one and only element path this app knows. No fallback provider exists. */
    const val PATH: String = "/NORAD/elements/gp.php"

    const val BASE_URL: String = "https://celestrak.org/NORAD/elements/gp.php"

    const val PARAM_GROUP: String = "GROUP"
    const val PARAM_FORMAT: String = "FORMAT"
    const val FORMAT_JSON: String = "JSON"

    /**
     * The `EPHEMERIS_TYPE` value SGP4/SGP8 is carried under. A record whose type is any other value
     * is **not** propagated silently: [OmmElementSet.isSgp4] states it, and the engine adapter
     * refuses rather than mis-propagating a set the model does not describe.
     */
    const val EPHEMERIS_TYPE_SGP4: Int = 0

    /**
     * The `User-Agent` of one release — a single fixed string per release, sent to every request.
     *
     * It is **not** re-derived here: [CloudProtocol.userAgent] is the one definition of the app's
     * network identity, and both services receive the identical string.
     */
    fun userAgent(versionName: String): String = CloudProtocol.userAgent(versionName)

    /**
     * The exact request for [group]. The `GROUP` value is placed in the query un-encoded because
     * `CATNR=<noradId>` is the source's own documented form and its `=` is part of the value.
     */
    fun requestUrl(group: SatelliteGroup): String =
        "$BASE_URL?$PARAM_GROUP=${group.wireValue}&$PARAM_FORMAT=$FORMAT_JSON"

    /**
     * Parses a CelesTrak `gp.php?...&FORMAT=JSON` body: a **top-level JSON array** of OMM records.
     *
     * **What is required and what may be absent.** The parser refuses the whole body (rather than
     * half-reading it) when a record lacks any field the propagator needs to compute a position:
     * `OBJECT_NAME`, `EPOCH`, `NORAD_CAT_ID`, `MEAN_MOTION`, `ECCENTRICITY`, `INCLINATION`,
     * `RA_OF_ASC_NODE`, `ARG_OF_PERICENTER`, `MEAN_ANOMALY`, `EPHEMERIS_TYPE`, `BSTAR`,
     * `MEAN_MOTION_DOT`, `MEAN_MOTION_DDOT`. The four **purely descriptive** keys —
     * `OBJECT_ID`, `CLASSIFICATION_TYPE`, `ELEMENT_SET_NO`, `REV_AT_EPOCH` — are read when present
     * and carried as `null` when the response omits them; an absence is recorded as an absence and
     * is **not** given a fabricated default. Missing a required key is [ElementsParse.Malformed],
     * because a set with a guessed mean motion or epoch is a fabricated orbit.
     *
     * `EPOCH` routinely arrives **without a `Z`** (`"2026-10-09T04:28:31.895104"`), so it is read
     * with [CloudProtocol.parseIso8601Utc], which treats a bare local date-time as UTC.
     */
    fun parseRecords(body: String): ElementsParse {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull()
            ?: return ElementsParse.Malformed("body is not valid JSON")
        val array = root as? JsonArray
            ?: return ElementsParse.Malformed("root is not a JSON array of OMM records")

        val sets = ArrayList<OmmElementSet>(array.size)
        for (entry in array) {
            val row = entry as? JsonObject
                ?: return ElementsParse.Malformed("an OMM record is not a JSON object")
            val parsed = parseRecord(row)
                ?: return ElementsParse.Malformed("an OMM record is missing a field the propagator needs")
            sets.add(parsed)
        }
        return ElementsParse.Records(sets)
    }

    /** Parses **one** OMM record, for a cached payload. `null` when a required field is absent. */
    fun parseRecord(row: JsonObject): OmmElementSet? {
        val objectName = row.text(KEY_OBJECT_NAME) ?: return null
        val epochText = row.text(KEY_EPOCH) ?: return null
        val epoch = CloudProtocol.parseIso8601Utc(epochText) ?: return null
        val noradId = row.integral(KEY_NORAD_CAT_ID) ?: return null
        val meanMotion = row.number(KEY_MEAN_MOTION) ?: return null
        val eccentricity = row.number(KEY_ECCENTRICITY) ?: return null
        val inclination = row.number(KEY_INCLINATION) ?: return null
        val raan = row.number(KEY_RA_OF_ASC_NODE) ?: return null
        val argPericenter = row.number(KEY_ARG_OF_PERICENTER) ?: return null
        val meanAnomaly = row.number(KEY_MEAN_ANOMALY) ?: return null
        val ephemerisType = row.integral(KEY_EPHEMERIS_TYPE) ?: return null
        val bstar = row.number(KEY_BSTAR) ?: return null
        val meanMotionDot = row.number(KEY_MEAN_MOTION_DOT) ?: return null
        val meanMotionDotDot = row.number(KEY_MEAN_MOTION_DDOT) ?: return null

        if (!eccentricity.isFinite() || eccentricity < 0.0 || eccentricity >= 1.0) return null
        if (!meanMotion.isFinite() || meanMotion <= 0.0) return null
        if (!inclination.isFinite() || Math.abs(inclination) > 180.0) return null

        return OmmElementSet(
            noradId = noradId,
            objectName = objectName,
            objectId = row.text(KEY_OBJECT_ID),
            classificationType = row.text(KEY_CLASSIFICATION_TYPE),
            epochUtc = epoch,
            elementSetNo = row.integral(KEY_ELEMENT_SET_NO)?.toInt(),
            revAtEpoch = row.integral(KEY_REV_AT_EPOCH),
            ephemerisType = ephemerisType.toInt(),
            meanMotionRevPerDay = meanMotion,
            eccentricity = eccentricity,
            inclinationDeg = inclination,
            raOfAscNodeDeg = raan,
            argOfPericenterDeg = argPericenter,
            meanAnomalyDeg = meanAnomaly,
            bstar = bstar,
            meanMotionDotRevPerDay2 = meanMotionDot,
            meanMotionDotDotRevPerDay3 = meanMotionDotDot,
            payload = row.toString(),
        )
    }

    const val KEY_OBJECT_NAME: String = "OBJECT_NAME"
    const val KEY_OBJECT_ID: String = "OBJECT_ID"
    const val KEY_EPOCH: String = "EPOCH"
    const val KEY_MEAN_MOTION: String = "MEAN_MOTION"
    const val KEY_ECCENTRICITY: String = "ECCENTRICITY"
    const val KEY_INCLINATION: String = "INCLINATION"
    const val KEY_RA_OF_ASC_NODE: String = "RA_OF_ASC_NODE"
    const val KEY_ARG_OF_PERICENTER: String = "ARG_OF_PERICENTER"
    const val KEY_MEAN_ANOMALY: String = "MEAN_ANOMALY"
    const val KEY_EPHEMERIS_TYPE: String = "EPHEMERIS_TYPE"
    const val KEY_CLASSIFICATION_TYPE: String = "CLASSIFICATION_TYPE"
    const val KEY_NORAD_CAT_ID: String = "NORAD_CAT_ID"
    const val KEY_ELEMENT_SET_NO: String = "ELEMENT_SET_NO"
    const val KEY_REV_AT_EPOCH: String = "REV_AT_EPOCH"
    const val KEY_BSTAR: String = "BSTAR"
    const val KEY_MEAN_MOTION_DOT: String = "MEAN_MOTION_DOT"
    const val KEY_MEAN_MOTION_DDOT: String = "MEAN_MOTION_DDOT"

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

    /** A catalogue number, which the source sends as a number and this reader also accepts as text. */
    private fun JsonObject.integral(key: String): Long? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.longOrNull ?: primitive.content.toLongOrNull()
    }

    private val json = Json { ignoreUnknownKeys = true }
}

/** The outcome of parsing an OMM body: the records, or the reason the body was refused. */
sealed interface ElementsParse {
    data class Records(val sets: List<OmmElementSet>) : ElementsParse

    /** A technical diagnostic for logs; the screens own the user-facing sentence. */
    data class Malformed(val reason: String) : ElementsParse
}

/**
 * One OMM element set, with every field the propagator needs and the byte range it came from.
 *
 * [payload] is the record's **own JSON object**, verbatim, so a downstream parser (Orekit's
 * `OmmParser` → `Omm.generateTLE()`) consumes the same bytes CelesTrak sent rather than a
 * re-serialisation this app invented.
 *
 * [ageAt] is the only place the age bands are applied: `sessionInstantUtc − EPOCH`, both UTC, with
 * the session instant captured once by the UI and never re-read from the clock — a set's age must
 * not twitch under a user who is reading a pass time.
 */
data class OmmElementSet(
    val noradId: Long,
    val objectName: String,
    /** `OBJECT_ID` (`"1998-067A"`), absent for some newly catalogued objects. */
    val objectId: String?,
    val classificationType: String?,
    val epochUtc: Instant,
    val elementSetNo: Int?,
    val revAtEpoch: Long?,
    /** `0` is SGP4/SGP8; any other value is a model this app does not propagate. */
    val ephemerisType: Int,
    val meanMotionRevPerDay: Double,
    val eccentricity: Double,
    val inclinationDeg: Double,
    val raOfAscNodeDeg: Double,
    val argOfPericenterDeg: Double,
    val meanAnomalyDeg: Double,
    val bstar: Double,
    val meanMotionDotRevPerDay2: Double,
    val meanMotionDotDotRevPerDay3: Double,
    val payload: String,
) {
    /** True when the record describes the SGP4 model the propagator implements. */
    val isSgp4: Boolean get() = ephemerisType == ElementsProtocol.EPHEMERIS_TYPE_SGP4

    /** The element set's age against the session instant, in the domain's own bands. */
    fun ageAt(sessionInstantUtc: Instant): ElementSetAge = ElementSetAge.from(epochUtc, sessionInstantUtc)

    /**
     * True when `EPOCH` is later than the session instant by more than [tolerance]. A future set is a
     * wrong device clock rather than a usable orbit, and no pass is computed from it.
     */
    fun isEpochInFuture(sessionInstantUtc: Instant, tolerance: Duration = FUTURE_EPOCH_TOLERANCE): Boolean =
        epochUtc.isAfter(sessionInstantUtc.plus(tolerance))

    /** The propagator's field set: radians, radians per second, and the wrapped angles. */
    fun toPropagatorParams(): OmmPropagatorParams = OmmPropagatorParams(
        noradId = noradId,
        epochUtc = epochUtc,
        eccentricity = eccentricity,
        bstar = bstar,
        inclinationRad = OmmUnits.toRadians(inclinationDeg),
        raOfAscNodeRad = OmmUnits.wrapToTwoPi(OmmUnits.toRadians(raOfAscNodeDeg)),
        argOfPericenterRad = OmmUnits.wrapToTwoPi(OmmUnits.toRadians(argOfPericenterDeg)),
        meanAnomalyRad = OmmUnits.wrapToTwoPi(OmmUnits.toRadians(meanAnomalyDeg)),
        meanMotionRadPerSec = OmmUnits.meanMotionToRadPerSecond(meanMotionRevPerDay),
        meanMotionDotRadPerSec2 = OmmUnits.meanMotionDotToRadPerSecond2(meanMotionDotRevPerDay2),
        meanMotionDotDotRadPerSec3 = OmmUnits.meanMotionDotDotToRadPerSecond3(meanMotionDotDotRevPerDay3),
    )

    companion object {
        /** The clock-skew tolerance a future `EPOCH` is allowed before the set is refused. */
        val FUTURE_EPOCH_TOLERANCE: Duration = Duration.ofMinutes(10)
    }
}

/**
 * An element set with its units converted to what the propagator takes.
 *
 * This is the **fallback contract** for a record Orekit's own `OmmParser` refuses, and the invariant
 * the unit test asserts either way.
 */
data class OmmPropagatorParams(
    val noradId: Long,
    val epochUtc: Instant,
    val eccentricity: Double,
    val bstar: Double,
    val inclinationRad: Double,
    val raOfAscNodeRad: Double,
    val argOfPericenterRad: Double,
    val meanAnomalyRad: Double,
    val meanMotionRadPerSec: Double,
    val meanMotionDotRadPerSec2: Double,
    val meanMotionDotDotRadPerSec3: Double,
)

/**
 * The OMM → propagator unit conversions, exactly as the spec states them.
 *
 * `meanMotion` is **rad/s, not rev/day**: passing `15.48769982` straight through yields a satellite
 * that falls out of the sky. The three angles are **wrapped**, never clamped — clamping a negative
 * `MEAN_ANOMALY` to zero moves the satellite rather than normalising it.
 */
object OmmUnits {

    /** Seconds in one day, the denominator of every rev/day conversion. */
    const val SECONDS_PER_DAY: Double = 86_400.0

    private const val TWO_PI: Double = 2.0 * Math.PI

    fun toRadians(degrees: Double): Double = degrees * Math.PI / 180.0

    /** Adds or subtracts 2π until the angle lies in `[0, 2π)`. */
    fun wrapToTwoPi(radians: Double): Double {
        if (!radians.isFinite()) return 0.0
        val m = radians % TWO_PI
        return if (m < 0.0) m + TWO_PI else m
    }

    /** `MEAN_MOTION × 2π / 86400` — revolutions per day to radians per second. */
    fun meanMotionToRadPerSecond(revPerDay: Double): Double = revPerDay * TWO_PI / SECONDS_PER_DAY

    /** `MEAN_MOTION_DOT × 2π / 86400²` — revolutions per day² to radians per second². */
    fun meanMotionDotToRadPerSecond2(revPerDay2: Double): Double =
        revPerDay2 * TWO_PI / (SECONDS_PER_DAY * SECONDS_PER_DAY)

    /** `MEAN_MOTION_DDOT × 2π / 86400³` — revolutions per day³ to radians per second³. */
    fun meanMotionDotDotToRadPerSecond3(revPerDay3: Double): Double =
        revPerDay3 * TWO_PI / (SECONDS_PER_DAY * SECONDS_PER_DAY * SECONDS_PER_DAY)
}

/**
 * The gate that turns **every** element fetch off.
 *
 * It exists because the source's commercial licence is an open item: if the terms are not confirmed
 * for a paid app, the capability ships disabled and states that satellite passes are unavailable,
 * rather than shipping a feed whose terms are unresolved. It is an interface rather than a read of
 * the settings store because the preference is owned by `settings-and-credits`.
 */
fun interface ElementsFetchGate {
    suspend fun isEnabled(): Boolean

    companion object {
        /** The default when nothing is wired: the capability is on. */
        val ALWAYS_ON: ElementsFetchGate = ElementsFetchGate { true }
    }
}

/**
 * Every way the element data can fail to be available, as a **typed state** rather than a sentence.
 *
 * The screens map each value to its own resource; nothing here produces user-facing text.
 */
enum class ElementFailure {
    /** The capability is switched off: no request is made, and none will be. */
    SOURCE_DISABLED,

    /** No usable network: the host could not be resolved or the route is unreachable. */
    NO_NETWORK,

    /** The host did not answer inside the app's timeout. */
    SERVICE_UNREACHABLE,

    /** A non-200 status other than 429 — including a 301, which is never followed. */
    SERVICE_ERROR,

    /** HTTP 429: the app is throttled, and no request is made again before the next cadence. */
    RATE_LIMITED,

    /** A 200 body the parser cannot read, or a record missing a field the propagator needs. */
    MALFORMED_RESPONSE,

    /** A usable 200 that carries no record for the requested group or object. */
    NO_ELEMENTS_FOR_OBJECT,

    /** Every set for the group is older than 72 h: no pass time of any kind may be computed. */
    EPOCH_EXPIRED,

    /** Every set's `EPOCH` is later than the session instant beyond the tolerance. */
    EPOCH_IN_FUTURE,
}

/**
 * The element data for one group: the sets to compute from, or a typed failure that may still carry
 * the **last cached set** so the section shows an age-labelled prediction instead of nothing.
 */
sealed interface ElementsResult {

    /**
     * The last element set known for the group, whatever the state — for display. A set whose band
     * is `EXPIRED` may be shown with its age and SHALL NOT be used to compute a pass.
     */
    val lastSet: Available?

    /**
     * Element sets that may be computed from: present, not in the future, and inside the age bands
     * (`age ≤ 72 h`). [sets] keeps the order the response carried. Each set carries its own
     * [OmmElementSet.ageAt]; [age] is the **newest** epoch in the group, which is the group's own
     * freshness and the badge the section head shows.
     */
    data class Available(
        val group: SatelliteGroup,
        val sets: List<OmmElementSet>,
        val age: ElementSetAge,
        val fetchedAtUtc: Instant?,
        /** True when the payload came from the cache without a request. */
        val servedFromCache: Boolean,
        /** Records whose `EPOCH` is in the future; counted, never computed from. */
        val setsInFuture: Int = 0,
    ) : ElementsResult {
        override val lastSet: Available get() = this

        /** The freshest set in the group, or `null` when the group carries none. */
        val newest: OmmElementSet? get() = sets.minByOrNull { it.epochUtc }

        fun setFor(noradId: Long): OmmElementSet? = sets.firstOrNull { it.noradId == noradId }
    }

    /**
     * The element data is unavailable. [lastSet] is the cached set when one exists — the UI then
     * renders the passes from it with its age shown, exactly as an offline cache hit — and `null`
     * when the section has nothing to show and states that it needs one connection.
     */
    data class Unavailable(
        val state: ElementFailure,
        override val lastSet: Available? = null,
    ) : ElementsResult
}

/**
 * The CelesTrak GP client: OMM/JSON, keyless, one download per group, at most once per 2 hours.
 *
 * **Fail-closed, and a single attempt is the rule.** The fetch loop contains exactly one request:
 * on **any** non-200 (301, 403, 404, 429, any 50x) the attempt ends, the status is logged, and the
 * request is **not** retried — not in this call, and not in the next one until the 2-hour cadence has
 * elapsed. CelesTrak's own policy warns that repeatedly ignoring non-200 responses sends a client's
 * address to a firewall.
 *
 * **No background retrieval.** There is no scheduler, no `WorkManager` job, no alarm and no timer in
 * this file: the only way a request happens is a call to [elementsFor] from a screen the user is
 * looking at. Nothing is fetched on the hour because nothing is scheduled at all. The reminder and
 * the widget read [cachedGroup], which never touches the network.
 *
 * **Cache.** One file per group records the fetch stamp
 * (`{group, fetchedAtUtc, lastAttemptUtc, noradIds, rawResponse}`), and one file per element set
 * records exactly `{noradId, group, epochUtc, fetchedAtUtc, payload}`. Both live under the app's own
 * cache directory, which Android never Auto-Backs-Up — which is the required behaviour, because an
 * element set is regenerable and a restored one must never be presented as fresh.
 *
 * **Two clocks, deliberately.** `lastAttemptUtc` advances on **every** attempt so the 2-hour
 * politeness gap holds after a failure; `fetchedAtUtc` advances only on a **successful** store, so a
 * malformed or failed retrieval is never recorded as though a fresh set had been cached.
 */
class ElementsRepository(
    private val context: Context,
    private val gate: ElementsFetchGate = ElementsFetchGate.ALWAYS_ON,
    private val nowProvider: () -> Instant = { Instant.now() },
) {

    /**
     * The element sets for [group], resolved against the session's now.
     *
     * The single-argument shape is the frozen data-layer contract. A screen that owns the session
     * instant passes it explicitly — that is the caller the spec's "captured once, never re-read"
     * rule describes — and the no-argument call falls back to the injected clock.
     */
    suspend fun elementsFor(
        group: SatelliteGroup,
        sessionInstantUtc: Instant = nowProvider(),
    ): ElementsResult {
        if (!gate.isEnabled()) {
            return ElementsResult.Unavailable(ElementFailure.SOURCE_DISABLED, cachedGroup(group, sessionInstantUtc).lastSet)
        }
        // One request in flight, and at most one per group per cadence: serialising here is what
        // makes that true even if two screens ask for the same group at once.
        return fetchMutex.withLock {
            val cached = readSnapshot(group)
            val cachedResult = classify(group, cached, sessionInstantUtc, servedFromCache = true)
            val lastAttempt = cached.lastAttemptUtc
            if (lastAttempt != null && Duration.between(lastAttempt, sessionInstantUtc) < CADENCE) {
                // Inside the source's own update cadence: serve what is cached and make no request.
                return@withLock cachedResult
            }

            val response = try {
                execute(ElementsProtocol.requestUrl(group))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unknownHost: UnknownHostException) {
                recordAttempt(group, cached, sessionInstantUtc)
                return@withLock ElementsResult.Unavailable(ElementFailure.NO_NETWORK, cachedResult.lastSet)
            } catch (timeout: SocketTimeoutException) {
                recordAttempt(group, cached, sessionInstantUtc)
                return@withLock ElementsResult.Unavailable(ElementFailure.SERVICE_UNREACHABLE, cachedResult.lastSet)
            } catch (io: IOException) {
                recordAttempt(group, cached, sessionInstantUtc)
                Log.w(LOG_TAG, "element fetch failed for ${group.cacheSlug}: ${io.javaClass.simpleName}")
                return@withLock ElementsResult.Unavailable(ElementFailure.SERVICE_UNREACHABLE, cachedResult.lastSet)
            }

            if (response.status != HTTP_OK) {
                // 301, 403, 404, 429 and every 50x land here, and the loop stops: no retry, and the
                // cadence clock advances so the next open does not try again either.
                recordAttempt(group, cached, sessionInstantUtc)
                Log.w(LOG_TAG, "element fetch for ${group.cacheSlug} answered HTTP ${response.status}; not retrying")
                val state = if (response.status == HTTP_TOO_MANY_REQUESTS) ElementFailure.RATE_LIMITED else ElementFailure.SERVICE_ERROR
                return@withLock ElementsResult.Unavailable(state, cachedResult.lastSet)
            }

            val body = response.body
            if (body == null) {
                recordAttempt(group, cached, sessionInstantUtc)
                return@withLock ElementsResult.Unavailable(ElementFailure.SERVICE_ERROR, cachedResult.lastSet)
            }
            val parsed = ElementsProtocol.parseRecords(body)
            if (parsed !is ElementsParse.Records) {
                // A malformed body is never cached in place of a valid set, and the group's
                // `fetchedAtUtc` is not advanced as though a fresh set had been stored.
                recordAttempt(group, cached, sessionInstantUtc)
                Log.w(LOG_TAG, "element body for ${group.cacheSlug} could not be read: ${(parsed as ElementsParse.Malformed).reason}")
                return@withLock ElementsResult.Unavailable(ElementFailure.MALFORMED_RESPONSE, cachedResult.lastSet)
            }

            store(group, parsed.sets, body, sessionInstantUtc)
            val stored = readSnapshot(group)
            val fetched = classify(group, stored, sessionInstantUtc, servedFromCache = false)
            if (fetched is ElementsResult.Available) return@withLock fetched
            return@withLock ElementsResult.Unavailable(
                state = (fetched as ElementsResult.Unavailable).state,
                lastSet = fetched.lastSet ?: cachedResult.lastSet,
            )
        }
    }

    /**
     * The element sets already on disk for [group], with **no network call of any kind** — the only
     * route the widget and the reminder take.
     */
    fun cachedGroup(group: SatelliteGroup, sessionInstantUtc: Instant = nowProvider()): ElementsResult =
        classify(group, readSnapshot(group), sessionInstantUtc, servedFromCache = true)

    // -----------------------------------------------------------------------------------------
    // Evaluation against the age bands
    // -----------------------------------------------------------------------------------------

    /**
     * Splits a group's cached sets into the three states the spec defines: computable
     * (`age ≤ 72 h`), expired (`age > 72 h`), and future-epoch (a wrong device clock).
     *
     * The order matters. A usable set wins outright. With none, an expired set is reported as
     * [ElementFailure.EPOCH_EXPIRED] carrying the set for display — never as an empty pass list —
     * and a group whose every set is in the future is [ElementFailure.EPOCH_IN_FUTURE].
     */
    private fun classify(
        group: SatelliteGroup,
        snapshot: GroupSnapshot,
        sessionInstantUtc: Instant,
        servedFromCache: Boolean,
    ): ElementsResult {
        if (snapshot.sets.isEmpty()) {
            return ElementsResult.Unavailable(ElementFailure.NO_ELEMENTS_FOR_OBJECT)
        }
        val future = snapshot.sets.filter { it.isEpochInFuture(sessionInstantUtc) }
        val present = snapshot.sets.filterNot { it.isEpochInFuture(sessionInstantUtc) }
        val live = present.filter { it.ageAt(sessionInstantUtc).band != ElementSetAgeBand.EXPIRED }

        if (live.isNotEmpty()) {
            return ElementsResult.Available(
                group = group,
                sets = live,
                age = live.maxBy { it.epochUtc }.ageAt(sessionInstantUtc),
                fetchedAtUtc = snapshot.fetchedAtUtc,
                servedFromCache = servedFromCache,
                setsInFuture = future.size,
            )
        }
        if (present.isNotEmpty()) {
            return ElementsResult.Unavailable(
                state = ElementFailure.EPOCH_EXPIRED,
                lastSet = ElementsResult.Available(
                    group = group,
                    sets = present,
                    age = present.maxBy { it.epochUtc }.ageAt(sessionInstantUtc),
                    fetchedAtUtc = snapshot.fetchedAtUtc,
                    servedFromCache = servedFromCache,
                ),
            )
        }
        return ElementsResult.Unavailable(
            state = ElementFailure.EPOCH_IN_FUTURE,
            lastSet = ElementsResult.Available(
                group = group,
                sets = future,
                age = ElementSetAge(Duration.ZERO, ElementSetAgeBand.FRESH),
                fetchedAtUtc = snapshot.fetchedAtUtc,
                servedFromCache = servedFromCache,
                setsInFuture = future.size,
            ),
        )
    }

    // -----------------------------------------------------------------------------------------
    // Cache
    // -----------------------------------------------------------------------------------------

    private class GroupSnapshot(
        val sets: List<OmmElementSet>,
        val fetchedAtUtc: Instant?,
        val lastAttemptUtc: Instant?,
    ) {
        companion object {
            val EMPTY = GroupSnapshot(emptyList(), null, null)
        }
    }

    private fun readSnapshot(group: SatelliteGroup): GroupSnapshot {
        val stamp = readStamp(group)
        val files = cacheDir().listFiles { file -> file.isFile && file.name.startsWith(setFilePrefix(group)) }
            ?: return GroupSnapshot(emptyList(), null, stamp?.lastAttemptUtc?.let(CloudProtocol::parseIso8601Utc))

        val sets = ArrayList<OmmElementSet>(files.size)
        for (file in files) {
            val text = runCatching { file.readText() }.getOrNull() ?: continue
            val dto = runCatching { elementJson.decodeFromString(ElementSetCacheDto.serializer(), text) }.getOrNull() ?: continue
            val object0 = runCatching { elementJson.parseToJsonElement(dto.payload) }.getOrNull() as? JsonObject ?: continue
            val parsed = ElementsProtocol.parseRecord(object0) ?: continue
            sets.add(parsed)
        }
        val fetchedAt = stamp?.fetchedAtUtc?.let(CloudProtocol::parseIso8601Utc)
        return GroupSnapshot(
            sets = sets.sortedBy { it.noradId },
            fetchedAtUtc = fetchedAt,
            lastAttemptUtc = stamp?.lastAttemptUtc?.let(CloudProtocol::parseIso8601Utc),
        )
    }

    private fun readStamp(group: SatelliteGroup): ElementGroupStampDto? {
        val file = stampFile(group)
        if (!file.isFile) return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        return runCatching { elementJson.decodeFromString(ElementGroupStampDto.serializer(), text) }.getOrNull()
    }

    /** Stores the group's sets and then its stamp, so a crash mid-write leaves no stamp at all. */
    private fun store(group: SatelliteGroup, sets: List<OmmElementSet>, body: String, nowUtc: Instant) {
        val fetchedAt = nowUtc.toString()
        for (set in sets) {
            writeAtomically(
                setFile(group, set.noradId),
                elementJson.encodeToString(
                    ElementSetCacheDto.serializer(),
                    ElementSetCacheDto(
                        noradId = set.noradId,
                        group = group.wireValue,
                        // The record's own EPOCH, exactly as the source stated it.
                        epochUtc = set.epochUtc.toString(),
                        fetchedAtUtc = fetchedAt,
                        payload = set.payload,
                    ),
                ),
            )
        }
        // A set the new response no longer carries is dropped from the cache: keeping it would let a
        // fresh-looking orbit for a de-orbited or re-grouped object outlive the response that named it.
        val keep = sets.mapTo(HashSet()) { "set_${group.cacheSlug}_${it.noradId}.json" }
        val directory = cacheDir()
        directory.listFiles { file -> file.isFile && file.name.startsWith(setFilePrefix(group)) }
            ?.forEach { file -> if (file.name !in keep) file.delete() }

        writeStamp(
            group = group,
            fetchedAtUtc = fetchedAt,
            lastAttemptUtc = fetchedAt,
            noradIds = sets.map { it.noradId },
            rawResponse = body,
        )
    }

    /**
     * Records an attempt that produced no usable set: `lastAttemptUtc` advances (so the source is not
     * asked again inside the cadence) while `fetchedAtUtc` and the stored sets are left untouched.
     */
    private fun recordAttempt(group: SatelliteGroup, snapshot: GroupSnapshot, nowUtc: Instant) {
        val previous = readStamp(group)
        writeStamp(
            group = group,
            fetchedAtUtc = snapshot.fetchedAtUtc?.toString() ?: previous?.fetchedAtUtc,
            lastAttemptUtc = nowUtc.toString(),
            noradIds = previous?.noradIds ?: snapshot.sets.map { it.noradId },
            rawResponse = previous?.rawResponse.orEmpty(),
        )
    }

    private fun writeStamp(
        group: SatelliteGroup,
        fetchedAtUtc: String?,
        lastAttemptUtc: String,
        noradIds: List<Long>,
        rawResponse: String,
    ) {
        writeAtomically(
            stampFile(group),
            elementJson.encodeToString(
                ElementGroupStampDto.serializer(),
                ElementGroupStampDto(
                    group = group.wireValue,
                    fetchedAtUtc = fetchedAtUtc,
                    lastAttemptUtc = lastAttemptUtc,
                    noradIds = noradIds,
                    rawResponse = rawResponse,
                ),
            ),
        )
    }

    private fun writeAtomically(file: File, text: String) {
        runCatching {
            file.parentFile?.mkdirs()
            // Write then rename, so a crash mid-write cannot leave a half-record in the cache.
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(text)
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
                temporary.delete()
            }
        }
    }

    /** `cacheDir` is never Auto-Backed-Up, which is exactly why a regenerable element set lives here. */
    private fun cacheDir(): File = File(context.cacheDir, CACHE_DIRECTORY)

    private fun stampFile(group: SatelliteGroup): File = File(cacheDir(), "group_${group.cacheSlug}.json")

    private fun setFile(group: SatelliteGroup, noradId: Long): File =
        File(cacheDir(), "${setFilePrefix(group)}$noradId.json")

    private fun setFilePrefix(group: SatelliteGroup): String = "set_${group.cacheSlug}_"

    // -----------------------------------------------------------------------------------------
    // Transport
    // -----------------------------------------------------------------------------------------

    private class HttpResponse(val status: Int, val body: String?)

    private suspend fun execute(requestUrl: String): HttpResponse = runInterruptible(Dispatchers.IO) {
        val connection = URL(requestUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            // A 3xx is a typed SERVICE_ERROR, not a redirect: the app has exactly one element host.
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", ElementsProtocol.userAgent(BuildConfig.VERSION_NAME))
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            // A non-200 body is never read: nothing is derived from an error page.
            val body = if (status == HTTP_OK) {
                connection.inputStream.use { it.readBytes().decodeToString() }
            } else {
                null
            }
            HttpResponse(status, body)
        } finally {
            connection.disconnect()
        }
    }

    private val fetchMutex = Mutex()

    private val elementJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        const val CACHE_DIRECTORY: String = "elements"

        /**
         * The source's own update cadence: "For GP data, updates are once every 2 hours". A group
         * fetched inside this window is served from the cache with no request at all.
         */
        val CADENCE: Duration = Duration.ofHours(2)

        const val CONNECT_TIMEOUT_MS: Int = 15_000
        const val READ_TIMEOUT_MS: Int = 20_000

        private const val HTTP_OK = 200
        private const val HTTP_TOO_MANY_REQUESTS = 429

        private const val LOG_TAG = "LumintraElements"
    }
}

/**
 * The per-group fetch stamp: when the group was last **successfully** stored, when it was last
 * **attempted**, which catalogue numbers it holds, and the response body verbatim.
 *
 * "Successful" and "attempted" are two fields on purpose — see [ElementsRepository]'s KDoc.
 */
@Serializable
internal class ElementGroupStampDto(
    val group: String,
    val fetchedAtUtc: String? = null,
    val lastAttemptUtc: String,
    val noradIds: List<Long> = emptyList(),
    val rawResponse: String = "",
)

/** One cached element set, in exactly the shape the spec names: `{noradId, group, epochUtc, fetchedAtUtc, payload}`. */
@Serializable
internal class ElementSetCacheDto(
    val noradId: Long,
    val group: String,
    val epochUtc: String,
    val fetchedAtUtc: String,
    val payload: String,
)
