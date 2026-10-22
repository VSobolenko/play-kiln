package com.gexetr.coverkit.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gexetr.coverkit.R
import com.gexetr.coverkit.data.ProjectStore
import com.gexetr.coverkit.domain.CoverProject
import com.gexetr.coverkit.domain.DestinationCatalog
import com.gexetr.coverkit.ui.components.ProjectThumb
import com.gexetr.coverkit.ui.components.StudioScaffold
import com.gexetr.coverkit.ui.theme.Cyan
import com.gexetr.coverkit.ui.theme.InkSoft
import com.gexetr.coverkit.ui.theme.Paper
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun ProjectsScreen(
    projectStore: ProjectStore,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val projects by projectStore.projects.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<CoverProject?>(null) }
    LaunchedEffect(Unit) { projectStore.refresh() }

    StudioScaffold(title = stringResource(R.string.nav_projects), onBack = onBack) {
        if (projects.isEmpty()) {
            Column(
                modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "✦",
                    style = MaterialTheme.typography.displayLarge,
                    color = Cyan,
                )
                Text(
                    stringResource(R.string.projects_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                )
                Text(
                    stringResource(R.string.projects_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper,
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(InkSoft)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.projects_step_1), color = Paper)
                    Text(stringResource(R.string.projects_step_2), color = Paper)
                    Text(stringResource(R.string.projects_step_3), color = Paper)
                }
                Text(
                    stringResource(R.string.projects_empty_cta),
                    style = MaterialTheme.typography.labelLarge,
                    color = Cyan,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.projects_list_header),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Paper.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    )
                }
                items(projects, key = { it.id }) { project ->
                    val dest = DestinationCatalog.byId(project.destinationId)
                    val destLabel = dest?.let { stringResource(it.labelRes) }
                        ?: project.destinationId
                    val updated = if (project.updatedAtMs > 0L) {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(project.updatedAtMs))
                    } else {
                        null
                    }
                    ProjectThumb(
                        project = project,
                        destinationLabel = destLabel,
                        updatedLabel = updated,
                        onOpen = { onOpen(project.id) },
                        onActions = { selected = project },
                    )
                }
            }
        }

        selected?.let { project ->
            AlertDialog(
                onDismissRequest = { selected = null },
                title = { Text(project.name) },
                text = { Text(stringResource(R.string.projects_actions_hint)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                projectStore.duplicate(project.id)?.let { onOpen(it.id) }
                            }
                            selected = null
                        },
                    ) {
                        Text(stringResource(R.string.projects_duplicate), color = Cyan)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scope.launch { projectStore.delete(project.id) }
                            selected = null
                        },
                    ) {
                        Text(stringResource(R.string.projects_delete), color = Paper)
                    }
                },
            )
        }
    }
}
