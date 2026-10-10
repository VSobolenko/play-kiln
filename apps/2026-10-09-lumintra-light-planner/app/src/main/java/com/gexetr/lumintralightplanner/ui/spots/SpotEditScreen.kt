package com.gexetr.lumintralightplanner.ui.spots

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.domain.EquipmentProfile
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.ui.components.ChipState
import com.gexetr.lumintralightplanner.ui.components.ConditionChip
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.components.ValueRow
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.PlayKilnTheme
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =================================================================================================
// Spot editor — the name, the coordinate (typed, pasted, or picked), the zone, and the horizon
// =================================================================================================
//
// The horizon editor is a SECTION OF THIS SCREEN and never a separate destination
// (openspec/changes/app-lumintra-light-planner/specs/scout-spots). It has to be: the horizon is
// meaningless without the coordinate it was drawn from, and a second screen would let a user draw a
// skyline for a spot they never saved.
//
// Three rules shape the file, and each one is a state the app would otherwise get wrong:
//
//   1. **There is no default coordinate.** The draft starts with empty coordinate fields,
//      `coordinateSet = false`, and a Save that refuses. `(0, 0)` is a real coordinate in the Gulf
//      of Guinea, and defaulting to it produces a confident plan for a place the user is not.
//   2. **A place name is a hint, not a measurement.** `Geoapify`-class lookups are not available
//      here; `android.location.Geocoder` is the only geocoder, it takes no key, and its answer is
//      labelled `approximate` (Scout.approximate) so nothing downstream reads a resolved town as a
//      precise viewpoint. A missing implementation, no network, and an empty result are all normal
//      states that leave the coordinate and the user's own label untouched.
//   3. **The zone is an IANA id.** A numeric offset is wrong for half the year, so the picker is
//      `ZoneId.getAvailableZoneIds()`, the default comes from the raw offset nearest the
//      coordinates, and the user can always override. When resolution fails the device zone is used
//      AND labelled as a fallback rather than silently substituted.

/** What the coordinate field is complaining about. Typed, so the screen localises it, never Kotlin. */
enum class CoordinateIssue { UNREADABLE, OUT_OF_RANGE }

/** Why Save refused. Typed for the same reason. */
enum class SpotValidation { NONE, NAME_MISSING, COORDINATE_MISSING, ELEVATION_INVALID }

/** Where the current zone came from, so the screen can label the two fallbacks honestly. */
enum class ZoneSource { DEVICE_DEFAULT, RESOLVED_FROM_COORDINATES, DEVICE_FALLBACK, USER_CHOSEN }

/** The place-name lookup's state. Every one of these is a normal outcome, not an error path. */
sealed interface LookupState {
    data object Idle : LookupState
    data object Searching : LookupState

    /** `Geocoder.isPresent()` was false: no backend on this device, so no call was attempted. */
    data object Unavailable : LookupState
    data object Empty : LookupState
    data class Results(val hits: List<GeocodeHit>) : LookupState
    data object ReverseEmpty : LookupState
}

/**
 * Everything the editor renders.
 *
 * [latitudeText] and [longitudeText] are the user's OWN text, unnormalised, so a half-typed value
 * stays exactly as it was typed; [latitudeDeg] and [longitudeDeg] are the parsed reading, and
 * [coordinateSet] is true only when both halves parsed and are inside their ranges. Keeping both is
 * what lets the screen show "47," while refusing to save it.
 */
data class SpotEditUiState(
    val loading: Boolean = true,
    val editingExisting: Boolean = false,
    val draftId: String = "",
    val name: String = "",
    val latitudeText: String = "",
    val longitudeText: String = "",
    val elevationText: String = "",
    val latitudeDeg: Double? = null,
    val longitudeDeg: Double? = null,
    val coordinateSet: Boolean = false,
    val approximate: Boolean = false,
    val latitudeIssue: CoordinateIssue? = null,
    val longitudeIssue: CoordinateIssue? = null,
    val pasteError: PairFailure? = null,
    val pasteApplied: Boolean = false,
    val entryFormat: CoordinateEntryFormat = CoordinateEntryFormat.DECIMAL,
    val tzId: String = "",
    val zoneSource: ZoneSource = ZoneSource.DEVICE_DEFAULT,
    val zoneIds: List<String> = emptyList(),
    val zoneQuery: String = "",
    val lookupQuery: String = "",
    val lookup: LookupState = LookupState.Idle,
    val pickerOpen: Boolean = false,
    val pickerCentre: GeodeticPoint? = null,
    val horizon: HorizonProfile? = null,
    val horizonFieldDeg: Int = 30,
    val equipment: EquipmentProfile = EquipmentProfile.DEFAULT,
    val dirty: Boolean = false,
    val validation: SpotValidation = SpotValidation.NONE,
    /** Non-null once the store has taken the record; the shell navigates on it. */
    val savedSpotId: String? = null,
) {
    /** The coordinate the picker shows, or `null` while none is set. Never a fabricated (0, 0). */
    val point: GeodeticPoint? = if (coordinateSet && latitudeDeg != null && longitudeDeg != null) {
        GeodeticPoint(latitudeDeg, longitudeDeg)
    } else {
        null
    }

    val dmsLatitude: DmsParts get() = CoordinateText.dmsParts(latitudeDeg ?: 0.0)
    val dmsLongitude: DmsParts get() = CoordinateText.dmsParts(longitudeDeg ?: 0.0)

    /** True when a horizon has been drawn and every sample is zero: legal, and worth saying so. */
    val horizonFlat: Boolean
        get() = horizon != null && horizon.lowestDeg == 0.0 && horizon.highestDeg == 0.0
}

// -------------------------------------------------------------------------------------------------
// The zone hint: which IANA zone sits nearest a coordinate
// -------------------------------------------------------------------------------------------------

/**
 * The zone a coordinate most plausibly belongs to.
 *
 * `java.time` carries no zone longitudes, so the rule is the one the spec states: the coordinate's
 * **nominally correct offset** (`longitude / 15` hours) is compared with every zone's *standard*
 * offset, and the closest wins. Standard rather than current offset is deliberate — a summer-time
 * zone must not win the tie in July and lose it in January, or a saved spot's zone would move twice
 * a year. Ties break on the zone id so the answer is deterministic rather than filesystem order.
 */
internal object ZoneHints {

    fun nearestTo(
        latitudeDeg: Double,
        longitudeDeg: Double,
        available: Set<String>,
        atUtc: Instant = Instant.now(),
    ): String? {
        if (available.isEmpty()) return null
        if (latitudeDeg !in -90.0..90.0 || longitudeDeg !in -180.0..180.0) return null
        val targetHours = longitudeDeg / 15.0
        var best: String? = null
        var bestDistance = Double.MAX_VALUE
        for (id in available) {
            val zone = runCatching { ZoneId.of(id) }.getOrNull() ?: continue
            val hours = zone.rules.getStandardOffset(atUtc).totalSeconds / 3600.0
            val distance = abs(hours - targetHours)
            if (distance < bestDistance || (distance == bestDistance && best != null && id < best)) {
                bestDistance = distance
                best = id
            }
        }
        return best
    }

    /**
     * Where the canvas opens when the spot has no coordinate yet: the meridian of its zone's own
     * offset, at the equator.
     *
     * This is a **view centre, never a coordinate.** It is only ever passed to the picker's
     * `centreHint`, which pans the canvas and places no pin, so no value here can become the spot's
     * position.
     */
    fun zoneMeridianHint(zoneId: String, atUtc: Instant = Instant.now()): GeodeticPoint {
        val hours = runCatching { ZoneId.of(zoneId) }
            .getOrNull()
            ?.rules
            ?.getStandardOffset(atUtc)
            ?.totalSeconds
            ?.div(3600.0)
            ?: 0.0
        return GeodeticPoint(0.0, (hours * 15.0).coerceIn(-180.0, 180.0))
    }
}

// -------------------------------------------------------------------------------------------------
// The geocoder as a seam
// -------------------------------------------------------------------------------------------------

/**
 * The only geocoding the app may use, behind one interface.
 *
 * `android.location.Geocoder` needs no key and no permission, which is why it is the only source
 * here; a third-party geocoding API would need a credential inside the APK, which
 * `.cursor/rules/client-secrets.mdc` forbids. The interface exists so the ViewModel can be driven
 * by a fake in a test without a device.
 */
interface GeocodingGateway {
    fun isPresent(): Boolean
    suspend fun forward(query: String): List<GeocodeHit>
    suspend fun reverse(point: GeodeticPoint): String?
}

/** The real gateway: [PlatformGeocoder], with the locale read at call time rather than captured. */
class PlatformGeocoding(
    private val context: Context,
    private val locale: () -> Locale = { Locale.getDefault() },
) : GeocodingGateway {
    override fun isPresent(): Boolean = PlatformGeocoder.isPresent()

    override suspend fun forward(query: String): List<GeocodeHit> =
        PlatformGeocoder.forwardLookup(context, query, locale())

    override suspend fun reverse(point: GeodeticPoint): String? =
        PlatformGeocoder.reverseLookup(context, point, locale())
}

// -------------------------------------------------------------------------------------------------
// The editor's state holder
// -------------------------------------------------------------------------------------------------

/**
 * Owns the draft spot, and only the draft.
 *
 * The committed record lives in `ScoutStore`; this ViewModel holds the user's edits until Save, so
 * abandoning a draft leaves no half-written spot and no orphan horizon profile behind — the horizon
 * is written with the spot, in the same coroutine, keyed by the same id.
 */
class SpotEditViewModel(
    private val store: ScoutStore,
    private val spotId: String?,
    private val gateway: GeocodingGateway,
    initialFormat: CoordinateEntryFormat = CoordinateEntryFormat.DECIMAL,
    private val availableZoneIds: () -> Set<String> = { ZoneId.getAvailableZoneIds() },
    private val deviceZoneId: () -> String = { ZoneId.systemDefault().id },
) : ViewModel() {

    private val draft: MutableStateFlow<SpotEditUiState> = MutableStateFlow(
        SpotEditUiState(
            entryFormat = initialFormat,
            draftId = spotId ?: newSpotId(),
            editingExisting = spotId != null,
        ),
    )

    val state: StateFlow<SpotEditUiState> = draft.asStateFlow()

    /** The saved record as it was loaded, so [SpotEditUiState.dirty] is a comparison and not a guess. */
    private var original: Scout? = null
    private var originalSamples: FloatArray? = null

    /**
     * The in-flight zone resolution. Cancelled by the next coordinate edit and by an explicit
     * choice, so a slow keystroke cannot land its zone on top of the user's own pick.
     */
    private var zoneResolution: Job? = null

    init {
        viewModelScope.launch {
            val snapshot = store.snapshot.first()
            val existing = spotId?.let { id -> snapshot.spots.firstOrNull { it.id == id } }
            val horizon = existing?.horizonProfileId?.let { snapshot.horizons[it] }
            original = existing
            originalSamples = horizon?.altitudeDeg?.copyOf()

            val zones = withContext(Dispatchers.Default) { availableZoneIds().toList().sorted() }
            val device = deviceZoneId()
            val mapCentre = existing?.point ?: ZoneHints.zoneMeridianHint(device)

            updated { current ->
                current.copy(
                    loading = false,
                    editingExisting = existing != null,
                    draftId = existing?.id ?: current.draftId,
                    name = existing?.name.orEmpty(),
                    latitudeText = existing?.let { CoordinateText.formatDecimal(it.latDeg) }.orEmpty(),
                    longitudeText = existing?.let { CoordinateText.formatDecimal(it.lonDeg) }.orEmpty(),
                    elevationText = existing?.let { CoordinateText.formatDecimal(it.elevationM) }.orEmpty(),
                    latitudeDeg = existing?.latDeg,
                    longitudeDeg = existing?.lonDeg,
                    coordinateSet = existing != null,
                    approximate = existing?.approximate ?: false,
                    tzId = existing?.tzId ?: device,
                    // A brand-new spot has no coordinate for the zone to be derived from, so the
                    // device zone is the plain default and is NOT labelled a fallback: nothing failed.
                    zoneSource = ZoneSource.DEVICE_DEFAULT,
                    zoneIds = zones,
                    horizon = horizon,
                    equipment = existing?.equipment ?: EquipmentProfile.DEFAULT,
                    // Map stays collapsed on create: an open 320 dp canvas ate scroll gestures and
                    // pushed Save under the nav bar. The user opens it with "Adjust on map".
                    pickerOpen = false,
                    pickerCentre = mapCentre,
                )
            }
        }
    }

    // --- edits -------------------------------------------------------------------------------------

    fun onName(text: String) = updated { it.copy(name = text, validation = SpotValidation.NONE) }

    fun onElevation(text: String) = updated { it.copy(elevationText = text, validation = SpotValidation.NONE) }

    fun onEntryFormat(format: CoordinateEntryFormat) = updated { it.copy(entryFormat = format) }

    fun onPaste(text: String) {
        when (val parsed = CoordinateText.parsePair(text)) {
            is PairParse.Ok -> updated {
                it.copy(
                    // The paste is normalised to the canonical decimal form and then travels the same
                    // channel as a typed value, so there is exactly one path that sets a coordinate.
                    latitudeText = CoordinateText.formatDecimal(parsed.latitudeDeg),
                    longitudeText = CoordinateText.formatDecimal(parsed.longitudeDeg),
                    latitudeDeg = parsed.latitudeDeg,
                    longitudeDeg = parsed.longitudeDeg,
                    coordinateSet = true,
                    latitudeIssue = null,
                    longitudeIssue = null,
                    pasteError = null,
                    pasteApplied = true,
                    approximate = false,
                    validation = SpotValidation.NONE,
                )
            }

            is PairParse.Failed -> updated { it.copy(pasteError = parsed.reason, pasteApplied = false) }
        }
    }

    /**
     * The single coordinate channel: the decimal fields, the degrees/minutes/seconds fields, the
     * pasted pair and the pin all arrive here.
     *
     * A coordinate that matches one of the outstanding place-name results is the one case that is
     * marked [SpotEditUiState.approximate], and that is decided by comparing the values rather than
     * by trusting a flag: a lookup result the user then corrects by hand is no longer approximate.
     */
    fun onLatLon(latitudeText: String, longitudeText: String) {
        val latitude = CoordinateText.parseDecimal(latitudeText)
        val longitude = CoordinateText.parseDecimal(longitudeText)
        val hits = (draft.value.lookup as? LookupState.Results)?.hits.orEmpty()
        val applied = hits.firstOrNull { it.latitudeDeg == latitude && it.longitudeDeg == longitude }
        val latitudeOk = latitude != null && latitude in -90.0..90.0
        val longitudeOk = longitude != null && longitude in -180.0..180.0

        updated { current ->
            val ready = latitudeOk && longitudeOk
            current.copy(
                latitudeText = latitudeText,
                longitudeText = longitudeText,
                latitudeDeg = latitude,
                longitudeDeg = longitude,
                coordinateSet = ready,
                latitudeIssue = when {
                    latitude == null && latitudeText.isNotBlank() -> CoordinateIssue.UNREADABLE
                    latitude != null && !latitudeOk -> CoordinateIssue.OUT_OF_RANGE
                    else -> null
                },
                longitudeIssue = when {
                    longitude == null && longitudeText.isNotBlank() -> CoordinateIssue.UNREADABLE
                    longitude != null && !longitudeOk -> CoordinateIssue.OUT_OF_RANGE
                    else -> null
                },
                pasteError = null,
                pasteApplied = false,
                approximate = applied != null,
                // A resolved name fills an empty label and never overwrites the user's own words.
                name = if (applied != null && current.name.isBlank()) applied.label else current.name,
                lookup = if (applied != null) LookupState.Idle else current.lookup,
                pickerCentre = if (ready) GeodeticPoint(latitude!!, longitude!!) else current.pickerCentre,
                validation = if (ready) SpotValidation.NONE else current.validation,
            )
        }
        if (latitudeOk && longitudeOk) {
            // Off the main thread, and cancelled by the next keystroke: comparing a coordinate with
            // every zone in the tz database is ~600 `ZoneId.of` calls, which is not work a text field
            // may do between two characters.
            scheduleZoneResolution(latitude!!, longitude!!)
        }
    }

    fun onTz(zoneId: String) {
        // The user's pick outranks the coordinate, so a resolution still in flight is dropped.
        zoneResolution?.cancel()
        updated { it.copy(tzId = zoneId, zoneSource = ZoneSource.USER_CHOSEN, zoneQuery = "") }
    }

    fun onZoneQuery(text: String) = updated { it.copy(zoneQuery = text) }

    /** "Use the device time zone": an explicit choice, so it stops following the coordinates. */
    fun onUseDeviceZone() {
        zoneResolution?.cancel()
        updated { it.copy(tzId = deviceZoneId(), zoneSource = ZoneSource.USER_CHOSEN, zoneQuery = "") }
    }

    fun onLookupQuery(text: String) = updated { it.copy(lookupQuery = text) }

    fun onLookupClear() = updated { it.copy(lookup = LookupState.Idle, lookupQuery = "") }

    fun onLookupSearch() {
        val query = draft.value.lookupQuery.trim()
        if (query.isEmpty()) return
        if (!gateway.isPresent()) {
            updated { it.copy(lookup = LookupState.Unavailable) }
            return
        }
        updated { it.copy(lookup = LookupState.Searching) }
        viewModelScope.launch {
            val hits = gateway.forward(query)
            updated {
                it.copy(lookup = if (hits.isEmpty()) LookupState.Empty else LookupState.Results(hits))
            }
        }
    }

    /**
     * Names the current coordinate from the geocoder.
     *
     * The coordinate is never touched — only the label is offered, and only while the field is
     * empty. `ReverseEmpty` is the ordinary "no name found" answer, and the user's own words stay.
     */
    fun onReverseLookup() {
        val point = draft.value.point ?: return
        if (!gateway.isPresent()) {
            updated { it.copy(lookup = LookupState.Unavailable) }
            return
        }
        updated { it.copy(lookup = LookupState.Searching) }
        viewModelScope.launch {
            val label = gateway.reverse(point)
            updated { current ->
                current.copy(
                    name = if (current.name.isBlank() && !label.isNullOrBlank()) label else current.name,
                    lookup = if (label.isNullOrBlank()) LookupState.ReverseEmpty else LookupState.Idle,
                )
            }
        }
    }

    fun onPickOnMap() = updated { it.copy(pickerOpen = !it.pickerOpen) }

    fun onHorizonField(fieldDeg: Int) = updated { it.copy(horizonFieldDeg = fieldDeg) }

    /**
     * One whole-degree sample, written through [HorizonProfile.withSample].
     *
     * The 360-sample array is the truth and the canvas only views it, so a sample survives a rotation
     * and reads identically at any screen size. The first sample creates the profile; nothing is
     * written to the store until Save.
     */
    fun onHorizonSample(index: Int, elevationDeg: Double) = updated { current ->
        val base = current.horizon ?: transientHorizon(current)
        current.copy(horizon = base.withSample(index, elevationDeg))
    }

    fun onHorizonReset() = updated { it.copy(horizon = null) }

    /** Copies the readout for a user who wants to paste it into a message or a logbook. */
    fun onCopyCoordinate(context: Context) {
        val point = draft.value.point ?: return
        val clip = context.getSystemService(ClipboardManager::class.java) ?: return
        clip.setPrimaryClip(
            ClipData.newPlainText(
                "coordinate",
                CoordinateText.formatDecimalPair(point.latDeg, point.lonDeg),
            ),
        )
    }

    // --- committing --------------------------------------------------------------------------------

    fun onSave() {
        val current = draft.value
        if (current.name.isBlank()) {
            updated { it.copy(validation = SpotValidation.NAME_MISSING) }
            return
        }
        if (!current.coordinateSet) {
            // The refusal is the requirement: no spot is ever saved at an assumed position.
            updated { it.copy(validation = SpotValidation.COORDINATE_MISSING) }
            return
        }
        val elevation = current.elevationText.trim().let { text ->
            if (text.isEmpty()) 0.0 else CoordinateText.parseDecimal(text)
        }
        if (elevation == null) {
            updated { it.copy(validation = SpotValidation.ELEVATION_INVALID) }
            return
        }
        val latitude = current.latitudeDeg ?: return
        val longitude = current.longitudeDeg ?: return
        val zoneId = current.tzId.ifBlank { deviceZoneId() }

        // The horizon the user drew is rebuilt here with the spot's real coordinate, so the record
        // that reaches the store carries the place it belongs to rather than the draft's placeholder.
        val drawn = current.horizon?.let { profile ->
            HorizonProfile(
                id = horizonIdFor(current.draftId),
                name = current.name.trim(),
                latitudeDeg = latitude,
                longitudeDeg = longitude,
                altitudeDeg = profile.altitudeDeg,
            )
        }
        val spot = Scout(
            id = current.draftId,
            name = current.name.trim(),
            latDeg = latitude,
            lonDeg = longitude,
            elevationM = elevation,
            tzId = zoneId,
            horizonProfileId = drawn?.id,
            equipment = current.equipment,
            approximate = current.approximate,
        )

        viewModelScope.launch {
            store.upsertSpot(spot)
            drawn?.let { store.upsertHorizon(it) }
            draft.update { it.copy(savedSpotId = spot.id, validation = SpotValidation.NONE) }
        }
    }

    // --- internals ---------------------------------------------------------------------------------

    /** Applies [transform] and refreshes [SpotEditUiState.dirty] from the loaded original. */
    private fun updated(transform: (SpotEditUiState) -> SpotEditUiState) {
        draft.update { current ->
            val next = transform(current)
            next.copy(dirty = next.dirty || isDirty(next))
        }
    }

    private fun isDirty(current: SpotEditUiState): Boolean {
        val loaded = original
        if (loaded == null) {
            return current.name.isNotBlank() ||
                current.coordinateSet ||
                current.horizon != null ||
                current.elevationText.isNotBlank()
        }
        val elevation = current.elevationText.trim().let { text ->
            if (text.isEmpty()) 0.0 else CoordinateText.parseDecimal(text)
        }
        return current.name.trim() != loaded.name ||
            current.latitudeDeg != loaded.latDeg ||
            current.longitudeDeg != loaded.lonDeg ||
            elevation != loaded.elevationM ||
            current.tzId != loaded.tzId ||
            current.approximate != loaded.approximate ||
            !samplesEqual(current.horizon?.altitudeDeg, originalSamples)
    }

    /**
     * Resolves the spot's zone from its coordinate, off the main thread, and applies it.
     *
     * Comparing a coordinate against every zone in the tz database is ~600 `ZoneId.of` calls plus a
     * standard-offset read; that is not work a text field may do between two characters. The job is
     * cancellable, so the latest keystroke wins and a superseded one never reaches the state.
     *
     * A fallback is [ZoneSource.DEVICE_FALLBACK] rather than a silent success, so the screen can say
     * the zone is the device's and not the spot's.
     */
    private fun scheduleZoneResolution(latitudeDeg: Double, longitudeDeg: Double) {
        zoneResolution?.cancel()
        zoneResolution = viewModelScope.launch {
            val resolution = withContext(Dispatchers.Default) {
                val known = draft.value.zoneIds
                val zones = if (known.isEmpty()) availableZoneIds() else known.toSet()
                val resolved = ZoneHints.nearestTo(latitudeDeg, longitudeDeg, zones)
                if (resolved != null) {
                    resolved to ZoneSource.RESOLVED_FROM_COORDINATES
                } else {
                    deviceZoneId() to ZoneSource.DEVICE_FALLBACK
                }
            }
            updated { current ->
                when {
                    // An explicit choice outranks the coordinate and is never overwritten.
                    current.zoneSource == ZoneSource.USER_CHOSEN -> current
                    // A cancellation between the launch and here leaves the field as the user left it.
                    !current.coordinateSet -> current
                    else -> current.copy(tzId = resolution.first, zoneSource = resolution.second)
                }
            }
        }
    }

    private fun transientHorizon(current: SpotEditUiState): HorizonProfile = HorizonProfile.flat(
        id = horizonIdFor(current.draftId),
        name = current.name.trim(),
        // The draft's own coordinate when it has one. The (0, 0) fallback here is transient and is
        // never stored: `onSave` rebuilds the profile with the real coordinate before it is written,
        // and no screen ever reads this field for display.
        latitudeDeg = current.latitudeDeg ?: 0.0,
        longitudeDeg = current.longitudeDeg ?: 0.0,
        elevationDeg = 0.0,
    )

    private fun newSpotId(): String = "spot-" + UUID.randomUUID().toString().take(8)

    private fun horizonIdFor(id: String): String = "$id.horizon"

    private fun samplesEqual(left: FloatArray?, right: FloatArray?): Boolean = when {
        left === right -> true
        left == null || right == null -> false
        else -> left.contentEquals(right)
    }

    companion object {
        /**
         * Manual dependencies, and the application context rather than the activity's: the geocoder
         * outlives the composition that created it, so holding an activity here would be a leak.
         */
        fun factory(
            context: Context,
            store: ScoutStore,
            spotId: String?,
            initialFormat: CoordinateEntryFormat = CoordinateEntryFormat.DECIMAL,
        ): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SpotEditViewModel(
                        store = store,
                        spotId = spotId,
                        gateway = PlatformGeocoding(applicationContext),
                        initialFormat = initialFormat,
                    ) as T
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// The screen
// -------------------------------------------------------------------------------------------------

/**
 * The spot editor.
 *
 * The ten parameters of `INTERFACES.md` are the contract and are unchanged. The trailing parameters
 * are **defaulted**, so the documented call site — `SpotEditScreen(state, onName, onLatLon,
 * onElevation, onTz, onPickOnMap, onHorizonSample, onHorizonReset, onSave, onCancel)` — still
 * compiles and behaves; the extras carry the interactions that have no channel in the frozen list
 * (the lookup query and its search, the paste box, the notation switch, the horizon's vertical
 * field, and delete). Passing none of them leaves a screen that is complete except for those.
 */
@Composable
fun SpotEditScreen(
    state: SpotEditUiState,
    onName: (String) -> Unit,
    onLatLon: (String, String) -> Unit,
    onElevation: (String) -> Unit,
    onTz: (String) -> Unit,
    onPickOnMap: () -> Unit,
    onHorizonSample: (Int, Double) -> Unit,
    onHorizonReset: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onPaste: (String) -> Unit = {},
    onEntryFormat: (CoordinateEntryFormat) -> Unit = {},
    onLookupQuery: (String) -> Unit = {},
    onLookupSearch: () -> Unit = {},
    onReverseLookup: () -> Unit = {},
    onLookupClear: () -> Unit = {},
    onZoneQuery: (String) -> Unit = {},
    onUseDeviceZone: () -> Unit = {},
    onHorizonField: (Int) -> Unit = {},
    onCopyCoordinate: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }

    // fillMaxSize + weight scroll + pinned footer: Save stays above the system nav bar and IME
    // instead of living under an open map that steals vertical scroll.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        LumintraScaffoldHeader(
            title = stringResource(
                if (state.editingExisting) R.string.spot_edit_title_edit else R.string.spot_edit_title_new,
            ),
            onBack = { if (state.dirty) showDiscard = true else onCancel() },
            actions = {
                if (onDelete != null) {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui_trash),
                            contentDescription = stringResource(R.string.spot_edit_delete),
                        )
                    }
                }
            },
        )

        if (state.loading) {
            Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                softWrap = true,
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NameSection(state = state, onName = onName)
                if (!state.editingExisting && !state.coordinateSet) {
                    Text(
                        text = stringResource(R.string.spot_edit_create_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        softWrap = true,
                    )
                }
                val setPoint = state.point
                if (state.coordinateSet && setPoint != null) {
                    Text(
                        text = stringResource(
                            R.string.spot_edit_coordinate_set,
                            CoordinateText.formatDecimalPair(setPoint.latDeg, setPoint.lonDeg),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        softWrap = true,
                    )
                }
                PickerSection(state = state, onPickOnMap = onPickOnMap, onLatLon = onLatLon)
                AdvancedOptionsSection(
                    expanded = advancedOpen,
                    onToggle = { advancedOpen = !advancedOpen },
                    state = state,
                    wide = wide,
                    onElevation = onElevation,
                    onLatLon = onLatLon,
                    onPaste = onPaste,
                    onEntryFormat = onEntryFormat,
                    onLookupQuery = onLookupQuery,
                    onLookupSearch = onLookupSearch,
                    onReverseLookup = onReverseLookup,
                    onLookupClear = onLookupClear,
                    onCopyCoordinate = onCopyCoordinate,
                    onTz = onTz,
                    onZoneQuery = onZoneQuery,
                    onUseDeviceZone = onUseDeviceZone,
                    onHorizonSample = onHorizonSample,
                    onHorizonReset = onHorizonReset,
                    onHorizonField = onHorizonField,
                )
            }

            // One primary in the sticky footer. A second full-width Cancel here was measured
            // clipped into the gesture inset on A40 (Cancel bounds ended at content bottom
            // 2110 with only ~82px height). Back already cancels; keep Save alone and clear
            // of system bars + a gesture safety margin.
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // navigationBarsPadding alone measured as ~0 inset on A40 gesture nav while
                    // content still ends at y=2110; Save sat in the gesture strip (2046–2106).
                    // Keep the inset *and* a fixed safety margin so the tap target clears it.
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ValidationLine(state = state)
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.spot_edit_save),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        softWrap = true,
                    )
                }
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.action_cancel)) },
            text = { Text(stringResource(R.string.spot_edit_discard_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscard = false
                        onCancel()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscard = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
        )
    }

    if (showDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.spot_edit_delete)) },
            text = { Text(stringResource(R.string.spot_edit_delete_confirm, state.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDelete = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Says exactly what is missing, rather than dimming the Save button with no explanation. */
@Composable
private fun ValidationLine(state: SpotEditUiState) {
    val message = when (state.validation) {
        SpotValidation.NONE -> null
        SpotValidation.NAME_MISSING -> stringResource(R.string.spot_edit_needs_name)
        SpotValidation.COORDINATE_MISSING -> stringResource(R.string.spot_edit_needs_coordinate)
        SpotValidation.ELEVATION_INVALID -> stringResource(R.string.spot_edit_elevation_invalid)
    }
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
            softWrap = true,
        )
    }
    if (state.pasteError != null) {
        Text(
            text = when (state.pasteError) {
                PairFailure.LATITUDE_OUT_OF_RANGE,
                PairFailure.LONGITUDE_OUT_OF_RANGE,
                -> stringResource(R.string.spot_edit_paste_out_of_range)

                else -> stringResource(R.string.spot_edit_paste_invalid)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
            softWrap = true,
        )
    } else if (state.pasteApplied) {
        Text(
            text = stringResource(R.string.spot_edit_paste_applied),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            softWrap = true,
        )
    }
}

// --- sections ------------------------------------------------------------------------------------

@Composable
private fun NameSection(
    state: SpotEditUiState,
    onName: (String) -> Unit,
) {
    SectionStrip(
        title = stringResource(R.string.spot_edit_name),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.name,
        onValueChange = onName,
        label = {
            Text(
                text = stringResource(R.string.spot_edit_name),
                modifier = Modifier.fillMaxWidth(),
                softWrap = true,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(min = 160.dp),
    )
}

@Composable
private fun AdvancedOptionsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    state: SpotEditUiState,
    wide: Boolean,
    onElevation: (String) -> Unit,
    onLatLon: (String, String) -> Unit,
    onPaste: (String) -> Unit,
    onEntryFormat: (CoordinateEntryFormat) -> Unit,
    onLookupQuery: (String) -> Unit,
    onLookupSearch: () -> Unit,
    onReverseLookup: () -> Unit,
    onLookupClear: () -> Unit,
    onCopyCoordinate: () -> Unit,
    onTz: (String) -> Unit,
    onZoneQuery: (String) -> Unit,
    onUseDeviceZone: () -> Unit,
    onHorizonSample: (Int, Double) -> Unit,
    onHorizonReset: () -> Unit,
    onHorizonField: (Int) -> Unit,
) {
    SectionStrip(
        title = stringResource(R.string.spot_edit_advanced_title),
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(
            text = stringResource(
                if (expanded) R.string.spot_edit_advanced_collapse else R.string.spot_edit_advanced_expand,
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            softWrap = true,
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.elevationText,
                onValueChange = onElevation,
                label = {
                    Text(
                        text = stringResource(R.string.spot_edit_elevation),
                        modifier = Modifier.fillMaxWidth(),
                        softWrap = true,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingText = {
                    Text(
                        text = stringResource(R.string.spot_edit_elevation_hint),
                        modifier = Modifier.fillMaxWidth(),
                        softWrap = true,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            CoordinateSection(
                state = state,
                wide = wide,
                onLatLon = onLatLon,
                onPaste = onPaste,
                onEntryFormat = onEntryFormat,
                onLookupQuery = onLookupQuery,
                onLookupSearch = onLookupSearch,
                onReverseLookup = onReverseLookup,
                onLookupClear = onLookupClear,
                onCopyCoordinate = onCopyCoordinate,
            )
            ZoneSection(
                state = state,
                onTz = onTz,
                onZoneQuery = onZoneQuery,
                onUseDeviceZone = onUseDeviceZone,
            )
            HorizonSection(
                state = state,
                onHorizonSample = onHorizonSample,
                onHorizonReset = onHorizonReset,
                onHorizonField = onHorizonField,
            )
            EquipmentSection(equipment = state.equipment, precision = state.approximate)
        }
    }
}

@Composable
private fun CoordinateSection(
    state: SpotEditUiState,
    wide: Boolean,
    onLatLon: (String, String) -> Unit,
    onPaste: (String) -> Unit,
    onEntryFormat: (CoordinateEntryFormat) -> Unit,
    onLookupQuery: (String) -> Unit,
    onLookupSearch: () -> Unit,
    onReverseLookup: () -> Unit,
    onLookupClear: () -> Unit,
    onCopyCoordinate: () -> Unit,
) {
    val description = stringResource(R.string.spot_edit_picker_content_description)
    SectionStrip(
        title = stringResource(R.string.spot_edit_coordinates_header),
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectionChip(
            label = stringResource(R.string.spot_edit_use_decimal),
            selected = state.entryFormat == CoordinateEntryFormat.DECIMAL,
            onSelect = { onEntryFormat(CoordinateEntryFormat.DECIMAL) },
            modifier = Modifier.weight(1f),
        )
        SelectionChip(
            label = stringResource(R.string.spot_edit_use_dms),
            selected = state.entryFormat == CoordinateEntryFormat.DMS,
            onSelect = { onEntryFormat(CoordinateEntryFormat.DMS) },
            modifier = Modifier.weight(1f),
        )
    }

    when (state.entryFormat) {
        CoordinateEntryFormat.DECIMAL -> {
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DecimalField(
                        value = state.latitudeText,
                        label = stringResource(R.string.spot_edit_latitude),
                        hint = stringResource(R.string.spot_edit_latitude_hint),
                        onValueChange = { onLatLon(it, state.longitudeText) },
                        modifier = Modifier.weight(1f),
                    )
                    DecimalField(
                        value = state.longitudeText,
                        label = stringResource(R.string.spot_edit_longitude),
                        hint = stringResource(R.string.spot_edit_longitude_hint),
                        onValueChange = { onLatLon(state.latitudeText, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                DecimalField(
                    value = state.latitudeText,
                    label = stringResource(R.string.spot_edit_latitude),
                    hint = stringResource(R.string.spot_edit_latitude_hint),
                    onValueChange = { onLatLon(it, state.longitudeText) },
                    modifier = Modifier.fillMaxWidth(),
                )
                DecimalField(
                    value = state.longitudeText,
                    label = stringResource(R.string.spot_edit_longitude),
                    hint = stringResource(R.string.spot_edit_longitude_hint),
                    onValueChange = { onLatLon(state.latitudeText, it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IssueLine(issue = state.latitudeIssue)
            IssueLine(issue = state.longitudeIssue)
        }

        CoordinateEntryFormat.DMS -> {
            // Degrees, minutes and seconds with an explicit hemisphere, because a dropped minus sign
            // is the classic entry error and a hemisphere control cannot be dropped.
            DmsAxisEditor(
                axisLabel = stringResource(R.string.spot_edit_latitude),
                parts = state.dmsLatitude,
                negative = (state.latitudeDeg ?: 0.0) < 0.0,
                north = stringResource(R.string.coord_hemisphere_n),
                south = stringResource(R.string.coord_hemisphere_s),
                onCompose = { degrees, minutes, seconds, isNegative ->
                    composeAxis(
                        degrees = degrees,
                        minutes = minutes,
                        seconds = seconds,
                        negative = isNegative,
                        maxMagnitude = 90.0,
                        onValue = { onLatLon(it, state.longitudeText) },
                    )
                },
            )
            DmsAxisEditor(
                axisLabel = stringResource(R.string.spot_edit_longitude),
                parts = state.dmsLongitude,
                negative = (state.longitudeDeg ?: 0.0) < 0.0,
                north = stringResource(R.string.coord_hemisphere_e),
                south = stringResource(R.string.coord_hemisphere_w),
                onCompose = { degrees, minutes, seconds, isNegative ->
                    composeAxis(
                        degrees = degrees,
                        minutes = minutes,
                        seconds = seconds,
                        negative = isNegative,
                        maxMagnitude = 180.0,
                        onValue = { onLatLon(state.latitudeText, it) },
                    )
                },
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.latitudeText + ", " + state.longitudeText,
            onValueChange = onPaste,
            label = { Text(stringResource(R.string.spot_edit_paste)) },
            supportingText = { Text(stringResource(R.string.spot_edit_paste_hint)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onCopyCoordinate,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = description },
        ) {
            Text(stringResource(R.string.coord_copy))
        }
    }

    // The place-name lookup: keyless, permissionless, and always only a hint.
    SectionStrip(title = stringResource(R.string.spot_edit_lookup_title))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.lookupQuery,
            onValueChange = onLookupQuery,
            label = { Text(stringResource(R.string.spot_edit_lookup_hint)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onLookupSearch,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.spot_edit_lookup_action))
        }
    }
    TextButton(
        onClick = onReverseLookup,
        enabled = state.coordinateSet,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.spot_edit_lookup_reverse_action))
    }

    when (val lookup = state.lookup) {
        LookupState.Idle -> Unit
        LookupState.Searching -> Text(
            text = stringResource(R.string.state_loading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LookupState.Unavailable -> Text(
            text = stringResource(R.string.spot_edit_lookup_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = LumintraTheme.colors.caution,
        )

        LookupState.Empty -> Text(
            text = stringResource(R.string.spot_edit_lookup_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LookupState.ReverseEmpty -> Text(
            text = stringResource(R.string.spot_edit_lookup_reverse_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is LookupState.Results -> {
            Text(
                text = stringResource(R.string.spot_edit_lookup_approximate_only),
                style = MaterialTheme.typography.bodySmall,
                color = LumintraTheme.colors.caution,
                modifier = Modifier.fillMaxWidth(),
                softWrap = true,
            )
            for (hit in lookup.hits) {
                TextButton(
                    onClick = {
                        onLatLon(
                            CoordinateText.formatDecimal(hit.latitudeDeg),
                            CoordinateText.formatDecimal(hit.longitudeDeg),
                        )
                        onLookupClear()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.spot_edit_lookup_result,
                            hit.label,
                            CoordinateText.formatDecimal(hit.latitudeDeg),
                            CoordinateText.formatDecimal(hit.longitudeDeg),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DecimalField(
    value: String,
    label: String,
    hint: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                softWrap = true,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingText = {
            Text(
                text = hint,
                modifier = Modifier.fillMaxWidth(),
                softWrap = true,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.widthIn(min = 120.dp),
    )
}

@Composable
private fun IssueLine(issue: CoordinateIssue?) {
    if (issue == null) return
    Text(
        text = when (issue) {
            CoordinateIssue.UNREADABLE -> stringResource(R.string.spot_edit_paste_invalid)
            CoordinateIssue.OUT_OF_RANGE -> stringResource(R.string.spot_edit_paste_out_of_range)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

/**
 * One axis in degrees/minutes/seconds.
 *
 * The three fields are local to the axis and re-seeded whenever the stored coordinate changes from
 * somewhere else (a pin move, a paste, the other axis's correction), so the fields and the stored
 * value cannot drift apart. Each keystroke is composed through [CoordinateText.composeDms], which is
 * the same codec the paste path uses, and an invalid triple simply does not reach the store.
 */
@Composable
private fun DmsAxisEditor(
    axisLabel: String,
    parts: DmsParts,
    negative: Boolean,
    north: String,
    south: String,
    onCompose: (String, String, String, Boolean) -> Unit,
) {
    var degreesText by rememberSaveable(parts.degrees) { mutableStateOf(parts.degrees.toString()) }
    var minutesText by rememberSaveable(parts.minutes) { mutableStateOf(parts.minutes.toString()) }
    var secondsText by rememberSaveable(parts.seconds) {
        mutableStateOf(CoordinateText.formatDecimal(parts.seconds))
    }
    var isNegative by rememberSaveable(negative) { mutableStateOf(negative) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = axisLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            softWrap = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = degreesText,
                onValueChange = {
                    degreesText = it
                    onCompose(degreesText, minutesText, secondsText, isNegative)
                },
                label = {
                    Text(
                        text = stringResource(R.string.coord_degrees),
                        softWrap = true,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 72.dp),
            )
            OutlinedTextField(
                value = minutesText,
                onValueChange = {
                    minutesText = it
                    onCompose(degreesText, minutesText, secondsText, isNegative)
                },
                label = {
                    Text(
                        text = stringResource(R.string.coord_minutes),
                        softWrap = true,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 72.dp),
            )
            OutlinedTextField(
                value = secondsText,
                onValueChange = {
                    secondsText = it
                    onCompose(degreesText, minutesText, secondsText, isNegative)
                },
                label = {
                    Text(
                        text = stringResource(R.string.coord_seconds),
                        softWrap = true,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 72.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.coord_hemisphere_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 48.dp),
                softWrap = true,
            )
            SelectionChip(
                label = north,
                selected = !isNegative,
                onSelect = {
                    isNegative = false
                    onCompose(degreesText, minutesText, secondsText, false)
                },
            )
            SelectionChip(
                label = south,
                selected = isNegative,
                onSelect = {
                    isNegative = true
                    onCompose(degreesText, minutesText, secondsText, true)
                },
            )
        }
    }
}

/** Composes one axis and hands the canonical text onward, or does nothing when it is not readable. */
private fun composeAxis(
    degrees: String,
    minutes: String,
    seconds: String,
    negative: Boolean,
    maxMagnitude: Double,
    onValue: (String) -> Unit,
) {
    val composed = CoordinateText.composeDms(
        degreesText = degrees,
        minutesText = minutes,
        secondsText = seconds,
        negative = negative,
        maxMagnitude = maxMagnitude,
    ) ?: return
    onValue(composed)
}

@Composable
private fun PickerSection(
    state: SpotEditUiState,
    onPickOnMap: () -> Unit,
    onLatLon: (String, String) -> Unit,
) {
    SectionStrip(
        title = stringResource(R.string.spot_edit_map_section),
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(
        onClick = onPickOnMap,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(
            text = stringResource(
                if (state.pickerOpen) R.string.spot_edit_picker_hide else R.string.spot_edit_picker,
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            softWrap = true,
        )
    }
    if (state.pickerOpen) {
        // The picker writes through the same `onLatLon` channel as every other route, so there is
        // one place where a coordinate becomes the draft and one place where its range is checked.
        // Save stays in the pinned footer, so opening the map never covers the commit action.
        CoordinatePicker(
            point = state.point,
            onMove = {
                onLatLon(
                    CoordinateText.formatDecimal(it.latDeg),
                    CoordinateText.formatDecimal(it.lonDeg),
                )
            },
            centreHint = state.pickerCentre,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ZoneSection(
    state: SpotEditUiState,
    onTz: (String) -> Unit,
    onZoneQuery: (String) -> Unit,
    onUseDeviceZone: () -> Unit,
) {
    SectionStrip(title = stringResource(R.string.spot_edit_timezone))
    ValueRow(label = stringResource(R.string.spot_edit_timezone), value = state.tzId)
    when (state.zoneSource) {
        ZoneSource.RESOLVED_FROM_COORDINATES -> Text(
            text = stringResource(R.string.spot_edit_zone_from_coordinates),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ZoneSource.DEVICE_FALLBACK -> Text(
            text = stringResource(R.string.spot_edit_zone_device_fallback),
            style = MaterialTheme.typography.bodySmall,
            color = LumintraTheme.colors.caution,
        )

        ZoneSource.DEVICE_DEFAULT -> Text(
            text = stringResource(R.string.spot_edit_zone_device_default),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ZoneSource.USER_CHOSEN -> Unit
    }

    OutlinedTextField(
        value = state.zoneQuery,
        onValueChange = onZoneQuery,
        label = { Text(stringResource(R.string.spot_edit_zone_search)) },
        supportingText = { Text(stringResource(R.string.spot_edit_zone_search_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(
        onClick = onUseDeviceZone,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.spot_edit_zone_use_device))
    }

    val query = state.zoneQuery.trim()
    if (query.isEmpty()) return
    val matches = state.zoneIds.filter { it.contains(query, ignoreCase = true) }
    if (matches.isEmpty()) {
        Text(
            text = stringResource(R.string.spot_edit_zone_no_match, query),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    // Capped so a one-letter query cannot build a list of six hundred rows: the user narrows the
    // query instead, which is what the field is for.
    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
        items(items = matches.take(ZONE_MATCH_LIMIT), key = { it }) { zoneId ->
            TextButton(
                onClick = { onTz(zoneId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(zoneId)
            }
        }
    }
}

private const val ZONE_MATCH_LIMIT = 60

@Composable
private fun HorizonSection(
    state: SpotEditUiState,
    onHorizonSample: (Int, Double) -> Unit,
    onHorizonReset: () -> Unit,
    onHorizonField: (Int) -> Unit,
) {
    SectionStrip(title = stringResource(R.string.spot_edit_horizon_field_label))
    if (state.horizonFlat) {
        Text(
            text = stringResource(R.string.spot_edit_horizon_flat_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // The editor itself, in the same screen as the coordinate it belongs to. It never navigates.
    HorizonEditor(
        profile = state.horizon,
        fieldDeg = state.horizonFieldDeg,
        onSample = onHorizonSample,
        onReset = onHorizonReset,
        onFieldChange = onHorizonField,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EquipmentSection(equipment: EquipmentProfile, precision: Boolean) {
    SectionStrip(title = stringResource(R.string.spot_edit_lens_header))
    ValueRow(
        label = stringResource(R.string.spot_edit_lens_equipment_profile),
        value = equipment.presetName ?: equipment.name,
        secondary = stringResource(
            R.string.spot_edit_lens_numbers,
            CoordinateText.formatDecimal(equipment.focalLengthMm),
            CoordinateText.formatDecimal(equipment.sensorWidthMm),
            CoordinateText.formatDecimal(equipment.sensorHeightMm),
        ),
    )
    Text(
        text = stringResource(R.string.spot_edit_lens_read_only),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(
            if (precision) R.string.spot_edit_precision_approximate else R.string.spot_edit_precision_typed,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = if (precision) LumintraTheme.colors.caution else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A selectable chip that carries its selection in a border and a weight as well as in colour, so the
 * state survives a monochrome palette and a colour-blind reader.
 *
 * `FieldChip` in `HorizonEditor.kt` is private to that file, so this is its sibling rather than a
 * second public component: the two are deliberately not shared, because the horizon's field chooser
 * and a hemisphere control are different decisions that happen to look alike.
 */
@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = MaterialTheme.shapes.small,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = colors.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            softWrap = true,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small typed fact about the coordinate that the section header cannot carry. */
@Composable
private fun CoordinatePrecisionChip(approximate: Boolean) {
    ConditionChip(
        text = stringResource(
            if (approximate) R.string.spot_edit_approximate_label else R.string.spot_detail_typed,
        ),
        state = if (approximate) ChipState.MARGINAL else ChipState.GOOD,
    )
}

@Preview(name = "Spot editor — new", showBackground = true, widthDp = 412, heightDp = 1400)
@Composable
private fun SpotEditNewPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        SpotEditScreen(
            state = SpotEditUiState(
                loading = false,
                draftId = "spot-preview",
                name = "Fjellstua",
                latitudeText = "59.9139",
                longitudeText = "10.7522",
                latitudeDeg = 59.9139,
                longitudeDeg = 10.7522,
                coordinateSet = true,
                tzId = "Europe/Oslo",
                zoneSource = ZoneSource.RESOLVED_FROM_COORDINATES,
                pickerCentre = GeodeticPoint(59.9139, 10.7522),
                horizonFieldDeg = 30,
            ),
            onName = {},
            onLatLon = { _, _ -> },
            onElevation = {},
            onTz = {},
            onPickOnMap = {},
            onHorizonSample = { _, _ -> },
            onHorizonReset = {},
            onSave = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Spot editor — saved spot", showBackground = true, widthDp = 412, heightDp = 1400)
@Composable
private fun SpotEditExistingPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        SpotEditScreen(
            state = SpotEditUiState(
                loading = false,
                editingExisting = true,
                draftId = "spot-preview",
                name = "Ridge camp",
                latitudeText = "-33.9",
                longitudeText = "151.2",
                latitudeDeg = -33.9,
                longitudeDeg = 151.2,
                coordinateSet = true,
                approximate = true,
                entryFormat = CoordinateEntryFormat.DMS,
                tzId = "Australia/Sydney",
                zoneSource = ZoneSource.USER_CHOSEN,
                horizon = HorizonProfile.flat(
                    id = "spot-preview.horizon",
                    name = "Ridge camp",
                    latitudeDeg = -33.9,
                    longitudeDeg = 151.2,
                    elevationDeg = 6.0,
                ),
            ),
            onName = {},
            onLatLon = { _, _ -> },
            onElevation = {},
            onTz = {},
            onPickOnMap = {},
            onHorizonSample = { _, _ -> },
            onHorizonReset = {},
            onSave = {},
            onCancel = {},
        )
    }
}
