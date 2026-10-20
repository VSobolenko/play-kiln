package com.gexetr.plinthproductphotos.ui.autocard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.RecognitionRequest
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImageLoader
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardFlowState
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardRecognition
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardScaleKind
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun recognizePhotoForAutoCard(
    api: ApiClient,
    jpeg: ByteArray,
    deviceToken: String,
): AutoCardFlowState = when (val result = api.recognizePhoto(RecognitionRequest(jpeg), deviceToken)) {
    is ApiResult.Failure -> AutoCardFlowState.failed()
    is ApiResult.Success -> result.value.let { value ->
        AutoCardFlowState.recognized(
            AutoCardRecognition(
                category = AutoCategory.valueOf(value.category.name),
                confidence = value.confidence.toFloat(),
                attributes = value.attributes,
                visibleText = value.visibleText,
                scaleKind = AutoCardScaleKind.valueOf(value.scaleHint.kind.name),
                scaleValueMm = value.scaleHint.valueMm,
                sku = value.sku,
            ),
        )
    }
}

internal class AutoCardRecognitionViewModel(
    context: Context,
    category: AutoCategory?,
    offline: Boolean,
) : ViewModel() {
    private val projects = ProjectRepository(context.applicationContext)
    private val sessions = WalletSessionStore(context.applicationContext)
    private val mutable = MutableStateFlow(initialAutoCardFlow(category, offline))
    val state = mutable.asStateFlow()
    private var attempted = category != null || offline

    fun recognize(photoId: String, photo: PixelBuffer? = null) {
        if (attempted) return
        attempted = true
        mutable.value = AutoCardFlowState.recognizing()
        viewModelScope.launch {
            mutable.value = try {
                val token = sessions.current()?.deviceToken
                if (token == null) AutoCardFlowState.failed()
                else {
                    val source = photo ?: withContext(Dispatchers.IO) {
                        ProjectImageLoader(projects).load(photoId).source
                    }
                    val jpeg = source.toRecognitionJpeg()
                    withContext(Dispatchers.IO) {
                        recognizePhotoForAutoCard(ApiClient(), jpeg, token)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                AutoCardFlowState.failed()
            } catch (_: IllegalArgumentException) {
                AutoCardFlowState.failed()
            } catch (_: IllegalStateException) {
                AutoCardFlowState.failed()
            }
        }
    }

    fun chooseCategory(category: AutoCategory) {
        if (!mutable.value.isRecognizing) mutable.value = mutable.value.chooseCategory(category)
    }

    fun editAttributes(attributes: List<String>) {
        mutable.value = mutable.value.editAttributes(attributes)
    }
}
