package com.gexetr.lumintralightplanner.ui.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.ScoutSelection
import com.gexetr.lumintralightplanner.ui.components.LumintraWordmark
import com.gexetr.lumintralightplanner.ui.format.formatSignedDegrees
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.PlayKilnTheme
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// =================================================================================================
// Welcome — the screen the app opens on while the spot store holds no record
// =================================================================================================

data class WelcomeUiState(
    val occupied: Boolean = false,
    val spotCount: Int = 0,
    val activeSpotName: String? = null,
    val activeSpotCoordinate: GeodeticPoint? = null,
    val deviceZoneId: String = ZoneId.systemDefault().id,
    val storeUnreadable: Boolean = false,
)

@Composable
fun WelcomeScreen(
    state: WelcomeUiState,
    onCreateSpot: () -> Unit,
    onSkip: () -> Unit,
    onManualEntry: () -> Unit,
) {
    val lumintra = LumintraTheme.colors
    val primaryDescription = stringResource(R.string.welcome_action_content_description)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LumintraWordmark()

        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!state.occupied) {
            Text(
                text = stringResource(R.string.welcome_compass_line),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.occupied) {
            Text(
                text = stringResource(
                    R.string.welcome_have_spot,
                    state.activeSpotName ?: stringResource(R.string.spot_title),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            state.activeSpotCoordinate?.let { point ->
                Text(
                    text = stringResource(
                        R.string.coord_decimal_pair,
                        formatSignedDegrees(point.latDeg),
                        formatSignedDegrees(point.lonDeg),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.storeUnreadable) {
            Text(
                text = stringResource(R.string.spot_store_quarantined),
                style = MaterialTheme.typography.bodySmall,
                color = lumintra.caution,
            )
        }

        Button(
            onClick = onCreateSpot,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = primaryDescription },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_map_pin),
                contentDescription = null,
            )
            Text(
                text = stringResource(
                    if (state.occupied) R.string.welcome_action_add_another else R.string.welcome_action_set_spot,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        TextButton(
            onClick = onManualEntry,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_action_manual),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.welcome_action_skip))
        }
    }
}

class WelcomeViewModel(
    store: ScoutStore,
    private val zoneId: String = ZoneId.systemDefault().id,
) : ViewModel() {

    val state: StateFlow<WelcomeUiState> = store.snapshot
        .map { snapshot ->
            val active = ScoutSelection.resolve(snapshot.spots, snapshot.activeSpotId)
            WelcomeUiState(
                occupied = snapshot.spots.isNotEmpty(),
                spotCount = snapshot.spots.size,
                activeSpotName = active?.name,
                activeSpotCoordinate = active?.point,
                deviceZoneId = zoneId,
                storeUnreadable = snapshot.quarantinedCount > 0,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = WelcomeUiState(deviceZoneId = zoneId),
        )

    companion object {
        fun factory(store: ScoutStore, zoneId: String = ZoneId.systemDefault().id): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WelcomeViewModel(store, zoneId) as T
            }
    }
}

@Preview(name = "Welcome — first run", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun WelcomeFirstRunPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        WelcomeScreen(
            state = WelcomeUiState(deviceZoneId = "Europe/Oslo"),
            onCreateSpot = {},
            onSkip = {},
            onManualEntry = {},
        )
    }
}

@Preview(name = "Welcome — spot already saved", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun WelcomeOccupiedPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        WelcomeScreen(
            state = WelcomeUiState(
                occupied = true,
                spotCount = 2,
                activeSpotName = "Fjellstua",
                activeSpotCoordinate = GeodeticPoint(59.9139, 10.7522),
            ),
            onCreateSpot = {},
            onSkip = {},
            onManualEntry = {},
        )
    }
}
