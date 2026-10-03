package com.gexetr.laneset.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.laneset.R
import com.gexetr.laneset.data.SessionPosition
import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.Gear
import com.gexetr.laneset.domain.PaceModel
import com.gexetr.laneset.domain.SectionKind
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SessionConstraints
import com.gexetr.laneset.domain.SessionSection
import com.gexetr.laneset.domain.SetBlock
import com.gexetr.laneset.domain.Stroke
import com.gexetr.laneset.ui.components.BrandMark
import com.gexetr.laneset.ui.components.FocusOption
import com.gexetr.laneset.ui.components.FocusSelectorStrip
import com.gexetr.laneset.ui.components.PaceHeroPanel
import com.gexetr.laneset.ui.components.SetVerdict
import com.gexetr.laneset.ui.components.StatTile
import com.gexetr.laneset.ui.components.StatusPill
import com.gexetr.laneset.ui.export.ExportSheet
import com.gexetr.laneset.ui.theme.PlayKilnTheme
import com.gexetr.laneset.ui.theme.laneSetColors
import com.gexetr.laneset.ui.theme.laneSetTypography
import kotlin.math.roundToInt

/**
 * The home route: state wiring, and the one place a request becomes a push.
 *
 * The screen never navigates by itself. Whether a focus is available, whether the trials are and
 * whether the handoff is are all one question — is the unlock owned — and it is answered once, in
 * [HomeViewModel]. This effect is only the place that answer turns into a destination, which is what
 * keeps "explain what the unlock gives" from being re-decided by a layout file.
 *
 * The request is cleared **before** the push, and the order is deliberate: a push can take this
 * composable out of composition (Home is the list pane, and a wide window keeps it, but nothing
 * promises that), and a request still set on re-entry would push the destination a second time. The
 * clearing write is synchronous and costs nothing when there is no request.
 */
@Composable
fun HomeRoute(
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaceTest: () -> Unit,
    onNavigateToCoachImport: () -> Unit,
    onNavigateToUnlock: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigationRequest) {
        val request = state.navigationRequest ?: return@LaunchedEffect
        viewModel.onEvent(HomeEvent.NavigationHandled)
        when (request) {
            HomeNavigation.PaceTest -> onNavigateToPaceTest()
            HomeNavigation.CoachImport -> onNavigateToCoachImport()
            HomeNavigation.Unlock -> onNavigateToUnlock()
        }
    }

    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        hapticsEnabled = hapticsEnabled,
        onAboutClick = onNavigateToAbout,
        onSettingsClick = onNavigateToSettings,
        modifier = modifier,
    )
}

/**
 * The front door and the core surface: constraints in, a computed set list out.
 *
 * It is one scrollable column because that is what the host can promise. On a wide window
 * `PlayKilnApp` keeps Home in the leading pane beside whichever flow is open, so the screen must not
 * assume a phone width and must not try to be a two-pane layout itself — it reads as one column at
 * any width, and the window decides what sits next to it.
 *
 * The order of the column is the order the product happens in: what the app is, then the session the
 * swimmer came back for (or the empty state before there is one), then the form that produces the
 * next one, then the ways the result leaves the phone. Nothing on the screen is derived inside a
 * composable that [HomeUiState] does not already know, and no control decides what a tap *means* —
 * each one sends a [HomeEvent] and the ViewModel answers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = false,
    onAboutClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val session = state.session

    // The one preference that changes what the app *does* rather than how it looks. `settings.behaviour`
    // fails a toggle nothing reads, and these three taps are where it is read: they are the app's own
    // actions, the ones a hand on a wet poolside phone wants confirmed.
    val actOnDeck: (HomeEvent) -> Unit = { event ->
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onEvent(event)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = onAboutClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(R.string.home_about_action),
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IdentityHeader()

            if (session == null) {
                EmptyPanel()
            } else {
                SessionPanel(state = state, session = session, onDeck = actOnDeck, onEvent = onEvent)
            }

            val notice = state.notice
            if (notice != null) {
                NoticeCard(notice = notice, onDismiss = { onEvent(HomeEvent.NoticeDismissed) })
            }

            ConstraintsForm(state = state, onEvent = onEvent)

            BuildActions(state = state, onDeck = actOnDeck)

            if (session != null) {
                HandoffSection(state = state, onEvent = onEvent)
            }
        }
    }

    // The sheet is the other worker's composable and is content, not chrome: the host owns the sheet
    // and its dismissal gesture, which is what keeps the two files from both deciding how it closes.
    // It is only ever reachable with a session, because the ViewModel does not open it without one.
    if (state.isExportSheetVisible && session != null) {
        ModalBottomSheet(onDismissRequest = { onEvent(HomeEvent.ExportDismissed) }) {
            ExportSheet(session = session, onDismiss = { onEvent(HomeEvent.ExportDismissed) })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// What the app is
// ---------------------------------------------------------------------------------------------

/** The mark and the one sentence that says what the app does, above everything else. */
@Composable
private fun IdentityHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandMark(modifier = Modifier.size(40.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Before anything is built: what the swimmer is about to get, and that it costs nothing to try. */
@Composable
private fun EmptyPanel() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The generated session
// ---------------------------------------------------------------------------------------------

/**
 * Everything the swimmer came for: the headline interval, the totals, where they are in the set and
 * the whole list.
 *
 * The hero carries the biggest block of the **main set** rather than the first block of the session,
 * because the warm-up is not why anyone opens the app and the main set is what the generator grew
 * from the constraints. "Biggest" is the largest total distance, and ties are broken by the list
 * order — deterministic, like the generator itself, so the panel does not move between two renders
 * of the same session.
 */
@Composable
private fun SessionPanel(
    state: HomeUiState,
    session: Session,
    onDeck: (HomeEvent) -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    val mainSetBlocks = session.mainSetBlocks
    val heroBlock = mainSetBlocks.maxByOrNull { it.totalDistanceMeters } ?: session.blocks.firstOrNull()
    val pace = state.activePacePer100Seconds

    Text(
        text = stringResource(R.string.home_session_header),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
    )

    if (heroBlock != null) {
        PaceHeroPanel(
            intervalHeadline = stringResource(
                R.string.home_block_headline,
                heroBlock.repetitions,
                heroBlock.distanceMeters,
            ),
            strokeLabel = strokeLabel(heroBlock.stroke),
            targetTime = formatClock(heroBlock.targetTimeSeconds),
            totalValue = formatClock(session.estimatedTotalSeconds),
            paceValue = formatClock(pace.roundToInt()),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (heroBlock != null) {
            // The verdict is about the *pace the targets were built from*, and that pace is named in
            // the sentence beside it — the free tier's default, or the swimmer's own trials. That is
            // the whole honesty of the pill: "holdable" means holdable at the pace the app used.
            StatusPill(verdict = verdictFor(heroBlock, pace))
        }
        Text(
            text = if (state.usesDefaultPace) {
                stringResource(R.string.home_pace_value_default, formatClock(pace.roundToInt()))
            } else {
                stringResource(R.string.home_pace_value_yours)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }

    OutlinedButton(
        onClick = { onEvent(HomeEvent.PaceTestRequested) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(
            stringResource(
                if (state.isUnlocked) R.string.home_trials_action else R.string.home_trials_locked_action,
            ),
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatTile(
            label = stringResource(R.string.home_stat_distance),
            value = stringResource(R.string.home_meters_value, session.totalDistanceMeters),
            icon = R.drawable.ic_pool,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.home_stat_time),
            value = formatClock(session.estimatedTotalSeconds),
            icon = R.drawable.ic_timer,
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatTile(
            label = stringResource(R.string.home_stat_intervals),
            value = session.blocks.size.toString(),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.home_stat_pace),
            value = formatClock(pace.roundToInt()),
            accent = laneSetColors.pace,
            modifier = Modifier.weight(1f),
        )
    }

    RunnerCard(state = state, session = session, onDeck = onDeck)

    Text(
        text = stringResource(R.string.home_setlist_header),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        text = stringResource(R.string.home_setlist_supporting),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SetList(session = session)

    TextButton(
        onClick = { onEvent(HomeEvent.SessionDiscarded) },
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.home_discard_action))
    }
}

/**
 * Where the swimmer is, and the two ways to move.
 *
 * The position is indices, not a stopwatch (`data/SessionStore.kt:22-27`), so this panel shows a
 * place in the set rather than a clock: the rep within its block, the block within the session, and
 * how many reps are left. Progress is stated in words rather than drawn as a bar, because a bar has
 * to be told what fraction it is and the words already say it.
 *
 * Both buttons are single focus targets with a word on them and no declared extent, so they are what
 * Material 3 draws — at least 48 dp tall — and TalkBack reads "Back" and "Next" rather than a glyph.
 */
@Composable
private fun RunnerCard(
    state: HomeUiState,
    session: Session,
    onDeck: (HomeEvent) -> Unit,
) {
    val block = state.currentBlock
    val position = state.position.clampedTo(session)
    val sectionKind = state.currentSectionKind
    val finished = state.isSessionFinished
    val atStart = position == SessionPosition.Start

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (sectionKind != null) {
                Text(
                    text = sectionKindLabel(sectionKind),
                    style = MaterialTheme.typography.labelLarge,
                    color = laneSetColors.laneOnSurface,
                )
            }

            if (block == null) {
                Text(
                    text = stringResource(R.string.home_runner_complete),
                    style = MaterialTheme.typography.bodyLarge,
                )
                return@Column
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.home_runner_rep,
                        position.repetitionIndex + 1,
                        block.repetitions,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                StatusPill(verdict = verdictFor(block, state.activePacePer100Seconds))
            }

            Text(
                text = stringResource(
                    R.string.home_block_line,
                    block.repetitions,
                    block.distanceMeters,
                    strokeLabel(block.stroke),
                ),
                style = MaterialTheme.laneSetTypography.metricsRow,
            )
            Text(
                text = stringResource(
                    R.string.home_block_target_rest,
                    formatClock(block.targetTimeSeconds),
                    formatClock(block.restSeconds),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = laneSetColors.pace,
            )
            if (block.gear.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_block_gear, gearList(block.gear)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(
                    R.string.home_runner_progress,
                    session.blockOrdinalAt(position) + 1,
                    session.blocks.size,
                    session.remainingRepetitions(position),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { onDeck(HomeEvent.RetreatRequested) },
                    enabled = !atStart,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.home_runner_back))
                }
                Button(
                    onClick = { onDeck(HomeEvent.AdvanceRequested) },
                    enabled = !finished,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.home_runner_next))
                }
            }

            if (finished) {
                Text(
                    text = stringResource(R.string.home_runner_complete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = laneSetColors.restOnSurface,
                )
            }
        }
    }
}

/**
 * The whole set, section by section, with a target on every line.
 *
 * This is the artifact the app exists to produce, so it is rendered plainly: section header, one row
 * per block, the target time in the pace colour because it is the one datum a swimmer reads off a
 * clock mid-set. Empty sections are skipped rather than shown as a header over nothing.
 */
@Composable
private fun SetList(session: Session) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        var firstSection = true
        for (section in session.sections) {
            if (section.blocks.isEmpty()) continue
            if (!firstSection) HorizontalDivider()
            firstSection = false
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = sectionKindLabel(section.kind),
                    style = MaterialTheme.laneSetTypography.countChip,
                    color = laneSetColors.laneOnSurface,
                )
                Text(
                    text = stringResource(
                        R.string.home_section_totals,
                        section.totalDistanceMeters,
                        formatClock(section.totalSeconds),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (block in section.blocks) {
                BlockRow(block = block)
            }
        }
    }
}

/** One line of the set list: what to swim, and the time to swim it in. */
@Composable
private fun BlockRow(block: SetBlock) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.home_block_line,
                    block.repetitions,
                    block.distanceMeters,
                    strokeLabel(block.stroke),
                ),
                style = MaterialTheme.laneSetTypography.metricsRow,
            )
            Text(
                text = stringResource(
                    R.string.home_block_target_rest,
                    formatClock(block.targetTimeSeconds),
                    formatClock(block.restSeconds),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (block.gear.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_block_gear, gearList(block.gear)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = formatClock(block.targetTimeSeconds),
            style = MaterialTheme.laneSetTypography.paceSplit,
            color = laneSetColors.pace,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The constraints
// ---------------------------------------------------------------------------------------------

/**
 * The five inputs the session is computed from, in the order the swimmer thinks about them.
 *
 * Every control is one selectable row or chip with a word on it and at least a 48 dp target, and the
 * chips wrap instead of scrolling: a horizontal scroller hides a choice, and a form is where hiding a
 * choice is worst. Nothing here regenerates anything — the swimmer fills the form and then says when.
 *
 * The focus strip is the one control that is not a chip row, because the paid/free boundary has to be
 * visible inside the control itself: a locked focus is labelled as locked and its tap opens the
 * unlock, so the swimmer is told what is on the other side rather than told no.
 */
@Composable
private fun ConstraintsForm(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
) {
    Text(
        text = stringResource(R.string.home_constraints_header),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
    )

    ChoiceChips(
        label = stringResource(R.string.home_pool_label),
        options = POOL_LENGTH_OPTIONS,
        isSelected = { it == state.poolLengthMeters },
        labelOf = { stringResource(R.string.home_meters_value, it) },
        onSelect = { meters -> onEvent(HomeEvent.PoolLengthSelected(meters)) },
    )

    ChoiceChips(
        label = stringResource(R.string.home_minutes_label),
        options = MINUTES_OPTIONS,
        isSelected = { it == state.minutesAvailable },
        labelOf = { stringResource(R.string.home_minutes_value, it) },
        onSelect = { minutes -> onEvent(HomeEvent.MinutesSelected(minutes)) },
    )

    FocusSelectorStrip(
        options = focusOptions(state.isUnlocked),
        selectedId = state.focus.name,
        onSelect = { id ->
            for (focus in Focus.entries) {
                if (focus.name == id) onEvent(HomeEvent.FocusSelected(focus))
            }
        },
    )
    if (!state.isUnlocked) {
        Text(
            text = stringResource(R.string.home_focus_locked_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    ChoiceChips(
        label = stringResource(R.string.home_stroke_avoid_label),
        supporting = stringResource(R.string.home_strokes_supporting),
        options = Stroke.entries,
        isSelected = { it in state.strokesToAvoid },
        labelOf = { strokeLabel(it) },
        onSelect = { stroke -> onEvent(HomeEvent.StrokeAvoidToggled(stroke)) },
    )

    ChoiceChips(
        label = stringResource(R.string.home_gear_label),
        supporting = stringResource(R.string.home_gear_supporting),
        options = Gear.entries,
        isSelected = { it in state.gearOnDeck },
        labelOf = { gearLabel(it) },
        onSelect = { gear -> onEvent(HomeEvent.GearToggled(gear)) },
    )
}

/**
 * One labelled set of chips that wrap.
 *
 * `FlowRow` rather than a scrolling `Row` for the reason above: a wrapped set shows every choice, and
 * a chip row that scrolls is how a swimmer misses that fins are an option. The generic form is used
 * because the four sets differ only in their value type, their labels and what a tap sends — there is
 * no behaviour here worth four copies of.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(
    label: String,
    options: List<T>,
    isSelected: (T) -> Boolean,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    supporting: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (option in options) {
                FilterChip(
                    selected = isSelected(option),
                    onClick = { onSelect(option) },
                    label = { Text(text = labelOf(option)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

/** The one action that produces a session, and whether the one on screen still matches the form. */
@Composable
private fun BuildActions(
    state: HomeUiState,
    onDeck: (HomeEvent) -> Unit,
) {
    if (state.session != null && !state.isSessionCurrent) {
        Text(
            text = stringResource(R.string.home_constraints_stale),
            style = MaterialTheme.typography.bodySmall,
            color = laneSetColors.pace,
        )
    }
    Button(
        onClick = { onDeck(HomeEvent.GenerateRequested) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(
            stringResource(
                if (state.session == null) R.string.home_build_action else R.string.home_rebuild_action,
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The handoff, and the one thing that is not free
// ---------------------------------------------------------------------------------------------

/**
 * The ways the set list leaves the phone, and the one purchase the app has.
 *
 * Both actions are on screen whether or not they are owned, because a control that is simply absent
 * teaches the swimmer nothing. A locked tap routes to the unlock screen — that is the ViewModel's
 * decision, not this file's — and the supporting line says which half is which, so the free tier is
 * explained rather than apologised for.
 *
 * The explicit unlock button exists for the swimmer who has not tapped a locked focus yet: the app
 * has one thing to sell and should be able to say so without waiting to be asked.
 */
@Composable
private fun HandoffSection(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
) {
    Text(
        text = stringResource(R.string.home_handoff_header),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        text = stringResource(
            if (state.isUnlocked) R.string.home_handoff_supporting else R.string.home_handoff_locked,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = { onEvent(HomeEvent.ExportRequested) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.home_handoff_action))
    }

    OutlinedButton(
        onClick = { onEvent(HomeEvent.ImportRequested) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.home_import_action))
    }
    if (!state.isUnlocked) {
        Text(
            text = stringResource(R.string.home_import_locked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = { onEvent(HomeEvent.UnlockRequested) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.home_unlock_action))
        }
    }
}

/** A refused action, in the page rather than in a snackbar that is gone before it is read. */
@Composable
private fun NoticeCard(
    notice: HomeNotice,
    onDismiss: () -> Unit,
) {
    val messageRes = when (notice) {
        HomeNotice.KeepOneStroke -> R.string.home_notice_keep_one_stroke
        HomeNotice.GenerationFailed -> R.string.home_notice_generation_failed
    }
    val container = when (notice) {
        HomeNotice.KeepOneStroke -> MaterialTheme.colorScheme.secondaryContainer
        HomeNotice.GenerationFailed -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (notice) {
        HomeNotice.KeepOneStroke -> MaterialTheme.colorScheme.onSecondaryContainer
        HomeNotice.GenerationFailed -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.home_notice_dismiss))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The vocabulary. Every user-facing word is a resource; these map the domain's enums onto them.
// ---------------------------------------------------------------------------------------------

@Composable
private fun sectionKindLabel(kind: SectionKind): String = stringResource(
    when (kind) {
        SectionKind.WARM_UP -> R.string.home_section_warm_up
        SectionKind.MAIN_SET -> R.string.home_section_main_set
        SectionKind.COOL_DOWN -> R.string.home_section_cool_down
    },
)

@Composable
private fun strokeLabel(stroke: Stroke): String = stringResource(
    when (stroke) {
        Stroke.FREESTYLE -> R.string.home_stroke_freestyle
        Stroke.BACKSTROKE -> R.string.home_stroke_backstroke
        Stroke.BREASTSTROKE -> R.string.home_stroke_breaststroke
        Stroke.BUTTERFLY -> R.string.home_stroke_butterfly
        Stroke.INDIVIDUAL_MEDLEY -> R.string.home_stroke_medley
    },
)

@Composable
private fun gearLabel(gear: Gear): String = stringResource(
    when (gear) {
        Gear.KICKBOARD -> R.string.home_gear_kickboard
        Gear.PULL_BUOY -> R.string.home_gear_pull_buoy
        Gear.FINS -> R.string.home_gear_fins
        Gear.PADDLES -> R.string.home_gear_paddles
        Gear.SNORKEL -> R.string.home_gear_snorkel
    },
)

/** A focus's chip label, carrying whether it is the free one or needs the unlock. */
@Composable
private fun focusChipLabel(focus: Focus, isUnlocked: Boolean): String {
    val name = stringResource(
        when (focus) {
            Focus.SPEED -> R.string.home_focus_speed
            Focus.THRESHOLD -> R.string.home_focus_threshold
            Focus.ENDURANCE -> R.string.home_focus_endurance
            Focus.TECHNIQUE -> R.string.home_focus_technique
            Focus.RECOVERY -> R.string.home_focus_recovery
        },
    )
    return when {
        focus == FREE_FOCUS -> stringResource(R.string.home_focus_chip_free, name)
        isUnlocked -> name
        else -> stringResource(R.string.home_focus_chip_locked, name)
    }
}

/**
 * The five focuses as strip options, in the domain enum's own order.
 *
 * Built with a plain loop rather than a `map`, so the composable label is resolved in a composable
 * context instead of inside a collection lambda.
 */
@Composable
private fun focusOptions(isUnlocked: Boolean): List<FocusOption> {
    val options = mutableListOf<FocusOption>()
    for (focus in Focus.entries) {
        options.add(FocusOption(id = focus.name, label = focusChipLabel(focus, isUnlocked)))
    }
    return options
}

/** The gear of one block as one string, joined by the resource that says how a list is punctuated. */
@Composable
private fun gearList(gear: Set<Gear>): String {
    val names = mutableListOf<String>()
    for (item in Gear.entries) {
        if (item in gear) names.add(gearLabel(item))
    }
    return names.joinToString(stringResource(R.string.home_list_separator))
}

// ---------------------------------------------------------------------------------------------
// The verdict. Whether the interval the swimmer is looking at is one they can hold.
// ---------------------------------------------------------------------------------------------

/**
 * The floor, as a fraction of the pace, below which an interval is not something this app would ask
 * for — the hardest its own programming rules programme a block at (a fins set, swum faster because
 * the fins are faster).
 *
 * **Stated as a limit rather than as a fact.** `SetGenerator`'s per-focus and per-gear factors are
 * private to the generator, so this number is not imported from them; it is a restatement, and if the
 * generator's factors move this must move with them. A `verdictFor` that could read the factor the
 * block was built from would not need this constant at all, which is why the handoff names it as a
 * candidate for the generator to export.
 */
private const val HARD_EFFORT_FRACTION = 0.90

/**
 * What the app can say about one interval: can the swimmer hold this send-off?
 *
 * Two questions, and both are answered from the block's own numbers against the pace every target in
 * the app is derived from — no factor table is consulted, because the screen does not have one.
 *
 * - **Is the effort inside the pace?** The target converted back to a per-100 time, against the pace
 *   the session was computed from. Inside it, the interval asks no more per 100 m than the swimmer's
 *   threshold and carries a target time, which is the product's whole claim.
 * - **Does the send-off leave room?** `target + rest` per 100 m, against the same pace. An interval
 *   whose whole send-off is faster than the swimmer's threshold pace is one no rest has paid for.
 *
 * **The honest limit, stated rather than implied.** The target of a *generated* block comes from the
 * pace through an authored per-focus or per-gear factor, and this function cannot tell a block the
 * app deliberately programmed faster (fins, paddles, a speed set) from one that is simply too hard.
 * Both read [SetVerdict.Tight], never [SetVerdict.OverPace], which is why the third state is
 * reachable only for an interval whose send-off is faster than anything the app itself would
 * programme — in practice a pasted set with a very short explicit rest. And one real gap is outside
 * this function entirely: `CoachSetParser` clamps a send-off shorter than the target to zero rest
 * (`domain/CoachSetParser.kt:169`), so a coach's impossible `on 1:20` reaches the screen as a set with
 * no rest and is read as holdable. The parser, not this mapping, is where that would be fixed.
 */
internal fun verdictFor(block: SetBlock, pacePer100Seconds: Double): SetVerdict {
    val targetPacePer100 = block.targetPacePer100Seconds()
    val sendOffPacePer100 = (block.targetTimeSeconds + block.restSeconds) * 100.0 / block.distanceMeters
    val hardFloor = pacePer100Seconds * HARD_EFFORT_FRACTION
    return when {
        targetPacePer100 >= pacePer100Seconds && sendOffPacePer100 >= pacePer100Seconds ->
            SetVerdict.Holdable

        targetPacePer100 >= hardFloor && sendOffPacePer100 >= hardFloor ->
            SetVerdict.Tight

        else -> SetVerdict.OverPace
    }
}

/** The pace per 100 m a block's target asks for, used by [verdictFor] and by a reader of this file. */
internal fun SetBlock.targetPacePer100Seconds(): Double = targetTimeSeconds * 100.0 / distanceMeters

// ---------------------------------------------------------------------------------------------
// Previews. A synthetic session so both schemes render the real screen, not an empty one.
// ---------------------------------------------------------------------------------------------

private val previewSections: List<SessionSection> = listOf(
    SessionSection(SectionKind.WARM_UP, listOf(SetBlock(100, 3, Stroke.FREESTYLE, 131, 20))),
    SessionSection(
        SectionKind.MAIN_SET,
        listOf(
            SetBlock(100, 8, Stroke.FREESTYLE, 116, 25),
            SetBlock(50, 6, Stroke.FREESTYLE, 69, 15, gear = setOf(Gear.KICKBOARD)),
        ),
    ),
    SessionSection(SectionKind.COOL_DOWN, listOf(SetBlock(100, 2, Stroke.FREESTYLE, 131, 20))),
)

private val previewSession: Session = Session(
    constraints = SessionConstraints(
        poolLengthMeters = 25,
        minutesAvailable = 45,
        focus = Focus.ENDURANCE,
        strokesToAvoid = setOf(Stroke.BUTTERFLY),
        gearOnDeck = setOf(Gear.KICKBOARD),
    ),
    pacePer100Seconds = DEFAULT_FREE_PACE_PER_100_SECONDS,
    sections = previewSections,
    estimatedTotalSeconds = previewSections.sumOf { it.totalSeconds },
    totalDistanceMeters = previewSections.sumOf { it.totalDistanceMeters },
)

@Preview(name = "Home light", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenLightPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(session = previewSession),
            onEvent = {},
        )
    }
}

@Preview(name = "Home dark", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenDarkPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(session = previewSession, isUnlocked = true, hasTrials = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Home unlocked", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenUnlockedPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(isUnlocked = true, hasTrials = true),
            onEvent = {},
        )
    }
}
