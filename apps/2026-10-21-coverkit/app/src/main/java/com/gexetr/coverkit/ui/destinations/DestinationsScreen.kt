package com.gexetr.coverkit.ui.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.coverkit.R
import com.gexetr.coverkit.domain.DestinationCatalog
import com.gexetr.coverkit.ui.components.AspectPreviewCard
import com.gexetr.coverkit.ui.components.StudioBottomBar
import com.gexetr.coverkit.ui.components.StudioScaffold
import com.gexetr.coverkit.ui.components.studioScrollBottomPadding

@Composable fun DestinationsScreen(onDestination: (String) -> Unit, onProjects: () -> Unit, onBrand: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit, hapticsEnabled: Boolean, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    StudioScaffold(title = stringResource(R.string.app_name_short), bottom = { StudioBottomBar(onProjects, onBrand, onSettings) }) {
        // Extra bottom pad on top of StudioScaffold's bar inset so the last cards
        // (Pinterest / LinkedIn) clear the chrome (owner: cannot scroll to end).
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = studioScrollBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(DestinationCatalog.all, key = { it.id }) { destination ->
                AspectPreviewCard(
                    destination = destination,
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDestination(destination.id)
                    },
                )
            }
        }
    }
}
@Composable fun destinationLabel(id: String): String { val destination = DestinationCatalog.byId(id); return if (destination != null) stringResource(destination.labelRes) else stringResource(R.string.dest_yt_thumb) }
