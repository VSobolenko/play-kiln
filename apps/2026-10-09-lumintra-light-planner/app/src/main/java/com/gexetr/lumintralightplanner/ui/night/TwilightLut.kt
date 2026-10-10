package com.gexetr.lumintralightplanner.ui.night

import androidx.compose.ui.graphics.Color
import com.gexetr.lumintralightplanner.data.BundledAsset
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import com.gexetr.lumintralightplanner.data.SkyLut
import com.gexetr.lumintralightplanner.data.TwilightLut as SkyTables
import com.gexetr.lumintralightplanner.ui.theme.BlueHour
import com.gexetr.lumintralightplanner.ui.theme.Ember
import com.gexetr.lumintralightplanner.ui.theme.EmberPale
import com.gexetr.lumintralightplanner.ui.theme.SkyHorizon
import com.gexetr.lumintralightplanner.ui.theme.SkyZenith
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow

/**
 * Linear-light sRGB radiance, the only colour space the sky is interpolated in.
 *
 * The scene capability is explicit that no gamma-encoded value is ever interpolated and no 8-bit value
 * exists anywhere before the tone map (`night-scene` § "The sky is evaluated from the twilight LUT").
 * Keeping the intermediate as three floats is what makes that true by construction: there is no
 * `ByteArray` in this file at all.
 */
data class LinearRgb(val r: Float, val g: Float, val b: Float) {

    operator fun plus(other: LinearRgb): LinearRgb = LinearRgb(r + other.r, g + other.g, b + other.b)

    operator fun times(k: Float): LinearRgb = LinearRgb(r * k, g * k, b * k)

    /** Per-channel scale, used by the Earth-shadow modulation ([earthShadowFactor]). */
    fun scaleChannels(sr: Float, sg: Float, sb: Float): LinearRgb =
        LinearRgb(r * sr, g * sg, b * sb)

    fun toDisplay(tonemapped: Boolean): Color {
        val rr = if (tonemapped) aces(r) else r
        val gg = if (tonemapped) aces(g) else g
        val bb = if (tonemapped) aces(b) else b
        return Color(
            red = linearToSrgb(rr),
            green = linearToSrgb(gg),
            blue = linearToSrgb(bb),
            alpha = 1f,
        )
    }
}

/**
 * The twilight sky table, and the one function that evaluates it.
 *
 * [sample] is a **pure function of `(ε, viewAltitude)`** — the solar altitude the table is indexed by
 * and the view elevation — so two evaluations of the same pair always return the same colour and the
 * whole scene is reproducible (`night-scene` § "The same frame renders the same pixels").
 *
 * **The table is read through `data/SkyAssetSource.kt`, which owns the asset's shape.** The header is
 * validated there before any cell is read (magic, `formatVersion`, axis counts, channel count, exact
 * length), and the cells come back **undecoded** because this renderer is their only consumer and
 * decodes them in its own sampler — the split `SkyLut`'s own documentation names. Nothing in this file
 * re-derives the asset's shape or opens `assets/` itself, so a format change cannot be fixed in one
 * place and left broken in the other.
 *
 * **The darkest band clamps, never extrapolates (E94).** A solar altitude at or below the table's
 * darkest row (−20.0°) resolves to that row with a finite weight; a deep polar night therefore renders
 * the darkest sky the table can express instead of a black screen, a negative index or a division by
 * zero.
 *
 * **The fallback is the asset-absent path and says so.** When `assets/sky/sky_lut_a.bin` is absent or
 * malformed — which is the state of this slot's `assets/` tree today — the table is replaced by a
 * monotone twilight ramp built from the slot's own `ui/theme/Color.kt` palette (deep night → first
 * light → blue hour), and [isFallback] reports `true` so the screen states the typed unavailable
 * condition rather than pretend a table was read (`night-scene` § "A missing sky asset is refused, not
 * invented"). The fallback is deliberately **azimuth-independent and monotone in `(ε, h)`**; it is the
 * honest degraded sky, not a second palette.
 *
 * **The AGSL shader path is not implemented in this slot.** `night-scene` § "The scene degrades by
 * capability" requires the shader to be additive and the row-strip fallback to be the path that must be
 * correct; this file is that path, and it is the only one, so the two can never drift because there is
 * one. The requirement's "API 33+ selects the shader" half is therefore **unsatisfied** and is reported
 * in the handoff rather than implied.
 */
class TwilightLut private constructor(
    private val a1Count: Int,
    private val a2Count: Int,
    private val a1StartDeg: Float,
    private val a1StepDeg: Float,
    private val a2StartDeg: Float,
    private val a2StepDeg: Float,
    private val exposure: FloatArray,
    private val shadowEdge: FloatArray,
    private val cells: FloatArray,
    /** The solar-meridian table's cells, or `null` when `sky_lut_b.bin` is not packaged. */
    private val solarCells: FloatArray?,
    /** True when no LUT asset was read and the palette ramp below answers instead. */
    val isFallback: Boolean,
) {

    /** Scratch for [sampleBlendedInto]; the UI thread is its only caller. */
    private val blendScratch = FloatArray(3)

    /** The range of solar altitudes the table carries; a value below it clamps to the darkest row. */
    val solarAltitudesDeg: ClosedRange<Double>
        get() = if (a1StepDeg >= 0f) {
            a1StartDeg.toDouble()..(a1StartDeg + a1StepDeg * (a1Count - 1)).toDouble()
        } else {
            (a1StartDeg + a1StepDeg * (a1Count - 1)).toDouble()..a1StartDeg.toDouble()
        }

    /** True when the second table is present, so [sampleBlended] is more than [sample]. */
    val hasSolarMeridian: Boolean get() = solarCells != null && !isFallback

    /** True when the table came from a real asset, so `(ε, h)` interpolation is bilinear. */
    val hasTable: Boolean get() = !isFallback && cells.isNotEmpty()

    /**
     * The sky radiance at `(ε, viewAltitude)`, in **linear light**.
     *
     * Bilinear in `(ε, h)` for a real table, both axes clamped into range (E94); the palette ramp for
     * the fallback. This is the single sky function the scene, the timeline strip and any exported
     * image all call, so no path can carry a second palette.
     */
    fun sample(epsilonDeg: Double, viewAltitudeDeg: Double): LinearRgb =
        sampleTable(cells, epsilonDeg, viewAltitudeDeg)

    /**
     * The sky radiance with the azimuth blend applied, in **linear light**.
     *
     * `LUT_A` is the antisolar meridian (Δaz = 180°) and `LUT_B` the solar meridian (Δaz = 0°), and the
     * requirement's blend is `w = (1 + cos Δaz) / 2`, applied **after both samples and before the tone
     * map** (`night-scene` § "The sky is evaluated from the twilight LUT"). With `sky_lut_b.bin` absent
     * this is exactly [sample], which is a stated capability loss rather than an invented second curve.
     */
    fun sampleBlended(epsilonDeg: Double, viewAltitudeDeg: Double, deltaAzimuthDeg: Double): LinearRgb {
        val solar = solarCells ?: return sample(epsilonDeg, viewAltitudeDeg)
        val w = ((1.0 + cos(Math.toRadians(deltaAzimuthDeg))) / 2.0).toFloat().coerceIn(0f, 1f)
        if (w <= 0f) return sample(epsilonDeg, viewAltitudeDeg)
        val antisolarColour = sample(epsilonDeg, viewAltitudeDeg)
        val solarColour = sampleTable(solar, epsilonDeg, viewAltitudeDeg)
        return LinearRgb(
            r = antisolarColour.r + (solarColour.r - antisolarColour.r) * w,
            g = antisolarColour.g + (solarColour.g - antisolarColour.g) * w,
            b = antisolarColour.b + (solarColour.b - antisolarColour.b) * w,
        )
    }

    /** The tonemapped display colour the chrome and the strip show for `(ε, h)`. */
    fun sampleDisplay(epsilonDeg: Double, viewAltitudeDeg: Double): Color =
        sample(epsilonDeg, viewAltitudeDeg).toDisplay(tonemapped = !isFallback)

    /**
     * [sampleBlended] writing straight into `out[offset..offset + 2]`, with **no allocation at all**.
     *
     * The scene samples this once per strip cell, so a 16 × 2369 strip is ~38 000 samples per rebuilt
     * frame; the object-returning form would allocate three `LinearRgb`s per sample and put ~1.2 MB per
     * frame into the young generation during a scrub. The arithmetic here is the same expressions as
     * [sampleBlended] and the two are the only implementations of it, so neither can drift.
     *
     * The instance scratch is safe because the caller is the single UI thread — stated rather than
     * assumed, because a shared mutable buffer is exactly the field that goes wrong when a second
     * caller appears.
     */
    fun sampleBlendedInto(
        epsilonDeg: Double,
        viewAltitudeDeg: Double,
        deltaAzimuthDeg: Double,
        out: FloatArray,
        offset: Int = 0,
    ) {
        val solar = solarCells
        if (solar == null || isFallback) {
            sampleTableInto(cells, epsilonDeg, viewAltitudeDeg, out, offset)
            return
        }
        val w = ((1.0 + cos(Math.toRadians(deltaAzimuthDeg))) / 2.0).toFloat().coerceIn(0f, 1f)
        sampleTableInto(cells, epsilonDeg, viewAltitudeDeg, out, offset)
        if (w <= 0f) return
        sampleTableInto(solar, epsilonDeg, viewAltitudeDeg, blendScratch, 0)
        out[offset] += (blendScratch[0] - out[offset]) * w
        out[offset + 1] += (blendScratch[1] - out[offset + 1]) * w
        out[offset + 2] += (blendScratch[2] - out[offset + 2]) * w
    }

    /** The allocation-free body shared by [sampleTable] and [sampleBlendedInto]. */
    private fun sampleTableInto(
        table: FloatArray,
        epsilonDeg: Double,
        viewAltitudeDeg: Double,
        out: FloatArray,
        offset: Int,
    ) {
        if (isFallback || table.isEmpty()) {
            fallbackRadianceInto(epsilonDeg, viewAltitudeDeg, out, offset)
            return
        }

        val rowF = rowFraction(epsilonDeg)
        val colF = if (a2Count <= 1 || a2StepDeg == 0f) {
            0f
        } else {
            ((viewAltitudeDeg.toFloat() - a2StartDeg) / a2StepDeg)
                .coerceIn(0f, (a2Count - 1).toFloat())
        }
        val r0 = rowF.toInt().coerceIn(0, a1Count - 1)
        val c0 = colF.toInt().coerceIn(0, a2Count - 1)
        val r1 = (r0 + 1).coerceAtMost(a1Count - 1)
        val c1 = (c0 + 1).coerceAtMost(a2Count - 1)
        val tr = (rowF - r0).coerceIn(0f, 1f)
        val tc = (colF - c0).coerceIn(0f, 1f)
        val exposure = exposureAt(epsilonDeg)
        for (channel in 0..2) {
            val i00 = ((r0 * a2Count) + c0) * 3 + channel
            val i01 = ((r0 * a2Count) + c1) * 3 + channel
            val i10 = ((r1 * a2Count) + c0) * 3 + channel
            val i11 = ((r1 * a2Count) + c1) * 3 + channel
            if (i11 >= table.size) {
                out[offset + channel] = 0f
                continue
            }
            val a = table[i00] + (table[i01] - table[i00]) * tc
            val b = table[i10] + (table[i11] - table[i10]) * tc
            out[offset + channel] = (a + (b - a) * tr) * exposure
        }
    }

    /**
     * The Earth-shadow edge altitude `α_s(ε)`, in degrees — the boundary the wedge is drawn below.
     *
     * It is derived, not authored: `α_s = ε + arcsin((R / 2H) · ε_rad²)` with `R = 6371 km`, `H = 20 km`,
     * floored at `ε` and a zenith when the argument exceeds unity. A real table carries the 61 baked
     * values; the fallback evaluates the same formula so neither path can drift from the other.
     */
    fun shadowEdgeDeg(epsilonDeg: Double): Double {
        if (!isFallback && shadowEdge.isNotEmpty()) {
            val rowF = rowFraction(epsilonDeg)
            val r0 = rowF.toInt().coerceIn(0, a1Count - 1)
            val r1 = (r0 + 1).coerceAtMost(a1Count - 1)
            val t = (rowF - r0).coerceIn(0f, 1f)
            return (shadowEdge[r0] + (shadowEdge[r1] - shadowEdge[r0]) * t).toDouble()
        }
        return earthShadowEdgeDeg(epsilonDeg)
    }

    private fun rowFraction(epsilonDeg: Double): Float {
        if (a1Count <= 1 || a1StepDeg == 0f) return 0f
        return ((a1StartDeg - epsilonDeg.toFloat()) / a1StepDeg)
            .coerceIn(0f, (a1Count - 1).toFloat())
    }

    private fun sampleTable(
        table: FloatArray,
        epsilonDeg: Double,
        viewAltitudeDeg: Double,
    ): LinearRgb {
        if (isFallback || table.isEmpty()) return fallbackRadiance(epsilonDeg, viewAltitudeDeg)

        val rowF = rowFraction(epsilonDeg)
        val colF = if (a2Count <= 1 || a2StepDeg == 0f) {
            0f
        } else {
            ((viewAltitudeDeg.toFloat() - a2StartDeg) / a2StepDeg)
                .coerceIn(0f, (a2Count - 1).toFloat())
        }
        val r0 = rowF.toInt().coerceIn(0, a1Count - 1)
        val c0 = colF.toInt().coerceIn(0, a2Count - 1)
        val r1 = (r0 + 1).coerceAtMost(a1Count - 1)
        val c1 = (c0 + 1).coerceAtMost(a2Count - 1)
        val tr = (rowF - r0).coerceIn(0f, 1f)
        val tc = (colF - c0).coerceIn(0f, 1f)

        fun cell(r: Int, c: Int): LinearRgb {
            val i = ((r * a2Count) + c) * 3
            if (i + 2 >= table.size) return LinearRgb(0f, 0f, 0f)
            return LinearRgb(table[i], table[i + 1], table[i + 2])
        }

        val a = lerpLinear(cell(r0, c0), cell(r0, c1), tc)
        val b = lerpLinear(cell(r1, c0), cell(r1, c1), tc)
        val rel = lerpLinear(a, b, tr)
        val e = exposureAt(epsilonDeg)
        return rel * e
    }

    private fun exposureAt(epsilonDeg: Double): Float {
        if (exposure.isEmpty()) return 1f
        val rowF = rowFraction(epsilonDeg)
        val r0 = rowF.toInt().coerceIn(0, a1Count - 1)
        val r1 = (r0 + 1).coerceAtMost(a1Count - 1)
        val t = (rowF - r0).coerceIn(0f, 1f)
        return exposure[r0] + (exposure[r1] - exposure[r0]) * t
    }

    companion object {

        /**
         * Reads the antisolar table through the data layer, or returns the palette fallback.
         *
         * A null source, an absent asset, a byte count that disagrees with the header or any malformed
         * value is the typed fallback — never an exception into the draw and never a partially read
         * table. [isFallback] on the result is what the screen reports.
         */
        fun load(source: SkyAssetSource?): TwilightLut {
            if (source == null) return fallback()
            return when (val tables = source.twilightLut()) {
                is BundledAsset.Present -> fromTables(tables.value) ?: fallback()
                is BundledAsset.Absent -> fallback()
                is BundledAsset.Malformed -> fallback()
            }
        }

        /** The documented palette-only fallback: no asset read, no invented table. */
        fun fallback(): TwilightLut = TwilightLut(
            a1Count = 0,
            a2Count = 0,
            a1StartDeg = 0f,
            a1StepDeg = 0f,
            a2StartDeg = 0f,
            a2StepDeg = 0f,
            exposure = FloatArray(0),
            shadowEdge = FloatArray(0),
            cells = FloatArray(0),
            solarCells = null,
            isFallback = true,
        )

        private fun fromTables(tables: SkyTables): TwilightLut? {
            val antisolar = tables.antisolar
            val cells = decodeCells(antisolar)
            if (cells.isEmpty()) return null
            val header = antisolar.header
            return TwilightLut(
                a1Count = header.solarAltitudeCount,
                a2Count = header.viewElevationCount,
                a1StartDeg = header.solarAltitudeStartDeg,
                a1StepDeg = header.solarAltitudeStepDeg,
                a2StartDeg = header.viewElevationStartDeg,
                a2StepDeg = header.viewElevationStepDeg,
                exposure = antisolar.exposureBySolarAltitude,
                shadowEdge = antisolar.earthShadowEdgeBySolarAltitude,
                cells = cells,
                solarCells = tables.solarMeridian?.let { decodeCells(it) }?.takeIf { it.isNotEmpty() },
                isFallback = false,
            )
        }

        /** The renderer's own sampler decode: `float16` cells, R, G, B, to linear floats. */
        private fun decodeCells(lut: SkyLut): FloatArray {
            val bytes = lut.cellBytes
            val count = lut.header.solarAltitudeCount * lut.header.viewElevationCount * 3
            if (bytes.size < count * 2) return FloatArray(0)
            val out = FloatArray(count)
            var p = 0
            for (i in 0 until count) {
                val raw = (bytes[p].toInt() and 0xFF) or ((bytes[p + 1].toInt() and 0xFF) shl 8)
                out[i] = halfToFloat(raw)
                p += 2
            }
            return out
        }

        private fun lerpLinear(a: LinearRgb, b: LinearRgb, t: Float): LinearRgb = LinearRgb(
            a.r + (b.r - a.r) * t,
            a.g + (b.g - a.g) * t,
            a.b + (b.b - a.b) * t,
        )

        /**
         * The palette ramp behind [isFallback]. Deep night at the darkest row, first light warm near the
         * horizon around `ε = −1°`, and the blue hour above it — every stop a constant of
         * `ui/theme/Color.kt`, so the fallback is the slot's own palette and not a second one.
         */
        // --- the fallback ramp's anchors, in linear light, computed once -------------------------
        //
        // `srgbToLinear` on the declared palette constants. `lerp()` on `Color` is an sRGB-space
        // interpolation, which is the right operation for *choosing* an anchor and the wrong one for
        // mixing light, so the anchors are converted here and mixed as radiance below.

        private val BLUE_HOUR_R = srgbToLinear(BlueHour.red)
        private val BLUE_HOUR_G = srgbToLinear(BlueHour.green)
        private val BLUE_HOUR_B = srgbToLinear(BlueHour.blue)
        private val SKY_ZENITH_R = srgbToLinear(SkyZenith.red)
        private val SKY_ZENITH_G = srgbToLinear(SkyZenith.green)
        private val SKY_ZENITH_B = srgbToLinear(SkyZenith.blue)
        private val SKY_HORIZON_R = srgbToLinear(SkyHorizon.red)
        private val SKY_HORIZON_G = srgbToLinear(SkyHorizon.green)
        private val SKY_HORIZON_B = srgbToLinear(SkyHorizon.blue)
        private val EMBER_PALE_R = srgbToLinear(EmberPale.red)
        private val EMBER_PALE_G = srgbToLinear(EmberPale.green)
        private val EMBER_PALE_B = srgbToLinear(EmberPale.blue)
        private val EMBER_R = srgbToLinear(Ember.red)
        private val EMBER_G = srgbToLinear(Ember.green)
        private val EMBER_B = srgbToLinear(Ember.blue)

        private fun fallbackRadiance(epsilonDeg: Double, viewAltitudeDeg: Double): LinearRgb {
            val out = FloatArray(3)
            fallbackRadianceInto(epsilonDeg, viewAltitudeDeg, out, 0)
            return LinearRgb(out[0], out[1], out[2])
        }

        /**
         * The same ramp, allocation-free.
         *
         * The palette anchors are converted to linear light **once**, in [ANKLE] and friends below,
         * rather than per sample: the earlier form called `Color.toLinear()` five times inside the
         * innermost loop, which is three allocations for every one of ~38 000 strip cells.
         */
        private fun fallbackRadianceInto(
            epsilonDeg: Double,
            viewAltitudeDeg: Double,
            out: FloatArray,
            offset: Int,
        ) {
            val t = ((10.0 - epsilonDeg) / 30.0).coerceIn(0.0, 1.0)
            val darkness = smoothstep(t).toFloat()

            // The Belt of Venus / first light: a bounded warm bump that peaks at ε = −1° and is gone by
            // ε = −8° (night-scene § "The alpenglow band feathers over the ridge" states the same shape).
            val warmArg = (epsilonDeg + 1.0) / 4.0
            val warmBump = exp(-warmArg * warmArg).toFloat()

            val elev = (viewAltitudeDeg.coerceIn(0.0, 90.0) / 90.0).pow(0.6).toFloat()
            val invElev = 1f - elev

            val zenithR = BLUE_HOUR_R + (SKY_ZENITH_R - BLUE_HOUR_R) * darkness
            val zenithG = BLUE_HOUR_G + (SKY_ZENITH_G - BLUE_HOUR_G) * darkness
            val zenithB = BLUE_HOUR_B + (SKY_ZENITH_B - BLUE_HOUR_B) * darkness

            val horizonR = EMBER_PALE_R + (SKY_HORIZON_R - EMBER_PALE_R) * darkness +
                EMBER_R * (0.30f * warmBump)
            val horizonG = EMBER_PALE_G + (SKY_HORIZON_G - EMBER_PALE_G) * darkness +
                EMBER_G * (0.30f * warmBump)
            val horizonB = EMBER_PALE_B + (SKY_HORIZON_B - EMBER_PALE_B) * darkness +
                EMBER_B * (0.30f * warmBump)

            out[offset] = horizonR * invElev + zenithR * elev
            out[offset + 1] = horizonG * invElev + zenithG * elev
            out[offset + 2] = horizonB * invElev + zenithB * elev
        }

        /** `α_s = ε + arcsin((R / 2H) · ε_rad²)`, floored at ε, zenith past unity. */
        private fun earthShadowEdgeDeg(epsilonDeg: Double): Double {
            val epsRad = Math.toRadians(epsilonDeg)
            val arg = (EARTH_RADIUS_KM / (2.0 * SHADOW_LAYER_HEIGHT_KM)) * epsRad * epsRad
            if (arg >= 1.0) return 90.0
            val edge = epsilonDeg + Math.toDegrees(asin(arg))
            return maxOf(edge, epsilonDeg)
        }

        private const val EARTH_RADIUS_KM: Double = 6371.0
        private const val SHADOW_LAYER_HEIGHT_KM: Double = 20.0
        private const val HALF: Int = 0x3FF

        private fun halfToFloat(h: Int): Float {
            val sign = (h and 0x8000) shl 16
            val exp = (h shr 10) and 0x1F
            val mant = h and HALF
            val bits: Int = when {
                exp == 0 && mant == 0 -> sign
                exp == 0 -> {
                    var e = -1
                    var m = mant
                    do {
                        m = m shl 1
                        e++
                    } while (m and 0x400 == 0)
                    sign or ((e + 112) shl 23) or ((m and HALF) shl 13)
                }

                exp == 0x1F -> sign or 0x7F800000 or (mant shl 13)
                else -> sign or ((exp + 112) shl 23) or (mant shl 13)
            }
            return Float.fromBits(bits)
        }
    }
}

/** `smoothstep(0, 1, t)` on an already-normalised `t`; the whole file's only easing. */
internal fun smoothstep(t: Double): Double {
    val x = t.coerceIn(0.0, 1.0)
    return x * x * (3.0 - 2.0 * x)
}

/** `smoothstep(edge0, edge1, x)`, ascending in `x`. */
internal fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
    if (edge0 == edge1) return if (x < edge0) 0.0 else 1.0
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)
}

/** Narkowicz's ACES filmic approximation, the tonemap `design.md` names for both render paths. */
internal fun aces(x: Float): Float {
    val v = x.coerceAtLeast(0f)
    return (v * (2.51f * v + 0.03f)) / (v * (2.43f * v + 0.59f) + 0.14f)
}

/** The sRGB piecewise transfer (IEC 61966-2-1) — never a 2.2 power. */
internal fun linearToSrgb(c: Float): Float {
    val v = c.coerceIn(0f, 1f)
    return if (v <= 0.0031308f) v * 12.92f else (1.055f * v.pow(1.0f / 2.4f) - 0.055f)
}

/** The inverse transfer, used to bring a declared palette constant into linear light. */
internal fun srgbToLinear(c: Float): Float {
    val v = c.coerceIn(0f, 1f)
    return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
}

/** A declared `Color` as linear radiance. */
internal fun Color.toLinear(): LinearRgb = LinearRgb(
    r = srgbToLinear(red),
    g = srgbToLinear(green),
    b = srgbToLinear(blue),
)

/** An sRGB-space lerp of two declared colours, used only for the palette ramp's anchors. */
internal fun lerp(a: Color, b: Color, t: Double): Color {
    val f = t.coerceIn(0.0, 1.0).toFloat()
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f,
    )
}

/**
 * The Earth-shadow wedge's darkening factor at a view direction, **applied inside layer 1** rather than
 * painted over the stars (`night-scene` § "The Earth-shadow wedge ... derived from the Sun's position").
 *
 * The surviving light there is long-path Rayleigh-scattered blue, so the wedge is a **blue-shifted**
 * darkening: the red channel is attenuated most, the blue least. It acts only on the antisolar half and
 * only below the derived `α_s`.
 */
internal fun earthShadowFactor(
    epsilonDeg: Double,
    viewAltitudeDeg: Double,
    viewAzimuthDeg: Double,
    sunAzimuthDeg: Double,
    shadowEdgeDeg: Double,
): Float {
    if (epsilonDeg >= -0.5) return 1f
    val antisolar = (1.0 - cos(Math.toRadians(viewAzimuthDeg - sunAzimuthDeg))) / 2.0
    if (antisolar <= 0.5) return 1f
    if (viewAltitudeDeg > shadowEdgeDeg) return 1f
    val depth = 0.45 * antisolar.coerceIn(0.0, 1.0) *
        smoothstep(0.0, 1.0, (shadowEdgeDeg - viewAltitudeDeg) / 12.0)
    return (1.0 - depth).toFloat().coerceIn(0f, 1f)
}

/** The wedge's per-channel attenuation: red most, blue least. */
internal fun applyEarthShadow(color: LinearRgb, factor: Float): LinearRgb = color.scaleChannels(
    sr = factor,
    sg = factor.pow(0.8f),
    sb = factor.pow(0.55f),
)
