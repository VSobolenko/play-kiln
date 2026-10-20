package com.gexetr.plinthproductphotos.ui.coach

import com.gexetr.plinthproductphotos.ui.theme.PlinthTheme

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface as DisplaySurface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.data.coach.CoachCaptureHandoff
import com.gexetr.plinthproductphotos.data.coach.CoachCaptureRepository
import com.gexetr.plinthproductphotos.data.coach.CoachLevelSensor
import com.gexetr.plinthproductphotos.data.coach.CoachLevelState
import com.gexetr.plinthproductphotos.domain.coach.CheckVerdict
import com.gexetr.plinthproductphotos.domain.coach.CoachBackgroundRule
import com.gexetr.plinthproductphotos.domain.coach.CoachCheck
import com.gexetr.plinthproductphotos.domain.coach.CoachMarketplaceGuidance
import com.gexetr.plinthproductphotos.domain.coach.CoachQuality
import com.gexetr.plinthproductphotos.domain.coach.CoachQualityInput
import com.gexetr.plinthproductphotos.domain.coach.CoachQualityResult
import com.gexetr.plinthproductphotos.domain.coach.CoachRemedy
import com.gexetr.plinthproductphotos.domain.coach.MeasurementState
import com.gexetr.plinthproductphotos.domain.coach.ShotListProgress
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachRoute(
    projectId: String,
    marketplace: MarketplacePreset,
    onBack: () -> Unit,
    onUsePhoto: (CoachCaptureHandoff) -> Unit,
    initialCategory: String = "clothing",
    modifier: Modifier = Modifier,
) {
    require(projectId.isNotBlank())
    val context = LocalContext.current
    val repository = remember(context) { CoachCaptureRepository(context) }
    val cameraController = remember(context) { LifecycleCameraController(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var cameraPermission by remember { mutableStateOf<CameraPermissionState>(CameraPermissionState.Checking) }
    var cameraFailure by remember { mutableStateOf(false) }
    var levelState by remember { mutableStateOf(CoachLevelState(available = false)) }
    var category by remember(initialCategory) { mutableStateOf(normalizeCategory(initialCategory)) }
    var shotProgress by remember(category) { mutableStateOf(ShotListProgress.forCategory(category)) }
    var review by remember { mutableStateOf<PhotoReview?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importFailed by remember { mutableStateOf(false) }
    var captureFailed by remember { mutableStateOf(false) }
    var captureInFlight by remember { mutableStateOf(false) }
    var showSetup by remember { mutableStateOf(false) }
    var showShotList by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val busy = isImporting || captureInFlight

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermission = if (granted) CameraPermissionState.Granted else CameraPermissionState.Denied
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                importFailed = false
                try {
                    review = withContext(Dispatchers.IO) {
                        repository.importPickedPhoto(uri, projectId, category, roleFor(shotProgress))
                            .toReview(context, marketplace)
                    }
                } catch (_: IOException) {
                    importFailed = true
                } catch (_: IllegalArgumentException) {
                    importFailed = true
                } catch (_: SecurityException) {
                    importFailed = true
                } finally {
                    isImporting = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        cameraPermission = when {
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ->
                CameraPermissionState.NoCamera
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                CameraPermissionState.Granted
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
                CameraPermissionState.Requesting
            }
        }
    }

    val cameraReady = cameraPermission == CameraPermissionState.Granted &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
        !cameraFailure
    DisposableEffect(cameraReady, lifecycleOwner, view) {
        if (cameraReady) {
            try {
                cameraController.imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                cameraController.setTapToFocusEnabled(true)
                cameraController.bindToLifecycle(lifecycleOwner)
            } catch (_: IllegalStateException) {
                cameraFailure = true
            } catch (_: IllegalArgumentException) {
                cameraFailure = true
            }
        }
        onDispose {
            if (cameraReady) cameraController.unbind()
        }
    }

    val levelSensor = remember(cameraReady, view) {
        if (cameraReady) {
            CoachLevelSensor(
                context = context,
                displayRotation = { view.display?.rotation ?: DisplaySurface.ROTATION_0 },
                onStateChanged = { levelState = it },
            )
        } else {
            null
        }
    }
    DisposableEffect(levelSensor) {
        levelSensor?.start()
        onDispose { levelSensor?.stop() }
    }

    LaunchedEffect(review) {
        scrollState.scrollTo(0)
    }

    val capturePhoto: () -> Unit = {
        captureFailed = false
        importFailed = false
        captureInFlight = true
        val captureCategory = category
        val captureRole = roleFor(shotProgress)
        val file = File(context.cacheDir, "coach-${UUID.randomUUID()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        try {
            cameraController.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        scope.launch {
                            isImporting = true
                            try {
                                review = withContext(Dispatchers.IO) {
                                    repository.importCameraFile(
                                        file, projectId, captureCategory, captureRole,
                                    ).toReview(context, marketplace)
                                }
                            } catch (_: IOException) {
                                importFailed = true
                            } catch (_: IllegalArgumentException) {
                                importFailed = true
                            } catch (_: SecurityException) {
                                importFailed = true
                            } finally {
                                withContext(Dispatchers.IO) { file.delete() }
                                isImporting = false
                                captureInFlight = false
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        file.delete()
                        captureFailed = true
                        captureInFlight = false
                    }
                },
            )
        } catch (_: IllegalStateException) {
            file.delete()
            captureFailed = true
            captureInFlight = false
        } catch (_: SecurityException) {
            file.delete()
            captureFailed = true
            captureInFlight = false
        }
    }

    PlinthScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.coach_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.coach_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val photoReview = review
                    if (photoReview != null) {
                        Button(
                            onClick = {
                                onUsePhoto(photoReview.handoff)
                                shotProgress = shotProgress.completeCurrent()
                                review = null
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) { Text(stringResource(R.string.coach_use_photo)) }
                        TextButton(
                            onClick = { review = null },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.coach_shoot_again)) }
                    } else if (cameraReady) {
                        Button(
                            enabled = !busy,
                            onClick = {
                                if (!captureInFlight && !isImporting && review == null) capturePhoto()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Text(stringResource(if (busy) R.string.coach_saving else R.string.coach_capture))
                        }
                    }
                    if (captureFailed || importFailed) {
                        Text(
                            stringResource(
                                if (captureFailed) R.string.coach_capture_failed else R.string.coach_import_failed,
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NextShot(shotProgress)
            val photoReview = review
            if (photoReview != null) {
                PhotoReviewCard(review = photoReview)
            } else {
                when (cameraPermission) {
                    CameraPermissionState.Denied -> CameraFallback(
                        title = R.string.coach_camera_denied_title,
                        message = R.string.coach_camera_denied_body,
                        onPickPhoto = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRequestCamera = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        isImporting = busy,
                    )
                    CameraPermissionState.NoCamera -> CameraFallback(
                        title = R.string.coach_no_camera_title,
                        message = R.string.coach_no_camera_body,
                        onPickPhoto = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRequestCamera = null,
                        isImporting = busy,
                    )
                    CameraPermissionState.Checking,
                    CameraPermissionState.Requesting -> Text(stringResource(R.string.coach_camera_permission_explanation))
                    CameraPermissionState.Granted -> {
                        if (cameraFailure) {
                            CameraFallback(
                                title = R.string.coach_camera_unavailable,
                                message = R.string.coach_no_camera_body,
                                onPickPhoto = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onRequestCamera = null,
                                isImporting = busy,
                            )
                        } else {
                            CameraPreview(
                                controller = cameraController,
                                marketplace = marketplace,
                                shotId = shotProgress.currentShot?.id,
                                modifier = Modifier.fillMaxWidth().height(340.dp),
                            )
                            LevelIndicator(levelState)
                        }
                    }
                }
                if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (review == null) {
                TextButton(
                    enabled = !busy,
                    onClick = { showShotList = !showShotList },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (showShotList) R.string.coach_hide_shots else R.string.coach_show_shots))
                }
                if (showShotList) {
                    ShotList(
                        progress = shotProgress,
                        onSkip = { shotProgress = shotProgress.skipCurrent() },
                        enabled = !busy,
                    )
                }
                TextButton(
                    enabled = !busy,
                    onClick = { showSetup = !showSetup },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.coach_setup_label, marketplace.name))
                }
                if (showSetup) {
                    CategoryPicker(category = category, onCategorySelected = { category = it }, enabled = !busy)
                    MarketplaceGuidance(marketplace)
                    Text(stringResource(R.string.coach_intro), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CameraFallback(
    title: Int,
    message: Int,
    onPickPhoto: () -> Unit,
    onRequestCamera: (() -> Unit)?,
    isImporting: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
            Button(
                enabled = !isImporting,
                onClick = onPickPhoto,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.coach_open_picker))
            }
            if (onRequestCamera != null) {
                OutlinedButton(
                    onClick = onRequestCamera,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.coach_open_camera))
                }
            }
        }
    }
}

@Composable
private fun CategoryPicker(category: String, onCategorySelected: (String) -> Unit, enabled: Boolean) {
    val categories = listOf(
        "clothing" to R.string.coach_category_clothing,
        "shoes" to R.string.coach_category_shoes,
        "jewellery" to R.string.coach_category_jewellery,
        "electronics" to R.string.coach_category_electronics,
        "other" to R.string.coach_category_other,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.coach_category), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { (id, label) ->
                if (category == id) {
                    Button(
                        enabled = enabled,
                        onClick = { onCategorySelected(id) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(label)) }
                } else {
                    OutlinedButton(
                        enabled = enabled,
                        onClick = { onCategorySelected(id) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(label)) }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceGuidance(marketplace: MarketplacePreset) {
    val guidance = remember(marketplace) { CoachMarketplaceGuidance.from(marketplace) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.coach_main_guidance), style = MaterialTheme.typography.bodyMedium)
        when (guidance.backgroundRule) {
            CoachBackgroundRule.WHITE_255 -> Text(stringResource(R.string.coach_background_white))
            CoachBackgroundRule.NEUTRAL -> Text(stringResource(R.string.coach_background_neutral))
            CoachBackgroundRule.NEUTRAL_ONLY -> Text(stringResource(R.string.coach_background_neutral_only))
            CoachBackgroundRule.ANY, CoachBackgroundRule.UNSPECIFIED -> Unit
        }
        if (guidance.itemMustBeFullyVisible) Text(stringResource(R.string.coach_frame_hint))
        if (guidance.forbidPackaging) Text(stringResource(R.string.coach_main_packaging))
        if (guidance.forbidMainText) Text(stringResource(R.string.coach_main_no_text))
        if (guidance.penalizeMainText) Text(stringResource(R.string.coach_main_text_penalty))
        if (guidance.avoidMarksAndBorders) Text(stringResource(R.string.coach_main_no_marks))
    }
}

@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
    marketplace: MarketplacePreset,
    shotId: String?,
    modifier: Modifier = Modifier,
) {
    val guidance = remember(marketplace) { CoachMarketplaceGuidance.from(marketplace) }
    val ratio = guidance.aspect?.let(::parseAspect)
    val fillRatio = guidance.minimumFillRatio
    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            update = { preview ->
                preview.controller = controller
            },
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val targetRatio = ratio ?: size.width / size.height
            val frameWidth: Float
            val frameHeight: Float
            if (size.width / size.height > targetRatio) {
                frameHeight = size.height * 0.82f
                frameWidth = frameHeight * targetRatio
            } else {
                frameWidth = size.width * 0.82f
                frameHeight = frameWidth / targetRatio
            }
            val left = (size.width - frameWidth) / 2f
            val top = (size.height - frameHeight) / 2f
            drawRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
                style = Stroke(width = 2.dp.toPx()),
            )
            fillRatio?.takeIf { it in 0.0..1.0 }?.let { target ->
                val insetScale = target.toFloat()
                val innerWidth = frameWidth * insetScale
                val innerHeight = frameHeight * insetScale
                drawRect(
                    color = Color.White.copy(alpha = 0.72f),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        left + (frameWidth - innerWidth) / 2f,
                        top + (frameHeight - innerHeight) / 2f,
                    ),
                    size = androidx.compose.ui.geometry.Size(innerWidth, innerHeight),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }
        }
        Surface(
            color = Color.Black.copy(alpha = 0.78f),
            contentColor = Color.White,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
        ) {
            Text(
                text = stringResource(shotHint(shotId)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LevelIndicator(state: CoachLevelState) {
    val message = when {
        !state.available || state.degreesFromLevel == null -> R.string.coach_level_unavailable
        state.isLevel == true -> R.string.coach_level_good
        else -> R.string.coach_level_tilted
    }
    Surface(
        color = if (state.isLevel == true) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun NextShot(progress: ShotListProgress) {
    Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.coach_shot_progress, progress.completedCount, progress.shots.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                progress.currentShot?.let {
                    stringResource(R.string.coach_shot_current, stringResource(shotLabel(it.id)))
                } ?: stringResource(R.string.coach_all_shots_done),
                style = MaterialTheme.typography.titleLarge,
            )
            if (progress.currentShot?.isReference == true) {
                Text(
                    stringResource(R.string.coach_reference_short),
                    style = MaterialTheme.typography.labelLarge,
                    color = PlinthTheme.accentInk,
                )
            }
            LinearProgressIndicator(
                progress = { progress.completedCount.toFloat() / progress.shots.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShotList(progress: ShotListProgress, onSkip: () -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.coach_shot_progress, progress.completedCount, progress.shots.size),
            style = MaterialTheme.typography.titleMedium,
        )
        progress.currentShot?.let { shot ->
            Text(
                stringResource(R.string.coach_shot_current, stringResource(shotLabel(shot.id))),
                style = MaterialTheme.typography.bodyLarge,
            )
        } ?: Text(stringResource(R.string.coach_all_shots_done))
        progress.shots.forEach { shot ->
            val status = when {
                shot.id in progress.completedIds -> R.string.coach_shot_done
                shot.id in progress.skippedIds -> R.string.coach_shot_skipped
                else -> null
            }
            ListItem(
                headlineContent = { Text(stringResource(shotLabel(shot.id))) },
                supportingContent = status?.let { resource -> ({ Text(stringResource(resource)) }) },
            )
        }
        if (progress.currentShot != null) {
            TextButton(onClick = onSkip, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.coach_skip_shot))
            }
        }
    }
}

@Composable
private fun PhotoReviewCard(
    review: PhotoReview,
) {
    var showDetails by remember(review) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.coach_review_title), style = MaterialTheme.typography.titleLarge)
            AsyncImage(
                model = review.imageFile,
                contentDescription = stringResource(R.string.coach_review_title),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            if (review.handoff.item.role == SlotRole.REFERENCE) {
                Text(stringResource(R.string.coach_reference_note), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(R.string.coach_review_manual),
                style = MaterialTheme.typography.bodyMedium,
            )
            review.quality.sortedBy {
                when (it.verdict) {
                    CheckVerdict.NEEDS_ATTENTION -> 0
                    CheckVerdict.UNVERIFIED -> 1
                    CheckVerdict.PASS -> 2
                }
            }.forEach { result -> QualityRow(result, showDetails) }
            TextButton(
                onClick = { showDetails = !showDetails },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (showDetails) R.string.coach_hide_checks else R.string.coach_show_checks))
            }
        }
    }
}

@Composable
private fun QualityRow(result: CoachQualityResult, showDetails: Boolean) {
    val checkLabel = when (result.check) {
        CoachCheck.SHARPNESS -> R.string.coach_quality_sharpness
        CoachCheck.EXPOSURE -> R.string.coach_quality_exposure
        CoachCheck.FILL -> R.string.coach_quality_fill
        CoachCheck.ASPECT -> R.string.coach_quality_aspect
    }
    val status = when (result.verdict) {
        CheckVerdict.PASS -> if (result.measurementState == MeasurementState.NO_PRESET_ASPECT) {
            R.string.coach_quality_no_aspect_rule
        } else {
            R.string.coach_quality_pass
        }
        CheckVerdict.NEEDS_ATTENTION -> R.string.coach_quality_attention
        CheckVerdict.UNVERIFIED -> when {
            result.check == CoachCheck.FILL && result.measurementState == MeasurementState.NO_THRESHOLD ->
                R.string.coach_quality_no_fill_rule
            result.check == CoachCheck.SHARPNESS && result.measurementState == MeasurementState.NO_MEASUREMENT ->
                R.string.coach_quality_sharpness_measurement_missing
            result.check == CoachCheck.SHARPNESS && result.measurementState == MeasurementState.NO_THRESHOLD ->
                R.string.coach_quality_sharpness_threshold_missing
            result.check == CoachCheck.SHARPNESS -> R.string.coach_quality_unverified_sharpness
            result.check == CoachCheck.EXPOSURE && result.measurementState == MeasurementState.NO_MEASUREMENT ->
                R.string.coach_quality_exposure_measurement_missing
            result.check == CoachCheck.EXPOSURE && result.measurementState == MeasurementState.NO_THRESHOLD ->
                R.string.coach_quality_exposure_threshold_missing
            result.check == CoachCheck.EXPOSURE -> R.string.coach_quality_unverified_exposure
            result.check == CoachCheck.FILL -> R.string.coach_quality_unverified_fill
            else -> R.string.coach_quality_unverified_aspect
        }
    }
    val remedy = when (result.remedy) {
        CoachRemedy.STEADY_AND_FOCUS -> R.string.coach_remedy_sharpness
        CoachRemedy.ADJUST_LIGHT_AND_EXPOSURE -> R.string.coach_remedy_exposure
        CoachRemedy.MOVE_CLOSER_WITHOUT_CROPPING -> R.string.coach_remedy_fill
        CoachRemedy.REPOSITION_TO_MATCH_FRAME -> R.string.coach_remedy_aspect
    }
    val badge = when {
        result.measurementState == MeasurementState.NO_PRESET_ASPECT ||
            result.measurementState == MeasurementState.NO_THRESHOLD -> R.string.coach_check_no_rule
        result.verdict == CheckVerdict.NEEDS_ATTENTION -> R.string.coach_check_adjust
        result.verdict == CheckVerdict.UNVERIFIED -> R.string.coach_check_manual
        else -> R.string.coach_check_matches
    }
    Surface(
        color = if (result.verdict == CheckVerdict.NEEDS_ATTENTION) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(checkLabel),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(badge),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            if (showDetails) Text(stringResource(status), style = MaterialTheme.typography.bodyMedium)
            if (result.verdict == CheckVerdict.NEEDS_ATTENTION ||
                (showDetails && result.verdict == CheckVerdict.UNVERIFIED)
            ) {
                Text(stringResource(remedy), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun shotHint(id: String?): Int = when (id) {
    "label", "mark" -> R.string.coach_hint_label
    "measurement", "scale" -> R.string.coach_hint_measurement
    "flaw", "clasp", "ports", "sole" -> R.string.coach_hint_detail
    else -> R.string.coach_frame_hint
}

private fun CoachCaptureHandoff.toReview(
    context: Context,
    marketplace: MarketplacePreset,
): PhotoReview {
    val results = CoachQuality.evaluate(
        CoachQualityInput(
            width = pixels.width,
            height = pixels.height,
            presetAspect = marketplace.image.aspect,
            minimumFillRatio = marketplace.main.minFillRatio,
        ),
    )
    return PhotoReview(
        handoff = this,
        imageFile = File(context.filesDir, item.sourcePath),
        quality = results,
    )
}

private fun roleFor(progress: ShotListProgress): SlotRole =
    if (progress.currentShot?.isReference == true) SlotRole.REFERENCE else SlotRole.MAIN

private fun shotLabel(id: String): Int = when (id) {
    "front" -> R.string.coach_shot_front
    "back" -> R.string.coach_shot_back
    "label" -> R.string.coach_shot_label
    "flaw" -> R.string.coach_shot_flaw
    "measurement", "scale" -> R.string.coach_shot_measurement
    "side" -> R.string.coach_shot_side
    "sole" -> R.string.coach_shot_sole
    "clasp" -> R.string.coach_shot_clasp
    "mark" -> R.string.coach_shot_mark
    "ports" -> R.string.coach_shot_ports
    else -> R.string.coach_shot_front
}

private fun parseAspect(value: String): Float? {
    val pieces = value.split(':')
    if (pieces.size != 2) return null
    val width = pieces[0].toFloatOrNull() ?: return null
    val height = pieces[1].toFloatOrNull() ?: return null
    if (width <= 0f || height <= 0f) return null
    return width / height
}

private fun normalizeCategory(category: String): String = when (category.trim().lowercase()) {
    "clothing", "apparel", "garment" -> "clothing"
    "shoes", "footwear" -> "shoes"
    "jewellery", "jewelry" -> "jewellery"
    "electronics" -> "electronics"
    else -> "other"
}

private sealed interface CameraPermissionState {
    data object Checking : CameraPermissionState
    data object Requesting : CameraPermissionState
    data object Granted : CameraPermissionState
    data object Denied : CameraPermissionState
    data object NoCamera : CameraPermissionState
}

private data class PhotoReview(
    val handoff: CoachCaptureHandoff,
    val imageFile: File,
    val quality: List<CoachQualityResult>,
)
