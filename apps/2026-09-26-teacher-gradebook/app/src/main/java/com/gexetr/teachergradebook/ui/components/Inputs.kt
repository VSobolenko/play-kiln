package com.gexetr.teachergradebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.gexetr.teachergradebook.R
import java.time.LocalDate

/** The one date format the app stores and shows: ISO, so a stored record sorts without a locale. */
fun todayIso(): String = LocalDate.now().toString()

/**
 * A dialog with one or more labelled text fields.
 *
 * Written once rather than per screen because the app has a dozen "name this thing" moments and
 * each one needs the same three behaviours: the confirm button stays disabled while the required
 * text is blank, the value is trimmed before it leaves, and a refusal from the domain is shown
 * without the dialog inventing its own validation rules.
 */
@Composable
fun TextInputDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    label: String = "",
    initial: String = "",
    supporting: String? = null,
    numeric: Boolean = false,
) {
    var value by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label.ifBlank { stringResource(R.string.common_name) }) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                )
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/** A dialog with two or three fields, for the records that carry a name and a number together. */
@Composable
fun TwoFieldDialog(
    title: String,
    confirmLabel: String,
    firstLabel: String,
    initialFirst: String,
    secondLabel: String,
    initialSecond: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    secondNumeric: Boolean = false,
    secondSupporting: String? = null,
) {
    var first by remember { mutableStateOf(initialFirst) }
    var second by remember { mutableStateOf(initialSecond) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it },
                    label = { Text(firstLabel) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                )
                OutlinedTextField(
                    value = second,
                    onValueChange = { second = it },
                    label = { Text(secondLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (secondNumeric) KeyboardType.Decimal else KeyboardType.Text,
                    ),
                    isError = second.isNotBlank() && second.toDoubleOrNull() == null && secondNumeric,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                )
                secondSupporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(first.trim(), second.trim()) },
                enabled = first.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/**
 * A dialog for a record with more than two fields.
 *
 * The field list is data rather than a bespoke composable per screen because the app has five
 * record types that differ only in their fields, and a hand-written dialog per type is where the
 * trimming, the numeric keyboard and the disabled-until-valid rule drift apart.
 */
data class FieldSpec(
    val key: String,
    val label: String,
    val initial: String = "",
    val numeric: Boolean = false,
    val supporting: String? = null,
)

@Composable
fun MultiFieldDialog(
    title: String,
    confirmLabel: String,
    fields: List<FieldSpec>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    // Compose 1.12 renamed `toMutableStateMapOf` to `toMutableStateMap`, and the extension now takes
    // an Iterable<Pair>, not a Map — so the pairs are built explicitly rather than via associate{}.
    val values = remember(fields) {
        fields.map { it.key to it.initial }.toMutableStateMap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.forEach { field ->
                    OutlinedTextField(
                        value = values[field.key].orEmpty(),
                        onValueChange = { values[field.key] = it },
                        label = { Text(field.label) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (field.numeric) KeyboardType.Decimal else KeyboardType.Text,
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    )
                    field.supporting?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(values.mapValues { (_, value) -> value.trim() })
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/**
 * The confirmation before something irreversible.
 *
 * It names what is being removed rather than asking "are you sure?", because the teacher is the one
 * who knows whether this is the class they meant, and "are you sure?" cannot help them decide.
 */
@Composable
fun DestructiveConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_keep_it)) }
        },
    )
}

/** A dated record field: the date as ISO text plus a one-tap "today", so entry is not fiddly. */
@Composable
fun IsoDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label.ifBlank { stringResource(R.string.common_date) }) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.common_date_placeholder)) },
            shape = MaterialTheme.shapes.medium,
            isError = value.isNotBlank() && !isIsoDate(value),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onValueChange(todayIso()) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.common_today))
        }
    }
}

/** A stored date is plain ISO; anything else is refused rather than parsed loosely. */
fun isIsoDate(text: String): Boolean = runCatching { LocalDate.parse(text) }.isSuccess

/** A block with a heading, used for every "one section of a screen" in the app. */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            title?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

/** Shown where a list is legitimately empty, and it says what to do rather than only that it is empty. */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 12.dp),
    )
}

/** A label/value pair, so a detail is never printed as a bare unlabelled number. */
@Composable
fun LabelledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
