package com.gexetr.lumintralightplanner.ui.night

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as NativeCanvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as NativePath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gexetr.lumintralightplanner.data.ConstellationFigures
import com.gexetr.lumintralightplanner.data.StarTable
import com.gexetr.lumintralightplanner.domain.HorizontalCoords
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.ObjectType
import com.gexetr.lumintralightplanner.domain.PassVisibility
import com.gexetr.lumintralightplanner.domain.SceneBody
import com.gexetr.lumintralightplanner.domain.SceneCamera
import com.gexetr.lumintralightplanner.domain.SceneFrame
import com.gexetr.lumintralightplanner.domain.ScenePassTrail
import com.gexetr.lumintralightplanner.domain.SceneTrailPoint
import com.gexetr.lumintralightplanner.ui.theme.LumintraColors
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * Process-wide memory-pressure seam for the night scene's large decoded surfaces.
 *
 * [onTrimMemory] recycles the sky LUT raster strips held by every live [RenderCache], unbinds the
 * Milky Way [BitmapShader] (the framework form of Compose's `ImageShader`), and lowers the drawn
 * star magnitude limit so the next frame rebuilds a thinner field. The decoded Milky Way /
 * Moon bitmaps themselves stay owned by the session loader; recycling them while that owner still
 * holds a strong reference would crash the next draw, so this path drops the GPU binding and the
 * LUT strips — the two allocations this module actually owns.
 */
object SceneMemoryPressure {

    private val _trimGeneration = MutableStateFlow(0)
    val trimGeneration: StateFlow<Int> = _trimGeneration.asStateFlow()

    /** Drawn magnitude ceiling after the last trim; [StarField.DEFAULT_MAGNITUDE_LIMIT] when idle. */
    @Volatile
    var magnitudeLimit: Double = StarField.DEFAULT_MAGNITUDE_LIMIT
        private set

    private val caches = CopyOnWriteArrayList<WeakReference<RenderCache>>()
    private var installed: Boolean = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        context.applicationContext.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    this@SceneMemoryPressure.onTrimMemory(level)
                }

                override fun onConfigurationChanged(newConfig: android.content.res.Configuration) = Unit

                override fun onLowMemory() {
                    this@SceneMemoryPressure.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
                }
            },
        )
    }

    internal fun register(cache: RenderCache) {
        caches.add(WeakReference(cache))
    }

    internal fun unregister(cache: RenderCache) {
        caches.removeAll { ref -> ref.get() == null || ref.get() === cache }
    }

    fun onTrimMemory(level: Int) {
        if (level < ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) return
        magnitudeLimit = when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> 4.0
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                level == ComponentCallbacks2.TRIM_MEMORY_MODERATE -> 5.0
            else -> min(magnitudeLimit, 5.5)
        }
        val alive = ArrayList<RenderCache>(caches.size)
        val iterator = caches.iterator()
        while (iterator.hasNext()) {
            val cache = iterator.next().get()
            if (cache == null) {
                iterator.remove()
            } else {
                alive.add(cache)
            }
        }
        for (cache in alive) cache.releaseLargeBitmaps()
        _trimGeneration.value = _trimGeneration.value + 1
    }
}

/**
 * The equator → horizon transform the scene places every sky coordinate through.
 *
 * **Why this is a parameter and not a formula here.** The scene must draw a star at its real azimuth and
 * altitude, and the only astronomy in `SceneFrame` is `skyRotationRad` — a single angle. Turning
 * `(RA, Dec)` into `(az, alt)` needs the observer's latitude *and* the local sidereal time, and neither
 * is a field of the frame, so a rotation alone cannot produce a position. `skyRotationRad` is therefore
 * the **cache key** for the transform, and this interface is how the transform reaches the draw: the
 * caller supplies it from the same `(spot, instant)` pair that produced the frame, so it is a pure
 * function of the frame and two renders of one frame still produce identical pixels.
 *
 * The caller's implementation must apply the same refraction convention as the rest of the app
 * (`Refraction.Normal`) and the same `azimuth 0° = north, 90° = east, clockwise` convention that
 * [SceneCamera.project] consumes.
 */
fun interface SkyProjector {

    fun horizontalOf(raDeg: Double, decDeg: Double): HorizontalCoords
}

/**
 * One star catalogue in the form the star layer draws from: parallel primitive arrays, pre-filtered by
 * magnitude, sorted brightest-first so a cap drops the faintest stars rather than an arbitrary set.
 *
 * Nothing here is derived per frame; the geometry is cached and only the projection runs per frame.
 */
class StarField private constructor(
    val count: Int,
    val raDeg: DoubleArray,
    val decDeg: DoubleArray,
    val magnitude: DoubleArray,
    /**
     * The catalogue's opaque colour bucket per star.
     *
     * **Kept, and deliberately not used.** No bucket → tint table exists anywhere in this slot's Kotlin
     * sources, so mapping a bucket to a temperature would be an invented table. Every star is drawn in
     * the palette's `onSky` instead, and the missing per-star tint is reported as an unsatisfied
     * requirement rather than faked. The array stays so the layer is ready when the table lands.
     */
    val colourBucket: IntArray,
    val magnitudeLimit: Double,
) {
    companion object {

        /** The point field's drawn limit. */
        const val DEFAULT_MAGNITUDE_LIMIT: Double = 6.5

        /** The star-trail mode's own limit; the arrow's set is brighter than the point field's. */
        const val TRAIL_MAGNITUDE_LIMIT: Double = 6.0

        /** Builds a field from the bundled table, dropping anything fainter than [magnitudeLimit]. */
        fun of(table: StarTable, magnitudeLimit: Double = DEFAULT_MAGNITUDE_LIMIT): StarField {
            val kept = table.stars.filter { it.magnitudeV.toDouble() <= magnitudeLimit }
                .sortedBy { it.magnitudeV.toDouble() }
            val n = kept.size
            val ra = DoubleArray(n)
            val dec = DoubleArray(n)
            val mag = DoubleArray(n)
            val bucket = IntArray(n)
            for (i in 0 until n) {
                val s = kept[i]
                ra[i] = s.raDeg
                dec[i] = s.decDeg
                mag[i] = s.magnitudeV.toDouble()
                bucket[i] = s.colourBucket
            }
            return StarField(n, ra, dec, mag, bucket, magnitudeLimit)
        }

        /** An empty field: the star table is absent, so no star is invented. */
        fun empty(): StarField =
            StarField(0, DoubleArray(0), DoubleArray(0), DoubleArray(0), IntArray(0), 0.0)
    }
}

/**
 * Everything the canvas draws with that is **not** a per-instant fact.
 *
 * Built once by the caller and handed to [SceneCanvas]; nothing in here is read from a clock, a
 * location or a network at draw time. Every field has an honest absent value, because the slot ships
 * none of the baked sky assets yet and a missing asset must degrade to a typed condition rather than to
 * a plausible invention — the scene capability's "a missing sky asset is refused, not invented".
 */
class SceneAssets(
    val lut: TwilightLut,
    val projector: SkyProjector,
    val starField: StarField? = null,
    val constellationFigures: ConstellationFigures? = null,
    val milkyWay: Bitmap? = null,
    val moonImage: Bitmap? = null,
    /** The same transform at another instant, in minutes from the frame's; `null` disables trail mode. */
    val projectorAt: ((Double) -> SkyProjector)? = null,
    /** The drawn magnitude limit; the memory-pressure path lowers this and rebuilds the cached field. */
    val magnitudeLimit: Double = StarField.DEFAULT_MAGNITUDE_LIMIT,
)

/**
 * The strings the labels drawn *inside* the scene rect need.
 *
 * Resolved with `stringResource` by the screen — so no user-facing prose is a literal in this file — and
 * pre-formatted in the cache build rather than in the draw lambda.
 */
class SceneLabels(
    val locale: Locale,
    /** Receives the target's name; e.g. `"Target %1$s"`. */
    val targetLabelFormat: String,
    /** Receives the element set's age; e.g. `"elements %1$s old"`. */
    val trailAgeFormat: String,
    /** Renders the element set's age in hours, resolved from resources by the screen. */
    val ageText: (hours: Double) -> String,
    /** Drawn under the lens frame when the target's apparent size is not in the catalogue. */
    val unknownSizeLabel: String,
    /** Drawn when the selected target is below the user's own ridge. */
    val belowRidgeLabel: String,
    /** The mandatory attribution, rendered with the band and never altered. */
    val esoCredit: String,
)

/**
 * Interaction state the frame deliberately does not carry, because none of it is an astronomical fact.
 *
 * It is a parameter and not an environment read, for the same reason as [SceneAssets]: the draw is a
 * pure function of its inputs, and on a fixed [SceneView] and [SceneFrame] the pixels repeat.
 */
class SceneView(
    /** A scrub, a play or any animation is running, so layer 4 re-derives its stochastic seeds. */
    val motionActive: Boolean = false,
    /** Star-trail mode: layer 4 draws arcs over the interval instead of points. */
    val trailMode: Boolean = false,
    /** The revealed boundary of a running transition in `0…1`, or `null` when none is running. */
    val wipeFraction: Float? = null,
    /** The star-trail interval, in minutes, centred on the frame's instant. */
    val trailIntervalMinutes: Double = 30.0,
    /** Cancelling the trail: the arc collapses toward its chord while the user scrubs. */
    val cancelTrail: Boolean = false,
    /** How far the cancellation has advanced, `0…1`; at 1 the arc is gone. */
    val cancelProgress: Double = 0.0,
    /**
     * Session scrub fraction `0…1`. Prep identity quantizes this to a strip stop so Play / drag
     * do not rebuild the sky on every animation millisecond.
     */
    val scrubValue: Double = 0.0,
)

/**
 * **The hero canvas: the 13 layers of the sky scene, in the order the capability fixes.**
 *
 * ## What is read, and where
 *
 * [frame] is a **lambda**, not a value. Layer prep (LUT strip, Milky Way mesh, star projection) runs on
 * [Dispatchers.Default] so the main thread only blits already-built buffers — the open-path freeze this
 * file used to cause was thousands of `SkyProjector` calls inside `drawWithCache` on the UI thread.
 *
 * ## The layer table, and where each layer is drawn
 *
 * | # | Layer | Drawn in |
 * |---|---|---|
 * | 1 | Sky field (LUT strip), **with the wedge inside it** | [drawSkyField] |
 * | 2 | Night floor | [drawNightFloor], additive |
 * | 3 | Milky Way composite | [drawMilkyWay], 48 × 24 mesh with per-vertex brightness |
 * | 4 | Star field | [drawStarField], ≤ 8 batched point calls |
 * | 5 | Deep-sky targets and constellation figures | [drawFiguresAndPlanets] |
 * | 6 | Moon | [drawMoon] |
 * | 7 | Satellite trails | [drawTrails] |
 * | 8 | Earth-shadow wedge | **inside layer 1, by construction** — see [TwilightLut.shadowEdgeDeg] |
 * | 9 | Alpenglow | [drawAlpenglow], additive, **before** the silhouette |
 * | 10 | Horizon silhouette | [drawHorizonSilhouette], opaque |
 * | 11 | Lens FOV frame | [drawLensFrame], unoccluded |
 * | 12 | Target marker then label | marker in [drawTargetMarker] (before 10), label in [drawLabels] |
 * | 13 | UI chrome | **not here** — `NightSceneScreen` draws it outside the scene rect |
 *
 * ## Non-negotiables this file implements
 *
 * - **Layer 8 has no draw call anywhere.** The wedge is a per-channel modulation of the radiance inside
 *   layer 1, so it cannot be painted over the star field: there is nothing to paint.
 * - **Azimuth increases to the right and is never mirrored.** `x` comes from the projected azimuth
 *   fraction; `LayoutDirection` is never consulted, so an RTL locale mirrors the chrome, not the sky.
 * - **Occlusion is by draw order only.** The silhouette is opaque and is painted after the Moon and the
 *   marker, so it cuts them instead of them being culled.
 * - **The draw lambda allocates nothing.** Every `Path`, `Paint`, `Bitmap`, `Matrix` and buffer lives in
 *   [RenderCache] and is reused; the per-frame work happens in the cache build, which the capability
 *   permits to allocate.
 * - **Idle draws no frames.** The canvas carries no ticker: it repaints only when its inputs change, and
 *   `NightSceneScreen` starts no clock while the scene is idle, so an untouched scene does not repaint.
 *
 * ## Honest limits, stated rather than hidden
 *
 * - **No AGSL shader path exists in this slot.** Layer 1 *is* the row-strip fallback — the path the
 *   capability says must be correct — so the two cannot drift, because there is only one. The API 33+
 *   half of that requirement is unimplemented and is reported.
 * - **The baked sky assets are absent**, so: no Milky Way band is drawn (the ESO credit is not painted
 *   under nothing), the star field is empty unless the caller supplies a table, the Moon carries phase
 *   geometry rather than the NASA SVS texture, and per-star colour is not drawn.
 * - **Frame time is not measured.** The ≤ 12 ms budget and the `dumpsys gfxinfo` acceptance are
 *   `UNVERIFIED` and are claimed nowhere.
 */
@Composable
fun SceneCanvas(
    frame: () -> SceneFrame,
    assets: SceneAssets,
    labels: SceneLabels,
    modifier: Modifier = Modifier,
    summary: String,
    view: SceneView = SceneView(),
) {
    val palette = LumintraTheme.colors
    val density = LocalDensity.current.density
    val context = LocalContext.current
    // Double-buffer: prep mutates the write buffer on Default; blit reads the display buffer on Main
    // with no shared lock — a single-cache synchronized prep was still an ANR (Main waited on the lock).
    val buffers = remember { arrayOf(RenderCache(), RenderCache()) }
    val trimGeneration by SceneMemoryPressure.trimGeneration.collectAsStateWithLifecycle()
    val frameRef = rememberUpdatedState(frame)
    val viewRef = rememberUpdatedState(view)

    var viewportW by remember { mutableFloatStateOf(0f) }
    var viewportH by remember { mutableFloatStateOf(0f) }
    var displayIndex by remember { mutableIntStateOf(-1) }
    var writeIndex by remember { mutableIntStateOf(0) }

    DisposableEffect(buffers) {
        SceneMemoryPressure.install(context.applicationContext)
        buffers.forEach { SceneMemoryPressure.register(it) }
        onDispose { buffers.forEach { SceneMemoryPressure.unregister(it) } }
    }

    LaunchedEffect(assets, labels, palette, density, trimGeneration, viewportW, viewportH) {
        if (viewportW <= 0f || viewportH <= 0f) return@LaunchedEffect
        snapshotFlow {
            val f = frameRef.value()
            val v = viewRef.value
            // Idle: 96 strip stops. During Play/scrub motion: 24 buckets so a 12s Play
            // does not schedule ~8 full star-field preps per second (that was the blink).
            val buckets = if (v.motionActive) 24 else TimelineStripModel.STOP_COUNT
            val scrubStop = round(
                v.scrubValue.coerceIn(0.0, 1.0) * (buckets - 1),
            ).toInt().coerceIn(0, buckets - 1)
            ScenePrepKey(
                scrubStop = scrubStop,
                redMode = f.redMode,
                selectedId = f.selectedTarget?.id,
                trailMode = v.trailMode,
                trailIntervalMinutes = v.trailIntervalMinutes,
                cancelTrail = v.cancelTrail,
                trimGeneration = trimGeneration,
                width = viewportW,
                height = viewportH,
            )
        }
            .distinctUntilChanged()
            // A slow Default prep finishes, then takes the latest key — never cancel mid-prep.
            .conflate()
            .collect { key ->
                val f = frameRef.value()
                val v = viewRef.value
                val targetIndex = writeIndex
                val holder = buffers[targetIndex]
                withContext(Dispatchers.Default) {
                    holder.forViewport(key.width, key.height, density, assets)
                    val plan = holder.plan
                    plan.trimGeneration = key.trimGeneration
                    plan.prepare(f, v, key.width, key.height)
                    layer01SkyField(holder, plan, assets, palette, f)
                    layer02NightFloor(holder, plan, palette, f)
                    layer03MilkyWay(holder, plan, assets, labels, f)
                    layer04StarField(holder, plan, assets, palette, f, v)
                    layer05FiguresAndPlanets(holder, plan, assets, labels, palette, f)
                    layer06Moon(holder, plan, assets, palette, f)
                    layer07Trails(holder, plan, labels, palette, f)
                    layer09Alpenglow(holder, plan, palette, f)
                    layer12TargetMarker(holder, plan, labels, palette, f)
                    layer10HorizonSilhouette(holder, plan, palette, f)
                    layer11LensFrame(holder, plan, palette)
                    layer12TargetLabel(holder, plan, labels, palette)
                }
                displayIndex = targetIndex
                writeIndex = 1 - targetIndex
            }
    }

    Box(
        modifier = modifier
            .semantics { contentDescription = summary }
            .onSizeChanged {
                viewportW = it.width.toFloat()
                viewportH = it.height.toFloat()
            }
            .drawBehind {
                val index = displayIndex
                if (index < 0) return@drawBehind
                val cache = buffers[index]
                val plan = cache.plan
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    blitSkyField(native, cache, plan)
                    blitNightFloor(native, cache, plan)
                    blitMilkyWay(native, cache, plan)
                    blitStarField(native, cache, plan)
                    blitFiguresAndPlanets(native, cache, plan)
                    blitMoon(native, cache, plan)
                    blitTrails(native, cache, plan)
                    blitAlpenglow(native, cache, plan)
                    blitTargetMarker(native, cache, plan)
                    blitHorizonSilhouette(native, cache, plan)
                    blitLensFrame(native, cache, plan)
                    blitLabels(native, cache, plan)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (displayIndex < 0) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = palette.firstLight,
            )
        }
    }
}

/**
 * Identity of one off-main prep.
 *
 * Time is [scrubStop] (bucketed), not epoch ms. Wipe / motion flags are deliberately absent —
 * they changed every animation frame and forced a star-field rebuild (the blink the owner saw).
 * [conflate] keeps a slow prep from being cancelled mid-flight.
 */
private data class ScenePrepKey(
    val scrubStop: Int,
    val redMode: Boolean,
    val selectedId: String?,
    val trailMode: Boolean,
    val trailIntervalMinutes: Double,
    val cancelTrail: Boolean,
    val trimGeneration: Int,
    val width: Float,
    val height: Float,
)

// ---------------------------------------------------------------------------------------------
// The reused surface: every allocation the draw lambda touches is created here, not in the draw.
// ---------------------------------------------------------------------------------------------

/** Per-frame values the draw phase reads. Mutated by [prepare] and by the layer preps, never by a draw. */
internal class ScenePlan {

    var frame: SceneFrame? = null
    var view: SceneView = SceneView()
    var width: Float = 0f
    var height: Float = 0f
    var density: Float = 1f
    /** Copied from [SceneMemoryPressure.trimGeneration] so a trim invalidates `drawWithCache`. */
    var trimGeneration: Int = 0

    // --- layer 1 -----------------------------------------------------------------------------
    /** The vertical span the strip covers, as fractions of the rect's height. */
    var skyBottomFraction: Float = 1f
    /** The outgoing frame's revealed boundary, or `NaN` when no transition is running. */
    var wipeFraction: Float = Float.NaN
    var hasSkyTable: Boolean = false

    // --- layer 2 -----------------------------------------------------------------------------
    var nightFloorAlpha: Int = 0

    // --- layer 3 -----------------------------------------------------------------------------
    var hasMilkyWay: Boolean = false

    // --- layer 4 -----------------------------------------------------------------------------
    var starMode: Int = STAR_MODE_POINTS
    var starLineValues: Int = 0

    // --- layer 6 -----------------------------------------------------------------------------
    var moonX: Float = Float.NaN
    var moonY: Float = Float.NaN
    var moonRadiusPx: Float = 0f
    var hasMoon: Boolean = false

    // --- layer 9 -----------------------------------------------------------------------------
    var alpenglowX: Float = Float.NaN
    var alpenglowY: Float = Float.NaN
    var alpenglowRadius: Float = 0f
    var alpenglowAlpha: Int = 0

    // --- layer 6 (draw-time handles) ---------------------------------------------------------
    var moonSurfaceImage: Bitmap? = null
    var moonLimbAngleDeg: Float = 0f

    // --- layer 10 ----------------------------------------------------------------------------
    var silhouetteDirty: Boolean = true

    // --- layer 11 ---------------------------------------------------------------------------
    var lensLeft: Float = 0f
    var lensTop: Float = 0f
    var lensRight: Float = 0f
    var lensBottom: Float = 0f

    // --- layer 12 ---------------------------------------------------------------------------
    var targetX: Float = Float.NaN
    var targetY: Float = Float.NaN
    var targetRadiusPx: Float = 0f
    var targetOccluded: Boolean = false
    var targetLabelX: Float = Float.NaN
    var targetLabelY: Float = Float.NaN
    var targetLabel: String? = null
    var esoCreditX: Float = 0f
    var esoCreditY: Float = 0f
    var esoCreditVisible: Boolean = false
    var esoCreditText: String? = null
    var belowRidgeLabelX: Float = 0f
    var belowRidgeLabelY: Float = 0f
    var belowRidgeText: String? = null
    var targetIsPointSource: Boolean = true

    fun prepare(frame: SceneFrame, view: SceneView, width: Float, height: Float) {
        val previous = this.frame
        val cameraChanged = previous == null || previous.camera != frame.camera
        val horizonChanged = previous == null || previous.horizon !== frame.horizon
        this.frame = frame
        this.view = view
        this.width = width
        this.height = height
        this.wipeFraction = view.wipeFraction ?: Float.NaN
        this.silhouetteDirty = cameraChanged || horizonChanged
        this.targetOccluded = frame.selectedTarget?.occludedByHorizon ?: false
    }

    companion object {
        const val STAR_MODE_POINTS: Int = 0
        const val STAR_MODE_TRAILS: Int = 1
    }
}

/**
 * Every reusable object the draw lambda touches, plus the per-frame buffers.
 *
 * The class is created once per composition (via `remember`) and its buffers are re-sized only when the
 * viewport changes, so a scrub reuses the same `Bitmap`, `Path` and `Paint` objects frame after frame.
 */
internal class RenderCache {

    val plan = ScenePlan()

    /** Guards prep on [Dispatchers.Default] against blit on the UI thread. */
    val lock = Any()

    // --- shared paints --------------------------------------------------------------------------
    val blitPaint: Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    val hairlinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    // --- the sky strip: 16 columns × H rows, stretched to the rect ---------------------------------
    var skyStrip: Bitmap? = null
    var skyStripAlt: Bitmap? = null
    var skyPixels: IntArray = IntArray(0)
    var skyPixelsAlt: IntArray = IntArray(0)
    var stripRows: Int = 0

    // --- layer 2 -------------------------------------------------------------------------------
    var nightFloorPaint: Paint = additivePaint()
    var nightFloorKey: Long = Long.MIN_VALUE
    var nightFloorLeft: Float = 0f
    var nightFloorTop: Float = 0f
    var nightFloorRight: Float = 0f
    var nightFloorBottom: Float = 0f

    // --- layer 3 -------------------------------------------------------------------------------
    var milkyWayBitmap: Bitmap? = null
    /** Bitmap the current [BitmapShader] was built from; null when the shader is unbound. */
    var meshShaderBitmap: Bitmap? = null
    val meshVerts: FloatArray = FloatArray(MESH_VERTEX_COUNT * 2)
    val meshTexCoords: FloatArray = FloatArray(MESH_VERTEX_COUNT * 2)
    val meshColours: IntArray = IntArray(MESH_VERTEX_COUNT)
    val meshPaint: Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    // --- layer 4 -------------------------------------------------------------------------------
    val starPoints: Array<FloatArray> = Array(STAR_GROUPS) { FloatArray(MAX_DRAWN_STARS * 2) }
    val starCounts: IntArray = IntArray(STAR_GROUPS)
    val starPointsY: Array<FloatArray> = Array(STAR_GROUPS) { FloatArray(MAX_DRAWN_STARS) }
    val starPointsX: Array<FloatArray> = Array(STAR_GROUPS) { FloatArray(MAX_DRAWN_STARS) }
    val starPaints: Array<Paint> = Array(STAR_GROUPS) { fillPaint() }
    val starTrailVertices: FloatArray = FloatArray(MAX_DRAWN_STARS * TRAIL_VERTICES * 4)

    // --- layer 5 -------------------------------------------------------------------------------
    var figuresKey: Long = Long.MIN_VALUE
    val figurePath: NativePath = NativePath()
    val figurePaint: Paint = strokePaint()
    val planetX: FloatArray = FloatArray(MAX_PLANETS)
    val planetY: FloatArray = FloatArray(MAX_PLANETS)
    val planetRadius: FloatArray = FloatArray(MAX_PLANETS)
    val planetVisible: BooleanArray = BooleanArray(MAX_PLANETS)
    val planetLabels: Array<String?> = arrayOfNulls(MAX_PLANETS)
    var planetCount: Int = 0
    val discPaint: Paint = fillPaint()
    val planetLabelPaint: Paint = textPaint()

    // --- layer 6 -------------------------------------------------------------------------------
    val moonLitPath: NativePath = NativePath()
    val moonSurfaceClip: NativePath = NativePath()
    val moonGlow: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    val moonSurface: Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    val moonDark: Paint = fillPaint()
    val moonRing: Paint = strokePaint()
    val moonMatrix: Matrix = Matrix()
    val moonSurfaceRect: RectF = RectF()

    // --- layer 7 -------------------------------------------------------------------------------
    /** One path per visibility class, so all the observable streaks cost one draw call. */
    val trailPaths: Array<NativePath> = Array(3) { NativePath() }
    val trailAlphas: IntArray = IntArray(3)
    val trailInnerX: ArrayList<Float> = ArrayList(MAX_TRAIL_POINTS)
    val trailInnerY: ArrayList<Float> = ArrayList(MAX_TRAIL_POINTS)
    val trailPaint: Paint = fillPaint()
    val trailAgeX: FloatArray = FloatArray(MAX_TRAILS)
    val trailAgeY: FloatArray = FloatArray(MAX_TRAILS)
    val trailAgeLabels: Array<String?> = arrayOfNulls(MAX_TRAILS)
    var trailAgeCount: Int = 0

    // --- layer 9 --------------------------------------------------------------------------------
    val glowPaint: Paint = additivePaint()

    // --- layer 10 -------------------------------------------------------------------------------
    val silhouettePath: NativePath = NativePath()
    val treePath: NativePath = NativePath()
    val silhouettePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    val treePaint: Paint = fillPaint()

    // --- layer 11 -------------------------------------------------------------------------------
    val framePaint: Paint = strokePaint()

    // --- layer 12 -------------------------------------------------------------------------------
    val markerRing: Paint = strokePaint()
    val markerFill: Paint = fillPaint()
    val labelPaint: Paint = textPaint()
    val creditPaint: Paint = textPaint()

    // --- scratch -------------------------------------------------------------------------------
    val scratch = DoubleArray(4)
    val scratchF = FloatArray(8)
    /** The drawn magnitude limit the last viewport was prepared with; see `forViewport`. */
    var drawnMagnitudeLimit: Double = StarField.DEFAULT_MAGNITUDE_LIMIT
    val dither: FloatArray = FloatArray(DITHER_ENTRIES) {
        ((((it * 1103515245) + 12345) ushr 16) and 0xFF) / 255f - 0.5f
    }

    fun forViewport(width: Float, height: Float, density: Float, assets: SceneAssets) {
        val rows = height.toInt().coerceAtLeast(1)
        plan.width = width
        plan.height = height
        plan.density = density
        // The strip and its buffers are re-allocated only when the viewport's height changes, so a
        // scrub, a camera move or a mode change reuses the same Bitmap and IntArray. On trim-memory
        // the strips are recycled to null and rebuilt here on the next frame.
        if (skyStrip == null || stripRows != rows) {
            skyStrip = Bitmap.createBitmap(STRIP_COLUMNS, rows, Bitmap.Config.ARGB_8888)
            skyStripAlt = Bitmap.createBitmap(STRIP_COLUMNS, rows, Bitmap.Config.ARGB_8888)
            skyPixels = IntArray(STRIP_COLUMNS * rows)
            skyPixelsAlt = IntArray(STRIP_COLUMNS * rows)
            stripRows = rows
        }
        drawnMagnitudeLimit = min(assets.magnitudeLimit, SceneMemoryPressure.magnitudeLimit)
    }

    /**
     * Drops the LUT raster strips and the Milky Way texture shader this cache owns.
     *
     * Called from [SceneMemoryPressure.onTrimMemory]. Does not recycle [milkyWayBitmap]: that
     * reference is borrowed from [SceneAssets] and remains owned by the session loader.
     */
    fun releaseLargeBitmaps() {
        synchronized(lock) {
            skyStrip?.takeIf { !it.isRecycled }?.recycle()
            skyStrip = null
            skyStripAlt?.takeIf { !it.isRecycled }?.recycle()
            skyStripAlt = null
            skyPixels = IntArray(0)
            skyPixelsAlt = IntArray(0)
            stripRows = 0
            meshPaint.shader = null
            meshShaderBitmap = null
            milkyWayBitmap = null
            drawnMagnitudeLimit = SceneMemoryPressure.magnitudeLimit
        }
    }

    companion object {
        const val STRIP_COLUMNS: Int = 16
        const val MESH_COLUMNS: Int = 48
        const val MESH_ROWS: Int = 24
        const val MESH_VERTEX_COUNT: Int = (MESH_COLUMNS + 1) * (MESH_ROWS + 1)
        const val STAR_GROUPS: Int = 8
        const val MAX_DRAWN_STARS: Int = 1200
        const val TRAIL_VERTICES: Int = 12
        const val MAX_PLANETS: Int = 12
        const val MAX_TRAILS: Int = 6
        const val MAX_TRAIL_POINTS: Int = 64
        const val DITHER_ENTRIES: Int = 256

        /**
         * Indexed triangulation of the 48 × 24 Milky Way mesh for [NativeCanvas.drawVertices].
         * Topology is fixed; only the screen-space verts and per-vertex colours change per frame.
         */
        val MESH_INDICES: ShortArray = ShortArray(MESH_COLUMNS * MESH_ROWS * 6).also { indices ->
            var write = 0
            val stride = MESH_COLUMNS + 1
            for (row in 0 until MESH_ROWS) {
                for (column in 0 until MESH_COLUMNS) {
                    val topLeft = (row * stride + column).toShort()
                    val topRight = (topLeft + 1).toShort()
                    val bottomLeft = (((row + 1) * stride) + column).toShort()
                    val bottomRight = (bottomLeft + 1).toShort()
                    indices[write++] = topLeft
                    indices[write++] = topRight
                    indices[write++] = bottomLeft
                    indices[write++] = topRight
                    indices[write++] = bottomRight
                    indices[write++] = bottomLeft
                }
            }
        }
    }
}

private fun additivePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    isDither = true
}

private fun fillPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    isDither = true
}

private fun strokePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 1f
}

private fun textPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    style = Paint.Style.FILL
}

// ---------------------------------------------------------------------------------------------
// Layer 1 — the sky field, with the Earth-shadow wedge applied inside it.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 1, cache half: evaluates the twilight table into a **16 × H strip** of ARGB pixels.
 *
 * **The wedge is applied here, not later.** `earthShadowFactor` modulates the radiance and
 * `applyEarthShadow`'s per-channel exponents attenuate red most and blue least, so layer 8's whole
 * requirement is satisfied by the numbers this function stores: there is no wedge draw call in the file
 * that could be painted over the star field.
 *
 * **Why a 16-cell-wide strip and not one column.** The strip is stretched across the canvas, and the
 * sky's only horizontal structure comes from the LUT's solar/antisolar blend, `w = (1 + cos Δaz)/2`.
 * With the default centring — the antisolar meridian, a 146° canvas — Δaz runs from ~107° to ~253°
 * across the rect, so `w` moves by ~0.35 of the antisolar-solar difference and the field visibly
 * brightens toward the Sun's side. One column would flatten that; 16 keeps it at ~8 600 samples per
 * rebuilt frame instead of 37 000.
 */
private fun layer01SkyField(
    cache: RenderCache,
    plan: ScenePlan,
    assets: SceneAssets,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    val camera = frame.camera
    plan.hasSkyTable = assets.lut.hasTable
    plan.skyBottomFraction = (
        (camera.topAltitudeDeg - 0.0) /
            (camera.topAltitudeDeg - camera.bottomAltitudeDeg).coerceAtLeast(1e-6)
        ).toFloat().coerceIn(0f, 1f)

    fillStrip(cache, cache.skyPixels, assets, palette, frame, altitudeOffsetDeg = 0.0, epsilonOffsetDeg = 0.0)
    cache.skyStrip?.setPixels(
        cache.skyPixels,
        0,
        RenderCache.STRIP_COLUMNS,
        0,
        0,
        RenderCache.STRIP_COLUMNS,
        cache.stripRows,
    )

    // The transition: the outgoing sky, one tick earlier, in its own buffer. The tick is a stated
    // approximation — ε + 2.0° and +0.75° of altitude is the same sky a few minutes before, not a
    // second frame computed from a second ephemeris call, because a transition runs inside one frame's
    // draw and the frame is the only astronomy the canvas is allowed to read.
    if (plan.wipeFraction.isFinite()) {
        fillStrip(
            cache,
            cache.skyPixelsAlt,
            assets,
            palette,
            frame,
            altitudeOffsetDeg = 0.75,
            epsilonOffsetDeg = 2.0,
        )
        cache.skyStripAlt?.setPixels(
            cache.skyPixelsAlt,
            0,
            RenderCache.STRIP_COLUMNS,
            0,
            0,
            RenderCache.STRIP_COLUMNS,
            cache.stripRows,
        )
    }
}

/** One strip fill at an optional shift, shared by the incoming and outgoing sky. */
private fun fillStrip(
    cache: RenderCache,
    pixels: IntArray,
    assets: SceneAssets,
    palette: LumintraColors,
    frame: SceneFrame,
    altitudeOffsetDeg: Double,
    epsilonOffsetDeg: Double,
) {
    val camera = frame.camera
    val rows = cache.stripRows
    val columns = RenderCache.STRIP_COLUMNS
    val top = camera.topAltitudeDeg + altitudeOffsetDeg
    val span = (camera.topAltitudeDeg - camera.bottomAltitudeDeg).coerceAtLeast(1e-6)
    val epsilon = frame.epsilonDeg + epsilonOffsetDeg
    val sunAzimuth = frame.sun.position.azimuthDeg
    val centreAzimuth = camera.centerAzimuthDeg
    val centreDelta = SceneCamera.wrap180(centreAzimuth - sunAzimuth)
    val shadowEdge = assets.lut.shadowEdgeDeg(epsilon)
    val tonemapped = assets.lut.hasTable
    val redMode = frame.redMode
    val lut = assets.lut
    val scratch = cache.scratchF
    val dither = cache.dither

    var index = 0
    for (row in 0 until rows) {
        val altitude = top - (row + 0.5) / rows * span
        val viewAltitude = altitude.coerceIn(0.0, 90.0)
        val ditherValue = dither[row and 0xFF]
        for (column in 0 until columns) {
            val columnFraction = (column + 0.5f) / columns
            val deltaAzimuth = centreDelta + (columnFraction - 0.5) * camera.fovH
            lut.sampleBlendedInto(epsilon, viewAltitude, deltaAzimuth, scratch, 0)
            val factor = earthShadowFactor(epsilon, altitude, centreAzimuth, sunAzimuth, shadowEdge)
            val r = scratch[0] * factor
            val g = scratch[1] * factor.pow(0.8f)
            val b = scratch[2] * factor.pow(0.55f)
            pixels[index++] = if (redMode) {
                redArgb(r, g, b, tonemapped, ditherValue, palette)
            } else {
                skyArgb(r, g, b, tonemapped, ditherValue)
            }
        }
    }
}

/**
 * Layer 1, draw half: one stretched blit, or two during a transition.
 *
 * [ScenePlan.wipeFraction] is the fraction of the rect's height, measured from the top, that still shows
 * the **outgoing** frame; the incoming frame fills below it. The boundary gets a hairline so the
 * transition reads as a wipe rather than as a seam.
 */
private fun blitSkyField(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    val strip = cache.skyStrip ?: return
    val width = plan.width
    val height = plan.height
    val rows = cache.stripRows
    if (!plan.wipeFraction.isFinite()) {
        native.drawBitmap(
            strip,
            Rect(0, 0, RenderCache.STRIP_COLUMNS, rows),
            RectF(0f, 0f, width, height),
            cache.blitPaint,
        )
        return
    }
    val outgoing = cache.skyStripAlt
    val outgoingRows = (rows * plan.wipeFraction.coerceIn(0f, 1f)).toInt().coerceIn(0, rows)
    val boundary = (outgoingRows.toFloat() / rows.coerceAtLeast(1)) * height
    if (outgoingRows > 0 && outgoing != null) {
        native.drawBitmap(
            outgoing,
            Rect(0, 0, RenderCache.STRIP_COLUMNS, outgoingRows),
            RectF(0f, 0f, width, boundary),
            cache.blitPaint,
        )
    }
    if (outgoingRows < rows) {
        native.drawBitmap(
            strip,
            Rect(0, outgoingRows, RenderCache.STRIP_COLUMNS, rows),
            RectF(0f, boundary, width, height),
            cache.blitPaint,
        )
    }
    native.drawLine(0f, boundary, width, boundary, cache.hairlinePaint)
}

// ---------------------------------------------------------------------------------------------
// Layer 2 — the night floor.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 2: an **additive vertical gradient** peaking on the horizon line, so the deep sky is not a flat
 * black and the ridge has something to sit on. Its amplitude is a function of the solar altitude alone
 * (zero while the Sun is above −8°), and it is drawn additively so it lifts the sky without lifting the
 * silhouette.
 */
private fun layer02NightFloor(
    cache: RenderCache,
    plan: ScenePlan,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    val height = plan.height
    val horizonY = plan.skyBottomFraction * height
    val darkness = 1.0 - smoothstep(-8.0, -16.0, frame.epsilonDeg)
    plan.nightFloorAlpha = (26.0 * darkness).toInt().coerceIn(0, 40)
    if (plan.nightFloorAlpha == 0) return

    val top = (horizonY - height * 0.55f).coerceAtLeast(0f)
    val bottom = (horizonY + height * 0.02f).coerceAtMost(height)
    if (bottom <= top) return
    val key = (top.toRawBits().toLong() shl 32) xor bottom.toRawBits().toLong()
    if (cache.nightFloorKey != key) {
        cache.nightFloorKey = key
        val peak = argb(palette.skyGlow, 255)
        val clear = argb(palette.skyGlow, 0)
        cache.nightFloorPaint.shader = LinearGradient(
            0f,
            top,
            0f,
            bottom,
            intArrayOf(clear, peak, clear),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    cache.nightFloorPaint.alpha = plan.nightFloorAlpha
    cache.nightFloorLeft = 0f
    cache.nightFloorTop = top
    cache.nightFloorRight = plan.width
    cache.nightFloorBottom = horizonY
}

private fun blitNightFloor(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (plan.nightFloorAlpha == 0) return
    native.drawRect(
        cache.nightFloorLeft,
        cache.nightFloorTop,
        cache.nightFloorRight,
        cache.nightFloorBottom,
        cache.nightFloorPaint,
    )
}

// ---------------------------------------------------------------------------------------------
// Layer 3 — the Milky Way composite.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 3, cache half: places a 48 × 24 textured mesh over the galactic plane.
 *
 * The mesh's vertices are **sky positions**, not screen positions: each vertex is a `(l, b)` pair on the
 * equirectangular panorama, taken through galactic → equatorial → horizontal → [SceneCamera.project].
 * The band's cross-section brightness rides on the mesh's per-vertex colours, which modulate the
 * [BitmapShader] (the framework form of Compose's `ImageShader`) at draw time: the two-peak profile
 * `0.6·exp(−((|b|+7)/11)²) + 0.4·exp(−(b/5)²)` gives the broad disc plus the thin plane, and the
 * atmosphere's extinction dims the whole band as one factor because near the horizon the band's own
 * differential extinction is small against the star field's.
 *
 * **The ESO credit is not painted under nothing:** with no bitmap this layer draws nothing at all, and
 * the screen renders the typed "band unavailable" condition instead. Painting `ESO/S. Brunier` beneath a
 * synthetic band would be a false attribution, which the capability forbids. When the bitmap *is*
 * present, [layer12TargetLabel] / [blitLabels] paint the credit from [SceneLabels.esoCredit] (the
 * `night_eso_credit` resource) beside the band.
 *
 * A vertex whose position is behind the viewer, or below the ridge, is given alpha 0 rather than being
 * moved: a quad spanning it feathers to nothing instead of tearing.
 */
private fun layer03MilkyWay(
    cache: RenderCache,
    plan: ScenePlan,
    assets: SceneAssets,
    labels: SceneLabels,
    frame: SceneFrame,
) {
    val map = assets.milkyWay?.takeIf { !it.isRecycled }
    plan.hasMilkyWay = map != null
    cache.milkyWayBitmap = map
    if (map == null || frame.milkyWayRotationRad.isNaN()) return

    val camera = frame.camera
    val width = plan.width
    val height = plan.height
    val columns = RenderCache.MESH_COLUMNS
    val rows = RenderCache.MESH_ROWS
    val verts = cache.meshVerts
    val texs = cache.meshTexCoords
    val colours = cache.meshColours
    val scratch = cache.scratch
    val bandAlpha = 0.42 * (1.0 - smoothstep(0.0, 6.0, frame.epsilonDeg))
    val centreX = width / 2f
    val centreY = height / 2f
    val mapWidth = map.width.toFloat()
    val mapHeight = map.height.toFloat()

    for (row in 0..rows) {
        val galacticLatitude = 90.0 - row / rows.toDouble() * 180.0
        val mel = 0.6 * exp(-((abs(galacticLatitude) + 7.0) / 11.0).pow(2.0)) +
            0.4 * exp(-(galacticLatitude / 5.0).pow(2.0))
        val v = row / rows.toFloat() * mapHeight
        for (column in 0..columns) {
            val galacticLongitude = column / columns.toDouble() * 360.0
            val vertex = row * (columns + 1) + column
            val u = column / columns.toFloat() * mapWidth
            texs[vertex * 2] = u
            texs[vertex * 2 + 1] = v
            galacticToEquatorial(galacticLongitude, galacticLatitude, scratch)
            val position = assets.projector.horizontalOf(scratch[0], scratch[1])
            val projected = camera.project(position)
            if (projected == null) {
                verts[vertex * 2] = centreX
                verts[vertex * 2 + 1] = centreY
                colours[vertex] = 0
                continue
            }
            verts[vertex * 2] = projected.xFraction * width
            verts[vertex * 2 + 1] = projected.yFraction * height
            val ridge = ridgeAt(frame.horizon, position.azimuthDeg)
            val above = smoothstep(ridge, ridge + 3.0, position.altitudeDeg)
            val alpha = (255.0 * bandAlpha * mel * above).toInt().coerceIn(0, 255)
            colours[vertex] = (alpha shl 24) or 0xFFFFFF
        }
    }
    if (cache.meshShaderBitmap !== map) {
        cache.meshPaint.shader = BitmapShader(map, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        cache.meshShaderBitmap = map
    }
    cache.meshPaint.alpha = 255
    // Read once here so the credit's presence is tied to the band's, not to a second flag.
    if (labels.esoCredit.isEmpty()) plan.hasMilkyWay = false
}

/**
 * Blits the Milky Way with [NativeCanvas.drawVertices] and a [BitmapShader] — the night-scene
 * capability's `drawVertices` + `ImageShader` path (BitmapShader is the framework ImageShader).
 */
private fun blitMilkyWay(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (!plan.hasMilkyWay) return
    val map = cache.milkyWayBitmap?.takeIf { !it.isRecycled } ?: return
    if (cache.meshPaint.shader == null || cache.meshShaderBitmap !== map) {
        cache.meshPaint.shader = BitmapShader(map, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        cache.meshShaderBitmap = map
    }
    native.drawVertices(
        NativeCanvas.VertexMode.TRIANGLES,
        RenderCache.MESH_VERTEX_COUNT,
        cache.meshVerts,
        0,
        cache.meshTexCoords,
        0,
        cache.meshColours,
        0,
        RenderCache.MESH_INDICES,
        0,
        RenderCache.MESH_INDICES.size,
        cache.meshPaint,
    )
}

// ---------------------------------------------------------------------------------------------
// Layer 4 — the star field.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 4, cache half: projects every catalogue star and batches it into one of **eight** magnitude
 * groups, so the whole field costs at most eight `drawPoints` calls.
 *
 * Per star: the projection, the ridge test (a star below the drawn ridge is dropped rather than drawn
 * under it), the air-mass extinction `10^(−0.4·β·(X−1))`, and — while the scene moves — a scintillation
 * term from a deterministic hash of `(index, instant)` so the same instant always flickers the same way.
 *
 * **Star-trail mode** replaces the points with arcs: [SceneView.trailMode] makes the layer sample the
 * star at [RenderCache.TRAIL_VERTICES] instants around the frame and emit line segments, cancelling the
 * arc toward its chord when the user scrubs (a long-exposure arc that slides across the screen reads as
 * the sky moving rather than as the user moving). All of it goes through one `drawLines` buffer.
 *
 * **Per-star colour is not drawn.** The catalogue's `colourBucket` is an opaque byte with no bucket →
 * tint table anywhere in this slot's sources, so every star is `palette.onSky`; mapping a bucket to a
 * temperature would be an invented table, and the gap is reported instead.
 */
private fun layer04StarField(
    cache: RenderCache,
    plan: ScenePlan,
    assets: SceneAssets,
    palette: LumintraColors,
    frame: SceneFrame,
    view: SceneView,
) {
    for (group in 0 until RenderCache.STAR_GROUPS) cache.starCounts[group] = 0
    plan.starLineValues = 0
    plan.starMode = if (view.trailMode && assets.projectorAt != null) {
        ScenePlan.STAR_MODE_TRAILS
    } else {
        ScenePlan.STAR_MODE_POINTS
    }

    val field = assets.starField ?: return
    if (field.count == 0) return

    val camera = frame.camera
    val width = plan.width
    val height = plan.height
    val density = plan.density
    val horizon = frame.horizon
    val limit = if (plan.starMode == ScenePlan.STAR_MODE_TRAILS) {
        min(StarField.TRAIL_MAGNITUDE_LIMIT, SceneMemoryPressure.magnitudeLimit)
    } else {
        min(field.magnitudeLimit, min(assets.magnitudeLimit, cache.drawnMagnitudeLimit))
    }
    val starTint = argb(palette.onSky, 255)
    val seed = frame.instantUtc.epochSecond
    val scintillation = if (view.motionActive) 0.22 else 0.07
    val drawn = min(field.count, RenderCache.MAX_DRAWN_STARS)

    // Point mode's group paints: size and alpha per magnitude group, both monotone in brightness.
    if (plan.starMode == ScenePlan.STAR_MODE_POINTS) {
        for (group in 0 until RenderCache.STAR_GROUPS) {
            val centre = groupCentreMagnitude(group)
            val weight = ((limit - centre) / (limit + 1.5)).coerceIn(0.0, 1.0)
            val paint = cache.starPaints[group]
            paint.color = starTint
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = (0.7 + 1.5 * weight).toFloat() * density
            paint.alpha = (150.0 + 105.0 * weight).toInt().coerceIn(0, 255)
        }
    }

    // Trail mode's projectors: TRAIL_VERTICES transforms, taken once instead of once per star.
    var trailProjectors: Array<SkyProjector>? = null
    if (plan.starMode == ScenePlan.STAR_MODE_TRAILS) {
        val at = assets.projectorAt
        if (at == null) {
            plan.starMode = ScenePlan.STAR_MODE_POINTS
        } else {
            val span = view.trailIntervalMinutes
            trailProjectors = Array(RenderCache.TRAIL_VERTICES) { k ->
                at.invoke(((k.toDouble() / (RenderCache.TRAIL_VERTICES - 1)) - 0.5) * span)
            }
        }
    }

    var lineValues = 0
    val lineBuffer = cache.starTrailVertices
    for (index in 0 until drawn) {
        val magnitude = field.magnitude[index]
        if (magnitude > limit) break
        val ra = field.raDeg[index]
        val dec = field.decDeg[index]
        val position = assets.projector.horizontalOf(ra, dec)
        val airMass = kastenYoung(position.altitudeDeg)
        val extinction = 10.0.pow(-0.4 * EXTINCTION_BETA * (airMass - 1.0))
        if (extinction < 0.06) continue

        if (plan.starMode == ScenePlan.STAR_MODE_POINTS) {
            val projected = camera.project(position) ?: continue
            if (position.altitudeDeg < ridgeAt(horizon, position.azimuthDeg)) continue
            val flicker = 1.0 + scintillation * (hashUnit(index, seed) - 0.5) * 2.0
            // Extinction and scintillation cannot modulate a batched group's alpha — the paint is shared
            // by every star in the bin — so they decide **membership**: a star whose surviving light
            // falls below the bin's floor is not drawn at all. The amplitude is therefore quantised to
            // the magnitude bin, which is the visible difference between this and a per-star draw, and
            // it is the reason 850 stars cost eight calls instead of 850.
            if (extinction * (0.6 + 0.4 * flicker) < 0.28) continue
            val jitterX = (hashUnit(index, seed + 1) - 0.5).toFloat() * scintillation * 1.4f
            val jitterY = (hashUnit(index, seed + 2) - 0.5).toFloat() * scintillation * 1.4f
            val group = magnitudeGroup(magnitude)
            val count = cache.starCounts[group]
            if (count >= RenderCache.MAX_DRAWN_STARS) continue
            val x = (projected.xFraction * width + jitterX).toFloat()
            val y = (projected.yFraction * height + jitterY).toFloat()
            cache.starPointsX[group][count] = x
            cache.starPointsY[group][count] = y
            cache.starPoints[group][count * 2] = x
            cache.starPoints[group][count * 2 + 1] = y
            cache.starCounts[group] = count + 1
            continue
        }

        val projectors = trailProjectors ?: continue
        var previousX = Float.NaN
        var previousY = Float.NaN
        for (k in 0 until RenderCache.TRAIL_VERTICES) {
            val at = projectors[k].horizontalOf(ra, dec)
            val projected = camera.project(at)
            if (projected == null || at.altitudeDeg < ridgeAt(horizon, at.azimuthDeg)) {
                previousX = Float.NaN
                previousY = Float.NaN
                continue
            }
            val x = projected.xFraction * width
            val y = projected.yFraction * height
            if (!previousX.isNaN() && lineValues + 4 <= lineBuffer.size) {
                lineBuffer[lineValues] = previousX
                lineBuffer[lineValues + 1] = previousY
                lineBuffer[lineValues + 2] = x
                lineBuffer[lineValues + 3] = y
                lineValues += 4
            }
            previousX = x
            previousY = y
        }
    }
    plan.starLineValues = lineValues
    if (plan.starMode == ScenePlan.STAR_MODE_TRAILS) {
        val paint = cache.starPaints[0]
        paint.color = starTint
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 0.9f * density
        paint.alpha = if (view.cancelTrail) (90 * (1.0 - view.cancelProgress)).toInt().coerceIn(0, 255) else 235
    }
}

private fun blitStarField(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (plan.starMode == ScenePlan.STAR_MODE_TRAILS) {
        if (plan.starLineValues == 0) return
        native.drawLines(cache.starTrailVertices, 0, plan.starLineValues, cache.starPaints[0])
        return
    }
    for (group in 0 until RenderCache.STAR_GROUPS) {
        val count = cache.starCounts[group]
        if (count == 0) continue
        native.drawPoints(cache.starPoints[group], 0, count * 2, cache.starPaints[group])
    }
}

// ---------------------------------------------------------------------------------------------
// Layer 5 — constellation figures, deep-sky targets and the planets.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 5, cache half: the IAU figure polylines and the planets.
 *
 * A polyline is **split** wherever a vertex projects to nothing or drops below the ridge, so a figure is
 * never drawn as a chord across the sky behind the viewer or through the hill. The figure path is
 * rebuilt only when the camera or the sky's rotation changes, which is what makes a scrub cheap.
 *
 * A planet is a **symbol, not a disc**: at a 146° canvas even Jupiter's ~40″ is ~0.02 px, so a radius
 * scaled from the apparent size would be thinner than a hairline. The symbol's size is a function of
 * magnitude; the planet's angular size is stated in the brief, never claimed by the scene.
 */
private fun layer05FiguresAndPlanets(
    cache: RenderCache,
    plan: ScenePlan,
    assets: SceneAssets,
    labels: SceneLabels,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    val camera = frame.camera
    val width = plan.width
    val height = plan.height
    val key = (frame.camera.hashCode().toLong() shl 32) xor
        (frame.skyRotationRad.toRawBits() and 0xFFFFFFFFL)
    if (cache.figuresKey != key) {
        cache.figuresKey = key
        val path = cache.figurePath
        path.rewind()
        val figures = assets.constellationFigures
        if (figures != null) {
            for (polylines in figures.polylinesByAbbrev.values) {
                for (polyline in polylines) {
                    var started = false
                    for (vertex in polyline) {
                        val position = assets.projector.horizontalOf(vertex.raHours * 15.0, vertex.decDeg)
                        val projected = camera.project(position)
                        if (projected == null ||
                            position.altitudeDeg < ridgeAt(frame.horizon, position.azimuthDeg)
                        ) {
                            started = false
                            continue
                        }
                        val x = projected.xFraction * width
                        val y = projected.yFraction * height
                        if (started) path.lineTo(x, y) else path.moveTo(x, y)
                        started = true
                    }
                }
            }
        }
    }
    cache.figurePaint.color = argb(palette.hairline, 150)
    cache.figurePaint.strokeWidth = 1f

    var count = 0
    for (body in frame.planetBodies) {
        if (count >= RenderCache.MAX_PLANETS) break
        val projected = camera.project(body.position)
        if (projected == null) continue
        if (body.position.altitudeDeg < ridgeAt(frame.horizon, body.position.azimuthDeg)) continue
        val magnitude = body.magnitude ?: 1.0
        cache.planetX[count] = projected.xFraction * width
        cache.planetY[count] = projected.yFraction * height
        cache.planetRadius[count] = (
            2.0 + 1.6 * ((4.0 - magnitude).coerceIn(0.0, 4.0) / 4.0)
            ).toFloat() * plan.density
        cache.planetVisible[count] = true
        cache.planetLabels[count] = body.name
        count++
    }
    cache.planetCount = count
    cache.discPaint.color = argb(palette.firstLight, 235)
    cache.planetLabelPaint.color = argb(palette.onSkyMuted, 235)
    cache.planetLabelPaint.textSize = 10f * plan.density
}

private fun blitFiguresAndPlanets(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    native.drawPath(cache.figurePath, cache.figurePaint)
    for (index in 0 until cache.planetCount) {
        if (!cache.planetVisible[index]) continue
        native.drawCircle(
            cache.planetX[index],
            cache.planetY[index],
            cache.planetRadius[index],
            cache.discPaint,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Layer 6 — the Moon.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 6, cache half: the disc, the libration offset and the phase clip.
 *
 * **The phase is geometry, not a graded texture.** The terminator's projection is the ellipse through
 * the disc's poles whose semi-minor axis is `r·|1 − 2f|`, and the lit region's area is exactly
 * `(1 − |1 − 2f|)/2` — the illuminated fraction the brief states, by construction rather than by eye. The
 * lit region is a **crescent** (`f < 0.5`) when the terminator's arc is traversed through `+x`, and a
 * gibbous shape when it is traversed through `−x`; both end on the limb's poles, so the path closes
 * exactly on the disc.
 *
 * **The bright limb faces the Sun**, which is the only direction the frame gives: the position angle is
 * `atan2` of the Sun's projected offset from the Moon. A fraction with no direction would leave the
 * crescent on an arbitrary side.
 *
 * The disc is **cut by the ridge, not culled by it** (`project` clamps an altitude below the rect onto
 * its lower edge), and the cut is draw order: layer 10's opaque silhouette is painted after this layer.
 * Libration is a first-order texture offset, stated as an approximation — the full three-axis
 * projection of the SVS texture is not implemented.
 */
private fun layer06Moon(
    cache: RenderCache,
    plan: ScenePlan,
    assets: SceneAssets,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    val moon = frame.moon
    val camera = frame.camera
    val projected = camera.project(moon.position)
    if (projected == null) {
        plan.hasMoon = false
        return
    }

    val width = plan.width
    val height = plan.height
    val density = plan.density
    val radius = (
        (moon.angularSizeDeg ?: DEFAULT_MOON_SIZE_DEG) / camera.fovV * height / 2.0
        ).toFloat().coerceAtLeast(5f * density)
    val centreX = projected.xFraction * width
    val centreY = projected.yFraction * height
    plan.hasMoon = true
    plan.moonX = centreX
    plan.moonY = centreY
    plan.moonRadiusPx = radius

    val fraction = (moon.illuminatedFraction ?: 0.5).toDouble().coerceIn(0.0, 1.0)
    val brightDeltaAzimuth = SceneCamera.wrap180(
        frame.sun.position.azimuthDeg - moon.position.azimuthDeg,
    ) * cos(Math.toRadians(moon.position.altitudeDeg))
    val brightDeltaAltitude = frame.sun.position.altitudeDeg - moon.position.altitudeDeg
    val limbAngleDeg = Math.toDegrees(atan2(-brightDeltaAltitude, brightDeltaAzimuth)).toFloat()

    val minor = radius * abs(1f - 2f * fraction.toFloat())
    val path = cache.moonLitPath
    path.rewind()
    path.arcTo(RectF(-radius, -radius, radius, radius), -90f, 180f, true)
    if (fraction < 0.5) {
        path.arcTo(RectF(-minor, -radius, minor, radius), 90f, -180f, false)
    } else {
        path.arcTo(RectF(-minor, -radius, minor, radius), 90f, 180f, false)
    }
    path.close()

    // Glow: additive, widest when the Moon is brightest, and behind the disc so it never tints it.
    val glowAlpha = (110.0 * (0.25 + 0.75 * fraction)).toInt().coerceIn(0, 120)
    if (glowAlpha > 0) {
        cache.moonGlow.shader = RadialGradient(
            centreX,
            centreY,
            radius * 4.2f,
            intArrayOf(argb(palette.moonlight, glowAlpha), argb(palette.moonlight, 0)),
            floatArrayOf(0.18f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    cache.moonGlow.alpha = glowAlpha

    val librationLongitude = (frame.moonLibration?.librationLonDeg ?: 0.0) / 90.0
    val librationLatitude = (frame.moonLibration?.librationLatDeg ?: 0.0) / 90.0
    val offsetX = (librationLongitude * radius * 0.35).toFloat()
    val offsetY = (-librationLatitude * radius * 0.35).toFloat()
    cache.moonSurfaceRect.set(
        -radius + offsetX,
        -radius + offsetY,
        radius + offsetX,
        radius + offsetY,
    )
    cache.moonMatrix.setRotate(limbAngleDeg)
    cache.moonDark.color = argb(palette.moonShadow, 255)
    cache.moonSurface.color = argb(palette.moonLit, 255)
    cache.moonRing.color = argb(palette.hairline, 200)
    cache.moonSurface.setAlpha(255)
    cache.moonRing.strokeWidth = 1f
    plan.moonSurfaceImage = assets.moonImage
    plan.moonLimbAngleDeg = limbAngleDeg
}

private fun blitMoon(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (!plan.hasMoon) return
    native.save()
    native.translate(plan.moonX, plan.moonY)
    if (cache.moonGlow.alpha > 0) {
        native.drawCircle(0f, 0f, plan.moonRadiusPx * 4.2f, cache.moonGlow)
    }
    native.rotate(plan.moonLimbAngleDeg)
    native.drawCircle(0f, 0f, plan.moonRadiusPx, cache.moonDark)
    native.save()
    native.clipPath(cache.moonLitPath)
    val image = plan.moonSurfaceImage
    if (image != null) {
        native.drawBitmap(image, null, cache.moonSurfaceRect, cache.moonSurface)
    } else {
        native.drawPath(cache.moonLitPath, cache.moonSurface)
    }
    native.restore()
    native.drawCircle(0f, 0f, plan.moonRadiusPx, cache.moonRing)
    native.restore()
}
// LAYER-INSERTION-POINT-B

// ---------------------------------------------------------------------------------------------
// Layer 7 — satellite trails.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 7, cache half: the pass trails, as tapered ribbons grouped by visibility class.
 *
 * A trail is a **streak, not a line**: the ribbon's half-width tapers from nothing at the tail to its
 * full width at the head, which is the only thing in the scene that says which way the satellite is
 * going. The recent window is the last [TRAIL_SECONDS] of the pass, so a trail drawn at the current
 * scrub never claims a position the satellite will not reach.
 *
 * **A non-observable trail is dimmed and labelled, never dropped** — a pass inside the Earth's shadow is
 * exactly the thing the user wants to be told about — and a trail whose element set is not fresh carries
 * its age *beside the streak* (the capability's own requirement), because an age badge in a list three
 * screens away is not where the reading happens.
 *
 * All trails of one visibility class go into one `Path` and are drawn with one call, which is why the
 * cache holds three paths rather than one path per pass.
 */
private fun layer07Trails(
    cache: RenderCache,
    plan: ScenePlan,
    labels: SceneLabels,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    cache.trailAgeCount = 0
    for (path in cache.trailPaths) path.rewind()
    for (alphas in cache.trailAlphas.indices) {
        cache.trailAlphas[alphas] = 0
    }
    val width = plan.width
    val height = plan.height
    val density = plan.density
    val centreAzimuth = frame.camera.centerAzimuthDeg

    var trailIndex = 0
    for (trail in frame.trails) {
        if (trailIndex >= RenderCache.MAX_TRAILS) break
        val classIndex = trail.visibility.ordinal.coerceIn(0, cache.trailPaths.size - 1)
        val path = cache.trailPaths[classIndex]
        cache.trailAlphas[classIndex] = visibilityAlpha(trail.visibility)
        var headX = Float.NaN
        var headY = Float.NaN

        for (segment in trail.segments) {
            val points = ArrayList<SceneTrailPoint>(segment.size)
            for (point in segment) {
                val age = java.time.Duration.between(point.instantUtc, frame.instantUtc).abs()
                if (age.seconds > TRAIL_SECONDS) continue
                points.add(point)
            }
            if (points.size < 2) continue
            var previousX = Float.NaN
            var previousY = Float.NaN
            var previousNormalX = 0f
            var previousNormalY = 0f
            val count = points.size
            var emitted = 0
            for (index in 0 until count) {
                val position = points[index].position
                val projected = frame.camera.project(position) ?: continue
                if (position.altitudeDeg < ridgeAt(frame.horizon, position.azimuthDeg)) continue
                val x = projected.xFraction * width
                val y = projected.yFraction * height
                var normalX = previousNormalX
                var normalY = previousNormalY
                if (!previousX.isNaN()) {
                    val dx = x - previousX
                    val dy = y - previousY
                    val length = sqrt(dx * dx + dy * dy)
                    if (length > 0.01f) {
                        normalX = -dy / length
                        normalY = dx / length
                    }
                }
                val taper = 0.18f + 0.82f * ((index.toFloat() / (count - 1).coerceAtLeast(1)))
                val halfWidth = 1.7f * density * taper
                val outerX = x + normalX * halfWidth
                val outerY = y + normalY * halfWidth
                val innerX = x - normalX * halfWidth
                val innerY = y - normalY * halfWidth
                if (emitted == 0) {
                    path.moveTo(outerX, outerY)
                    cache.trailInnerX.clear()
                    cache.trailInnerY.clear()
                } else {
                    path.lineTo(outerX, outerY)
                }
                cache.trailInnerX.add(innerX)
                cache.trailInnerY.add(innerY)
                previousX = x
                previousY = y
                previousNormalX = normalX
                previousNormalY = normalY
                headX = x
                headY = y
                emitted++
            }
            for (index in cache.trailInnerX.size - 1 downTo 0) {
                path.lineTo(cache.trailInnerX[index], cache.trailInnerY[index])
            }
            if (emitted > 0) path.close()
        }

        // The age badge travels with the streak, at its head, on whichever side of the meridian it is.
        if (trail.mustStateAge && !headX.isNaN() && cache.trailAgeCount < RenderCache.MAX_TRAILS) {
            val right = SceneCamera.wrap180(headX / width * frame.camera.fovH + centreAzimuth) >= 0.0
            cache.trailAgeX[cache.trailAgeCount] = headX + if (right) 6f * density else -6f * density
            cache.trailAgeY[cache.trailAgeCount] = headY - 5f * density
            cache.trailAgeLabels[cache.trailAgeCount] = String.format(
                labels.locale,
                labels.trailAgeFormat,
                labels.ageText(trail.elementSetAge.ageHours),
            )
            cache.trailAgeCount++
        }
        trailIndex++
    }

    cache.trailPaint.color = argb(palette.onSkyMuted, 235)
    for (index in cache.trailPaths.indices) {
        cache.trailPaint.alpha = cache.trailAlphas[index]
    }
}

private fun blitTrails(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    for (index in cache.trailPaths.indices) {
        val alpha = cache.trailAlphas[index]
        if (alpha == 0 || cache.trailPaths[index].isEmpty) continue
        cache.trailPaint.alpha = alpha
        native.drawPath(cache.trailPaths[index], cache.trailPaint)
    }
}

// ---------------------------------------------------------------------------------------------
// Layer 9 — the alpenglow band.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 9: a warm **additive** band centred on the Sun's azimuth, drawn *before* the silhouette so it
 * feathers over the ridge instead of flooding it.
 *
 * If the Sun is not inside the rect its projected position is `null` and the band is **not drawn at
 * all** — which is the correct reading: on the antisolar meridian, where the scene opens by default,
 * there is nothing to glow there, and a band drawn at the rect's edge would be a bright ridge that the
 * capability explicitly forbids. When the Sun is in the rect but below its lower edge, `project` clamps
 * it onto that edge, so the glow sits at the bottom with its centre at the Sun's true altitude.
 */
private fun layer09Alpenglow(
    cache: RenderCache,
    plan: ScenePlan,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    plan.alpenglowAlpha = 0
    val projected = frame.camera.project(frame.sun.position) ?: return
    plan.alpenglowX = projected.xFraction * plan.width
    plan.alpenglowY = projected.yFraction * plan.height
    plan.alpenglowRadius = plan.height * 1.35f
    val alpha = (150.0 * smoothstep(-9.0, -0.5, frame.epsilonDeg)).toInt().coerceIn(0, 160)
    plan.alpenglowAlpha = alpha
    if (alpha == 0) return
    cache.glowPaint.shader = RadialGradient(
        plan.alpenglowX,
        plan.alpenglowY,
        plan.alpenglowRadius,
        intArrayOf(argb(palette.firstLight, 255), argb(palette.firstLight, 0)),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    cache.glowPaint.alpha = alpha
}

private fun blitAlpenglow(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (plan.alpenglowAlpha == 0) return
    native.drawRect(0f, 0f, plan.width, plan.height, cache.glowPaint)
}

// ---------------------------------------------------------------------------------------------
// Layer 10 — the horizon silhouette.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 10, cache half: the opaque ridge, from [HorizonProfile.altitudeDeg]'s 360 samples.
 *
 * The silhouette is drawn from the profile **alone** — no epoch shift — because the ridge belongs to the
 * place and not to the instant, while the *occlusion test* that drops stars and the Moon uses the same
 * profile plus the shift the scene's own parallax implies. The path is rebuilt only when the camera or
 * the profile changes, never on a scrub. Its fill is opaque and darker than the sky, which is what makes
 * "occlusion by draw order" true: the Moon and the target marker painted before it are cut, not culled.
 *
 * Trees are **derived, not invented**: a glyph is drawn only where the profile rises
 * [TREE_MIN_PROMINENCE_DEG] above its own smoothed baseline, so a hill is a hill and an obstruction that
 * is actually a treeline reads as one. The glyph is slot-owned line art in the UI stroke weight — the
 * capability's own allowance for a domain noun the icon set cannot express.
 */
private fun layer10HorizonSilhouette(
    cache: RenderCache,
    plan: ScenePlan,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    if (!plan.silhouetteDirty) return
    plan.silhouetteDirty = false
    val camera = frame.camera
    val width = plan.width
    val height = plan.height
    val profile = frame.horizon
    val path = cache.silhouettePath
    val treePath = cache.treePath
    path.rewind()
    treePath.rewind()

    val samples = SILHOUETTE_SAMPLES
    var ridgeTop = height
    var lastY = height
    var started = false
    val altitudes = FloatArray(samples + 1)
    for (index in 0..samples) {
        val azimuth = camera.centerAzimuthDeg - camera.fovH / 2.0 +
            index.toDouble() / samples * camera.fovH
        altitudes[index] = (profile?.altitudeAt(azimuth) ?: 0.0).toFloat()
    }
    for (index in 0..samples) {
        val azimuth = camera.centerAzimuthDeg - camera.fovH / 2.0 +
            index.toDouble() / samples * camera.fovH
        val position = HorizontalCoords(altitudeDeg = altitudes[index].toDouble(), azimuthDeg = azimuth)
        val projected = camera.project(position) ?: continue
        val x = projected.xFraction * width
        val y = projected.yFraction * height
        if (!started) {
            path.moveTo(x, height + SILHOUETTE_OVERSHOOT)
            started = true
        }
        path.lineTo(x, y)
        if (y < ridgeTop) ridgeTop = y
        lastY = y
    }
    if (started) {
        path.lineTo(width + SILHOUETTE_OVERSHOOT, lastY)
        path.lineTo(width + SILHOUETTE_OVERSHOOT, height + SILHOUETTE_OVERSHOOT)
        path.lineTo(-SILHOUETTE_OVERSHOOT, height + SILHOUETTE_OVERSHOOT)
        path.close()
    }

    // The trees: prominence over a smoothed baseline, at least 3 samples wide, at most every 12th.
    val baselineWindow = 9
    var lastTree = -baselineWindow
    for (index in 0..samples) {
        if (index - lastTree < 12) continue
        var sum = 0f
        var n = 0
        for (k in -baselineWindow..baselineWindow) {
            val j = index + k
            if (j in 0..samples) {
                sum += altitudes[j]
                n++
            }
        }
        if (n == 0) continue
        val prominence = altitudes[index] - sum / n
        if (prominence < TREE_MIN_PROMINENCE_DEG) continue
        val azimuth = camera.centerAzimuthDeg - camera.fovH / 2.0 +
            index.toDouble() / samples * camera.fovH
        val position = HorizontalCoords(altitudeDeg = altitudes[index].toDouble(), azimuthDeg = azimuth)
        val projected = camera.project(position) ?: continue
        val x = projected.xFraction * width
        val y = projected.yFraction * height
        val spread = 3.2f * plan.density
        val glyphHeight = min(3.4f * plan.density * (prominence / TREE_MIN_PROMINENCE_DEG), 9f * plan.density)
        treePath.moveTo(x - spread, y + 2f)
        treePath.lineTo(x, y - glyphHeight)
        treePath.lineTo(x + spread, y + 2f)
        treePath.close()
        treePath.moveTo(x - spread * 0.68f, y - glyphHeight * 0.35f)
        treePath.lineTo(x, y - glyphHeight * 1.18f)
        treePath.lineTo(x + spread * 0.68f, y - glyphHeight * 0.35f)
        treePath.close()
        lastTree = index
    }

    val ridgeGradient = LinearGradient(
        0f,
        ridgeTop.coerceAtLeast(0f),
        0f,
        height,
        intArrayOf(argb(nightShade(palette.skyHorizon), 255), argb(nightShade(palette.skyZenith), 255)),
        null,
        Shader.TileMode.CLAMP,
    )
    cache.silhouettePaint.shader = ridgeGradient
    cache.silhouettePaint.alpha = 255
    cache.treePaint.color = argb(nightShade(palette.skyZenith), 255)
    cache.treePaint.alpha = 255
}

private fun blitHorizonSilhouette(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (cache.silhouettePath.isEmpty) return
    native.drawPath(cache.silhouettePath, cache.silhouettePaint)
    native.drawPath(cache.treePath, cache.treePaint)
}

// ---------------------------------------------------------------------------------------------
// Layer 11 — the lens field-of-view frame.
// ---------------------------------------------------------------------------------------------

/**
 * Layer 11: the hairline rectangle the lens actually covers, inside the 2 × canvas, plus its corner
 * ticks.
 *
 * It is computed by **projecting the lens's own corners**, not by insetting the rect: the canvas is twice
 * the lens field only while the camera is unclamped, and at the zenith clamp the lens window is not
 * centred in the canvas. Projecting is the only version of this that stays correct when the camera is.
 *
 * A named control returns the camera to the default centring (`SceneCamera.of` with no target); the frame
 * is never recentred from inside the draw.
 */
private fun layer11LensFrame(
    cache: RenderCache,
    plan: ScenePlan,
    palette: LumintraColors,
) {
    val frame = plan.frame ?: return
    val camera = frame.camera
    val lens = frame.lensFov
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    var any = false
    for (azimuthSign in -1..1 step 2) {
        for (altitudeSign in -1..1 step 2) {
            val position = HorizontalCoords(
                altitudeDeg = camera.centerAltitudeDeg + altitudeSign * lens.fovV / 2.0,
                azimuthDeg = camera.centerAzimuthDeg + azimuthSign * lens.fovH / 2.0,
            )
            val projected = camera.project(position) ?: continue
            val x = projected.xFraction * plan.width
            val y = projected.yFraction * plan.height
            left = min(left, x)
            right = max(right, x)
            top = min(top, y)
            bottom = max(bottom, y)
            any = true
        }
    }
    if (!any) {
        plan.lensLeft = Float.NaN
        return
    }
    plan.lensLeft = left
    plan.lensTop = top
    plan.lensRight = right
    plan.lensBottom = bottom
    cache.framePaint.color = argb(palette.hairline, 190)
    cache.framePaint.strokeWidth = max(1f, plan.density * 0.8f)
}

private fun blitLensFrame(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (plan.lensLeft.isNaN()) return
    val tick = 7f * plan.density
    native.drawRect(plan.lensLeft, plan.lensTop, plan.lensRight, plan.lensBottom, cache.framePaint)
    val corners = floatArrayOf(
        plan.lensLeft, plan.lensTop,
        plan.lensRight, plan.lensTop,
        plan.lensRight, plan.lensBottom,
        plan.lensLeft, plan.lensBottom,
    )
    var index = 0
    while (index < corners.size) {
        val x = corners[index]
        val y = corners[index + 1]
        val horizontal = if (index == 0 || index == 6) 1f else -1f
        val vertical = if (index == 0 || index == 2) 1f else -1f
        native.drawLine(x, y, x + horizontal * tick, y, cache.framePaint)
        native.drawLine(x, y, x, y + vertical * tick, cache.framePaint)
        index += 2
    }
}

// ---------------------------------------------------------------------------------------------
// Layer 12 — the target marker (before the ridge) and its label (after).
// ---------------------------------------------------------------------------------------------

/**
 * Layer 12, first half: the marker, drawn **before** the silhouette.
 *
 * That order is the whole point: a target that has set behind the user's own ridge must be *occluded* by
 * it, not drawn through it, and the marker therefore draws without knowing about the ridge at all. The
 * ring is the target's apparent size at the canvas's scale — half the size it occupies on the sensor,
 * because the canvas is twice the lens field — floored at a readable minimum so a point source is still
 * a marker, and the ticks are at a fixed multiple of it.
 *
 * A target whose position is outside the canvas horizontally projects to `null` and draws nothing here;
 * that is the screen's typed off-rect indicator, not a marker clamped onto an edge.
 */
private fun layer12TargetMarker(
    cache: RenderCache,
    plan: ScenePlan,
    labels: SceneLabels,
    palette: LumintraColors,
    frame: SceneFrame,
) {
    val target = frame.selectedTarget
    plan.targetX = Float.NaN
    plan.targetLabel = null
    cache.markerRing.color = argb(palette.firstLight, 235)
    cache.markerRing.strokeWidth = max(1.2f, plan.density * 1.1f)
    cache.markerFill.color = argb(palette.firstLight, 210)
    if (target == null) return
    plan.targetLabel = String.format(labels.locale, labels.targetLabelFormat, target.name)
    plan.targetIsPointSource = target.isPointSource
    val projected = frame.camera.project(target.position) ?: return
    val x = projected.xFraction * plan.width
    val y = projected.yFraction * plan.height
    val angular = target.angularSizeDeg ?: 0.0
    val radius = max(
        9f * plan.density,
        (angular / frame.camera.fovV * plan.height / 2.0).toFloat(),
    )
    plan.targetX = x
    plan.targetY = y
    plan.targetRadiusPx = radius
}

private fun blitTargetMarker(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (plan.targetX.isNaN()) return
    val radius = plan.targetRadiusPx
    native.drawCircle(plan.targetX, plan.targetY, radius, cache.markerRing)
    native.drawCircle(plan.targetX, plan.targetY, max(1.4f, plan.density), cache.markerFill)
    val tick = radius * 1.55f
    val arm = radius * 0.45f
    native.drawLine(plan.targetX - tick, plan.targetY - tick, plan.targetX - tick + arm, plan.targetY - tick, cache.markerRing)
    native.drawLine(plan.targetX - tick, plan.targetY - tick, plan.targetX - tick, plan.targetY - tick + arm, cache.markerRing)
    native.drawLine(plan.targetX + tick, plan.targetY - tick, plan.targetX + tick - arm, plan.targetY - tick, cache.markerRing)
    native.drawLine(plan.targetX + tick, plan.targetY - tick, plan.targetX + tick, plan.targetY - tick + arm, cache.markerRing)
    native.drawLine(plan.targetX - tick, plan.targetY + tick, plan.targetX - tick + arm, plan.targetY + tick, cache.markerRing)
    native.drawLine(plan.targetX - tick, plan.targetY + tick, plan.targetX - tick, plan.targetY + tick - arm, cache.markerRing)
    native.drawLine(plan.targetX + tick, plan.targetY + tick, plan.targetX + tick - arm, plan.targetY + tick, cache.markerRing)
    native.drawLine(plan.targetX + tick, plan.targetY + tick, plan.targetX + tick, plan.targetY + tick - arm, cache.markerRing)
}

/**
 * Layer 12, second half, and the label plane: the target's label, the planets' names, every trail's age
 * badge and the band's attribution — all drawn **after** the silhouette, because a label the ridge can
 * eat is a label the user cannot read. The plane is inside the canvas on purpose: labels are part of the
 * scene's own drawing and are clipped by the scene rect, never overhanging the chrome.
 */
private fun layer12TargetLabel(
    cache: RenderCache,
    plan: ScenePlan,
    labels: SceneLabels,
    palette: LumintraColors,
) {
    cache.labelPaint.color = argb(palette.onSky, 240)
    cache.labelPaint.textSize = 11f * plan.density
    cache.labelPaint.isFakeBoldText = false
    cache.creditPaint.color = argb(palette.onSkyMuted, 190)
    cache.creditPaint.textSize = 9f * plan.density

    if (!plan.targetX.isNaN()) {
        val text = plan.targetLabel
        if (text != null) {
            val measured = cache.labelPaint.measureText(text)
            val above = plan.targetY - plan.targetRadiusPx - 12f * plan.density
            val y = if (above < 14f * plan.density) {
                plan.targetY + plan.targetRadiusPx + 14f * plan.density
            } else {
                above
            }
            plan.targetLabelX = (plan.targetX - measured / 2f)
                .coerceIn(4f, (plan.width - measured - 4f).coerceAtLeast(4f))
            plan.targetLabelY = y.coerceIn(14f * plan.density, plan.height - 4f)
        }
    }
    if (plan.targetOccluded) {
        // The text is assigned here, beside the text it is measured from. Leaving it null would draw an
        // empty string through a `?: ""` at the blit, which renders an invisible label and passes a check
        // that only looks for the flag — the label and its flag are one decision, so they are set together.
        plan.belowRidgeText = labels.belowRidgeLabel
        val measured = cache.labelPaint.measureText(labels.belowRidgeLabel)
        plan.belowRidgeLabelX = ((plan.width - measured) / 2f).coerceAtLeast(4f)
        plan.belowRidgeLabelY = plan.height - 10f * plan.density
    }
    plan.esoCreditX = 6f * plan.density
    plan.esoCreditY = 14f * plan.density
    plan.esoCreditText = labels.esoCredit
    plan.esoCreditVisible = plan.hasMilkyWay && labels.esoCredit.isNotEmpty()
}

private fun blitLabels(native: NativeCanvas, cache: RenderCache, plan: ScenePlan) {
    if (!plan.targetX.isNaN()) {
        val text = plan.targetLabel
        if (text != null) native.drawText(text, plan.targetLabelX, plan.targetLabelY, cache.labelPaint)
    }
    for (index in 0 until cache.planetCount) {
        val text = cache.planetLabels[index] ?: continue
        if (!cache.planetVisible[index]) continue
        val measured = cache.planetLabelPaint.measureText(text)
        native.drawText(
            text,
            cache.planetX[index] - measured / 2f,
            cache.planetY[index] - cache.planetRadius[index] - 4f * plan.density,
            cache.planetLabelPaint,
        )
    }
    for (index in 0 until cache.trailAgeCount) {
        val text = cache.trailAgeLabels[index] ?: continue
        native.drawText(text, cache.trailAgeX[index], cache.trailAgeY[index], cache.planetLabelPaint)
    }
    if (plan.targetOccluded) {
        native.drawText(
            plan.belowRidgeText ?: "",
            plan.belowRidgeLabelX,
            plan.belowRidgeLabelY,
            cache.labelPaint,
        )
    }
    if (plan.esoCreditVisible) {
        native.drawText(plan.esoCreditText ?: "", plan.esoCreditX, plan.esoCreditY, cache.creditPaint)
    }
}

// ---------------------------------------------------------------------------------------------
// Helpers — the arithmetic the layers share, each one stated once.
// ---------------------------------------------------------------------------------------------

/** `k = 2f − 1` is the *fraction's* own complement: this file keeps the ellipse's semi-minor, `r·|1−2f|`. */
internal const val DEFAULT_MOON_SIZE_DEG: Double = 0.52

/** The V-band extinction coefficient, magnitudes per air mass: a mid-latitude site's typical value. */
internal const val EXTINCTION_BETA: Double = 0.20

/** How much of a pass is drawn as the streak, in seconds before the frame's instant. */
internal const val TRAIL_SECONDS: Long = 20L

/** The ridge's samples across the rect; a whole-degree profile needs no finer a line than this. */
internal const val SILHOUETTE_SAMPLES: Int = 240

/** The silhouette is closed this far outside the rect, so a filled path never shows a seam. */
internal const val SILHOUETTE_OVERSHOOT: Float = 6f

/** How far a profile sample must rise above its own baseline before it is drawn as a tree. */
internal const val TREE_MIN_PROMINENCE_DEG: Float = 1.6f

/**
 * The Kasten–Young relative air mass, clamped where the formula breaks down.
 *
 * `X = 1 / (cos z + 0.50572·(96.07995 − z)^−1.6364)`, with `z = 90° − h` in degrees. Below the horizon the
 * value is meaningless light, so it is clamped to the horizon's own value — the caller has already
 * dropped anything below the ridge, and this clamp is what keeps a negative altitude from producing a
 * NaN by way of a negative power.
 */
private fun kastenYoung(altitudeDeg: Double): Double {
    val zenith = (90.0 - altitudeDeg.coerceIn(0.0, 90.0))
    val denominator = cos(Math.toRadians(zenith)) +
        0.50572 * (96.07995 - zenith).pow(-1.6364)
    if (denominator <= 0.0) return KASTEN_MAX
    return (1.0 / denominator).coerceAtMost(KASTEN_MAX)
}

private const val KASTEN_MAX: Double = 40.0

/** The ridge's altitude at an azimuth; no profile is a flat horizon at 0°, which is the honest default. */
private fun ridgeAt(horizon: HorizonProfile?, azimuthDeg: Double): Double =
    if (horizon == null) 0.0 else horizon.altitudeAt(azimuthDeg)

/**
 * Galactic → equatorial (J2000), into a scratch pair.
 *
 * The same rotation the standard galactic constants define: the north galactic pole at
 * `RA 192.85948°, Dec 27.12825°` and the galactic centre at `l = 122.93192°` from the ascending node.
 */
private fun galacticToEquatorial(longitudeDeg: Double, latitudeDeg: Double, out: DoubleArray) {
    val l = Math.toRadians(longitudeDeg)
    val b = Math.toRadians(latitudeDeg)
    val poleDec = Math.toRadians(27.12825)
    val poleRa = Math.toRadians(192.85948)
    val node = Math.toRadians(122.93192)

    val sinDec = sin(poleDec) * sin(b) + cos(poleDec) * cos(b) * cos(node - l)
    val dec = Math.asin(sinDec.coerceIn(-1.0, 1.0))
    val y = cos(b) * sin(node - l)
    val x = cos(poleDec) * sin(b) - sin(poleDec) * cos(b) * cos(node - l)
    var ra = poleRa + atan2(y, x)
    if (ra < 0.0) ra += 2.0 * PI_RAW
    out[0] = Math.toDegrees(ra) % 360.0
    out[1] = Math.toDegrees(dec)
}

private const val PI_RAW: Double = 3.141592653589793

/** The magnitude group a star belongs to, `0` for the brightest: 1-magnitude bins, eight of them. */
private fun magnitudeGroup(magnitude: Double): Int =
    floor((magnitude + 1.5) / 1.0).toInt().coerceIn(0, RenderCache.STAR_GROUPS - 1)

/** The magnitude at a group's centre, used for its size and alpha. */
private fun groupCentreMagnitude(group: Int): Double = -1.5 + group + 0.5

/** A deterministic `[0, 1)` from two integers, so one instant always scintillates one way. */
private fun hashUnit(index: Int, seed: Long): Double {
    var h = index * 374761393L + seed * 668265263L
    h = (h xor (h shr 13)) * 1274126177L
    h = h xor (h shr 16)
    return ((h ushr 11).toDouble() / (1L shl 53).toDouble()).let { it - floor(it) }
}

/** A non-observable pass is dimmed, never dropped: the dimming is the difference, not the presence. */
private fun visibilityAlpha(visibility: PassVisibility): Int = when (visibility) {
    PassVisibility.OBSERVABLE -> 235
    PassVisibility.IN_EARTH_SHADOW -> 105
    PassVisibility.TOO_BRIGHT -> 90
}

/** An opaque ridge is a darkened sky, not a black bar, so it keeps the palette's hue in red mode. */
private fun nightShade(color: Color): Color = lerp(color, Color.Black, 0.78)

/** `Color` → the ARGB int the native canvas takes. */
internal fun argb(color: Color, alpha: Int): Int =
    ((alpha.coerceIn(0, 255)) shl 24) or
        ((color.red * 255f + 0.5f).toInt().coerceIn(0, 255) shl 16) or
        ((color.green * 255f + 0.5f).toInt().coerceIn(0, 255) shl 8) or
        (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255)

/** The tone map and the transfer curve, then an ordered dither, then 8 bits — in that order. */
private fun skyArgb(r: Float, g: Float, b: Float, tonemapped: Boolean, dither: Float): Int {
    val red = encodeChannel(r, tonemapped, dither)
    val green = encodeChannel(g, tonemapped, dither)
    val blue = encodeChannel(b, tonemapped, dither)
    return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun encodeChannel(value: Float, tonemapped: Boolean, dither: Float): Int {
    val linear = if (tonemapped) aces(value) else value
    val encoded = linearToSrgb(linear) + dither
    return (encoded.coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255)
}

/**
 * Red mode: the sky's own luminance through the theme's red ramp, with green and blue **exactly zero**.
 *
 * The ramp's shape comes from the palette — `skyZenith` to `firstLight` — so red mode is a remapping of
 * the theme and not a second palette invented here, and the two dark channels are forced to zero rather
 * than trusted to the palette's own reds. The luminance is taken after the tone map, so the ramp stays
 * monotone in what the eye receives rather than in raw radiance.
 */
private fun redArgb(
    r: Float,
    g: Float,
    b: Float,
    tonemapped: Boolean,
    dither: Float,
    palette: LumintraColors,
): Int {
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val linear = if (tonemapped) aces(luminance) else luminance
    val level = (linearToSrgb(linear) + dither).coerceIn(0f, 1f).toDouble()
    val ramp = lerp(palette.skyZenith, palette.firstLight, level)
    val red = (ramp.red * 255f + 0.5f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (red shl 16)
}


