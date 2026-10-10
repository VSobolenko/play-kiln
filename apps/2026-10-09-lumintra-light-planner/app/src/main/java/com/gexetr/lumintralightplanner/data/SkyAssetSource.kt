package com.gexetr.lumintralightplanner.data

import android.content.Context
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * The result of reading one bundled asset. Exactly one of three states, so a caller degrades by
 * **capability** rather than by a truth it cannot check:
 *
 * - [Present] — the asset was read and, where the asset has a declared shape, that shape validated.
 * - [Absent] — the asset is not in the APK, or could not be opened. The feature that needs it turns
 *   itself off; nothing else changes.
 * - [Malformed] — the bytes exist but disagree with the declared shape (a bad LUT magic, a truncated
 *   header, a star table that is not a whole number of records). Reading past the header is exactly
 *   what the LUT contract forbids, so this state is returned instead of a partial table.
 *
 * The `reason` on [Malformed] is a **technical diagnostic for logs**, not a user-facing sentence: the
 * screens own their own copy and nothing in this file produces UI text.
 */
sealed interface BundledAsset<out T> {

    /** The asset is usable. */
    data class Present<T>(val value: T) : BundledAsset<T>

    /** The asset is not packaged, or could not be opened. */
    data class Absent(val assetPath: String, val reason: AssetAbsenceReason) : BundledAsset<Nothing>

    /** The asset exists but its bytes disagree with the declared shape. */
    data class Malformed(val assetPath: String, val reason: String) : BundledAsset<Nothing>
}

/** Why a bundled asset is absent. Both cases end in the same degradation; they differ for the log. */
enum class AssetAbsenceReason {
    /** `Assets.open` raised [FileNotFoundException]: the path is not in the APK. */
    NOT_PACKAGED,

    /** The asset exists but reading it raised [IOException]. */
    READ_FAILED,
}

/**
 * The parsed header of one twilight LUT, `assets/sky/sky_lut_a.bin` / `sky_lut_b.bin`.
 *
 * Layout (design.md § "The sky scene pipeline", little-endian):
 *
 * | Offset | Length | Content |
 * |---:|---:|---|
 * | 0 | 8 B | magic ASCII `LUMSKY01` |
 * | 8 | 4 B | `formatVersion`, `uint32` = 1 |
 * | 12 | 2 B | `a1Count`, `uint16` = 61 (solar altitudes) |
 * | 14 | 2 B | `a2Count`, `uint16` = 91 (view elevations) |
 * | 16 | 4 B | `a1StartDeg`, `float32` = +10.0 |
 * | 20 | 4 B | `a1StepDeg`, `float32` = 0.5 (descending toward −20.0°) |
 * | 24 | 4 B | `a2StartDeg`, `float32` = 0.0 |
 * | 28 | 4 B | `a2StepDeg`, `float32` = 1.0 |
 * | 32 | 1 B | `channelCount`, `uint8` = 3 |
 * | 33 | 31 B | zero padding, to the fixed 64-byte header |
 *
 * The cells follow the header: `E(ε)` (61 × float32), `α_s(ε)` (61 × float32), then
 * `61 × 91 × 3` `float16` cells in R, G, B order. One file is 33,858 B; the pair is 67,716 B.
 */
data class SkyLutHeader(
    val formatVersion: Int,
    val solarAltitudeCount: Int,
    val viewElevationCount: Int,
    val solarAltitudeStartDeg: Float,
    val solarAltitudeStepDeg: Float,
    val viewElevationStartDeg: Float,
    val viewElevationStepDeg: Float,
    val channelCount: Int,
)

/**
 * One parsed twilight LUT. [cellBytes] holds the `float16` cells **undecoded**, in R, G, B order,
 * because the renderer is the only consumer and decodes in its own sampler; [exposureBySolarAltitude]
 * (`E(ε)`) and [earthShadowEdgeBySolarAltitude] (`α_s(ε)`) are decoded here because both are indexed
 * by solar altitude alone.
 */
class SkyLut(
    val header: SkyLutHeader,
    val exposureBySolarAltitude: FloatArray,
    val earthShadowEdgeBySolarAltitude: FloatArray,
    val cellBytes: ByteArray,
)

/**
 * The pair of twilight tables. [antisolar] (`sky_lut_a.bin`, Δaz = 180°) is the fallback path's
 * entire sky and is required; [solarMeridian] (`sky_lut_b.bin`, Δaz = 0°) is used only to blend on the
 * API 33+ shader path, so its absence is a graceful capability loss rather than a failure.
 */
class TwilightLut(
    val antisolar: SkyLut,
    val solarMeridian: SkyLut?,
)

/**
 * One row of the baked star table, `assets/catalog/stars.bin` (FK6 ⊕ hand-authored brightest-star
 * supplement). Per star, little-endian: RA then Dec as two `float32` (8 B), V as one `float16` (2 B),
 * and a colour-bucket index as one `uint8` (1 B) — **11 B per star**.
 */
data class Star(
    val raDeg: Double,
    val decDeg: Double,
    val magnitudeV: Float,
    val colourBucket: Int,
)

/** The whole baked star table; its size is discovery, never a hard-coded row count. */
data class StarTable(val stars: List<Star>)

/** One vertex of a constellation figure, in the asset's own units: RA in hours, Dec in degrees. */
data class FigureVertex(val raHours: Double, val decDeg: Double)

/**
 * The constellation figures from `assets/sky/constellation_lines.json`: a JSON object keyed by the IAU
 * three-letter abbreviation, whose every value is an array of polylines, each polyline an array of
 * `[raHours, decDegrees]` vertices.
 *
 * The credit travels with the data: *"Constellation figures: IAU and Sky & Telescope (Roger Sinnott,
 * Rick Fienberg; patterns by Alan MacRobert) — CC BY 4.0"* (design.md § "Sky assets and attribution").
 */
class ConstellationFigures(val polylinesByAbbrev: Map<String, List<List<FigureVertex>>>) {

    /** The abbreviations the asset actually carries. */
    val abbreviations: Set<String> get() = polylinesByAbbrev.keys
}

/**
 * Reads the app's bundled sky and catalogue binaries, and answers for each a **typed presence** so the
 * scene and the catalogue degrade by capability. This class makes **no network call** and writes
 * nothing; it only opens `assets/`.
 *
 * The asset shapes are the design's, taken from design.md § "Sky assets and attribution" and
 * § "The sky scene pipeline":
 *
 * - `assets/sky/sky_lut_a.bin`, `assets/sky/sky_lut_b.bin` — the twilight tables described on
 *   [SkyLutHeader]. A file whose magic, version, axis counts, channel count or length disagrees with
 *   the declared shape is rejected before any cell is read.
 * - `assets/sky/milky_way_eso0932a.webp` — the self-baked Milky Way, an **equirectangular** panorama
 *   of at most **4096 × 2048**, WebP, under 4 MB, from the ESO panorama `eso0932a` (S. Brunier),
 *   **CC BY 4.0**, credited `ESO/S. Brunier` beside the image.
 * - `assets/sky/moon_svs.webp` — the NASA SVS Moon Kit image, **public domain**, acknowledged with no
 *   agency insignia and never credited to JPL-Caltech.
 * - `assets/catalog/stars.bin` — the FK6 ⊕ supplement star table, **11 B per star** as on [Star].
 * - `assets/sky/constellation_lines.json` — the figure polylines as on [ConstellationFigures].
 *
 * None of these is required for the app to start: every reader answers with a typed presence, and the
 * scene degrades one layer at a time.
 */
class SkyAssetSource(private val context: Context) {

    /**
     * Opens a bundled asset for reading, or returns `null` when it is not packaged. Callers own the
     * returned stream and must close it; this exists so [CatalogueRepository] reads its JSON through
     * the same door as the binaries rather than deriving the asset root twice.
     */
    fun open(path: String): InputStream? = try {
        context.assets.open(path)
    } catch (missing: FileNotFoundException) {
        null
    } catch (io: IOException) {
        null
    }

    /** `assets/sky/sky_lut_a.bin` + `sky_lut_b.bin`, parsed and validated against [SkyLutHeader]. */
    fun twilightLut(): BundledAsset<TwilightLut> {
        val antisolar = when (val bytes = readBytes(LUT_A_PATH)) {
            is BundledAsset.Present -> parseLut(LUT_A_PATH, bytes.value)
            is BundledAsset.Absent -> return bytes
            is BundledAsset.Malformed -> return bytes
        }
        val antisolarLut = when (antisolar) {
            is BundledAsset.Present -> antisolar.value
            is BundledAsset.Absent -> return antisolar
            is BundledAsset.Malformed -> return antisolar
        }
        val solarMeridian = when (val bytes = readBytes(LUT_B_PATH)) {
            is BundledAsset.Present -> {
                val parsed = parseLut(LUT_B_PATH, bytes.value)
                if (parsed is BundledAsset.Present) parsed.value else null
            }
            else -> null
        }
        return BundledAsset.Present(TwilightLut(antisolarLut, solarMeridian))
    }

    /**
     * `assets/sky/milky_way_eso0932a.webp`, present or absent. The bytes are returned undecoded: the
     * renderer decodes the WebP itself, and a decode here would put image work on the load path.
     */
    fun milkyWayMap(): BundledAsset<ByteArray> = readBytes(MILKY_WAY_PATH)

    /**
     * `assets/sky/moon_svs.webp` — the NASA SVS Moon Kit image, **public domain**, acknowledged in
     * the credits with **no agency insignia** and **never credited to JPL-Caltech** (design.md
     * § "Sky assets and attribution").
     *
     * Undecoded bytes for the same reason as [milkyWayMap]: the scene's L6 layer draws the disc and
     * clips it to the phase, so a decode here would put image work on the load path and be thrown
     * away. Its absence is a capability loss — the Moon is then drawn as a magnitude-scaled disc with
     * a label, exactly the rendering the planets use — and never a reason to hide the Moon.
     */
    fun moonImage(): BundledAsset<ByteArray> = readBytes(MOON_PATH)

    /** `assets/catalog/stars.bin`, parsed into [StarTable]. */
    fun starTable(): BundledAsset<StarTable> = when (val bytes = readBytes(STAR_TABLE_PATH)) {
        is BundledAsset.Present -> parseStarTable(STAR_TABLE_PATH, bytes.value)
        is BundledAsset.Absent -> bytes
        is BundledAsset.Malformed -> bytes
    }

    /** `assets/sky/constellation_lines.json`, parsed into [ConstellationFigures]. */
    fun constellationFigures(): BundledAsset<ConstellationFigures> {
        val text = when (val bytes = readBytes(FIGURE_PATH)) {
            is BundledAsset.Present -> bytes.value.decodeToString()
            is BundledAsset.Absent -> return bytes
            is BundledAsset.Malformed -> return bytes
        }
        val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return BundledAsset.Malformed(FIGURE_PATH, "root is not a JSON object")
        val out = LinkedHashMap<String, List<List<FigureVertex>>>()
        for ((abbrev, element) in root) {
            val polylines = element as? JsonArray
                ?: return BundledAsset.Malformed(FIGURE_PATH, "figure '$abbrev' is not an array")
            val lines = ArrayList<List<FigureVertex>>(polylines.size)
            for (polyline in polylines) {
                val vertices = polyline as? JsonArray
                    ?: return BundledAsset.Malformed(FIGURE_PATH, "polyline '$abbrev' is not an array")
                val line = ArrayList<FigureVertex>(vertices.size)
                for (vertex in vertices) {
                    val pair = vertex as? JsonArray
                        ?: return BundledAsset.Malformed(FIGURE_PATH, "vertex '$abbrev' is not an array")
                    if (pair.size < 2) {
                        return BundledAsset.Malformed(FIGURE_PATH, "vertex '$abbrev' has fewer than two numbers")
                    }
                    val raHours = (pair[0] as? JsonPrimitive)?.doubleOrNull
                    val decDeg = (pair[1] as? JsonPrimitive)?.doubleOrNull
                    if (raHours == null || decDeg == null) {
                        return BundledAsset.Malformed(FIGURE_PATH, "vertex '$abbrev' is not numeric")
                    }
                    line.add(FigureVertex(raHours, decDeg))
                }
                lines.add(line)
            }
            out[abbrev] = lines
        }
        return BundledAsset.Present(ConstellationFigures(out))
    }

    /** Reads a bundled text asset, through the same presence typing as the binaries. */
    internal fun readText(path: String): BundledAsset<String> = when (val bytes = readBytes(path)) {
        is BundledAsset.Present -> BundledAsset.Present(bytes.value.decodeToString())
        is BundledAsset.Absent -> bytes
        is BundledAsset.Malformed -> bytes
    }

    /** Reads a bundled binary asset, or reports why it could not be read. */
    internal fun readBytes(path: String): BundledAsset<ByteArray> {
        return try {
            val bytes = context.assets.open(path).use { it.readBytes() }
            BundledAsset.Present(bytes)
        } catch (missing: FileNotFoundException) {
            BundledAsset.Absent(path, AssetAbsenceReason.NOT_PACKAGED)
        } catch (io: IOException) {
            BundledAsset.Absent(path, AssetAbsenceReason.READ_FAILED)
        }
    }

    private fun parseLut(path: String, bytes: ByteArray): BundledAsset<SkyLut> {
        if (bytes.size < HEADER_BYTES) {
            return BundledAsset.Malformed(path, "shorter than the ${HEADER_BYTES}-byte header: ${bytes.size} B")
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(LUT_MAGIC.length)
        buffer.get(magic)
        if (String(magic, Charsets.US_ASCII) != LUT_MAGIC) {
            return BundledAsset.Malformed(path, "bad magic: ${String(magic, Charsets.US_ASCII)}")
        }
        val version = buffer.int
        if (version != LUT_FORMAT_VERSION) {
            return BundledAsset.Malformed(path, "formatVersion $version, expected $LUT_FORMAT_VERSION")
        }
        val a1 = buffer.short.toInt() and 0xFFFF
        val a2 = buffer.short.toInt() and 0xFFFF
        val a1Start = buffer.float
        val a1Step = buffer.float
        val a2Start = buffer.float
        val a2Step = buffer.float
        val channels = buffer.get().toInt() and 0xFF
        if (a1 != LUT_A1_COUNT) return BundledAsset.Malformed(path, "a1Count $a1, expected $LUT_A1_COUNT")
        if (a2 != LUT_A2_COUNT) return BundledAsset.Malformed(path, "a2Count $a2, expected $LUT_A2_COUNT")
        if (channels != LUT_CHANNELS) return BundledAsset.Malformed(path, "channelCount $channels, expected $LUT_CHANNELS")

        val expectedBytes = HEADER_BYTES + a1 * FLOAT_BYTES + a1 * FLOAT_BYTES + a1 * a2 * channels * HALF_BYTES
        if (bytes.size < expectedBytes) {
            return BundledAsset.Malformed(path, "length ${bytes.size} B, expected at least $expectedBytes B")
        }
        buffer.position(HEADER_BYTES)
        val exposure = FloatArray(a1) { buffer.float }
        val shadowEdge = FloatArray(a1) { buffer.float }
        val cells = ByteArray(a1 * a2 * channels * HALF_BYTES)
        buffer.get(cells)
        val header = SkyLutHeader(
            formatVersion = version,
            solarAltitudeCount = a1,
            viewElevationCount = a2,
            solarAltitudeStartDeg = a1Start,
            solarAltitudeStepDeg = a1Step,
            viewElevationStartDeg = a2Start,
            viewElevationStepDeg = a2Step,
            channelCount = channels,
        )
        return BundledAsset.Present(SkyLut(header, exposure, shadowEdge, cells))
    }

    private fun parseStarTable(path: String, bytes: ByteArray): BundledAsset<StarTable> {
        if (bytes.isEmpty()) return BundledAsset.Malformed(path, "empty")
        if (bytes.size % STAR_BYTES != 0) {
            return BundledAsset.Malformed(path, "size ${bytes.size} B is not a multiple of $STAR_BYTES B")
        }
        val count = bytes.size / STAR_BYTES
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val stars = ArrayList<Star>(count)
        repeat(count) {
            val raDeg = buffer.float.toDouble()
            val decDeg = buffer.float.toDouble()
            val magnitude = halfToFloat(buffer.short.toInt() and 0xFFFF)
            val colourBucket = buffer.get().toInt() and 0xFF
            stars.add(Star(raDeg, decDeg, magnitude, colourBucket))
        }
        return BundledAsset.Present(StarTable(stars))
    }

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val LUT_A_PATH: String = "sky/sky_lut_a.bin"
        const val LUT_B_PATH: String = "sky/sky_lut_b.bin"
        const val MILKY_WAY_PATH: String = "sky/milky_way_eso0932a.webp"
        const val MOON_PATH: String = "sky/moon_svs.webp"
        const val STAR_TABLE_PATH: String = "catalog/stars.bin"
        const val FIGURE_PATH: String = "sky/constellation_lines.json"

        const val LUT_MAGIC: String = "LUMSKY01"
        const val LUT_FORMAT_VERSION: Int = 1
        const val LUT_A1_COUNT: Int = 61
        const val LUT_A2_COUNT: Int = 91
        const val LUT_CHANNELS: Int = 3
        const val HEADER_BYTES: Int = 64
        const val STAR_BYTES: Int = 11
        private const val FLOAT_BYTES: Int = 4
        private const val HALF_BYTES: Int = 2

        /**
         * IEEE 754 binary16 → binary32. The table stores V as `float16`; there is no JVM `Half` on
         * `minSdk = 26`, so the conversion is written out rather than reached for from a library.
         */
        internal fun halfToFloat(bits: Int): Float {
            val sign = (bits ushr 15) and 0x1
            val exponent = (bits ushr 10) and 0x1F
            val mantissa = bits and 0x3FF
            val asInt = when (exponent) {
                0x00 -> if (mantissa == 0) {
                    sign shl 31
                } else {
                    var normalized = mantissa
                    var shift = -1
                    while ((normalized and 0x400) == 0) {
                        normalized = normalized shl 1
                        shift++
                    }
                    normalized = normalized and 0x3FF
                    (sign shl 31) or ((shift - 15 + 127) shl 23) or (normalized shl 13)
                }
                0x1F -> (sign shl 31) or 0x7F800000 or (mantissa shl 13)
                else -> (sign shl 31) or ((exponent - 15 + 127) shl 23) or (mantissa shl 13)
            }
            return Float.fromBits(asInt)
        }
    }
}
