package com.gexetr.lumintralightplanner.ui.targets

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.gexetr.lumintralightplanner.data.ScoutSnapshot
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.data.sky.AstronomySkyEphemeris
import com.gexetr.lumintralightplanner.data.sky.SkyEphemeris
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.ObjectType
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.ScoutSelection
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.TargetInterval
import com.gexetr.lumintralightplanner.domain.TargetWindow
import com.gexetr.lumintralightplanner.domain.CrossingSearch
import com.gexetr.lumintralightplanner.domain.ObjectVisibility
import com.gexetr.lumintralightplanner.domain.WindowBoundary
import com.gexetr.lumintralightplanner.data.sky.AltitudeModel
import com.gexetr.lumintralightplanner.ui.components.LumintraEmptyState
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.format.formatNightLabel
import com.gexetr.lumintralightplanner.ui.theme.LumintraType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =================================================================================================
// The target window, computed in one place
// =================================================================================================

/**
 * The one place a target's window for a night is computed, shared by the catalogue list, the detail
 * surface and the `Share` brief so the three cannot state different windows for the same target.
 *
 * ### What it implements, and the one simplification it states
 *
 * The rule is the spec's: the target is above its **effective horizon** — the greater of the user's
 * drawn horizon sample *at the target's azimuth* and the altitude mask — for the interval to count.
 * The effective horizon is therefore azimuth-dependent, and this object evaluates it at the azimuth
 * of the target's own culmination (its highest point on the night) rather than re-evaluating it at
 * every sample. The simplification is stated here rather than hidden: for a target whose azimuth
 * swings widely across a drawn ridge, a second estimate sampled at each instant would be tighter. It
 * is exact for a flat or absent horizon, and it never reports a boundary as coming from the drawn
 * horizon unless the drawn horizon at the culmination really is the binding one
 * ([TargetInterval.clearsDrawnHorizon]).
 *
 * `internal` on purpose: the `Share` path is in another package of the same module, and this must
 * remain the single source of the answer rather than being re-derived there.
 */
internal object TargetWindows {

    /** The sampled-search step the app uses everywhere: ten minutes. */
    const val SAMPLE_STEP_SECONDS: Long = 600L

    /** The cheap grid the "up tonight" list filter uses; it decides a boolean, not a boundary. */
    const val CHEAP_STEP_SECONDS: Long = 1200L

    /** The altitude mask, exactly the pass engine's default so the two cannot drift apart. */
    const val MASK_DEG: Double = SatellitePass.DEFAULT_MASK_DEG

    /** The effective horizon at [azimuthDeg]: the greater of the drawn sample and the mask. */
    fun effectiveHorizonDeg(horizon: HorizonProfile?, azimuthDeg: Double): Double =
        maxOf(MASK_DEG, horizon?.altitudeAt(azimuthDeg) ?: MASK_DEG)

    /** True when the drawn horizon, not the mask, is the binding obstruction at [azimuthDeg]. */
    fun clearsDrawnHorizon(horizon: HorizonProfile?, azimuthDeg: Double): Boolean =
        horizon != null && horizon.altitudeAt(azimuthDeg) >= MASK_DEG

    /**
     * The cheap predicate behind the "Up tonight" filter: does the target clear its effective horizon
     * at any point of the (already dark-window-clipped) span?
     *
     * It samples at [CHEAP_STEP_SECONDS] and refines nothing, because the caller needs a boolean and
     * not a boundary; the answer is `false` only when no sample clears the horizon, so a target that
     * is up is never reported as down.
     */
    fun isUpTonight(
        obj: SkyObject,
        point: GeodeticPoint,
        horizon: HorizonProfile?,
        span: NightSpan,
        ephemeris: SkyEphemeris,
    ): Boolean {
        val coords = obj.equatorial
        var best = Double.NEGATIVE_INFINITY
        var bestAzimuth = 0.0
        for (instant in CrossingSearch.sampleInstants(span, CHEAP_STEP_SECONDS)) {
            val position = ephemeris.fixedHorizontal(point, coords, instant, AltitudeModel.REFRACTED)
            if (position.altitudeDeg > best) {
                best = position.altitudeDeg
                bestAzimuth = position.azimuthDeg
            }
        }
        if (!best.isFinite()) return false
        return best >= effectiveHorizonDeg(horizon, bestAzimuth)
    }

    /**
     * The full classification for one target on one night.
     *
     * [session] is the spot's local-noon-to-noon session; [darkWindows] are the astronomical-dark
     * intervals of that session (empty for a night with none). When there is no dark window the
     * intervals are reported as the session's own (a boundary named [WindowBoundary.SESSION_START] /
     * [WindowBoundary.SESSION_END]), never clipped silently into a window that does not exist.
     */
    fun classify(
        obj: SkyObject,
        point: GeodeticPoint,
        horizon: HorizonProfile?,
        session: NightSpan,
        darkWindows: List<NightSpan>,
        ephemeris: SkyEphemeris,
    ): TargetWindow {
        val coords = obj.equatorial
        fun altitudeAt(instant: Instant): Double =
            ephemeris.fixedHorizontal(point, coords, instant, AltitudeModel.REFRACTED).altitudeDeg

        // The culmination fixes the azimuth whose drawn horizon sample is the binding obstruction.
        val highest = CrossingSearch.extremum(session, findMax = true, f = ::altitudeAt)
        val culminationAzimuth = ephemeris
            .fixedHorizontal(point, coords, highest.instantUtc, AltitudeModel.REFRACTED)
            .azimuthDeg
        val effectiveHorizon = effectiveHorizonDeg(horizon, culminationAzimuth)
        val fromDrawnHorizon = clearsDrawnHorizon(horizon, culminationAzimuth)

        val lowest = CrossingSearch.extremum(session, findMax = false, f = ::altitudeAt)

        val above = CrossingSearch.intervalsAbove(session, effectiveHorizon, f = ::altitudeAt)
        val clipTo = if (darkWindows.isEmpty()) listOf(session) else NightSpan.normalise(darkWindows)
        val clipped = ArrayList<NightSpan>()
        for (window in NightSpan.intersectAll(above, clipTo)) {
            if (!window.isEmpty) clipped.add(window)
        }

        val intervals = clipped.map { window ->
            val peak = CrossingSearch.extremum(window, findMax = true, f = ::altitudeAt)
            val trough = CrossingSearch.extremum(window, findMax = false, f = ::altitudeAt)
            val startBoundary = if (window.startUtc == session.startUtc && darkWindows.isEmpty()) {
                WindowBoundary.SESSION_START
            } else if (above.none { it.startUtc == window.startUtc }) {
                WindowBoundary.DARK_WINDOW_START
            } else {
                WindowBoundary.RISE
            }
            val endBoundary = if (window.endUtc == session.endUtc && darkWindows.isEmpty()) {
                WindowBoundary.SESSION_END
            } else if (above.none { it.endUtc == window.endUtc }) {
                WindowBoundary.DARK_WINDOW_END
            } else {
                WindowBoundary.SET
            }
            TargetInterval(
                startUtc = window.startUtc,
                endUtc = window.endUtc,
                startBoundary = startBoundary,
                endBoundary = endBoundary,
                transitUtc = peak.instantUtc,
                peakAltitudeDeg = peak.value,
                minimumAltitudeDeg = trough.value,
                effectiveHorizonDeg = effectiveHorizon,
                clearsDrawnHorizon = fromDrawnHorizon,
            )
        }

        return TargetWindow.of(
            minimumAltitudeDeg = lowest.value,
            minimumAtInstant = lowest.instantUtc,
            maximumAltitudeDeg = highest.value,
            effectiveHorizonDeg = effectiveHorizon,
            intervals = intervals,
        )
    }
}

// =================================================================================================
// State
// =================================================================================================

/**
 * The magnitude limits the filter offers, in the order the chips render. `null` is "no limit", and it
 * is first because it is the state the screen opens in.
 */
val MAGNITUDE_LIMIT_OPTIONS: List<Double?> = listOf(null, 4.0, 6.0, 8.0, 10.0)

/**
 * The callbacks the frozen `TargetsScreen` signature does not carry.
 *
 * `INTERFACES.md` freezes the screen's own parameters (`state`, `onQuery`, `onToggleFavourites`,
 * `onToggleVisibleTonight`, `onOpenTarget`, `onBack`), and the spec requires more controls than those
 * six name — a magnitude limit, an object-type filter, a clear-filters action, the settings entry and
 * the wide-window detail dismissal. They travel here, inside the state, so the frozen signature is
 * honoured exactly and no control is silently dropped. The value is built **once** by the
 * `ViewModel`, so its identity is stable across recompositions.
 */
data class TargetsActions(
    val onMagnitudeLimit: (Double?) -> Unit = {},
    val onTypeFilter: (ObjectType?) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onToggleFavouriteTarget: (String) -> Unit = {},
    val onDismissDetail: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenSpots: () -> Unit = {},
) {
    companion object {
        val NONE = TargetsActions()
    }
}

/**
 * What the catalogue screen renders.
 *
 * [rows] is already filtered and **already in the catalogue's own order** — magnitude ascending,
 * magnitude-unknown rows last, then by `id`. The per-night visibility control produced
 * [rows] as a filter and never as a ranking: nothing in this state re-sorts a list.
 */
data class TargetsUiState(
    val query: String = "",
    val magnitudeLimit: Double? = null,
    val typeFilter: ObjectType? = null,
    val favouritesOnly: Boolean = false,
    val visibleOnly: Boolean = false,
    val filtersExpanded: Boolean = false,
    val rows: List<TargetRowModel> = emptyList(),
    val totalCount: Int = 0,
    val nightDate: LocalDate? = null,
    val nightLabel: String? = null,
    /** No spot is saved: the list is not computed and the scoped empty state is shown. */
    val noSpot: Boolean = false,
    /** The bundled catalogue could not be read. */
    val catalogueUnreadable: Boolean = false,
    /** A night-visibility pass is running; the list is usable meanwhile. */
    val computing: Boolean = false,
    /** The open detail, for the wide window's second pane and for the compact modal sheet. */
    val detail: TargetDetailUiState? = null,
    val actions: TargetsActions = TargetsActions.NONE,
) {
    /** Alias for [query]: the find-handoff token the catalogue SearchBar / search field binds. */
    val searchQuery: String get() = query

    /** True when at least one control changes the result, so the summary can name them. */
    val hasFilters: Boolean
        get() = query.isNotBlank() || magnitudeLimit != null || typeFilter != null ||
            favouritesOnly || visibleOnly
}

// =================================================================================================
// ViewModel
// =================================================================================================

/**
 * The catalogue screen's state owner.
 *
 * It owns exactly three things: the bundled catalogue (read once, offline), the saved spots and
 * favourites, and the filter/selection state. Every visibility answer is delegated to
 * [TargetWindows], and the ordering is the catalogue's own — this class filters and never sorts.
 */
class TargetsViewModel(
    private val catalogue: CatalogueRepository,
    private val store: ScoutStore,
    private val ephemeris: SkyEphemeris = AstronomySkyEphemeris(),
    private val nowProvider: () -> Instant = { Instant.now() },
    onOpenSettings: () -> Unit = {},
    onOpenSpots: () -> Unit = {},
    /**
     * Survives process death for the catalogue search query and magnitude filter (E30 / depth
     * `find` claim). The active spot itself is durable in [ScoutStore]; this handle restores the
     * session's filter surface.
     */
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _uiState: MutableStateFlow<TargetsUiState> = MutableStateFlow(
        TargetsUiState(
            query = savedStateHandle.get<String>(KEY_QUERY).orEmpty(),
            magnitudeLimit = savedStateHandle.get<Double>(KEY_MAGNITUDE_LIMIT),
        ),
    )

    /** Built once so its identity is stable across recompositions. */
    private val actions: TargetsActions = TargetsActions(
        onMagnitudeLimit = { limit ->
            savedStateHandle[KEY_MAGNITUDE_LIMIT] = limit
            _uiState.update { it.copy(magnitudeLimit = limit) }
            recomputeRows()
        },
        onTypeFilter = { type -> _uiState.update { it.copy(typeFilter = type) }; recomputeRows() },
        onClearFilters = {
            savedStateHandle[KEY_QUERY] = ""
            savedStateHandle[KEY_MAGNITUDE_LIMIT] = null
            _uiState.update {
                it.copy(
                    query = "",
                    magnitudeLimit = null,
                    typeFilter = null,
                    favouritesOnly = false,
                    visibleOnly = false,
                )
            }
            recomputeRows()
        },
        onToggleFavouriteTarget = { id -> toggleFavourite(id) },
        onDismissDetail = { _uiState.update { it.copy(detail = null) } },
        onOpenSettings = onOpenSettings,
        onOpenSpots = onOpenSpots,
    )
    val uiState: StateFlow<TargetsUiState> = _uiState.asStateFlow()

    /** The authored catalogue, in `SkyObject.CATALOGUE_ORDER`; empty until it has loaded. */
    private var objects: List<SkyObject> = emptyList()

    /** Per-night visibility, keyed by object id; `null` values are never stored. */
    private var visibleTonight: Map<String, Boolean> = emptyMap()

    private var snapshot: ScoutSnapshot? = null

    init {
        _uiState.value = _uiState.value.copy(actions = actions)
        viewModelScope.launch {
            loadCatalogue()
            store.snapshot.collect { current ->
                snapshot = current
                onSessionChanged()
            }
        }
    }

    private suspend fun loadCatalogue() {
        when (val result = catalogue.load()) {
            is CatalogueResult.Success -> {
                objects = result.objects
                _uiState.update { it.copy(catalogueUnreadable = false) }
                onSessionChanged()
            }
            is CatalogueResult.Unreadable -> {
                objects = emptyList()
                _uiState.update {
                    it.copy(
                        catalogueUnreadable = true,
                        rows = emptyList(),
                        totalCount = 0,
                        computing = false,
                    )
                }
            }
        }
    }

    /** The active spot and its horizon, resolved with the domain's own fallback rule. */
    private fun activeSpot(current: ScoutSnapshot): Scout? =
        ScoutSelection.resolve(current.spots, current.activeSpotId)

    private fun horizonOf(current: ScoutSnapshot, spot: Scout): HorizonProfile? =
        spot.horizonProfileId?.let { current.horizons[it] }

    private fun onSessionChanged() {
        val current = snapshot ?: return
        val spot = activeSpot(current)
        if (spot == null) {
            visibleTonight = emptyMap()
            _uiState.update {
                it.copy(
                    noSpot = true,
                    computing = false,
                    nightDate = null,
                    nightLabel = null,
                    detail = null,
                )
            }
            recomputeRows()
            return
        }
        val zone = runCatching { ZoneId.of(spot.tzId) }.getOrDefault(ZoneId.systemDefault())
        val session = SessionClock.sessionWindow(
            SessionClock.nightOf(nowProvider(), zone),
            zone,
            spot.id,
        )
        val label = formatNightLabel(session.startUtc, session.endUtc, zone, Locale.getDefault())
        _uiState.update { it.copy(noSpot = false, nightDate = SessionClock.dateLabel(session, zone), nightLabel = label) }
        recomputeRows()
        recomputeVisibility(spot, horizonOf(current, spot), session)
    }

    /**
     * The night-visibility pass, off the main thread.
     *
     * It samples every authored row once per invocation; the resulting map is a **filter** input and
     * never an order. When no spot exists the map stays empty and the "Up tonight" control reports the
     * typed needs-a-spot state rather than an empty list.
     */
    private fun recomputeVisibility(
        spot: Scout,
        horizon: HorizonProfile?,
        session: NightSpan,
    ) {
        if (objects.isEmpty()) return
        _uiState.update { it.copy(computing = true) }
        viewModelScope.launch {
            val point = spot.point
            val result = withContext(Dispatchers.Default) {
                objects.associate { obj ->
                    obj.id to TargetWindows.isUpTonight(obj, point, horizon, session, ephemeris)
                }
            }
            visibleTonight = result
            _uiState.update { it.copy(computing = false) }
            recomputeRows()
        }
    }

    /** Applies the filters to the catalogue order; the order itself is never touched. */
    private fun recomputeRows() {
        val state = _uiState.value
        val favourites = snapshot?.favourites.orEmpty()
        val filtered = objects.filter { obj ->
            (state.query.isBlank() || SkyObject.matchesQuery(obj, state.query)) &&
                matchesMagnitude(obj, state.magnitudeLimit) &&
                (state.typeFilter == null || obj.type == state.typeFilter) &&
                (!state.favouritesOnly || obj.id in favourites) &&
                (!state.visibleOnly || visibleTonight[obj.id] == true)
        }
        val rows = filtered.map { obj ->
            TargetRowModel(
                id = obj.id,
                designation = obj.designation,
                name = obj.name,
                nameRu = obj.nameRu,
                type = obj.type,
                magnitude = obj.displayMagnitude,
                magnitudeSystem = obj.magSystem,
                magnitudeKind = obj.magKind,
                magnitudeDisputed = obj.magDisputed,
                sizeMajArcmin = obj.sizeMajArcmin,
                sizeMinArcmin = obj.sizeMinArcmin,
                favourite = obj.id in favourites,
                visibleTonight = visibleTonight[obj.id],
            )
        }
        val detail = state.detail?.let { open ->
            val obj = objects.firstOrNull { it.id == open.targetId } ?: return@let null
            detailStateFor(obj, favourites.contains(obj.id))
        }
        _uiState.update { it.copy(rows = rows, totalCount = filtered.size, detail = detail) }
    }

    /**
     * The magnitude filter, evaluated inside **one** system. The limit is a **V** limit — `magV` is the
     * primary value and `magB` exists only where no V value does — so a row whose only magnitude is B
     * has no value in the compared system and is treated as magnitude-unknown rather than compared
     * across systems (spec: "a row with no value in that system is treated as magnitude-unknown rather
     * than compared across systems"). Rows with no known magnitude are **excluded** while a limit is
     * active ("rows with no known magnitude SHALL be excluded while a limit is active"); with no limit
     * they are listed and the row states `target_magnitude_unknown` in words.
     */
    private fun matchesMagnitude(obj: SkyObject, limit: Double?): Boolean {
        if (limit == null) return true
        val comparable = obj.magV ?: return false
        return comparable <= limit
    }

    // ---- intents ---------------------------------------------------------------------------------

    fun onQuery(query: String) {
        savedStateHandle[KEY_QUERY] = query
        _uiState.update { it.copy(query = query) }
        recomputeRows()
    }

    fun onToggleFavourites() {
        _uiState.update { it.copy(favouritesOnly = !it.favouritesOnly) }
        recomputeRows()
    }

    fun onToggleVisibleTonight() {
        _uiState.update { it.copy(visibleOnly = !it.visibleOnly) }
        recomputeRows()
    }

    fun onToggleFilters() {
        _uiState.update { it.copy(filtersExpanded = !it.filtersExpanded) }
    }

    fun onOpenTarget(id: String) {
        val obj = objects.firstOrNull { it.id == id } ?: return
        val favourites = snapshot?.favourites.orEmpty()
        _uiState.update { it.copy(detail = detailStateFor(obj, id in favourites)) }
    }

    fun onDismissDetail() {
        _uiState.update { it.copy(detail = null) }
    }

    /** Writes the favourite through the store, which is the only durable copy of the set. */
    fun toggleFavourite(id: String) {
        val current = snapshot ?: return
        val updated = if (id in current.favourites) {
            current.favourites - id
        } else {
            current.favourites + id
        }
        viewModelScope.launch { store.setFavourites(updated) }
    }

    private fun detailStateFor(obj: SkyObject, favourite: Boolean): TargetDetailUiState {
        val current = snapshot
        val spot = current?.let { activeSpot(it) }
        val horizon = if (current != null && spot != null) horizonOf(current, spot) else null
        return buildTargetDetailState(
            obj = obj,
            favourite = favourite,
            spot = spot,
            horizon = horizon,
            ephemeris = ephemeris,
            nowUtc = nowProvider(),
            locale = Locale.getDefault(),
        )
    }

    companion object {
        private const val KEY_QUERY: String = "targets.query"
        private const val KEY_MAGNITUDE_LIMIT: String = "targets.magnitudeLimit"

        fun factory(
            context: Context,
            store: ScoutStore = ScoutStore(context.applicationContext),
            ephemeris: SkyEphemeris = AstronomySkyEphemeris(),
            nowProvider: () -> Instant = { Instant.now() },
            onOpenSettings: () -> Unit = {},
            onOpenSpots: () -> Unit = {},
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                TargetsViewModel(
                    catalogue = CatalogueRepository(context.applicationContext),
                    store = store,
                    ephemeris = ephemeris,
                    nowProvider = nowProvider,
                    onOpenSettings = onOpenSettings,
                    onOpenSpots = onOpenSpots,
                    savedStateHandle = extras.createSavedStateHandle(),
                ) as T

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TargetsViewModel(
                    catalogue = CatalogueRepository(context.applicationContext),
                    store = store,
                    ephemeris = ephemeris,
                    nowProvider = nowProvider,
                    onOpenSettings = onOpenSettings,
                    onOpenSpots = onOpenSpots,
                ) as T
        }
    }
}

// =================================================================================================
// Screen
// =================================================================================================

/**
 * The catalogue: search, filters and the list, with the detail beside it in a wide window.
 *
 * The signature is the frozen one from `INTERFACES.md`. The wide-window branch reads
 * `LocalConfiguration.current.screenWidthDp` and keeps the list and the detail on screen together at
 * 600 dp and above, where the compact layout opens the detail as a modal sheet instead.
 */
@Composable
fun TargetsScreen(
    state: TargetsUiState,
    onQuery: (String) -> Unit,
    onToggleFavourites: () -> Unit,
    onToggleVisibleTonight: () -> Unit,
    onOpenTarget: (String) -> Unit,
    onBack: () -> Unit,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        LumintraScaffoldHeader(
            title = stringResource(R.string.target_title),
            onBack = onBack,
        ) {
            IconButton(
                onClick = state.actions.onOpenSettings,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_settings),
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SearchRow(state = state, onQuery = onQuery)
        Spacer(Modifier.height(8.dp))
        FilterBar(
            state = state,
            onToggleFavourites = onToggleFavourites,
            onToggleVisibleTonight = onToggleVisibleTonight,
        )
        ResultsSummary(state = state)
        Spacer(Modifier.height(8.dp))

        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    TargetList(state = state, onOpenTarget = onOpenTarget)
                }
                VerticalDivider(modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    val detail = state.detail
                    if (detail == null) {
                        LumintraEmptyState(
                            title = stringResource(R.string.target_detail_select_prompt),
                            body = stringResource(R.string.target_detail_select_prompt_body),
                            iconRes = R.drawable.ic_ui_telescope,
                        )
                    } else {
                        TargetDetailPane(
                            state = detail,
                            onToggleFavourite = { state.actions.onToggleFavouriteTarget(detail.targetId) },
                        )
                    }
                }
            }
        } else {
            TargetList(state = state, onOpenTarget = onOpenTarget)
            val detail = state.detail
            if (detail != null) {
                TargetDetailSheet(
                    state = detail,
                    onToggleFavourite = { state.actions.onToggleFavouriteTarget(detail.targetId) },
                    onDismiss = state.actions.onDismissDetail,
                )
            }
        }
    }
}

/**
 * Catalogue search: Material 3 [SearchBar] wired to the ViewModel query, with the magnitude filter
 * available in the expanded panel so the depth `find` claim's `SearchBar` token is present here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchRow(state: TargetsUiState, onQuery: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val clearLabel = stringResource(R.string.target_search_clear)
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = state.query,
                onQueryChange = onQuery,
                onSearch = { _ -> expanded = false },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.target_search_field)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_search),
                        contentDescription = stringResource(R.string.target_search_hint),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQuery("") },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_ui_x),
                                contentDescription = clearLabel,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.target_filter_magnitude),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (limit in MAGNITUDE_LIMIT_OPTIONS) {
                    FilterChip(
                        selected = state.magnitudeLimit == limit,
                        onClick = { state.actions.onMagnitudeLimit(limit) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        label = {
                            if (limit == null) {
                                Text(stringResource(R.string.target_magnitude_limit_any))
                            } else {
                                Text(
                                    stringResource(
                                        R.string.target_filter_magnitude_value,
                                        String.format(Locale.getDefault(), "%.1f", limit),
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * The filter bar. Every control is a `FilterChip` — a real 48 dp target with its own selected state —
 * and the selected state is carried by the chip's own semantics as well as by its container colour, so
 * the choice is never colour alone.
 */
@Composable
private fun FilterBar(
    state: TargetsUiState,
    onToggleFavourites: () -> Unit,
    onToggleVisibleTonight: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScrollIfNeeded(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.favouritesOnly,
                onClick = onToggleFavourites,
                modifier = Modifier.minimumInteractiveComponentSize(),
                label = { Text(stringResource(R.string.target_filter_favourites)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_star),
                        contentDescription = null,
                    )
                },
            )
            FilterChip(
                selected = state.visibleOnly,
                onClick = onToggleVisibleTonight,
                modifier = Modifier.minimumInteractiveComponentSize(),
                label = { Text(stringResource(R.string.target_filter_visible_tonight)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_eye),
                        contentDescription = null,
                    )
                },
            )
            FilterChip(
                selected = state.magnitudeLimit != null,
                onClick = { state.actions.onMagnitudeLimit(nextMagnitudeLimit(state.magnitudeLimit)) },
                modifier = Modifier.minimumInteractiveComponentSize(),
                label = {
                    Text(
                        text = state.magnitudeLimit?.let { limit ->
                            stringResource(
                                R.string.target_filter_magnitude_value,
                                String.format(Locale.getDefault(), "%.1f", limit),
                            )
                        } ?: stringResource(R.string.target_filter_magnitude),
                    )
                },
            )
            FilterChip(
                selected = state.typeFilter != null,
                onClick = { state.actions.onTypeFilter(nextTypeFilter(state.typeFilter)) },
                modifier = Modifier.minimumInteractiveComponentSize(),
                label = {
                    Text(
                        text = state.typeFilter?.let { stringResource(objectTypeLabel(it)) }
                            ?: stringResource(R.string.target_filter_type),
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_layers),
                        contentDescription = null,
                    )
                },
            )
            if (state.hasFilters) {
                TextButton(
                    onClick = state.actions.onClearFilters,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text(stringResource(R.string.target_filter_clear))
                }
            }
        }
    }
}

/** The summary line: how many rows, which filters, and which night they were judged against. */
@Composable
private fun ResultsSummary(state: TargetsUiState) {
    val summary = if (state.hasFilters) {
        stringResource(R.string.target_results_summary_filters, state.totalCount, activeFilterWords(state))
    } else {
        stringResource(R.string.target_results_summary, state.totalCount)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val night = state.nightLabel
        if (night != null) {
            Text(
                text = stringResource(R.string.target_night_named, night),
                style = LumintraType.clockSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun activeFilterWords(state: TargetsUiState): String {
    val words = ArrayList<String>()
    if (state.query.isNotBlank()) words.add(state.query)
    if (state.magnitudeLimit != null) {
        words.add(
            stringResource(
                R.string.target_filter_magnitude_value,
                String.format(Locale.getDefault(), "%.1f", state.magnitudeLimit),
            ),
        )
    }
    if (state.typeFilter != null) words.add(stringResource(objectTypeLabel(state.typeFilter)))
    if (state.favouritesOnly) words.add(stringResource(R.string.target_filter_favourites))
    if (state.visibleOnly) words.add(stringResource(R.string.target_filter_visible_tonight))
    return if (words.isEmpty()) stringResource(R.string.target_filters_none) else words.joinToString(", ")
}

/**
 * The list, or the one typed empty state that explains why there is none.
 *
 * The order is the state's; this composable renders `state.rows` as it arrives and never sorts.
 */
@Composable
private fun TargetList(state: TargetsUiState, onOpenTarget: (String) -> Unit) {
    when {
        state.catalogueUnreadable -> LumintraEmptyState(
            title = stringResource(R.string.target_list_unavailable),
            body = stringResource(R.string.target_list_unavailable_body),
            iconRes = R.drawable.ic_ui_triangle_alert,
        )

        state.noSpot -> LumintraEmptyState(
            title = stringResource(R.string.target_needs_spot),
            body = stringResource(R.string.target_needs_spot_body),
            iconRes = R.drawable.ic_ui_map_pin,
            actionLabel = stringResource(R.string.target_needs_spot_action),
            onAction = state.actions.onOpenSpots,
        )

        state.rows.isEmpty() && state.query.isNotBlank() -> LumintraEmptyState(
            title = stringResource(R.string.target_no_result, state.query),
            body = stringResource(R.string.target_no_result_body),
            iconRes = R.drawable.ic_ui_search,
            actionLabel = stringResource(R.string.target_filter_clear),
            onAction = state.actions.onClearFilters,
        )

        state.rows.isEmpty() && state.favouritesOnly -> LumintraEmptyState(
            title = stringResource(R.string.target_favourites_empty),
            body = stringResource(R.string.target_favourites_empty_body),
            iconRes = R.drawable.ic_ui_star,
            actionLabel = stringResource(R.string.target_filter_clear),
            onAction = state.actions.onClearFilters,
        )

        state.rows.isEmpty() && state.visibleOnly -> LumintraEmptyState(
            title = stringResource(
                R.string.target_none_visible_tonight,
                state.nightLabel ?: stringResource(R.string.target_needs_night),
            ),
            body = stringResource(R.string.target_none_visible_tonight_body),
            iconRes = R.drawable.ic_ui_moon_star,
            actionLabel = stringResource(R.string.target_filter_clear),
            onAction = state.actions.onClearFilters,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = state.rows, key = { row -> row.id }) { row ->
                TargetRow(
                    model = row,
                    selected = state.detail?.targetId == row.id,
                    onOpen = { onOpenTarget(row.id) },
                    onToggleFavourite = { state.actions.onToggleFavouriteTarget(row.id) },
                )
            }
        }
    }
}

/** The next limit in the chip's cycle: `null → 4 → 6 → 8 → 10 → null`. */
private fun nextMagnitudeLimit(current: Double?): Double? {
    val index = MAGNITUDE_LIMIT_OPTIONS.indexOf(current)
    return MAGNITUDE_LIMIT_OPTIONS[(index + 1) % MAGNITUDE_LIMIT_OPTIONS.size]
}

/** The next object-type filter in the chip's cycle: `null → …declared types… → null`. */
private fun nextTypeFilter(current: ObjectType?): ObjectType? {
    val filterable = ObjectType.entries.filterNot { it.isComputedOnly }
    if (current == null) return filterable.first()
    val index = filterable.indexOf(current)
    if (index < 0 || index == filterable.lastIndex) return null
    return filterable[index + 1]
}

/** A row that scrolls only when it must, so a wide window is not given a pointless scroll container. */
@Composable
private fun Modifier.horizontalScrollIfNeeded(): Modifier = this
