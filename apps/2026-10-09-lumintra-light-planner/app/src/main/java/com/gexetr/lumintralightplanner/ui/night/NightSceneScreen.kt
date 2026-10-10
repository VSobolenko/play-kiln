package com.gexetr.lumintralightplanner.ui.night

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.CloudFailure
import com.gexetr.lumintralightplanner.data.CloudRepository
import com.gexetr.lumintralightplanner.data.CloudResult
import com.gexetr.lumintralightplanner.data.ScoutSnapshot
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import com.gexetr.lumintralightplanner.data.StarTable
import com.gexetr.lumintralightplanner.data.sky.AltitudeModel
import com.gexetr.lumintralightplanner.data.sky.AstronomySkyEphemeris
import com.gexetr.lumintralightplanner.data.sky.SkyEphemeris
import com.gexetr.lumintralightplanner.data.sky.SolarSystemBody
import com.gexetr.lumintralightplanner.domain.BriefCondition
import com.gexetr.lumintralightplanner.domain.BriefFacts
import com.gexetr.lumintralightplanner.domain.BriefNotice
import com.gexetr.lumintralightplanner.domain.BriefOrdering
import com.gexetr.lumintralightplanner.domain.BriefState
import com.gexetr.lumintralightplanner.domain.ConditionAssessment
import com.gexetr.lumintralightplanner.domain.ConditionInput
import com.gexetr.lumintralightplanner.domain.ConditionKind
import com.gexetr.lumintralightplanner.domain.ConditionSource
import com.gexetr.lumintralightplanner.domain.ConditionState
import com.gexetr.lumintralightplanner.domain.ConditionUnavailableReason
import com.gexetr.lumintralightplanner.domain.CrossingSearch
import com.gexetr.lumintralightplanner.domain.DarkWindowClassifier
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.DarkWindowReport
import com.gexetr.lumintralightplanner.domain.ElementSetAge
import com.gexetr.lumintralightplanner.domain.EquatorialCoords
import com.gexetr.lumintralightplanner.domain.HorizontalCoords
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.KeyTimeBand
import com.gexetr.lumintralightplanner.domain.KeyTimeRow
import com.gexetr.lumintralightplanner.domain.MoonLibration
import com.gexetr.lumintralightplanner.domain.MoonPhaseName
import com.gexetr.lumintralightplanner.domain.MoonState
import com.gexetr.lumintralightplanner.domain.MoonlessKind
import com.gexetr.lumintralightplanner.domain.MoonlessWindow
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.ObjectKind
import com.gexetr.lumintralightplanner.domain.ObjectType
import com.gexetr.lumintralightplanner.domain.ObjectVisibility
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.SceneBody
import com.gexetr.lumintralightplanner.domain.SceneCamera
import com.gexetr.lumintralightplanner.domain.SceneFrame
import com.gexetr.lumintralightplanner.domain.ScenePassTrail
import com.gexetr.lumintralightplanner.domain.SceneTrailPoint
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.domain.SkyGrade
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.SunMeasurements
import com.gexetr.lumintralightplanner.domain.TargetInterval
import com.gexetr.lumintralightplanner.domain.TargetWindow
import com.gexetr.lumintralightplanner.domain.TwilightKind
import com.gexetr.lumintralightplanner.domain.WindowBoundary
import com.gexetr.lumintralightplanner.ui.components.ChipState
import com.gexetr.lumintralightplanner.ui.components.ConditionChip
import com.gexetr.lumintralightplanner.ui.components.LumintraEmptyState
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.components.ValueRow
import com.gexetr.lumintralightplanner.ui.format.formatDegrees
import com.gexetr.lumintralightplanner.ui.format.formatDurationShort
import com.gexetr.lumintralightplanner.ui.format.formatNightLabel
import com.gexetr.lumintralightplanner.ui.format.formatPercent
import com.gexetr.lumintralightplanner.ui.format.formatSignedDegrees
import com.gexetr.lumintralightplanner.ui.format.formatTime
import com.gexetr.lumintralightplanner.ui.theme.LumintraColors
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.LumintraType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =================================================================================================
// 1. The hero screen's state
// =================================================================================================

/**
 * A notice the scene carries, as a **typed case rather than a sentence**: the screen maps each case to
 * a string resource, so no user-facing prose is a literal in Kotlin and `values-ru/` reaches every one.
 */
sealed interface NightNotice {

    /** The bundled twilight LUT is absent or malformed: the documented palette fallback is drawing. */
    data object SkyTableFallback : NightNotice

    /** No horizon profile exists for the spot. A flat horizon is drawn and the note says so. */
    data object HorizonFlat : NightNotice

    /** The profile blocks the whole sky, so nothing can clear it. */
    data object HorizonBlocksSky : NightNotice

    /** The Sun does not set: the scene keeps its daylight palette all night. */
    data object PolarDay : NightNotice

    /** No darkness at all tonight (a non-DARK outcome the user must not read as a dark window). */
    data class NoDarkness(val kind: DarkWindowKind) : NightNotice

    /** The bundled catalogue is missing or unparseable: no target can be marked. */
    data object CatalogueUnreadable : NightNotice

    /** The element set is stale, so pass times may shift by minutes. */
    data class ElementSetStale(val days: Long) : NightNotice

    /** The cloud forecast could not be read this session. */
    data object CloudUnavailable : NightNotice
}

/**
 * One row of "in this frame": a catalogue or computed object whose projected position is inside the
 * scene rect, with the tap point the row selects.
 *
 * [xFraction] / [yFraction] are **fractions of the scene rect**, so a tap can be resolved to a row
 * without the panel and the canvas agreeing on pixels. A row whose altitude is below the horizon is
 * [behindHorizon] rather than omitted, because "nothing is above your horizon" and "one object is up
 * but behind your ridge" are different sentences.
 */
@Immutable
data class NightObjectRow(
    val id: String,
    val designation: String,
    val name: String,
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val magnitude: Double?,
    val behindHorizon: Boolean,
    val isSelected: Boolean,
    val xFraction: Float,
    val yFraction: Float,
)

/**
 * The night scene's state.
 *
 * It carries **facts, never prose**: instants, degrees and typed cases. The screen turns them into
 * words with `stringResource`, which is the only way a second locale reaches the hero surface.
 */
@Immutable
data class NightUiState(
    val loading: Boolean = true,
    /** `NoSpot` until a saved spot exists; a scoped empty state, never a fabricated sky. */
    val briefState: BriefState = BriefState.NoSpot,
    val spotId: String? = null,
    val spotName: String = "",
    val nightLabel: String = "",
    val zoneId: String = "",
    val spanStartUtc: Instant? = null,
    val spanEndUtc: Instant? = null,
    /** `0…1` inside the session window. */
    val scrubValue: Double = 0.0,
    /** The instant the scrub value resolves to, `null` while no night is computed. */
    val instantUtc: Instant? = null,
    val frame: SceneFrame? = null,
    val assets: SceneAssets? = null,
    val strip: TimelineStripModel? = null,
    val rows: List<NightObjectRow> = emptyList(),
    val horizonDrawn: Boolean = false,
    val horizonBlocksSky: Boolean = false,
    val redMode: Boolean = false,
    val selectedTargetId: String? = null,
    /** The active lens, as the three numbers the readout states. Never a pre-formatted sentence. */
    val lensFocalMm: Double = 0.0,
    val lensFovHorizontalDeg: Double = 0.0,
    val lensFovVerticalDeg: Double = 0.0,
    /** The computed darkness band, mapped to words by the screen; the user's own estimate is apart. */
    val skyLevel: com.gexetr.lumintralightplanner.domain.DarknessLevel? = null,
    val userEstimateSqm: Double? = null,
    val userEstimateBortle: Int? = null,
    /** The dark-window answer, typed: the screen renders one sentence per case. */
    val darkWindowKind: DarkWindowKind = DarkWindowKind.DARK,
    val darkWindowStartUtc: Instant? = null,
    val darkWindowEndUtc: Instant? = null,
    val darkWindowSecondStartUtc: Instant? = null,
    val darkWindowSecondEndUtc: Instant? = null,
    val moonFraction: Double = 0.0,
    val moonAltitudeDeg: Double = 0.0,
    val moonPhase: com.gexetr.lumintralightplanner.domain.MoonPhaseName? = null,
    val notices: List<NightNotice> = emptyList(),
    /**
     * The **Play night** toggle. While it is true the screen advances the scrub along the session and
     * stops at the end; it is a user-started, self-terminating run, not a clock, and it is the only
     * state here that produces frames with no input behind them.
     */
    val playing: Boolean = false,
    /** Star-trail mode, entered and left by the user on the scene's chrome. */
    val trailMode: Boolean = false,
    /** The trail interval in minutes: one of [TRAIL_INTERVALS_MIN], default 30. */
    val trailIntervalMinutes: Double = TRAIL_INTERVAL_DEFAULT_MIN,
    /**
     * The scene's own event sink for the controls its frozen signature has no callback for.
     *
     * The hero screen's signature is fixed by `INTERFACES.md` — `state`, `onScrub`,
     * `onSelectTarget`, `onBackToHorizon`, `onToggleRedMode`, `onOpenBrief` — so the Play-night
     * toggle, the star-trails switch and the interval selector have nowhere else to go. They are
     * routed through [NightSceneCommands] rather than through added parameters, because a screen
     * whose signature is frozen is a screen nobody may re-wire from underneath.
     */
    val commands: NightSceneCommands = NightSceneCommands.NONE,
    /** The TalkBack summary of the scene, built by the ViewModel from the same facts it draws. */
    val sceneSummary: String = "",
) {
    val hasSelectedTarget: Boolean get() = selectedTargetId != null

    val span: NightSpan?
        get() {
            val start = spanStartUtc ?: return null
            val end = spanEndUtc ?: return null
            return NightSpan(start, end, spotId)
        }
}

/**
 * The three events the hero screen's frozen signature has no callback for.
 *
 * `INTERFACES.md` fixes `NightSceneScreen`'s parameter list, so the Play-night toggle, the star-trails
 * switch and the trail-interval selector are delivered with the state instead of beside it. It is an
 * **interface of intents**, not a bag of stored lambdas: [NightUiState] is a data class and equality is
 * what keeps a recomposition from running every frame, so the field must compare by identity — which a
 * single ViewModel instance does and a freshly-created lambda would not.
 *
 * Red night mode is deliberately **not** here: the frozen signature already carries
 * `onToggleRedMode`, and a second path to the same switch is how two controls end up disagreeing.
 */
interface NightSceneCommands {

    /** Start or stop the bounded Play-night run. */
    fun togglePlay()

    /** Enter or leave star-trail mode. */
    fun toggleTrails()

    /** Choose the trail interval; a value outside [TRAIL_INTERVALS_MIN] is refused, never clamped. */
    fun setTrailInterval(minutes: Double)

    companion object {
        /**
         * The no-op sink: a preview, a screenshot harness or a test drives nothing.
         *
         * It is the default so a screen composed from a literal state still draws and still reports
         * its facts - it simply has no model behind it to change.
         */
        val NONE: NightSceneCommands = object : NightSceneCommands {
            override fun togglePlay() = Unit
            override fun toggleTrails() = Unit
            override fun setTrailInterval(minutes: Double) = Unit
        }
    }
}

// =================================================================================================
// 2. The engine: the one place a night is computed
// =================================================================================================

/** The target altitude mask the app applies on top of the drawn horizon, in degrees. */
private const val TARGET_ALTITUDE_MASK_DEG: Double = 10.0

/** The scene's ambient live source for the moment the app opens: nothing else reads a wall clock. */
internal fun defaultInstant(): Instant = Instant.now()

/**
 * The display names of the bodies the canvas marks, resolved from string resources by the ViewModel.
 *
 * It is a parameter rather than a lookup because the scene layer holds no `Context`: a `draw` call
 * could not read a resource anyway, and a literal here would be a string no locale could translate.
 */
@Immutable
data class BodyNames(
    val sun: String,
    val moon: String,
    /** One name per planet, keyed by the body the ephemeris names. */
    val planets: Map<SolarSystemBody, String>,
)

/**
 * The per-night computation, ready to be rendered at any instant.
 *
 * It exists so the expensive half — the session's twilight search, the dark window, the Moon, the sky
 * grade, the catalogue's computed rows, the pass trails — happens **once per (spot, night)**, while the
 * per-scrub half (`sceneAt`) is a projection of facts already in hand. A scrub therefore costs no
 * ephemeris search, which is what the capability's scrub requirement asks for: only the draw and the
 * projection rerun.
 */
internal class NightComputation(
    val brief: com.gexetr.lumintralightplanner.domain.SessionBrief,
    /** The catalogue's fixed rows plus the computed Solar-System rows, in catalogue order. */
    val objects: List<SkyObject>,
    val horizon: HorizonProfile?,
    /** The trails for the whole night, split at horizon crossings and carrying their element age. */
    val trails: List<ScenePassTrail>,
    /**
     * The names of the bodies the canvas marks. They arrive as resources rather than as literals, so
     * `values-ru/` reaches a solar-system label exactly as it reaches a button.
     */
    val names: BodyNames,
) {

    /**
     * The frame for one instant.
     *
     * Every field is derived from the passed instant and the engine — never from a device clock — so two
     * calls with one instant produce one frame, which is what makes the screen's memoisation sound.
     */
    fun sceneAt(
        instantUtc: Instant,
        ephemeris: SkyEphemeris,
        lut: TwilightLut,
        lens: com.gexetr.lumintralightplanner.domain.LensPreset,
        assets: SceneAssets,
        selectedTargetId: String?,
        redMode: Boolean,
    ): SceneFrame {
        val point = brief.point()
        val sunHorizontal = ephemeris.sunHorizontal(point, instantUtc, AltitudeModel.REFRACTED)
        val moonState = ephemeris.moonState(point, instantUtc, AltitudeModel.REFRACTED)
        val sun = SceneBody(
            id = SUN_ID,
            name = names.sun,
            type = ObjectType.SUN,
            position = sunHorizontal,
            magnitude = ephemeris.sunMagnitude(instantUtc),
            angularSizeDeg = SUN_ANGULAR_SIZE_DEG,
            kind = ObjectKind.COMPUTED,
            occludedByHorizon = occluded(horizon, sunHorizontal),
        )
        val moon = SceneBody(
            id = MOON_ID,
            name = names.moon,
            type = ObjectType.MOON,
            position = moonState.position,
            magnitude = moonState.magnitude,
            angularSizeDeg = ephemeris.moonAngularDiameterDeg(instantUtc),
            illuminatedFraction = moonState.illuminatedFraction,
            kind = ObjectKind.COMPUTED,
            occludedByHorizon = occluded(horizon, moonState.position),
        )
        val planets = SolarSystemBody.entries.map { body ->
            val horizontal = ephemeris.planetHorizontal(body, point, instantUtc, AltitudeModel.REFRACTED)
            SceneBody(
                id = body.name,
                name = names.planets[body] ?: body.name,
                type = ObjectType.PLANET,
                position = horizontal,
                magnitude = ephemeris.planetMagnitude(body, instantUtc),
                angularSizeDeg = null,
                kind = ObjectKind.COMPUTED,
                occludedByHorizon = occluded(horizon, horizontal),
            )
        }
        val selected = selectedTargetId?.let { id ->
            objects.firstOrNull { it.id == id }?.let { target ->
                val horizontal = ephemeris.fixedHorizontal(
                    point,
                    EquatorialCoords(target.raDeg, target.decDeg),
                    instantUtc,
                    AltitudeModel.REFRACTED,
                )
                SceneBody(
                    id = target.id,
                    name = target.name,
                    type = target.type,
                    position = horizontal,
                    magnitude = target.displayMagnitude,
                    angularSizeDeg = target.angularSizeMajDeg,
                    kind = target.kind,
                    occludedByHorizon = occluded(horizon, horizontal),
                )
            }
        }
        val targetPosition = selected?.position
        val camera = SceneCamera.of(lens, brief.latitudeDeg, targetPosition)
        val core = ephemeris.fixedHorizontal(
            point,
            EquatorialCoords(GALACTIC_CENTRE_RA_DEG, GALACTIC_CENTRE_DEC_DEG),
            instantUtc,
            AltitudeModel.REFRACTED,
        )
        val siderealRad = Math.toRadians(ephemeris.localSiderealHours(point, instantUtc) * 15.0)
        return SceneFrame(
            instantUtc = instantUtc,
            spotId = brief.spotId,
            point = point,
            tzId = brief.tzId,
            camera = camera,
            lensFov = com.gexetr.lumintralightplanner.domain.FieldOfView.of(lens),
            sun = sun,
            moon = moon,
            moonLibration = ephemeris.moonLibration(instantUtc),
            planetBodies = planets,
            selectedTarget = selected,
            trails = trails,
            horizon = horizon,
            skyRotationRad = siderealRad,
            milkyWayRotationRad = siderealRad,
            milkyWayCorePosition = core,
            airMassAtCenter = HorizontalCoords.airMassAt(camera.centerAltitudeDeg),
            twilightBand = twilightBandOf(ephemeris.sunHorizontal(point, instantUtc, AltitudeModel.GEOMETRIC).altitudeDeg),
            skyGrade = brief.skyGrade,
            redMode = redMode,
        )
    }

    /** Every object whose projected position lands inside the rect, brightest first. */
    fun rowsAt(frame: SceneFrame, ephemeris: SkyEphemeris): List<NightObjectRow> {
        val point = frame.point
        val out = ArrayList<NightObjectRow>(ROWS_LIMIT)
        for (target in objects) {
            if (target.type.isComputedOnly && target.type != ObjectType.PLANET) continue
            val horizontal = ephemeris.fixedHorizontal(
                point,
                EquatorialCoords(target.raDeg, target.decDeg),
                frame.instantUtc,
                AltitudeModel.REFRACTED,
            )
            val projected = frame.camera.project(horizontal) ?: continue
            out.add(
                NightObjectRow(
                    id = target.id,
                    designation = target.designation,
                    name = target.name,
                    altitudeDeg = horizontal.altitudeDeg,
                    azimuthDeg = horizontal.azimuthDeg,
                    magnitude = target.displayMagnitude,
                    behindHorizon = occluded(horizon, horizontal),
                    isSelected = frame.selectedTarget?.id == target.id,
                    xFraction = projected.xFraction,
                    yFraction = projected.yFraction,
                ),
            )
        }
        planetsOf(frame).forEach { body -> out.add(rowOf(body, frame, horizon)) }
        out.sortWith(compareBy({ it.magnitude ?: Double.MAX_VALUE }, { it.designation }))
        return if (out.size <= ROWS_LIMIT) out else out.subList(0, ROWS_LIMIT)
    }

    private fun planetsOf(frame: SceneFrame): List<SceneBody> =
        frame.planetBodies.filter { frame.camera.project(it.position) != null }

    private fun rowOf(body: SceneBody, frame: SceneFrame, horizon: HorizonProfile?): NightObjectRow {
        val projected = frame.camera.project(body.position)!!
        return NightObjectRow(
            id = body.id,
            designation = body.name,
            name = body.name,
            altitudeDeg = body.position.altitudeDeg,
            azimuthDeg = body.position.azimuthDeg,
            magnitude = body.magnitude,
            behindHorizon = occluded(horizon, body.position),
            isSelected = frame.selectedTarget?.id == body.id,
            xFraction = projected.xFraction,
            yFraction = projected.yFraction,
        )
    }

    companion object {
        /** How many rows the "in this frame" panel holds. A cap, not a filter. */
        const val ROWS_LIMIT: Int = 40

        const val SUN_ID: String = "sun"
        const val MOON_ID: String = "moon"

        /** The Sun's mean apparent diameter, 32.0′. A mean, never a per-instant semidiameter. */
        const val SUN_ANGULAR_SIZE_DEG: Double = 32.0 / 60.0

        /**
         * The Galactic Centre (Sgr A*), J2000.0 — the point the Milky Way band is anchored on.
         *
         * It is a fixed catalogue direction, so it is stated here as one rather than derived from a
         * table this slot does not ship. If a row for it ever lands in `objects.json`, that row should
         * replace this constant.
         */
        const val GALACTIC_CENTRE_RA_DEG: Double = 266.4168
        const val GALACTIC_CENTRE_DEC_DEG: Double = -29.0078

        /** True when the drawn horizon hides a body. A flat/absent profile occludes nothing above 0°. */
        fun occluded(horizon: HorizonProfile?, position: HorizontalCoords): Boolean {
            val ridge = horizon?.altitudeAt(position.azimuthDeg) ?: 0.0
            return position.altitudeDeg < ridge
        }

        /** The band the Sun is inside: the deepest threshold it is at or below, or `null` in daylight. */
        fun twilightBandOf(sunAltitudeDeg: Double): TwilightKind? = when {
            sunAltitudeDeg <= TwilightKind.ASTRONOMICAL.angleDeg -> TwilightKind.ASTRONOMICAL
            sunAltitudeDeg <= TwilightKind.NAUTICAL.angleDeg -> TwilightKind.NAUTICAL
            sunAltitudeDeg <= TwilightKind.CIVIL.angleDeg -> TwilightKind.CIVIL
            else -> null
        }
    }
}

/** The Sun's geometric altitude at an instant, as a plain lambda the searches consume. */
private fun sunAltitude(point: com.gexetr.lumintralightplanner.domain.GeodeticPoint, e: SkyEphemeris): (Instant) -> Double =
    { t -> e.sunHorizontal(point, t, AltitudeModel.GEOMETRIC).altitudeDeg }

/** The Moon's apparent altitude at an instant. */
private fun moonAltitude(point: com.gexetr.lumintralightplanner.domain.GeodeticPoint, e: SkyEphemeris): (Instant) -> Double =
    { t -> e.moonHorizontal(point, t, AltitudeModel.REFRACTED).altitudeDeg }

/**
 * The night's numbers, computed from the spot and the night date.
 *
 * Every method here is pure with respect to its arguments, and the class is the only place a night is
 * assembled — the two screens render what it returns rather than recomputing anything, so the scene and
 * the brief cannot disagree about the same night.
 */
internal object NightEngine {

    /** The four core conditions the brief always carries, plus one per favourite that is up tonight. */
    private const val TARGET_CONDITION_LIMIT: Int = 6

    /**
     * Builds one night.
     *
     * [objects] are the catalogue's rows (fixed) plus its computed Solar-System appendix; the target
     * windows are computed for favourites and for anything the caller names, and **never** for the whole
     * catalogue, because the session is a sampled search per target and 300 sampled searches per scrub
     * would be work charged to a gesture.
     */
    fun compute(
        spot: Scout,
        horizon: HorizonProfile?,
        nightDate: LocalDate,
        ephemeris: SkyEphemeris,
        objects: List<SkyObject>,
        cloud: CloudReportHolder,
        passes: List<SatellitePass>,
        elementSetAge: ElementSetAge?,
        favouriteIds: Set<String>,
        windowIds: Set<String>,
        notices: List<BriefNotice>,
        names: BodyNames,
    ): NightComputation {
        val zone = ZoneId.of(spot.tzId)
        val session = SessionClock.sessionWindow(nightDate, zone, spot.id)
        val point = spot.point
        val sun = sunAltitude(point, ephemeris)
        val belowThreshold = TwilightKind.entries.associateWith { kind ->
            CrossingSearch.intervalsAtOrBelow(session, kind.angleDeg, f = sun)
        }
        val sunMin = CrossingSearch.extremum(session, findMax = false, f = sun)
        val sunMax = CrossingSearch.extremum(session, findMax = true, f = sun)
        val sunriseSunset = ephemeris.sunriseSunset(point, session)
        val measurements = SunMeasurements(
            session = session,
            sunsetUtc = sunriseSunset.sunsetUtc,
            sunriseUtc = sunriseSunset.sunriseUtc,
            sunAltMinDeg = sunMin.value,
            sunAltMinInstant = sunMin.instantUtc,
            sunAltMaxDeg = sunMax.value,
            sunAltMaxInstant = sunMax.instantUtc,
            belowThreshold = belowThreshold,
        )
        val report = DarkWindowClassifier.classify(measurements)
        val keyTimes = keyTimes(session, sun)
        val midnight = session.instantAt(0.5)
        val moon = ephemeris.moonState(point, midnight, AltitudeModel.REFRACTED)
        val moonless = moonlessWindow(session, report, point, ephemeris, moon)
        val grade = SkyGrade(
            darkestSunAltitudeDeg = sunMin.value,
            darkestInstant = sunMin.instantUtc,
            darkWindowKind = report.kind,
            darkDuration = report.darkWindows.fold(Duration.ZERO) { acc, span -> acc + span.duration },
            moonIlluminatedFraction = moon.illuminatedFraction,
            moonAltitudeDeg = moon.altitudeDeg,
        )
        val windows = targetWindows(session, point, ephemeris, horizon, objects, favouriteIds, windowIds)
        val conditions = conditionsOf(report, moon, moonless, cloud, passes, horizon, spot)
        val brief = com.gexetr.lumintralightplanner.domain.SessionBrief(
            spotId = spot.id,
            spotName = spot.name,
            tzId = spot.tzId,
            latitudeDeg = spot.latDeg,
            longitudeDeg = spot.lonDeg,
            nightDate = nightDate,
            session = session,
            darkWindow = report,
            keyTimes = keyTimes,
            sunriseSunset = sunriseSunset,
            moon = moon,
            moonless = moonless,
            targetWindows = windows,
            passes = passes,
            cloud = cloud.report,
            skyGrade = grade,
            conditions = BriefOrdering.order(conditions, report.darkWindows.firstOrNull()?.startUtc),
            notices = notices,
            elementSetAge = elementSetAge,
        )
        return NightComputation(
            brief = brief,
            objects = objects,
            horizon = horizon,
            trails = trailsOf(passes, point),
            names = names,
        )
    }

    /** The five key-time rows: golden and blue are band differences, the three twilights are crossings. */
    fun keyTimes(session: NightSpan, sun: (Instant) -> Double): List<KeyTimeRow> {
        val goldenHigh = CrossingSearch.intervalsAtOrBelow(session, KeyTimeBand.GOLDEN_HIGH_DEG.d(), f = sun)
        val goldenLow = CrossingSearch.intervalsAtOrBelow(session, KeyTimeBand.GOLDEN_LOW_DEG.d(), f = sun)
        val blueLow = CrossingSearch.intervalsAtOrBelow(session, KeyTimeBand.BLUE_LOW_DEG.d(), f = sun)
        return listOf(
            KeyTimeRow.of(KeyTimeBand.GOLDEN_HOUR, NightSpan.subtract(goldenHigh, goldenLow)),
            KeyTimeRow.of(KeyTimeBand.BLUE_HOUR, NightSpan.subtract(goldenLow, blueLow)),
            KeyTimeRow.of(KeyTimeBand.CIVIL, CrossingSearch.intervalsAtOrBelow(session, TwilightKind.CIVIL.angleDeg, f = sun)),
            KeyTimeRow.of(KeyTimeBand.NAUTICAL, CrossingSearch.intervalsAtOrBelow(session, TwilightKind.NAUTICAL.angleDeg, f = sun)),
            KeyTimeRow.of(KeyTimeBand.ASTRONOMICAL, CrossingSearch.intervalsAtOrBelow(session, TwilightKind.ASTRONOMICAL.angleDeg, f = sun)),
        )
    }

    /** `KeyTimeBand`'s own bounds are degrees already; this keeps the call site from restating them. */
    private fun Double.d(): Double = this

    /**
     * The moonless sub-intervals of the dark window.
     *
     * The Moon counts as interfering when it is **up and more than 10 % lit**, which is the app's one
     * stated rule; the extremes carry the actionable instant of each opposite case rather than a
     * fraction on its own.
     */
    fun moonlessWindow(
        session: NightSpan,
        report: DarkWindowReport,
        point: com.gexetr.lumintralightplanner.domain.GeodeticPoint,
        ephemeris: SkyEphemeris,
        moonAtMidnight: MoonState,
    ): MoonlessWindow {
        val darkWindows = report.darkWindows
        if (darkWindows.isEmpty()) return MoonlessWindow(MoonlessKind.NO_DARK_WINDOW)
        val moonAlt = moonAltitude(point, ephemeris)
        val upWindows = CrossingSearch.intervalsAbove(session, 0.0, f = moonAlt)
        val blocksLight = moonAtMidnight.illuminatedFraction > DARK_MOON_INTERFERENCE_FRACTION
        val gaps = if (blocksLight) {
            NightSpan.intersectAll(darkWindows, NightSpan.subtract(listOf(session), upWindows))
        } else {
            darkWindows
        }
        val rise = crossingsOf(moonAlt, session).firstOrNull { it.second }
        val set = crossingsOf(moonAlt, session).firstOrNull { !it.second }
        val kind = when {
            !blocksLight && upWindows.isEmpty() -> MoonlessKind.MOONLESS_ALL_NIGHT
            !blocksLight && rise == null -> MoonlessKind.MOON_NEVER_RISES
            !blocksLight && set == null -> MoonlessKind.MOON_NEVER_SETS
            gaps.isNotEmpty() -> MoonlessKind.MOONLESS_INTERVALS
            else -> MoonlessKind.MOON_UP_ALL_WINDOW
        }
        val extreme = if (blocksLight && gaps.isEmpty()) {
            val window = NightSpan.longest(darkWindows)
            window?.let { CrossingSearch.extremum(it, findMax = true, f = moonAlt) }
        } else {
            val window = NightSpan.longest(darkWindows)
            window?.let { CrossingSearch.extremum(it, findMax = false, f = moonAlt) }
        }
        return MoonlessWindow(
            kind = kind,
            intervals = gaps,
            extremeAltitudeDeg = extreme?.value,
            extremeInstant = extreme?.instantUtc,
            washedOut = blocksLight && gaps.isEmpty() && moonAtMidnight.isFullMoon,
        )
    }

    /** The first rise or set of a sampled altitude inside the session. */
    private fun crossingsOf(altitude: (Instant) -> Double, session: NightSpan): List<Pair<Instant, Boolean>> {
        val out = ArrayList<Pair<Instant, Boolean>>()
        val step = Duration.ofMinutes(5)
        var previous = altitude(session.startUtc) > 0.0
        var t = session.startUtc.plus(step)
        while (t.isBefore(session.endUtc)) {
            val current = altitude(t) > 0.0
            if (current != previous) out.add(t to current)
            previous = current
            t = t.plus(step)
        }
        return out
    }

    /**
     * The target windows for the named ids.
     *
     * The effective horizon is `max(drawn ridge at the target's own azimuth, 10°)`: the ridge is the
     * user's and the mask is the app's floor, and the greater of the two is what the target must clear.
     */
    fun targetWindows(
        session: NightSpan,
        point: com.gexetr.lumintralightplanner.domain.GeodeticPoint,
        ephemeris: SkyEphemeris,
        horizon: HorizonProfile?,
        objects: List<SkyObject>,
        favouriteIds: Set<String>,
        windowIds: Set<String>,
    ): Map<String, TargetWindow> {
        val ids = (favouriteIds + windowIds).take(MAX_WINDOW_TARGETS)
        if (ids.isEmpty()) return emptyMap()
        val out = LinkedHashMap<String, TargetWindow>()
        for (target in objects) {
            if (target.id !in ids) continue
            val coords = EquatorialCoords(target.raDeg, target.decDeg)
            val altitude: (Instant) -> Double = { t ->
                ephemeris.fixedHorizontal(point, coords, t, AltitudeModel.REFRACTED).altitudeDeg
            }
            val azimuth = ephemeris.fixedHorizontal(point, coords, session.instantAt(0.5), AltitudeModel.REFRACTED).azimuthDeg
            val effective = max(horizon?.altitudeAt(azimuth) ?: 0.0, TARGET_ALTITUDE_MASK_DEG)
            val min = CrossingSearch.extremum(session, findMax = false, f = altitude)
            val max = CrossingSearch.extremum(session, findMax = true, f = altitude)
            val above = CrossingSearch.intervalsAbove(session, effective, f = altitude)
            val intervals = above.map { span ->
                TargetInterval(
                    startUtc = span.startUtc,
                    endUtc = span.endUtc,
                    startBoundary = if (span.startUtc == session.startUtc) WindowBoundary.SESSION_START else WindowBoundary.RISE,
                    endBoundary = if (span.endUtc == session.endUtc) WindowBoundary.SESSION_END else WindowBoundary.SET,
                    transitUtc = CrossingSearch.extremum(span, findMax = true, f = altitude).instantUtc,
                    peakAltitudeDeg = CrossingSearch.extremum(span, findMax = true, f = altitude).value,
                    minimumAltitudeDeg = CrossingSearch.extremum(span, findMax = false, f = altitude).value,
                    effectiveHorizonDeg = effective,
                    clearsDrawnHorizon = (horizon?.altitudeAt(azimuth) ?: 0.0) >= TARGET_ALTITUDE_MASK_DEG,
                )
            }
            out[target.id] = TargetWindow.of(
                minimumAltitudeDeg = min.value,
                minimumAtInstant = min.instantUtc,
                maximumAltitudeDeg = max.value,
                effectiveHorizonDeg = effective,
                intervals = intervals,
            )
        }
        return out
    }

    /** The brief's condition list: four core conditions, plus one per favourite that is up tonight. */
    private fun conditionsOf(
        report: DarkWindowReport,
        moon: MoonState,
        moonless: MoonlessWindow,
        cloud: CloudReportHolder,
        passes: List<SatellitePass>,
        horizon: HorizonProfile?,
        spot: Scout,
    ): List<BriefCondition> {
        val conditions = ArrayList<BriefCondition>()
        conditions.add(
            BriefCondition(
                kind = ConditionKind.DARK_WINDOW,
                anchorUtc = report.darkWindows.firstOrNull()?.startUtc,
                assessment = if (report.darkWindows.isEmpty()) {
                    ConditionAssessment.Unavailable(ConditionUnavailableReason.NO_DARK_WINDOW)
                } else {
                    ConditionAssessment.Assessed(
                        if (report.kind == DarkWindowKind.DARK) ConditionState.GOOD else ConditionState.MARGINAL,
                    )
                },
                facts = BriefFacts.DarkWindow(report),
                source = ConditionSource(ConditionInput.EPHEMERIS),
            ),
        )
        conditions.add(
            BriefCondition(
                kind = ConditionKind.MOON,
                anchorUtc = moonless.extremeInstant,
                assessment = ConditionAssessment.Assessed(
                    when {
                        moonless.washedOut -> ConditionState.BLOCKING
                        moonless.hasUsableWindow -> ConditionState.GOOD
                        else -> ConditionState.MARGINAL
                    },
                ),
                facts = BriefFacts.Moon(moon, moonless),
                source = ConditionSource(ConditionInput.EPHEMERIS),
            ),
        )
        conditions.add(
            BriefCondition(
                kind = ConditionKind.CLOUD,
                anchorUtc = cloud.report?.age?.fetchedAtUtc,
                assessment = when {
                    cloud.report == null -> ConditionAssessment.Unavailable(
                        cloud.reason ?: ConditionUnavailableReason.NOT_FETCHED,
                    )
                    else -> ConditionAssessment.Assessed(cloud.state)
                },
                facts = BriefFacts.Cloud(cloud.report, cloud.meanPercent),
                source = cloud.report?.let {
                    ConditionSource(ConditionInput.FORECAST_MET_NORWAY, it.age.age, it.age.fetchedAtUtc)
                },
            ),
        )
        conditions.add(
            BriefCondition(
                kind = ConditionKind.HORIZON,
                anchorUtc = null,
                assessment = if (horizon == null) {
                    ConditionAssessment.Unavailable(ConditionUnavailableReason.HORIZON_MISSING)
                } else {
                    ConditionAssessment.Assessed(
                        if (horizon.highestDeg >= 90.0) ConditionState.BLOCKING else ConditionState.GOOD,
                    )
                },
                facts = BriefFacts.Horizon(horizon?.id ?: spot.horizonProfileId, occludesChosenTarget = false),
                source = ConditionSource(ConditionInput.HORIZON_PROFILE),
            ),
        )
        passes.firstOrNull()?.let { pass ->
            conditions.add(
                BriefCondition(
                    kind = ConditionKind.SATELLITE,
                    anchorUtc = pass.aosUtc,
                    assessment = ConditionAssessment.Assessed(
                        if (pass.observable) ConditionState.GOOD else ConditionState.MARGINAL,
                    ),
                    facts = BriefFacts.Satellite(pass),
                    source = ConditionSource(ConditionInput.ELEMENT_SET_CELESTRAK, pass.elementSetAge.age, null),
                ),
            )
        }
        return conditions
    }

    /** How many target windows one night computes. Favourites are the user's own list. */
    private const val MAX_WINDOW_TARGETS: Int = 12

    /** The fraction of the disc above which the Moon is treated as interfering. */
    private const val DARK_MOON_INTERFERENCE_FRACTION: Double = 0.10

    /**
     * The trails, from each observable pass's own ground track.
     *
     * **Honest limit, stated where it is produced.** `SatellitePass` carries a *sub-satellite ground
     * track*, not sampled topocentric directions, and no propagator is exposed to this file; the
     * observer-relative geometry is therefore derived from the ground track by the standard spherical
     * conversion (`ψ` from the observer to the sub-point, then the elevation from the right triangle
     * with the Earth's mean radius). It is a real derivation, not a random walk — and it is an
     * approximation of a track that is itself a prediction, so a streak's endpoints can be a few degrees
     * off. The element-set age is drawn beside the streak for exactly this reason.
     */
    fun trailsOf(
        passes: List<SatellitePass>,
        observer: com.gexetr.lumintralightplanner.domain.GeodeticPoint,
    ): List<ScenePassTrail> =
        passes.filter { it.observable && it.subPointTrack.size >= 2 }.map { pass ->
            val points = pass.subPointTrack.map { track ->
                SceneTrailPoint(track.instantUtc, topocentricOf(track, observer))
            }
            ScenePassTrail(
                noradId = pass.noradId,
                satelliteName = pass.satelliteName,
                visibility = pass.visibility,
                elementSetAge = pass.elementSetAge,
                segments = splitAtHorizon(points),
            )
        }

    /** Splits a sampled track into the arcs that are above the horizon, closing each at its end. */
    private fun splitAtHorizon(points: List<SceneTrailPoint>): List<List<SceneTrailPoint>> {
        val out = ArrayList<List<SceneTrailPoint>>()
        var current = ArrayList<SceneTrailPoint>()
        for (point in points) {
            if (point.position.altitudeDeg >= 0.0) {
                current.add(point)
            } else if (current.size >= 2) {
                out.add(current)
                current = ArrayList()
            } else {
                current = ArrayList()
            }
        }
        if (current.size >= 2) out.add(current)
        return out
    }

    /** The standard observer ↔ sub-satellite-point conversion. See [trailsOf] for its stated limit. */
    private fun topocentricOf(
        track: com.gexetr.lumintralightplanner.domain.GroundTrackPoint,
        observer: com.gexetr.lumintralightplanner.domain.GeodeticPoint,
    ): HorizontalCoords {
        val lat1 = Math.toRadians(track.latDeg)
        val lat2 = Math.toRadians(observer.latDeg)
        val dLon = Math.toRadians(track.lonDeg - observer.lonDeg)
        val cosPsi = (sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(dLon)).coerceIn(-1.0, 1.0)
        val psi = acos(cosPsi)
        val radius = EARTH_MEAN_RADIUS_KM + track.altitudeKm
        val elevation = Math.toDegrees(atan2(radius * cos(psi) - EARTH_MEAN_RADIUS_KM, radius * sin(psi)))
        val azimuth = Math.toDegrees(
            atan2(
                sin(dLon) * cos(lat1),
                cos(lat2) * sin(lat1) - sin(lat2) * cos(lat1) * cos(dLon),
            ),
        )
        return HorizontalCoords(
            altitudeDeg = elevation,
            azimuthDeg = HorizontalCoords.normaliseAzimuth(azimuth),
        )
    }

    /** The Earth's mean radius, for the topocentric derivation above. */
    private const val EARTH_MEAN_RADIUS_KM: Double = 6371.0088
}

/** The cloud input, as the brief needs it: the report (or its absence), the reason and the verdict. */
@Immutable
class CloudReportHolder(
    val report: com.gexetr.lumintralightplanner.domain.CloudReport?,
    val reason: ConditionUnavailableReason?,
    val meanPercent: Int?,
    val state: ConditionState,
)

/** `SessionBrief`'s own point accessor, restated here because the domain type does not carry one. */
private fun com.gexetr.lumintralightplanner.domain.SessionBrief.point() =
    com.gexetr.lumintralightplanner.domain.GeodeticPoint(latitudeDeg, longitudeDeg, 0.0)

/** The twilight LUT and the bundled imagery, as one bundle the ViewModel holds across scrubs. */
internal class SceneAssetBundle(
    val lut: TwilightLut,
    val starTable: StarTable?,
    /** Filtered once at load — never rebuilt per scrub (that path used to allocate on every frame). */
    val starField: StarField?,
    val constellationFigures: com.gexetr.lumintralightplanner.data.ConstellationFigures?,
    val milkyWay: Bitmap?,
    val moonImage: Bitmap?,
) {

    /** The assets for one instant: only the two projectors change with the instant. */
    fun at(instantUtc: Instant, point: com.gexetr.lumintralightplanner.domain.GeodeticPoint, e: SkyEphemeris): SceneAssets =
        SceneAssets(
            lut = lut,
            projector = projectorFor(instantUtc, point, e),
            starField = starField,
            constellationFigures = constellationFigures,
            milkyWay = milkyWay,
            moonImage = moonImage,
            projectorAt = { minutesAgo ->
                projectorFor(instantUtc.minusMillis((minutesAgo * 60_000.0).toLong()), point, e)
            },
            magnitudeLimit = StarField.DEFAULT_MAGNITUDE_LIMIT,
        )

    private fun projectorFor(
        instantUtc: Instant,
        point: com.gexetr.lumintralightplanner.domain.GeodeticPoint,
        e: SkyEphemeris,
    ): SkyProjector = SkyProjector { raDeg, decDeg ->
        e.fixedHorizontal(point, EquatorialCoords(raDeg, decDeg), instantUtc, AltitudeModel.REFRACTED)
    }

    companion object {

        /** Reads the bundled LUT and imagery. Every absence is a typed null, never a fallback image. */
        fun load(source: SkyAssetSource?): SceneAssetBundle {
            val table = (source?.starTable() as? com.gexetr.lumintralightplanner.data.BundledAsset.Present)?.value
            return SceneAssetBundle(
                lut = TwilightLut.load(source),
                starTable = table,
                starField = table?.let { StarField.of(it, StarField.DEFAULT_MAGNITUDE_LIMIT) },
                constellationFigures = (source?.constellationFigures()
                    as? com.gexetr.lumintralightplanner.data.BundledAsset.Present)?.value,
                milkyWay = decode(source, SkyAssetSource.MILKY_WAY_PATH, MILKY_WAY_SAMPLE),
                moonImage = decode(source, SkyAssetSource.MOON_PATH, MOON_SAMPLE),
            )
        }

        /** Subsample the equirect panorama — full decode froze first open on mid-range devices. */
        private const val MILKY_WAY_SAMPLE: Int = 4
        private const val MOON_SAMPLE: Int = 1

        /**
         * Decodes one bundled WebP.
         *
         * A missing asset returns `null` — the layer turns itself off — and a decode that fails (or an
         * allocation the device refuses) also returns `null` rather than a placeholder image, because a
         * placeholder in the Milky Way layer would be a picture the user reads as the sky.
         */
        private fun decode(source: SkyAssetSource?, path: String, sampleSize: Int): Bitmap? {
            val bytes = (source?.readBytes(path) as? com.gexetr.lumintralightplanner.data.BundledAsset.Present)?.value
                ?: return null
            return try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize.coerceAtLeast(1)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } catch (error: Throwable) {
                null
            }
        }
    }
}

// =================================================================================================
// 3. The ViewModel
// =================================================================================================

/**
 * The night scene's ViewModel.
 *
 * ## The two clocks it has, and why
 *
 * [clock] is read **once per load**, to choose which night the user is planning; the scrub then moves a
 * value inside that night and reads nothing. A scene that re-read `Instant.now()` on every draw could
 * not be memoised, and the capability's "idle draws no frames" requirement would be unsatisfiable.
 *
 * ## What it never does
 *
 * It never samples the catalogue per scrub (windows are computed once per night), never fetches during
 * a gesture, and never animates while nothing changes: a frame is produced only when the scrub moves,
 * the night's structure changes, or the **Play night** run the user started is in flight
 * ([NightUiState.playing]), which stops itself at the end of the session. The screen owns the two
 * bounded animations - the playback and the ≤ 200 ms settle - and this class owns none of them.
 */
class NightViewModel(
    private val appContext: Context,
    private val scoutStore: ScoutStore,
    private val catalogue: com.gexetr.lumintralightplanner.data.CatalogueRepository,
    private val cloudRepository: CloudRepository,
    private val ephemeris: SkyEphemeris,
    private val assetSource: SkyAssetSource,
    /**
     * The night's predicted passes, as an injected search.
     *
     * It is a parameter rather than a call because the search needs the shared Orekit warm-up, an
     * element-set fetch and a horizon mask — none of which this screen owns — and because a screen that
     * fetched element sets from its own `init` would be a network call the user never asked for. The
     * default is the honest empty answer: the trail layer then draws nothing and the brief says the
     * pass list is empty, rather than inventing a track.
     */
    private val passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass> = { _, _, _ -> emptyList() },
    /**
     * The user's own sky-brightness estimate, as the brief shows it.
     *
     * It is injected because it is a **user input, not a measurement**: the app ships no light-pollution
     * raster, so a value here can only have been typed by the user, and a `null` means the user has
     * entered none — which is a different sentence from "the sky is good".
     */
    private val brightnessEstimate: () -> com.gexetr.lumintralightplanner.domain.SkyBrightnessEstimate =
        { com.gexetr.lumintralightplanner.domain.SkyBrightnessEstimate.NONE },
    private val clock: () -> Instant = ::defaultInstant,
) : ViewModel(), NightSceneCommands {

    private val _state = MutableStateFlow(NightUiState())
    val state: StateFlow<NightUiState> = _state.asStateFlow()

    private var bundle: SceneAssetBundle? = null
    private var computation: NightComputation? = null
    private var objects: List<SkyObject> = emptyList()
    private var activeLens: com.gexetr.lumintralightplanner.domain.LensPreset? = null
    private var scrubJob: Job? = null
    private val use24Hour: Boolean = android.text.format.DateFormat.is24HourFormat(appContext)
    private val locale: Locale = appContext.resources.configuration.locales.get(0) ?: Locale.getDefault()

    init {
        // Publish loading before any asset I/O so navigation paints a spinner on the first frame.
        _state.value = NightUiState(loading = true, commands = this)
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                bundle ?: SceneAssetBundle.load(assetSource).also { bundle = it }
            }
            scoutStore.snapshot.collectLatest { snapshot -> recompute(snapshot, loaded) }
        }
    }

    /** The scrub: a pure projection of the computation already in hand. */
    fun onScrub(value: Double) {
        val current = _state.value
        val span = current.span ?: return
        val clamped = value.coerceIn(0.0, 1.0)
        val instant = span.instantAt(clamped)
        // Keep the scrub value on Main immediately so the strip tracks the finger; frame rebuild is Default.
        _state.value = current.copy(scrubValue = clamped, instantUtc = instant)
        scrubJob?.cancel()
        scrubJob = viewModelScope.launch {
            val next = withContext(Dispatchers.Default) {
                render(current.copy(scrubValue = clamped, instantUtc = instant, assets = current.assets?.let {
                    // Rebuild projectors for the new instant when a bundle is in hand.
                    val spotId = current.spotId
                    val computationLocal = computation
                    if (computationLocal != null && spotId != null) {
                        val point = computationLocal.brief.point()
                        bundle?.at(instant, point, ephemeris) ?: it
                    } else {
                        it
                    }
                }))
            }
            _state.value = next
        }
    }

    fun onSelectTarget(id: String?) {
        viewModelScope.launch {
            val next = withContext(Dispatchers.Default) {
                render(_state.value.copy(selectedTargetId = id))
            }
            _state.value = next
        }
    }

    fun onBackToHorizon() {
        viewModelScope.launch {
            val next = withContext(Dispatchers.Default) {
                render(_state.value.copy(selectedTargetId = null))
            }
            _state.value = next
        }
    }

    /**
     * The **Play night** toggle.
     *
     * It flips a flag and nothing else: the screen owns the animation, because the frames a play
     * produces are the screen's business and the model's job is to answer "what is true at this
     * instant". A play started here stops itself when the scrub reaches the end of the session.
     */
    fun onTogglePlay() {
        _state.value = _state.value.copy(playing = !_state.value.playing)
    }

    /** Star-trail mode: the same night, drawn as arcs over the interval instead of points. */
    fun onToggleTrails() {
        _state.value = _state.value.copy(trailMode = !_state.value.trailMode)
    }

    /**
     * The trail interval, restricted to the four values the capability names.
     *
     * An unknown value is **refused rather than clamped**: a fifth interval would be a number the
     * readout claims and no requirement defines, so the setter refuses it and the state keeps the
     * last legal choice.
     */
    fun onSetTrailInterval(minutes: Double) {
        if (TRAIL_INTERVALS_MIN.none { it == minutes }) return
        _state.value = _state.value.copy(trailIntervalMinutes = minutes)
    }

    // --- the frozen screen's command sink -------------------------------------------------------
    //
    // The hero screen's signature carries no callback for these three, so the state hands it this
    // object. Each method is the `on…` form above rather than a second implementation, so a control
    // reached from the chrome and a control reached from anywhere else cannot drift apart.
    override fun togglePlay() = onTogglePlay()

    override fun toggleTrails() = onToggleTrails()

    override fun setTrailInterval(minutes: Double) = onSetTrailInterval(minutes)

    fun onToggleRedMode() {
        viewModelScope.launch {
            val next = withContext(Dispatchers.Default) {
                render(_state.value.copy(redMode = !_state.value.redMode))
            }
            _state.value = next
        }
    }

    /** Re-reads the external inputs. The only path that touches the network, and never a gesture. */
    fun onRefreshCloud() {
        viewModelScope.launch {
            val snapshot = scoutStore.snapshot.first()
            recompute(snapshot, bundle ?: return@launch, refreshCloud = true)
        }
    }

    private suspend fun recompute(snapshot: ScoutSnapshot, loaded: SceneAssetBundle, refreshCloud: Boolean = false) {
        val spot = com.gexetr.lumintralightplanner.domain.ScoutSelection.resolve(snapshot.spots, snapshot.activeSpotId)
        if (spot == null) {
            computation = null
            _state.value = NightUiState(
                loading = false,
                briefState = BriefState.NoSpot,
                redMode = _state.value.redMode,
                // A play has nothing to advance when there is no session, so it stops here rather
                // than leaving a toggle lit over an empty screen.
                playing = false,
                trailMode = _state.value.trailMode,
                trailIntervalMinutes = _state.value.trailIntervalMinutes,
                commands = this,
            )
            return
        }
        // First open only: keep the previous frame on screen while Default refreshes.
        // Mid-session loading=true + frame=null swapped the hero for a spinner and looked like thrash.
        val base = _state.value
        val horizonPreview = spot.horizonProfileId?.let { snapshot.horizons[it] }
        val firstLoad = base.frame == null && computation == null
        _state.value = base.copy(
            loading = firstLoad,
            spotId = spot.id,
            spotName = spot.name,
            horizonDrawn = horizonPreview != null,
            horizonBlocksSky = (horizonPreview?.highestDeg ?: 0.0) >= 90.0,
            commands = this,
        )

        val assembled = withContext(Dispatchers.Default) {
            val horizon = horizonPreview
            val now = clock()
            val zone = ZoneId.of(spot.tzId)
            val nightDate = SessionClock.nightOf(now, zone)
            val session = SessionClock.sessionWindow(nightDate, zone, spot.id)

            val catalogueResult = catalogue.load()
            val initialObjects = catalogueObjects(catalogueResult)
            objects = initialObjects

            val cloud = if (refreshCloud) cloudRepository.fetch(spot.networkPoint, session, now) else null
            val cloudReport = cloudReportOf(cloud, cloudRepository, spot, session, now)
            val cloudHolder = holderOf(cloud, cloudReport, session)
            val estimate = brightnessEstimate()

            val passes = passProvider(spot, session, now)
            val elementAge = passes.firstOrNull()?.elementSetAge
            val lens = spot.equipment.toLensPreset()
            activeLens = lens

            val night = NightEngine.compute(
                spot = spot,
                horizon = horizon,
                nightDate = nightDate,
                ephemeris = ephemeris,
                objects = initialObjects,
                cloud = cloudHolder,
                passes = passes,
                elementSetAge = elementAge,
                favouriteIds = snapshot.favourites,
                windowIds = snapshot.favourites,
                notices = emptyList(),
                names = bodyNames(),
            )
            computation = night
            val strip = stripOf(night.brief, loaded.lut, spot, ephemeris)
            val scrub = base.scrubValue.takeIf { base.spotId == spot.id } ?: defaultScrub(night.brief)
            val focusedInstant = night.brief.session.instantAt(scrub)
            render(
                NightUiState(
                    loading = false,
                    briefState = BriefState.Ready(night.brief),
                    spotId = spot.id,
                    spotName = spot.name,
                    nightLabel = nightLabelOf(night.brief, zone),
                    zoneId = spot.tzId,
                    spanStartUtc = night.brief.session.startUtc,
                    spanEndUtc = night.brief.session.endUtc,
                    scrubValue = scrub,
                    instantUtc = focusedInstant,
                    assets = loaded.at(focusedInstant, spot.point, ephemeris),
                    strip = strip,
                    horizonDrawn = horizon != null,
                    horizonBlocksSky = (horizon?.highestDeg ?: 0.0) >= 90.0,
                    redMode = base.redMode,
                    selectedTargetId = base.selectedTargetId,
                    lensFocalMm = lens.focalLengthMm,
                    lensFovHorizontalDeg = com.gexetr.lumintralightplanner.domain.FieldOfView.of(lens).fovH,
                    lensFovVerticalDeg = com.gexetr.lumintralightplanner.domain.FieldOfView.of(lens).fovV,
                    skyLevel = night.brief.skyGrade.level,
                    userEstimateSqm = estimate.sqm,
                    userEstimateBortle = estimate.bortle,
                    darkWindowKind = night.brief.darkWindow.kind,
                    darkWindowStartUtc = night.brief.darkWindow.darkWindows.firstOrNull()?.startUtc,
                    darkWindowEndUtc = night.brief.darkWindow.darkWindows.firstOrNull()?.endUtc,
                    darkWindowSecondStartUtc = night.brief.darkWindow.darkWindows.getOrNull(1)?.startUtc,
                    darkWindowSecondEndUtc = night.brief.darkWindow.darkWindows.getOrNull(1)?.endUtc,
                    moonFraction = night.brief.moon.illuminatedFraction,
                    moonAltitudeDeg = night.brief.moon.altitudeDeg,
                    moonPhase = night.brief.moon.phaseName,
                    playing = base.playing,
                    trailMode = base.trailMode,
                    trailIntervalMinutes = base.trailIntervalMinutes,
                    commands = this@NightViewModel,
                    notices = noticesOf(night.brief, loaded, cloud, catalogueResult),
                ),
            )
        }
        _state.value = assembled
    }

    /** The per-instant projection: the frame, its rows and the state's derived labels. */
    private fun render(base: NightUiState): NightUiState {
        val night = computation ?: return base
        val instant = base.instantUtc ?: return base
        val assets = base.assets ?: return base
        val lens = lensOf()
        val frame = night.sceneAt(instant, ephemeris, assets.lut, lens, assets, base.selectedTargetId, base.redMode)
        val rows = night.rowsAt(frame, ephemeris)
        val zone = ZoneId.of(night.brief.tzId)
        return base.copy(
            instantUtc = instant,
            frame = frame,
            rows = rows,
            assets = assets,
            sceneSummary = sceneSummaryOf(night.brief, frame, zone),
        )
    }

    private fun lensOf(): com.gexetr.lumintralightplanner.domain.LensPreset =
        activeLens ?: com.gexetr.lumintralightplanner.domain.LensPreset(
            name = "",
            sensorWidthMm = DEFAULT_SENSOR_WIDTH_MM,
            sensorHeightMm = DEFAULT_SENSOR_HEIGHT_MM,
            focalLengthMm = DEFAULT_FOCAL_MM,
        )

    private fun nightLabelOf(brief: com.gexetr.lumintralightplanner.domain.SessionBrief, zone: ZoneId): String =
        formatNightLabel(brief.session.startUtc, brief.session.endUtc, zone, locale)

    private fun defaultScrub(brief: com.gexetr.lumintralightplanner.domain.SessionBrief): Double {
        val dark = NightSpan.longest(brief.darkWindow.darkWindows) ?: return 0.5
        return brief.session.fractionOf(dark.startUtc)
    }

    private fun sceneSummaryOf(
        brief: com.gexetr.lumintralightplanner.domain.SessionBrief,
        frame: SceneFrame,
        zone: ZoneId,
    ): String {
        val time = formatTime(frame.instantUtc, zone, locale, use24Hour)
        val head = appContext.getString(R.string.night_summary_head, brief.spotName, time)
        val where = appContext.getString(
            R.string.night_summary_direction,
            formatDegrees(frame.camera.centerAzimuthDeg),
            formatSignedDegrees(frame.sun.position.altitudeDeg),
        )
        val moon = appContext.getString(
            R.string.night_summary_moon,
            formatPercent(frame.moon.illuminatedFraction ?: 0.0),
            formatSignedDegrees(frame.moon.position.altitudeDeg),
        )
        return "$head. $where. $moon"
    }

    /** The bodies' display names, from resources rather than from literals in the engine. */
    private fun bodyNames(): BodyNames = BodyNames(
        sun = appContext.getString(R.string.night_body_sun),
        moon = appContext.getString(R.string.share_phase_moon),
        planets = SolarSystemBody.entries.associateWith { body ->
            appContext.getString(planetNameRes(body))
        },
    )

    private fun planetNameRes(body: SolarSystemBody): Int = when (body) {
        SolarSystemBody.MERCURY -> R.string.night_planet_mercury
        SolarSystemBody.VENUS -> R.string.night_planet_venus
        SolarSystemBody.MARS -> R.string.night_planet_mars
        SolarSystemBody.JUPITER -> R.string.night_planet_jupiter
        SolarSystemBody.SATURN -> R.string.night_planet_saturn
        SolarSystemBody.URANUS -> R.string.night_planet_uranus
        SolarSystemBody.NEPTUNE -> R.string.night_planet_neptune
    }

    /** The catalogue's rows, or nothing at all — an unreadable catalogue is a notice, not a fake sky. */
    private fun catalogueObjects(
        result: com.gexetr.lumintralightplanner.data.CatalogueResult,
    ): List<SkyObject> = when (result) {
        is com.gexetr.lumintralightplanner.data.CatalogueResult.Success -> result.objects
        is com.gexetr.lumintralightplanner.data.CatalogueResult.Unreadable -> emptyList()
    }

    /**
     * The report to work from: the fresh one, the cached one a failure carried, or the last cached
     * reading for the window. A report that covers no hour of the window is `null` rather than a zero.
     */
    private fun cloudReportOf(
        outcome: CloudResult?,
        repository: CloudRepository,
        spot: Scout,
        session: NightSpan,
        now: Instant,
    ): com.gexetr.lumintralightplanner.domain.CloudReport? = when (outcome) {
        is CloudResult.Available -> outcome.report
        is CloudResult.Unavailable -> outcome.lastReading
            ?: repository.cachedReading(spot.networkPoint, session, now)
        null -> repository.cachedReading(spot.networkPoint, session, now)
    }

    /**
     * The cloud input as the brief needs it.
     *
     * The mean is taken across the **session window** — a report with no hour inside it leaves the mean
     * `null` rather than at zero, because a zero would read as a clear sky the source never forecast.
     * `NOT_FETCHED` is its own reason: not asking is not a failure.
     */
    private fun holderOf(
        outcome: CloudResult?,
        report: com.gexetr.lumintralightplanner.domain.CloudReport?,
        session: NightSpan,
    ): CloudReportHolder {
        val mean = report?.meanCloudPercentWithin(session)
        val reason = when {
            report != null -> null
            outcome is CloudResult.Unavailable && outcome.state == CloudFailure.SOURCE_DISABLED ->
                ConditionUnavailableReason.SOURCE_DISABLED
            outcome is CloudResult.Unavailable && outcome.state == CloudFailure.NO_HOURS_IN_WINDOW ->
                ConditionUnavailableReason.NO_DARK_WINDOW
            outcome is CloudResult.Unavailable -> ConditionUnavailableReason.OFFLINE
            else -> ConditionUnavailableReason.NOT_FETCHED
        }
        val state = when {
            report == null -> ConditionState.MARGINAL
            report.age.isStale -> ConditionState.MARGINAL
            (mean ?: 0) >= CLOUD_BLOCKING_PERCENT -> ConditionState.BLOCKING
            else -> ConditionState.GOOD
        }
        return CloudReportHolder(report = report, reason = reason, meanPercent = mean, state = state)
    }

    private fun noticesOf(
        brief: com.gexetr.lumintralightplanner.domain.SessionBrief,
        bundle: SceneAssetBundle,
        cloud: CloudResult?,
        catalogueResult: com.gexetr.lumintralightplanner.data.CatalogueResult,
    ): List<NightNotice> {
        val out = ArrayList<NightNotice>()
        if (bundle.lut.isFallback) out.add(NightNotice.SkyTableFallback)
        if (!_state.value.horizonDrawn) out.add(NightNotice.HorizonFlat)
        if (_state.value.horizonBlocksSky) out.add(NightNotice.HorizonBlocksSky)
        when (brief.darkWindow.kind) {
            DarkWindowKind.POLAR_DAY -> out.add(NightNotice.PolarDay)
            DarkWindowKind.DARK, DarkWindowKind.POLAR_NIGHT -> Unit
            else -> out.add(NightNotice.NoDarkness(brief.darkWindow.kind))
        }
        if (catalogueResult is com.gexetr.lumintralightplanner.data.CatalogueResult.Unreadable) {
            out.add(NightNotice.CatalogueUnreadable)
        }
        brief.elementSetAge?.let { age ->
            if (age.showBadge) out.add(NightNotice.ElementSetStale(age.ageDays.toLong()))
        }
        if (cloud is CloudResult.Unavailable && cloud.lastReading == null) out.add(NightNotice.CloudUnavailable)
        return out
    }

    private fun stripOf(
        brief: com.gexetr.lumintralightplanner.domain.SessionBrief,
        lut: TwilightLut,
        spot: Scout,
        ephemeris: SkyEphemeris,
    ): TimelineStripModel {
        val sun = sunAltitude(spot.point, ephemeris)
        return TimelineStripSampler.sample(
            session = brief.session,
            lut = lut,
            sunAltitudeDeg = sun,
            darkWindows = brief.darkWindow.darkWindows,
            moonlessWindows = brief.moonless.intervals,
            moonHorizontal = moonAltitude(spot.point, ephemeris),
            cloud = brief.cloud,
        )
    }

    companion object {

        /** Above this mean cloud the condition is BLOCKING rather than MARGINAL. */
        const val CLOUD_BLOCKING_PERCENT: Int = 80

        /** The 24 mm full-frame preset, as the numbers `LensPreset` needs, for the pre-load frame. */
        const val DEFAULT_SENSOR_WIDTH_MM: Double = 36.0
        const val DEFAULT_SENSOR_HEIGHT_MM: Double = 24.0
        const val DEFAULT_FOCAL_MM: Double = 24.0

        /**
         * Builds the factory for the screen.
         *
         * The dependencies are taken explicitly — no Hilt, no Koin, no service locator — because the
         * factory is not a dependency of the app's own graph and a ViewModel that resolved its own
         * collaborators could not be tested with a fake ephemeris.
         */
        fun factory(
            appContext: Context,
            scoutStore: ScoutStore,
            catalogue: com.gexetr.lumintralightplanner.data.CatalogueRepository,
            cloudRepository: CloudRepository,
            assetSource: SkyAssetSource = SkyAssetSource(appContext),
            ephemeris: SkyEphemeris = AstronomySkyEphemeris(),
            passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass> = { _, _, _ -> emptyList() },
            brightnessEstimate: () -> com.gexetr.lumintralightplanner.domain.SkyBrightnessEstimate =
                { com.gexetr.lumintralightplanner.domain.SkyBrightnessEstimate.NONE },
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NightViewModel(
                    appContext = appContext,
                    scoutStore = scoutStore,
                    catalogue = catalogue,
                    cloudRepository = cloudRepository,
                    ephemeris = ephemeris,
                    assetSource = assetSource,
                    passProvider = passProvider,
                    brightnessEstimate = brightnessEstimate,
                )
            }
        }
    }
}

// =================================================================================================
// 4. The screen
// =================================================================================================

/**
 * The hero surface: the night scene, its instrument readout and its 96-stop time strip.
 *
 * The scene itself is one `Canvas` ([SceneCanvas]) drawing the 13 layers in the capability's order; the
 * chrome around it — the header, the layer legend, the readout, the strip and the "in this frame"
 * panel — is Compose here, **outside the scene rect**, which is what layer 13 means.
 */
@Composable
fun NightSceneScreen(
    state: NightUiState,
    onScrub: (Double) -> Unit,
    onSelectTarget: (String?) -> Unit,
    onBackToHorizon: () -> Unit,
    onToggleRedMode: () -> Unit,
    onOpenBrief: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LumintraTheme.colors
    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp >= 600

    // --- the only two animations this screen owns -------------------------------------------------
    //
    // `scrubAnim` holds the value the scene draws while a gesture is being *eased* (a tap-advance, a
    // release settle) and while the Play-night run is in flight. A **drag never touches it**: the drag
    // writes `onScrub` directly, so the value the pointer produced is the value in the drawn frame and
    // no interpolation sits between them. `wipe` is the scene's discontinuous-frame transition, and
    // nothing else in this screen produces frames: when both are idle the scene draws none.
    val scrubAnim = remember { Animatable(state.scrubValue.toFloat()) }
    val wipe = remember { Animatable(0f) }
    val settleScope = rememberCoroutineScope()
    val motionActive = scrubAnim.isRunning || wipe.isRunning

    // A new night, or an instant restored after process death, is a **snap** and not an animation.
    // The effect is keyed on the structure rather than on the value, so a drag does not relaunch it on
    // every frame it writes.
    LaunchedEffect(state.spotId, state.spanStartUtc) {
        if (!scrubAnim.isRunning) scrubAnim.snapTo(state.scrubValue.toFloat())
    }

    // Play night: one bounded run from where the scrub is to the end of the session, and then it
    // stops. It is a tween rather than a loop for exactly that reason - the frames end with it, so a
    // stopped play returns the scene to zero frames per second instead of leaving a clock running.
    LaunchedEffect(state.playing) {
        if (!state.playing) {
            // Pause / end: kill any leftover Animatable so motionActive cannot stay hot.
            if (scrubAnim.isRunning) scrubAnim.stop()
            return@LaunchedEffect
        }
        val remainingMs = ((1f - scrubAnim.value) * PLAY_NIGHT_DURATION_MS).toInt().coerceAtLeast(1)
        scrubAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = remainingMs, easing = LinearEasing),
        ) {
            onScrub(value.toDouble())
        }
        // Reaching the end is what stops the play: the screen never leaves the toggle lit over a run
        // that has already finished.
        state.commands.togglePlay()
    }

    // A discontinuous frame identity - the selected target, the lens, the red mode - wipes the
    // outgoing sky away over a bounded 200 ms. It is deliberately *not* driven by the scrub: a wipe on
    // every scrub frame would put an interpolation between the pointer and the pixels, which is the
    // one thing the scrub requirement forbids. The first composition is skipped, so opening the screen
    // is not a transition.
    val frameIdentity = FrameIdentity(state.selectedTargetId, state.lensFocalMm, state.redMode)
    val wipeArmed = remember { mutableStateOf(false) }
    LaunchedEffect(frameIdentity) {
        if (!wipeArmed.value) {
            wipeArmed.value = true
            return@LaunchedEffect
        }
        wipe.snapTo(1f)
        wipe.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = EmphasizedDecelerate),
        )
    }

    // The settle and the tap-advance are the same bounded easing, and `Animatable` guarantees there is
    // never more than one of them: starting a new animation cancels the running one, so a second tap
    // or a release mid-flight cannot leave two animations driving one value.
    val easeTo: (Double) -> Unit = { target ->
        settleScope.launch {
            scrubAnim.animateTo(
                targetValue = target.toFloat().coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = SETTLE_DURATION_MS, easing = EmphasizedDecelerate),
            ) {
                onScrub(value.toDouble())
            }
        }
    }
    val onDrag: (Double) -> Unit = { value ->
        // A hand on the strip stops a running play, exactly as it would stop a transport.
        if (state.playing) state.commands.togglePlay()
        settleScope.launch { scrubAnim.snapTo(value.toFloat().coerceIn(0f, 1f)) }
        onScrub(value)
    }
    val onRelease: () -> Unit = {
        // The settle target is the nearest strip stop: a bounded move to a whole stop of the 96 the
        // strip is drawn from, never an unbounded glide.
        val stops = (TimelineStripModel.STOP_COUNT - 1).toFloat()
        easeTo((round(scrubAnim.value * stops) / stops).toDouble())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LumintraScaffoldHeader(
            title = stringResource(R.string.night_title),
            onBack = onOpenBrief,
            actions = {
                if (state.hasSelectedTarget) {
                    IconButton(
                        onClick = onBackToHorizon,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui_locate_fixed),
                            contentDescription = stringResource(R.string.night_back_to_horizon),
                        )
                    }
                }
                IconButton(
                    onClick = onOpenBrief,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_calendar),
                        contentDescription = stringResource(R.string.brief_title),
                    )
                }
            },
        )

        when {
            state.briefState is BriefState.NoSpot && !state.loading -> LumintraEmptyState(
                title = stringResource(R.string.brief_no_spot_title),
                body = stringResource(R.string.brief_no_spot_body),
                iconRes = R.drawable.ic_ui_map_pin,
                modifier = Modifier.padding(16.dp),
            )
            // Keep the scene up while refreshing: loading alone must not hide an existing frame.
            state.frame == null || state.assets == null ->
                LoadingPanel(spotName = state.spotName)
            else -> {
                val frame = state.frame
                val assets = state.assets
                if (frame == null || assets == null) {
                    LoadingPanel(spotName = state.spotName)
                } else {
                    if (wide) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                        ) {
                            ScenePane(
                                state = state,
                                frame = frame,
                                assets = assets,
                                motionActive = motionActive,
                                wipeFraction = wipe.value.takeIf { wipe.isRunning },
                                onScrub = onDrag,
                                onScrubEased = easeTo,
                                onScrubRelease = onRelease,
                                onSelectTarget = onSelectTarget,
                                onToggleRedMode = onToggleRedMode,
                                modifier = Modifier.weight(1.4f),
                            )
                            SidePanel(
                                state = state,
                                onScrub = onDrag,
                                onScrubEased = easeTo,
                                onScrubRelease = onRelease,
                                onSelectTarget = onSelectTarget,
                                onToggleRedMode = onToggleRedMode,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                scrollable = true,
                            )
                        }
                    } else {
                        // Phone: capped sky + scrollable strip/objects; Play / trails / red stay
                        // pinned above the nav bar so they are never buried under a long object list.
                        val sceneHeight = (configuration.screenHeightDp * 0.36f)
                            .coerceIn(200f, 300f)
                            .dp
                        Column(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ScenePane(
                                    state = state,
                                    frame = frame,
                                    assets = assets,
                                    motionActive = motionActive,
                                    wipeFraction = wipe.value.takeIf { wipe.isRunning },
                                    onScrub = onDrag,
                                    onScrubEased = easeTo,
                                    onScrubRelease = onRelease,
                                    onSelectTarget = onSelectTarget,
                                    onToggleRedMode = onToggleRedMode,
                                    modifier = Modifier.fillMaxWidth(),
                                    sceneHeight = sceneHeight,
                                )
                                SidePanel(
                                    state = state,
                                    onScrub = onDrag,
                                    onScrubEased = easeTo,
                                    onScrubRelease = onRelease,
                                    onSelectTarget = onSelectTarget,
                                    onToggleRedMode = onToggleRedMode,
                                    modifier = Modifier.fillMaxWidth(),
                                    scrollable = false,
                                    // Trails / red stay in the scroll; only Play is pinned.
                                    showControls = true,
                                    pinPlayOutside = true,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            PlayNightButton(
                                state = state,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    // Same A40 gesture-strip safety as the spot Save footer.
                                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 48.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingPanel(spotName: String = "") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        if (spotName.isNotBlank()) {
            Text(
                text = spotName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = stringResource(R.string.state_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The scene rect and everything that belongs to it.
 *
 * The tap overlay is *above* the canvas and holds no pixels of its own: a tap resolves to the nearest
 * row's tap point, which is a value the projection already produced. Nothing in the scene reads
 * `LayoutDirection` — the azimuth axis increases to the right in every locale — while the chrome around
 * it mirrors with the layout, which is the capability's own split.
 */
@Composable
private fun ScenePane(
    state: NightUiState,
    frame: SceneFrame,
    assets: SceneAssets,
    motionActive: Boolean,
    wipeFraction: Float?,
    onScrub: (Double) -> Unit,
    onScrubEased: (Double) -> Unit,
    onScrubRelease: () -> Unit,
    onSelectTarget: (String?) -> Unit,
    onToggleRedMode: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * When set (phone scroll layout), the sky is this tall and the column does not use
     * [Modifier.weight] — weight only works inside a height-bounded parent. When null (wide
     * pane), the canvas expands with weight to fill the pane beside the side panel.
     */
    sceneHeight: Dp? = null,
) {
    val palette = LumintraTheme.colors
    val context = LocalContext.current
    val labels = rememberSceneLabels(context)
    val frameRef = rememberUpdatedState(frame)
    val holder = remember { { frameRef.value } }
    // The scene rect and the readout are two rows of one column: the readout is chrome, and chrome
    // laid over the rect would cover the horizon line, the target marker or a pass streak - the
    // capability's "no control covers the thing being planned" is a defect, not a preference.
    Column(modifier = modifier.fillMaxWidth()) {
        val canvasModifier = if (sceneHeight != null) {
            Modifier
                .fillMaxWidth()
                .height(sceneHeight)
        } else {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        }
        Box(
            modifier = canvasModifier.background(palette.skyZenith),
        ) {
            SceneCanvas(
                frame = holder,
                assets = assets,
                labels = labels,
                modifier = Modifier.fillMaxSize(),
                summary = state.sceneSummary,
                view = SceneView(
                    motionActive = motionActive,
                    trailMode = state.trailMode,
                    wipeFraction = wipeFraction,
                    trailIntervalMinutes = state.trailIntervalMinutes,
                    scrubValue = state.scrubValue,
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.rows) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val height = size.height.toFloat().coerceAtLeast(1f)
                            onSelectTarget(nearestRow(state.rows, offset.x / width, offset.y / height)?.id)
                        }
                    },
            )
        }
        InstrumentReadout(state = state, frame = frame)
    }
}

@Composable
private fun rememberSceneLabels(context: Context): SceneLabels {
    val configuration = context.resources.configuration
    val locale = configuration.locales.get(0) ?: Locale.getDefault()
    return SceneLabels(
        locale = locale,
        targetLabelFormat = stringResource(R.string.night_target_label),
        trailAgeFormat = stringResource(R.string.night_trail_age),
        ageText = { hours ->
            val days = (hours / 24.0).toLong().coerceAtLeast(0L)
            context.resources.getQuantityString(
                R.plurals.pass_element_age_days,
                days.toInt().coerceAtLeast(1),
                days,
            )
        },
        unknownSizeLabel = stringResource(R.string.target_size_unknown),
        belowRidgeLabel = stringResource(R.string.target_occluded_short),
        esoCredit = stringResource(R.string.night_eso_credit),
    )
}

/**
 * The instrument readout: the four numbers a photographer acts on.
 *
 * It is the layer-13 chrome, so it lives outside the scene rect and is drawn over the rect's edge rather
 * than into the sky: a readout painted inside the field would be photographed as part of the frame.
 */
@Composable
private fun InstrumentReadout(
    state: NightUiState,
    frame: SceneFrame,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.of(state.zoneId.ifEmpty { ZoneId.systemDefault().id })
    val use24Hour = rememberUse24Hour()
    val locale = Locale.getDefault()
    // Full-width ValueRows — not three HeroStat columns. Narrow three-up columns wrap long
    // locale labels into a letter-per-line crush; a label/value row keeps each string on one axis.
    LumintraPanel(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = formatTime(frame.instantUtc, zone, locale, use24Hour),
                style = LumintraType.clock,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            frame.twilightBand?.let { band ->
                ConditionChip(
                    text = twilightLabel(band),
                    state = ChipState.MARGINAL,
                )
            }
        }
        ValueRow(
            label = stringResource(R.string.night_readout_azimuth),
            value = formatDegrees(frame.viewingAzimuthDeg),
            modifier = Modifier.fillMaxWidth(),
        )
        ValueRow(
            label = stringResource(R.string.night_readout_air_mass),
            value = String.format(locale, "%.2f", frame.airMassAtCenter),
            modifier = Modifier.fillMaxWidth(),
        )
        ValueRow(
            label = stringResource(R.string.night_readout_sun),
            value = formatSignedDegrees(frame.sun.position.altitudeDeg),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.trailMode) {
            ValueRow(
                label = stringResource(R.string.night_trail_interval_readout),
                value = stringResource(R.string.night_trail_interval_option, state.trailIntervalMinutes.toInt()),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun twilightLabel(kind: TwilightKind): String = when (kind) {
    TwilightKind.CIVIL -> stringResource(R.string.brief_twilight_civil)
    TwilightKind.NAUTICAL -> stringResource(R.string.brief_twilight_nautical)
    TwilightKind.ASTRONOMICAL -> stringResource(R.string.share_text_twilight_astronomical_short)
}

/** The panel beside (or under) the scene: strip, controls, rows and notices. */
@Composable
private fun SidePanel(
    state: NightUiState,
    onScrub: (Double) -> Unit,
    onScrubEased: (Double) -> Unit,
    onScrubRelease: () -> Unit,
    onSelectTarget: (String?) -> Unit,
    onToggleRedMode: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Wide layout scrolls this panel alone. Phone layout already scrolls the parent past the
     * capped sky — nesting [verticalScroll] there would steal gestures and clip controls.
     */
    scrollable: Boolean = true,
    /** When false, omit all scene controls (wide can pass true). */
    showControls: Boolean = true,
    /** Phone sticky footer owns Play; panel keeps trails/red only. */
    pinPlayOutside: Boolean = false,
) {
    val use24Hour = rememberUse24Hour()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.strip?.let { strip ->
            SectionStrip(title = stringResource(R.string.night_scrub_label))
            val zone = ZoneId.of(state.zoneId.ifEmpty { ZoneId.systemDefault().id })
            val locale = Locale.getDefault()
            TimelineStrip(
                model = strip,
                scrubValue = state.scrubValue,
                onScrub = onScrub,
                onScrubRelease = onScrubRelease,
                onScrubEased = onScrubEased,
                modifier = Modifier.padding(horizontal = 4.dp),
                contentDescription = stringResource(
                    R.string.night_scrub_content_description,
                    formatTime(strip.session.startUtc, zone, locale, use24Hour),
                    formatTime(strip.session.endUtc, zone, locale, use24Hour),
                ),
            )
        }

        if (showControls) {
            SceneControls(
                state = state,
                onToggleRedMode = onToggleRedMode,
                includePlay = !pinPlayOutside,
            )
        }

        SectionStrip(title = stringResource(R.string.night_objects_header))
        if (state.rows.isEmpty()) {
            Text(
                text = stringResource(R.string.night_no_objects),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            for (row in state.rows) {
                ValueRow(
                    label = row.designation,
                    value = stringResource(
                        R.string.night_object_row,
                        row.name,
                        formatDegrees(row.altitudeDeg),
                        formatDegrees(row.azimuthDeg),
                    ),
                    secondary = if (row.behindHorizon) stringResource(R.string.target_occluded_short) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTarget(row.id) }
                        .sizeIn(minHeight = 48.dp),
                )
            }
        }

        if (state.notices.isNotEmpty()) {
            SectionStrip(title = stringResource(R.string.night_layers_header))
            for (notice in state.notices) {
                NoticeLine(notice)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * The scene's named controls: the **Play night** toggle, the **star trails** switch, the trail
 * interval selector and the **red night mode** switch.
 *
 * Every one of them is chrome, so it is laid out **outside** the scene rect - a control over the field
 * would cover the target marker, the lens frame or the ridge it is being used to plan against. Each is
 * at least 48 dp in both dimensions, each carries a role (`button`, `switch`, `radio button`), and no
 * state here is carried by colour alone: the play toggle's own text alternates between play and pause,
 * and the switches carry their labels.
 *
 * The four trail intervals are the capability's own set ([TRAIL_INTERVALS_MIN]) and the selector is a
 * radio group rather than a slider, because the set is discrete and a slider would imply values the
 * trail layer cannot draw.
 */
@Composable
private fun PlayNightButton(
    state: NightUiState,
    modifier: Modifier = Modifier,
) {
    val playLabel = if (state.playing) {
        stringResource(R.string.night_play_pause)
    } else {
        stringResource(R.string.night_play_start)
    }
    Button(
        onClick = { state.commands.togglePlay() },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { contentDescription = playLabel },
    ) {
        Text(
            text = playLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SceneControls(
    state: NightUiState,
    onToggleRedMode: () -> Unit,
    modifier: Modifier = Modifier,
    includePlay: Boolean = true,
) {
    val commands = state.commands
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (includePlay) {
            PlayNightButton(state = state)
        }
        SwitchRow(
            label = stringResource(R.string.night_trails_label),
            checked = state.trailMode,
            onCheckedChange = { commands.toggleTrails() },
            modifier = Modifier.fillMaxWidth(),
        )
        SwitchRow(
            label = stringResource(R.string.night_red_mode_toggle),
            checked = state.redMode,
            onCheckedChange = { onToggleRedMode() },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.trailMode) {
            Text(
                text = stringResource(R.string.night_trail_interval_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Horizontally scrollable radio group: four options in a longer locale must not wrap
            // into a column of two words.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (minutes in TRAIL_INTERVALS_MIN) {
                    val selected = minutes == state.trailIntervalMinutes
                    val intervalLabel =
                        stringResource(R.string.night_trail_interval_option, minutes.toInt())
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { contentDescription = intervalLabel }
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { commands.setTrailInterval(minutes) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        ConditionChip(
                            text = intervalLabel,
                            state = if (selected) ChipState.GOOD else ChipState.NEUTRAL,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A labelled two-state control.
 *
 * The whole row is the switch: the `Switch` itself is given no change handler, so the tap target is
 * the row and TalkBack announces one control instead of a label beside a second one. The row is at
 * least 48 dp tall, and red night-vision mode recolours it with the rest of the chrome because the
 * `Switch` takes its colours from the theme rather than from a literal here.
 */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .sizeIn(minHeight = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** The frame's discontinuous identity: the three inputs a wipe is allowed to run for. */
private data class FrameIdentity(
    val targetId: String?,
    val focalLengthMm: Double,
    val redMode: Boolean,
)

/**
 * The four trail intervals the capability names, in minutes.
 *
 * The selector offers exactly this set and [NightSceneCommands.setTrailInterval] refuses anything
 * else, so the readout can never state an interval the trail layer does not draw.
 */
internal val TRAIL_INTERVALS_MIN: List<Double> = listOf(15.0, 30.0, 60.0, 120.0)

/** The trail interval a fresh scene starts on. */
internal const val TRAIL_INTERVAL_DEFAULT_MIN: Double = 30.0

/** A release settle and a tap-advance: one bounded easing, capped by the capability at 200 ms. */
private const val SETTLE_DURATION_MS: Int = 180

/** One pass of the Play-night run over the whole session. */
private const val PLAY_NIGHT_DURATION_MS: Float = 12000f

/** The scene's discontinuous-frame wipe, also capped by the capability at 200 ms. */
private const val TRANSITION_DURATION_MS: Int = 180

/**
 * Material's **emphasized-decelerate**, written as its own curve.
 *
 * It is spelled out rather than imported because the capability names this curve by name and the
 * pin in `AGENTS.md` deliberately excludes the M3 Expressive / `MotionScheme` artifacts that carry it:
 * the constants above and below this line are the one place the easing exists.
 */
private val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
private fun NoticeLine(notice: NightNotice) {
    val text = when (notice) {
        NightNotice.SkyTableFallback -> stringResource(R.string.night_lut_fallback_note)
        NightNotice.HorizonFlat -> stringResource(R.string.night_horizon_flat_note)
        NightNotice.HorizonBlocksSky -> stringResource(R.string.night_horizon_blocked_note)
        NightNotice.PolarDay -> stringResource(R.string.night_polar_day_note)
        is NightNotice.NoDarkness -> stringResource(R.string.night_no_darkness_note)
        NightNotice.CatalogueUnreadable -> stringResource(R.string.target_list_unavailable_body)
        is NightNotice.ElementSetStale -> stringResource(R.string.pass_age_days, notice.days)
        NightNotice.CloudUnavailable -> stringResource(R.string.cloud_unavailable)
    }
    val chipState = when (notice) {
        NightNotice.HorizonBlocksSky, NightNotice.CatalogueUnreadable -> ChipState.BLOCKING
        NightNotice.SkyTableFallback, NightNotice.HorizonFlat,
        NightNotice.PolarDay, is NightNotice.NoDarkness,
        is NightNotice.ElementSetStale, NightNotice.CloudUnavailable,
        -> ChipState.MARGINAL
    }
    // One element, not a sentence plus a chip that repeats it: the chip carries the text and the state.
    ConditionChip(text = text, state = chipState, modifier = Modifier.padding(horizontal = 4.dp))
}

/** The row whose tap point is nearest a tap, with a generous catch radius on a phone. */
internal fun nearestRow(rows: List<NightObjectRow>, xFraction: Float, yFraction: Float): NightObjectRow? {
    var best: NightObjectRow? = null
    var bestDistance = Float.MAX_VALUE
    for (row in rows) {
        val dx = row.xFraction - xFraction
        val dy = row.yFraction - yFraction
        val distance = dx * dx + dy * dy
        if (distance < bestDistance) {
            bestDistance = distance
            best = row
        }
    }
    return best?.takeIf { bestDistance <= MAX_TAP_DISTANCE_SQUARED }
}

/** `(0.12)²`: a tap within about a ninth of the rect selects the nearest row. */
private const val MAX_TAP_DISTANCE_SQUARED: Float = 0.0144f

/** The 12- or 24-hour choice, read from the device once per composition of the chrome. */
@Composable
private fun rememberUse24Hour(): Boolean {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) { android.text.format.DateFormat.is24HourFormat(context) }
}

/** The palette the strip and the chrome share, so a legend cannot drift from the strip's inks. */
internal fun paletteOf(colors: LumintraColors): LumintraColors = colors
