package com.gexetr.lumintralightplanner.ui.brief

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.CatalogueRepository
import com.gexetr.lumintralightplanner.data.CatalogueResult
import com.gexetr.lumintralightplanner.data.CloudFailure
import com.gexetr.lumintralightplanner.data.CloudRepository
import com.gexetr.lumintralightplanner.data.CloudResult
import com.gexetr.lumintralightplanner.data.DataStoreSettingsStore
import com.gexetr.lumintralightplanner.data.ScoutSnapshot
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.data.SettingsStore
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import com.gexetr.lumintralightplanner.data.sky.AltitudeModel
import com.gexetr.lumintralightplanner.data.sky.NightComputation
import com.gexetr.lumintralightplanner.data.sky.NightEngine
import com.gexetr.lumintralightplanner.data.sky.NightRequest
import com.gexetr.lumintralightplanner.data.sky.SkyEphemeris
import com.gexetr.lumintralightplanner.domain.BriefCondition
import com.gexetr.lumintralightplanner.domain.BriefNotice
import com.gexetr.lumintralightplanner.domain.BriefState
import com.gexetr.lumintralightplanner.domain.CloudReport
import com.gexetr.lumintralightplanner.domain.ConditionState
import com.gexetr.lumintralightplanner.domain.ConditionUnavailableReason
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.DarkWindowReport
import com.gexetr.lumintralightplanner.domain.DarknessLevel
import com.gexetr.lumintralightplanner.domain.ElementSetAge
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.KeyTimeRow
import com.gexetr.lumintralightplanner.domain.LensPreset
import com.gexetr.lumintralightplanner.domain.MoonState
import com.gexetr.lumintralightplanner.domain.MoonlessKind
import com.gexetr.lumintralightplanner.domain.MoonlessWindow
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.ScoutSelection
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.domain.SkyBrightnessEstimate
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.SunriseSunsetTimes
import com.gexetr.lumintralightplanner.domain.TargetWindow
import com.gexetr.lumintralightplanner.notify.DarkWindowReminder
import com.gexetr.lumintralightplanner.notify.ReminderScheduler
import com.gexetr.lumintralightplanner.ui.components.HeroStat
import com.gexetr.lumintralightplanner.ui.components.LumintraEmptyState
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.components.ValueRow
import com.gexetr.lumintralightplanner.ui.format.formatNightLabel
import com.gexetr.lumintralightplanner.ui.night.TimelineStrip
import com.gexetr.lumintralightplanner.ui.night.TimelineStripModel
import com.gexetr.lumintralightplanner.ui.night.TimelineStripSampler
import com.gexetr.lumintralightplanner.ui.night.TwilightLut
import com.gexetr.lumintralightplanner.ui.night.rememberTimelineLegendColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

// =================================================================================================
// 1. The state the screen renders
// =================================================================================================

/**
 * Everything the brief renders, as one immutable value.
 *
 * The **instants are absolute and the words are already localised for a chosen zone**: [zoneId] is the
 * zone the whole brief is rendered in — the spot's, or the device's once [onToggleZone] has been used —
 * and [locale] and [use24Hour] are carried rather than read from the environment at render time, so a
 * recomposition cannot silently change the zone a label came from.
 *
 * Every derived section is a field rather than a `SessionBrief`, because the brief's sections are a
 * rendering contract: a missing [darkAnswer] is a state the screen must handle, and it must be visible
 * here rather than discovered by a null dereference during layout.
 */
@Immutable
data class BriefUiState(
    val loading: Boolean = true,
    /** The typed answer: no spot, no night, or a complete brief. Never a fabricated sky. */
    val state: BriefState = BriefState.NoSpot,
    val spotId: String? = null,
    val spotName: String = "",
    val zoneId: String = "",
    val zoneIsDevice: Boolean = false,
    val locale: Locale = Locale.getDefault(),
    val use24Hour: Boolean = true,
    val nightLabel: String = "",
    val sessionStartUtc: Instant? = null,
    val sessionEndUtc: Instant? = null,
    val scrubValue: Double = 0.0,
    val instantUtc: Instant? = null,
    val instantLabel: String = "",
    // The hero panel's own three numbers, already worded for the locale.
    val heroValue: String = "",
    val heroLabelRes: Int = R.string.brief_dark_window_header,
    val heroEmphasised: Boolean = false,
    val skyLevelRes: Int = R.string.night_sky_grade_unknown,
    val darkAnswer: DarkAnswer? = null,
    val darkWindow: NightSpan? = null,
    val keyTimes: List<KeyTimeRow> = emptyList(),
    val sunriseSunset: SunriseSunsetTimes = SunriseSunsetTimes(null, null),
    val moonPanel: MoonPanel? = null,
    val cloudRow: CloudRow? = null,
    val targets: List<BriefTargetRow> = emptyList(),
    val passes: List<BriefPassRow> = emptyList(),
    val elementSetAge: ElementSetAge? = null,
    val conditions: List<com.gexetr.lumintralightplanner.domain.BriefCondition> = emptyList(),
    val notices: List<BriefNotice> = emptyList(),
    val strip: TimelineStripModel? = null,
    val horizonDrawn: Boolean = false,
    val catalogueUnreadable: Boolean = false,
    /** The bundled sky table could not be read, so the scene's palette is the app's own fallback. */
    val lutFallback: Boolean = false,
    val session: NightSpan? = null,
) {
    val hasInstant: Boolean get() = instantUtc != null

    /** The dark-window report when there is one, so the screen never re-derives the typed answer. */
    val report get() = darkAnswer?.report
}

// =================================================================================================
// 2. The ViewModel
// =================================================================================================

/**
 * The brief's ViewModel: one computation per (spot, night), then a projection per scrub.
 *
 * ## Two clocks, and why
 *
 * [clock] is read **once per load**, to choose which night is being planned; a scrub moves a value
 * inside that night and reads nothing. That is what makes the scrub a projection of facts already in
 * hand — the target altitude at a scrub instant is one ephemeris call per shown target, never a new
 * search — and it is why the brief has no animation while the user is not touching it.
 *
 * ## What it does not do
 *
 * It never fetches during a gesture: [onRefreshCloud] is the only path that touches the network, and it
 * is a button. It never fabricates an input: a cloud reading that could not be fetched becomes a typed
 * `ConditionUnavailableReason`, and a pass list that was not supplied stays empty so the passes section
 * states its own absence.
 *
 * ## Honest limit, stated rather than implied
 *
 * The target rows are the **fixed catalogue rows** this class is given. The computed Solar-System
 * appendix (`CatalogueRepository.computedSolarSystem`) is assembled for the scene's own bodies and is
 * not folded into the brief's target list, because its provider interface is not the one this
 * ViewModel is constructed with; the Moon is reported by its own panel, and the planets are drawn in
 * the scene. A caller who wants planets listed as target rows must assemble them into `objects`.
 */
class BriefViewModel(
    private val appContext: Context,
    private val scoutStore: ScoutStore,
    private val catalogue: CatalogueRepository,
    private val cloudRepository: CloudRepository,
    private val ephemeris: SkyEphemeris,
    private val assetSource: SkyAssetSource,
    private val settingsStore: SettingsStore = DataStoreSettingsStore(appContext),
    /**
     * The night's predicted passes, as an injected search — the same shape `NightViewModel` takes, for
     * the same reason: the search needs Orekit, an element-set fetch and the horizon mask, none of
     * which the brief owns. The default is the honest empty answer.
     */
    private val passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass> =
        { _, _, _ -> emptyList() },
    /** The user's own sky-brightness estimate: a user input, never a measurement. */
    private val brightnessEstimate: () -> SkyBrightnessEstimate = { SkyBrightnessEstimate.NONE },
    private val clock: () -> Instant = { Instant.now() },
    /**
     * Survives process death: the scrub fraction and the spot id it was taken against. Restored only
     * when the active spot still matches, so a different spot never inherits another night's scrub.
     */
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _state = MutableStateFlow(BriefUiState())
    val state: StateFlow<BriefUiState> = _state.asStateFlow()

    private val use24Hour: Boolean = android.text.format.DateFormat.is24HourFormat(appContext)
    private val locale: Locale = appContext.resources.configuration.locales.get(0) ?: Locale.getDefault()

    private var lut: TwilightLut? = null
    private var loaded: Loaded? = null
    private var zoneIsDevice: Boolean = savedStateHandle.get<Boolean>(KEY_ZONE_IS_DEVICE) ?: false

    init {
        viewModelScope.launch {
            val table = withContext(Dispatchers.Default) {
                lut ?: TwilightLut.load(assetSource).also { lut = it }
            }
            combine(
                scoutStore.snapshot,
                settingsStore.settings.map { it.reminderEnabled }.distinctUntilChanged(),
            ) { snapshot, reminderEnabled ->
                snapshot to reminderEnabled
            }.collect { (snapshot, reminderEnabled) ->
                recompute(snapshot, table, reminderEnabled = reminderEnabled)
            }
        }
    }

    /** The scrub. Cheap on purpose: it projects facts already computed, and fetches nothing. */
    fun onScrub(value: Double) {
        val current = loaded ?: return
        val scrub = value.coerceIn(0.0, 1.0)
        savedStateHandle[KEY_SCRUB] = scrub
        savedStateHandle[KEY_SPOT_ID] = current.spotId
        _state.value = render(current, scrub)
    }

    /** The zone toggle: the same instants, the other zone. Nothing astronomical changes. */
    fun onToggleZone() {
        zoneIsDevice = !zoneIsDevice
        savedStateHandle[KEY_ZONE_IS_DEVICE] = zoneIsDevice
        val current = loaded ?: run {
            _state.value = _state.value.copy(zoneIsDevice = zoneIsDevice)
            return
        }
        _state.value = render(
            current,
            current.lastScrub ?: defaultScrubOf(current.computation),
        )
    }

    /** Re-reads the one external input with a network cost. Never called from a gesture. */
    fun onRefreshCloud() {
        viewModelScope.launch {
            val snapshot = scoutStore.snapshot.let { flow ->
                var latest: ScoutSnapshot? = null
                flow.collect { latest = it; return@collect }
                latest
            } ?: return@launch
            val reminderEnabled = settingsStore.settings.map { it.reminderEnabled }.first()
            recompute(snapshot, lut ?: return@launch, refreshCloud = true, reminderEnabled = reminderEnabled)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The one computation
    // ---------------------------------------------------------------------------------------------

    private suspend fun recompute(
        snapshot: ScoutSnapshot,
        table: TwilightLut,
        refreshCloud: Boolean = false,
        reminderEnabled: Boolean,
    ) {
        val spot = ScoutSelection.resolve(snapshot.spots, snapshot.activeSpotId)
        if (spot == null) {
            loaded = null
            _state.value = BriefUiState(
                loading = false,
                state = BriefState.NoSpot,
                zoneIsDevice = zoneIsDevice,
                locale = locale,
                use24Hour = use24Hour,
                lutFallback = table.isFallback,
            )
            reconcileReminder(
                reminderEnabled = reminderEnabled,
                night = null,
                spot = null,
                horizon = null,
                objects = emptyList(),
                cloud = null,
                passes = emptyList(),
                notices = emptyList(),
                now = clock(),
                zone = ZoneId.systemDefault(),
                nightDate = LocalDate.now(ZoneId.systemDefault()),
                computedOffline = true,
            )
            return
        }

        val horizon = spot.horizonProfileId?.let { snapshot.horizons[it] }
        val now = clock()
        val zone = ZoneId.of(spot.tzId)
        val nightDate = SessionClock.nightOf(now, zone)
        val session = SessionClock.sessionWindow(nightDate, zone, spot.id)

        val catalogueResult = withContext(Dispatchers.Default) { catalogue.load() }
        val objects = (catalogueResult as? CatalogueResult.Success)?.objects.orEmpty()
        val catalogueUnreadable = catalogueResult is CatalogueResult.Unreadable

        val cloudResult = cloudOf(spot, session, now, refreshCloud)
        val passes = withContext(Dispatchers.Default) { passProvider(spot, session, now) }
        val notices = noticesOf(snapshot, cloudResult, now)

        val request = NightRequest(
            spot = spot,
            horizon = horizon,
            equipment = spot.equipment,
            span = session,
            zone = zone,
            nowUtc = now,
            favouriteIds = snapshot.favourites,
            objects = objects,
            cloud = cloudReportOf(cloudResult),
            passes = passes,
            elementSetAge = passes.firstOrNull()?.elementSetAge,
            altitudeMaskDeg = NightRequest.DEFAULT_ALTITUDE_MASK_DEG,
            skyBrightnessEstimate = brightnessEstimate(),
            notices = notices,
        )
        val night = withContext(Dispatchers.Default) { NightEngine.compute(request, ephemeris) }

        val lens = spot.equipment.toLensPreset()
        val strip = withContext(Dispatchers.Default) {
            TimelineStripSampler.sample(
                session = session,
                lut = table,
                sunAltitudeDeg = { ephemeris.sunHorizontal(spot.point, it, AltitudeModel.GEOMETRIC).altitudeDeg },
                darkWindows = night.darkWindow.darkWindows,
                moonlessWindows = night.moonless.intervals,
                moonHorizontal = { ephemeris.moonHorizontal(spot.point, it).altitudeDeg },
                cloud = night.request.cloud,
            )
        }
        val targets = targetIdsOf(objects, snapshot.favourites, night)

        val restoredScrub = restoredScrubFor(spot.id)
        val fresh = Loaded(
            computation = night,
            point = spot.point,
            spotId = spot.id,
            spotName = spot.name,
            zone = zone,
            session = session,
            nightLabel = formatNightLabel(session.startUtc, session.endUtc, zone, locale),
            lens = lens,
            strip = strip,
            cloudRow = cloudRowOf(cloudResult, night),
            objects = objects.associateBy { it.id },
            targetIds = targets,
            lut = table,
            catalogueUnreadable = catalogueUnreadable,
            horizonDrawn = horizon != null,
            lastScrub = restoredScrub,
        )
        loaded = fresh
        savedStateHandle[KEY_SPOT_ID] = spot.id
        val scrub = restoredScrub ?: defaultScrubOf(night)
        savedStateHandle[KEY_SCRUB] = scrub
        _state.value = render(fresh, scrub)

        val computedOffline = cloudResult !is CloudResult.Available
        reconcileReminder(
            reminderEnabled = reminderEnabled,
            night = night,
            spot = spot,
            horizon = horizon,
            objects = objects,
            cloud = night.request.cloud,
            passes = passes,
            notices = notices,
            now = now,
            zone = zone,
            nightDate = nightDate,
            computedOffline = computedOffline,
        )
    }

    /**
     * Arms or cancels the opt-in dark-window reminder from the composed brief.
     *
     * Reads [reminderEnabled] from [SettingsStore] (via the caller) so the preference is consumed
     * outside the settings surface — the contract `settings.behaviour` requires. Candidates are
     * tonight first and the following night second, matching [DarkWindowReminder.reconcile].
     */
    private suspend fun reconcileReminder(
        reminderEnabled: Boolean,
        night: NightComputation?,
        spot: Scout?,
        horizon: HorizonProfile?,
        objects: List<SkyObject>,
        cloud: CloudReport?,
        passes: List<SatellitePass>,
        notices: List<BriefNotice>,
        now: Instant,
        zone: ZoneId,
        nightDate: LocalDate,
        computedOffline: Boolean,
    ) {
        if (!reminderEnabled || night == null || spot == null) {
            withContext(Dispatchers.Default) {
                DarkWindowReminder.reconcile(
                    context = appContext,
                    enabled = false,
                    candidates = emptyList(),
                    nowUtc = now,
                )
            }
            return
        }
        val tonight = ReminderScheduler.planOf(night.brief, computedOffline)
        val nextDate = nightDate.plusDays(1)
        val nextSession = SessionClock.sessionWindow(nextDate, zone, spot.id)
        val nextNight = withContext(Dispatchers.Default) {
            NightEngine.compute(
                NightRequest(
                    spot = spot,
                    horizon = horizon,
                    equipment = spot.equipment,
                    span = nextSession,
                    zone = zone,
                    nowUtc = now,
                    favouriteIds = emptySet(),
                    objects = objects,
                    cloud = cloud,
                    passes = passes,
                    elementSetAge = passes.firstOrNull()?.elementSetAge,
                    altitudeMaskDeg = NightRequest.DEFAULT_ALTITUDE_MASK_DEG,
                    skyBrightnessEstimate = brightnessEstimate(),
                    notices = notices,
                ),
                ephemeris,
            )
        }
        val tomorrow = ReminderScheduler.planOf(nextNight.brief, computedOffline)
        withContext(Dispatchers.Default) {
            DarkWindowReminder.reconcile(
                context = appContext,
                enabled = true,
                candidates = listOf(tonight, tomorrow),
                nowUtc = now,
            )
        }
    }

    /**
     * Scrub restored from [SavedStateHandle] when the active spot still matches the one that was
     * saved; otherwise the in-memory state for the same spot, then null so the default scrub wins.
     */
    private fun restoredScrubFor(spotId: String): Double? {
        val savedSpot = savedStateHandle.get<String>(KEY_SPOT_ID)
        val savedScrub = savedStateHandle.get<Double>(KEY_SCRUB)
        if (savedSpot == spotId && savedScrub != null) return savedScrub.coerceIn(0.0, 1.0)
        return _state.value.takeIf { it.spotId == spotId }?.scrubValue
    }

    private suspend fun cloudOf(spot: Scout, session: NightSpan, now: Instant, refresh: Boolean): CloudResult? {
        val enabled = withContext(Dispatchers.Default) { cloudRepository.isEnabled() }
        val cached = withContext(Dispatchers.Default) { cloudRepository.cachedReading(spot.networkPoint, session, now) }
        return when {
            refresh && enabled -> withContext(Dispatchers.Default) {
                cloudRepository.fetch(spot.networkPoint, session, now)
            }

            // A cached reading is served with its age and the word forecast; it is never presented as
            // current conditions, and it is never re-fetched on a gesture.
            cached != null -> CloudResult.Available(
                report = cached,
                modelTimeUtc = null,
                servedFromCache = true,
                deprecatedProduct = false,
                serverDateUtc = null,
            )

            enabled -> withContext(Dispatchers.Default) {
                cloudRepository.fetch(spot.networkPoint, session, now)
            }

            else -> null
        }
    }

    /** The notices this ViewModel can see for itself: a saved spot, a missing reference, a clock skew. */
    private fun noticesOf(snapshot: ScoutSnapshot, cloud: CloudResult?, nowUtc: Instant): List<BriefNotice> {
        val out = ArrayList<BriefNotice>(3)
        if (snapshot.quarantinedCount > 0) out += BriefNotice.SavedSpotUnreadable(snapshot.quarantinedCount)
        if (snapshot.missingTargetRefCount > 0) out += BriefNotice.SavedTargetMissing(snapshot.missingTargetRefCount)
        if (cloud is CloudResult.Available) {
            cloud.clockSkew(nowUtc)?.toMinutes()?.let { minutes ->
                if (kotlin.math.abs(minutes) >= CLOCK_SKEW_TOLERANCE_MINUTES) {
                    out += BriefNotice.DeviceClockSkew(minutes)
                }
            }
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------
    // The projection: one instant inside the night
    // ---------------------------------------------------------------------------------------------

    private fun render(data: Loaded, scrub: Double): BriefUiState {
        val night = data.computation
        val span = data.session
        val instant = span.instantAt(scrub)
        val zone = if (zoneIsDevice) ZoneId.systemDefault() else data.zone
        val time = BriefTimeFormat(zone, locale, use24Hour)

        val moonState = ephemeris.moonState(data.point, instant)
        val report = night.darkWindow
        val darkWindow = night.bestDarkWindow

        return BriefUiState(
            loading = false,
            state = BriefState.Ready(night.brief),
            spotId = data.spotId,
            spotName = data.spotName,
            zoneId = zone.id,
            zoneIsDevice = zoneIsDevice,
            locale = locale,
            use24Hour = use24Hour,
            nightLabel = formatNightLabel(span.startUtc, span.endUtc, zone, locale),
            sessionStartUtc = span.startUtc,
            sessionEndUtc = span.endUtc,
            scrubValue = scrub,
            instantUtc = instant,
            instantLabel = time.time(instant),
            heroValue = heroValueOf(report, darkWindow, time),
            heroLabelRes = R.string.brief_dark_window_header,
            heroEmphasised = report.kind == DarkWindowKind.DARK,
            skyLevelRes = darknessLevelRes(night.skyGrade.level),
            darkAnswer = DarkAnswer(report),
            darkWindow = darkWindow,
            keyTimes = night.keyTimes,
            sunriseSunset = night.sunriseSunset,
            moonPanel = moonPanelOf(night.moonless, moonState),
            cloudRow = data.cloudRow,
            targets = data.targetIds.mapNotNull { id ->
                data.objects[id]?.let { obj -> targetRowOf(obj, data, instant) }
            },
            passes = night.request.passes.map { BriefPassRow(it) },
            elementSetAge = night.request.elementSetAge,
            conditions = night.brief.conditions,
            notices = night.brief.notices,
            strip = data.strip,
            horizonDrawn = data.horizonDrawn,
            catalogueUnreadable = data.catalogueUnreadable,
            lutFallback = data.lut.isFallback,
            session = span,
        )
    }

    /** The hero number: the answer itself, in the same words the five typed outcomes use. */
    private fun heroValueOf(
        report: com.gexetr.lumintralightplanner.domain.DarkWindowReport,
        darkWindow: NightSpan?,
        time: BriefTimeFormat,
    ): String = when (report.kind) {
        DarkWindowKind.DARK ->
            darkWindow?.let { time.duration(it.duration) } ?: appContext.getString(R.string.brief_moonless_none)

        DarkWindowKind.POLAR_NIGHT ->
            darkWindow?.let { time.duration(it.duration) }
                ?: appContext.getString(R.string.brief_dark_kind_polar_night)

        DarkWindowKind.POLAR_DAY -> appContext.getString(R.string.brief_dark_kind_polar_day)
        DarkWindowKind.NO_ASTRO_DARK -> appContext.getString(R.string.brief_dark_kind_no_astro)
        DarkWindowKind.NO_DARK -> appContext.getString(R.string.brief_dark_kind_no_dark)
    }

    /** The one row shape every target block uses, with its position at the scrubbed instant. */
    private fun targetRowOf(obj: SkyObject, data: Loaded, instant: Instant): BriefTargetRow? {
        val window = data.computation.targetWindows[obj.id] ?: return null
        val now = if (window is TargetWindow.NeverRises) {
            null
        } else {
            ephemeris.fixedHorizontal(data.point, obj.equatorial, instant)
        }
        return BriefTargetRow(
            id = obj.id,
            name = obj.name,
            designation = obj.messier ?: obj.ngc ?: obj.ic ?: obj.id,
            type = obj.type,
            magnitude = obj.displayMagnitude,
            magnitudeIsDisputed = obj.magDisputed,
            sizeMajArcmin = obj.sizeMajArcmin,
            window = window,
            fit = data.computation.targetFits[obj.id],
            lens = data.lens,
            nowAltitudeDeg = now?.altitudeDeg,
            nowAzimuthDeg = now?.azimuthDeg,
            washedOut = data.computation.moonless.washedOut,
        )
    }

    private fun moonPanelOf(moonless: MoonlessWindow, moon: MoonState): MoonPanel {
        val extreme = moonless.extremeAltitudeDeg
        val extremeAt = moonless.extremeInstant
        return MoonPanel(
            fraction = moon.illuminatedFraction,
            phase = moon.phaseName,
            altitudeDeg = moon.altitudeDeg,
            azimuthDeg = moon.azimuthDeg,
            magnitude = moon.magnitude,
            // A rise or set is stated only when the night actually has one: the engine's own moonless
            // answer decides that, and an absent time is never invented.
            riseUtc = if (moonless.kind == MoonlessKind.MOON_NEVER_RISES) null else moonriseOf(moonless),
            setUtc = if (moonless.kind == MoonlessKind.MOON_NEVER_SETS) null else moonsetOf(moonless),
            moonless = moonless,
            isWashedOut = moonless.washedOut,
            lowestAltitudeDeg = if (!moonless.washedOut && moonless.kind == MoonlessKind.MOON_UP_ALL_WINDOW) {
                extreme
            } else {
                null
            },
            lowestInstant = if (!moonless.washedOut && moonless.kind == MoonlessKind.MOON_UP_ALL_WINDOW) {
                extremeAt
            } else {
                null
            },
            highestAltitudeDeg = if (moonless.washedOut) extreme else null,
            highestInstant = if (moonless.washedOut) extremeAt else null,
        )
    }

    /**
     * The Moon's rise or set inside the night, or `null`.
     *
     * The typed [MoonlessWindow] carries the moonless intervals and the extreme altitude, not the
     * crossing instants, so no time is **invented** here: the caller supplies the crossings through the
     * engine when it has them, and until then the panel states the type it knows (`up all night`,
     * `below the horizon all night`, `never rises`, `never sets`) rather than a guessed minute.
     */
    private fun moonriseOf(moonless: MoonlessWindow): Instant? = null

    private fun moonsetOf(moonless: MoonlessWindow): Instant? = null

    // ---------------------------------------------------------------------------------------------
    // The pieces that do not depend on the scrub
    // ---------------------------------------------------------------------------------------------

    private fun cloudReportOf(cloud: CloudResult?): CloudReport? = when (cloud) {
        is CloudResult.Available -> cloud.report
        is CloudResult.Unavailable -> cloud.lastReading
        null -> null
    }

    private fun cloudRowOf(cloud: CloudResult?, night: NightComputation): CloudRow {
        val report = cloudReportOf(cloud)
        val darkWindow = night.bestDarkWindow
        val mean = when {
            report == null -> null
            darkWindow == null -> report.meanCloudPercentWithin(night.session)
            else -> report.meanCloudPercentWithin(darkWindow)
        }
        val reason = when (cloud) {
            is CloudResult.Unavailable -> cloud.reason()
            null -> ConditionUnavailableReason.SOURCE_DISABLED
            else -> null
        }
        val state = mean?.let {
            when {
                it <= CLOUD_GOOD_PERCENT -> ConditionState.GOOD
                it <= CLOUD_MARGINAL_PERCENT -> ConditionState.MARGINAL
                else -> ConditionState.BLOCKING
            }
        }
        val hours = report?.hours.orEmpty()
        return CloudRow(
            report = report,
            meanPercent = mean,
            reason = reason,
            state = state,
            offline = cloud is CloudResult.Unavailable && report != null ||
                (cloud as? CloudResult.Available)?.servedFromCache == true,
            coveredRange = if (hours.size >= 2) NightSpan(hours.first().hourUtc, hours.last().hourUtc) else null,
        )
    }

    private fun CloudResult.Unavailable.reason(): ConditionUnavailableReason = when (state) {
        CloudFailure.SOURCE_DISABLED -> ConditionUnavailableReason.SOURCE_DISABLED
        CloudFailure.NO_NETWORK,
        CloudFailure.SERVICE_UNREACHABLE,
        CloudFailure.SERVICE_ERROR,
        CloudFailure.RATE_LIMITED,
        -> ConditionUnavailableReason.OFFLINE

        CloudFailure.NO_HOURS_IN_WINDOW,
        CloudFailure.OUT_OF_COVERAGE,
        -> ConditionUnavailableReason.NOT_FETCHED

        CloudFailure.MALFORMED_RESPONSE -> ConditionUnavailableReason.OFFLINE
    }

    /**
     * The targets the brief lists: the favourites that are up tonight first, then the objects that are
     * visible tonight, capped — the same order the user's own choices imply, never a ranking the
     * catalogue did not make.
     */
    private fun targetIdsOf(
        objects: List<SkyObject>,
        favourites: Set<String>,
        night: NightComputation,
    ): List<String> {
        val visible = objects.map { it.id }.filter { night.targetWindows[it]?.isVisibleTonight == true }
        val ordered = ArrayList<String>(BRIEF_TARGET_LIMIT)
        favourites.filter { night.targetWindows[it] != null }.forEach { id ->
            if (ordered.size < BRIEF_TARGET_LIMIT && id !in ordered) ordered += id
        }
        visible.forEach { id ->
            if (ordered.size < BRIEF_TARGET_LIMIT && id !in ordered) ordered += id
        }
        return ordered
    }

    /** Where the brief opens: the middle of the dark window when there is one, else local midnight. */
    private fun defaultScrubOf(night: NightComputation): Double {
        val window = night.bestDarkWindow ?: return 0.5
        return night.session.fractionOf(window.startUtc.plus(window.duration.dividedBy(2L)))
    }

    /** One load's facts, kept so a scrub is a projection and not a second computation. */
    private class Loaded(
        val computation: NightComputation,
        val point: GeodeticPoint,
        val spotId: String,
        val spotName: String,
        val zone: ZoneId,
        val session: NightSpan,
        val nightLabel: String,
        val lens: LensPreset,
        val strip: TimelineStripModel,
        val cloudRow: CloudRow,
        val objects: Map<String, SkyObject>,
        val targetIds: List<String>,
        val lut: TwilightLut,
        val catalogueUnreadable: Boolean,
        val horizonDrawn: Boolean,
        val lastScrub: Double?,
    )

    companion object {
        /** A device clock this far from the server's `Date` is worth a notice rather than a silent skew. */
        const val CLOCK_SKEW_TOLERANCE_MINUTES: Long = 5L

        /** How many target rows the brief lists before it stops. */
        const val BRIEF_TARGET_LIMIT: Int = 6

        /** The cloud amounts the brief's own chips are cut at; product thresholds, not measurements. */
        const val CLOUD_GOOD_PERCENT: Int = 20
        const val CLOUD_MARGINAL_PERCENT: Int = 70

        private const val KEY_SCRUB: String = "brief.scrub"
        private const val KEY_SPOT_ID: String = "brief.spotId"
        private const val KEY_ZONE_IS_DEVICE: String = "brief.zoneIsDevice"

        /**
         * A factory taking manual dependencies, because this app uses no injection framework. The
         * caller owns the repositories' lifetime. [SavedStateHandle] is taken from [CreationExtras]
         * so scrub and spot survive process death.
         */
        @Suppress("UNCHECKED_CAST")
        fun factory(
            appContext: Context,
            scoutStore: ScoutStore,
            catalogue: CatalogueRepository,
            cloudRepository: CloudRepository,
            ephemeris: SkyEphemeris,
            assetSource: SkyAssetSource,
            settingsStore: SettingsStore = DataStoreSettingsStore(appContext.applicationContext),
            passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass> =
                { _, _, _ -> emptyList() },
            brightnessEstimate: () -> SkyBrightnessEstimate = { SkyBrightnessEstimate.NONE },
            clock: () -> Instant = { Instant.now() },
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                BriefViewModel(
                    appContext = appContext.applicationContext,
                    scoutStore = scoutStore,
                    catalogue = catalogue,
                    cloudRepository = cloudRepository,
                    ephemeris = ephemeris,
                    assetSource = assetSource,
                    settingsStore = settingsStore,
                    passProvider = passProvider,
                    brightnessEstimate = brightnessEstimate,
                    clock = clock,
                    savedStateHandle = extras.createSavedStateHandle(),
                ) as T

            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BriefViewModel(
                    appContext = appContext.applicationContext,
                    scoutStore = scoutStore,
                    catalogue = catalogue,
                    cloudRepository = cloudRepository,
                    ephemeris = ephemeris,
                    assetSource = assetSource,
                    settingsStore = settingsStore,
                    passProvider = passProvider,
                    brightnessEstimate = brightnessEstimate,
                    clock = clock,
                ) as T
        }
    }
}

/** The level of darkness, in the five words the brief uses for it. */
internal fun darknessLevelRes(level: DarknessLevel?): Int = when (level) {
    DarknessLevel.EXCELLENT -> R.string.brief_sky_level_excellent
    DarknessLevel.GOOD -> R.string.brief_sky_level_good
    DarknessLevel.FAIR -> R.string.brief_sky_level_fair
    DarknessLevel.POOR -> R.string.brief_sky_level_poor
    DarknessLevel.NONE -> R.string.brief_sky_level_none
    null -> R.string.night_sky_grade_unknown
}

// =================================================================================================
// 3. The screen
// =================================================================================================

/**
 * The session brief: the app's answer, in one scrollable column (two on a wide screen).
 *
 * The order is the order of the questions: **what can I actually see tonight** (the dark window), when
 * it is, how much Moon is in the way, what the cloud is doing, what the sky does at the edges, what is
 * up, what passes, and then every named condition with its source and age. Every edge state is typed
 * and rendered as a sentence, so no panel is ever blank and no absence is ever silent.
 *
 * The timeline under the hero is the scrub: it moves the whole brief, and it is the only interactive
 * element here that carries a progress semantics.
 */
@Composable
fun BriefScreen(
    state: BriefUiState,
    onScrub: (Double) -> Unit,
    onOpenTarget: (String) -> Unit,
    onShare: () -> Unit,
    onOpenScene: () -> Unit,
    onToggleZone: () -> Unit,
    onRefreshCloud: () -> Unit,
    onBack: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp >= 600
    val time = remember(state.zoneId, state.locale, state.use24Hour) {
        BriefTimeFormat(
            zoneId = runCatching { ZoneId.of(state.zoneId) }.getOrElse { ZoneId.systemDefault() },
            locale = state.locale,
            use24Hour = state.use24Hour,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LumintraScaffoldHeader(
            title = stringResource(R.string.brief_title),
            onBack = onBack,
        ) {
            IconButton(
                onClick = onShare,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_share_2),
                    contentDescription = stringResource(R.string.brief_share_action),
                )
            }
            IconButton(
                onClick = onOpenScene,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_telescope),
                    contentDescription = stringResource(R.string.brief_open_night_action),
                )
            }
        }

        when {
            state.loading -> BriefLoading(spotName = state.spotName)

            state.state is BriefState.NoSpot -> LumintraEmptyState(
                title = stringResource(R.string.brief_no_spot_title),
                body = stringResource(R.string.brief_no_spot_body),
                modifier = Modifier.fillMaxSize(),
                iconRes = R.drawable.ic_ui_map_pin,
            )

            else -> BriefBody(
                state = state,
                time = time,
                wide = wide,
                onScrub = onScrub,
                onOpenTarget = onOpenTarget,
                onOpenScene = onOpenScene,
                onShare = onShare,
                onToggleZone = onToggleZone,
                onRefreshCloud = onRefreshCloud,
            )
        }
    }
}

@Composable
private fun BriefLoading(spotName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LumintraPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (spotName.isEmpty()) {
                    stringResource(R.string.state_loading)
                } else {
                    stringResource(R.string.brief_loading, spotName)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.size(12.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BriefBody(
    state: BriefUiState,
    time: BriefTimeFormat,
    wide: Boolean,
    onScrub: (Double) -> Unit,
    onOpenTarget: (String) -> Unit,
    onOpenScene: () -> Unit,
    onShare: () -> Unit,
    onToggleZone: () -> Unit,
    onRefreshCloud: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BriefHeroPanel(state = state, time = time, wide = wide)

        state.strip?.let { strip ->
            LumintraPanel(modifier = Modifier.fillMaxWidth()) {
                SectionStrip(title = stringResource(R.string.brief_timeline_header))
                TimelineStrip(
                    model = strip,
                    scrubValue = state.scrubValue,
                    onScrub = onScrub,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = stringResource(
                        R.string.brief_timeline_description,
                        state.instantLabel,
                    ),
                )
                TimelineLegend()
                Text(
                    text = stringResource(R.string.brief_scrub_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BriefLeftColumn(state, time, onRefreshCloud)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BriefRightColumn(state, time, onOpenTarget)
                }
            }
        } else {
            BriefLeftColumn(state, time, onRefreshCloud)
            BriefRightColumn(state, time, onOpenTarget)
        }

        BriefZoneRow(state = state, time = time, onToggleZone = onToggleZone)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                label = stringResource(R.string.brief_open_night_action),
                iconRes = R.drawable.ic_ui_telescope,
                onClick = onOpenScene,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = stringResource(R.string.brief_share_action),
                iconRes = R.drawable.ic_ui_share_2,
                onClick = onShare,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
    }
}

/** The hero: the dark-window answer, the scrubbed instant and the sky level, side by side. */
@Composable
private fun BriefHeroPanel(state: BriefUiState, time: BriefTimeFormat, wide: Boolean) {
    LumintraPanel(modifier = Modifier.fillMaxWidth(), accent = state.heroEmphasised) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeroStat(
                    value = state.heroValue,
                    label = stringResource(state.heroLabelRes),
                    modifier = Modifier.weight(1.4f),
                    emphasised = state.heroEmphasised,
                )
                HeroStat(
                    value = state.instantLabel,
                    label = stringResource(R.string.brief_instant_label),
                    modifier = Modifier.weight(1f),
                )
                HeroStat(
                    value = stringResource(state.skyLevelRes),
                    label = stringResource(R.string.night_sky_grade_label),
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeroStat(
                    value = state.heroValue,
                    label = stringResource(state.heroLabelRes),
                    modifier = Modifier.fillMaxWidth(),
                    emphasised = state.heroEmphasised,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HeroStat(
                        value = state.instantLabel,
                        label = stringResource(R.string.brief_instant_label),
                        modifier = Modifier.weight(1f),
                    )
                    HeroStat(
                        value = stringResource(state.skyLevelRes),
                        label = stringResource(R.string.night_sky_grade_label),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.spotName.isNotEmpty()) {
            ValueRow(
                label = stringResource(R.string.brief_spot_label),
                value = state.spotName,
            )
        }
        if (state.nightLabel.isNotEmpty()) {
            ValueRow(
                label = stringResource(R.string.brief_title),
                value = state.nightLabel,
            )
        }
        state.session?.let { session ->
            ValueRow(
                label = stringResource(R.string.brief_timeline_header),
                value = stringResource(
                    R.string.brief_range,
                    time.time(session.startUtc),
                    time.time(session.endUtc),
                ),
                secondary = time.duration(session.duration),
            )
        }
    }
}

@Composable
private fun BriefZoneRow(state: BriefUiState, time: BriefTimeFormat, onToggleZone: () -> Unit) {
    val zoneLabel = state.zoneId
    // Read outside the semantics lambda: `stringResource` is composable and `semantics {}` is not.
    val toggleDescription = stringResource(R.string.brief_zone_toggle)
    TextButton(
        onClick = onToggleZone,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .semantics { contentDescription = toggleDescription },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ui_clock),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = if (state.zoneIsDevice) {
                stringResource(R.string.brief_zone_other, zoneLabel)
            } else {
                stringResource(R.string.brief_zone_label, zoneLabel)
            },
        )
    }
}

@Composable
private fun BriefLeftColumn(state: BriefUiState, time: BriefTimeFormat, onRefreshCloud: () -> Unit) {
    state.darkAnswer?.let { answer ->
        BriefDarkWindowSection(
            answer = answer,
            spotName = state.spotName,
            darkWindow = state.darkWindow,
            time = time,
        )
    }
    state.moonPanel?.let { moon ->
        BriefMoonSection(panel = moon, time = time)
    }
    state.cloudRow?.let { cloud ->
        BriefCloudSection(
            row = cloud,
            darkWindow = state.darkWindow,
            time = time,
            onRefresh = onRefreshCloud,
        )
    }
}

@Composable
private fun BriefRightColumn(state: BriefUiState, time: BriefTimeFormat, onOpenTarget: (String) -> Unit) {
    if (state.keyTimes.isNotEmpty() || state.sunriseSunset.sunsetUtc != null) {
        BriefKeyTimesSection(
            keyTimes = state.keyTimes,
            sunriseSunset = state.sunriseSunset,
            time = time,
        )
    }
    BriefTargetsSection(rows = state.targets, time = time, onOpenTarget = onOpenTarget)
    BriefPassesSection(rows = state.passes, elementSetAge = state.elementSetAge, time = time)
    BriefConditionsSection(conditions = state.conditions, time = time)
    BriefNoticesSection(
        notices = state.notices,
        elementSetAge = state.elementSetAge,
        cloudReason = state.cloudRow?.reason,
        lutFallback = state.lutFallback,
        horizonDrawn = state.horizonDrawn,
        catalogueUnreadable = state.catalogueUnreadable,
        time = time,
    )
}

/** The three lanes the strip draws, named once so the picture is readable without a tooltip. */
@Composable
private fun TimelineLegend() {
    val colours = rememberTimelineLegendColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendSwatch(colour = colours.darkWindow, label = stringResource(R.string.brief_dark_window_header))
        LegendSwatch(colour = colours.moonless, label = stringResource(R.string.brief_moonless_legend))
        LegendSwatch(colour = colours.cloud, label = stringResource(R.string.cloud_total_cover_label))
    }
}

@Composable
private fun LegendSwatch(colour: androidx.compose.ui.graphics.Color, label: String) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .border(1.dp, outline.copy(alpha = 0.55f), CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colour.copy(alpha = 0.95f),
                            colour.copy(alpha = 0.38f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
        )
    }
}
