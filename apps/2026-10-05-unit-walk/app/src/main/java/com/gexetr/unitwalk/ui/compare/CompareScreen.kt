package com.gexetr.unitwalk.ui.compare

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.unitwalk.R
import com.gexetr.unitwalk.domain.CompareOutcome
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.ui.components.HeroPanel
import com.gexetr.unitwalk.ui.components.PillKind
import com.gexetr.unitwalk.ui.components.StatusPill
import com.gexetr.unitwalk.ui.theme.PlayKilnTheme

/**
 * Move-in versus move-out for one walk — the surface behind the `move-in-vs-move-out` promise.
 *
 * The screen is split the way `ui/settings/SettingsScreen.kt` is split: [CompareScreen] / [CompareRoute]
 * build the ViewModel and collect its state, while `CompareContent` is a pure function of
 * [CompareUiState] that emits [CompareEvent]s. No business logic lives in a composable, and no
 * user-facing word is compiled in — every label is a `stringResource`.
 *
 * Two product rules are honoured where they are visible:
 *
 *  - **A missing prior report is stated, never drawn.** When the ViewModel reports
 *    [CompareOutcome.NoPriorReport], the screen shows `compare_no_prior_notice` and every row's
 *    move-in cell reads `compare_no_prior_short`; no baseline `GOOD` is ever fabricated.
 *  - **A condition is carried by a word and a shape, not by colour.** Each condition is a labelled
 *    [FilterChip] and a [StatusPill] with its own silhouette, so the state survives greyscale,
 *    colour blindness and a translated label.
 */
@Composable
fun CompareScreen(
    walkId: String,
    onBack: () -> Unit,
    onOpenReport: (walkId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CompareViewModel = viewModel(
        key = walkId,
        factory = CompareViewModel.factory(LocalContext.current, walkId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompareContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenReport = onOpenReport,
        modifier = modifier,
    )
}

/**
 * The route form, mirroring `SettingsRoute`: the shell may call either name, and both build the same
 * ViewModel over the walk the caller navigated with.
 */
@Composable
fun CompareRoute(
    walkId: String,
    onBack: () -> Unit,
    onOpenReport: (walkId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompareScreen(
        walkId = walkId,
        onBack = onBack,
        onOpenReport = onOpenReport,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompareContent(
    state: CompareUiState,
    onEvent: (CompareEvent) -> Unit,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        stringResource(
            when (message) {
                CompareMessage.DeltaSaved -> R.string.compare_delta_saved
                CompareMessage.SaveFailed -> R.string.compare_save_failed
                CompareMessage.WalkNotFound -> R.string.compare_walk_not_found
            },
        )
    }
    val heroTitle = if (state.unitName.isBlank()) {
        stringResource(R.string.compare_title)
    } else {
        state.unitName
    }

    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onEvent(CompareEvent.MessageDismissed)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.compare_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.isLoaded) {
                Text(
                    text = stringResource(R.string.compare_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            HeroPanel(
                title = heroTitle,
                subtitle = stringResource(kindResource(state.walkKind)),
                stat = state.rows.size.toString(),
                statLabel = stringResource(R.string.compare_rooms_label),
            )

            if (state.outcome == CompareOutcome.NoPriorReport) {
                StatusPill(
                    label = stringResource(R.string.compare_no_prior_short),
                    kind = PillKind.NEUTRAL,
                )
                Text(
                    text = stringResource(R.string.compare_no_prior_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.compare_condition_supporting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.rows.forEach { row ->
                CompareRowCard(
                    row = row,
                    hasPrior = state.outcome == CompareOutcome.HasPrior,
                    onRecord = { condition, note ->
                        onEvent(CompareEvent.RecordDelta(row.roomId, condition, note))
                    },
                )
            }

            Button(
                onClick = { onOpenReport(state.walkId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.compare_open_report))
            }
        }
    }
}

/** One room's line: what the prior report recorded, what this walk records, and how to change it. */
@Composable
private fun CompareRowCard(
    row: CompareRow,
    hasPrior: Boolean,
    onRecord: (RoomCondition, String) -> Unit,
) {
    var selectedCondition by rememberSaveable(row.roomId) {
        mutableStateOf(row.currentCondition ?: RoomCondition.GOOD)
    }
    var noteText by rememberSaveable(row.roomId) { mutableStateOf(row.note) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = row.roomName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ConditionLine(
                label = stringResource(R.string.compare_column_prior),
                condition = if (hasPrior) row.priorCondition else null,
                // With no prior report there is no baseline to read, and saying "not recorded"
                // would imply one exists and was skipped.
                missingText = stringResource(
                    if (hasPrior) R.string.compare_condition_not_recorded else R.string.compare_no_prior_short,
                ),
            )

            ConditionLine(
                label = stringResource(R.string.compare_column_current),
                condition = row.currentCondition,
                missingText = stringResource(R.string.compare_condition_not_recorded),
            )

            Text(
                text = stringResource(R.string.compare_record_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoomCondition.entries.forEach { condition ->
                    FilterChip(
                        selected = selectedCondition == condition,
                        onClick = { selectedCondition = condition },
                        label = { Text(stringResource(conditionResource(condition))) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.compare_note_label)) },
                placeholder = { Text(stringResource(R.string.compare_note_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onRecord(selectedCondition, noteText) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.compare_save_delta))
            }
        }
    }
}

/**
 * One labelled condition: its word and its silhouette. A `null` condition renders [missingText] —
 * "not recorded" for a walk that simply has no value yet, "no prior report" for a baseline that does
 * not exist — and is never rounded up to `GOOD`.
 */
@Composable
private fun ConditionLine(label: String, condition: RoomCondition?, missingText: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (condition == null) {
            Text(
                text = missingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            StatusPill(
                label = stringResource(conditionResource(condition)),
                kind = conditionKind(condition),
            )
        }
    }
}

private fun kindResource(kind: WalkKind): Int = when (kind) {
    WalkKind.MOVE_IN -> R.string.compare_kind_move_in
    WalkKind.MOVE_OUT -> R.string.compare_kind_move_out
    WalkKind.SINGLE -> R.string.compare_kind_single
}

private fun conditionResource(condition: RoomCondition): Int = when (condition) {
    RoomCondition.GOOD -> R.string.compare_condition_good
    RoomCondition.WORN -> R.string.compare_condition_worn
    RoomCondition.DAMAGED -> R.string.compare_condition_damaged
    RoomCondition.MISSING -> R.string.compare_condition_missing
}

private fun conditionKind(condition: RoomCondition): PillKind = when (condition) {
    RoomCondition.GOOD -> PillKind.OK
    RoomCondition.WORN -> PillKind.WARN
    RoomCondition.DAMAGED -> PillKind.DANGER
    RoomCondition.MISSING -> PillKind.NEUTRAL
}

@Preview(name = "Compare light", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun CompareContentLightPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        CompareContent(
            state = CompareUiState(
                isLoaded = true,
                unitName = "Unit A",
                walkId = "walk-1",
                walkKind = WalkKind.MOVE_OUT,
                outcome = CompareOutcome.HasPrior,
                priorWalkId = "walk-0",
                rows = listOf(
                    CompareRow(
                        roomId = "room-1",
                        roomName = "Kitchen",
                        priorCondition = RoomCondition.GOOD,
                        currentCondition = RoomCondition.WORN,
                        note = "One wall scratched",
                    ),
                    CompareRow(
                        roomId = "room-2",
                        roomName = "Hallway",
                        priorCondition = RoomCondition.GOOD,
                        currentCondition = null,
                        note = "",
                    ),
                ),
            ),
            onEvent = {},
            onBack = {},
            onOpenReport = {},
        )
    }
}

@Preview(
    name = "Compare dark, no prior",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CompareContentDarkPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        CompareContent(
            state = CompareUiState(
                isLoaded = true,
                unitName = "Unit B",
                walkId = "walk-2",
                walkKind = WalkKind.MOVE_OUT,
                outcome = CompareOutcome.NoPriorReport,
                rows = listOf(
                    CompareRow(
                        roomId = "room-9",
                        roomName = "Bathroom",
                        priorCondition = null,
                        currentCondition = RoomCondition.DAMAGED,
                        note = "Tiles cracked",
                    ),
                ),
            ),
            onEvent = {},
            onBack = {},
            onOpenReport = {},
        )
    }
}
