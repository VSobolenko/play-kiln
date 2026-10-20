package com.gexetr.plinthproductphotos.ui.autocard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.SubcomposeAsyncImage
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardFlowState
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset
import com.gexetr.plinthproductphotos.ui.LocalPhotoSharedTransitionScope
import com.gexetr.plinthproductphotos.ui.PhotoTransitionKey
import com.gexetr.plinthproductphotos.ui.components.PlinthScaffold
import com.gexetr.plinthproductphotos.ui.importscreen.photoSwipeBack

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AutoCardSourcePhoto(
    photoId: String?,
    bitmap: Bitmap?,
    aspectRatio: Float,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val sourceFile = remember(context, photoId) {
        photoId?.let { id ->
            val repository = ProjectRepository(context)
            repository.photo(id)?.let { repository.sourceFile(it.sourcePath) }
        }
    }
    val sourceAspectRatio = remember(sourceFile) {
        sourceFile?.let { file ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth > 0 && bounds.outHeight > 0) bounds.outWidth.toFloat() / bounds.outHeight else null
        }
    }
    val photoTransition = LocalPhotoSharedTransitionScope.current?.takeIf { photoId != null }?.let { sharedScope ->
        with(sharedScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(PhotoTransitionKey(requireNotNull(photoId))),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            )
        }
    } ?: Modifier
    SubcomposeAsyncImage(
        model = bitmap ?: sourceFile,
        contentDescription = stringResource(R.string.home_preview_original),
        contentScale = ContentScale.Fit,
        modifier = photoTransition.fillMaxWidth().aspectRatio(sourceAspectRatio ?: aspectRatio).photoSwipeBack(onBack),
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        error = {
            Text(
                stringResource(R.string.w3_preview_failed),
                color = MaterialTheme.colorScheme.error,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutoCardLoadingScreen(
    onBack: () -> Unit,
    sourcePhotoContent: @Composable (Bitmap?, Float) -> Unit,
) {
    PlinthScaffold(
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
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(Modifier.padding(12.dp)) {
                    sourcePhotoContent(null, 1.2f)
                }
            }
            Text(stringResource(R.string.home_nav_photo_loading))
        }
    }
}

internal fun initialAutoCardFlow(
    lastUsedCategory: AutoCategory?,
    isOffline: Boolean,
): AutoCardFlowState {
    val initial = if (isOffline) AutoCardFlowState.offline() else AutoCardFlowState()
    return lastUsedCategory?.let { initial.chooseCategory(it) } ?: initial
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun AutoCardRoute(
    photo: PixelBuffer,
    mask: Mask,
    canvas: Canvas,
    preset: MarketplacePreset,
    sceneRecipesJson: String,
    lastUsedCategory: AutoCategory?,
    isOffline: Boolean,
    sceneBackgrounds: Map<String, PixelBuffer> = emptyMap(),
    onBack: () -> Unit,
    onCardReady: (PixelBuffer) -> Unit,
    modifier: Modifier = Modifier,
    onChangePhoto: () -> Unit = onBack,
    photoId: String? = null,
    sourcePhotoContent: (@Composable (Bitmap?, Float) -> Unit)? = null,
) {
    GeneratedSceneRoute(
        photo = photo,
        preset = preset,
        sceneRecipesJson = sceneRecipesJson,
        lastUsedCategory = lastUsedCategory,
        isOffline = isOffline,
        onUseOriginal = { onCardReady(photo) },
        photoId = photoId,
        onBack = onBack,
        onChangePhoto = onChangePhoto,
        modifier = modifier,
        sourceAspectRatio = photo.width.toFloat() / photo.height,
        sourcePhotoContent = sourcePhotoContent,
    )
}
