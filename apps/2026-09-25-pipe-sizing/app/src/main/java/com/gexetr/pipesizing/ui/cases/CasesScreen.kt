package com.gexetr.pipesizing.ui.cases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gexetr.pipesizing.R
import java.text.DateFormat
import java.util.Date

/**
 * The saved-case library.
 *
 * Content only, like every other screen: the app's Scaffold owns the bars. Loading a case replaces
 * the calculator's working case, which is why the load button is the one that navigates.
 */
@Composable
fun CasesScreen(
    cases: List<SavedCase>,
    isPro: Boolean,
    freeCaseLimit: Int,
    onLoad: (SavedCase) -> Unit,
    onDelete: (SavedCase) -> Unit,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.cases_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.cases_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isPro) {
                        stringResource(R.string.cases_unlimited, cases.size)
                    } else {
                        stringResource(R.string.cases_saved_count, cases.size, freeCaseLimit)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                if (!isPro) {
                    LinearProgressIndicator(
                        progress = { (cases.size.toFloat() / freeCaseLimit).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (cases.isEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.cases_empty), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.cases_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(items = cases, key = { it.id }) { saved ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = saved.name.ifBlank { context.getString(R.string.case_unnamed) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = caseSummary(saved),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = DateFormat.getDateTimeInstance().format(Date(saved.savedAtEpochMillis)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onLoad(saved) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.cases_load))
                            }
                            TextButton(onClick = { onDelete(saved) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.cases_delete))
                            }
                        }
                    }
                }
            }
        }

        if (!isPro && cases.size >= freeCaseLimit) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.cases_limit_reached, freeCaseLimit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        TextButton(onClick = onOpenPaywall, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.paywall_title))
                        }
                    }
                }
            }
        }
    }
}

/** One readable line for the list, from the same keys the calculator stores. */
internal fun caseSummary(saved: SavedCase): String {
    val material = saved.fields["material"].orEmpty()
    val pressure = saved.fields["pressure"].orEmpty()
    val load = saved.fields["load"].orEmpty()
    return listOf(material, pressure, load).filter { it.isNotBlank() }.joinToString(" · ")
}
