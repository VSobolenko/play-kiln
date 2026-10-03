package com.gexetr.laneset.ui.pace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.laneset.R
import com.gexetr.laneset.data.PaceProfile
import com.gexetr.laneset.ui.components.BrandMark
import com.gexetr.laneset.ui.components.PaceHeroPanel
import com.gexetr.laneset.ui.components.StatTile
import com.gexetr.laneset.ui.theme.PlayKilnTheme

/**
 * The pace-test route: state wiring only, so the screen below stays a pure function of its arguments.
 *
 * The entry point is exactly `PaceTestRoute(onBack, modifier)` because `ui/PlayKilnApp.kt` pushes this
 * destination on the host's own back stack and calls it by that shape.
 */
@Composable
fun PaceTestRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PaceTestViewModel =
        viewModel(factory = PaceTestViewModel.factory(LocalContext.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaceTestScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

/**
 * The guided 400 m / 200 m time trial.
 *
 * The screen walks two steps rather than presenting two bare number fields, because the reason for
 * each swim is the product: the 400 m is the slower trial that sets the top of the pace line, the
 * 200 m is the faster one, and the *gap* between them is the threshold pace the generator puts on
 * every interval (`domain/PaceModel.kt`). So each step states its reason in words above the field.
 *
 * The derived pace is the hero panel's headline number — the slot's own component, not a stock card —
 * and the two trials are stat tiles under it. An impossible pair is never a crash and never a silent
 * old pace: it is a typed [PaceTestError] the screen resolves to a sentence that names what is wrong.
 *
 * Insets are consumed where the rule requires them: the `Scaffold`'s `innerPadding` goes on the
 * scrolling child with `consumeWindowInsets`, and `imePadding()` keeps the field clear of the
 * keyboard, which the `Scaffold` default does not carry
 * (`.cursor/rules/ui-ux-baseline.mdc`). Every control is a 48 dp target with a resource-backed name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaceTestScreen(
    state: PaceTestUiState,
    onBack: () -> Unit,
    onEvent: (PaceTestEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pace_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.pace_back))
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val errorText = paceErrorText(state.error)
            when (state.step) {
                PaceTestStep.FOUR_HUNDRED -> {
                    Text(
                        text = stringResource(R.string.pace_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TimeStep(
                        stepLabel = stringResource(R.string.pace_step_400_label),
                        reason = stringResource(R.string.pace_step_400_reason),
                        fieldLabel = stringResource(R.string.pace_time_400_label),
                        value = state.time400Input,
                        errorText = errorText,
                        imeAction = ImeAction.Next,
                        onValueChange = { onEvent(PaceTestEvent.Time400Changed(it)) },
                        onPrimary = { onEvent(PaceTestEvent.SubmitTime400) },
                        primaryLabel = stringResource(R.string.pace_next_action),
                    )
                }

                PaceTestStep.TWO_HUNDRED -> TimeStep(
                    stepLabel = stringResource(R.string.pace_step_200_label),
                    reason = stringResource(R.string.pace_step_200_reason),
                    fieldLabel = stringResource(R.string.pace_time_200_label),
                    value = state.time200Input,
                    errorText = errorText,
                    imeAction = ImeAction.Done,
                    onValueChange = { onEvent(PaceTestEvent.Time200Changed(it)) },
                    onPrimary = { onEvent(PaceTestEvent.SubmitTime200) },
                    primaryLabel = stringResource(R.string.pace_save_action),
                    secondaryLabel = stringResource(R.string.pace_change_400_action),
                    onSecondary = { onEvent(PaceTestEvent.BackToTime400) },
                )

                PaceTestStep.RESULT -> ResultStep(state = state, onEvent = onEvent)
            }
        }
    }
}

/**
 * One step of the trial: its reason, its field, and the one action that moves the swimmer on.
 *
 * The format help lives in the field's supporting text and is replaced by the error when the entry
 * cannot be read, so the swimmer reads the reason to fix it in the same place they typed it.
 */
@Composable
private fun TimeStep(
    stepLabel: String,
    reason: String,
    fieldLabel: String,
    value: String,
    errorText: String?,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onPrimary: () -> Unit,
    primaryLabel: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StepHeader(label = stepLabel)
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(fieldLabel) },
            placeholder = { Text(stringResource(R.string.pace_time_hint)) },
            supportingText = {
                Text(errorText ?: stringResource(R.string.pace_time_format_help))
            },
            isError = errorText != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onPrimary() },
                onDone = { onPrimary() },
            ),
        )
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(primaryLabel)
        }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(
                onClick = onSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(secondaryLabel)
            }
        }
    }
}

/** The step's own header: the slot's mark beside the step's name. */
@Composable
private fun StepHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrandMark(
            modifier = Modifier.size(24.dp),
            contentDescription = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * The result: the threshold pace in the hero panel, the two trials as tiles, and the two ways to
 * change them.
 *
 * The hero's `Total` metric is the total time of the two trials and its `Pace /100 m` metric is the
 * derived pace, so both of the component's fixed labels are honest. The pace is deliberately both
 * the panel's headline and its per-100 m metric: they are the same number, and showing it twice is
 * the point — it is the value every later interval target is computed from.
 */
@Composable
private fun ResultStep(
    state: PaceTestUiState,
    onEvent: (PaceTestEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trials = state.resultTrials
    if (trials == null) {
        // Unreachable through the flow: the result step is only opened with a complete profile. It is
        // reported rather than rendered as a hero panel with holes in it.
        Text(
            text = stringResource(R.string.pace_result_incomplete),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    val pace = trials.thresholdPacePer100Seconds ?: return
    val time400 = trials.timeTrial400Seconds ?: return
    val time200 = trials.timeTrial200Seconds ?: return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.pace_result_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        PaceHeroPanel(
            intervalHeadline = stringResource(R.string.pace_result_hero_headline),
            strokeLabel = stringResource(
                R.string.pace_result_trials_summary,
                formatPaceTime(time400),
                formatPaceTime(time200),
            ),
            targetTime = formatPaceTime(pace),
            totalValue = formatPaceTime(time400 + time200),
            paceValue = formatPaceTime(pace),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.pace_result_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = stringResource(R.string.pace_result_trial_400_label),
                value = formatPaceTime(time400),
                icon = R.drawable.ic_timer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.pace_result_trial_200_label),
                value = formatPaceTime(time200),
                icon = R.drawable.ic_timer,
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = { onEvent(PaceTestEvent.Retry) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.pace_retry_action))
        }
        TextButton(
            onClick = { onEvent(PaceTestEvent.Reset) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.pace_reset_action))
        }
        Text(
            text = stringResource(R.string.pace_reset_supporting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The sentence for a refused entry, or null when there is nothing to report.
 *
 * A `when` over the sealed [PaceTestError] with no `else`, so a new cause cannot compile without its
 * message (`.cursor/rules/verification-discipline.mdc`, the exhaustive-`when` rule).
 */
@Composable
private fun paceErrorText(error: PaceTestError?): String? = when (error) {
    null -> null
    PaceTestError.Unreadable -> stringResource(R.string.pace_error_unreadable)
    PaceTestError.NotPositive -> stringResource(R.string.pace_error_not_positive)
    is PaceTestError.NotSlower -> stringResource(
        R.string.pace_error_400_not_slower,
        formatPaceTime(error.time400Seconds),
        formatPaceTime(error.time200Seconds),
    )
}

@Preview(name = "Pace test 400 m", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PaceTestFourHundredPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        PaceTestScreen(
            state = PaceTestUiState(time400Input = "6:4"),
            onBack = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Pace test result", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PaceTestResultPreview() {
    PlayKilnTheme(darkTheme = false, dynamicColor = false) {
        PaceTestScreen(
            state = PaceTestUiState(
                profile = PaceProfile(
                    timeTrial400Seconds = 400.0,
                    timeTrial200Seconds = 190.0,
                    thresholdPacePer100Seconds = 105.0,
                ),
                step = PaceTestStep.RESULT,
            ),
            onBack = {},
            onEvent = {},
        )
    }
}
