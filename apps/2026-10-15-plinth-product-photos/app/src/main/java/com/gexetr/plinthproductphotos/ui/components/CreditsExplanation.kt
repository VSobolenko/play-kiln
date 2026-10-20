package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R

@Composable
fun CreditsExplanation(
    onOpenWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (dismissed) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.shell_credits_explanation),
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onOpenWallet,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.shell_credits_open_wallet)) }
                TextButton(
                    onClick = { dismissed = true },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.shell_credits_dismiss)) }
            }
        }
    }
}
