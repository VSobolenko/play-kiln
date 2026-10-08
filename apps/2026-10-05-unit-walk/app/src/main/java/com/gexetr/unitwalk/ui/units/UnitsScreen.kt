package com.gexetr.unitwalk.ui.units

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.capability.billing.BillingController
import com.gexetr.unitwalk.R
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.ui.components.EmptyState
import com.gexetr.unitwalk.ui.components.HeroPanel
import com.gexetr.unitwalk.ui.components.PillKind
import com.gexetr.unitwalk.ui.components.StatTile
import com.gexetr.unitwalk.ui.components.StatusPill
import com.gexetr.unitwalk.ui.theme.UnitWalkTheme

/**
 * The portfolio: the landlord's named units, and the walk he starts or continues from each one.
 *
 * The screen is split the way `ui/compare/CompareScreen.kt` is split:
 * [UnitsScreen] / [UnitsRoute] build the ViewModel and collect its state, while [UnitsContent] is a
 * pure function of [UnitsUiState] that emits [UnitsEvent]s. No business logic lives in a composable,
 * and no user-facing word is compiled in — every label is a `stringResource`.
 *
 * Four product rules are honoured where they are visible:
 *
 *  - **Nothing is fabricated.** A unit with no walks and a unit with no finished move-in render the
 *    honest states `units_no_walks_body` and `units_prior_absent_body`; a walk count and a baseline
 *    state are read from [UnitRow] and never rounded up, so an empty portfolio never shows a row that
 *    is not on disk.
 *  - **Deleting is a two-step act.** Tapping Delete only stages [UnitsUiState.pendingDelete], and the
 *    confirmation names the unit and the exact number of walks it takes with it; the removal itself is
 *    [UnitsEvent.ConfirmDeleteUnit].
 *  - **A draft is resumed durably.** Starting or continuing a walk goes through the brain and opens
 *    only once [UnitsUiState.openWalk] names a walk whose snapshot is already on disk, so a kill
 *    between the tap and the screen cannot lose the walk.
 *  - **The free plan holds one unit.** While the unlock is not owned only the portfolio's first unit
 *    is usable, and that is shown rather than hidden: a locked row keeps its name and its walk count
 *    and offers the Unlock destination in place of its walk actions, so the portfolio tells the truth
 *    instead of refusing a tap it still offered. A refused create, start or resume leaves the
 *    portfolio untouched; the Unlock destination opens only when the user taps the action on the
 *    message, and [UnitsUiState.openUnlock] is the one-shot that arms the action rather than a
 *    navigation instruction. Nothing the user already has is deleted or hidden to enforce the cap.
 */
@Composable
fun UnitsScreen(
    onStartWalk: (unitId: String, kind: WalkKind) -> Unit,
    onResumeWalk: (walkId: String) -> Unit,
    onOpenCompare: (walkId: String) -> Unit,
    onOpenUnlock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    billing: BillingController? = null,
) {
    val context = LocalContext.current
    val viewModel: UnitsViewModel = viewModel(factory = UnitsViewModel.factory(context, billing))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEvent(UnitsEvent.ScreenShown) }

    // The walk is opened only after the brain has persisted it and published `openWalk`, so the id the
    // shell navigates with always names a walk a restart would still find.
    LaunchedEffect(state.openWalk) {
        val walk = state.openWalk ?: return@LaunchedEffect
        onResumeWalk(walk.walkId)
        viewModel.onEvent(UnitsEvent.WalkOpened)
    }

    UnitsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onStartWalk = onStartWalk,
        onOpenCompare = onOpenCompare,
        onOpenUnlock = onOpenUnlock,
        onOpenSettings = onOpenSettings,
        onOpenAbout = onOpenAbout,
        modifier = modifier,
    )
}

/**
 * The route form, mirroring `CompareRoute`: the shell may call either name, and both
 * build the same ViewModel over the portfolio.
 */
@Composable
fun UnitsRoute(
    onStartWalk: (unitId: String, kind: WalkKind) -> Unit,
    onResumeWalk: (walkId: String) -> Unit,
    onOpenCompare: (walkId: String) -> Unit,
    onOpenUnlock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    billing: BillingController? = null,
) {
    UnitsScreen(
        onStartWalk = onStartWalk,
        onResumeWalk = onResumeWalk,
        onOpenCompare = onOpenCompare,
        onOpenUnlock = onOpenUnlock,
        onOpenSettings = onOpenSettings,
        onOpenAbout = onOpenAbout,
        modifier = modifier,
        billing = billing,
    )
}

/** The unit the editor dialog is filling: `unitId == null` creates, otherwise it renames. */
private data class UnitEditor(val unitId: String?, val initialName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitsContent(
    state: UnitsUiState,
    onEvent: (UnitsEvent) -> Unit,
    onStartWalk: (String, WalkKind) -> Unit,
    onOpenCompare: (String) -> Unit,
    onOpenUnlock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var editor by remember { mutableStateOf<UnitEditor?>(null) }

    val messageText: String? = state.message?.let { message ->
        stringResource(
            when (message) {
                UnitsMessage.UnitCreated -> R.string.units_created_message
                UnitsMessage.UnitRenamed -> R.string.units_renamed_message
                UnitsMessage.UnitDeleted -> R.string.units_deleted_message
                UnitsMessage.NameRequired -> R.string.units_name_required_message
                UnitsMessage.UnitNotFound -> R.string.units_not_found_message
                UnitsMessage.WalkStarted -> R.string.units_walk_started_message
                UnitsMessage.UnitLimitReached -> R.string.units_limit_reached_message
                UnitsMessage.NotSaved -> R.string.units_not_saved_message
            },
        )
    }

    // The free-plan refusal is the one message that carries a route to the unlock, and
    // [UnitsUiState.openUnlock] is the one-shot that says this message is that refusal. The duration
    // is chosen rather than defaulted: Material 3 defaults a snackbar *with an action* to
    // `Indefinite`, which would sit on screen until dismissed and hold back every later message, and
    // `Short` (~4 s) is too brief to read why the create was refused and decide, so the refusal asks
    // for `Long` deliberately.
    //
    // The effect is keyed on [UnitsUiState.messageId], not on `message`: `UnitsMessage` cases are
    // `data object`s, so a second refusal while the first snackbar is still up would be an equal key
    // and would show nothing at all. The marker is bumped on every publication, so every attempt —
    // including a repeated one — produces its own feedback.
    val unlockAction = stringResource(R.string.units_unlock_action)
    val refusalOffersUnlock = state.message == UnitsMessage.UnitLimitReached && state.openUnlock

    LaunchedEffect(state.messageId) {
        if (state.message == null) return@LaunchedEffect
        if (messageText != null && refusalOffersUnlock) {
            // The one-shot is consumed here, as it is read, so a later unrelated message can never
            // inherit the action. Consuming it leaves `message` itself alone, which is what keeps this
            // snackbar on screen until the user acts or it times out.
            onEvent(UnitsEvent.UnlockOpened)
            val result = snackbarHostState.showSnackbar(
                message = messageText,
                actionLabel = unlockAction,
                duration = SnackbarDuration.Long,
            )
            onEvent(UnitsEvent.MessageShown)
            // Navigation is the last act: leaving this composition cancels this effect, so the
            // cleanup above must already have been dispatched.
            if (result == SnackbarResult.ActionPerformed) onOpenUnlock()
        } else {
            if (messageText != null) snackbarHostState.showSnackbar(messageText)
            onEvent(UnitsEvent.MessageShown)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.units_title)) },
                actions = {
                    IconButton(onClick = onOpenUnlock) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = stringResource(R.string.units_unlock_action),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                        )
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.isLoading) {
                // The hero is a reading of the portfolio, so it is not drawn before the portfolio
                // has been read: a count of zero while the document is still loading would be a
                // fabricated row rather than an empty state.
                HeroPanel(
                    title = stringResource(R.string.units_hero_title),
                    subtitle = stringResource(R.string.units_hero_subtitle),
                    stat = state.units.size.toString(),
                    statLabel = stringResource(R.string.units_hero_stat_label),
                )
            }

            Button(
                onClick = { editor = UnitEditor(unitId = null, initialName = "") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.units_add_action))
            }

            if (!state.isLoading && state.units.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.units_empty_title),
                    body = stringResource(R.string.units_empty_body),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.units.forEach { unit ->
                UnitCard(
                    unit = unit,
                    onEvent = onEvent,
                    onStartWalk = onStartWalk,
                    onOpenCompare = onOpenCompare,
                    onOpenUnlock = onOpenUnlock,
                    onEdit = { editor = UnitEditor(unitId = unit.id, initialName = unit.name) },
                )
            }

            TextButton(
                onClick = onOpenAbout,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.units_about_action))
            }
        }
    }

    editor?.let { editing ->
        var name by remember(editing) { mutableStateOf(editing.initialName) }
        val creating = editing.unitId == null
        AlertDialog(
            onDismissRequest = { editor = null },
            title = {
                Text(
                    stringResource(
                        if (creating) R.string.units_new_unit_title else R.string.units_rename_title,
                    ),
                )
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.units_name_label)) },
                    placeholder = { Text(stringResource(R.string.units_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val unitId = editing.unitId
                        if (unitId == null) {
                            onEvent(UnitsEvent.CreateUnit(name))
                        } else {
                            onEvent(UnitsEvent.RenameUnit(unitId, name))
                        }
                        editor = null
                    },
                ) {
                    Text(
                        stringResource(
                            if (creating) R.string.units_create_action else R.string.units_save_action,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { editor = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    state.pendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { onEvent(UnitsEvent.CancelDeleteUnit) },
            title = { Text(stringResource(R.string.units_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.units_delete_body,
                        pending.unitName,
                        pending.walkCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(UnitsEvent.ConfirmDeleteUnit) }) {
                    Text(stringResource(R.string.units_delete_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(UnitsEvent.CancelDeleteUnit) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * One unit: its name and address, how many walks it carries, which of them is resumable, and the three
 * ways to begin a new one. Nothing here is invented — a unit with no walks shows the honest empty
 * sentence rather than a zeroed row.
 */
@Composable
private fun UnitCard(
    unit: UnitRow,
    onEvent: (UnitsEvent) -> Unit,
    onStartWalk: (String, WalkKind) -> Unit,
    onOpenCompare: (String) -> Unit,
    onOpenUnlock: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = unit.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unit.address.isNotBlank()) {
                Text(
                    text = unit.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // The baseline state is a word and a shape as well as a colour, so it survives greyscale.
            StatusPill(
                label = stringResource(
                    if (unit.hasPriorMoveIn) {
                        R.string.units_status_prior_present
                    } else {
                        R.string.units_status_prior_absent
                    },
                ),
                kind = if (unit.hasPriorMoveIn) PillKind.OK else PillKind.NEUTRAL,
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.units_walks_label),
                    value = unit.walkCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            if (unit.locked) {
                // The unit is behind the one-time unlock. Its record is all above — name, address,
                // status and walk count — and only the way in is closed, so the row is shown rather
                // than hidden and the cap is legible instead of a tap that mysteriously does nothing.
                // The lock is carried by the icon and the word together, never by colour alone
                // (ui-ux-baseline). The action is the same Unlock destination the top bar opens; the
                // brain refuses the walk routes behind it as well.
                LockedUnitAction(onOpenUnlock = onOpenUnlock)
            } else {
                when {
                    unit.walkCount == 0 -> Text(
                        text = stringResource(R.string.units_no_walks_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    !unit.hasPriorMoveIn -> Text(
                        text = stringResource(R.string.units_prior_absent_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                unit.resumableDraft?.let { draft ->
                    Button(
                        onClick = { onEvent(UnitsEvent.ResumeWalk(draft.walkId)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.units_continue_action))
                    }
                    if (unit.hasPriorMoveIn) {
                        OutlinedButton(
                            onClick = { onOpenCompare(draft.walkId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.units_compare_action))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.units_start_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StartWalkButton(
                    labelRes = R.string.units_kind_move_in,
                    onClick = {
                        onStartWalk(unit.id, WalkKind.MOVE_IN)
                        onEvent(UnitsEvent.StartWalk(unit.id, WalkKind.MOVE_IN))
                    },
                )
                StartWalkButton(
                    labelRes = R.string.units_kind_move_out,
                    onClick = {
                        onStartWalk(unit.id, WalkKind.MOVE_OUT)
                        onEvent(UnitsEvent.StartWalk(unit.id, WalkKind.MOVE_OUT))
                    },
                )
                StartWalkButton(
                    labelRes = R.string.units_kind_single,
                    onClick = {
                        onStartWalk(unit.id, WalkKind.SINGLE)
                        onEvent(UnitsEvent.StartWalk(unit.id, WalkKind.SINGLE))
                    },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.units_rename_action))
                }
                TextButton(
                    onClick = { onEvent(UnitsEvent.RequestDeleteUnit(unit.id)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.units_delete_action))
                }
            }
        }
    }
}

/**
 * One way to begin a walk. The label is a full-width button rather than a chip in a row, because the
 * three words do not fit beside each other in every locale and a fixed-width row would be a defect
 * that only appears after translation.
 */
@Composable
private fun StartWalkButton(
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(labelRes))
    }
}

/**
 * The one affordance a locked unit offers in place of its walk actions: it says the unlock is needed
 * and opens the Unlock destination.
 *
 * It adds no icon and no string of its own — the lock glyph is the vendored set's `ic_lock` and the
 * label and content description are the same `units_unlock_action` the portfolio's top bar already
 * uses — and it is a full-width `OutlinedButton` like [StartWalkButton] so the row keeps one action
 * vocabulary. The lock is stated by the glyph and the word together, not by colour alone.
 */
@Composable
private fun LockedUnitAction(onOpenUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.units_unlock_action)
    OutlinedButton(
        onClick = onOpenUnlock,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = label,
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Preview(name = "Units light", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun UnitsContentLightPreview() {
    UnitWalkTheme(darkTheme = false, dynamicColor = false) {
        UnitsContent(
            state = UnitsPreviewState,
            onEvent = {},
            onStartWalk = { _, _ -> },
            onOpenCompare = {},
            onOpenUnlock = {},
            onOpenSettings = {},
            onOpenAbout = {},
        )
    }
}

@Preview(
    name = "Units dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun UnitsContentDarkPreview() {
    UnitWalkTheme(darkTheme = true, dynamicColor = false) {
        UnitsContent(
            state = UnitsPreviewState,
            onEvent = {},
            onStartWalk = { _, _ -> },
            onOpenCompare = {},
            onOpenUnlock = {},
            onOpenSettings = {},
            onOpenAbout = {},
        )
    }
}

// `unlock` defaults to `UnlockState.Unknown`, which is not owned, so the second unit is the locked
// state and the first is the free one: the preview shows both the walk actions and the lock
// affordance without pretending the portfolio is unlimited.
private val UnitsPreviewState: UnitsUiState = UnitsUiState(
    isLoading = false,
    units = listOf(
        UnitRow(
            id = "unit-1",
            name = "Flat 3B",
            address = "12 Baker Street",
            walkCount = 2,
            resumableDraft = WalkRef(walkId = "walk-1", unitId = "unit-1", kind = WalkKind.MOVE_OUT),
            hasPriorMoveIn = true,
            locked = false,
        ),
        UnitRow(
            id = "unit-2",
            name = "Studio 4A",
            address = "",
            walkCount = 0,
            resumableDraft = null,
            hasPriorMoveIn = false,
            locked = true,
        ),
    ),
)
