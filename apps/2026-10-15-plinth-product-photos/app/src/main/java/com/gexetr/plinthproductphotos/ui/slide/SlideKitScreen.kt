package com.gexetr.plinthproductphotos.ui.slide

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.data.slide.SlidePixelRenderer
import com.gexetr.plinthproductphotos.data.slide.SlideProhibitedTextCatalog
import com.gexetr.plinthproductphotos.data.slide.SlideTemplateCatalog
import com.gexetr.plinthproductphotos.data.slide.SlideTemplateDefinition
import com.gexetr.plinthproductphotos.data.slide.SlideTextLayoutException
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.slide.SlideDraft
import com.gexetr.plinthproductphotos.domain.slide.SlideLayoutEngine
import com.gexetr.plinthproductphotos.domain.slide.SlideMarketplaceRules
import com.gexetr.plinthproductphotos.domain.slide.SlideRenderPlan
import com.gexetr.plinthproductphotos.domain.slide.SlideSeriesBackground
import com.gexetr.plinthproductphotos.domain.slide.SlideStyle
import com.gexetr.plinthproductphotos.domain.slide.SlideTemplate
import com.gexetr.plinthproductphotos.domain.slide.SlideTextSafety
import com.gexetr.plinthproductphotos.domain.slide.SlideTextSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlinx.serialization.SerializationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideKitScreen(
    items: List<PixelBuffer>,
    background: SlideSeriesBackground?,
    style: SlideStyle,
    marketplaceRules: SlideMarketplaceRules,
    onRenderAndExport: (List<PixelBuffer>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val catalog = remember(context) { loadCatalog(context) }
    val effectiveRules = remember(marketplaceRules, catalog) {
        val assetPhrases = (catalog as? CatalogState.Loaded)
            ?.prohibitedText
            ?.phrasesFor(marketplaceRules.languageTag)
            .orEmpty()
        val categories = assetPhrases.keys + marketplaceRules.phraseRules.keys
        marketplaceRules.copy(
            phraseRules = categories.associateWith { category ->
                (assetPhrases[category].orEmpty() + marketplaceRules.phraseRules[category].orEmpty()).distinct()
            },
        )
    }
    var drafts by remember {
        mutableStateOf(listOf(SlideDraft(SlideTemplate.HERO, emptyMap())))
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentDraft = drafts.getOrNull(selectedIndex) ?: drafts.first()
    val checks = remember(drafts, effectiveRules) {
        drafts.map { SlideTextSafety.check(it.texts, effectiveRules) }
    }
    val allTextSafe = checks.all { it.canRender }
    val renderable = items.isNotEmpty() && background != null && allTextSafe
    val preview by produceState(
        PreviewResult(emptyList(), emptyList(), null, null, false),
        drafts,
        items,
        background,
        style,
        effectiveRules,
        renderable,
    ) {
        if (!renderable || background == null) {
            value = PreviewResult(emptyList(), emptyList(), null, null, false)
        } else {
            value = PreviewResult(emptyList(), emptyList(), null, null, true)
            value = withContext(Dispatchers.Default) {
                try {
                    val plans = SlideLayoutEngine.renderSeries(
                        slides = drafts,
                        items = items,
                        background = background,
                        style = style,
                        marketplaceRules = effectiveRules,
                    )
                    PreviewResult(
                        plans = plans,
                        pixels = plans.map(SlidePixelRenderer::render),
                        error = null,
                        renderError = null,
                        isLoading = false,
                    )
                } catch (error: SlideTextLayoutException) {
                    PreviewResult(emptyList(), emptyList(), error, null, false)
                } catch (error: IllegalArgumentException) {
                    PreviewResult(emptyList(), emptyList(), null, error.message ?: "invalid slide layout", false)
                }
            }
        }
    }
    val bitmaps = remember(preview.pixels) { preview.pixels.map { it.toBitmap() } }
    DisposableEffect(bitmaps) {
        onDispose {
            bitmaps.forEach { it.recycle() }
        }
    }
    val templateListState = rememberLazyListState()
    val seriesListState = rememberLazyListState()
    val templates = (catalog as? CatalogState.Loaded)?.templates.orEmpty()
    LaunchedEffect(currentDraft.template, templates) {
        val index = templates.indexOfFirst { it.template == currentDraft.template }
        if (index >= 0) templateListState.animateScrollToItem(index)
    }
    LaunchedEffect(selectedIndex, bitmaps.size) {
        if (selectedIndex in bitmaps.indices) seriesListState.animateScrollToItem(selectedIndex)
    }
    val sampleTexts = SlideTextSlot.entries.associateWith { stringResource(slotLabel(it)) }
    val templateCardWidth = (144 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val seriesCardWidth = (216 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp

    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.slide_kit_title)) })
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (catalog) {
                is CatalogState.Failed -> {
                    Text(
                        stringResource(R.string.slide_kit_asset_error, catalog.message),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is CatalogState.Loaded -> {
                    if (items.isEmpty() || background == null) {
                        Text(stringResource(R.string.slide_kit_empty))
                    } else {
                        Text(stringResource(R.string.slide_kit_marketplace, marketplaceRules.marketplaceName))
                        Text(stringResource(R.string.slide_kit_apply_look_info))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            drafts.forEachIndexed { index, draft ->
                                OutlinedButton(
                                    onClick = { selectedIndex = index },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.slide_kit_slide_label,
                                            index + 1,
                                            stringResource(templateLabel(draft.template)),
                                        ),
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    drafts = drafts + SlideDraft(SlideTemplate.HERO, emptyMap())
                                    selectedIndex = drafts.lastIndex
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.slide_kit_add_slide))
                            }
                        }

                        Text(stringResource(R.string.slide_kit_template_picker), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.slide_kit_template_samples),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(
                            state = templateListState,
                            flingBehavior = rememberSnapFlingBehavior(
                                lazyListState = templateListState,
                                snapPosition = SnapPosition.Start,
                            ),
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(catalog.templates, key = { it.template.wireValue }) { definition ->
                                val selected = currentDraft.template == definition.template
                                val borderColor by animateColorAsState(
                                    if (selected) PlinthTheme.accentInk
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    label = "slideTemplateSelection",
                                )
                                val selectionDescription = stringResource(
                                    if (selected) R.string.slide_kit_template_selected
                                    else R.string.slide_kit_template_not_selected,
                                )
                                Card(
                                    border = BorderStroke(2.dp, borderColor),
                                    modifier = Modifier
                                        .animateItem()
                                        .width(templateCardWidth)
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = {
                                                drafts = drafts.toMutableList().also { updated ->
                                                    updated[selectedIndex] = currentDraft.copy(
                                                        template = definition.template,
                                                    )
                                                }
                                            },
                                        )
                                        .semantics { stateDescription = selectionDescription },
                                ) {
                                    key(definition.template, items, background, style, effectiveRules, sampleTexts) {
                                        TemplateLayoutPreview(
                                            template = definition.template,
                                            items = items,
                                            background = background,
                                            style = style,
                                            rules = effectiveRules,
                                            sampleTexts = sampleTexts,
                                        )
                                    }
                                    Text(
                                        stringResource(templateLabel(definition.template)),
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
                            }
                        }

                        if (currentDraft.template.slots.isEmpty()) {
                            Text(stringResource(R.string.slide_kit_empty_text_slots))
                        } else {
                            currentDraft.template.slots.forEach { slot ->
                                val value = currentDraft.texts[slot].orEmpty()
                                val limit = marketplaceRules.limits[slot]
                                val count = value.codePointCount(0, value.length)
                                val supportingText = if (limit == null) {
                                    stringResource(R.string.slide_kit_characters, count)
                                } else {
                                    stringResource(R.string.slide_kit_character_limit, count, limit.maxCharacters)
                                }
                                val currentCheck = checks[selectedIndex]
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { text ->
                                        drafts = drafts.toMutableList().also { updated ->
                                            updated[selectedIndex] = currentDraft.copy(
                                                texts = currentDraft.texts + (slot to text),
                                            )
                                        }
                                    },
                                    label = { Text(stringResource(slotLabel(slot))) },
                                    supportingText = { Text(supportingText) },
                                    isError = currentCheck.findings.any { it.slot == slot },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        if (marketplaceRules.prohibitedCategories.isEmpty()) {
                            Text(stringResource(R.string.slide_kit_rules_not_read))
                        }
                        if (checks.any { !it.matcherAvailable && !it.prohibitedListNotRead }) {
                            Text(
                                stringResource(R.string.slide_kit_matcher_unavailable),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        val unsupported = checks.flatMap { it.unsupportedCategories }.toSet()
                        if (unsupported.isNotEmpty()) {
                            Text(
                                stringResource(
                                    R.string.slide_kit_category_unavailable,
                                    unsupported.sorted().joinToString(),
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        checks.forEach { check ->
                            check.findings.forEach { finding ->
                                Text(
                                    stringResource(
                                        R.string.slide_kit_finding,
                                        finding.ruleId,
                                        finding.category ?: finding.ruleId,
                                        stringResource(slotLabel(finding.slot)),
                                        finding.offendingSubstring,
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        preview.error?.let {
                            Text(
                                stringResource(R.string.slide_kit_text_does_not_fit),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        preview.renderError?.let { message ->
                            Text(
                                stringResource(R.string.slide_kit_render_error, message),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (preview.isLoading) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator()
                                Text(stringResource(R.string.slide_kit_rendering))
                            }
                        }
                        if (preview.pixels.isNotEmpty()) {
                            Text(stringResource(R.string.slide_kit_preview), style = MaterialTheme.typography.titleMedium)
                            preview.plans.flatMap { it.textBlocks }.forEach { block ->
                                Text(
                                    stringResource(
                                        R.string.slide_kit_contrast,
                                        block.contrastRatio,
                                        block.contrastMinimum,
                                    ),
                                )
                            }
                            LazyRow(
                                state = seriesListState,
                                flingBehavior = rememberSnapFlingBehavior(
                                    lazyListState = seriesListState,
                                    snapPosition = SnapPosition.Start,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                itemsIndexed(bitmaps, key = { index, _ -> index }) { index, bitmap ->
                                    Card(Modifier.animateItem().width(seriesCardWidth)) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text(
                                                stringResource(
                                                    R.string.slide_kit_slide_label,
                                                    index + 1,
                                                    stringResource(templateLabel(preview.plans[index].template)),
                                                ),
                                            )
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = stringResource(
                                                    R.string.slide_kit_slide_label,
                                                    index + 1,
                                                    stringResource(templateLabel(preview.plans[index].template)),
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(3f / 4f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { onRenderAndExport(preview.pixels) },
                            enabled = renderable && preview.pixels.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.slide_kit_render_export))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateLayoutPreview(
    template: SlideTemplate,
    items: List<PixelBuffer>,
    background: SlideSeriesBackground,
    style: SlideStyle,
    rules: SlideMarketplaceRules,
    sampleTexts: Map<SlideTextSlot, String>,
) {
    val preview by produceState<TemplatePreviewState>(TemplatePreviewState.Loading) {
        value = withContext(Dispatchers.Default) {
            ensureActive()
            try {
                val plan = SlideLayoutEngine.renderSeries(
                    slides = listOf(SlideDraft(template, sampleTexts.filterKeys { it in template.slots })),
                    items = items,
                    background = background,
                    style = style,
                    marketplaceRules = rules,
                ).single()
                val pixels = SlidePixelRenderer.render(plan).scale(222, 296)
                ensureActive()
                TemplatePreviewState.Ready(pixels)
            } catch (error: SlideTextLayoutException) {
                TemplatePreviewState.Failed(error.message.orEmpty())
            } catch (error: IllegalArgumentException) {
                TemplatePreviewState.Failed(error.message.orEmpty())
            }
        }
    }
    val result = preview
    if (result is TemplatePreviewState.Failed) {
        Text(
            stringResource(R.string.slide_kit_template_preview_error, result.message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(8.dp),
        )
        return
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        contentAlignment = Alignment.Center,
    ) {
        if (result is TemplatePreviewState.Ready) {
            val bitmap = remember(result.pixels) { result.pixels.toBitmap() }
            DisposableEffect(bitmap) {
                onDispose { bitmap.recycle() }
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

private sealed interface TemplatePreviewState {
    data object Loading : TemplatePreviewState
    data class Ready(val pixels: PixelBuffer) : TemplatePreviewState
    data class Failed(val message: String) : TemplatePreviewState
}

private fun loadCatalog(context: Context): CatalogState = try {
    CatalogState.Loaded(
        templates = SlideTemplateCatalog.load(context.assets),
        prohibitedText = SlideProhibitedTextCatalog.load(context.assets),
    )
} catch (error: IOException) {
    CatalogState.Failed(error.message ?: "asset read error")
} catch (error: SerializationException) {
    CatalogState.Failed(error.message ?: "invalid template data")
} catch (error: IllegalStateException) {
    CatalogState.Failed(error.message ?: "invalid template data")
} catch (error: IllegalArgumentException) {
    CatalogState.Failed(error.message ?: "invalid template data")
}

private sealed interface CatalogState {
    data class Loaded(
        val templates: List<SlideTemplateDefinition>,
        val prohibitedText: SlideProhibitedTextCatalog,
    ) : CatalogState
    data class Failed(val message: String) : CatalogState
}

private data class PreviewResult(
    val plans: List<SlideRenderPlan>,
    val pixels: List<PixelBuffer>,
    val error: SlideTextLayoutException?,
    val renderError: String?,
    val isLoading: Boolean,
)

private fun templateLabel(template: SlideTemplate): Int = when (template) {
    SlideTemplate.HERO -> R.string.slide_kit_template_hero
    SlideTemplate.SPEC3 -> R.string.slide_kit_template_spec3
    SlideTemplate.SIZE -> R.string.slide_kit_template_size
    SlideTemplate.DETAIL -> R.string.slide_kit_template_detail
    SlideTemplate.KIT -> R.string.slide_kit_template_kit
    SlideTemplate.USE -> R.string.slide_kit_template_use
    SlideTemplate.VARIANTS -> R.string.slide_kit_template_variants
    SlideTemplate.CLEAN -> R.string.slide_kit_template_clean
}

private fun slotLabel(slot: SlideTextSlot): Int = when (slot) {
    SlideTextSlot.TITLE -> R.string.slide_kit_slot_title
    SlideTextSlot.SPEC_1 -> R.string.slide_kit_slot_spec_1
    SlideTextSlot.SPEC_2 -> R.string.slide_kit_slot_spec_2
    SlideTextSlot.SPEC_3 -> R.string.slide_kit_slot_spec_3
    SlideTextSlot.BADGE -> R.string.slide_kit_slot_badge
    SlideTextSlot.CAPTION -> R.string.slide_kit_slot_caption
}
