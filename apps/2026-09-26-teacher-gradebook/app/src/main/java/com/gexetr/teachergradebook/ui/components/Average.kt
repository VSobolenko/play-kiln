package com.gexetr.teachergradebook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.domain.GradeMath
import com.gexetr.teachergradebook.ui.roundingRuleText
import com.gexetr.teachergradebook.domain.model.GradeBand
import com.gexetr.teachergradebook.domain.model.GradeCell
import com.gexetr.teachergradebook.domain.model.GradeKind
import com.gexetr.teachergradebook.domain.model.RoundingRule
import java.util.Locale

/**
 * One average, with the rule that produced it and the categories that were left out.
 *
 * A gradebook's number is only trustworthy if the teacher can explain it, and the incumbents'
 * complaints are full of averages nobody could explain — a category silently counted as zero, a
 * weight that did not apply. So the number is never shown alone: the rounding rule sits beside it,
 * and every excluded category is named.
 */
@Composable
fun AverageBlock(
    overall: Double?,
    rounding: RoundingRule,
    categories: List<GradeMath.CategoryOutcome>,
    scale: List<GradeBand>,
    missingCountsAsZero: Boolean,
    fellBackToUnweighted: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val decimals = rounding.decimals.coerceIn(0, 2)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (overall == null) {
            Panel {
                Text(stringResource(R.string.average_not_enough_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.average_not_enough_body), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            val letter = GradeMath.letterFor(overall, scale)
            GradeDisplay(
                percentText = stringResource(
                    R.string.average_percent_value,
                    String.format(Locale.US, "%.${decimals}f", overall),
                ),
                letter = letter,
                caption = stringResource(R.string.average_overall_caption),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    if (missingCountsAsZero) R.string.average_rule_missing_zero
                    else R.string.average_rule_missing_excluded,
                    roundingRuleText(rounding),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (fellBackToUnweighted) {
                Text(stringResource(R.string.average_rule_unweighted), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.average_meaning), style = MaterialTheme.typography.bodyMedium)
        }

        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(if (expanded) R.string.average_hide_working else R.string.average_show_working))
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (categories.isEmpty()) {
                    EmptyHint(stringResource(R.string.average_no_categories))
                }
                categories.forEach { outcome ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val categoryLabel = stringResource(
                            R.string.average_category_weight,
                            outcome.categoryName,
                            trim(outcome.weight),
                        )
                        if (outcome.average == null) {
                            Text(categoryLabel, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.average_category_excluded),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            StatTile(
                                label = categoryLabel,
                                value = stringResource(
                                    R.string.average_percent_value,
                                    String.format(Locale.US, "%.${decimals}f", outcome.average),
                                ),
                            )
                            Text(
                                pluralStringResource(
                                    R.plurals.average_category_grade_count,
                                    outcome.usedCount,
                                    String.format(Locale.US, "%.${decimals}f", outcome.average),
                                    outcome.usedCount,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (outcome.droppedCount > 0) Text(
                            pluralStringResource(R.plurals.average_dropped_count, outcome.droppedCount, outcome.droppedCount),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (outcome.missingCount > 0) Text(
                            pluralStringResource(R.plurals.average_missing_count, outcome.missingCount, outcome.missingCount),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (outcome.exemptCount > 0) Text(
                            pluralStringResource(R.plurals.average_exempt_count, outcome.exemptCount, outcome.exemptCount),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/** A compact cell label: the same states everywhere, so a grid is readable at a glance. */
@Composable
fun GradeCellLabel(cell: GradeCell?, assignmentPoints: Double, modifier: Modifier = Modifier) {
    val text = when (cell?.kind) {
        GradeKind.SCORE -> stringResource(R.string.average_score_value, trim(cell.points), trim(assignmentPoints))
        GradeKind.MISSING -> stringResource(R.string.average_missing)
        GradeKind.EXEMPT -> stringResource(R.string.average_exempt)
        null -> stringResource(R.string.average_empty_mark)
    }
    val description = when (cell?.kind) {
        GradeKind.SCORE -> stringResource(R.string.average_score_description, trim(cell.points), trim(assignmentPoints))
        GradeKind.MISSING -> stringResource(R.string.average_missing_description)
        GradeKind.EXEMPT -> stringResource(R.string.average_exempt_description)
        null -> stringResource(R.string.average_empty_description)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.semantics { contentDescription = description },
    )
}

/**
 * The three choices a cell can hold, presented as three choices.
 *
 * `Missing` and `Exempt` are not "zero" and not "empty", and the incumbents' reviews are full of
 * teachers whose zeroes came from a blank cell. So the app asks which of the three it is, and an
 * empty cell stays empty until the teacher says otherwise.
 */
@Composable
fun GradeEntryDialog(
    assignmentName: String,
    assignmentPoints: Double,
    existing: GradeCell?,
    onDismiss: () -> Unit,
    onSet: (GradeKind, Double) -> Unit,
    onClear: () -> Unit,
) {
    var kind by remember { mutableStateOf(existing?.kind ?: GradeKind.SCORE) }
    var points by remember {
        mutableStateOf(
            if (existing?.kind == GradeKind.SCORE) trim(existing.points) else "",
        )
    }
    val parsed = points.toDoubleOrNull()
    val outOfRange = kind == GradeKind.SCORE && parsed != null && parsed > assignmentPoints
    val invalid = kind == GradeKind.SCORE && (parsed == null || parsed < 0.0 || outOfRange)
    val recordScoreDescription = stringResource(R.string.average_record_score_description)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(assignmentName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.average_assignment_points, trim(assignmentPoints)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == GradeKind.SCORE,
                        onClick = { kind = GradeKind.SCORE },
                        label = { Text(stringResource(R.string.average_score)) },
                        modifier = Modifier.semantics { contentDescription = recordScoreDescription },
                    )
                    FilterChip(
                        selected = kind == GradeKind.MISSING,
                        onClick = { kind = GradeKind.MISSING },
                        label = { Text(stringResource(R.string.average_missing)) },
                    )
                    FilterChip(
                        selected = kind == GradeKind.EXEMPT,
                        onClick = { kind = GradeKind.EXEMPT },
                        label = { Text(stringResource(R.string.average_exempt)) },
                    )
                }
                if (kind == GradeKind.SCORE) {
                    OutlinedTextField(
                        value = points,
                        onValueChange = { points = it },
                        label = { Text(stringResource(R.string.average_points_earned)) },
                        singleLine = true,
                        isError = invalid && points.isNotBlank(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (outOfRange) {
                        Text(
                            text = stringResource(R.string.average_points_too_high, trim(assignmentPoints)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(
                        text = if (kind == GradeKind.MISSING) {
                            stringResource(R.string.average_missing_rule_help)
                        } else {
                            stringResource(R.string.average_exempt_rule_help)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (kind == GradeKind.SCORE) onSet(kind, parsed ?: 0.0) else onSet(kind, 0.0)
                },
                enabled = !invalid,
                modifier = Modifier.size(width = 96.dp, height = 48.dp),
            ) { Text(stringResource(R.string.average_save)) }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.average_clear)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.average_cancel)) }
            }
        },
    )
}

/** `7.0` prints as `7`, and `7.25` keeps its decimals: how a teacher writes a mark. */
fun trim(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
