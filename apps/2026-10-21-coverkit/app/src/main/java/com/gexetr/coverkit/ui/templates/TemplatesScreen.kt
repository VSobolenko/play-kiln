package com.gexetr.coverkit.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.coverkit.R
import com.gexetr.coverkit.domain.CoverTemplate
import com.gexetr.coverkit.domain.TemplateFactory
import com.gexetr.coverkit.ui.components.BlankTemplateThumb
import com.gexetr.coverkit.ui.components.StudioScaffold
import com.gexetr.coverkit.ui.components.TemplateThumb
import com.gexetr.coverkit.ui.components.studioScrollBottomPadding
import com.gexetr.coverkit.ui.destinations.destinationLabel
import com.gexetr.coverkit.ui.theme.Cyan
import com.gexetr.coverkit.ui.theme.Paper

@Composable
fun TemplatesScreen(
    destinationId: String,
    unlocked: Boolean,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onNeedUnlock: () -> Unit,
    onCreateBlank: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val templates = TemplateFactory.templatesFor(destinationId)
    StudioScaffold(
        title = stringResource(R.string.templates_title_for, destinationLabel(destinationId)),
        onBack = onBack,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = studioScrollBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(
                        stringResource(R.string.templates_intro),
                        color = Paper,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.templates_intro_hint),
                        color = Cyan,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item("blank_photo") { BlankTemplateThumb(onCreateBlank) }
            items(templates, key = { it.id }) { template ->
                TemplateThumb(
                    templateName = templateDisplayName(template),
                    project = TemplateFactory.createProject(
                        templateId = template.id,
                        projectId = "preview_${template.id}",
                        nowMs = 0,
                    ),
                    locked = template.requiresUnlock && !unlocked,
                ) {
                    if (template.requiresUnlock && !unlocked) onNeedUnlock()
                    else onOpenProject(template.id)
                }
            }
        }
    }
}

@Composable
private fun templateDisplayName(template: CoverTemplate): String {
    val res = TemplateFactory.nameResOrNull(template.id)
    return if (res != null) stringResource(res) else template.name
}
