package com.gexetr.teachergradebook.ui.about

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.teachergradebook.BuildConfig
import com.gexetr.teachergradebook.R
import com.gexetr.teachergradebook.ui.components.GutterColumn
import com.gexetr.teachergradebook.ui.components.HeroPanel
import com.gexetr.teachergradebook.ui.components.Panel
import com.gexetr.teachergradebook.ui.components.ScreenShell

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onShowUnlock: () -> Unit,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    ScreenShell(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        GutterColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
            spacedBy = 16.dp,
        ) {
            HeroPanel(
                eyebrow = stringResource(R.string.about_hero_eyebrow),
                headline = stringResource(R.string.about_app_name),
                supporting = stringResource(R.string.about_hero_purpose),
            )
            Panel {
                Text(stringResource(R.string.about_what_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_what_body), style = MaterialTheme.typography.bodyMedium)
            }
            Panel {
                Text(stringResource(R.string.about_privacy_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_privacy_body), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.about_privacy_billing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Panel {
                Text(stringResource(R.string.about_averages_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_averages_body), style = MaterialTheme.typography.bodyMedium)
            }
            Panel {
                Text(stringResource(R.string.about_purchase_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (unlocked) R.string.about_purchase_unlocked else R.string.about_purchase_free),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!unlocked) {
                    TextButton(onClick = onShowUnlock, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.about_purchase_action))
                    }
                }
            }
            Panel {
                Text(stringResource(R.string.about_version_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.about_version_value, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium)
            }
            Panel {
                Text(stringResource(R.string.about_support_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.about_support_email), style = MaterialTheme.typography.bodyMedium)
            }
            Panel {
                Text(stringResource(R.string.about_leaving_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_leaving_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
