package com.gexetr.lumintralightplanner.data.sky

import android.content.Context
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.orekit.bodies.CelestialBody
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.data.DataContext
import org.orekit.data.DataLoader
import org.orekit.data.DataProvider
import org.orekit.data.DataProvidersManager
import org.orekit.data.LazyLoadedDataContext
import org.orekit.frames.Frame
import org.orekit.frames.Frames
import org.orekit.frames.TopocentricFrame
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScale
import org.orekit.time.TimeScales
import org.orekit.utils.Constants
import org.orekit.utils.IERSConventions
import org.orekit.bodies.GeodeticPoint as OrekitGeodeticPoint

/**
 * The **one** place in Lumintra that configures Orekit.
 *
 * Orekit is initialised exactly once, from one bundled file, with **no network access of any kind**:
 * `DataProvidersManager.clearProviders()` removes every default provider (the `DirectoryCrawler` that
 * scans `$OREKIT_DATA_PATH`), and exactly one provider — the bundled leap-second table — is installed
 * in its place. Orekit never downloads data on its own, and after this initialisation it has no
 * provider that could read anything outside the caller's stream either, so "Orekit tries to fetch at
 * runtime" is not a state this class can enter.
 *
 * ### What is loaded, and why
 *
 * Orekit refuses to run without an IERS UTC–TAI history: the first touch of the UTC time scale throws
 * `no IERS UTC-TAI history data loaded` when the table is missing. The table is the 3,321-byte
 * `tai-utc.dat` published by USNO at `https://maia.usno.navy.mil/ser7/tai-utc.dat` and bundled with the
 * app; its provenance (source, sha256, and the fact that it is append-only and has recorded no leap
 * second since 2017-01-01) is recorded in the slot's `docs/`. The bytes are **not** read from the
 * filesystem: the caller supplies a stream opener, so the same code works from `assets/`, from a test
 * fixture, or from any other source.
 *
 * ### The ITRF-versions table
 *
 * Building the ITRF frame makes Orekit ask for `itrf-versions.conf` as well. The slot's asset tree on
 * disk holds `orekit-data/tai-utc.dat` and **not** `itrf-versions.conf` (measured this session), so
 * [create] takes that stream as an **optional** second opener: when the orchestrator/package adds the
 * file, pass its opener and Orekit will use it; when it is absent, pass `null` and the runtime is built
 * with only the leap-second table. Passing `null` is the honest representation of the measured state of
 * the asset tree, not a claim that the file is unnecessary — [fromAssets] probes for it with
 * `AssetManager.list` and passes `null` when it is not there, so a missing optional file can never be
 * reported as a required-file failure.
 *
 * ### Why a dedicated `DataContext` and not the process default
 *
 * `LazyLoadedDataContext()` owns its own `DataProvidersManager`, `Frames`, `TimeScales` and
 * `CelestialBodies`. The runtime keeps that context and every frame/time-scale it hands out comes from
 * it (see [teme], [itrf], [utc]), so a second consumer of Orekit elsewhere in the process cannot
 * perturb this one's data set and this one cannot perturb its. `DataContext.setDefault(context)` is
 * called as well, because inside Orekit some code paths (the `TLE` propagator's own time scale, a
 * parser's default context) reach for the process default; setting it to the same context makes the two
 * routes agree instead of silently disagreeing.
 *
 * ### Threading and cancellation
 *
 * [warmUp] is the one potentially slow step (loading the leap-second table, building the ITRF frame and
 * the WGS84 ellipsoid) and it is `suspend`, runs on [dispatcher] (`Dispatchers.Default`, never the main
 * thread), and is idempotent. The synchronous accessors ([absoluteDate], [teme], [itrf], [utc]) must not
 * be called before [warmUp] has completed — doing so does the same work on the calling thread. The
 * engine checks [isReady] and refuses with a typed state rather than initialising on a UI thread.
 *
 * ### What this class cannot promise
 *
 * It cannot prove Orekit performed no network I/O: it removes and replaces every provider it knows
 * about, which is the whole surface Orekit offers, but a future Orekit release adding a network-backed
 * default provider would be installed by a `DataProvidersManager` this class does not manage. It also
 * cannot make the frame construction succeed without the ITRF-versions table — see above.
 */
class OrekitRuntime private constructor(
    private val context: LazyLoadedDataContext,
) {

    /** The Orekit data context this runtime owns. Every frame, scale and parser below comes from it. */
    val dataContext: DataContext get() = context

    /**
     * Where the long searches run. `Dispatchers.Default` is CPU-bound work with no I/O once the data is
     * loaded, which is exactly what the default dispatcher is for; it is never the main thread.
     */
    val dispatcher: CoroutineDispatcher get() = Dispatchers.Default

    /** Orekit's time scales of this runtime. */
    val timeScales: TimeScales get() = context.getTimeScales()

    /**
     * The UTC scale. Touching this property is what loads the bundled leap-second table, and it throws
     * an `OrekitException` when the table could not be read.
     */
    val utc: TimeScale get() = timeScales.getUTC()

    /** Orekit's frames of this runtime. */
    val frames: Frames get() = context.getFrames()

    /** TEME — the frame SGP4 propagates in and the frame `TLE` element sets are published in. */
    val teme: Frame get() = frames.getTEME()

    /**
     * ITRF, built with `IERS_2010` and **`simpleEOP = true`**: the simple-EOP model is what removes the
     * need for the `finals.all` Earth-orientation series, so the app ships one 3,321-byte data file
     * rather than a multi-megabyte one.
     */
    val itrf: Frame by lazy { frames.getITRF(IERSConventions.IERS_2010, true) }

    /** The WGS84 ellipsoid the umbra test and the sub-satellite point are computed against. */
    val earth: OneAxisEllipsoid by lazy {
        OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            itrf,
        )
    }

    /**
     * The Sun, used **only as the shadow-casting body** for the satellite umbra test — never as the
     * source of the observer's own Sun altitude, which comes from the Astronomy Engine (`design.md`:
     * the two engines are not interchangeable for a given body).
     */
    val sun: CelestialBody get() = context.getCelestialBodies().getSun()

    private val warmedUp = AtomicBoolean(false)

    /** True once [warmUp] has completed: the frame and the data are built and nothing blocks a call. */
    val isReady: Boolean get() = warmedUp.get()

    /**
     * Builds everything that is expensive: the UTC scale (which loads the leap-second table), the ITRF
     * frame and the WGS84 ellipsoid. Idempotent; runs on [dispatcher]; cancellable.
     *
     * @throws OrekitRuntimeInitException when the leap-second table could not be read — the app then
     *         reports the capability as unavailable rather than propagating from a fabricated clock.
     */
    suspend fun warmUp() {
        if (warmedUp.get()) return
        withContext(dispatcher) {
            coroutineContext.ensureActive()
            try {
                utc
                itrf
                earth
                teme
            } catch (e: Exception) {
                throw OrekitRuntimeInitException(
                    "Orekit could not load its bundled data (${e.javaClass.simpleName}): ${e.message}",
                    e,
                )
            }
            coroutineContext.ensureActive()
            warmedUp.set(true)
        }
    }

    /** An [AbsoluteDate] for a UTC instant, on this runtime's UTC scale. */
    fun absoluteDate(instantUtc: Instant): AbsoluteDate = AbsoluteDate(instantUtc, utc)

    /** The UTC instant of an [AbsoluteDate], resolved through this runtime's own time scales. */
    fun instantOf(date: AbsoluteDate): Instant = date.toInstant(timeScales)

    /** The observer's geodetic position in Orekit's own type, with the same sign conventions. */
    fun orekitPoint(point: GeodeticPoint): OrekitGeodeticPoint =
        OrekitGeodeticPoint(point.latDeg, point.lonDeg, point.elevationM)

    /**
     * The topocentric frame of a saved spot: the frame every elevation, azimuth and range in this
     * package is measured in. It is built against [earth], so its zenith is the **geodetic** zenith.
     */
    fun topocentricFrame(point: GeodeticPoint, name: String = "observer"): TopocentricFrame =
        TopocentricFrame(earth, orekitPoint(point), name)

    companion object {

        /** The bundled leap-second table, relative to `src/main/assets/`. */
        const val LEAP_SECOND_ASSET_PATH: String = "orekit-data/tai-utc.dat"

        /**
         * The name the stream is presented under. It is a **bare file name** because Orekit's own
         * leap-second loader matches either the full path or the base name, and a bare base name
         * satisfies both readings.
         */
        const val LEAP_SECOND_DATA_NAME: String = "tai-utc.dat"

        /** The optional ITRF-versions table, relative to `src/main/assets/`. Absent today. */
        const val ITRF_VERSIONS_ASSET_PATH: String = "orekit-data/itrf-versions.conf"

        /** The name the optional ITRF-versions stream is presented under. */
        const val ITRF_VERSIONS_DATA_NAME: String = "itrf-versions.conf"

        /**
         * Builds the runtime. No I/O happens here: the openers are only called when Orekit asks for the
         * data, which [warmUp] triggers off the main thread.
         *
         * @param openLeapSeconds called (possibly more than once) to obtain a **fresh** stream of the
         *        leap-second table: a stream that cannot be opened is a typed initialisation failure.
         * @param openItrfVersions optional opener for `itrf-versions.conf`; pass `null` when the asset
         *        tree does not carry it (the state measured this session). A stream that cannot be
         *        opened here is **not** fatal — the provider simply does not supply the file, and
         *        Orekit decides whether it needed it.
         */
        fun create(
            openLeapSeconds: () -> InputStream,
            openItrfVersions: (() -> InputStream)? = null,
        ): OrekitRuntime {
            val context = LazyLoadedDataContext()
            val manager = context.getDataProvidersManager()
            // Local-only: every default provider (the DirectoryCrawler that scans $OREKIT_DATA_PATH) is
            // removed, so Orekit cannot read a file it was not handed, and cannot reach a network.
            manager.clearProviders()
            manager.addProvider(
                BundledStreamProvider(LEAP_SECOND_DATA_NAME, required = true, open = openLeapSeconds),
            )
            if (openItrfVersions != null) {
                manager.addProvider(
                    BundledStreamProvider(ITRF_VERSIONS_DATA_NAME, required = false, open = openItrfVersions),
                )
            }
            // Inside Orekit some code paths (a propagator's own scale, a parser's default context) use
            // the process default; pointing it at this same context keeps the two routes in agreement.
            DataContext.setDefault(context)
            return OrekitRuntime(context)
        }

        /**
         * The Android-facing factory. This is the **only** place in this file that names `Context`, and
         * it does nothing with it beyond opening two asset streams: the context is a door, not a
         * dependency, and it is not stored anywhere.
         *
         * The optional `itrf-versions.conf` is probed with `AssetManager.list` rather than opened
         * blindly, so the measured absence of that file cannot be mistaken for a required-file failure.
         */
        fun fromAssets(
            context: Context,
            assetPath: String = LEAP_SECOND_ASSET_PATH,
            itrfVersionsPath: String? = ITRF_VERSIONS_ASSET_PATH,
        ): OrekitRuntime {
            val appContext = context.applicationContext
            val itrfOpener: (() -> InputStream)? = itrfVersionsPath
                ?.takeIf { assetExists(appContext, it) }
                ?.let { path -> { openAsset(appContext, path) } }
            return create(openLeapSeconds = { openAsset(appContext, assetPath) }, openItrfVersions = itrfOpener)
        }

        /** True when [path] is present under `assets/`, asked of the asset manager without opening it. */
        fun assetExists(context: Context, path: String): Boolean {
            val cut = path.lastIndexOf('/')
            val folder = if (cut < 0) "" else path.substring(0, cut)
            val name = if (cut < 0) path else path.substring(cut + 1)
            return try {
                context.assets.list(folder)?.any { it == name } == true
            } catch (e: IOException) {
                false
            }
        }

        private fun openAsset(context: Context, path: String): InputStream =
            try {
                context.assets.open(path)
            } catch (e: IOException) {
                throw OrekitRuntimeInitException("bundled asset '$path' could not be opened: ${e.message}", e)
            }
    }
}

/**
 * Serves one bundled stream to Orekit under one name.
 *
 * `DataProvider` in Orekit 13 is `feed(Pattern, DataLoader, DataProvidersManager)`: the manager has
 * already compiled the supported-names expression Orekit's own loader asked for, and the provider's job
 * is to hand that loader a stream when the name matches. This implementation matches the name it was
 * created with and nothing else, so a provider cannot widen what Orekit reads.
 *
 * `required` is what separates the two files: the leap-second table is the one Orekit cannot run
 * without, so a stream that will not open is a typed initialisation failure; an optional table that
 * will not open is simply not supplied, and Orekit's own error — if it needed the file — surfaces
 * through [OrekitRuntime.warmUp] with its own message instead of this provider's.
 */
private class BundledStreamProvider(
    private val name: String,
    private val required: Boolean,
    private val open: () -> InputStream,
) : DataProvider {

    override fun feed(supported: Pattern, loader: DataLoader, manager: DataProvidersManager): Boolean {
        if (!loader.stillAcceptsData()) return false
        if (!supported.matcher(name).matches()) return false
        return try {
            open().use { stream -> loader.loadData(stream, name) }
            true
        } catch (e: IOException) {
            if (required) {
                throw OrekitRuntimeInitException("bundled data '$name' could not be read: ${e.message}", e)
            }
            false
        }
    }
}

/**
 * The one failure this runtime raises: its bundled data could not be read or could not be turned into a
 * usable frame. It is a typed, catchable exception so the engine can map it to a `PassRefusal` state
 * instead of letting it escape as an `OrekitException` a screen would have to interpret.
 */
class OrekitRuntimeInitException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
