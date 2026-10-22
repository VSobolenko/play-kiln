package com.gexetr.coverkit.ui.editor

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.gexetr.coverkit.R
import com.gexetr.coverkit.data.BrandKitStore
import com.gexetr.coverkit.data.ProjectStore
import com.gexetr.coverkit.domain.BrandKit
import com.gexetr.coverkit.domain.CalloutStickers
import com.gexetr.coverkit.domain.CoverProject
import com.gexetr.coverkit.domain.DestinationCatalog
import com.gexetr.coverkit.domain.ExportRenderer
import com.gexetr.coverkit.domain.Layer
import com.gexetr.coverkit.domain.TemplateBackdrop
import com.gexetr.coverkit.ui.components.HsvColorPicker
import com.gexetr.coverkit.ui.components.StudioChromeHeight
import com.gexetr.coverkit.ui.theme.Cyan
import com.gexetr.coverkit.ui.theme.Ink
import com.gexetr.coverkit.ui.theme.InkSoft
import com.gexetr.coverkit.ui.theme.OnCyan
import com.gexetr.coverkit.ui.theme.Paper
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.SaveFileResult
import ja.burhanrashid52.photoeditor.TextBorder
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import ja.burhanrashid52.photoeditor.ViewType
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** System emoji glyphs (font vectors) — not hand-drawn pixel stickers. */
private val STICKER_EMOJIS = listOf(
    "😀", "🔥", "✅", "💡", "🎯", "❤️", "⚡", "📌",
    "⭐", "💥", "🙌", "👀", "✨", "📣", "🆕", "💯",
    "👍", "🎉", "🚀", "💎", "🏆", "📸", "🎬", "💬",
)

private enum class EditorTool { STICKERS, TEXT, SIZES, CROP, LAYERS }

private data class TextEditRequest(
    val rootView: View?,
    val initialText: String,
    val initialColor: Int,
)

private data class EditorLayerItem(
    val root: View,
    val isText: Boolean,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    projectStore: ProjectStore,
    brandKitStore: BrandKitStore,
    unlocked: Boolean,
    onBack: () -> Unit,
    onNeedUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val brandKit by brandKitStore.brandKit.collectAsStateWithLifecycle(initialValue = BrandKit())
    var project by remember { mutableStateOf<CoverProject?>(null) }
    var photoEditor by remember { mutableStateOf<PhotoEditor?>(null) }
    var photoEditorView by remember { mutableStateOf<PhotoEditorView?>(null) }
    var activeTool by remember { mutableStateOf<EditorTool?>(null) }
    var packSelection by remember { mutableStateOf(emptySet<String>()) }
    var textEdit by remember { mutableStateOf<TextEditRequest?>(null) }
    var safeZoneVisible by remember { mutableStateOf(false) }
    var layerEpoch by remember { mutableIntStateOf(0) }

    // Free Export watermarks the PNG; FLAG_SECURE omitted so store capture can photograph the UI.

    LaunchedEffect(projectId) { project = projectStore.load(projectId) }

    fun exitBrushMode() {
        photoEditor?.setBrushDrawingMode(false)
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            exitBrushMode()
            context.coilBitmap(uri)?.let { photoEditor?.addImage(it) }
        }
    }
    val pickSticker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            exitBrushMode()
            context.coilBitmap(uri)?.let { bmp ->
                val sticker = bmp.scaleMax(512)
                photoEditor?.addImage(sticker)
            }
            activeTool = null
        }
    }

    fun capture(action: (Bitmap) -> Unit) {
        val editor = photoEditor ?: return
        scope.launch {
            val stagingFile = File(context.cacheDir, "editor-capture/${projectId}.png").also { it.parentFile?.mkdirs() }
            if (editor.saveAsFile(stagingFile.absolutePath) !is SaveFileResult.Success) {
                snackbar.showSnackbar(context.getString(R.string.editor_save_failed))
                return@launch
            }
            val bitmap = BitmapFactory.decodeFile(stagingFile.absolutePath) ?: run {
                snackbar.showSnackbar(context.getString(R.string.editor_save_failed))
                return@launch
            }
            action(if (unlocked) bitmap else bitmap.withWatermark(context.getString(R.string.app_name)))
        }
    }

    /** Persist PhotoEditor canvas into Projects (no watermark) so leave/kill does not lose work. */
    fun saveDraft(then: (() -> Unit)? = null, quiet: Boolean = false) {
        val editor = photoEditor ?: run {
            then?.invoke()
            return
        }
        val current = project ?: run {
            then?.invoke()
            return
        }
        scope.launch {
            val dir = File(context.filesDir, "projects/${current.id}").also { it.mkdirs() }
            val canvasFile = File(dir, "canvas.png")
            if (editor.saveAsFile(canvasFile.absolutePath) !is SaveFileResult.Success) {
                if (!quiet) snackbar.showSnackbar(context.getString(R.string.editor_save_failed))
                then?.invoke()
                return@launch
            }
            val updated = current.copy(
                layers = listOf(
                    Layer.PhotoLayer(
                        id = "draft-canvas",
                        uriString = Uri.fromFile(canvasFile).toString(),
                    ),
                ),
                updatedAtMs = System.currentTimeMillis(),
            )
            projectStore.save(updated)
            project = updated
            if (!quiet) {
                snackbar.showSnackbar(
                    message = context.getString(R.string.editor_draft_saved),
                    duration = SnackbarDuration.Long,
                )
            }
            then?.invoke()
        }
    }

    fun exportSingle(share: Boolean) = capture { bitmap ->
        scope.launch(Dispatchers.IO) {
            if (share) sharePng(context, bitmap, "${project?.name ?: "cover"}.png")
            else savePng(context, bitmap, "${project?.name ?: "cover"}.png")
            withContext(Dispatchers.Main) {
                snackbar.showSnackbar(context.getString(if (share) R.string.editor_shared else R.string.editor_saved))
            }
        }
    }

    fun exportPack() = capture { bitmap ->
        scope.launch(Dispatchers.IO) {
            val name = project?.name ?: "cover"
            val flattened = File(context.cacheDir, "editor-capture/$projectId-pack-source.png")
            FileOutputStream(flattened).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val sourceProject = project?.copy(
                layers = listOf(
                    Layer.PhotoLayer(
                        id = "photo-editor-capture",
                        uriString = Uri.fromFile(flattened).toString(),
                    ),
                ),
            )
            packSelection.forEach { destinationId ->
                val rendered = sourceProject?.let { captured ->
                    ExportRenderer.renderBitmap(
                        project = captured,
                        watermark = false,
                        targetDestinationId = destinationId,
                        photoDecoder = ExportRenderer.PhotoDecoder { uri ->
                            BitmapFactory.decodeFile(Uri.parse(uri).path)
                        },
                    )
                } ?: bitmap
                savePng(context, rendered, "${name}_$destinationId.png")
            }
            withContext(Dispatchers.Main) {
                snackbar.showSnackbar(context.getString(R.string.editor_saved))
            }
        }
    }

    fun defaultTextColor(): Int =
        brandKit.colors.firstOrNull()?.let { it or (0xFF shl 24) } ?: AndroidColor.WHITE

    fun openTextEditor(preferExisting: Boolean) {
        exitBrushMode()
        val existing = if (preferExisting) photoEditorView?.latestTextLayer() else null
        textEdit = if (existing != null) {
            TextEditRequest(existing.root, existing.text, existing.color)
        } else {
            TextEditRequest(null, context.getString(R.string.editor_default_text), defaultTextColor())
        }
        activeTool = EditorTool.TEXT
    }

    fun applyText(text: String, color: Int, strokeEnabled: Boolean, strokeColor: Int) {
        val trimmed = text.trim().ifEmpty { return }
        exitBrushMode()
        val opaque = color or (0xFF shl 24)
        val style = TextStyleBuilder().apply {
            withTextColor(opaque)
            withTextSize(36f)
            withTextFont(brandKit.textTypeface())
            if (strokeEnabled) {
                withTextBorder(
                    TextBorder(
                        /* corner = */ 4f,
                        /* backGroundColor = */ AndroidColor.TRANSPARENT,
                        /* strokeWidth = */ 8,
                        /* strokeColor = */ strokeColor or (0xFF shl 24),
                    ),
                )
            }
        }
        val req = textEdit
        val editor = photoEditor ?: return
        val root = req?.rootView?.takeIf { it.isAttachedToWindow }
        if (root != null) {
            editor.editText(root, trimmed, style)
        } else {
            editor.addText(trimmed, style)
        }
        textEdit = null
        activeTool = null
        layerEpoch += 1
        scope.launch {
            snackbar.showSnackbar(context.getString(R.string.editor_text_applied))
        }
    }

    fun addEmojiSticker(emoji: String, tintArgb: Int) {
        exitBrushMode()
        photoEditor?.addImage(emojiBitmap(emoji, tintArgb))
        activeTool = null
        layerEpoch += 1
    }

    fun addCallout(kind: CalloutStickers.Kind, tintArgb: Int) {
        exitBrushMode()
        photoEditor?.addImage(CalloutStickers.render(kind, tintArgb))
        activeTool = null
        layerEpoch += 1
    }

    fun applyCrop(fill: Boolean) {
        val view = photoEditorView ?: return
        val dest = project?.let { DestinationCatalog.byId(it.destinationId) } ?: return
        val source = view.source.asBitmap() ?: return
        val result = if (fill) {
            source.centerCropTo(dest.widthPx, dest.heightPx)
        } else {
            source.fitInside(dest.widthPx, dest.heightPx)
        }
        view.source.setImageBitmap(result)
        activeTool = null
        scope.launch {
            snackbar.showSnackbar(context.getString(R.string.editor_crop_done))
        }
    }

    // Spec: process kill restores the open project — quiet draft write on stop.
    val lifecycleOwner = LocalLifecycleOwner.current
    val quietAutosave by rememberUpdatedState(newValue = { saveDraft(quiet = true) })
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) quietAutosave()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Ink,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Ink)) {
            EditorTopBar(
                safeZoneVisible = safeZoneVisible,
                onBack = { saveDraft(then = onBack) },
                onUndo = { photoEditor?.undo() },
                onRedo = { photoEditor?.redo() },
                onToggleSafeZone = { safeZoneVisible = !safeZoneVisible },
                // Free path: watermarked Export/Share (OpenSpec T2.6–T2.7). Unlock removes watermark.
                onExport = { exportSingle(false) },
                onShare = { exportSingle(true) },
                // Save = durable draft in Projects (free, no watermark).
                onSave = { saveDraft() },
            )
            Text(
                stringResource(R.string.editor_hint_tap_text),
                color = Paper.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                PhotoEditorCanvas(
                    project = project,
                    modifier = Modifier.fillMaxSize(),
                    onReady = { view, editor ->
                        photoEditorView = view
                        photoEditor = editor
                        editor.setOnPhotoEditorListener(
                            object : OnPhotoEditorListener {
                                override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int) {
                                    textEdit = TextEditRequest(rootView, text, colorCode or (0xFF shl 24))
                                    activeTool = EditorTool.TEXT
                                }

                                override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                                    layerEpoch += 1
                                }

                                override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
                                    layerEpoch += 1
                                }

                                override fun onStartViewChangeListener(viewType: ViewType) = Unit
                                override fun onStopViewChangeListener(viewType: ViewType) = Unit
                                override fun onTouchSourceImage(event: android.view.MotionEvent) = Unit
                            },
                        )
                    },
                    onApplied = { editor ->
                        photoEditor = editor
                        editor.setBrushDrawingMode(false)
                        layerEpoch += 1
                    },
                )
                // Compose-only overlay — never drawn into PhotoEditor save/export.
                if (safeZoneVisible) {
                    SafeZoneOverlay(
                        destinationId = project?.destinationId,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            EditorRail(
                onPhoto = {
                    exitBrushMode()
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onText = { openTextEditor(preferExisting = true) },
                onSticker = {
                    exitBrushMode()
                    activeTool = EditorTool.STICKERS
                },
                onCrop = {
                    exitBrushMode()
                    activeTool = EditorTool.CROP
                },
                onLayers = {
                    exitBrushMode()
                    activeTool = EditorTool.LAYERS
                },
                onSizes = {
                    if (unlocked) {
                        packSelection = setOf(project?.destinationId.orEmpty())
                        activeTool = EditorTool.SIZES
                    } else {
                        onNeedUnlock()
                    }
                },
            )
        }
    }

    when (activeTool) {
        EditorTool.TEXT -> TextEditSheet(
            request = textEdit ?: TextEditRequest(
                null,
                stringResource(R.string.editor_default_text),
                defaultTextColor(),
            ),
            onDismiss = { activeTool = null; textEdit = null },
            onApply = { text, color, strokeOn, strokeColor ->
                applyText(text, color, strokeOn, strokeColor)
            },
        )
        EditorTool.STICKERS -> StickersSheet(
            onDismiss = { activeTool = null },
            onCallout = { kind, tint -> addCallout(kind, tint) },
            onEmoji = { emoji, tint -> addEmojiSticker(emoji, tint) },
            onPickGallery = {
                pickSticker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
        EditorTool.CROP -> CropSheet(
            onDismiss = { activeTool = null },
            onFill = { applyCrop(fill = true) },
            onFit = { applyCrop(fill = false) },
        )
        EditorTool.LAYERS -> {
            val layers = remember(photoEditorView, layerEpoch) {
                photoEditorView?.collectEditableLayers().orEmpty()
            }
            LayersSheet(
                layers = layers,
                onDismiss = { activeTool = null },
                onDelete = { item ->
                    (item.root.parent as? ViewGroup)?.removeView(item.root)
                    layerEpoch += 1
                },
                onBringFront = { item ->
                    item.root.bringToFront()
                    (item.root.parent as? ViewGroup)?.invalidate()
                    layerEpoch += 1
                },
            )
        }
        EditorTool.SIZES -> ModalBottomSheet(
            onDismissRequest = { activeTool = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = InkSoft,
            contentColor = Paper,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    stringResource(R.string.editor_sizes_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                )
                Text(
                    stringResource(R.string.editor_sizes_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.copy(alpha = 0.85f),
                )
                DestinationCatalog.all.forEach { destination ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                packSelection =
                                    if (destination.id in packSelection) packSelection - destination.id
                                    else packSelection + destination.id
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = destination.id in packSelection,
                            onCheckedChange = {
                                packSelection =
                                    if (it) packSelection + destination.id else packSelection - destination.id
                            },
                        )
                        Text(
                            stringResource(
                                R.string.editor_size_item,
                                stringResource(destination.labelRes),
                                stringResource(R.string.dest_size_format, destination.widthPx, destination.heightPx),
                            ),
                        )
                    }
                }
                Button(
                    onClick = { activeTool = null; exportPack() },
                    enabled = packSelection.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = OnCyan),
                ) { Text(stringResource(R.string.nav_export)) }
            }
        }
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextEditSheet(
    request: TextEditRequest,
    onDismiss: () -> Unit,
    onApply: (text: String, color: Int, strokeEnabled: Boolean, strokeColor: Int) -> Unit,
) {
    var draft by remember(request) { mutableStateOf(request.initialText) }
    var color by remember(request) { mutableIntStateOf(request.initialColor or (0xFF shl 24)) }
    var strokeEnabled by remember(request) { mutableStateOf(true) }
    var strokeColor by remember(request) { mutableIntStateOf(AndroidColor.BLACK or (0xFF shl 24)) }
    val scroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Sheet drag steals HSV / hue gestures — disable it (owner 2026-10-22).
        sheetGesturesEnabled = false,
        containerColor = InkSoft,
        contentColor = Paper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (request.rootView != null) {
                    stringResource(R.string.editor_edit_existing)
                } else {
                    stringResource(R.string.editor_add_new_text)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Cyan,
            )
            Text(
                stringResource(R.string.editor_text_howto),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.copy(alpha = 0.85f),
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                label = { Text(stringResource(R.string.editor_text_label)) },
            )
            // Apply first so it stays on-screen above colour pickers (A40 sheet buried it).
            Button(
                onClick = { onApply(draft, color, strokeEnabled, strokeColor) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = OnCyan),
            ) { Text(stringResource(R.string.editor_text_apply), fontWeight = FontWeight.Bold) }
            HsvColorPicker(argb = color, onArgb = { color = it }, compact = true)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        stringResource(R.string.editor_text_stroke),
                        color = Paper,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.editor_text_stroke_hint),
                        color = Paper.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = strokeEnabled,
                    onCheckedChange = { strokeEnabled = it },
                )
            }
            if (strokeEnabled) {
                HsvColorPicker(argb = strokeColor, onArgb = { strokeColor = it }, compact = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StickersSheet(
    onDismiss: () -> Unit,
    onCallout: (CalloutStickers.Kind, Int) -> Unit,
    onEmoji: (String, Int) -> Unit,
    onPickGallery: () -> Unit,
) {
    var tint by remember { mutableIntStateOf(AndroidColor.WHITE) }
    val scroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        containerColor = InkSoft,
        contentColor = Paper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.nav_stickers),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Cyan,
            )
            Text(stringResource(R.string.editor_sticker_howto), color = Paper.copy(alpha = 0.85f))
            Text(stringResource(R.string.editor_callouts), color = Paper, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CalloutChip(R.string.editor_callout_arrow) {
                    onCallout(CalloutStickers.Kind.ARROW, tint)
                }
                CalloutChip(R.string.editor_callout_ribbon) {
                    onCallout(CalloutStickers.Kind.RIBBON, tint)
                }
                CalloutChip(R.string.editor_callout_burst) {
                    onCallout(CalloutStickers.Kind.BURST, tint)
                }
                CalloutChip(R.string.editor_callout_frame) {
                    onCallout(CalloutStickers.Kind.STORY_FRAME, tint)
                }
                CalloutChip(R.string.editor_callout_circle) {
                    onCallout(CalloutStickers.Kind.CIRCLE, tint)
                }
            }
            Text(stringResource(R.string.editor_sticker_emoji), color = Paper, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                STICKER_EMOJIS.forEach { emoji ->
                    Text(
                        emoji,
                        modifier = Modifier
                            .padding(4.dp)
                            .heightIn(min = 48.dp)
                            .clickable { onEmoji(emoji, tint) },
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Button(
                onClick = onPickGallery,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = OnCyan),
            ) { Text(stringResource(R.string.editor_sticker_from_photo)) }
            HsvColorPicker(argb = tint, onArgb = { tint = it }, compact = true)
        }
    }
}

@Composable
private fun CalloutChip(labelRes: Int, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(labelRes), color = Cyan, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropSheet(
    onDismiss: () -> Unit,
    onFill: () -> Unit,
    onFit: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkSoft,
        contentColor = Paper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.editor_crop_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Cyan,
            )
            Text(
                stringResource(R.string.editor_crop_hint),
                color = Paper.copy(alpha = 0.85f),
            )
            Button(
                onClick = onFill,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = OnCyan),
            ) { Text(stringResource(R.string.editor_crop_fill), fontWeight = FontWeight.Bold) }
            Button(
                onClick = onFit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = OnCyan),
            ) { Text(stringResource(R.string.editor_crop_fit), fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayersSheet(
    layers: List<EditorLayerItem>,
    onDismiss: () -> Unit,
    onDelete: (EditorLayerItem) -> Unit,
    onBringFront: (EditorLayerItem) -> Unit,
) {
    val scroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkSoft,
        contentColor = Paper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.editor_layers_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Cyan,
            )
            if (layers.isEmpty()) {
                Text(
                    stringResource(R.string.editor_layers_empty),
                    color = Paper.copy(alpha = 0.85f),
                )
            } else {
                layers.asReversed().forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (item.isText) {
                                stringResource(
                                    R.string.editor_layers_text,
                                    item.label.ifBlank { "…" }.take(40),
                                )
                            } else {
                                stringResource(R.string.editor_layers_image)
                            },
                            modifier = Modifier.weight(1f),
                            color = Paper,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = { onBringFront(item) }) {
                            Text(stringResource(R.string.editor_layers_bring_front), color = Cyan)
                        }
                        IconButton(
                            onClick = { onDelete(item) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trash),
                                contentDescription = stringResource(R.string.editor_layers_delete),
                                tint = Paper,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeZoneOverlay(
    destinationId: String?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val insets = safeZoneInsets(destinationId, size.width, size.height, density)
        val left = insets.left
        val top = insets.top
        val right = size.width - insets.right
        val bottom = size.height - insets.bottom
        val path = Path().apply {
            addRect(Rect(left, top, right, bottom))
        }
        drawPath(
            path = path,
            color = Cyan.copy(alpha = 0.85f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
            ),
        )
    }
}

@Composable
private fun PhotoEditorCanvas(
    project: CoverProject?,
    modifier: Modifier,
    onReady: (PhotoEditorView, PhotoEditor) -> Unit,
    onApplied: (PhotoEditor) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val destination = project?.let { DestinationCatalog.byId(it.destinationId) }
    var appliedKey by remember { mutableStateOf<String?>(null) }
    var editorRef by remember { mutableStateOf<PhotoEditor?>(null) }
    AndroidView(
        factory = {
            PhotoEditorView(it).also { view ->
                val editor = PhotoEditor.Builder(it, view).setPinchTextScalable(true).build()
                editorRef = editor
                onReady(view, editor)
            }
        },
        modifier = modifier.background(Ink),
        update = { view ->
            val currentProject = project ?: return@AndroidView
            val dest = destination ?: return@AndroidView
            val editor = editorRef ?: return@AndroidView
            val photoLayers = currentProject.layers.filterIsInstance<Layer.PhotoLayer>()
            val photo = photoLayers.lastOrNull()
            val texts = currentProject.layers.filterIsInstance<Layer.TextLayer>()
            val key = listOf(
                currentProject.id,
                currentProject.backdropStyle,
                photo?.colorArgb,
                photo?.uriString,
                texts.joinToString { "${it.text}:${it.x}:${it.y}:${it.fillArgb}" },
            ).joinToString("|")
            if (appliedKey == key) return@AndroidView
            appliedKey = key
            // Flattened Save/Back draft: sole PhotoLayer with uri (id draft-canvas) — load as
            // source, never re-add template TextLayers or treat the uri as a movable sticker.
            val draftUri = photo?.uriString?.takeIf { uri ->
                uri.isNotBlank() && (
                    photo.id == "draft-canvas" ||
                        (photoLayers.size == 1 && texts.isEmpty())
                    )
            }
            scope.launch {
                if (draftUri != null) {
                    val draftBmp = withContext(Dispatchers.IO) {
                        val parsed = Uri.parse(draftUri)
                        parsed.path?.let { path -> BitmapFactory.decodeFile(path) }
                            ?: context.coilBitmap(parsed)
                    }
                    withContext(Dispatchers.Main) {
                        editor.setBrushDrawingMode(false)
                        editor.clearAllViews()
                        // Draft is the flattened canvas — source only; never template text.
                        if (draftBmp != null) view.source.setImageBitmap(draftBmp)
                        onApplied(editor)
                    }
                    return@launch
                }
                val color = photo?.colorArgb ?: 0xFF1D3557.toInt()
                val style = TemplateBackdrop.fromIndex(currentProject.backdropStyle)
                val backdrop = TemplateBackdrop.render(dest.widthPx, dest.heightPx, color, style)
                val userPhoto = photo?.uriString?.takeIf { it.isNotBlank() }?.let { uri ->
                    context.coilBitmap(Uri.parse(uri))
                }
                withContext(Dispatchers.Main) {
                    editor.setBrushDrawingMode(false)
                    editor.clearAllViews()
                    view.source.setImageBitmap(backdrop)
                    // User photo is an added (movable) layer — not locked as source.
                    userPhoto?.let { editor.addImage(it.scaleMax(maxOf(dest.widthPx, dest.heightPx))) }
                    // One multi-line block — PhotoEditor Position(x,y) can collapse width into
                    // a vertical strip, and two centred addText calls stack on top of each other.
                    val copy = texts.sortedBy { it.zIndex }.joinToString("\n") { it.text }.trim()
                    if (copy.isNotEmpty()) {
                        val lead = texts.maxByOrNull { it.scale } ?: texts.first()
                        val styleBuilder = TextStyleBuilder().apply {
                            withTextColor(lead.fillArgb)
                            withTextSize((40f * lead.scale).coerceIn(26f, 72f))
                        }
                        editor.addText(copy, styleBuilder)
                    }
                    onApplied(editor)
                }
            }
        },
    )
}

@Composable
private fun EditorTopBar(
    safeZoneVisible: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSafeZone: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) = Row(
    Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 4.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    EditorIcon(R.drawable.ic_arrow_left, stringResource(R.string.action_back), onBack)
    EditorIcon(R.drawable.ic_arrow_left, stringResource(R.string.action_undo), onUndo)
    EditorIcon(R.drawable.ic_plus, stringResource(R.string.action_redo), onRedo)
    IconButton(onClick = onToggleSafeZone, modifier = Modifier.size(48.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_layout_grid),
            contentDescription = stringResource(
                if (safeZoneVisible) R.string.nav_safe_zone_on else R.string.nav_safe_zone,
            ),
            tint = if (safeZoneVisible) Cyan else Paper,
        )
    }
    Spacer(Modifier.weight(1f))
    Button(
        onClick = onExport,
        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = OnCyan),
    ) { Text(stringResource(R.string.nav_export), fontWeight = FontWeight.Bold) }
    EditorIcon(R.drawable.ic_share, stringResource(R.string.nav_share), onShare)
    EditorIcon(R.drawable.ic_save, stringResource(R.string.nav_save), onSave)
}

@Composable
private fun EditorRail(
    onPhoto: () -> Unit,
    onText: () -> Unit,
    onSticker: () -> Unit,
    onCrop: () -> Unit,
    onLayers: () -> Unit,
    onSizes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .height(StudioChromeHeight)
            .background(InkSoft),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RailItem(Modifier.weight(1f), R.drawable.ic_photo, R.string.nav_photo, onPhoto)
            RailItem(Modifier.weight(1f), R.drawable.ic_text, R.string.nav_text, onText)
            RailItem(Modifier.weight(1f), R.drawable.ic_sticker, R.string.nav_stickers, onSticker)
            RailItem(Modifier.weight(1f), R.drawable.ic_crop, R.string.nav_crop, onCrop)
            RailItem(Modifier.weight(1f), R.drawable.ic_layers, R.string.nav_layers, onLayers)
            RailItem(Modifier.weight(1f), R.drawable.ic_export, R.string.editor_sizes_short, onSizes)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RailItem(modifier: Modifier, icon: Int, label: Int, onClick: () -> Unit) =
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = Cyan,
            modifier = Modifier.size(22.dp),
        )
        Text(
            stringResource(label),
            color = Paper,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

@Composable
private fun EditorIcon(icon: Int, description: String, onClick: () -> Unit) =
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(painter = painterResource(icon), contentDescription = description, tint = Paper)
    }

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private data class CanvasTextLayer(val root: View, val text: String, val color: Int)

private fun BrandKit.textTypeface(): Typeface =
    when (fontPrimary) {
        "serif" -> Typeface.SERIF
        else -> Typeface.DEFAULT_BOLD
    }

private fun safeZoneInsets(
    destinationId: String?,
    width: Float,
    height: Float,
    density: Density,
): ZoneInsets = with(density) {
    when {
        destinationId == "yt_thumb" -> {
            val mx = width * 0.08f
            val my = height * 0.08f
            ZoneInsets(mx, my, mx, my)
        }
        destinationId == "ig_post" || (width > 0f && height > 0f && kotlin.math.abs(width - height) < 1f) -> {
            val m = 16.dp.toPx()
            ZoneInsets(m, m, m, m)
        }
        height > width -> ZoneInsets(0f, 56.dp.toPx(), 0f, 72.dp.toPx())
        else -> {
            val m = 16.dp.toPx()
            ZoneInsets(m, m, m, m)
        }
    }
}

private data class ZoneInsets(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Walk PhotoEditorView for text roots (tvPhotoEditorText) — tap-to-edit is flaky when drag wins. */
private fun PhotoEditorView.latestTextLayer(): CanvasTextLayer? {
    val textId = ja.burhanrashid52.photoeditor.R.id.tvPhotoEditorText
    val layers = mutableListOf<CanvasTextLayer>()
    fun walk(node: View) {
        if (node !is ViewGroup) return
        var foundHere = false
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is TextView && child.id == textId) {
                layers += CanvasTextLayer(
                    root = node,
                    text = child.text?.toString().orEmpty(),
                    color = child.currentTextColor or (0xFF shl 24),
                )
                foundHere = true
            }
        }
        if (!foundHere) {
            for (i in 0 until node.childCount) {
                val child = node.getChildAt(i)
                if (child is ViewGroup) walk(child)
            }
        }
    }
    walk(this)
    return layers.lastOrNull()
}

private fun PhotoEditorView.collectEditableLayers(): List<EditorLayerItem> {
    val textId = ja.burhanrashid52.photoeditor.R.id.tvPhotoEditorText
    val imageId = ja.burhanrashid52.photoeditor.R.id.imgPhotoEditorImage
    val layers = mutableListOf<EditorLayerItem>()
    fun walk(node: View) {
        if (node !is ViewGroup) return
        var matched = false
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            when {
                child is TextView && child.id == textId -> {
                    layers += EditorLayerItem(
                        root = node,
                        isText = true,
                        label = child.text?.toString().orEmpty(),
                    )
                    matched = true
                }
                child is ImageView && child.id == imageId -> {
                    layers += EditorLayerItem(
                        root = node,
                        isText = false,
                        label = "",
                    )
                    matched = true
                }
            }
        }
        if (!matched) {
            for (i in 0 until node.childCount) {
                val child = node.getChildAt(i)
                if (child is ViewGroup) walk(child)
            }
        }
    }
    walk(this)
    return layers
}

private fun ImageView.asBitmap(): Bitmap? {
    val d = drawable ?: return null
    val bitmapDrawable = d as? BitmapDrawable
    if (bitmapDrawable?.bitmap != null) return bitmapDrawable.bitmap
    val w = d.intrinsicWidth.coerceAtLeast(1)
    val h = d.intrinsicHeight.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    d.setBounds(0, 0, canvas.width, canvas.height)
    d.draw(canvas)
    return bmp
}

private fun Bitmap.centerCropTo(destW: Int, destH: Int): Bitmap {
    val srcAspect = width.toFloat() / height.toFloat()
    val destAspect = destW.toFloat() / destH.toFloat()
    return if (srcAspect > destAspect) {
        val h = destH
        val w = (destH * srcAspect).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(this, w, h, true)
        val x = ((w - destW) / 2).coerceAtLeast(0)
        Bitmap.createBitmap(scaled, x, 0, destW.coerceAtMost(scaled.width), destH.coerceAtMost(scaled.height))
    } else {
        val w = destW
        val h = (destW / srcAspect).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(this, w, h, true)
        val y = ((h - destH) / 2).coerceAtLeast(0)
        Bitmap.createBitmap(scaled, 0, y, destW.coerceAtMost(scaled.width), destH.coerceAtMost(scaled.height))
    }
}

private fun Bitmap.fitInside(destW: Int, destH: Int, letterboxColor: Int = AndroidColor.BLACK): Bitmap {
    val out = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(letterboxColor)
    val scale = minOf(destW.toFloat() / width, destH.toFloat() / height)
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(this, w, h, true)
    val left = (destW - w) / 2f
    val top = (destH - h) / 2f
    canvas.drawBitmap(scaled, left, top, null)
    return out
}

private suspend fun Context.coilBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(this@coilBitmap).data(uri).build()
    val result = ImageLoader(this@coilBitmap).execute(request)
    (result as? SuccessResult)?.image?.toBitmap()
}

private fun Bitmap.scaleMax(maxSide: Int): Bitmap {
    val scale = maxSide.toFloat() / maxOf(width, height)
    if (scale >= 1f) return this
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}

/** Renders a system emoji glyph (font vector) onto a transparent bitmap; optional HSV tint for monochrome overlays. */
private fun emojiBitmap(emoji: String, tintArgb: Int, sizePx: Int = 256): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sizePx * 0.72f
        typeface = Typeface.DEFAULT
        color = AndroidColor.WHITE
    }
    val x = sizePx / 2f
    val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(emoji, x, y, paint)
    if (tintArgb != AndroidColor.WHITE && tintArgb != 0xFFFFFFFF.toInt()) {
        val tinted = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val tc = Canvas(tinted)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.PorterDuffColorFilter(
                tintArgb or (0xFF shl 24),
                android.graphics.PorterDuff.Mode.SRC_IN,
            )
        }
        tc.drawBitmap(bmp, 0f, 0f, tp)
        return tinted
    }
    return bmp
}

private fun Bitmap.withWatermark(label: String): Bitmap =
    copy(config ?: Bitmap.Config.ARGB_8888, true).also { bitmap ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(170, 255, 255, 255)
            textAlign = Paint.Align.RIGHT
            textSize = minOf(bitmap.width, bitmap.height) * 0.045f
            isFakeBoldText = true
        }
        Canvas(bitmap).drawText(label, bitmap.width - paint.textSize, bitmap.height - paint.textSize, paint)
    }

private fun sharePng(context: Context, bitmap: Bitmap, name: String) {
    val file = File(context.cacheDir, "exports/$name").also { it.parentFile?.mkdirs() }
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            context.getString(R.string.nav_share),
        ),
    )
}

private fun savePng(context: Context, bitmap: Bitmap, displayName: String) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null,
        )
    }
}
