package com.gexetr.lumintralightplanner.data

import android.content.Context
import com.gexetr.lumintralightplanner.BuildConfig
import com.gexetr.lumintralightplanner.domain.CloudHour
import com.gexetr.lumintralightplanner.domain.CloudReport
import com.gexetr.lumintralightplanner.domain.ForecastAge
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.NightSpan
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CancellationException
import kotlin.random.Random
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

/**
 * The forecast endpoint, its request, and the parsing of its response — **all of it pure**, so the
 * three things the spec pins down (the coordinate truncation, the request URL and `User-Agent`, and
 * the timeseries parse) are unit-testable without a device, a network or a coroutine.
 *
 * Four facts are load-bearing and each is a measured requirement rather than a preference:
 *
 * 1. **The coordinates are truncated, not rounded, to at most 4 decimals** before the URL is built:
 *    `api.met.no` answers **403** to a request carrying a fifth decimal. 4 decimals of latitude is
 *    ≈ 11 m, and the cap is a protocol requirement rather than a claim about the spot's accuracy.
 * 2. **The `User-Agent` names the app and a contact address.** The service's terms require an
 *    identifiable client; a fake or random string is a ban, so the header is a fixed string per
 *    release and is never omitted.
 * 3. **The value read is `properties.timeseries[].data.instant.details.cloud_area_fraction`**, a
 *    number 0–100 in per cent, and it is **total cloud**. The product carries no low/mid/high split,
 *    so nothing here produces one.
 * 4. **The altitude parameter is sent only when the spot carries an elevation.** A spot at 0 m (or
 *    one whose elevation was never entered) omits it rather than sending `altitude=0`.
 */
object CloudProtocol {

    const val SCHEME: String = "https"
    const val HOST: String = "api.met.no"

    /** The one and only weather path this app knows. No fallback provider exists. */
    const val PATH: String = "/weatherapi/locationforecast/2.0/compact"

    const val BASE_URL: String = "https://api.met.no/weatherapi/locationforecast/2.0/compact"

    /**
     * The endpoint's own cap. A request with 5 or more decimals is refused with **403**, so the
     * truncation happens here, before the URL exists, and nowhere else.
     */
    const val MAX_COORDINATE_DECIMALS: Int = 4

    /** The application half of the `User-Agent`. */
    const val APP_NAME: String = "Lumintra"

    /**
     * The developer account's **public support address**, published on the store listing. It is
     * deliberately not a secret, and it is not read from `.env`.
     */
    const val CONTACT: String = "v.sobolenko.vs@gmail.com"

    /** The `User-Agent` of one release — a single fixed string, sent to every request. */
    fun userAgent(versionName: String): String = "$APP_NAME/$versionName ($CONTACT)"

    /**
     * The exact request URL for a spot. The coordinate is truncated toward zero to
     * [MAX_COORDINATE_DECIMALS] decimals first ([GeodeticPoint.networkRounded]); a spot with no
     * elevation (0 m) omits `altitude` rather than sending a zero.
     */
    fun requestUrl(point: GeodeticPoint): String {
        val rounded = point.networkRounded()
        val builder = StringBuilder(BASE_URL)
        builder.append("?lat=").append(formatCoordinate(rounded.latDeg))
        builder.append("&lon=").append(formatCoordinate(rounded.lonDeg))
        val elevation = rounded.elevationM
        if (elevation.isFinite() && elevation != 0.0) {
            builder.append("&altitude=").append(Math.round(elevation))
        }
        return builder.toString()
    }

    /**
     * The cache key: the **truncated** coordinates, so two spots inside the same 0.0001° cell are one
     * request and one cache entry.
     */
    fun cacheKey(point: GeodeticPoint): String {
        val rounded = point.networkRounded()
        return "${formatCoordinate(rounded.latDeg)}_${formatCoordinate(rounded.lonDeg)}"
    }

    /**
     * Renders one coordinate with at most [MAX_COORDINATE_DECIMALS] decimals, in the C locale and
     * without a locale-sensitive formatter: a device set to a comma-decimal locale must not turn
     * `lat=59.9138` into `lat=59,9138`, which is a different request and a 400.
     *
     * Trailing zeros are dropped, so a whole number renders as `59` rather than `59.0000` — both are
     * inside the 4-decimal cap, and the shorter form is what the service's own examples use.
     */
    fun formatCoordinate(value: Double): String {
        val truncated = GeodeticPoint.truncateTo4(value)
        val negative = truncated < 0.0 || (truncated == 0.0 && 1.0 / truncated < 0.0)
        val scaled = Math.round(Math.abs(truncated) * 10_000.0)
        val whole = scaled / 10_000L
        val fraction = scaled % 10_000L
        val sign = if (negative && scaled != 0L) "-" else ""
        if (fraction == 0L) return "$sign$whole"
        val fractionText = fraction.toString().padStart(4, '0').trimEnd('0')
        return "$sign$whole.$fractionText"
    }

    /** Parses an ISO-8601 instant from the payload, accepting a `Z`, an offset, or a bare UTC time. */
    fun parseIso8601Utc(text: String): Instant? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { Instant.parse(trimmed) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
                .getOrNull()
            ?: runCatching {
                LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC)
            }.getOrNull()
    }

    /**
     * Parses an HTTP date header (`Expires`, `Last-Modified`, `Date`), accepting the RFC 1123 form the
     * service sends and, defensively, the two older forms. A header that cannot be read is `null`,
     * never a fabricated instant.
     */
    fun parseHttpDate(value: String?): Instant? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return runCatching { Instant.parse(trimmed) }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
                .getOrNull()
            ?: runCatching { OffsetDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
                .getOrNull()
            ?: runCatching {
                LocalDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC)
            }.getOrNull()
    }

    /**
     * The next instant at which a fetch is permitted: the response's own `Expires`, plus a **random
     * offset of a few minutes** so refreshes do not land on the hour.
     *
     * When `Expires` is absent or already in the past, a fallback window is used instead of "now", so
     * a burst of calls cannot become a request storm — the service's terms require conditional,
     * sparing requests, and one request per window is the contract.
     */
    fun nextFetchInstant(expiresUtc: Instant?, nowUtc: Instant, random: Random): Instant {
        val base = expiresUtc?.takeIf { it.isAfter(nowUtc) } ?: nowUtc.plus(FALLBACK_FRESH_WINDOW)
        val jitterSeconds = MIN_JITTER_SECONDS + random.nextLong(0L, JITTER_SPAN_SECONDS)
        return base.plusSeconds(jitterSeconds)
    }

    /** The window used when the server states no usable `Expires`. */
    val FALLBACK_FRESH_WINDOW: Duration = Duration.ofHours(1)

    /** The shortest gap between two permitted requests, so a bad `Expires` cannot cause a storm. */
    const val MIN_JITTER_SECONDS: Long = 60L

    /** The width of the random offset, in seconds (1–6 minutes). */
    const val JITTER_SPAN_SECONDS: Long = 300L

    /**
     * Reads `properties.timeseries[].data.instant.details.cloud_area_fraction` with its companion
     * `.time`, and **only** those two fields.
     *
     * A body that is not valid JSON, that lacks `properties.timeseries`, or that carries an entry
     * without `time`, `data.instant.details` or a numeric `cloud_area_fraction` is a
     * [CloudParse.Malformed] — the whole body is refused rather than half-read, because a partially
     * parsed forecast is a fabricated one. An empty `timeseries` parses to an empty list, which is the
     * honest "no hour in the window" input rather than a malformed body.
     */
    fun parseTimeseries(body: String): CloudParse {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull()
            ?: return CloudParse.Malformed("body is not valid JSON")
        val rootObject = root as? JsonObject
            ?: return CloudParse.Malformed("root is not a JSON object")
        val properties = rootObject["properties"] as? JsonObject
            ?: return CloudParse.Malformed("no `properties` object")
        val timeseries = properties["timeseries"] as? JsonArray
            ?: return CloudParse.Malformed("no `properties.timeseries` array")

        val hours = ArrayList<CloudHour>(timeseries.size)
        for (entry in timeseries) {
            val row = entry as? JsonObject
                ?: return CloudParse.Malformed("a timeseries entry is not an object")
            val timeText = (row["time"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: return CloudParse.Malformed("a timeseries entry has no string `time`")
            val hourUtc = parseIso8601Utc(timeText)
                ?: return CloudParse.Malformed("`time` is not an instant: $timeText")
            val data = row["data"] as? JsonObject
                ?: return CloudParse.Malformed("a timeseries entry has no `data`")
            val instantBlock = data["instant"] as? JsonObject
                ?: return CloudParse.Malformed("a timeseries entry has no `data.instant`")
            val details = instantBlock["details"] as? JsonObject
                ?: return CloudParse.Malformed("a timeseries entry has no `data.instant.details`")
            val cloud = (details["cloud_area_fraction"] as? JsonPrimitive)
                ?.takeIf { !it.isString }
                ?.doubleOrNull
                ?: return CloudParse.Malformed("`cloud_area_fraction` is missing or not numeric")
            if (!cloud.isFinite() || cloud < 0.0 || cloud > 100.0) {
                return CloudParse.Malformed("`cloud_area_fraction` $cloud is outside 0..100")
            }
            hours.add(CloudHour(hourUtc, Math.round(cloud).toInt()))
        }
        return CloudParse.Hours(hours)
    }

    /** The response's model time, when it names one. */
    fun modelTimeFrom(lastModifiedHeader: String?): Instant? = parseHttpDate(lastModifiedHeader)

    private val json = Json { ignoreUnknownKeys = true }
}

/** The outcome of parsing one forecast body: an hour list, or the reason the body was refused. */
sealed interface CloudParse {
    data class Hours(val hours: List<CloudHour>) : CloudParse

    /** A technical diagnostic for logs; the screens own the user-facing sentence. */
    data class Malformed(val reason: String) : CloudParse
}

/**
 * The gate that turns **every** forecast fetch off when the user's *Cloud forecast* preference is off.
 *
 * It is an interface rather than a read of `SettingsStore` because the settings contract is owned by
 * the `settings-and-credits` capability: the preference is added to that store together with the code
 * that reads it, and the wiring passes it in here. With the gate closed, [CloudRepository.fetch]
 * returns [CloudFailure.SOURCE_DISABLED] and issues no request at all.
 */
fun interface CloudFetchGate {
    suspend fun isEnabled(): Boolean

    companion object {
        /** The default when no preference is wired: the forecast is on, which is the store's default. */
        val ALWAYS_ON: CloudFetchGate = CloudFetchGate { true }
    }
}

/**
 * Every way the forecast can fail to be available, as a **typed state** rather than a sentence.
 *
 * The screens map each value to its own resource; nothing here produces user-facing text. Two of
 * them share one wording deliberately: the app cannot distinguish a coordinate outside the service's
 * coverage from a window the feed does not reach, so it records which one it believes and shows the
 * single "the forecast does not cover this window" sentence for both.
 */
enum class CloudFailure {
    /** The user turned the Cloud forecast preference off: no request was made, and none will be. */
    SOURCE_DISABLED,

    /** No usable network: the host could not be resolved or the route is unreachable. */
    NO_NETWORK,

    /** The host did not answer inside the app's timeout. */
    SERVICE_UNREACHABLE,

    /** A response status other than 200, 203, 304 or 429 — including a 3xx, which is never followed. */
    SERVICE_ERROR,

    /** HTTP 429: the app is throttled, and no further request is made before the next window. */
    RATE_LIMITED,

    /** A usable 200 whose hours contain none inside the chosen window. */
    NO_HOURS_IN_WINDOW,

    /**
     * The same observation, believed to be a coordinate the service does not forecast. The app
     * invents no coverage boundary to test against; the screens show the NO_HOURS_IN_WINDOW wording
     * and record this reason.
     */
    OUT_OF_COVERAGE,

    /** The body could not be read as a forecast: not JSON, or missing a field the parser reads. */
    MALFORMED_RESPONSE,
}

/**
 * The forecast for one spot: a readable report with its age, or a typed failure that may still carry
 * the **last good reading** so the user sees age-labelled data instead of nothing.
 */
sealed interface CloudResult {

    /**
     * A usable forecast. [servedFromCache] is true when the payload came from the cache without a
     * request — the value is then labelled with its age and the word *forecast*, never presented as
     * current conditions.
     */
    data class Available(
        val report: CloudReport,
        /** The service's model time (`Last-Modified`), or `null` when it stated none. */
        val modelTimeUtc: Instant?,
        val servedFromCache: Boolean,
        /** True when the endpoint answered `203`, which means the product version is deprecated. */
        val deprecatedProduct: Boolean,
        /** The response's `Date` header, for the device-clock-skew notice. */
        val serverDateUtc: Instant? = null,
    ) : CloudResult {
        /**
         * How far the device clock is from the server's `Date`, in whole minutes, or `null` when the
         * server stated no date. The brief turns a value past its own tolerance into one notice; this
         * carries the measurement only.
         */
        fun clockSkew(nowUtc: Instant): Duration? =
            serverDateUtc?.let { Duration.between(nowUtc, it) }

        /** True when the reading is old enough to be shown as stale. */
        val isStale: Boolean get() = report.age.isStale
    }

    /**
     * The forecast is unavailable. [lastReading] is the cached report when one exists **and** covers
     * the requested window; the UI then shows the `OfflineReadingFrom` wording with that age. When it
     * is `null` there is nothing to show and the `NoForecast` wording applies.
     */
    data class Unavailable(
        val state: CloudFailure,
        val lastReading: CloudReport? = null,
    ) : CloudResult

    companion object {
        /**
         * The state a usable 200 with no hour in the window resolves to. [outOfCoverage] records the
         * reason the app believes; the wording is the same for both, because the app cannot tell.
         */
        fun noHoursInWindow(outOfCoverage: Boolean = false): Unavailable = Unavailable(
            state = if (outOfCoverage) CloudFailure.OUT_OF_COVERAGE else CloudFailure.NO_HOURS_IN_WINDOW,
            lastReading = null,
        )
    }
}

/**
 * The `api.met.no` Locationforecast 2.0 **compact** client.
 *
 * It is the app's only network weather client: there is no fallback provider, no bundled weather
 * asset and no second endpoint, because the money plan fixes this source and a second one would be a
 * different decision rather than a variation of this one.
 *
 * **A single attempt is the rule.** There is no retry loop anywhere in this file: a failure is typed
 * and returned, and the next attempt cannot happen before the next permitted refresh instant. There
 * is also no `HEAD` probe — one `GET` resolves a spot's forecast or it fails.
 *
 * **It never fetches in the background.** This class has no scheduler, no `WorkManager` and no
 * receiver: the only way a request happens is a call to [fetch] from a screen the user is looking at,
 * for the spot in view. The widget and the reminder read the cache and never call this.
 *
 * **Cache.** One entry per **truncated** coordinate pair, holding
 * `{lat4, lon4, fetchedAtUtc, expiresUtc, lastModified, etag, payload}`, written under the app's own
 * cache directory — which Android never Auto-Backs-Up, so a restore can never present a stale forecast
 * as fresh. A fresh entry is served with no request at all; an expired one is refreshed with
 * `If-Modified-Since`, and a `304` keeps the payload and only refreshes the freshness fields.
 *
 * **HTTP.** `java.net.HttpURLConnection` on [Dispatchers.IO], cancellable, HTTPS only, redirects not
 * followed (a 3xx is a typed `SERVICE_ERROR`), no OkHttp/Retrofit/Ktor, and no key of any kind: the
 * endpoint is keyless and this file carries no credential.
 */
class CloudRepository(
    private val context: Context,
    private val gate: CloudFetchGate = CloudFetchGate.ALWAYS_ON,
    private val random: Random = Random.Default,
) {

    /** Whether the user's cloud-forecast preference allows a network fetch. */
    suspend fun isEnabled(): Boolean = gate.isEnabled()

    /**
     * The forecast for [point] resolved against the next 24 hours from [nowUtc].
     *
     * The two-argument shape is the frozen data-layer contract. A caller that knows the session's
     * window should use the three-argument overload, which is what decides
     * [CloudFailure.NO_HOURS_IN_WINDOW] against the window the user is actually planning.
     */
    suspend fun fetch(point: GeodeticPoint, nowUtc: Instant): CloudResult =
        fetch(point, NightSpan(nowUtc, nowUtc.plus(Duration.ofHours(24))), nowUtc)

    /**
     * The forecast for [point], judged against [window] (the session window, whose dark hours the cloud
     * row is aligned to) at [nowUtc].
     *
     * Nothing is fetched when [gate] is closed, or when a cache entry is still inside its window.
     */
    suspend fun fetch(point: GeodeticPoint, window: NightSpan, nowUtc: Instant): CloudResult {
        if (!gate.isEnabled()) {
            return CloudResult.Unavailable(CloudFailure.SOURCE_DISABLED)
        }
        val key = CloudProtocol.cacheKey(point)
        // One request per spot per window: serialising here is what makes that true under two callers.
        return fetchMutex.withLock {
            val cached = readCache(key)
            val cachedReading = cached?.let { usableReport(it, window, nowUtc) }

            if (cached != null && cached.expiresAt.isAfter(nowUtc)) {
                // A fresh entry produces no request at all.
                return@withLock cachedReading
                    ?.let { report ->
                        CloudResult.Available(
                            report = report,
                            modelTimeUtc = cached.modelTimeUtc,
                            servedFromCache = true,
                            deprecatedProduct = false,
                        )
                    }
                    ?: CloudResult.noHoursInWindow()
            }

            val url = CloudProtocol.requestUrl(point)
            val response = try {
                execute(url, ifModifiedSince = cached?.lastModified)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unknownHost: UnknownHostException) {
                return@withLock CloudResult.Unavailable(CloudFailure.NO_NETWORK, cachedReading)
            } catch (timeout: SocketTimeoutException) {
                return@withLock CloudResult.Unavailable(CloudFailure.SERVICE_UNREACHABLE, cachedReading)
            } catch (io: IOException) {
                return@withLock CloudResult.Unavailable(classifyIoFailure(io), cachedReading)
            }

            when (response.status) {
                HTTP_NOT_MODIFIED -> {
                    // The payload is unchanged; keep it and refresh only the freshness fields. The
                    // fetch age is deliberately NOT reset: the data is not newer than it was.
                    val previous = cached
                    if (previous == null) {
                        // A 304 with nothing cached has no payload to keep, and none is invented.
                        return@withLock CloudResult.Unavailable(CloudFailure.SERVICE_ERROR)
                    }
                    val refreshed = previous.copy(
                        expiresAt = CloudProtocol.nextFetchInstant(
                            expiresUtc = CloudProtocol.parseHttpDate(response.expires),
                            nowUtc = nowUtc,
                            random = random,
                        ),
                        lastModified = response.lastModified ?: previous.lastModified,
                        etag = response.etag ?: previous.etag,
                    )
                    writeCache(refreshed)
                    val report = usableReport(refreshed, window, nowUtc)
                    return@withLock report
                        ?.let {
                            CloudResult.Available(
                                report = it,
                                modelTimeUtc = refreshed.modelTimeUtc,
                                servedFromCache = true,
                                deprecatedProduct = false,
                            )
                        }
                        ?: CloudResult.noHoursInWindow()
                }

                HTTP_OK, HTTP_DEPRECATED_PRODUCT -> {
                    val body = response.body
                    if (body == null) {
                        return@withLock CloudResult.Unavailable(CloudFailure.SERVICE_ERROR, cachedReading)
                    }
                    val parsed = CloudProtocol.parseTimeseries(body)
                    if (parsed is CloudParse.Malformed) {
                        // A body the parser cannot read is never cached in place of a valid set.
                        return@withLock CloudResult.Unavailable(CloudFailure.MALFORMED_RESPONSE, cachedReading)
                    }
                    val hours = (parsed as CloudParse.Hours).hours
                    val fetchedAt = nowUtc
                    val expires = CloudProtocol.nextFetchInstant(
                        expiresUtc = CloudProtocol.parseHttpDate(response.expires),
                        nowUtc = nowUtc,
                        random = random,
                    )
                    val entry = CloudCacheEntry(
                        lat4 = point.networkRounded().latDeg,
                        lon4 = point.networkRounded().lonDeg,
                        fetchedAtUtc = fetchedAt,
                        expiresAt = expires,
                        lastModified = response.lastModified,
                        etag = response.etag,
                        modelTimeUtc = CloudProtocol.modelTimeFrom(response.lastModified),
                        payload = body,
                    )
                    writeCache(entry)
                    val report = CloudReport(hours, ForecastAge.from(fetchedAt, nowUtc))
                    val inWindow = report.hoursWithin(window)
                    if (inWindow.isEmpty()) {
                        return@withLock CloudResult.noHoursInWindow()
                    }
                    return@withLock CloudResult.Available(
                        report = report,
                        modelTimeUtc = entry.modelTimeUtc,
                        servedFromCache = false,
                        deprecatedProduct = response.status == HTTP_DEPRECATED_PRODUCT,
                        serverDateUtc = CloudProtocol.parseHttpDate(response.dateHeader),
                    )
                }

                HTTP_TOO_MANY_REQUESTS -> {
                    return@withLock CloudResult.Unavailable(CloudFailure.RATE_LIMITED, cachedReading)
                }

                else -> {
                    return@withLock CloudResult.Unavailable(CloudFailure.SERVICE_ERROR, cachedReading)
                }
            }
        }
    }

    /** The last cached reading for a spot without any network call — the widget's only route. */
    fun cachedReading(point: GeodeticPoint, window: NightSpan, nowUtc: Instant): CloudReport? =
        readCache(CloudProtocol.cacheKey(point))?.let { usableReport(it, window, nowUtc) }

    // -----------------------------------------------------------------------------------------
    // Cache
    // -----------------------------------------------------------------------------------------

    private fun usableReport(entry: CloudCacheEntry, window: NightSpan, nowUtc: Instant): CloudReport? {
        val parsed = CloudProtocol.parseTimeseries(entry.payload)
        if (parsed !is CloudParse.Hours) return null
        val report = CloudReport(parsed.hours, ForecastAge.from(entry.fetchedAtUtc, nowUtc))
        return if (report.hoursWithin(window).isEmpty()) null else report
    }

    private fun readCache(key: String): CloudCacheEntry? {
        val file = cacheFile(key)
        if (!file.isFile) return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        val dto = runCatching { cacheJson.decodeFromString(ForecastCacheDto.serializer(), text) }.getOrNull()
            ?: return null
        val fetchedAt = CloudProtocol.parseIso8601Utc(dto.fetchedAtUtc) ?: return null
        val expiresAt = CloudProtocol.parseIso8601Utc(dto.expiresUtc) ?: return null
        return CloudCacheEntry(
            lat4 = dto.lat4,
            lon4 = dto.lon4,
            fetchedAtUtc = fetchedAt,
            expiresAt = expiresAt,
            lastModified = dto.lastModified,
            etag = dto.etag,
            modelTimeUtc = dto.modelTimeUtc?.let { CloudProtocol.parseIso8601Utc(it) },
            payload = dto.payload,
        )
    }

    private fun writeCache(entry: CloudCacheEntry) {
        val file = cacheFile(CloudProtocol.formatCoordinate(entry.lat4) + "_" + CloudProtocol.formatCoordinate(entry.lon4))
        runCatching {
            file.parentFile?.mkdirs()
            val dto = ForecastCacheDto(
                lat4 = entry.lat4,
                lon4 = entry.lon4,
                fetchedAtUtc = entry.fetchedAtUtc.toString(),
                expiresUtc = entry.expiresAt.toString(),
                lastModified = entry.lastModified,
                etag = entry.etag,
                modelTimeUtc = entry.modelTimeUtc?.toString(),
                payload = entry.payload,
            )
            // Write then rename, so a crash mid-write cannot leave a half-record in the cache.
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(cacheJson.encodeToString(ForecastCacheDto.serializer(), dto))
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
                temporary.delete()
            }
        }
    }

    /** `cacheDir` is never Auto-Backed-Up, which is exactly why the regenerable forecast lives here. */
    private fun cacheFile(key: String): File = File(File(context.cacheDir, CACHE_DIRECTORY), "$key.json")

    // -----------------------------------------------------------------------------------------
    // Transport
    // -----------------------------------------------------------------------------------------

    private suspend fun execute(requestUrl: String, ifModifiedSince: String?): HttpResponse =
        runInterruptible(Dispatchers.IO) {
            val connection = URL(requestUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                // A 3xx is a typed SERVICE_ERROR, not a redirect: the app has exactly one weather host.
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.setRequestProperty("User-Agent", CloudProtocol.userAgent(versionName()))
                connection.setRequestProperty("Accept", "application/json")
                ifModifiedSince?.let { connection.setRequestProperty("If-Modified-Since", it) }
                val status = connection.responseCode
                val body = when (status) {
                    HTTP_OK, HTTP_DEPRECATED_PRODUCT -> connection.inputStream.use { it.readBytes().decodeToString() }
                    else -> null
                }
                HttpResponse(
                    status = status,
                    body = body,
                    lastModified = connection.getHeaderField("Last-Modified"),
                    expires = connection.getHeaderField("Expires"),
                    etag = connection.getHeaderField("ETag"),
                    dateHeader = connection.getHeaderField("Date"),
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun versionName(): String = BuildConfig.VERSION_NAME

    /**
     * A failed connection with no usable route reads as [CloudFailure.NO_NETWORK]; anything else that
     * threw while connecting reads as [CloudFailure.SERVICE_UNREACHABLE].
     *
     * The app deliberately does **not** consult `ConnectivityManager`: that would require
     * `ACCESS_NETWORK_STATE`, which the design's permission list does not carry, and a runtime
     * permission that buys nothing is a compliance surface rather than a feature.
     */
    private fun classifyIoFailure(io: IOException): CloudFailure {
        val message = io.message.orEmpty().lowercase()
        val unreachable = message.contains("unreachable") ||
            message.contains("unable to resolve host") ||
            message.contains("no address associated")
        return if (unreachable) CloudFailure.NO_NETWORK else CloudFailure.SERVICE_UNREACHABLE
    }

    private class HttpResponse(
        val status: Int,
        val body: String?,
        val lastModified: String?,
        val expires: String?,
        val etag: String?,
        val dateHeader: String?,
    )

    private class CloudCacheEntry(
        val lat4: Double,
        val lon4: Double,
        val fetchedAtUtc: Instant,
        val expiresAt: Instant,
        val lastModified: String?,
        val etag: String?,
        val modelTimeUtc: Instant?,
        val payload: String,
    ) {
        fun copy(
            expiresAt: Instant = this.expiresAt,
            lastModified: String? = this.lastModified,
            etag: String? = this.etag,
        ): CloudCacheEntry = CloudCacheEntry(
            lat4 = lat4,
            lon4 = lon4,
            fetchedAtUtc = fetchedAtUtc,
            expiresAt = expiresAt,
            lastModified = lastModified,
            etag = etag,
            modelTimeUtc = modelTimeUtc,
            payload = payload,
        )
    }

    private val fetchMutex = Mutex()

    private val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        const val CACHE_DIRECTORY: String = "forecast"

        const val CONNECT_TIMEOUT_MS: Int = 15_000
        const val READ_TIMEOUT_MS: Int = 20_000

        private const val HTTP_OK = 200
        private const val HTTP_DEPRECATED_PRODUCT = 203
        private const val HTTP_NOT_MODIFIED = 304
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}

/**
 * The on-disk cache record. Instants travel as ISO-8601 strings so the file is readable and diffable,
 * and the raw `payload` is kept verbatim so a `304` can renew the freshness fields without reparsing.
 */
@Serializable
internal class ForecastCacheDto(
    val lat4: Double,
    val lon4: Double,
    val fetchedAtUtc: String,
    val expiresUtc: String,
    val lastModified: String? = null,
    val etag: String? = null,
    val modelTimeUtc: String? = null,
    val payload: String,
)
