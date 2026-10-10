package com.gexetr.lumintralightplanner.data

import com.gexetr.lumintralightplanner.domain.CloudHour
import com.gexetr.lumintralightplanner.domain.CloudReport
import com.gexetr.lumintralightplanner.domain.ForecastAge
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.NightSpan
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests over the **pure** half of the forecast client: the coordinate truncation, the request
 * URL and `User-Agent` it builds, and the parse of a captured `api.met.no` timeseries body.
 *
 * These are the three things the `cloud-forecast` spec's first requirement pins down, and each of
 * them is a place a silent defect reaches the network: a fifth decimal is answered with **403**, a
 * randomised `User-Agent` is answered with a ban, and a mis-read `cloud_area_fraction` publishes a
 * number the source never sent.
 */
class CloudRepositoryTest {

    // -------------------------------------------------------------------------------------------
    // Coordinate truncation — 4 decimals, truncated and never rounded
    // -------------------------------------------------------------------------------------------

    @Test
    fun `truncation moves toward zero and never rounds up`() {
        assertEquals(59.9138, GeodeticPoint.truncateTo4(59.91387), 1e-12)
        assertEquals(10.7523, GeodeticPoint.truncateTo4(10.75239), 1e-12)
        // A value whose fifth decimal would round the fourth up must still go down.
        assertEquals(59.9138, GeodeticPoint.truncateTo4(59.91389), 1e-12)
        // Negative coordinates truncate toward zero as well, not toward minus infinity.
        assertEquals(-10.7523, GeodeticPoint.truncateTo4(-10.75239), 1e-12)
        assertEquals(-0.0004, GeodeticPoint.truncateTo4(-0.00049), 1e-12)
    }

    @Test
    fun `formatting a coordinate cannot exceed four decimals`() {
        assertEquals("59.9138", CloudProtocol.formatCoordinate(59.91387))
        assertEquals("10.7523", CloudProtocol.formatCoordinate(10.75239))
        assertEquals("-10.7523", CloudProtocol.formatCoordinate(-10.75239))
        // Trailing zeros are dropped; a whole number carries no decimal point at all.
        assertEquals("59", CloudProtocol.formatCoordinate(59.0))
        assertEquals("0", CloudProtocol.formatCoordinate(0.00004))
        assertEquals("0.0004", CloudProtocol.formatCoordinate(0.0004))
        // The locale-sensitive `%.4f` formatter is deliberately not used: a comma decimal separator
        // would build a different request and earn a 400.
        assertTrue("a comma decimal separator reached the URL", "," !in CloudProtocol.formatCoordinate(59.9138))
    }

    // -------------------------------------------------------------------------------------------
    // The request URL
    // -------------------------------------------------------------------------------------------

    @Test
    fun `the request URL carries the spec's truncated coordinate pair`() {
        val url = CloudProtocol.requestUrl(GeodeticPoint(59.91387, 10.75239))
        assertEquals("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=59.9138&lon=10.7523", url)
        assertTrue("a fifth decimal reached the URL", !url.contains("59.91387"))
    }

    @Test
    fun `altitude is sent only when the spot carries one`() {
        val withElevation = CloudProtocol.requestUrl(GeodeticPoint(59.91387, 10.75239, 135.2))
        assertEquals(
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=59.9138&lon=10.7523&altitude=135",
            withElevation,
        )

        val withoutElevation = CloudProtocol.requestUrl(GeodeticPoint(59.91387, 10.75239, 0.0))
        assertTrue("altitude=0 was sent", !withoutElevation.contains("altitude"))

        // A below-sea-level spot states its negative elevation rather than dropping the parameter.
        val belowSeaLevel = CloudProtocol.requestUrl(GeodeticPoint(31.5, 35.5, -430.0))
        assertTrue(belowSeaLevel.endsWith("&altitude=-430"))
    }

    @Test
    fun `the cache key collapses two spots inside one truncated cell`() {
        val first = CloudProtocol.cacheKey(GeodeticPoint(59.91381, 10.75231))
        val second = CloudProtocol.cacheKey(GeodeticPoint(59.91389, 10.75239))
        assertEquals("59.9138_10.7523", first)
        assertEquals("two spots in one 0.0001 cell produced two cache entries", first, second)
    }

    @Test
    fun `the User-Agent names the app, its version and the contact address`() {
        assertEquals(
            "Lumintra/1.2.0 (v.sobolenko.vs@gmail.com)",
            CloudProtocol.userAgent("1.2.0"),
        )
    }

    // -------------------------------------------------------------------------------------------
    // Parsing a captured timeseries
    // -------------------------------------------------------------------------------------------

    @Test
    fun `a captured timeseries parses to total cloud hours only`() {
        val parsed = CloudProtocol.parseTimeseries(CAPTURED_COMPACT_BODY)
        assertTrue("body was refused: $parsed", parsed is CloudParse.Hours)
        val hours = (parsed as CloudParse.Hours).hours

        assertEquals(5, hours.size)
        assertEquals(Instant.parse("2026-10-09T21:00:00Z"), hours[0].hourUtc)
        assertEquals(0, hours[0].cloudCoverPercent)
        assertEquals(25, hours[1].cloudCoverPercent)
        // 62.4 is rounded to a whole per cent for display, and the layer sibling is not read at all.
        assertEquals(62, hours[2].cloudCoverPercent)
        assertEquals(100, hours[3].cloudCoverPercent)
        // An integer JSON literal is a number too.
        assertEquals(76, hours[4].cloudCoverPercent)
        assertEquals(Instant.parse("2026-10-10T01:00:00Z"), hours[4].hourUtc)
    }

    @Test
    fun `no layer field is invented beside the total`() {
        val hours = (CloudProtocol.parseTimeseries(CAPTURED_COMPACT_BODY) as CloudParse.Hours).hours
        // The captured body carries cloud_area_fraction_high on one entry; the parsed value is the
        // total. CloudHour has no layer field at all, which is the intent the spec states.
        assertEquals(62, hours[2].cloudCoverPercent)
        assertTrue(hours.all { it.cloudCoverPercent in 0..100 })
    }

    @Test
    fun `an empty timeseries parses to an empty hour list rather than a malformed body`() {
        val body = """{"properties":{"timeseries":[]}}"""
        val parsed = CloudProtocol.parseTimeseries(body)
        assertTrue(parsed is CloudParse.Hours)
        assertEquals(0, (parsed as CloudParse.Hours).hours.size)
    }

    @Test
    fun `a body the parser cannot read is refused whole, never half-read`() {
        val notJson = CloudProtocol.parseTimeseries("<html>503 Service Unavailable</html>")
        assertTrue(notJson is CloudParse.Malformed)

        val noTimeseries = CloudProtocol.parseTimeseries("""{"properties":{"meta":{}}}""")
        assertTrue(noTimeseries is CloudParse.Malformed)

        val missingCloud = CloudProtocol.parseTimeseries(
            """{"properties":{"timeseries":[{"time":"2026-10-09T21:00:00Z","data":{"instant":{"details":{"air_temperature":6.8}}}}]}}""",
        )
        assertTrue(missingCloud is CloudParse.Malformed)

        val noDetails = CloudProtocol.parseTimeseries(
            """{"properties":{"timeseries":[{"time":"2026-10-09T21:00:00Z","data":{"instant":{}}}]}}""",
        )
        assertTrue(noDetails is CloudParse.Malformed)

        val noTime = CloudProtocol.parseTimeseries(
            """{"properties":{"timeseries":[{"data":{"instant":{"details":{"cloud_area_fraction":10}}}}]}}""",
        )
        assertTrue(noTime is CloudParse.Malformed)

        val notAnInstant = CloudProtocol.parseTimeseries(
            """{"properties":{"timeseries":[{"time":"tomorrow","data":{"instant":{"details":{"cloud_area_fraction":10}}}}]}}""",
        )
        assertTrue(notAnInstant is CloudParse.Malformed)

        // One bad entry refuses the whole body: a partially parsed forecast is a fabricated one.
        val oneBadEntry = CloudProtocol.parseTimeseries(
            """{"properties":{"timeseries":[
                {"time":"2026-10-09T21:00:00Z","data":{"instant":{"details":{"cloud_area_fraction":10}}}},
                {"time":"2026-10-09T22:00:00Z","data":{"instant":{"details":{}}}}
            ]}}""",
        )
        assertTrue(oneBadEntry is CloudParse.Malformed)

        val outOfRange = CloudProtocol.parseTimeseries(
            """{"properties":{"timeseries":[{"time":"2026-10-09T21:00:00Z","data":{"instant":{"details":{"cloud_area_fraction":140}}}}]}}""",
        )
        assertTrue(outOfRange is CloudParse.Malformed)
    }

    // -------------------------------------------------------------------------------------------
    // The report derived from the parsed hours
    // -------------------------------------------------------------------------------------------

    @Test
    fun `the report derives its window statistics from the parsed hours`() {
        val hours = (CloudProtocol.parseTimeseries(CAPTURED_COMPACT_BODY) as CloudParse.Hours).hours
        val fetchedAt = Instant.parse("2026-10-09T20:40:00Z")
        val report = CloudReport(hours, ForecastAge.from(fetchedAt, Instant.parse("2026-10-09T20:40:00Z")))

        val window = NightSpan(Instant.parse("2026-10-09T22:00:00Z"), Instant.parse("2026-10-10T00:00:00Z"))
        val inside = report.hoursWithin(window)
        assertEquals(2, inside.size)
        assertEquals(Instant.parse("2026-10-09T22:00:00Z"), inside[0].hourUtc)
        // The end of a half-open window is excluded.
        assertEquals(Instant.parse("2026-10-09T23:00:00Z"), inside[1].hourUtc)

        // 25 % and 62 % average to 43.5, which rounds to 44.
        assertEquals(44, report.meanCloudPercentWithin(window))
        assertEquals(25, report.clearestHourWithin(window)?.cloudCoverPercent)

        val emptyWindow = NightSpan(Instant.parse("2026-10-11T00:00:00Z"), Instant.parse("2026-10-11T03:00:00Z"))
        assertNull("an uncovered window must report no mean, not a zero", report.meanCloudPercentWithin(emptyWindow))
        assertNull(report.clearestHourWithin(emptyWindow))
        assertTrue(CloudReport.SOURCE_LABEL.isNotEmpty())
    }

    @Test
    fun `an age is never negative when the fetch instant is ahead of the session instant`() {
        val fetched = Instant.parse("2026-10-09T21:00:00Z")
        val earlier = Instant.parse("2026-10-09T20:00:00Z")
        val age = ForecastAge.from(fetched, earlier)
        assertEquals(Duration.ZERO, age.age)
        assertTrue("a clamped age must not read as stale", !age.isStale)
    }

    // -------------------------------------------------------------------------------------------
    // Conditional-request bookkeeping
    // -------------------------------------------------------------------------------------------

    @Test
    fun `an HTTP date header is read, and an unreadable one is null rather than invented`() {
        assertEquals(
            Instant.parse("2026-10-21T07:28:00Z"),
            CloudProtocol.parseHttpDate("Wed, 21 Oct 2026 07:28:00 GMT"),
        )
        assertNull(CloudProtocol.parseHttpDate("not a date"))
        assertNull(CloudProtocol.parseHttpDate(null))
        assertNull(CloudProtocol.parseHttpDate("   "))
    }

    @Test
    fun `the next fetch instant honours Expires and always carries a random offset`() {
        val now = Instant.parse("2026-10-09T20:00:00Z")
        val expires = Instant.parse("2026-10-09T22:00:00Z")
        val next = CloudProtocol.nextFetchInstant(expires, now, Random(7))
        assertTrue("the refresh must not precede Expires", next.isAfter(expires))
        assertTrue(
            "the jitter must stay inside one to six minutes",
            !next.isAfter(expires.plusSeconds(CloudProtocol.MIN_JITTER_SECONDS + CloudProtocol.JITTER_SPAN_SECONDS)),
        )

        // An absent or already-past Expires falls back to a window rather than to "now", so a burst
        // of calls cannot become a request storm.
        val fallback = CloudProtocol.nextFetchInstant(null, now, Random(7))
        assertTrue(fallback.isAfter(now.plus(CloudProtocol.FALLBACK_FRESH_WINDOW)))
        val pastExpires = CloudProtocol.nextFetchInstant(now.minusSeconds(60), now, Random(7))
        assertTrue(pastExpires.isAfter(now))
    }

    @Test
    fun `CloudHour refuses a fraction outside nought to one hundred`() {
        val refused = runCatching { CloudHour(Instant.parse("2026-10-09T21:00:00Z"), 101) }.isFailure
        assertTrue("CloudHour accepted 101 %", refused)
        assertEquals(100, CloudHour(Instant.parse("2026-10-09T21:00:00Z"), 100).cloudCoverPercent)
    }

    private companion object {
        /**
         * A trimmed capture of a real `locationforecast/2.0/compact` body — a live response carried
         * 87 hourly points; five are kept here, with the surrounding shape untouched. Two details are
         * deliberate: the entry at 23:00 carries a `cloud_area_fraction_high` sibling to prove no
         * layer field is read, and the last entry carries its fraction as an integer literal.
         */
        val CAPTURED_COMPACT_BODY: String = """
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [ 10.7523, 59.9138, 135 ] },
              "properties": {
                "meta": {
                  "updated_at": "2026-10-09T18:23:22Z",
                  "units": { "air_temperature": "celsius", "cloud_area_fraction": "%" }
                },
                "timeseries": [
                  {
                    "time": "2026-10-09T21:00:00Z",
                    "data": { "instant": { "details": { "air_temperature": 6.8, "cloud_area_fraction": 0.0, "relative_humidity": 88.5 } } }
                  },
                  {
                    "time": "2026-10-09T22:00:00Z",
                    "data": { "instant": { "details": { "air_temperature": 6.5, "cloud_area_fraction": 25.0 } } }
                  },
                  {
                    "time": "2026-10-09T23:00:00Z",
                    "data": { "instant": { "details": { "air_temperature": 6.2, "cloud_area_fraction": 62.4, "cloud_area_fraction_high": 90.0 } } }
                  },
                  {
                    "time": "2026-10-10T00:00:00Z",
                    "data": { "instant": { "details": { "air_temperature": 5.9, "cloud_area_fraction": 100.0 } } }
                  },
                  {
                    "time": "2026-10-10T01:00:00Z",
                    "data": {
                      "instant": { "details": { "cloud_area_fraction": 76 } },
                      "next_1_hours": { "summary": { "symbol_code": "partlycloudy_night" }, "details": { "precipitation_amount": 0.0 } }
                    }
                  }
                ]
              }
            }
        """.trimIndent()
    }
}
