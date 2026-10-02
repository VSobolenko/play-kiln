package com.gexetr.lawnroundlog.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.ui.AppearanceMode
import com.gexetr.lawnroundlog.ui.ScreenHeader

/**
 * The settings surface: language, appearance, and the four preferences the rest of the app reads.
 *
 * It is a pure function of [settings] and its callbacks — no ViewModel is created here, because the
 * appearance has to be observed by the theme host and the only holder that can serve both is the
 * activity-scoped `SettingsViewModel` that `MainActivity` already owns. A second ViewModel inside this
 * route would be a second copy of one preference: one value, one owner.
 *
 * Nothing here names a DataStore type and no row writes a file. A preference is a callback, and the
 * callback lands in the ViewModel that owns the store — which is what keeps `persistence` a property
 * of the data layer rather than of this screen.
 */
@Composable
fun SettingsScreen(
    settings: SettingsUiState,
    problem: SettingsDraftProblem?,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDefaultAmountChange: (String) -> Unit,
    onConfirmBeforeDeleteChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            // The IME is its own inset and `Scaffold` does not carry it. Two rows below are text
            // fields, and `adjustResize` is what makes the window shrink for the keyboard at all, so
            // without this the field being typed into can sit underneath it.
            .imePadding(),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
        )

        LazyColumn(
            // `weight` takes the height left below the header rather than the whole column, and the
            // navigation-bar inset ends the list viewport above the system bar. Without both, the last
            // rows — language and the two behaviour toggles — were laid out under the 48 dp three-button
            // bar and could not be read or tapped.
            modifier = Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SectionHeading(stringResource(R.string.settings_appearance_header))
            }
            item {
                AppearanceChoice(
                    selected = settings.appearance,
                    onSelect = onAppearanceChange,
                )
            }
            item {
                SupportingLine(
                    text = stringResource(R.string.settings_appearance_note),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            item {
                SectionHeading(stringResource(R.string.settings_language_header))
            }
            item {
                ActionRow(
                    title = stringResource(R.string.settings_language),
                    body = if (perAppLanguageIsAvailable()) {
                        stringResource(R.string.settings_language_summary)
                    } else {
                        stringResource(R.string.settings_language_summary_legacy)
                    },
                    onClick = { openAppLanguageSettings(context) },
                )
            }

            item {
                SectionHeading(stringResource(R.string.settings_behaviour_header))
            }
            item {
                TextPreference(
                    title = stringResource(R.string.settings_currency),
                    body = stringResource(R.string.settings_currency_summary),
                    storedValue = settings.currencySymbol,
                    commit = onCurrencyChange,
                    error = if (problem == SettingsDraftProblem.CURRENCY_TOO_LONG) {
                        stringResource(R.string.settings_currency_error)
                    } else {
                        null
                    },
                )
            }
            item {
                TextPreference(
                    title = stringResource(R.string.settings_default_amount),
                    body = stringResource(R.string.settings_default_amount_summary),
                    storedValue = formatAmount(settings.defaultVisitAmountUsd),
                    commit = onDefaultAmountChange,
                    error = if (problem == SettingsDraftProblem.AMOUNT_NOT_A_NUMBER) {
                        stringResource(R.string.settings_amount_error)
                    } else {
                        null
                    },
                    keyboardType = KeyboardType.Decimal,
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.settings_confirm_delete),
                    body = stringResource(R.string.settings_confirm_delete_summary),
                    checked = settings.confirmBeforeDelete,
                    onCheckedChange = onConfirmBeforeDeleteChange,
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.settings_haptics),
                    body = stringResource(R.string.settings_haptics_summary),
                    checked = settings.hapticsEnabled,
                    onCheckedChange = onHapticsChange,
                )
            }
        }
    }
}

/**
 * The appearance choice, as three radio rows rather than a segmented control.
 *
 * A segmented button divides its width between its segments, and these labels are one to three words
 * in every language this slot ships; at a large font scale that is exactly the container that cannot
 * grow, and the fix would be to shrink the type to fit — which is the defect, not the fix. A stacked
 * row per option wraps, keeps its 48 dp target, and reads the same to a screen reader.
 */
@Composable
private fun AppearanceChoice(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        for (mode in AppearanceMode.entries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == mode,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) },
                    )
                    .heightIn(min = 48.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(
                    selected = selected == mode,
                    // The row carries the click and the name. A second target for one choice is a
                    // second thing for TalkBack to announce and a second way to get it wrong.
                    onClick = null,
                )
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/** A row that opens something else, with a body line saying what it opens. */
@Composable
private fun ActionRow(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                SupportingLine(body)
            }
        }
        HorizontalDivider()
    }
}

/**
 * A preference whose value is typed, with the message the ViewModel rejected an edit with.
 *
 * The field keeps a local draft and commits it when it loses focus or the IME action fires, rather
 * than writing on every keystroke. That is not an optimisation: the value round-trips through
 * DataStore, so a write per character means the field is re-set from a stored value that is one
 * keystroke behind and a fast typist watches characters disappear.
 *
 * The draft is replaced only when the *stored* value actually changes, which is what keeps a rejected
 * edit on screen: the operator sees the text he typed and the reason it was not accepted, instead of
 * a field that silently reverts to the old value under the error message.
 */
@Composable
private fun TextPreference(
    title: String,
    body: String,
    storedValue: String,
    commit: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var draft by rememberSaveable { mutableStateOf(storedValue) }
    var lastStored by rememberSaveable { mutableStateOf(storedValue) }

    LaunchedEffect(storedValue) {
        if (storedValue != lastStored) {
            lastStored = storedValue
            draft = storedValue
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SupportingLine(body)
        OutlinedTextField(
            value = draft,
            onValueChange = { typed ->
                draft = typed
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    // Committing on focus loss rather than per keystroke: leaving the field, opening
                    // another row, or closing the keyboard all commit exactly once. The equality test
                    // is what keeps the first composition — which reports an unfocused field — from
                    // writing the fallback value over the stored one.
                    if (!focus.isFocused && draft != lastStored) commit(draft)
                },
            label = { Text(title) },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { commit(draft) },
            ),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** A preference that is on or off, where the whole row is the target rather than only the switch. */
@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onCheckedChange(!checked) })
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // `weight` rather than `fillMaxWidth`: the label column has to leave the switch its own
            // width and take whatever is left, in that order.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                SupportingLine(body)
            }
            Switch(
                checked = checked,
                // A switch is state, not a second action: the row already announces and toggles it.
                onCheckedChange = onCheckedChange,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SupportingLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** `12.5` rather than `12.50`: this is a number the operator edits, not a price on a receipt. */
private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

/**
 * Whether the platform offers the per-app language picker.
 *
 * The picker arrived with Android 13, alongside the `android:localeConfig` this slot declares. Below
 * it the row says which screen it actually opens rather than promising one that does not exist.
 */
private fun perAppLanguageIsAvailable(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Opens Android's own language picker for this app.
 *
 * Deliberately the system picker and not a second in-app one: `android:localeConfig` already tells
 * Android which languages this app ships, so a picker built here would be a second source of truth
 * that can disagree with the one the system, the launcher and Play all read.
 *
 * The `runCatching` is not decoration. A device whose Settings app is disabled or absent — a stripped
 * emulator image, a managed profile that blocks the intent — throws `ActivityNotFoundException`, and
 * a settings row that crashes the app is worse than a row that does nothing.
 */
fun openAppLanguageSettings(context: Context) {
    val packageUri = Uri.fromParts(PACKAGE_URI_SCHEME, context.packageName, null)
    val intent = if (perAppLanguageIsAvailable()) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(packageUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri)
    }
    runCatching { context.startActivity(intent) }
}

/** The scheme of a `package:` URI. Named so the literal is not repeated at the call site. */
private const val PACKAGE_URI_SCHEME: String = "package"
