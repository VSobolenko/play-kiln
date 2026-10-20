package com.gexetr.plinthproductphotos.ui.home

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.components.AuroraField
import com.gexetr.plinthproductphotos.ui.components.PlinthStage

@Composable
fun MoreScreen(
    onOpenWallet: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoreHero()
        MoreGroup(stringResource(R.string.more_group_account)) {
            MoreAction(stringResource(R.string.shell_more_wallet), stringResource(R.string.shell_more_wallet_summary), R.drawable.ic_lucide_plus, onOpenWallet)
        }
        MoreGroup(stringResource(R.string.more_group_preferences)) {
            MoreAction(stringResource(R.string.plinth_settings_title), stringResource(R.string.shell_more_settings_summary), R.drawable.ic_lucide_settings, onOpenSettings)
        }
        MoreGroup(stringResource(R.string.more_group_about_legal)) {
            MoreAction(stringResource(R.string.about_title), stringResource(R.string.shell_more_about_summary), R.drawable.ic_lucide_info, onOpenAbout)
        }
    }
}

@Composable
private fun MoreHero() {
    Box {
        AuroraField(Modifier.matchParentSize(), intensity = 0.35f)
        PlinthStage(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoreGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun MoreAction(
    title: String,
    summary: String,
    icon: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 88.dp).padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(
                    painter = painterResource(icon), contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(24.dp), tint = PlinthTheme.accentInk,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_lucide_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
