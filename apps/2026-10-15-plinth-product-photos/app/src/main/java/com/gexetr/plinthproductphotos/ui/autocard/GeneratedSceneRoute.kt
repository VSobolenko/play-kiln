package com.gexetr.plinthproductphotos.ui.autocard

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardEngine
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardFallbackReason
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardSceneCatalog
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.studio.StudioViewModel
import com.gexetr.plinthproductphotos.ui.studio.studioViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun GeneratedSceneRoute(
    photoId: String?,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    sourceAspectRatio: Float = 1f,
    sourcePhotoContent: (@Composable (Bitmap?, Float) -> Unit)? = null,
    photo: PixelBuffer? = null,
    preset: MarketplacePreset? = null,
    sceneRecipesJson: String? = null,
    lastUsedCategory: AutoCategory? = null,
    isOffline: Boolean = false,
    onUseOriginal: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val factory = remember(context) { studioViewModelFactory(context) }
    val model: StudioViewModel = viewModel(key = "create-scene-$photoId", factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    val recognitionFactory = remember(context, lastUsedCategory, isOffline) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                modelClass.cast(AutoCardRecognitionViewModel(context, lastUsedCategory, isOffline))
                    ?: error("Unsupported recognition ViewModel")
        }
    }
    val recognitionModel: AutoCardRecognitionViewModel =
        viewModel(key = "create-recognition-$photoId", factory = recognitionFactory)
    val recognition by recognitionModel.state.collectAsStateWithLifecycle()
    val catalog = remember(context, sceneRecipesJson) {
        AutoCardSceneCatalog.fromJson(
            sceneRecipesJson ?: context.assets.open("autocard/scene-recipes.json")
                .bufferedReader().use { it.readText() },
        )
    }
    var guidance by rememberSaveable(photoId) { mutableStateOf("") }
    var showOriginal by rememberSaveable(photoId) { mutableStateOf(false) }
    // The seller's own words are the scene. Engine context (category, marketplace white-background policy)
    // used to be prepended and the provider rendered the white background it asked for, not the jungle.
    val prompt = guidance.trim().takeIf { it.isNotEmpty() }
        ?: AutoCardEngine.generationPrompt(recognition, catalog, preset, null, "")
    val gate = photoId?.let { sceneGenerationGate(state, it, prompt.orEmpty()) }
    val canGenerate = gate?.canGenerate == true && prompt != null
    val selectedSource = photoId != null && state.photoId == photoId
    val result = state.result.takeIf {
        selectedSource && state.operation == OperationId.GENERATED_SCENE
    }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri -> if (uri != null) model.saveDocument(uri) }

    // Studio owns the durable upload/job/receipt. Never replace a different pending input.
    LaunchedEffect(model) { model.refresh() }
    LaunchedEffect(recognitionModel, state.photoId, state.busy, state.hasPending, photoId) {
        if (photoId != null && !state.busy && !state.hasPending &&
            state.result == null && preset?.editPolicy != EditPolicy.ORIGINAL_ONLY
        ) {
            recognitionModel.recognize(photoId, photo)
        }
    }
    LaunchedEffect(state.busy, state.photoId, state.hasPending, state.errorCode, state.config, photoId) {
        if (photoId != null && !state.busy && !state.hasPending &&
            state.errorCode == null && state.config != null && state.photoId != photoId
        ) {
            model.choosePhoto(photoId)
        }
    }

    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.w74_ai_background)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.autocard_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (result == null) {
                        Button(
                            onClick = {
                                val credits = gate?.credits
                                if (canGenerate && credits != null && prompt != null) {
                                    showOriginal = false
                                    model.generate(OperationId.GENERATED_SCENE, prompt, credits)
                                }
                            },
                            enabled = canGenerate,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Text(
                                gate?.credits?.let { stringResource(R.string.w74_generate_cost, it) }
                                    ?: stringResource(R.string.w74_generate),
                            )
                        }
                    } else if (!state.applied) {
                        Button(
                            onClick = model::applyResult,
                            enabled = !state.busy && state.deliveryConfirmed,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Text(stringResource(R.string.w74_keep_result))
                        }
                    } else {
                        Button(
                            onClick = { documentPicker.launch("plinth-scene.png") },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Text(stringResource(R.string.w74_save_image))
                        }
                    }
                }
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (result != null && !showOriginal) {
                FormingCardPreview(
                    bitmap = result,
                    aspectRatio = result.width.toFloat() / result.height,
                    busy = false,
                    failed = false,
                    message = "",
                )
            } else if (photoId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))) {
                        if (sourcePhotoContent != null) {
                            sourcePhotoContent(null, sourceAspectRatio)
                        } else {
                            AutoCardSourcePhoto(photoId, null, sourceAspectRatio, onBack)
                        }
                    }
                    TextButton(
                        onClick = onChangePhoto,
                        enabled = !state.busy,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.w74_change_photo))
                    }
                }
            } else {
                TextButton(onClick = onChangePhoto, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.w74_choose_photo))
                }
            }
            if (result != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showOriginal,
                        onClick = { showOriginal = true },
                        label = { Text(stringResource(R.string.autocard_result_before)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = !showOriginal,
                        onClick = { showOriginal = false },
                        label = { Text(stringResource(R.string.autocard_result_after)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                Text(
                    stringResource(
                        if (state.applied) R.string.w74_result_kept else R.string.w74_generated_disclosure,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.documentSaved) Text(stringResource(R.string.w74_image_saved))
                if (!state.busy && state.deliveryConfirmed) {
                    TextButton(
                        onClick = model::discard,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.w74_another_background))
                    }
                }
            } else if (!state.hasPending) {
                if (preset?.editPolicy == EditPolicy.ORIGINAL_ONLY) {
                    Text(stringResource(R.string.autocard_marketplace_forbidden))
                    TextButton(onClick = onUseOriginal, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.autocard_return_original))
                    }
                } else {
                    when {
                        recognition.isRecognizing -> {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.autocard_recognition_progress))
                        }
                        recognition.fallbackReason != null -> Text(
                            stringResource(
                                if (recognition.fallbackReason == AutoCardFallbackReason.OFFLINE)
                                    R.string.autocard_recognition_offline else R.string.autocard_recognition_failed,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                        recognition.isSuggested -> Text(
                            stringResource(
                                R.string.autocard_recognition_suggestion,
                                requireNotNull(recognition.recognition).confidence * 100,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = guidance,
                    onValueChange = { guidance = it },
                    enabled = !state.busy,
                    label = { Text(stringResource(R.string.w87_scene_prompt_label)) },
                    placeholder = { Text(stringResource(R.string.w87_scene_prompt_hint)) },
                    isError = (prompt?.length ?: guidance.length) > 2000,
                    supportingText = if ((prompt?.length ?: guidance.length) > 2000) {
                        { Text(stringResource(R.string.w74_guidance_too_long)) }
                    } else null,
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        R.string.w87_chip_sea,
                        R.string.w87_chip_forest,
                        R.string.w87_chip_table,
                        R.string.w87_chip_studio,
                    ).forEach { chip ->
                        val text = stringResource(chip)
                        AssistChip(
                            onClick = { guidance = text },
                            enabled = !state.busy,
                            label = { Text(text) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }

            when {
                gate?.conflictingPending == true -> {
                    Text(stringResource(R.string.w74_other_pending), color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = model::finishPending,
                            enabled = !state.busy,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.w74_pending_finish)) }
                        OutlinedButton(
                            onClick = model::discardPending,
                            enabled = !state.busy,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.w74_pending_discard)) }
                    }
                }
                state.busy -> {
                    CircularProgressIndicator()
                    if (state.hasPending) Text(stringResource(R.string.w74_generating))
                }
                state.errorCode != null -> {
                    Text(
                        stringResource(
                            when (state.errorCode) {
                                "INSUFFICIENT_CREDITS" -> R.string.w74_add_credits
                                "SESSION_REQUIRED" -> R.string.w74_session_required
                                "PRICE_CHANGED" -> R.string.w74_price_changed
                                else -> R.string.w74_generation_failed
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = model::refresh, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.w74_retry))
                    }
                }
                photoId == null ->
                    Text(stringResource(R.string.w74_choose_photo_first))
                gate?.needsCredits == true ->
                    Text(stringResource(R.string.w74_add_credits), color = MaterialTheme.colorScheme.error)
                gate?.serviceAvailable != true && result == null ->
                    Text(stringResource(R.string.w74_scene_unavailable), color = MaterialTheme.colorScheme.error)
            }
        }
    }

}

private fun autoCategoryLabel(category: AutoCategory): Int = when (category) {
    AutoCategory.CLOTHING_TOP -> R.string.autocard_category_clothing_top
    AutoCategory.CLOTHING_BOTTOM -> R.string.autocard_category_clothing_bottom
    AutoCategory.DRESS -> R.string.autocard_category_dress
    AutoCategory.OUTERWEAR -> R.string.autocard_category_outerwear
    AutoCategory.SHOES -> R.string.autocard_category_shoes
    AutoCategory.BAG -> R.string.autocard_category_bag
    AutoCategory.ACCESSORY -> R.string.autocard_category_accessory
    AutoCategory.JEWELRY -> R.string.autocard_category_jewelry
    AutoCategory.WATCH -> R.string.autocard_category_watch
    AutoCategory.COSMETICS -> R.string.autocard_category_cosmetics
    AutoCategory.SKINCARE -> R.string.autocard_category_skincare
    AutoCategory.PACKAGED_FOOD -> R.string.autocard_category_packaged_food
    AutoCategory.BEVERAGE -> R.string.autocard_category_beverage
    AutoCategory.ELECTRONICS_SMALL -> R.string.autocard_category_electronics_small
    AutoCategory.HOME_DECOR -> R.string.autocard_category_home_decor
    AutoCategory.KITCHENWARE -> R.string.autocard_category_kitchenware
    AutoCategory.TOY -> R.string.autocard_category_toy
    AutoCategory.BOOK -> R.string.autocard_category_book
    AutoCategory.FURNITURE_SMALL -> R.string.autocard_category_furniture_small
    AutoCategory.TOOL -> R.string.autocard_category_tool
    AutoCategory.OTHER -> R.string.autocard_category_other
}
