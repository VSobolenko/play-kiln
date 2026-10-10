package com.gexetr.lumintralightplanner.ui.spots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.ScoutSelection
import com.gexetr.lumintralightplanner.ui.components.ChipState
import com.gexetr.lumintralightplanner.ui.components.ConditionChip
import com.gexetr.lumintralightplanner.ui.components.LumintraEmptyState
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.components.ValueRow
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.PlayKilnTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// =================================================================================================
// Spots — the saved-spot list, the active spot, and the two-pane detail on a wide window
// =================================================================================================
//
// The list is the app's only navigation into a place, so it has to hold the facts that make a spot
// a spot rather than a pair of numbers: its name, its coordinate, its zone, whether a horizon has
// been drawn for it, and which lens profile it carries (specs/scout-spots).
//
// Two rules the screen keeps visible:
//
//   * **The empty state is scoped and single-action.** With no spot there is no list, no count and
//     no invented coordinate — one sentence saying what a spot is for and one button. The app never
//     shows a `(0, 0)` spot, because a sky computed for the Gulf of Guinea is a lie dressed as data.
//   * **The active spot is a real, persisted selection.** `activeSpotId` here is the value the store
//     resolves through `ScoutSelection.reconcile`, not "whichever row is first": a dangling id has
//     already fallen back to a real record before this state was built, so the pill on screen and
//     the spot the brief will use cannot disagree.

/** One row of the list: everything the row and the detail pane draw, already resolved. */
data class SpotRow(
    val id: String,
    val name: String,
    val point: GeodeticPoint,
    val tzId: String,
    val elevationM: Double,
    val approximate: Boolean,
    val horizonDrawn: Boolean,
    val equipmentName: String,
)

/**
 * Everything the spots screen renders.
 *
 * [loading] is a real state and not a placeholder: `stateIn` starts with it `true`, so the screen
 * can tell "the store has not answered yet" from "the store answered and holds nothing". Collapsing
 * the two is what produces the flash of an empty state over a store that was about to return two
 * spots.
 */
data class SpotsUiState(
    val loading: Boolean = true,
    val spots: List<SpotRow> = emptyList(),
    val activeSpotId: String? = null,
    val quarantinedCount: Int = 0,
    val missingTargetRefCount: Int = 0,
    val coordinateFormat: CoordinateEntryFormat = CoordinateEntryFormat.DECIMAL,
) {
    /** The record the detail pane shows, on the active spot's rule rather than on list order. */
    val activeSpot: SpotRow? get() = spots.firstOrNull { it.id == activeSpotId } ?: spots.firstOrNull()
}

@Composable
fun SpotsScreen(
    state: SpotsUiState,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onSetActive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LumintraScaffoldHeader(
            title = stringResource(R.string.spot_title),
            onBack = onBack,
            actions = {
                IconButton(onClick = onAdd) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_plus),
                        contentDescription = stringResource(R.string.spot_add),
                    )
                }
            },
        )

        when {
            state.loading -> Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )

            state.spots.isEmpty() -> LumintraEmptyState(
                title = stringResource(R.string.spot_empty_title),
                body = stringResource(R.string.spot_empty_body),
                iconRes = R.drawable.ic_ui_map_pin,
                actionLabel = stringResource(R.string.spot_add),
                onAction = onAdd,
                modifier = Modifier.padding(20.dp),
            )

            wide -> {
                // A tablet or a landscape phone: the list and the detail side by side, so selecting a
                // row does not navigate away from the list the user is working in.
                Row(modifier = Modifier.fillMaxSize()) {
                    SpotList(
                        state = state,
                        selectedId = selectedId ?: state.activeSpotId,
                        onSelect = { selectedId = it },
                        onOpen = onOpen,
                        onSetActive = onSetActive,
                        onRequestDelete = { pendingDeleteId = it },
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight(),
                    )
                    val detail = state.spots.firstOrNull { it.id == (selectedId ?: state.activeSpotId) }
                        ?: state.activeSpot
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (detail == null) {
                            SpotDetailPrompt()
                        } else {
                            SpotDetailPane(
                                row = detail,
                                isActive = detail.id == state.activeSpotId,
                                format = state.coordinateFormat,
                                onEdit = { onOpen(detail.id) },
                                onSetActive = { onSetActive(detail.id) },
                                onDelete = { pendingDeleteId = detail.id },
                            )
                        }
                    }
                }
            }

            else -> SpotList(
                state = state,
                selectedId = state.activeSpotId,
                onSelect = onOpen,
                onOpen = onOpen,
                onSetActive = onSetActive,
                onRequestDelete = { pendingDeleteId = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    val pendingRow = state.spots.firstOrNull { it.id == pendingDeleteId }
    if (pendingRow != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.spot_edit_delete)) },
            // The confirmation names the spot AND says what goes with it, because the horizon
            // profile and the equipment profile are not visible from this screen.
            text = { Text(stringResource(R.string.spot_edit_delete_confirm, pendingRow.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = pendingRow.id
                        pendingDeleteId = null
                        if (selectedId == id) selectedId = null
                        onDelete(id)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteId = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The list itself: a header strip, the store's degraded notes, and one row per saved spot. */
@Composable
private fun SpotList(
    state: SpotsUiState,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onOpen: (String) -> Unit,
    onSetActive: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.spot_list_header))

        if (state.quarantinedCount > 0) {
            Text(
                text = stringResource(R.string.spot_store_quarantined),
                style = MaterialTheme.typography.bodySmall,
                color = LumintraTheme.colors.caution,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (state.missingTargetRefCount > 0) {
            Text(
                text = stringResource(R.string.target_reference_missing),
                style = MaterialTheme.typography.bodySmall,
                color = LumintraTheme.colors.caution,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = state.spots, key = { it.id }) { row ->
                SpotRowItem(
                    row = row,
                    isActive = row.id == state.activeSpotId,
                    selected = row.id == selectedId,
                    format = state.coordinateFormat,
                    onSelect = { onSelect(row.id) },
                    onEdit = { onOpen(row.id) },
                    onSetActive = { onSetActive(row.id) },
                    onDelete = { onRequestDelete(row.id) },
                )
            }
        }
    }
}

/**
 * One saved spot.
 *
 * The row body opens the spot (or selects it on a wide window); the three trailing controls are
 * separate named targets rather than one long-press menu, so each has a 48 dp target a screen reader
 * can announce and a sighted user can hit.
 */
@Composable
private fun SpotRowItem(
    row: SpotRow,
    isActive: Boolean,
    selected: Boolean,
    format: CoordinateEntryFormat,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
) {
    val openDescription = stringResource(R.string.spot_row_open_description, row.name)
    LumintraPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onSelect)
            .semantics { contentDescription = openDescription },
        accent = selected || isActive,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.spot_row_subtitle,
                        coordinateText(row.point.latDeg, row.point.lonDeg, LATITUDE, format),
                        coordinateText(row.point.latDeg, row.point.lonDeg, LONGITUDE, format),
                        row.tzId,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (row.horizonDrawn) {
                            R.string.spot_row_horizon_drawn
                        } else {
                            R.string.spot_row_horizon_flat
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isActive) {
                ConditionChip(
                    text = stringResource(R.string.spot_active_label),
                    state = ChipState.GOOD,
                )
            }
            if (row.approximate) {
                ConditionChip(
                    text = stringResource(R.string.spot_edit_approximate_label),
                    state = ChipState.MARGINAL,
                )
            }

            IconButton(onClick = onSetActive) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_locate_fixed),
                    contentDescription = stringResource(R.string.spot_row_set_active_description, row.name),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_pencil),
                    contentDescription = stringResource(R.string.spot_row_edit_description, row.name),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_trash),
                    contentDescription = stringResource(R.string.spot_row_delete_description, row.name),
                )
            }
        }
    }
}

/** The wide window's right pane before a row has been chosen. */
@Composable
private fun SpotDetailPrompt() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.spot_detail_prompt_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.spot_detail_prompt_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The wide window's right pane: the selected spot's facts, and the same three actions. */
@Composable
private fun SpotDetailPane(
    row: SpotRow,
    isActive: Boolean,
    format: CoordinateEntryFormat,
    onEdit: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = row.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (isActive) {
            ConditionChip(
                text = stringResource(R.string.spot_active_label),
                state = ChipState.GOOD,
            )
        }

        SectionStrip(title = stringResource(R.string.spot_edit_coordinates_header))
        ValueRow(
            label = stringResource(R.string.spot_edit_latitude),
            value = coordinateText(row.point.latDeg, row.point.lonDeg, LATITUDE, format),
        )
        ValueRow(
            label = stringResource(R.string.spot_edit_longitude),
            value = coordinateText(row.point.latDeg, row.point.lonDeg, LONGITUDE, format),
        )
        ValueRow(
            label = stringResource(R.string.spot_detail_elevation),
            value = stringResource(
                R.string.spot_elevation_metres,
                CoordinateText.formatDecimal(row.elevationM),
            ),
        )
        ValueRow(
            label = stringResource(R.string.spot_detail_zone),
            value = row.tzId,
        )
        ValueRow(
            label = stringResource(R.string.spot_edit_horizon_field_label),
            value = stringResource(
                if (row.horizonDrawn) {
                    R.string.spot_row_horizon_drawn
                } else {
                    R.string.spot_row_horizon_flat
                },
            ),
        )
        ValueRow(
            label = stringResource(R.string.spot_edit_lens_equipment_profile),
            value = row.equipmentName,
        )
        ValueRow(
            label = stringResource(R.string.spot_detail_precision),
            value = stringResource(
                if (row.approximate) {
                    R.string.spot_edit_approximate_label
                } else {
                    R.string.spot_detail_typed
                },
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onSetActive,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.spot_set_active_action))
            }
            TextButton(
                onClick = onEdit,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_edit))
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Coordinate readout
// -------------------------------------------------------------------------------------------------

/** Which half of a coordinate is being rendered; the sign convention differs between them. */
private enum class Axis { LATITUDE, LONGITUDE }

private val LATITUDE = Axis.LATITUDE
private val LONGITUDE = Axis.LONGITUDE

/**
 * One half of a coordinate in the user's chosen notation.
 *
 * The hemisphere letters are localised, so they are read from resources here rather than baked into
 * [CoordinateText]; the number itself is `Locale.ROOT` by design, because `47,3769` is a different
 * number in a locale that uses the comma as the decimal mark.
 */
@Composable
private fun coordinateText(
    latDeg: Double,
    lonDeg: Double,
    axis: Axis,
    format: CoordinateEntryFormat,
): String {
    val north = stringResource(R.string.coord_hemisphere_n)
    val south = stringResource(R.string.coord_hemisphere_s)
    val east = stringResource(R.string.coord_hemisphere_e)
    val west = stringResource(R.string.coord_hemisphere_w)
    val value = if (axis == LATITUDE) latDeg else lonDeg
    return when (format) {
        CoordinateEntryFormat.DECIMAL -> CoordinateText.formatDecimal(value)
        CoordinateEntryFormat.DMS -> {
            val letter = when {
                axis == LATITUDE && value < 0.0 -> south
                axis == LATITUDE -> north
                value < 0.0 -> west
                else -> east
            }
            CoordinateText.formatDms(value, letter)
        }
    }
}

/**
 * The list's state holder.
 *
 * The store is the single source of truth for the records and the selection; this ViewModel adds no
 * mirror of either, only the rows the screen draws and the two degraded counts the store reports.
 * Writes go straight back to the store, so a spot deleted here is deleted on disk before the list
 * re-emits — there is no local optimistic copy that could survive a failure.
 */
class SpotsViewModel(
    private val store: ScoutStore,
    coordinateFormat: CoordinateEntryFormat = CoordinateEntryFormat.DECIMAL,
) : ViewModel() {

    /** Only the notation, which is a display choice and never a stored coordinate. */
    private val notation = MutableStateFlow(coordinateFormat)

    val state: StateFlow<SpotsUiState> = combine(store.snapshot, notation) { snapshot, format ->
        // `reconcile` is the domain's own rule: a dangling or absent id resolves to a real record,
        // and an empty store resolves to null. It is imported rather than restated so the pill this
        // screen draws and the spot the brief computes from are the same decision.
        val activeId = ScoutSelection.reconcile(snapshot.spots, snapshot.activeSpotId)
        SpotsUiState(
            loading = false,
            spots = snapshot.spots.map { spot ->
                SpotRow(
                    id = spot.id,
                    name = spot.name,
                    point = spot.point,
                    tzId = spot.tzId,
                    elevationM = spot.elevationM,
                    approximate = spot.approximate,
                    horizonDrawn = spot.horizonProfileId != null &&
                        snapshot.horizons[spot.horizonProfileId] != null,
                    equipmentName = spot.equipment.presetName ?: spot.equipment.name,
                )
            },
            activeSpotId = activeId,
            quarantinedCount = snapshot.quarantinedCount,
            missingTargetRefCount = snapshot.missingTargetRefCount,
            coordinateFormat = format,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SpotsUiState(loading = true, coordinateFormat = coordinateFormat),
    )

    fun setActive(id: String) {
        viewModelScope.launch { store.setActiveSpot(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { store.deleteSpot(id) }
    }

    fun setCoordinateFormat(format: CoordinateEntryFormat) {
        notation.value = format
    }

    companion object {
        fun factory(
            store: ScoutStore,
            coordinateFormat: CoordinateEntryFormat = CoordinateEntryFormat.DECIMAL,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SpotsViewModel(store, coordinateFormat) as T
        }
    }
}

@Preview(name = "Spots — empty", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun SpotsEmptyPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        SpotsScreen(
            state = SpotsUiState(loading = false),
            onAdd = {},
            onOpen = {},
            onSetActive = {},
            onDelete = {},
            onBack = {},
        )
    }
}

@Preview(name = "Spots — two panes", showBackground = true, widthDp = 800, heightDp = 600)
@Composable
private fun SpotsWidePreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        SpotsScreen(
            state = SpotsUiState(
                loading = false,
                spots = listOf(
                    SpotRow(
                        id = "a",
                        name = "Fjellstua",
                        point = GeodeticPoint(59.9139, 10.7522, 42.0),
                        tzId = "Europe/Oslo",
                        elevationM = 42.0,
                        approximate = false,
                        horizonDrawn = true,
                        equipmentName = "24 mm full frame",
                    ),
                    SpotRow(
                        id = "b",
                        name = "Ridge camp",
                        point = GeodeticPoint(-33.9, 151.2),
                        tzId = "Australia/Sydney",
                        elevationM = 0.0,
                        approximate = true,
                        horizonDrawn = false,
                        equipmentName = "50 mm full frame",
                    ),
                ),
                activeSpotId = "a",
            ),
            onAdd = {},
            onOpen = {},
            onSetActive = {},
            onDelete = {},
            onBack = {},
        )
    }
}
