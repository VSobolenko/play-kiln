package com.gexetr.lawnroundlog.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.domain.Customer
import com.gexetr.lawnroundlog.domain.Payment
import com.gexetr.lawnroundlog.ui.ScreenHeader
import com.gexetr.lawnroundlog.ui.components.BalanceLine
import com.gexetr.lawnroundlog.ui.components.GlyphDelete
import com.gexetr.lawnroundlog.ui.components.GlyphLock
import com.gexetr.lawnroundlog.ui.round.FREE_TIER_CUSTOMER_LIMIT
import com.gexetr.lawnroundlog.ui.round.RoundViewModel
import com.gexetr.lawnroundlog.ui.round.formatAmount
import com.gexetr.lawnroundlog.ui.settings.SettingsUiState

/**
 * One customer: who they are, what they owe, what they have paid, and how to remove them.
 *
 * Adding and editing share one form, because the fields are identical and a separate "add" flow would
 * be a second place for the validation to drift. What differs is only which doors are open: balance,
 * payments and delete appear once there is something to balance against.
 */
@Composable
fun CustomerRoute(
    round: RoundViewModel,
    customerId: String?,
    settings: SettingsUiState,
    unlocked: Boolean,
    onBack: () -> Unit,
    onRequestUnlock: () -> Unit,
    onSave: (Customer, Boolean) -> Unit,
) {
    val state by round.state.collectAsStateWithLifecycle()
    val editor: CustomerViewModel = viewModel()
    val editorState by editor.editor.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val existing = customerId?.let { id -> state.customers.firstOrNull { it.id == id } }

    // Re-seeded whenever the *identity* changes, never whenever the record does: recording a payment
    // here rewrites that customer inside the record, and re-seeding on that would erase what he is
    // typing. The id is therefore the key, not the object.
    LaunchedEffect(customerId, existing?.id, settings.defaultVisitAmountUsd) {
        when {
            existing != null -> editor.startEdit(existing)
            customerId == null -> editor.startNew(settings.defaultVisitAmountUsd)
        }
    }

    val buzz: () -> Unit = {
        if (settings.hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var paymentText by rememberSaveable { mutableStateOf("") }

    val balance = existing?.let { state.balances[it.id] } ?: 0.0
    val atFreeLimit = existing == null &&
        !unlocked &&
        state.customers.count { it.active } >= FREE_TIER_CUSTOMER_LIMIT

    val commit: () -> Unit = {
        val customer = editor.validatedCustomer()
        if (customer != null) {
            buzz()
            onSave(customer, editorState.isNew)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .imePadding(),
    ) {
        ScreenHeader(
            title = existing?.name ?: stringResource(R.string.customer_new_title),
            subtitle = existing?.phone?.takeIf { it.isNotBlank() },
            onBack = onBack,
            actions = {
                TextButton(onClick = commit) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (existing != null) {
                BalanceLine(
                    name = stringResource(R.string.customer_balance),
                    amountUsd = balance,
                    currencySymbol = settings.currencySymbol,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (atFreeLimit) {
                FreeTierNotice(
                    limit = FREE_TIER_CUSTOMER_LIMIT,
                    onUnlock = onRequestUnlock,
                )
            }

            SectionCard(title = stringResource(R.string.customer_details_title)) {
                OutlinedTextField(
                    value = editorState.name,
                    onValueChange = editor::setName,
                    label = { Text(stringResource(R.string.customer_name)) },
                    isError = editorState.nameInvalid,
                    supportingText = if (editorState.nameInvalid) {
                        { Text(stringResource(R.string.customer_name_required)) }
                    } else {
                        null
                    },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editorState.address,
                    onValueChange = editor::setAddress,
                    label = { Text(stringResource(R.string.customer_address)) },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editorState.phone,
                    onValueChange = editor::setPhone,
                    label = { Text(stringResource(R.string.customer_phone)) },
                    singleLine = false,
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editorState.notes,
                    onValueChange = editor::setNotes,
                    label = { Text(stringResource(R.string.customer_notes)) },
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editorState.rateText,
                    onValueChange = editor::setRate,
                    label = { Text(stringResource(R.string.customer_rate)) },
                    prefix = { Text(settings.currencySymbol) },
                    isError = editorState.rateInvalid,
                    supportingText = if (editorState.rateInvalid) {
                        { Text(stringResource(R.string.customer_rate_invalid)) }
                    } else {
                        { Text(stringResource(R.string.customer_rate_hint)) }
                    },
                    singleLine = false,
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (existing != null) {
                PaymentSection(
                    payments = state.record.payments.filter { it.customerId == existing.id },
                    paymentText = paymentText,
                    onPaymentTextChange = { input ->
                        val normalized = input.replace(',', '.')
                        if (normalized.isEmpty() || normalized.all { it.isDigit() || it == '.' }) {
                            paymentText = normalized
                        }
                    },
                    onRecord = {
                        val amount = paymentText.toDoubleOrNull() ?: 0.0
                        buzz()
                        round.recordPayment(existing.id, amount)
                        paymentText = ""
                    },
                    onRemove = { payment ->
                        buzz()
                        round.removePayment(payment)
                    },
                    currencySymbol = settings.currencySymbol,
                )

                OutlinedButton(
                    onClick = {
                        if (settings.confirmBeforeDelete) {
                            confirmDelete = true
                        } else {
                            buzz()
                            round.deleteCustomer(existing.id)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    GlyphDelete()
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_delete))
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        // The name is in the sentence: "Delete this customer?" under a thumb that has scrolled is a
        // question nobody can answer, and the answer here is destructive and unrecoverable.
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, existing.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        buzz()
                        round.deleteCustomer(existing.id)
                        onBack()
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The balance, the one field, and every payment already recorded against it. */
@Composable
private fun PaymentSection(
    payments: List<Payment>,
    paymentText: String,
    onPaymentTextChange: (String) -> Unit,
    onRecord: () -> Unit,
    onRemove: (Payment) -> Unit,
    currencySymbol: String,
) {
    val ordered = remember(payments) { payments.sortedByDescending { it.dateIso } }

    SectionCard(title = stringResource(R.string.payment_section_title)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = paymentText,
                onValueChange = onPaymentTextChange,
                label = { Text(stringResource(R.string.payment_amount)) },
                prefix = { Text(currencySymbol) },
                singleLine = false,
                maxLines = 2,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onRecord() }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onRecord,
                enabled = paymentText.toDoubleOrNull() != null,
            ) {
                Text(text = stringResource(R.string.payment_add))
            }
        }

        if (ordered.isEmpty()) {
            Text(
                text = stringResource(R.string.payment_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ordered.forEach { payment ->
                PaymentRow(
                    payment = payment,
                    currencySymbol = currencySymbol,
                    onRemove = { onRemove(payment) },
                )
            }
        }
    }
}

/** One recorded payment, and the one thing an operator ever needs to do to it. */
@Composable
private fun PaymentRow(
    payment: Payment,
    currencySymbol: String,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatAmount(currencySymbol, payment.amountUsd),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = payment.dateIso,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(48.dp),
            ) {
                val removeLabel = stringResource(R.string.payment_remove)
                GlyphDelete(
                    modifier = Modifier.semantics { contentDescription = removeLabel },
                )
            }
        }
    }
}

/**
 * The free tier's own explanation, shown exactly where it becomes true.
 *
 * A sentence and a door, rather than a locked Save button: the customer the operator has already typed
 * stays in the draft, so unlocking from here returns him to a filled form instead of a blank one.
 */
@Composable
private fun FreeTierNotice(
    limit: Int,
    onUnlock: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphLock(
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.free_tier_limit_message, limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            TextButton(onClick = onUnlock) {
                Text(text = stringResource(R.string.unlock_action))
            }
        }
    }
}

/** A titled group of fields, so the form reads as three decisions rather than eleven boxes. */
@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}
