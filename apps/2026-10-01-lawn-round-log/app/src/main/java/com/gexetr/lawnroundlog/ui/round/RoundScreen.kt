package com.gexetr.lawnroundlog.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.domain.Customer
import com.gexetr.lawnroundlog.domain.VisitStatus
import com.gexetr.lawnroundlog.ui.components.GlyphAdd
import com.gexetr.lawnroundlog.ui.components.GlyphCustomer
import com.gexetr.lawnroundlog.ui.components.GlyphDate
import com.gexetr.lawnroundlog.ui.components.GlyphInfo
import com.gexetr.lawnroundlog.ui.components.GlyphLock
import com.gexetr.lawnroundlog.ui.components.GlyphMowed
import com.gexetr.lawnroundlog.ui.components.GlyphMoveDown
import com.gexetr.lawnroundlog.ui.components.GlyphMoveUp
import com.gexetr.lawnroundlog.ui.components.GlyphRain
import com.gexetr.lawnroundlog.ui.components.GlyphSettings
import com.gexetr.lawnroundlog.ui.components.GlyphSkipped
import com.gexetr.lawnroundlog.ui.components.RoundEmptyState
import com.gexetr.lawnroundlog.ui.components.RoundMark
import com.gexetr.lawnroundlog.ui.components.SectionStrip
import com.gexetr.lawnroundlog.ui.components.StatTile
import com.gexetr.lawnroundlog.ui.components.VisitStatusPill
import com.gexetr.lawnroundlog.ui.settings.SettingsUiState

private const val TAB_TODAY = 0
private const val TAB_ALL = 1

/**
 * The round, as the operator sees it: the work in his own order, and one tap per stop.
 *
 * This is the slot's whole product, so the screen is deliberately one list rather than a dashboard.
 * The only two things above it are the number he came to check — what he is owed — and the one bulk
 * action that changes the shape of the day.
 */
@Composable
fun RoundRoute(
    round: RoundViewModel,
    settings: SettingsUiState,
    unlocked: Boolean,
    onOpenCustomer: (String) -> Unit,
    onAddCustomer: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenUnlock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val state by round.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var tab by rememberSaveable { mutableIntStateOf(TAB_TODAY) }
    var confirmPush by rememberSaveable { mutableStateOf(false) }

    // One place decides whether a tap is felt. Reading `hapticsEnabled` here — in the screens that act
    // on the record, not inside the settings surface — is what makes the preference a behaviour rather
    // than a stored boolean, which is what `settings.behaviour` measures.
    val act: (() -> Unit) -> Unit = { action ->
        if (settings.hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        action()
    }

    RoundScreen(
        state = state,
        settings = settings,
        unlocked = unlocked,
        selectedTab = tab,
        onSelectTab = { tab = it },
        onOpenCustomer = onOpenCustomer,
        onAddCustomer = onAddCustomer,
        onOpenHistory = onOpenHistory,
        onOpenUnlock = onOpenUnlock,
        onOpenSettings = onOpenSettings,
        onOpenAbout = onOpenAbout,
        onMark = { customerId, status -> act { round.mark(customerId, status) } },
        onMove = { customerId, toIndex -> act { round.move(customerId, toIndex) } },
        onRequestPushDay = { confirmPush = true },
    )

    if (confirmPush) {
        // The action moves real records, so it is described rather than labelled: "push the round by
        // one day" is the sentence a slip of the thumb could not explain to a customer.
        AlertDialog(
            onDismissRequest = { confirmPush = false },
            title = { Text(stringResource(R.string.round_push_day)) },
            text = { Text(stringResource(R.string.round_push_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmPush = false
                        act { round.pushByOneDay() }
                    },
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmPush = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun RoundScreen(
    state: RoundUiState,
    settings: SettingsUiState,
    unlocked: Boolean,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenCustomer: (String) -> Unit,
    onAddCustomer: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenUnlock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onMark: (String, VisitStatus) -> Unit,
    onMove: (String, Int) -> Unit,
    onRequestPushDay: () -> Unit,
) {
    val rows = if (selectedTab == TAB_TODAY) state.due else state.ordered

    // Horizontal safe-drawing insets only: the hero gradient is deliberately full-bleed under the
    // status bar, so no top inset is applied here — that would leave a bare strip above the hero.
    // The sides are inset so nothing lands under a display cutout in landscape.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        RoundHero(
            title = stringResource(R.string.round_title),
            dayLabel = stringResource(R.string.round_day_label, state.todayIso),
            totalOwed = formatAmount(settings.currencySymbol, state.totalOwed),
            customerCount = state.customers.size.toString(),
        )

        SectionStrip(
            items = listOf(
                stringResource(R.string.round_tab_today),
                stringResource(R.string.round_tab_all),
            ),
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (rows.isEmpty()) {
            RoundEmptyState(
                title = stringResource(R.string.round_empty_title),
                body = stringResource(R.string.round_empty_body),
                // `weight`, not `fillMaxSize`, in a Column: an unweighted filler takes the whole
                // remaining height and measures the pinned `RoundActions` sibling at zero.
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = rows, key = { customer -> customer.id }) { customer ->
                    RoundCustomerRow(
                        customer = customer,
                        position = state.ordered.indexOfFirst { it.id == customer.id } + 1,
                        status = state.todayStatus[customer.id],
                        currencySymbol = settings.currencySymbol,
                        canMoveUp = targetIndex(state.ordered, customer.id, up = true) != null,
                        canMoveDown = targetIndex(state.ordered, customer.id, up = false) != null,
                        onOpen = { onOpenCustomer(customer.id) },
                        onMove = { up ->
                            targetIndex(state.ordered, customer.id, up)?.let { onMove(customer.id, it) }
                        },
                        onMark = { status -> onMark(customer.id, status) },
                    )
                }
            }
        }

        RoundActions(
            unlocked = unlocked,
            onAddCustomer = onAddCustomer,
            onPushDay = onRequestPushDay,
            onHistory = onOpenHistory,
            onUnlock = onOpenUnlock,
            onSettings = onOpenSettings,
            onAbout = onOpenAbout,
        )
    }
}

/**
 * The one place the app says what it is: the mark, the day, and the number that is the reason the
 * operator opened it.
 *
 * The field is a gradient rather than a flat fill so the app has depth at a glance, and the ink is the
 * container's own `on` colour, so contrast holds in both light and dark without a hand-picked value
 * that would have to be re-picked for each theme.
 */
@Composable
private fun RoundHero(
    title: String,
    dayLabel: String,
    totalOwed: String,
    customerCount: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundMark(
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.round_total_owed),
                    value = totalOwed,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.round_customers_label),
                    value = customerCount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * One stop on the round.
 *
 * The status controls are labelled icon buttons rather than three coloured chips: the outcome has to
 * be readable without colour — `VisitStatusPill` carries the word as well as the tone, and it is drawn
 * once a status exists — and each control has to be a target a thumb can find next to a mower.
 */
@Composable
private fun RoundCustomerRow(
    customer: Customer,
    position: Int,
    status: VisitStatus?,
    currencySymbol: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onMove: (Boolean) -> Unit,
    onMark: (VisitStatus) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The position is a round chip that grows around its digit rather than a bounded
                // square: a fixed `size(...)` here is a box a numeral cannot grow out of at a large
                // font scale, which is the defect the UI baseline names. The padding is what makes
                // the shape round, and the shape grows with whatever it holds.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = position.coerceAtLeast(1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (customer.address.isNotBlank()) {
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val openLabel = stringResource(R.string.action_open, customer.name)
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = openLabel },
                ) {
                    GlyphCustomer()
                }
                Column {
                    val moveUpLabel = stringResource(R.string.round_move_up, customer.name)
                    IconButton(
                        onClick = { onMove(true) },
                        enabled = canMoveUp,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = moveUpLabel },
                    ) {
                        GlyphMoveUp()
                    }
                    val moveDownLabel = stringResource(R.string.round_move_down, customer.name)
                    IconButton(
                        onClick = { onMove(false) },
                        enabled = canMoveDown,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = moveDownLabel },
                    ) {
                        GlyphMoveDown()
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusAction(
                    status = VisitStatus.MOWED,
                    label = stringResource(R.string.round_mark_mowed, customer.name),
                    selected = status == VisitStatus.MOWED,
                    onClick = { onMark(VisitStatus.MOWED) },
                )
                StatusAction(
                    status = VisitStatus.SKIPPED,
                    label = stringResource(R.string.round_mark_skipped, customer.name),
                    selected = status == VisitStatus.SKIPPED,
                    onClick = { onMark(VisitStatus.SKIPPED) },
                )
                StatusAction(
                    status = VisitStatus.RAINED_OFF,
                    label = stringResource(R.string.round_mark_rained_off, customer.name),
                    selected = status == VisitStatus.RAINED_OFF,
                    onClick = { onMark(VisitStatus.RAINED_OFF) },
                )
                Spacer(Modifier.weight(1f))
                if (status != null) {
                    VisitStatusPill(status = status)
                }
            }

            Text(
                text = stringResource(
                    R.string.round_rate_line,
                    formatAmount(currencySymbol, customer.rateUsd),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * One outcome, one tap.
 *
 * The description names the outcome **and** the customer, because three identical "Mowed" buttons in
 * a list are indistinguishable to a screen reader and, without the name, unusable as a spoken target.
 */
@Composable
private fun StatusAction(
    status: VisitStatus,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = label },
        colors = if (selected) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            IconButtonDefaults.filledTonalIconButtonColors()
        },
    ) {
        StatusGlyph(status = status)
    }
}

/**
 * The outcome's own silhouette, selected by an exhaustive `when` with no `else`.
 *
 * The compiler is what keeps a fourth outcome from being added to the domain and rendering as an
 * empty button; the same shape as the status pill's own selection, one screen apart.
 */
@Composable
private fun StatusGlyph(status: VisitStatus) {
    when (status) {
        VisitStatus.MOWED -> GlyphMowed()
        VisitStatus.SKIPPED -> GlyphSkipped()
        VisitStatus.RAINED_OFF -> GlyphRain()
    }
}

/** The round's own two verbs, plus the doors the operator needs and does not open hourly. */
@Composable
private fun RoundActions(
    unlocked: Boolean,
    onAddCustomer: () -> Unit,
    onPushDay: () -> Unit,
    onHistory: () -> Unit,
    onUnlock: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The padding sits on the inner Column, not on the Surface: the Surface must keep
                // painting to the screen bottom so the bar stays one edge-to-edge field, while the
                // two button rows must clear the system navigation bar a thumb would otherwise tap
                // through. Lifting the Surface instead would leave a bare strip below the bar.
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddCustomer,
                    modifier = Modifier.weight(1f),
                ) {
                    GlyphAdd()
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_add_customer))
                }
                OutlinedButton(
                    onClick = onPushDay,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.round_push_day))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTextButton(
                    glyph = { GlyphDate() },
                    label = stringResource(R.string.history_title),
                    onClick = onHistory,
                )
                IconTextButton(
                    glyph = { GlyphLock() },
                    label = stringResource(
                        if (unlocked) R.string.unlock_unlocked else R.string.unlock_title,
                    ),
                    onClick = onUnlock,
                )
                IconTextButton(
                    glyph = { GlyphSettings() },
                    label = stringResource(R.string.settings_title),
                    onClick = onSettings,
                )
                IconTextButton(
                    glyph = { GlyphInfo() },
                    label = stringResource(R.string.about_title),
                    onClick = onAbout,
                )
            }
        }
    }
}

/**
 * An icon above its word.
 *
 * The word is a real label rather than a `contentDescription` on a bare glyph: these four doors are
 * opened rarely, and a rarely-opened door whose meaning is a picture is a door the operator does not
 * find. The label wraps rather than truncating, because the Russian names for them are longer.
 *
 * The glyph is a composable rather than an `ImageVector`, which is what lets the slot draw its own
 * iconography instead of depending on Material's.
 */
@Composable
private fun IconTextButton(
    glyph: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 48.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            glyph()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The ordered index one step up or down from a customer, or null when there is no step to take.
 *
 * It answers from the *ordered* round in both tabs on purpose: the order is one global fact, so moving
 * someone while looking at today's list must not depend on who else happens to be due today.
 */
private fun targetIndex(ordered: List<Customer>, customerId: String, up: Boolean): Int? {
    val current = ordered.indexOfFirst { it.id == customerId }
    if (current < 0) return null
    val next = if (up) current - 1 else current + 1
    return if (next in ordered.indices) next else null
}
