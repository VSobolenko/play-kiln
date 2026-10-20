package com.gexetr.plinthproductphotos.ui.editor

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.components.PlinthStage
import com.gexetr.plinthproductphotos.ui.theme.PlinthProductPalette
import com.gexetr.plinthproductphotos.ui.cutout.MaskBrushMode
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class CutoutActionGlyph {
    REMOVE,
    ADD,
    UNDO,
    REDO,
    SELECT_ALL,
    RESET,
    PHOTO,
    BACK,
}

private const val TRANSPARENT_BACKGROUND = 0x00000000
private const val MAX_RECENT_CUSTOM_COLORS = 6
private val recentCustomColorMemory = mutableStateListOf<Int>()

val LocalEditorSavedAction = staticCompositionLocalOf<((String) -> Unit)?> { null }

@Composable
internal fun CutoutActionGlyphIcon(
    glyph: CutoutActionGlyph,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val iconTint = tint ?: MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        val left = size.width * 0.2f
        val right = size.width * 0.8f
        val top = size.height * 0.2f
        val bottom = size.height * 0.8f
        val center = Offset(size.width / 2f, size.height / 2f)
        when (glyph) {
            CutoutActionGlyph.REMOVE -> {
                drawLine(iconTint, Offset(left, center.y), Offset(right, center.y), strokeWidth)
                drawLine(iconTint, Offset(size.width * 0.32f, top), Offset(size.width * 0.68f, bottom), strokeWidth)
            }
            CutoutActionGlyph.ADD -> {
                drawLine(iconTint, Offset(left, center.y), Offset(right, center.y), strokeWidth)
                drawLine(iconTint, Offset(center.x, top), Offset(center.x, bottom), strokeWidth)
            }
            CutoutActionGlyph.UNDO,
            CutoutActionGlyph.RESET -> {
                drawArc(
                    color = iconTint,
                    startAngle = 45f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(strokeWidth),
                )
                drawLine(iconTint, Offset(left, top), Offset(left, top + size.height * 0.25f), strokeWidth)
                drawLine(iconTint, Offset(left, top), Offset(left + size.width * 0.25f, top), strokeWidth)
            }
            CutoutActionGlyph.REDO -> {
                drawArc(
                    color = iconTint,
                    startAngle = 210f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(strokeWidth),
                )
                drawLine(iconTint, Offset(right, top), Offset(right, top + size.height * 0.25f), strokeWidth)
                drawLine(iconTint, Offset(right, top), Offset(right - size.width * 0.25f, top), strokeWidth)
            }
            CutoutActionGlyph.SELECT_ALL -> {
                drawRect(
                    color = iconTint,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(strokeWidth),
                )
                drawRect(
                    color = iconTint,
                    topLeft = Offset(left + strokeWidth * 2f, top + strokeWidth * 2f),
                    size = Size(right - left - strokeWidth * 4f, bottom - top - strokeWidth * 4f),
                    style = Stroke(strokeWidth),
                )
            }
            CutoutActionGlyph.PHOTO -> {
                drawRect(
                    color = iconTint,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(strokeWidth),
                )
                drawLine(iconTint, Offset(left + strokeWidth, bottom - strokeWidth), center, strokeWidth)
                drawLine(iconTint, center, Offset(right - strokeWidth, top + size.height * 0.4f), strokeWidth)
            }
            CutoutActionGlyph.BACK -> {
                drawLine(iconTint, Offset(right, center.y), Offset(left, center.y), strokeWidth)
                drawLine(iconTint, Offset(left, center.y), Offset(left + size.width * 0.28f, top), strokeWidth)
                drawLine(iconTint, Offset(left, center.y), Offset(left + size.width * 0.28f, bottom), strokeWidth)
            }
        }
    }
}

@Composable
fun EditorRoute(
    photoId: String,
    onBack: () -> Unit,
    onSaved: (() -> Unit)? = null,
    initialMaskAlpha: ByteArray? = null,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.factory(LocalContext.current)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedAction = LocalEditorSavedAction.current
    LaunchedEffect(photoId, initialMaskAlpha?.contentHashCode()) {
        viewModel.load(photoId, initialMaskAlpha)
    }
    EditorScreen(
        state = state,
        onBack = onBack,
        onSaved = {
            if (onSaved != null) onSaved()
            else savedAction?.invoke(photoId) ?: onBack()
        },
        onSave = viewModel::save,
        onPreviewSettingsChange = viewModel::previewSettings,
        onClearPreview = viewModel::clearPreview,
        onSettingsChange = viewModel::updateSettings,
        onShadowChange = viewModel::setShadow,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        modifier = modifier,
    )
}

@Composable
internal fun CutoutSelectionSurface(
    source: Bitmap,
    overlay: Bitmap?,
    selectionSeed: Pair<Float, Float>?,
    selectAll: Boolean,
    enabled: Boolean,
    onTap: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.cutout_select_object_hint)
    val sourceImage = remember(source) { source.asImageBitmap() }
    val overlayImage = remember(overlay) { overlay?.asImageBitmap() }
    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(source, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    mapImagePoint(offset, size, source)?.let { onTap(it.first, it.second) }
                }
            },
    ) {
        val bounds = imageBounds(size, source)
        drawImage(
            image = sourceImage,
            dstOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()),
            dstSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt()),
        )
        if (selectAll) {
            drawRect(
                color = Color(0xff00a895).copy(alpha = 0.25f),
                topLeft = Offset(bounds.left, bounds.top),
                size = Size(bounds.width, bounds.height),
            )
        }
        if (overlayImage != null) {
            drawImage(
                image = overlayImage,
                dstOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()),
                dstSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt()),
            )
        }
        selectionSeed?.let { (x, y) ->
            val point = Offset(bounds.left + x * bounds.width, bounds.top + y * bounds.height)
            drawCircle(Color.White, radius = 15.dp.toPx(), center = point, style = Stroke(width = 5.dp.toPx()))
            drawCircle(Color(0xff00a895), radius = 12.dp.toPx(), center = point, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun CutoutMaskEditorScreen(
    source: Bitmap,
    overlay: Bitmap?,
    brushMode: MaskBrushMode,
    brushSize: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onBrushModeChange: (MaskBrushMode) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onBrushStroke: (List<Pair<Float, Float>>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectAll: () -> Unit,
    onResetSelection: () -> Unit,
) {
    PlinthScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cutout_editor_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.plinth_back))
                    }
                },
            )
        },
        bottomBar = {
            androidx.compose.material3.Surface(
                modifier = Modifier.navigationBarsPadding(),
                shadowElevation = 6.dp,
            ) {
                androidx.compose.material3.Button(
                    onClick = onBack,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.cutout_editor_done))
                }
            }
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            val removeMode = brushMode == MaskBrushMode.REMOVE
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = availableHeight * 0.55f),
                ) {
                    CutoutMaskPaintCanvas(
                        source = source,
                        overlay = overlay,
                        brushMode = brushMode,
                        enabled = !isSaving,
                        onBrushStroke = onBrushStroke,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        shape = MaterialTheme.shapes.small,
                        color = if (removeMode) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = if (removeMode) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            stringResource(
                                if (removeMode) R.string.cutout_editor_mode_remove
                                else R.string.cutout_editor_mode_restore,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    ) {
                        Text(
                            stringResource(R.string.cutout_editor_drag_hint),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val brushSizeDescription = stringResource(
                    R.string.cutout_editor_brush_size,
                    (brushSize * 200).roundToInt(),
                )
                val removeDescription = stringResource(R.string.cutout_editor_remove)
                val restoreDescription = stringResource(R.string.cutout_editor_restore)
                val undoDescription = stringResource(R.string.cutout_undo)
                val redoDescription = stringResource(R.string.cutout_redo)
                Text(
                    brushSizeDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { onBrushModeChange(MaskBrushMode.REMOVE) },
                        enabled = !isSaving,
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                            .background(
                                if (removeMode) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .semantics {
                                contentDescription = removeDescription
                            },
                    ) {
                        CutoutActionGlyphIcon(
                            glyph = CutoutActionGlyph.REMOVE,
                            tint = if (removeMode) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { onBrushModeChange(MaskBrushMode.ADD) },
                        enabled = !isSaving,
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                            .background(
                                if (!removeMode) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .semantics {
                                contentDescription = restoreDescription
                            },
                    ) {
                        CutoutActionGlyphIcon(
                            glyph = CutoutActionGlyph.ADD,
                            tint = if (!removeMode) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = brushSize,
                        onValueChange = onBrushSizeChange,
                        valueRange = 0.005f..0.06f,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                            .semantics { contentDescription = brushSizeDescription },
                    )
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo && !isSaving,
                        modifier = Modifier.size(48.dp).semantics {
                            contentDescription = undoDescription
                        },
                    ) {
                        CutoutActionGlyphIcon(CutoutActionGlyph.UNDO)
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo && !isSaving,
                        modifier = Modifier.size(48.dp).semantics {
                            contentDescription = redoDescription
                        },
                    ) {
                        CutoutActionGlyphIcon(CutoutActionGlyph.REDO)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MaskEditorActionChip(
                        label = R.string.cutout_need_action_all,
                        description = R.string.cutout_select_all,
                        glyph = CutoutActionGlyph.SELECT_ALL,
                        enabled = !isSaving,
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f),
                    )
                    MaskEditorActionChip(
                        label = R.string.cutout_need_action_reset,
                        description = R.string.cutout_reset_selection,
                        glyph = CutoutActionGlyph.RESET,
                        enabled = !isSaving,
                        onClick = onResetSelection,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    stringResource(R.string.cutout_editor_selection_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isSaving) {
                    Text(
                        stringResource(R.string.cutout_mask_saving),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun MaskEditorActionChip(
    label: Int,
    description: Int,
    glyph: CutoutActionGlyph,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconDescription = stringResource(description)
    FilterChip(
        selected = false,
        enabled = enabled,
        onClick = onClick,
        label = {
            Text(
                stringResource(label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            CutoutActionGlyphIcon(
                glyph,
                modifier = Modifier.semantics {
                    contentDescription = iconDescription
                },
            )
        },
        modifier = modifier.heightIn(min = 48.dp),
    )
}

@Composable
internal fun CutoutMaskPaintCanvas(
    source: Bitmap,
    overlay: Bitmap?,
    brushMode: MaskBrushMode,
    enabled: Boolean,
    onBrushStroke: (List<Pair<Float, Float>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (brushMode == MaskBrushMode.REMOVE) {
            R.string.cutout_editor_remove
        } else {
            R.string.cutout_editor_restore
        },
    )
    val sourceImage = remember(source) { source.asImageBitmap() }
    val overlayImage = remember(overlay) { overlay?.asImageBitmap() }
    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(source, enabled) {
                if (!enabled) return@pointerInput
                val points = mutableListOf<Pair<Float, Float>>()
                detectDragGestures(
                    onDragStart = { offset ->
                        points.clear()
                        mapImagePoint(offset, size, source)?.let(points::add)
                    },
                    onDragEnd = {
                        if (points.isNotEmpty()) onBrushStroke(points.toList())
                    },
                    onDragCancel = { points.clear() },
                ) { change, _ ->
                    change.consume()
                    mapImagePoint(change.position, size, source)?.let(points::add)
                }
            },
    ) {
        val bounds = imageBounds(size, source)
        drawImage(
            image = sourceImage,
            dstOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()),
            dstSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt()),
        )
        if (overlayImage != null) {
            drawImage(
                image = overlayImage,
                dstOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()),
                dstSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt()),
            )
        }
    }
}

private data class ImageBounds(val left: Float, val top: Float, val width: Float, val height: Float)

private fun imageBounds(size: Size, bitmap: Bitmap): ImageBounds {
    val scale = min(size.width / bitmap.width, size.height / bitmap.height)
    val width = bitmap.width * scale
    val height = bitmap.height * scale
    return ImageBounds((size.width - width) / 2f, (size.height - height) / 2f, width, height)
}

private fun mapImagePoint(
    offset: Offset,
    canvasSize: IntSize,
    bitmap: Bitmap,
): Pair<Float, Float>? {
    val bounds = imageBounds(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()), bitmap)
    if (offset.x !in bounds.left..(bounds.left + bounds.width) ||
        offset.y !in bounds.top..(bounds.top + bounds.height)
    ) return null
    return ((offset.x - bounds.left) / bounds.width) to ((offset.y - bounds.top) / bounds.height)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onSave: () -> Unit,
    onPreviewSettingsChange: (CompositionSettings) -> Unit,
    onClearPreview: () -> Unit,
    onSettingsChange: (CompositionSettings, String) -> Unit,
    onShadowChange: (ShadowSpec.Kind) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showExitConfirmation by rememberSaveable(state.photoName) { mutableStateOf(false) }
    var saveAndExit by rememberSaveable(state.photoName) { mutableStateOf(false) }
    val backDescription = stringResource(R.string.editor_back_to_photo)
    val settings = state.settings
    val requestExit = {
        when {
            state.saveState == EditorSaveState.SAVING -> Unit
            state.hasUnsavedChanges -> showExitConfirmation = true
            state.saveState == EditorSaveState.SAVED -> onSaved()
            else -> onBack()
        }
    }
    BackHandler(enabled = true, onBack = requestExit)
    LaunchedEffect(state.saveState, saveAndExit) {
        when {
            saveAndExit && state.saveState == EditorSaveState.SAVED -> {
                saveAndExit = false
                showExitConfirmation = false
                onSaved()
            }
            saveAndExit && state.saveState == EditorSaveState.FAILED -> saveAndExit = false
        }
    }

    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plinth_editor_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = requestExit,
                        enabled = state.saveState != EditorSaveState.SAVING,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics {
                            contentDescription = backDescription
                        },
                    ) {
                        CutoutActionGlyphIcon(
                            glyph = CutoutActionGlyph.BACK,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                shadowElevation = 6.dp,
            ) {
                Button(
                    onClick = {
                        when (state.saveState) {
                            EditorSaveState.IDLE,
                            EditorSaveState.FAILED -> {
                                onClearPreview()
                                onSave()
                            }
                            EditorSaveState.SAVED -> onSaved()
                            EditorSaveState.SAVING -> Unit
                        }
                    },
                    enabled = !state.isLoading && state.composition != null &&
                        state.saveState != EditorSaveState.SAVING,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .heightIn(min = 52.dp),
                ) {
                    if (state.saveState == EditorSaveState.SAVING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(
                        stringResource(
                            when (state.saveState) {
                                EditorSaveState.IDLE -> R.string.editor_save_card
                                EditorSaveState.SAVING -> R.string.editor_saving
                                EditorSaveState.SAVED -> R.string.editor_saved_return
                                EditorSaveState.FAILED -> R.string.editor_retry_save
                            },
                        ),
                    )
                }
            }
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            val availableHeight = maxHeight
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error && state.composition == null -> Text(
                    stringResource(R.string.plinth_photo_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
                settings != null && state.composition != null -> {
                    val minimumDockHeight = 104.dp
                    val previewHeight = (availableHeight * 0.6f)
                        .coerceAtMost((availableHeight - minimumDockHeight).coerceAtLeast(0.dp))
                    var activeCategory by rememberSaveable(state.photoName) {
                        mutableStateOf(EditorToolCategory.BACKGROUND)
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EditorPreview(
                            state = state,
                            settings = settings,
                            modifier = Modifier.fillMaxWidth().height(previewHeight),
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 0.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                EditorToolDock(
                                    state = state,
                                    activeCategory = activeCategory,
                                    onCategorySelected = { activeCategory = it },
                                    onUndo = onUndo,
                                    onRedo = onRedo,
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    EditorTools(
                                        state = state,
                                        settings = settings,
                                        category = activeCategory,
                                        onSettingsChange = { next, key ->
                                            onClearPreview()
                                            onSettingsChange(next, key)
                                        },
                                        onShadowChange = {
                                            onClearPreview()
                                            onShadowChange(it)
                                        },
                                        onPreviewSettingsChange = onPreviewSettingsChange,
                                        onClearPreview = onClearPreview,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            title = { Text(stringResource(R.string.editor_exit_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.editor_exit_message))
                    if (state.saveState == EditorSaveState.FAILED) {
                        Text(
                            stringResource(R.string.editor_save_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = {
                            saveAndExit = true
                            onSave()
                        },
                        enabled = state.saveState != EditorSaveState.SAVING,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        if (state.saveState == EditorSaveState.SAVING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(
                            stringResource(
                                if (state.saveState == EditorSaveState.SAVING) R.string.editor_saving
                                else R.string.editor_save_and_return,
                            ),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            saveAndExit = false
                            showExitConfirmation = false
                            onBack()
                        },
                        enabled = state.saveState != EditorSaveState.SAVING,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.editor_discard))
                    }
                    TextButton(
                        onClick = {
                            saveAndExit = false
                            showExitConfirmation = false
                        },
                        enabled = state.saveState != EditorSaveState.SAVING,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.editor_keep_editing))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
    }
}

@Composable
private fun EditorPreview(
    state: EditorUiState,
    settings: CompositionSettings,
    modifier: Modifier = Modifier,
) {
    var dividerFraction by rememberSaveable(state.photoName) { mutableFloatStateOf(0.5f) }
    var comparisonEnabled by rememberSaveable(state.photoName) { mutableStateOf(true) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = comparisonEnabled,
            onClick = { comparisonEnabled = !comparisonEnabled },
            label = { Text(stringResource(R.string.plinth_comparison_mode)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        PlinthStage(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(minOf(maxWidth, maxHeight))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(settings.background.colorArgb ?: PlinthProductPalette.Paper.toArgb())),
                ) {
                    if (settings.background.colorArgb == TRANSPARENT_BACKGROUND) {
                        CheckerboardBackground(Modifier.fillMaxSize())
                    }
                    val originalImage = remember(state.original) { state.original?.asImageBitmap() }
                    val compositionImage = remember(state.composition) { state.composition?.asImageBitmap() }
                    val comparisonDescription = "${stringResource(R.string.plinth_before)} / ${stringResource(R.string.plinth_after)}"
                    val compositionDescription = stringResource(R.string.plinth_after_description)
                    val dividerColor = PlinthTheme.accentInk
                    if (originalImage != null && compositionImage != null) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (comparisonEnabled) {
                                        Modifier.pointerInput(Unit) {
                                            detectHorizontalDragGestures { change, dragAmount ->
                                                change.consume()
                                                if (size.width > 0) {
                                                    dividerFraction = (dividerFraction + dragAmount / size.width).coerceIn(0f, 1f)
                                                }
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .semantics {
                                    contentDescription = if (comparisonEnabled) comparisonDescription else compositionDescription
                                }
                        ) {
                            fun imageBounds(image: androidx.compose.ui.graphics.ImageBitmap, offsetX: Float = 0f, offsetY: Float = 0f): Pair<IntOffset, IntSize> {
                                val scale = min(size.width / image.width, size.height / image.height)
                                val width = (image.width * scale).roundToInt()
                                val height = (image.height * scale).roundToInt()
                                return IntOffset(
                                    ((size.width - width) / 2f + offsetX).roundToInt(),
                                    ((size.height - height) / 2f + offsetY).roundToInt(),
                                ) to IntSize(width, height)
                            }
                            val originalBounds = imageBounds(originalImage)
                            val compositionBounds = imageBounds(
                                compositionImage,
                                settings.horizontalPosition * size.width,
                                settings.verticalPosition * size.height,
                            )
                            if (comparisonEnabled) {
                                drawImage(originalImage, dstOffset = originalBounds.first, dstSize = originalBounds.second)
                                clipRect(right = size.width * dividerFraction) {
                                    drawImage(
                                        compositionImage,
                                        dstOffset = compositionBounds.first,
                                        dstSize = compositionBounds.second,
                                    )
                                }
                                val dividerX = size.width * dividerFraction
                                drawLine(
                                    color = Color.White,
                                    start = Offset(dividerX, 0f),
                                    end = Offset(dividerX, size.height),
                                    strokeWidth = 2.dp.toPx(),
                                )
                                drawCircle(Color.White, radius = 16.dp.toPx(), center = Offset(dividerX, size.height / 2f))
                                drawCircle(
                                    dividerColor,
                                    radius = 12.dp.toPx(),
                                    center = Offset(dividerX, size.height / 2f),
                                )
                            } else {
                                drawImage(
                                    compositionImage,
                                    dstOffset = compositionBounds.first,
                                    dstSize = compositionBounds.second,
                                )
                            }
                        }
                    } else {
                        compositionImage?.let { image ->
                            Image(
                                bitmap = image,
                                contentDescription = stringResource(R.string.plinth_after_description),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
        if (state.error) {
            Text(
                stringResource(R.string.plinth_compose_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.saveState == EditorSaveState.FAILED) {
            Text(
                stringResource(R.string.editor_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private enum class EditorToolCategory(val labelResource: Int) {
    BACKGROUND(R.string.plinth_background_section),
    SHADOW(R.string.plinth_shadow_section),
    FRAMING(R.string.plinth_editor_framing),
    POSITION(R.string.plinth_editor_position),
}

@Composable
private fun EditorToolDock(
    state: EditorUiState,
    activeCategory: EditorToolCategory,
    onCategorySelected: (EditorToolCategory) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    val canEdit = state.saveState != EditorSaveState.SAVING
    val undoDescription = stringResource(R.string.plinth_undo)
    val redoDescription = stringResource(R.string.plinth_redo)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f).padding(start = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(EditorToolCategory.entries) { category ->
                FilterChip(
                    selected = activeCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(stringResource(category.labelResource)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        IconButton(
            onClick = onUndo,
            enabled = state.canUndo && canEdit,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = undoDescription },
        ) {
            CutoutActionGlyphIcon(glyph = CutoutActionGlyph.UNDO)
        }
        IconButton(
            onClick = onRedo,
            enabled = state.canRedo && canEdit,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = redoDescription },
        ) {
            CutoutActionGlyphIcon(glyph = CutoutActionGlyph.REDO)
        }
    }
}

@Composable
private fun EditorTools(
    state: EditorUiState,
    settings: CompositionSettings,
    category: EditorToolCategory,
    onSettingsChange: (CompositionSettings, String) -> Unit,
    onShadowChange: (ShadowSpec.Kind) -> Unit,
    onPreviewSettingsChange: (CompositionSettings) -> Unit,
    onClearPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canEdit = state.saveState != EditorSaveState.SAVING
    var showCustomColorPicker by rememberSaveable(state.photoName) { mutableStateOf(false) }
    val recentCustomColors = recentCustomColorMemory
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (category) {
            EditorToolCategory.BACKGROUND -> {
                SectionLabel(stringResource(R.string.plinth_background_section))
                Text(
                    stringResource(R.string.plinth_editor_background_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val backgroundChoices = listOf(
                    R.string.plinth_settings_paper to PlinthProductPalette.Paper,
                    R.string.plinth_settings_graphite to PlinthProductPalette.Graphite,
                    R.string.plinth_settings_travertine to PlinthProductPalette.Travertine,
                    R.string.plinth_settings_clay to PlinthProductPalette.Clay,
                    R.string.plinth_settings_clay_deep to PlinthProductPalette.ClayDeep,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(backgroundChoices) { (label, color) ->
                        BackgroundChoice(
                            label = stringResource(label),
                            color = color,
                            selected = settings.background.colorArgb == color.toArgb(),
                            enabled = canEdit,
                            onClick = {
                                onSettingsChange(settings.copy(background = solidBackground(color.toArgb())), "background")
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.background.colorArgb == TRANSPARENT_BACKGROUND,
                        onClick = {
                            onSettingsChange(
                                settings.copy(background = solidBackground(TRANSPARENT_BACKGROUND)),
                                "background",
                            )
                        },
                        enabled = canEdit,
                        label = { Text(stringResource(R.string.editor_background_none)) },
                        leadingIcon = { TransparentSwatch() },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = settings.background.colorArgb != null &&
                            settings.background.colorArgb != TRANSPARENT_BACKGROUND &&
                            backgroundChoices.none { (_, color) -> color.toArgb() == settings.background.colorArgb },
                        onClick = { showCustomColorPicker = true },
                        enabled = canEdit,
                        label = { Text(stringResource(R.string.editor_custom_color)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                if (recentCustomColors.isNotEmpty()) {
                    Text(
                        stringResource(R.string.editor_recent_colors),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentCustomColors) { colorArgb ->
                            val colorLabel = stringResource(
                                R.string.editor_recent_custom_color,
                                "#" + colorArgb.toUInt().toString(16).uppercase(),
                            )
                            BackgroundChoice(
                                label = colorLabel,
                                color = Color(colorArgb),
                                selected = settings.background.colorArgb == colorArgb,
                                enabled = canEdit,
                                onClick = {
                                    onSettingsChange(settings.copy(background = solidBackground(colorArgb)), "background")
                                },
                            )
                        }
                    }
                }
                if (showCustomColorPicker) {
                    CustomColorDialog(
                        initialColor = settings.background.colorArgb
                            ?.takeIf { it != TRANSPARENT_BACKGROUND }
                            ?: PlinthProductPalette.Paper.toArgb(),
                        settings = settings,
                        state = state,
                        onPreviewSettingsChange = onPreviewSettingsChange,
                        onClearPreview = onClearPreview,
                        onCancel = { showCustomColorPicker = false },
                        onApply = { colorArgb ->
                            recentCustomColors.remove(colorArgb)
                            recentCustomColors.add(0, colorArgb)
                            while (recentCustomColors.size > MAX_RECENT_CUSTOM_COLORS) {
                                recentCustomColors.removeAt(recentCustomColors.lastIndex)
                            }
                            showCustomColorPicker = false
                            onClearPreview()
                            onSettingsChange(settings.copy(background = solidBackground(colorArgb)), "background")
                        },
                    )
                }
            }
            EditorToolCategory.SHADOW -> {
                SectionLabel(stringResource(R.string.plinth_shadow_section))
                Text(
                    stringResource(R.string.plinth_editor_shadow_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShadowSpec.Kind.entries.forEach { kind ->
                        FilterChip(
                            selected = settings.shadow.kind == kind,
                            onClick = { onShadowChange(kind) },
                            enabled = canEdit,
                            label = {
                                Text(
                                    shadowLabel(kind),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        )
                    }
                }
                SliderSetting(
                    label = stringResource(
                        R.string.plinth_shadow_strength,
                        (settings.shadow.opacity * 100).roundToInt(),
                    ),
                    value = settings.shadow.opacity,
                    range = 0f..1f,
                    enabled = canEdit,
                    onValueChange = {
                        onSettingsChange(settings.copy(shadow = settings.shadow.copy(opacity = it)), "shadow-opacity")
                    },
                )
            }
            EditorToolCategory.FRAMING -> {
                SectionLabel(stringResource(R.string.plinth_editor_framing))
                Text(
                    stringResource(R.string.plinth_editor_framing_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SliderSetting(
                    label = stringResource(R.string.plinth_margin_value, (settings.marginPct * 100).toInt()),
                    value = settings.marginPct,
                    range = 0f..0.14f,
                    enabled = canEdit,
                    onValueChange = { onSettingsChange(settings.copy(marginPct = it), "margin") },
                )
                SliderSetting(
                    label = stringResource(R.string.plinth_fill_value, (settings.fillTarget * 100).toInt()),
                    value = settings.fillTarget,
                    range = 0.55f..0.95f,
                    enabled = canEdit,
                    onValueChange = { onSettingsChange(settings.copy(fillTarget = it), "fill") },
                )
                SliderSetting(
                    label = stringResource(R.string.plinth_item_scale_value, (settings.itemScale * 100).toInt()),
                    value = settings.itemScale,
                    range = 0.75f..1.25f,
                    enabled = canEdit,
                    onValueChange = { onSettingsChange(settings.copy(itemScale = it), "scale") },
                )
            }
            EditorToolCategory.POSITION -> {
                SectionLabel(stringResource(R.string.plinth_editor_position))
                Text(
                    stringResource(R.string.plinth_editor_position_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SliderSetting(
                    label = stringResource(
                        R.string.plinth_horizontal_value,
                        (settings.horizontalPosition * 100).toInt(),
                    ),
                    value = settings.horizontalPosition,
                    range = -0.12f..0.12f,
                    enabled = canEdit,
                    onValueChange = { onSettingsChange(settings.copy(horizontalPosition = it), "horizontal") },
                )
                SliderSetting(
                    label = stringResource(
                        R.string.plinth_vertical_value,
                        (settings.verticalPosition * 100).toInt(),
                    ),
                    value = settings.verticalPosition,
                    range = -0.12f..0.12f,
                    enabled = canEdit,
                    onValueChange = { onSettingsChange(settings.copy(verticalPosition = it), "vertical") },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = PlinthTheme.accentInk,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun BackgroundChoice(
    label: String,
    color: Color,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { BackgroundChoiceSwatch(color) },
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    )
}

@Composable
private fun BackgroundChoiceSwatch(color: Color) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, MaterialTheme.shapes.small),
        )
    }
}

@Composable
private fun TransparentSwatch() {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CheckerboardBackground(Modifier.size(20.dp).clip(MaterialTheme.shapes.small))
    }
}

@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    val light = MaterialTheme.colorScheme.surface
    val dark = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val cellSize = 8.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var column = 0
            while (x < size.width) {
                if ((row + column) % 2 == 0) {
                    drawRect(
                        color = light,
                        topLeft = Offset(x, y),
                        size = Size(cellSize, cellSize),
                    )
                } else {
                    drawRect(
                        color = dark,
                        topLeft = Offset(x, y),
                        size = Size(cellSize, cellSize),
                    )
                }
                x += cellSize
                column += 1
            }
            y += cellSize
            row += 1
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
        )
    }
}

@Composable
private fun CustomColorDialog(
    initialColor: Int,
    settings: CompositionSettings,
    state: EditorUiState,
    onPreviewSettingsChange: (CompositionSettings) -> Unit,
    onClearPreview: () -> Unit,
    onCancel: () -> Unit,
    onApply: (Int) -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) }
    }
    var hue by rememberSaveable(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    val colorArgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    val previewSettings = settings.copy(background = solidBackground(colorArgb))
    val previewBitmap = state.previewComposition ?: state.composition
    val previewImage = remember(previewBitmap) { previewBitmap?.asImageBitmap() }

    LaunchedEffect(colorArgb) {
        onPreviewSettingsChange(previewSettings)
    }

    AlertDialog(
        onDismissRequest = {
            onClearPreview()
            onCancel()
        },
        title = { Text(stringResource(R.string.editor_custom_color_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.editor_color_preview))
                if (state.error) {
                    Text(
                        stringResource(R.string.plinth_compose_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(colorArgb)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (colorArgb ushr 24 == 0) {
                        CheckerboardBackground(Modifier.fillMaxSize())
                    }
                    previewImage?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = stringResource(R.string.plinth_after_description),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        )
                    }
                }
                ColorSlider(
                    label = stringResource(R.string.editor_color_hue, hue.roundToInt()),
                    value = hue,
                    range = 0f..360f,
                    onValueChange = { hue = it },
                )
                ColorSlider(
                    label = stringResource(
                        R.string.editor_color_saturation,
                        (saturation * 100).roundToInt(),
                    ),
                    value = saturation,
                    range = 0f..1f,
                    onValueChange = { saturation = it },
                )
                ColorSlider(
                    label = stringResource(
                        R.string.editor_color_brightness,
                        (brightness * 100).roundToInt(),
                    ),
                    value = brightness,
                    range = 0f..1f,
                    onValueChange = { brightness = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(colorArgb) },
                enabled = state.saveState != EditorSaveState.SAVING,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.editor_apply))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClearPreview()
                    onCancel()
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.editor_cancel))
            }
        },
    )
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
        )
    }
}

@Composable
private fun shadowLabel(kind: ShadowSpec.Kind): String = when (kind) {
    ShadowSpec.Kind.NONE -> stringResource(R.string.plinth_shadow_none)
    ShadowSpec.Kind.CONTACT -> stringResource(R.string.plinth_shadow_contact)
    ShadowSpec.Kind.DROP -> stringResource(R.string.plinth_shadow_drop)
}
