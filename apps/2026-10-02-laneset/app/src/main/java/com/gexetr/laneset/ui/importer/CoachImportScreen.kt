package com.gexetr.laneset.ui.importer

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.laneset.R
import com.gexetr.laneset.domain.Focus
import com.gexetr.laneset.domain.SectionKind
import com.gexetr.laneset.domain.Session
import com.gexetr.laneset.domain.SessionConstraints
import com.gexetr.laneset.domain.SessionSection
import com.gexetr.laneset.domain.SetBlock
import com.gexetr.laneset.domain.Stroke
import com.gexetr.laneset.ui.components.BrandMark
import com.gexetr.laneset.ui.components.StatTile
import com.gexetr.laneset.ui.export.SessionExport
import com.gexetr.laneset.ui.theme.PlayKilnTheme
import kotlin.math.roundToInt

/**
 * The import route: state wiring only, so the screen below stays a pure function of its arguments.
 *
 * The factory is built from the application context and reaches the stores through `AppGraph`, so the
 * screen never names a DataStore and a preview can hand it a state directly.
 */
@Composable
fun CoachImportRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoachImportViewModel = viewModel(
        factory = CoachImportViewModel.factory(LocalContext.current),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoachImportScreen(
        state = state,
        onBack = onBack,
        onTextChange = viewModel::onTextChange,
        onPasteFromClipboard = viewModel::onPasteFromClipboard,
        onRead = viewModel::onRead,
        onAccept = viewModel::onAccept,
        modifier = modifier,
    )
}

/**
 * The coach-workout import surface.
 *
 * The screen is a function of one [CoachImportUiState], so every branch — the empty form, each
 * refusal, the review and the saved confirmation — is reachable in a preview and in a test without a
 * store, a device or a network.
 *
 * Three decisions are deliberate and are the reason the code is shaped this way:
 *
 * - **The parser is never re-implemented here.** The review is the `Session`
 *   [CoachImportViewModel] got back from `CoachSetParser.parse`, rendered through the same
 *   [SessionExport] renderer the share and print paths use, so what the swimmer approves and what
 *   leaves the phone cannot disagree. The parser's own vocabulary is the pool's canonical notation,
 *   which is also what the shared copy uses by design.
 * - **A refusal is visible, not silent.** Nothing read, nothing recognised and no threshold pace are
 *   three different sentences with three different remedies, and a line the parser skipped is named
 *   by its number rather than dropped.
 * - **The store is the bus.** Accepting calls `SessionStore.save` through the ViewModel; this screen
 *   publishes no event, because Home already reads the store.
 *
 * Accessibility: the back control is an `IconButton` (48 dp by construction) with a named glyph, every
 * `Icon` declares its `contentDescription`, the field is carried clear of the keyboard with
 * `imePadding()`, and the whole surface scrolls so a long paste is reachable one-handed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachImportScreen(
    state: CoachImportUiState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onPasteFromClipboard: (String?) -> Unit,
    onRead: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.import_back),
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.import_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExampleCard()

            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.import_field_label)) },
                placeholder = { Text(stringResource(R.string.import_field_placeholder)) },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (state.text.isNotEmpty()) {
                        IconButton(onClick = { onTextChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.import_clear_field),
                            )
                        }
                    }
                },
            )

            OutlinedButton(
                onClick = { onPasteFromClipboard(readClipboardText(context)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_clipboard_action))
            }

            Button(
                onClick = onRead,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pool),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_read_action))
            }

            when (state.status) {
                CoachImportStatus.Idle -> EmptyHint()
                CoachImportStatus.EmptyInput -> StatusNotice(
                    icon = R.drawable.ic_info,
                    text = stringResource(R.string.import_status_empty),
                    tone = MaterialTheme.colorScheme.error,
                )
                CoachImportStatus.ClipboardEmpty -> StatusNotice(
                    icon = R.drawable.ic_info,
                    text = stringResource(R.string.import_status_clipboard_empty),
                    tone = MaterialTheme.colorScheme.error,
                )
                CoachImportStatus.NothingRecognised -> {
                    StatusNotice(
                        icon = R.drawable.ic_info,
                        text = stringResource(R.string.import_status_nothing),
                        tone = MaterialTheme.colorScheme.error,
                    )
                    UnreadableLines(state.unreadableLineNumbers)
                }
                CoachImportStatus.NeedsPace -> StatusNotice(
                    icon = R.drawable.ic_timer,
                    text = stringResource(R.string.import_status_needs_pace),
                    tone = MaterialTheme.colorScheme.error,
                )
                CoachImportStatus.Reviewed -> {
                    val session = state.parsed
                    if (session != null) {
                        Review(
                            session = session,
                            unreadableLineNumbers = state.unreadableLineNumbers,
                            onAccept = onAccept,
                        )
                    }
                }
                CoachImportStatus.Saved -> StatusNotice(
                    icon = R.drawable.ic_check,
                    text = stringResource(R.string.import_saved),
                    tone = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Reads the clipboard as text, or `null` when there is nothing there.
 *
 * The paste button exists because the coach's set usually arrives in a messaging app the swimmer has
 * to leave to reach this screen: retyping it is the friction the *before* stage of the job is meant to
 * remove. A clipboard that has no text (or holds an image, a link the provider will not coerce) is
 * handed to the view-model as `null`, which is what turns it into a visible refusal rather than a tap
 * that appears to do nothing.
 */
private fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = clipboard?.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val text = clip.getItemAt(0).coerceToText(context).toString()
    return text.ifBlank { null }
}

/** What a set looks like in the notation the parser reads, so the swimmer can paste one. */
@Composable
private fun ExampleCard() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.import_example_header),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.import_example_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The set before anything has been read: the mark and one sentence about what will appear. */
@Composable
private fun EmptyHint() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandMark(modifier = Modifier.size(72.dp))
        Text(
            text = stringResource(R.string.import_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** One refusal or confirmation, as a card rather than a bare line of red text. */
@Composable
private fun StatusNotice(icon: Int, text: String, tone: Color) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * The lines the parser could not turn into a set, by number.
 *
 * Shown in both refusals: when nothing at all was recognised it is the only actionable information
 * the swimmer has, and when part of the practice was read it says which part was not. Renders
 * nothing when the list is empty, so a clean paste carries no warning.
 */
@Composable
private fun UnreadableLines(unreadableLineNumbers: List<Int>) {
    if (unreadableLineNumbers.isEmpty()) return
    Text(
        text = stringResource(R.string.import_unreadable_header),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.error,
    )
    unreadableLineNumbers.forEach { number ->
        Text(
            text = stringResource(R.string.import_unreadable_line, number),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The parsed session to approve: what was read, what was skipped, and the set list as it will leave.
 *
 * The numbers are the session's own ([Session.totalDistanceMeters], `estimatedTotalSeconds`,
 * `blocks.size`, `pacePer100Seconds`), never re-added here — the domain already knows them, and a
 * second sum is a second answer.
 */
@Composable
private fun Review(
    session: Session,
    unreadableLineNumbers: List<Int>,
    onAccept: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.import_review_header),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.import_review_supporting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.import_stat_distance),
                value = stringResource(R.string.import_meters_value, session.totalDistanceMeters),
                icon = R.drawable.ic_pool,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.import_stat_time),
                value = SessionExport.formatSeconds(session.estimatedTotalSeconds),
                icon = R.drawable.ic_timer,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.import_stat_intervals),
                value = session.blocks.size.toString(),
                icon = R.drawable.ic_add,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.import_stat_pace),
                value = stringResource(
                    R.string.import_pace_value,
                    SessionExport.formatSeconds(session.pacePer100Seconds.roundToInt()),
                ),
                icon = R.drawable.ic_water_drop,
                modifier = Modifier.weight(1f),
            )
        }

        UnreadableLines(unreadableLineNumbers)

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = SessionExport.buildSetListText(session),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
        }

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.import_accept_action))
        }
    }
}

/**
 * A session whose verdict is known, so the review can be inspected without a store or a device.
 *
 * It is the worked example from the design's own shape — a 300 warm-up, an 8 x 100 main set and a
 * 200 cool-down at a 1:45 threshold — so the preview reads as the practice the copy describes.
 */
private val previewSession = Session(
    constraints = SessionConstraints(
        poolLengthMeters = 25,
        minutesAvailable = 45,
        focus = Focus.ENDURANCE,
        strokesToAvoid = emptySet(),
        gearOnDeck = emptySet(),
    ),
    pacePer100Seconds = 105.0,
    sections = listOf(
        SessionSection(
            kind = SectionKind.WARM_UP,
            blocks = listOf(SetBlock(300, 1, Stroke.FREESTYLE, 315, 0)),
        ),
        SessionSection(
            kind = SectionKind.MAIN_SET,
            blocks = listOf(SetBlock(100, 8, Stroke.FREESTYLE, 105, 15)),
        ),
        SessionSection(
            kind = SectionKind.COOL_DOWN,
            blocks = listOf(SetBlock(200, 1, Stroke.FREESTYLE, 210, 0)),
        ),
    ),
    estimatedTotalSeconds = 3000,
    totalDistanceMeters = 1300,
)

@Preview(name = "Import empty", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun CoachImportIdlePreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        CoachImportScreen(
            state = CoachImportUiState(),
            onBack = {},
            onTextChange = {},
            onPasteFromClipboard = {},
            onRead = {},
            onAccept = {},
        )
    }
}

@Preview(name = "Import reviewed", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun CoachImportReviewedPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        CoachImportScreen(
            state = CoachImportUiState(
                text = "Warm up 300 free\n8 x 100 free on 1:45\n4 x 50 fly rest 20\nCool down 200",
                parsed = previewSession,
                unreadableLineNumbers = listOf(4),
                status = CoachImportStatus.Reviewed,
            ),
            onBack = {},
            onTextChange = {},
            onPasteFromClipboard = {},
            onRead = {},
            onAccept = {},
        )
    }
}
